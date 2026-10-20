package ai.copilotdrive.core

import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

fun normalize(text: String) = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").trim().trim('¿', '?', '.', '¡', '!')

class IntentResolver {
    fun resolve(text: String): ToolRequest? {
        val original = text.trim().replace(Regex("^jarvis[, ]*", RegexOption.IGNORE_CASE), "")
        val normalized = normalize(original)
        if (normalized.matches(Regex("(cuanta gasolina( me)? (queda|tengo)|cuanto combustible( me)? (queda|tengo)|combustible|gasolina|nivel de combustible)"))) return ToolRequest("vehicle.get_fuel")
        if (normalized in setOf("temperatura", "temperatura del motor", "refrigerante")) return ToolRequest("vehicle.get_temperature")
        if (normalized in setOf("estado del coche", "como esta el coche", "estado del vehiculo")) return ToolRequest("vehicle.get_snapshot")
        if (normalized in setOf("pon musica", "reproduce musica", "play")) return ToolRequest("media.play")
        if (normalized in setOf("pausa", "pausa la musica")) return ToolRequest("media.pause")
        if (normalized == "pausa spotify") return ToolRequest("media.pause", mapOf("sourceApp" to "com.spotify.music"))
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
        Regex("^(?:busca|buscar|muestrame) (restaurantes|gasolineras|aparcamientos|cafeterias|cafes|puntos de interes)(?: cercanos| cerca)?$")
            .matchEntire(normalized)?.let { return ToolRequest("places.search_nearby", mapOf("query" to it.groupValues[1])) }
        if (normalized == "restaurantes cercanos") return ToolRequest("places.search_nearby", mapOf("query" to "restaurantes"))
        return null
    }
}

