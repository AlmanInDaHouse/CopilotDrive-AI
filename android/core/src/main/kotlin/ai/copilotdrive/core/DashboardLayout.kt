package ai.copilotdrive.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable enum class WidgetType { VEHICLE, MEDIA, NAVIGATION, ASSISTANT, PLACES, OBD_STATUS, CLOCK, SYSTEM_STATUS }
@Serializable enum class WidgetSize(val columns: Int) { COMPACT(1), EXPANDED(2), WIDE(4) }
@Serializable enum class WidgetVisibility { VISIBLE, HIDDEN }
@Serializable enum class DashboardPreset { CLASSIC, DRIVER, MEDIA, LABORATORY }
enum class WidgetCapability { VEHICLE, MEDIA_SESSION, NAVIGATION, VOICE, PLACES, BLUETOOTH, DIAGNOSTICS, LOCAL_CLOCK }
@Serializable data class WidgetPlacement(val column: Int = 0, val row: Int = 0)
@Serializable data class WidgetInstance(
    val id: String, val type: WidgetType, val size: WidgetSize = WidgetSize.COMPACT,
    val placement: WidgetPlacement = WidgetPlacement(), val visibility: WidgetVisibility = WidgetVisibility.VISIBLE,
)
@Serializable data class DashboardLayout(
    val schemaVersion: Int = 1, val preset: DashboardPreset = DashboardPreset.CLASSIC,
    val widgets: List<WidgetInstance> = emptyList(),
)
data class LayoutValidationResult(val errors: List<String>) { val valid get() = errors.isEmpty() }
data class LayoutLoadResult(val layout: DashboardLayout, val notice: String? = null)
interface LayoutRepository {
    val state: Flow<LayoutLoadResult>
    suspend fun save(layout: DashboardLayout)
}

object WidgetRegistry {
    val definitions: List<WidgetDefinition> = WidgetType.entries.map { type ->
        val (title, capability) = when (type) {
            WidgetType.VEHICLE -> "Vehículo" to WidgetCapability.VEHICLE
            WidgetType.MEDIA -> "Multimedia" to WidgetCapability.MEDIA_SESSION
            WidgetType.NAVIGATION -> "Navegación" to WidgetCapability.NAVIGATION
            WidgetType.ASSISTANT -> "Jarvis" to WidgetCapability.VOICE
            WidgetType.PLACES -> "Lugares" to WidgetCapability.PLACES
            WidgetType.OBD_STATUS -> "Estado OBD" to WidgetCapability.BLUETOOTH
            WidgetType.CLOCK -> "Reloj" to WidgetCapability.LOCAL_CLOCK
            WidgetType.SYSTEM_STATUS -> "Sistema" to WidgetCapability.DIAGNOSTICS
        }
        WidgetDefinition(type.name.lowercase(), type, title, 1, 1, setOf(capability), "{}", emptySet(),
            if (type in setOf(WidgetType.VEHICLE, WidgetType.MEDIA, WidgetType.ASSISTANT, WidgetType.SYSTEM_STATUS)) WidgetSize.entries.toSet()
            else setOf(WidgetSize.COMPACT, WidgetSize.EXPANDED))
    }
    fun definition(type: WidgetType): WidgetDefinition = definitions.first { it.type == type }
}

object LayoutEngine {
    fun validate(layout: DashboardLayout, columns: Int = 4): LayoutValidationResult {
        val errors = mutableListOf<String>()
        if (columns !in setOf(1, 2, 4)) return LayoutValidationResult(listOf("Columnas inválidas"))
        if (layout.schemaVersion != 1) errors += "Versión no compatible"
        if (layout.widgets.size !in 1..8 || layout.widgets.none { it.visibility == WidgetVisibility.VISIBLE }) errors += "Debe quedar alguna tarjeta visible"
        if (layout.widgets.map { it.id }.distinct().size != layout.widgets.size) errors += "ID duplicado"
        if (layout.widgets.map { it.type }.distinct().size != layout.widgets.size) errors += "Tipo duplicado"
        val occupied = mutableSetOf<Pair<Int, Int>>()
        layout.widgets.take(9).forEach { widget ->
            if (!widget.id.matches(Regex("[a-z0-9_-]{1,64}"))) errors += "ID inválido"
            if (widget.size !in WidgetRegistry.definition(widget.type).sizes) errors += "Tamaño no compatible"
            val p = widget.placement
            val span = widget.size.columns
            if (p.column < 0 || p.row !in 0..31 || p.column > columns - span) errors += "Posición fuera de límites"
            else for (x in p.column until p.column + span) if (!occupied.add(x to p.row)) errors += "Tarjetas superpuestas"
        }
        return LayoutValidationResult(errors.distinct())
    }

