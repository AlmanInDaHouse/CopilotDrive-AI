package ai.copilotdrive.core

data class ProviderConfiguration(
    val enabled: Boolean = false,
    val endpoint: String = "",
    val model: String = "",
    val timeoutMs: Int = 10_000,
    val consented: Boolean = false,
    val shareVehicleContext: Boolean = false,
    val toolCalling: Boolean = false,
    val providerName: String = "",
)
data class ProviderCapabilities(val structuredTools: Boolean = false)
interface ProviderSettings {
    fun current(): ProviderConfiguration
}
interface SecretStore {
    fun get(): String?
    fun put(secret: String)
    fun clear()
}
data class AIRequest(
    val text: String,
    val history: List<ConversationMessage> = emptyList(),
    val allowedTools: List<ToolDefinition> = emptyList(),
    val locale: String = "es-ES",
)
data class ToolProposal(val toolId: String, val arguments: Map<String, String> = emptyMap()) {
    fun asRequest() = ToolRequest(toolId, arguments)
}
data class AIError(val code: String, val message: String)
sealed interface AIResponse {
    data class Text(val content: String) : AIResponse
    data class ProposedTool(val proposal: ToolProposal) : AIResponse
    data class Unavailable(val error: AIError) : AIResponse
    data class Invalid(val error: AIError) : AIResponse
}