class AssistantOrchestrator(
    private val tools: ToolRegistry,
    private val clock: Clock,
    private val ai: AIProvider? = null,
    private val resolver: IntentResolver = IntentResolver(),
    private val conversation: ConversationStore = EphemeralConversationStore(),
) {
    private data class Pending(val contacts: List<Contact>, val expiresAt: Long)
    private var pending: Pending? = null
    private var pendingPlaceId: String? = null
    fun cancelPending() { pending = null; pendingPlaceId = null }
    fun clearConversation(sessionId: String) { conversation.clear(sessionId); cancelPending() }
    fun clearAllConversations() { conversation.clearAll(); cancelPending() }

    suspend fun answer(request: AssistantRequest): ToolResult {
        if (request.userText.length > 400 || request.userText.isBlank()) return ToolResult.denied("assistant", "INVALID_INPUT", "Di un comando breve.")
        val normalized = normalize(request.userText).removePrefix("jarvis ").removePrefix("jarvis, ")
        if (normalized in setOf("cancelar", "cancela", "no")) {
            cancelPending()
            return ToolResult("assistant", ExecutionStatus.SUCCESS, "Acción cancelada.")
        }
        if (normalized in setOf("si", "confirmar", "confirmo") && pendingPlaceId != null) {
            val id = pendingPlaceId!!
            pendingPlaceId = null
            if (tools.selectedPlaceId() != id)
                return ToolResult.denied("navigation.open_selected_place", "SELECTION_MISSING", "El resultado ya no está disponible.")
            return tools.execute(ToolRequest("navigation.open_selected_place", mapOf("provider" to "maps")))
        }
        if (normalized.contains("cuanto tardaria") || normalized.contains("tiempo de llegada"))
            return ToolResult("assistant", ExecutionStatus.SUCCESS, "No dispongo de una estimación de llegada verificable. Maps o Waze pueden calcularla al abrir el destino.")
        if (normalized.contains("mejor valorados")) {
            val rated = tools.placeResults().filter { it.rating != null }.sortedByDescending { it.rating }.take(3)
            return ToolResult("places.get_selection", ExecutionStatus.SUCCESS, if (rated.isEmpty())
                "No hay valoraciones suficientes en los resultados actuales." else rated.mapIndexed { index, place ->
                    "${index + 1}. ${place.name}: ${place.rating}/5${place.ratingCount?.let { " ($it reseñas)" } ?: ""}"
                }.joinToString(". "))
        }
        Regex("^(?:llevame al|navega al) (primero|segundo|tercero|1|2|3)$").matchEntire(normalized)?.let { match ->
            val index = mapOf("primero" to 0, "segundo" to 1, "tercero" to 2, "1" to 0, "2" to 1, "3" to 2)[match.groupValues[1]]!!
            val place = tools.placeResults().getOrNull(index)
                ?: return ToolResult.denied("places.select_result", "SELECTION_MISSING", "No hay un resultado vigente con ese número.")
            val selected = tools.execute(ToolRequest("places.select_result", mapOf("id" to place.providerId!!)))
            if (selected.executionStatus != ExecutionStatus.SUCCESS) return selected
            pendingPlaceId = place.providerId
            return ToolResult("navigation.open_selected_place", ExecutionStatus.CONFIRMATION_REQUIRED,
                "¿Abrir Google Maps con ${place.name}? Di confirmar o cancelar.")
        }
        pendingPlaceId = null
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
        // Range cannot be inferred from fuel percentage. Resolve it against real data first.
        if (normalized.contains("autonomia") || normalized.contains("kilometros puedo recorrer")) {
            val value = tools.execute(ToolRequest("vehicle.get_range"))
            return value
        }
        if (resolved == null && ai == null) return ToolResult("assistant", ExecutionStatus.SUCCESS,
            "Prueba: cuánta gasolina queda, temperatura, pausa la música o llévame a un destino. IA externa no configurada.")
        val response = if (resolved == null) try {
            withTimeout(31_000) {
                ai?.generate(AIRequest(request.userText, conversation.history(request.sessionId), tools.definitions.values
                    .filter { it.id !in setOf("contacts.search", "phone.open_dialer", "places.search_nearby", "places.select_result", "navigation.open_selected_place") }, request.locale))
            }
        } catch (_: TimeoutCancellationException) {
            AIResponse.Unavailable(AIError("TIMEOUT", "La IA ha agotado el tiempo de espera."))
        } else null
        if (response is AIResponse.Text) {
            val actionClaim = Regex("\\b(he|hemos|ya)\\s+(pausado|abierto|llamado|reproducido|apagado|encendido|ejecutado|cambiado)\\b|\\b(acción|operación)\\s+(completada|realizada)\\b", RegexOption.IGNORE_CASE)
            if (actionClaim.containsMatchIn(response.content)) return ToolResult.denied("assistant", "UNVERIFIED_ACTION", "La IA afirmó una acción que no se ha verificado.")
            return remember(request, ToolResult("assistant", ExecutionStatus.SUCCESS,
                "IA externa (sin acciones ejecutadas): " + response.content.take(1_400)))
        }
        if (response is AIResponse.Unavailable) return ToolResult("assistant", ExecutionStatus.FAILED, response.error.message, response.error.code)
        if (response is AIResponse.Invalid) return ToolResult.denied("assistant", response.error.code, response.error.message)
        val proposed = resolved ?: (response as? AIResponse.ProposedTool)?.proposal?.asRequest()
        if (proposed == null) return ToolResult("assistant", ExecutionStatus.FAILED, "IA no disponible.", "AI_UNAVAILABLE")
        if (resolved == null && proposed.toolId in setOf("contacts.search", "phone.open_dialer", "places.search_nearby", "places.select_result", "navigation.open_selected_place"))
            return ToolResult.denied(proposed.toolId, "AI_TOOL_DENIED", "Esa acción requiere una petición directa.")
        val result = tools.execute(proposed)
        // Only a direct user contact command can establish pending confirmation; AI output cannot.
        if (resolved?.toolId == "contacts.search" && result.contacts.isNotEmpty()) {
            pending = Pending(result.contacts, clock.now() + 30_000)
            if (result.contacts.size == 1) return promptConfirmation(result.contacts.single())
            return result.copy(message = "Hay varios contactos. Di el número: " + result.contacts.mapIndexed { i, c -> "${i + 1}, ${c.name}, termina en ${c.number.takeLast(3)}" }.joinToString(". "))
        }
        // Tool output can contain telemetry or contact data; never add it to remote history.
        return result
    }

    private fun remember(request: AssistantRequest, result: ToolResult): ToolResult {
        if (result.executionStatus == ExecutionStatus.SUCCESS) {
            conversation.append(request.sessionId, ConversationMessage(ConversationRole.USER, request.userText))
            conversation.append(request.sessionId, ConversationMessage(ConversationRole.ASSISTANT, result.message))
        }
        return result
    }

    private fun promptConfirmation(contact: Contact) = ToolResult("phone.open_dialer", ExecutionStatus.CONFIRMATION_REQUIRED,
        "¿Abrir el marcador para ${contact.name}, ${contact.number}? Di confirmar o cancelar.")
}
