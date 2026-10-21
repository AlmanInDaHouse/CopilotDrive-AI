package ai.copilotdrive.app

import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.test.platform.app.InstrumentationRegistry
import ai.copilotdrive.app.platform.DataStoreLayoutRepository
import ai.copilotdrive.app.platform.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Local UI only. Does not invoke speech, location, media playback or an external provider. */
class StabilityJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun dashboard() {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("dashboard-ready").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("push-to-talk").assertIsDisplayed()
    }
    private fun action(container: String, tag: String) {
        compose.onNodeWithTag(container).performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performClick()
    }
    @Test fun finishingActivityCancelsViewModelJobs() {
        lateinit var model: DashboardViewModel
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[DashboardViewModel::class.java] }
        val owner = requireNotNull(model.viewModelScope.coroutineContext[kotlinx.coroutines.Job])
        assertTrue(owner.isActive)
        compose.activityRule.scenario.close()
        assertFalse(owner.isActive)
        assertTrue(owner.children.none { it.isActive })
    }
    @Test fun restoredSettingsDoesNotDeclareDashboardReady() {
        val preferences = Preferences(compose.activity.applicationContext)
        val original = runBlocking { preferences.state.first() }
        try {
            runBlocking { preferences.save(original.copy(onboarded = true)) }
            dashboard()
            compose.onNodeWithTag("dashboard-ready").assertExists()
            compose.onNodeWithText("Ajustes").performClick()
            compose.onNodeWithText("Estoy en un entorno detenido").performClick()
            compose.activityRule.scenario.recreate()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("settings").fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodesWithTag("dashboard-ready").assertCountEquals(0)
            compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
            dashboard()
            compose.onNodeWithTag("dashboard-ready").assertExists()
        } finally { runBlocking { preferences.save(original) } }
    }
    @Test fun cyclesPreserveConfigurationAndRecover() {
        require(Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("SDK")) { "EMULATOR_ONLY" }
        val preferences = Preferences(compose.activity.applicationContext)
        val layout = DataStoreLayoutRepository(compose.activity)
        val originalPreferences = runBlocking { preferences.state.first() }
        val originalLayout = runBlocking { layout.state.first().layout }
        val count = InstrumentationRegistry.getArguments().getString("cycles", "3").toInt()
        require(count in 1..1000)
        val memory = org.json.JSONArray()
        val operations = org.json.JSONArray()
        val oldActivities = mutableListOf<java.lang.ref.WeakReference<MainActivity>>()
        val model = ViewModelProvider(compose.activity)[DashboardViewModel::class.java]
        val process = android.os.Process.myPid()
        fun measure(name: String, action: () -> Unit) {
            val at = android.os.SystemClock.elapsedRealtimeNanos()
            action(); compose.waitForIdle()
            operations.put(org.json.JSONObject().put("operation", name)
                .put("upperBoundMs", (android.os.SystemClock.elapsedRealtimeNanos() - at) / 1_000_000.0))
        }
        val started = android.os.SystemClock.elapsedRealtime()
        try {
            runBlocking { preferences.save(originalPreferences.copy(onboarded = true)) }
            dashboard()
            repeat(count) { index ->
                action("dashboard", "edit-dashboard")
                compose.onNodeWithTag("layout-laboratory").performClick()
                compose.onNodeWithTag("confirm-layout-laboratory").performClick()
                measure("presetDraft") { action("dashboard-editor", "preset-${if (index % 2 == 0) "MEDIA" else "DRIVER"}") }
                measure("backgroundRestore") {
                    compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
                    compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
                    dashboard()
                }
                compose.onAllNodesWithTag("dashboard-editor").assertCountEquals(0)
                measure("settingsWithConfirmation") {
                    compose.onNodeWithText("Ajustes").performClick()
                    compose.onNodeWithText("Estoy en un entorno detenido").performClick()
                    compose.onNodeWithTag("settings").assertExists()
                }
                measure("deviceLabWithScroll") { action("settings", "open-device-lab") }
                compose.onNodeWithTag("run-local-diagnostics").performClick()
                compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
                compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
                dashboard()
                action("dashboard", "open-places")
                compose.onNodeWithTag("places-screen").assertExists()
                compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
                dashboard()
                oldActivities += java.lang.ref.WeakReference(compose.activity)
                compose.activityRule.scenario.recreate()
                dashboard()
                assertSame(model, ViewModelProvider(compose.activity)[DashboardViewModel::class.java])
                assertEquals(process, android.os.Process.myPid())
                assertEquals(originalLayout, runBlocking { layout.state.first().layout })
                val info = android.os.Debug.MemoryInfo().also { android.os.Debug.getMemoryInfo(it) }
                val runtime = Runtime.getRuntime()
                memory.put(org.json.JSONObject().put("cycle", index + 1)
                    .put("elapsedMs", android.os.SystemClock.elapsedRealtime() - started)
                    .put("instrumentedPssKiB", info.totalPss)
                    .put("javaHeapAllocatedKiB", (runtime.totalMemory() - runtime.freeMemory()) / 1024)
                    .put("nativeHeapAllocatedKiB", android.os.Debug.getNativeHeapAllocatedSize() / 1024)
                    .put("graphicsPssKiB", info.getMemoryStat("summary.graphics")?.toIntOrNull() ?: org.json.JSONObject.NULL)
                    .put("oldActivitiesWeakLive", oldActivities.count { it.get() != null })
                    .put("viewModelDirectJobs", model.viewModelScope.coroutineContext[kotlinx.coroutines.Job]?.children?.count() ?: 0))
            }
        } finally {
            runBlocking { preferences.save(originalPreferences) }
            assertEquals(originalLayout, runBlocking { layout.state.first().layout })
            java.io.File(compose.activity.filesDir, "sprint8-stability-memory.json").writeText(memory.toString())
            java.io.File(compose.activity.filesDir, "sprint8-operation-bounds.json").writeText(operations.toString())
        }
    }
}
