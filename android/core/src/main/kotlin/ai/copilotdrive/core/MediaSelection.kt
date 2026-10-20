package ai.copilotdrive.core

data class MediaSessionChoice(val id: String, val label: String, val playing: Boolean)
fun selectMediaSession(sessions: List<MediaSessionChoice>, selected: String?): String? {
    sessions.firstOrNull { it.id == selected }?.let { return it.id }
    val playing = sessions.filter { it.playing }
    return playing.singleOrNull()?.id ?: sessions.singleOrNull()?.id
}
