package ai.copilotdrive.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoMediaAdapterTest {
    @Test fun `demo source stays labelled and responds to controls`() = runTest {
        var now = 100L
        val demo = DemoMediaAdapter(Clock { now })
        assertTrue(demo.state.value.simulated)
        assertEquals("Simulador de audio", demo.state.value.sourceApp)

        now = 200L
        assertEquals(ExecutionStatus.SUCCESS, demo.command(MediaControl.PLAY).executionStatus)
        assertEquals(PlaybackStatus.PLAYING, demo.state.value.playbackStatus)
        assertEquals(200L, demo.state.value.capturedAt)
        assertTrue(demo.state.value.simulated)

        assertEquals(ExecutionStatus.SUCCESS, demo.command(MediaControl.NEXT).executionStatus)
        assertEquals("Coastal Road", demo.state.value.title)
        assertEquals(ExecutionStatus.SUCCESS, demo.command(MediaControl.PAUSE).executionStatus)
        assertEquals(PlaybackStatus.PAUSED, demo.state.value.playbackStatus)
    }

    @Test fun `unsupported seek cannot mutate demo state`() = runTest {
        val demo = DemoMediaAdapter(Clock { 100L })
        val before = demo.state.value
        val result = demo.command(MediaControl.SEEK)
        assertEquals(ExecutionStatus.DENIED, result.executionStatus)
        assertEquals("UNSUPPORTED", result.errorCode)
        assertEquals(before, demo.state.value)
    }
}
