package ai.copilotdrive.app

import ai.copilotdrive.app.ui.*
import ai.copilotdrive.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

/** Only this test injects a synthetic MediaSession presentation. Always labelled TEST/DEMO. */
class MediaFixtureVisualTest {
    @get:Rule val compose = createComposeRule()
    @Test fun syntheticMediaIsLabelledAndUnsupportedControlsStayDisabled() {
        val fixture = DemoMediaAdapter(Clock { 1 }).state.value.copy(supportedControls = setOf(MediaControl.PLAY))
        compose.setContent { CopilotTheme { androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = Cockpit.Background) { Column(Modifier.safeDrawingPadding().padding(24.dp)) {
            Text("TEST FIXTURE · DEMO · NO ES REPRODUCCIÓN REAL", color = Amber)
            MediaCard(fixture, null, {})
        } } } }
        compose.onNodeWithText("AUDIO / DEMO").assertExists()
        compose.onNodeWithText("Siguiente").assertIsNotEnabled()
        compose.waitForIdle()
        Thread.sleep(400)
        val bitmap = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        java.io.File(context.filesDir, "sprint11-media-test-fixture.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
