package ai.copilotdrive.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @org.junit.Before fun initialRealMode() {
        kotlinx.coroutines.runBlocking { ai.copilotdrive.app.platform.Preferences(compose.activity).save(ai.copilotdrive.app.platform.VoicePreferences(onboarded = true)) }
        compose.activityRule.scenario.recreate()
    }

    @Test fun placesWithoutConsentOrProviderCannotSearch() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithTag("accept-onboarding").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("open-places").performClick()
        val provider = if (BuildConfig.PLACES_ANDROID_API_KEY.isBlank()) "Proveedor: NOT_CONFIGURED" else "Proveedor: Google Places"
        compose.onNodeWithTag("places-screen").performScrollToNode(hasText(provider))
        compose.onNodeWithText(provider).assertIsDisplayed()
        compose.onNodeWithTag("places-screen").performScrollToNode(hasTestTag("places-stopped"))
        compose.onNodeWithTag("places-stopped").performClick()
        compose.onNodeWithTag("places-screen").performScrollToNode(hasTestTag("search-RESTAURANT"))
        compose.onNodeWithTag("search-RESTAURANT").assertIsNotEnabled()
        compose.onNodeWithTag("places-screen").performScrollToNode(hasText("Volver"))
        compose.onNodeWithText("Volver").performClick()
        compose.onNodeWithTag("dashboard").assertExists()
    }

    @Test fun onboardingToFuelQueryThroughRealViewModel() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("accept-onboarding").performScrollTo().performClick()
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("demo-fuel").assertIsDisplayed().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText("No puedo consultar el combustible porque todavía no estoy conectado", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("voice-status").assertTextContains("No puedo consultar el combustible porque todavía no estoy conectado", substring = true)
    }

    @Test fun obdSettingsRequireAdapterSelectionAndExplicitDemo() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("accept-onboarding").performScrollTo().performClick()
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("Estoy en un entorno detenido").performClick()
        compose.onNodeWithTag("settings").performScrollToNode(hasText("04 / VEHÍCULO"))
        compose.onNodeWithText("04 / VEHÍCULO").assertIsDisplayed()
        compose.onNodeWithText("Conectar").assertIsNotEnabled()
        compose.onNodeWithText("Activar modo DEMO").assertExists()
    }

    @Test fun deviceLabRunsPassiveChecksWithoutHardware() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithTag("accept-onboarding").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("Estoy en un entorno detenido").performClick()
        compose.onNodeWithTag("settings").performScrollToNode(hasTestTag("open-device-lab"))
        compose.onNodeWithTag("open-device-lab").performClick()
        compose.onNodeWithTag("device-lab").assertExists()
        compose.onNodeWithTag("run-local-diagnostics").performClick()
        compose.onNodeWithTag("device-lab").performScrollToNode(hasText("Resultados (", substring = true))
        compose.onNodeWithText("Resultados (", substring = true).assertExists()
        compose.onNodeWithText("Borrar historial").performClick()
        compose.onNodeWithText("Sin comprobaciones en esta sesión.").assertExists()
    }

    @Test fun placesDemoSearchSelectOpenAndReturnToLocalDashboard() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithTag("accept-onboarding").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("open-places").performClick()
        compose.onNodeWithTag("places-screen").assertExists()
        compose.onNodeWithTag("places-screen").performScrollToNode(hasTestTag("places-stopped"))
        compose.onNodeWithTag("places-stopped").performClick()
        compose.onNodeWithTag("places-screen").performScrollToNode(hasTestTag("places-demo"))
        compose.onNodeWithTag("places-demo").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Proveedor: SIMULACIÓN").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("places-screen").performScrollToNode(hasTestTag("search-RESTAURANT"))
        compose.onNodeWithTag("search-RESTAURANT").performClick()
        compose.waitUntil(10_000) {
            compose.onNodeWithTag("places-screen").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription] == "Estado: READY"
        }
        compose.onNodeWithTag("places-screen").performScrollToNode(hasTestTag("select-place-0"))
        compose.onNodeWithTag("select-place-0").performClick()
        compose.onNodeWithTag("places-screen").performScrollToNode(hasTestTag("open-selected-maps"))
        compose.onNodeWithTag("open-selected-maps").performClick()
        compose.onNodeWithTag("places-screen").performScrollToNode(hasText("Volver"))
        compose.onNodeWithText("Volver").performClick()
        compose.onNodeWithTag("dashboard").assertExists()
        compose.onNodeWithTag("demo-fuel").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("Simulación. Combustible: 42 %", substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }
}
