package ai.copilotdrive.app

import ai.copilotdrive.core.*
import ai.copilotdrive.app.ui.CopilotTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Synthetic state only: never starts a microphone or audio service and cannot qualify acoustics. */
class PhysicalPilotUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun packagedManifestKeepsCoarseAndExcludesTransitivePreciseLocation() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        @Suppress("DEPRECATION")
        val permissions = context.packageManager.getPackageInfo(context.packageName,
            android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toSet()
        assertTrue(android.Manifest.permission.ACCESS_COARSE_LOCATION in permissions)
        assertFalse(android.Manifest.permission.ACCESS_FINE_LOCATION in permissions)
        assertFalse(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION in permissions)
    }
    @Test fun callbackLeavesHumanPendingAndExplicitNegativeConfirmationIsIndependent() {
        var state by mutableStateOf(AcousticProbeState(1, AcousticProbeStatus.COMPLETED))
        compose.setContent { CopilotTheme {
            AcousticProbePanel(state, {}, {}, { attempt, heard ->
                assertEquals(1L, attempt)
                state = state.copy(human = if (heard) HumanOutcome.CONFIRMED else HumanOutcome.NOT_OBSERVED)
            })
        } }
        compose.onNodeWithTag("acoustic-human").assertTextEquals("Observación humana: PENDING")
        compose.onNodeWithTag("acoustic-not-heard").performClick()
        compose.onNodeWithTag("acoustic-human").assertTextEquals("Observación humana: NOT_OBSERVED")
        compose.onNodeWithTag("acoustic-heard").assertIsNotEnabled()
        compose.onNodeWithText("Calificación física PENDING", substring = true).assertExists()
    }
    @Test fun failurePreservesWrittenPhraseAndDoesNotOfferAudibilityConfirmation() {
        compose.setContent { CopilotTheme { AcousticProbePanel(AcousticProbeState(1, AcousticProbeStatus.FAILED), {}, {}, { _, _ -> }) } }
        compose.onNodeWithText(AcousticProbe.PHRASE).assertExists()
        compose.onNodeWithTag("acoustic-heard").assertDoesNotExist()
        compose.onNodeWithText("TTS no disponible.", substring = true).assertExists()
    }
}
