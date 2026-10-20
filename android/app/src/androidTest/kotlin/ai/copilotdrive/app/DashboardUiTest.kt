package ai.copilotdrive.app

import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ai.copilotdrive.app.ui.CopilotTheme
import ai.copilotdrive.app.ui.DashboardScreen
import ai.copilotdrive.core.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DashboardUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun simulationAndUnknownRangeAreVisible() {
        compose.setContent {
            CopilotTheme {
                Surface {
                    DashboardScreen(DashboardState(vehicle = simulatedVehicle(0)), {}, {}, {}, {}, {})
                }
            }
        }
        compose.onNodeWithText("SIMULADOR · Datos de vehículo ficticios").assertExists()
        compose.onNodeWithTag("fuel-value").assertTextEquals("42 %")
        compose.onNodeWithText("No disponible").assertExists()
    }

    @Test fun demoQueryHasAnAccessibleAction() {
        var requested = ""
        compose.setContent {
            CopilotTheme { Surface { DashboardScreen(DashboardState(vehicle = simulatedVehicle(0)), {}, {}, {}, { requested = it }, {}) } }
        }
        compose.onNodeWithTag("demo-fuel").assertIsDisplayed().performClick()
        assertEquals("Jarvis, cuánta gasolina queda", requested)
    }

    @Test fun unsupportedNextIsDisabled() {
        val media = DemoMediaAdapter(Clock { 0 }).state.value.copy(supportedControls = setOf(MediaControl.PLAY))
        compose.setContent {
            CopilotTheme { Surface { DashboardScreen(DashboardState(vehicle = simulatedVehicle(0), media = media), {}, {}, {}, {}, {}) } }
        }
        compose.onNodeWithText("Siguiente").assertIsNotEnabled()
    }

    @Test fun onboardingRequiresExplicitStart() {
        var accepted = false
        compose.setContent { CopilotTheme { Onboarding { accepted = true } } }
        compose.onNodeWithTag("accept-onboarding").performScrollTo().performClick()
        assertEquals(true, accepted)
    }

    @Test fun compactLandscapeKeepsVoiceActionsVisible() {
        compose.setContent {
            CopilotTheme { Box(Modifier.requiredSize(800.dp, 480.dp)) {
                DashboardScreen(DashboardState(vehicle = simulatedVehicle(0)), {}, {}, {}, {}, {})
            } }
        }
        compose.onNodeWithTag("push-to-talk").assertIsDisplayed()
        compose.onNodeWithTag("demo-fuel").assertIsDisplayed()
    }

    @Test fun portraitKeepsVoiceActionsAndResultVisible() {
        compose.setContent {
            CopilotTheme { Box(Modifier.requiredSize(360.dp, 640.dp)) {
                DashboardScreen(DashboardState(voice = VoiceSessionState(message = "Simulación. Combustible: 42 %.")), {}, {}, {}, {}, {})
            } }
        }
        compose.onNodeWithTag("push-to-talk").assertIsDisplayed()
        compose.onNodeWithTag("voice-status").assertIsDisplayed().assertTextContains("42 %", substring = true)
    }
}