    /** Stable first-fit, bounded by eight native widgets. No physical pixel assumptions. */
    fun pack(widgets: List<WidgetInstance>, columns: Int = 4): List<WidgetInstance> {
        require(columns in setOf(1, 2, 4)); require(widgets.size <= 8)
        val occupied = mutableSetOf<Pair<Int, Int>>()
        return widgets.map { widget ->
            val size = if (widget.size.columns <= columns) widget.size else
                WidgetRegistry.definition(widget.type).sizes.filter { it.columns <= columns }.maxBy { it.columns }
            val position = (0..31).asSequence().flatMap { row -> (0..columns - size.columns).asSequence().map { WidgetPlacement(it, row) } }
                .first { p -> (p.column until p.column + size.columns).none { (it to p.row) in occupied } }
            for (x in position.column until position.column + size.columns) occupied.add(x to position.row)
            widget.copy(size = size, placement = position)
        }
    }
    fun project(layout: DashboardLayout, columns: Int): List<WidgetInstance> = pack(
        layout.widgets.filter { it.visibility == WidgetVisibility.VISIBLE }.map { widget ->
            val span = (widget.size.columns * columns + 3) / 4
            widget.copy(size = WidgetRegistry.definition(widget.type).sizes.filter { it.columns <= span }.maxBy { it.columns })
        }, columns)
}

object WidgetLayouts {
    fun preset(preset: DashboardPreset): DashboardLayout {
        val types = when (preset) {
            DashboardPreset.CLASSIC -> listOf(WidgetType.VEHICLE, WidgetType.MEDIA, WidgetType.NAVIGATION, WidgetType.ASSISTANT)
            DashboardPreset.DRIVER -> listOf(WidgetType.VEHICLE, WidgetType.NAVIGATION, WidgetType.CLOCK)
            DashboardPreset.MEDIA -> listOf(WidgetType.MEDIA, WidgetType.NAVIGATION, WidgetType.ASSISTANT)
            DashboardPreset.LABORATORY -> listOf(WidgetType.OBD_STATUS, WidgetType.SYSTEM_STATUS, WidgetType.VEHICLE, WidgetType.ASSISTANT)
        }
        return DashboardLayout(preset = preset, widgets = LayoutEngine.pack(types.mapIndexed { index, type ->
            WidgetInstance(WidgetRegistry.definition(type).id, type,
                when {
                    preset == DashboardPreset.CLASSIC && type == WidgetType.ASSISTANT -> WidgetSize.WIDE
                    preset == DashboardPreset.CLASSIC && type != WidgetType.VEHICLE -> WidgetSize.COMPACT
                    index == 0 && preset in setOf(DashboardPreset.DRIVER, DashboardPreset.MEDIA) -> WidgetSize.WIDE
                    else -> WidgetSize.EXPANDED
                })
        }))
    }
}

object LayoutCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }
    fun encode(layout: DashboardLayout): String {
        require(LayoutEngine.validate(layout).valid)
        return json.encodeToString(DashboardLayout.serializer(), layout)
    }
    fun decode(raw: String?): LayoutLoadResult {
        fun fallback(message: String) = LayoutLoadResult(WidgetLayouts.preset(DashboardPreset.CLASSIC), message)
        if (raw == null) return fallback("Diseño Classic inicial · referencia Sprint 5")
        if (raw.length > 32_768) return fallback("Diseño demasiado grande; restaurado Classic")
        return try {
            val element = json.parseToJsonElement(raw).jsonObject
            val version = element["schemaVersion"]?.jsonPrimitive?.intOrNull
            require(version in 0..1) { "Versión desconocida" }
            val migrated = if (version == 0) JsonObject(element + ("schemaVersion" to JsonPrimitive(1)) +
                ("preset" to (element["preset"] ?: JsonPrimitive("CLASSIC")))) else element
            val layout = json.decodeFromJsonElement(DashboardLayout.serializer(), migrated)
            require(LayoutEngine.validate(layout).valid)
            LayoutLoadResult(layout, if (version == 0) "Diseño migrado de v0 a v1" else null)
        } catch (_: IllegalArgumentException) { fallback("Diseño inválido; restaurado Classic") }
    }
}

