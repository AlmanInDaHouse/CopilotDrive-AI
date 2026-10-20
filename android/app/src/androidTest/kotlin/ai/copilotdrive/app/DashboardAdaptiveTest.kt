package ai.copilotdrive.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ai.copilotdrive.app.ui.*
import ai.copilotdrive.core.*
import org.junit.Rule
import org.junit.Test

class DashboardAdaptiveTest {
    @get:Rule val compose = createComposeRule()
    private fun render(width: Int, height: Int, scale: Float = 1f) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, scale)) {
                CopilotTheme { Surface { Box(Modifier.requiredSize(width.dp, height.dp)) {
                    DashboardScreen(DashboardState(vehicle = simulatedVehicle(0)), {}, {}, {}, {}, {},
                        layout = WidgetLayouts.preset(DashboardPreset.LABORATORY))
                } } }
            }
        }
        compose.onNodeWithTag("push-to-talk").assertIsDisplayed()
        compose.onNodeWithText("Ajustes").assertIsDisplayed()
        compose.onNodeWithTag("telemetry-source").assertIsDisplayed()
        compose.onNodeWithTag("voice-status").assertIsDisplayed()
        compose.onNodeWithTag("dashboard").performScrollToNode(hasTestTag("widget-system_status"))
        compose.onNodeWithTag("widget-device-lab").assertExists()
    }
    @Test fun smallLandscape800by480dp() = render(800, 480)
    @Test fun landscape1024by600dp() = render(1024, 600)
    @Test fun landscape1280by720dp() = render(1280, 720)
    @Test fun portrait360by640dp() = render(360, 640)
    @Test fun largeFontCompact800by480dp() = render(800, 480, 1.5f)
    @Test fun unavailableVehicleAndCapabilitiesRemainHonest() {
        compose.setContent { CopilotTheme { Surface {
            DashboardScreen(DashboardState(), {}, {}, {}, {}, {})
        } } }
        compose.onNodeWithTag("fuel-value").assertTextEquals("No disponible")
        compose.onNodeWithTag("telemetry-source").assertTextContains("Sin datos", substring = true)
        compose.onNodeWithTag("push-to-talk").assertIsDisplayed()
    }
}
