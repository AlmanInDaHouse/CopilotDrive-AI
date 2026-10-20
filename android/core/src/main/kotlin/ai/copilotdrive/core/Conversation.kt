package ai.copilotdrive.core

enum class ConversationRole { USER, ASSISTANT }
data class ConversationMessage(val role: ConversationRole, val text: String)

interface ConversationStore {
    fun history(sessionId: String): List<ConversationMessage>
    fun append(sessionId: String, message: ConversationMessage)
    fun clear(sessionId: String)
    fun clearAll()
}

/** Memory is process-local, bounded, and never contains audio or contact data. */
class EphemeralConversationStore(
    private val maxMessages: Int = 8,
    private val maxChars: Int = 2_000,
    private val maxTokenBudget: Int = 2_000,
) : ConversationStore {
    private val sessions = mutableMapOf<String, MutableList<ConversationMessage>>()
    override fun history(sessionId: String): List<ConversationMessage> = synchronized(this) {
        sessions[sessionId]?.toList().orEmpty()
    }
    override fun append(sessionId: String, message: ConversationMessage) = synchronized(this) {
        if (message.text.isBlank()) return@synchronized
        val list = sessions.getOrPut(sessionId) { mutableListOf() }
        list += message.copy(text = message.text.take(400))
        // UTF-8 bytes are a conservative upper bound for byte-based tokenizer tokens.
        while (list.size > maxMessages || list.sumOf { it.text.length } > maxChars ||
            list.sumOf { it.text.toByteArray(Charsets.UTF_8).size } > maxTokenBudget) list.removeAt(0)
    }
    override fun clear(sessionId: String) = synchronized(this) { sessions.remove(sessionId); Unit }
    override fun clearAll() = synchronized(this) { sessions.clear() }
}