data class DashboardEditSession(val original: DashboardLayout, val draft: DashboardLayout = original) {
    private fun replace(widgets: List<WidgetInstance>): DashboardEditSession {
        val candidate = draft.copy(widgets = LayoutEngine.pack(widgets))
        return if (LayoutEngine.validate(candidate).valid) copy(draft = candidate) else this
    }
    fun add(type: WidgetType): DashboardEditSession {
        val existing = draft.widgets.firstOrNull { it.type == type }
        return if (existing != null) replace(draft.widgets.map { if (it.type == type) it.copy(visibility = WidgetVisibility.VISIBLE) else it })
        else replace(draft.widgets + WidgetInstance(WidgetRegistry.definition(type).id, type))
    }
    fun hide(id: String) = replace(draft.widgets.map { if (it.id == id) it.copy(visibility = WidgetVisibility.HIDDEN) else it })
    fun resize(id: String, size: WidgetSize) = replace(draft.widgets.map { if (it.id == id) it.copy(size = size) else it })
    fun move(id: String, offset: Int): DashboardEditSession {
        val list = draft.widgets.toMutableList(); val index = list.indexOfFirst { it.id == id }
        if (index < 0) return this
        val target = (index + offset.coerceIn(-1, 1)).coerceIn(0, list.lastIndex)
        val item = list.removeAt(index); list.add(target, item)
        return replace(list)
    }
    fun preset(preset: DashboardPreset) = copy(draft = WidgetLayouts.preset(preset))
    fun restore() = preset(DashboardPreset.CLASSIC)
}

/** Positive speed is evidence of movement. Zero/simulation/stale/disconnected is never proof of parking. */
fun layoutMotion(vehicle: VehicleSnapshot?, now: Long): MotionState {
    val speed = vehicle?.speedKmh ?: return MotionState.UNKNOWN
    return if (vehicle.connectionStatus == ConnectionStatus.CONNECTED && speed.at(now).availability in
        setOf(Availability.AVAILABLE, Availability.SIMULATED) && (speed.value ?: 0.0) > 0.0) MotionState.MOVING else MotionState.UNKNOWN
}

data class DashboardCustomizationState(
    val layout: DashboardLayout = WidgetLayouts.preset(DashboardPreset.CLASSIC),
    val loaded: Boolean = false, val edit: DashboardEditSession? = null, val laboratory: Boolean = false,
    val motion: MotionState = MotionState.UNKNOWN, val saving: Boolean = false, val error: String? = null,
)

/** Called on the owner dispatcher, separately from telemetry and without any tool/provider dependencies. */
class DashboardCustomization(private val repository: LayoutRepository, private val clock: Clock, @Suppress("UNUSED_PARAMETER") scope: CoroutineScope) {
    private val mutable = MutableStateFlow(DashboardCustomizationState())
    val state = mutable.asStateFlow()
    private var confirmedAt: Long? = null
    private var inForeground = true
    private var activeWrite: Job? = null
    private fun safe(): Boolean {
        val at = confirmedAt ?: return false
        return inForeground && mutable.value.laboratory && mutable.value.motion != MotionState.MOVING && clock.now() >= at && clock.now() - at <= 120_000
    }
    fun load(result: LayoutLoadResult) {
        mutable.value = mutable.value.copy(layout = result.layout, loaded = true, error = result.notice)
    }
    fun confirmLaboratory(confirmed: Boolean) {
        confirmedAt = if (confirmed && inForeground && mutable.value.motion != MotionState.MOVING) clock.now() else null
        if (confirmedAt == null) activeWrite?.cancel()
        mutable.value = mutable.value.copy(laboratory = confirmedAt != null, edit = if (confirmedAt == null) null else mutable.value.edit)
    }
    fun motion(motion: MotionState) { mutable.value = mutable.value.copy(motion = motion); tick() }
    fun tick() { if (!safe()) { activeWrite?.cancel(); confirmedAt = null; mutable.value = mutable.value.copy(edit = null, laboratory = false) } }
    fun begin(): Boolean {
        tick()
        if (!safe() || !mutable.value.loaded || mutable.value.saving) return false
        mutable.value = mutable.value.copy(edit = DashboardEditSession(mutable.value.layout), error = null)
        return true
    }
    fun change(operation: DashboardEditSession.() -> DashboardEditSession) {
        tick(); if (mutable.value.saving) return
        mutable.value = mutable.value.copy(edit = mutable.value.edit?.operation(), error = null)
    }
    fun cancel() { mutable.value = mutable.value.copy(edit = null, error = null) }
    fun foreground() { inForeground = true }
    fun background() { inForeground = false; confirmLaboratory(false); activeWrite?.cancel() }
    suspend fun save() {
        tick(); val edit = mutable.value.edit ?: return
        if (!safe() || mutable.value.saving || !LayoutEngine.validate(edit.draft).valid) return
        mutable.value = mutable.value.copy(saving = true, error = null)
        activeWrite = currentCoroutineContext()[Job]
        try {
            repository.save(edit.draft)
            mutable.value = mutable.value.copy(layout = edit.draft, edit = null)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: java.io.IOException) { mutable.value = mutable.value.copy(error = "No se pudo guardar el diseño. Reintenta o cancela.") }
        finally { activeWrite = null; mutable.value = mutable.value.copy(saving = false) }
    }
}
