package ai.copilotdrive.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

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
            compose.onAllNodes(hasText("Simulación. Combustible: 42 %", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("voice-status").assertTextContains("Simulación. Combustible: 42 %", substring = true)
    }

    @Test fun obdSettingsRequireAdapterSelectionAndShowSimulatorFallback() {
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
        compose.onNodeWithTag("settings").performScrollToNode(hasText("VEHÍCULO · OBD-II"))
        compose.onNodeWithText("VEHÍCULO · OBD-II").assertIsDisplayed()
        compose.onNodeWithText("Conectar").assertIsNotEnabled()
        compose.onNodeWithText("Volver al simulador").assertExists()
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
}
