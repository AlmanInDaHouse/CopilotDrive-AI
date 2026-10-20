package ai.copilotdrive.core

import org.junit.Assert.*
import org.junit.Test

class MediaSelectionTest {
    @Test fun `one playing session wins deterministically`() {
        val sessions = listOf(MediaSessionChoice("b", "B", false), MediaSessionChoice("a", "A", true))
        assertEquals("a", selectMediaSession(sessions, null))
        assertEquals("b", selectMediaSession(sessions, "b"))
    }
    @Test fun `multiple playing or paused sessions require choice`() {
        assertNull(selectMediaSession(listOf(MediaSessionChoice("a", "A", true), MediaSessionChoice("b", "B", true)), null))
        assertNull(selectMediaSession(listOf(MediaSessionChoice("a", "A", false), MediaSessionChoice("b", "B", false)), null))
    }
    @Test fun `removed manual selection cannot leave stale session`() {
        assertNull(selectMediaSession(emptyList(), "a"))
        assertEquals("b", selectMediaSession(listOf(MediaSessionChoice("b", "B", false)), "a"))
    }
}
