package ai.copilotdrive.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import ai.copilotdrive.app.platform.DataStoreLayoutRepository
import ai.copilotdrive.core.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DashboardCustomizationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun dashboard() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isNotEmpty() ||
            compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithTag("accept-onboarding").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun editor() {
        compose.onNodeWithTag("dashboard").performScrollToNode(hasTestTag("edit-dashboard"))
        compose.onNodeWithTag("edit-dashboard").performClick()
        compose.onNodeWithTag("confirm-layout-laboratory").assertIsNotEnabled()
        compose.onNodeWithTag("layout-laboratory").performClick()
        compose.onNodeWithTag("confirm-layout-laboratory").performClick()
        compose.onNodeWithTag("dashboard-editor").assertExists()
    }
    private fun action(tag: String) {
        compose.onNodeWithTag("dashboard-editor").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performClick()
    }
    private fun persisted() = runBlocking { DataStoreLayoutRepository(compose.activity).state.first().layout }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val bitmap = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(compose.activity.filesDir, "sprint6-$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    @Test fun addReorderResizeHidePreviewSaveRecreateRestoreAndCancel() {
        dashboard(); editor(); screenshot("editor"); action("preset-CLASSIC")
        action("add-clock"); action("move-up-clock")
        action("resize-vehicle-WIDE"); action("toggle-media")
        action("preview-layout")
        screenshot("preview")
        compose.onNodeWithTag("push-to-talk").assertIsDisplayed()
        action("preview-layout"); action("save-layout")
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty() }
        val saved = persisted()
        assertEquals(WidgetSize.WIDE, saved.widgets.first { it.type == WidgetType.VEHICLE }.size)
        assertEquals(WidgetVisibility.HIDDEN, saved.widgets.first { it.type == WidgetType.MEDIA }.visibility)
        assertTrue(saved.widgets.indexOfFirst { it.type == WidgetType.CLOCK } < saved.widgets.indexOfFirst { it.type == WidgetType.ASSISTANT })
        compose.activityRule.scenario.recreate()
        dashboard()
        assertEquals(saved, persisted())
        compose.onNodeWithTag("dashboard").performScrollToNode(hasTestTag("widget-clock"))
        compose.onNodeWithTag("widget-clock").assertExists()
        editor(); action("preset-MEDIA"); action("cancel-layout")
        assertEquals(saved, persisted())
        editor(); action("restore-layout"); action("save-layout")
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(WidgetLayouts.preset(DashboardPreset.CLASSIC), persisted())
    }
    @Test fun revokeAndBackgroundDiscardDraftAndRequireNewConfirmation() {
        dashboard(); editor(); action("preset-LABORATORY"); action("revoke-layout-lab")
        compose.onNodeWithTag("dashboard").assertExists()
        editor(); action("preset-DRIVER")
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        dashboard()
        compose.onAllNodesWithTag("dashboard-editor").assertCountEquals(0)
        editor(); action("cancel-layout")
        compose.onNodeWithTag("push-to-talk").assertIsDisplayed()
    }
}
