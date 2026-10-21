package ai.copilotdrive.app

import ai.copilotdrive.app.platform.Preferences
import ai.copilotdrive.app.platform.VoicePreferences
import ai.copilotdrive.core.OperationMode
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Writes test preferences: run only on a named disposable AVD. No physical qualification. */
class RealModeJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun real() {
        runBlocking { Preferences(compose.activity).save(VoicePreferences(onboarded = true)) }
        compose.activityRule.scenario.recreate()
        dashboard()
    }
    private fun dashboard() = compose.waitUntil(15_000) {
        compose.onAllNodesWithTag("dashboard-ready").fetchSemanticsNodes().isNotEmpty()
    }
    private fun settings() {
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("Estoy en un entorno detenido").performClick()
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        // Window/RenderThread submission can finish after Compose semantics become idle.
        Thread.sleep(400)
        val bitmap = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(compose.activity.filesDir, "sprint11-$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
    @Test fun realEmptyTelemetryAndLocalFuelNeverInventValues() {
        compose.onNodeWithTag("telemetry-source").assertTextEquals("REAL · OBD desconectado")
        capture("dashboard-real")
        compose.onNodeWithTag("dashboard").performScrollToNode(hasTestTag("fuel-value"))
        compose.onNodeWithTag("fuel-value").assertTextEquals("No disponible")
        compose.onNodeWithTag("demo-fuel").performClick()
        compose.waitUntil(15_000) { compose.onAllNodes(hasText("No puedo consultar el combustible", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("voice-status").performClick()
        capture("jarvis-response")
        compose.onNodeWithText("Cerrar").performClick()
        compose.onNodeWithTag("dashboard").performScrollToNode(hasTestTag("widget-media"))
        compose.onNodeWithText("Sin reproducción").assertExists()
        capture("media-empty")
    }
    @Test fun explicitDemoPersistsAndReturningRealRemovesAllSyntheticSources() {
        settings(); capture("settings")
        compose.onNodeWithTag("operation-mode-demo").performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithTag("operation-mode-demo").assertIsOff()
        compose.onNodeWithTag("operation-mode-demo").performClick()
        compose.onNodeWithText("Activar DEMO").performClick()
        compose.waitUntil { compose.onAllNodesWithText("DEMO · FICTICIO").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Volver").performScrollTo().performClick()
        dashboard(); capture("dashboard-demo")
        compose.activityRule.scenario.recreate(); dashboard()
        compose.onNodeWithTag("telemetry-source").assertTextEquals("DEMO · Datos de vehículo ficticios")
        settings(); compose.onNodeWithTag("operation-mode-demo").performClick()
        compose.onNodeWithText("Volver").performScrollTo().performClick(); dashboard()
        compose.onNodeWithTag("telemetry-source").assertTextEquals("REAL · OBD desconectado")
        compose.onNodeWithTag("dashboard").performScrollToNode(hasTestTag("fuel-value"))
        compose.onNodeWithTag("fuel-value").assertTextEquals("No disponible")
        compose.onNodeWithTag("dashboard").performScrollToNode(hasTestTag("assistant-message"))
        capture("jarvis-idle")
    }
    @Test fun placesNotConfiguredAndDeviceLabAreHonest() {
        compose.onNodeWithTag("open-places").performClick(); capture("places")
        if (BuildConfig.PLACES_ANDROID_API_KEY.isBlank()) {
            compose.onNodeWithTag("places-screen").performScrollToNode(hasTestTag("search-RESTAURANT"))
            compose.onNodeWithTag("search-RESTAURANT").assertIsNotEnabled()
        }
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }; dashboard()
        settings(); compose.onNodeWithTag("settings").performScrollToNode(hasTestTag("open-device-lab"))
        compose.onNodeWithTag("open-device-lab").performClick(); capture("device-lab")
    }
    @Test fun legacyMigrationInformsWithoutReplacingVoiceOrLayoutAndAcknowledgementPersists() {
        // Exercise acknowledgement persistence and unrelated preferences; legacy mode decoding is covered in OperationModeTest.
        val context = compose.activity
        val original = runBlocking { ai.copilotdrive.app.platform.DataStoreLayoutRepository(context).state.first().layout }
        runBlocking { Preferences(context).save(VoicePreferences(onboarded = true, rate = 1.2f, modeNoticeRequired = true)) }
        compose.activityRule.scenario.recreate(); dashboard()
        compose.onNodeWithTag("dashboard").performScrollToNode(hasTestTag("acknowledge-real-mode"))
        compose.onNodeWithText("Ahora empiezas en REAL").assertExists()
        compose.onNodeWithTag("acknowledge-real-mode").performClick()
        compose.activityRule.scenario.recreate(); dashboard()
        compose.onAllNodesWithTag("acknowledge-real-mode").assertCountEquals(0)
        val saved = runBlocking { Preferences(context).state.first() }
        org.junit.Assert.assertEquals(1.2f, saved.rate)
        org.junit.Assert.assertEquals(OperationMode.REAL, saved.operationMode)
        org.junit.Assert.assertEquals(original, runBlocking { ai.copilotdrive.app.platform.DataStoreLayoutRepository(context).state.first().layout })
    }
}
