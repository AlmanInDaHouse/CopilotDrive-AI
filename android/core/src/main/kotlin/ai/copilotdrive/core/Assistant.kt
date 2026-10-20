package ai.copilotdrive.core

import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.withTimeoutOrNull

fun normalize(text: String) = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").trim().trim('¿', '?', '.', '¡', '!')

class IntentResolver {
    fun resolve(text: String): ToolRequest? {
        val original = text.trim().replace(Regex("^jarvis[, ]*", RegexOption.IGNORE_CASE), "")
        val normalized = normalize(original)
        if (normalized.matches(Regex("(cuanta gasolina( me)? queda|cuanto combustible( me)? queda|combustible|gasolina|nivel de combustible)"))) return ToolRequest("vehicle.get_fuel")
        if (normalized in setOf("temperatura", "temperatura del motor", "refrigerante")) return ToolRequest("vehicle.get_temperature")
        if (normalized in setOf("estado del coche", "como esta el coche", "estado del vehiculo")) return ToolRequest("vehicle.get_snapshot")
        if (normalized in setOf("pon musica", "reproduce musica", "play")) return ToolRequest("media.play")
        if (normalized in setOf("pausa", "pausa la musica", "pausa spotify")) return ToolRequest("media.pause")
        if (normalized in setOf("siguiente cancion", "siguiente pista")) return ToolRequest("media.next")
        if (normalized in setOf("cancion anterior", "pista anterior")) return ToolRequest("media.previous")
        if (normalized in setOf("que suena", "musica actual")) return ToolRequest("media.get_current")
        Regex("^(?:ll[eé]vame a|navega a) (.+)$", RegexOption.IGNORE_CASE).matchEntire(original)?.let {
            return ToolRequest("navigation.open_destination", mapOf("destination" to it.groupValues[1], "provider" to "maps"))
        }
        Regex("^waze (.+)$", RegexOption.IGNORE_CASE).matchEntire(original)?.let {
            return ToolRequest("navigation.open_destination", mapOf("destination" to it.groupValues[1], "provider" to "waze"))
        }
        Regex("^(?:llama a|busca contacto) (.+)$", RegexOption.IGNORE_CASE).matchEntire(original)?.let {
            return ToolRequest("contacts.search", mapOf("name" to it.groupValues[1]))
        }
        if (normalized in setOf("restaurantes cercanos", "busca restaurantes cercanos")) return ToolRequest("places.search_nearby", mapOf("query" to "restaurantes"))
        return null
    }
}

class AssistantOrchestrator(
    private val tools: ToolRegistry,
    private val clock: Clock,
    private val ai: AIProvider? = null,
    private val resolver: IntentResolver = IntentResolver(),
) {
    private data class Pending(val contacts: List<Contact>, val expiresAt: Long)
    private var pending: Pending? = null
    fun cancelPending() { pending = null }

    suspend fun answer(request: AssistantRequest): ToolResult {
        if (request.userText.length > 400 || request.userText.isBlank()) return ToolResult.denied("assistant", "INVALID_INPUT", "Di un comando breve.")
        val normalized = normalize(request.userText).removePrefix("jarvis ").removePrefix("jarvis, ")
        if (normalized in setOf("cancelar", "cancela", "no")) {
            pending = null
            return ToolResult("assistant", ExecutionStatus.SUCCESS, "Acción cancelada.")
        }
        val choice = pending
        if (normalized in setOf("si", "confirmar", "confirmo")) {
            pending = null // Consume before any suspension; no duplicate dial.
            if (choice == null || clock.now() > choice.expiresAt || choice.contacts.size != 1) {
                return ToolResult.denied("phone.open_dialer", "NO_CONFIRMATION", "No hay un número único pendiente de confirmación.")
            }
            val number = choice.contacts.single().number
            return tools.execute(ToolRequest("phone.open_dialer", mapOf("number" to number)), confirmedNumber = number)
        }
        val selection = mapOf("uno" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5)[normalized] ?: normalized.toIntOrNull()
        if (selection != null && choice != null && clock.now() <= choice.expiresAt) {
            val contact = choice.contacts.getOrNull(selection - 1)
            if (contact != null) {
                pending = Pending(listOf(contact), clock.now() + 30_000)
                return promptConfirmation(contact)
            }
        }
        pending = null
        val resolved = resolver.resolve(request.userText)
        val proposed = resolved ?: withTimeoutOrNull(5_000) { ai?.propose(request.copy(contextReferences = emptySet())) }
        if (proposed == null) return ToolResult("assistant", ExecutionStatus.SUCCESS,
            "Prueba: cuánta gasolina queda, temperatura, pausa la música o llévame a un destino. IA externa no configurada.")
        val result = tools.execute(proposed)
        // Only a direct user contact command can establish pending confirmation; AI output cannot.
        if (resolved?.toolId == "contacts.search" && result.contacts.isNotEmpty()) {
            pending = Pending(result.contacts, clock.now() + 30_000)
            if (result.contacts.size == 1) return promptConfirmation(result.contacts.single())
            return result.copy(message = "Hay varios contactos. Di el número: " + result.contacts.mapIndexed { i, c -> "${i + 1}, ${c.name}, termina en ${c.number.takeLast(3)}" }.joinToString(". "))
        }
        return result
    }

    private fun promptConfirmation(contact: Contact) = ToolResult("phone.open_dialer", ExecutionStatus.CONFIRMATION_REQUIRED,
        "¿Abrir el marcador para ${contact.name}, ${contact.number}? Di confirmar o cancelar.")
}
