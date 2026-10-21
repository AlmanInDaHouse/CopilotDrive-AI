package ai.copilotdrive.app

import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Semantics regressions only: these are not human TalkBack observations. */
class AccessibilityTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun dashboard() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isNotEmpty() ||
            compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithTag("accept-onboarding").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun settings() {
        dashboard(); compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("Estoy en un entorno detenido").performClick()
    }
    @Test fun settingsControlsHaveAccessibleNamesAndDisabledContextExplainsWhy() {
        settings()
        for (label in listOf("Permitir motor STT del sistema; puede enviar audio a su proveedor", "Velocidad de voz", "Tono de voz",
            "Usar audio de demostración", "Activar IA externa", "Tiempo de espera del proveedor", "Herramientas estructuradas", "Compartir contexto del vehículo")) {
            compose.onNodeWithTag("settings").performScrollToNode(hasContentDescription(label))
            compose.onNodeWithContentDescription(label).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithContentDescription("Compartir contexto del vehículo").assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "No disponible; solo se envía texto"))
    }
    @Test fun fixedControlsExposeMicrophoneStateTouchTargetsAndDismissibleFullAnswer() {
        dashboard()
        compose.onNodeWithTag("voice-status").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Micrófono apagado"))
        for (tag in listOf("push-to-talk", "voice-status", "demo-fuel")) {
            val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            val density = compose.activity.resources.displayMetrics.density
            assertTrue("48dp target: $tag", bounds.height / density >= 48f && bounds.width / density >= 48f)
        }
        compose.onNodeWithTag("voice-status").performClick()
        compose.onNodeWithText("Cerrar").assertHasClickAction().performClick()
        compose.onNodeWithTag("push-to-talk").assertIsDisplayed()
    }
    @Test fun editorActionsIdentifyWidgetAndCurrentSize() {
        dashboard(); compose.onNodeWithTag("edit-dashboard").performClick()
        compose.onNodeWithTag("layout-laboratory").assertContentDescriptionEquals("Activar modo laboratorio").performClick()
        compose.onNodeWithTag("confirm-layout-laboratory").performClick()
        compose.onNodeWithTag("dashboard-editor").performScrollToNode(hasTestTag("preset-CLASSIC"))
        compose.onNodeWithTag("preset-CLASSIC").performClick()
        compose.onNodeWithTag("dashboard-editor").performScrollToNode(hasTestTag("move-up-vehicle"))
        compose.onNodeWithTag("move-up-vehicle").assertIsNotEnabled()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
        compose.onNodeWithTag("move-down-vehicle").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
        compose.onNodeWithTag("dashboard-editor").performScrollToNode(hasTestTag("resize-vehicle-EXPANDED"))
        compose.onNodeWithTag("resize-vehicle-EXPANDED")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Tamaño actual"))
    }
    @Test fun placesTogglesAreNamedAndBackReturnsToDashboard() {
        dashboard(); compose.onNodeWithTag("open-places").performClick()
        compose.onNodeWithTag("places-stopped").assertContentDescriptionEquals("Estoy en un entorno detenido")
        compose.onNodeWithTag("places-screen").performScrollToNode(hasTestTag("places-demo"))
        compose.onNodeWithTag("places-demo").assertContentDescriptionEquals("Modo de prueba de lugares; datos ficticios")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("push-to-talk").assertIsDisplayed()
    }
}
