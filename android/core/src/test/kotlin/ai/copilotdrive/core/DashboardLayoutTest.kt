package ai.copilotdrive.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class DashboardLayoutTest {
    private val classic = WidgetLayouts.preset(DashboardPreset.CLASSIC)
    private fun invalid(layout: DashboardLayout) = assertFalse(LayoutEngine.validate(layout).valid)

    @Test fun presetsAndClosedRegistry() {
        assertEquals(8, WidgetRegistry.definitions.size)
        DashboardPreset.entries.forEach { assertTrue(LayoutEngine.validate(WidgetLayouts.preset(it)).valid) }
    }
    @Test fun emptyAndAllHiddenAreUnrecoverable() {
        invalid(classic.copy(widgets = emptyList()))
        invalid(classic.copy(widgets = classic.widgets.map { it.copy(visibility = WidgetVisibility.HIDDEN) }))
    }
    @Test fun duplicateTypeAndIdRejected() {
        invalid(classic.copy(widgets = classic.widgets + classic.widgets.first()))
        invalid(classic.copy(widgets = classic.widgets + classic.widgets.first().copy(id = "another")))
    }
    @Test fun overlapsNegativeAndOutOfBoundsRejected() {
        invalid(classic.copy(widgets = classic.widgets.map { it.copy(placement = WidgetPlacement(0, 0)) }))
        listOf(WidgetPlacement(-1, 0), WidgetPlacement(4, 0), WidgetPlacement(0, 32)).forEach { p ->
            invalid(classic.copy(widgets = classic.widgets.mapIndexed { i, w -> if (i == 0) w.copy(placement = p) else w }))
        }
    }
    @Test fun unsupportedSizeRejected() {
        val clock = WidgetInstance("clock", WidgetType.CLOCK, WidgetSize.WIDE)
        invalid(DashboardLayout(widgets = listOf(clock)))
    }
    @Test fun corruptUnknownFutureAndMissingVersionRecover() {
        val raw = LayoutCodec.encode(classic)
        listOf("{", "", raw.replace("VEHICLE", "REMOTE_APP"), raw.replace("\"schemaVersion\":1", "\"schemaVersion\":99"),
            raw.replace("\"schemaVersion\":1,", "")).forEach {
            val loaded = LayoutCodec.decode(it)
            assertNotNull(loaded.notice); assertEquals(classic, loaded.layout)
        }
    }
    @Test fun roundTripMigrationAndMissingPreferences() {
        assertEquals(classic, LayoutCodec.decode(LayoutCodec.encode(classic)).layout)
        val v0 = LayoutCodec.encode(classic).replace("\"schemaVersion\":1", "\"schemaVersion\":0").replace("\"preset\":\"CLASSIC\",", "")
        assertEquals(classic, LayoutCodec.decode(v0).layout)
        assertNotNull(LayoutCodec.decode(v0).notice)
        assertEquals(classic, LayoutCodec.decode(null).layout)
    }
    @Test fun deterministicPackingAndResponsiveProjectionDoNotMutateDocument() {
        val original = LayoutCodec.encode(classic)
        for (columns in listOf(1, 2, 4)) {
            val projection = LayoutEngine.project(classic, columns)
            assertEquals(projection, LayoutEngine.project(classic, columns))
            assertTrue(LayoutEngine.validate(classic.copy(widgets = projection), columns).valid)
        }
        assertEquals(original, LayoutCodec.encode(classic))
    }
    @Test fun editAddHideMoveResizePresetCancelAndRestore() {
        var session = DashboardEditSession(classic)
        session = session.add(WidgetType.CLOCK).add(WidgetType.CLOCK)
        assertEquals(1, session.draft.widgets.count { it.type == WidgetType.CLOCK })
        session = session.move("clock", -1).resize("vehicle", WidgetSize.WIDE).hide("media")
        assertTrue(LayoutEngine.validate(session.draft).valid)
        assertEquals(WidgetVisibility.HIDDEN, session.draft.widgets.first { it.id == "media" }.visibility)
        assertEquals(classic, session.original)
        assertEquals(WidgetLayouts.preset(DashboardPreset.MEDIA), session.preset(DashboardPreset.MEDIA).draft)
        assertEquals(classic, session.restore().draft)
    }
    @Test fun lastVisibleCannotBeHidden() {
        val one = DashboardLayout(widgets = listOf(WidgetInstance("clock", WidgetType.CLOCK)))
        assertEquals(one, DashboardEditSession(one).hide("clock").draft)
    }
    @Test fun unavailableCapabilitiesDoNotChangeLayoutOrGrantPermission() {
        val w = WidgetRegistry.definition(WidgetType.MEDIA)
        assertTrue(WidgetCapability.MEDIA_SESSION in w.capabilities)
        assertTrue(LayoutEngine.validate(classic).valid)
        assertEquals(Availability.SIMULATED, simulatedVehicle(10).speedKmh.availability)
        assertEquals(MotionState.UNKNOWN, layoutMotion(simulatedVehicle(10), 10))
        assertEquals(MotionState.UNKNOWN, layoutMotion(null, 10))
        val real = simulatedVehicle(10).copy(simulationMode = false, source = "elm", speedKmh = Metric(12.0,"km/h","elm",10,Availability.AVAILABLE),
            engineRpm = Metric(null,"rpm","elm",10,Availability.UNAVAILABLE), fuelLevelPercent = Metric(null,"%","elm",10,Availability.UNAVAILABLE),
            coolantTemperatureC = Metric(null,"°C","elm",10,Availability.UNAVAILABLE), batteryVoltage = Metric(null,"V","elm",10,Availability.UNAVAILABLE),
            vehicleRangeKm = Metric(null,"km","elm",10,Availability.UNAVAILABLE))
        assertEquals(MotionState.MOVING, layoutMotion(real, 10))
        assertEquals(MotionState.UNKNOWN, layoutMotion(real, 20_000))
        assertEquals(MotionState.UNKNOWN, layoutMotion(real.copy(connectionStatus = ConnectionStatus.DISCONNECTED), 10))
    }
    private class Repository : LayoutRepository {
        override val state = MutableStateFlow(LayoutLoadResult(WidgetLayouts.preset(DashboardPreset.CLASSIC)))
        var writes = 0
        var failure: Exception? = null
        override suspend fun save(layout: DashboardLayout) { failure?.let { throw it }; writes++; state.value = LayoutLoadResult(layout) }
    }
    @Test fun safeLabUnknownMotionAndRevocation() = runTest {
        var now = 0L
        val controller = DashboardCustomization(Repository(), Clock { now }, backgroundScope)
        controller.load(LayoutLoadResult(classic))
        assertFalse(controller.begin())
        controller.confirmLaboratory(true)
        assertTrue(controller.begin())
        controller.motion(MotionState.MOVING)
        assertNull(controller.state.value.edit); assertFalse(controller.begin())
        controller.motion(MotionState.UNKNOWN); controller.confirmLaboratory(true); assertTrue(controller.begin())
        controller.confirmLaboratory(false); assertNull(controller.state.value.edit)
        controller.confirmLaboratory(true); controller.begin(); controller.background()
        assertNull(controller.state.value.edit); assertFalse(controller.begin())
        controller.foreground(); controller.confirmLaboratory(true); controller.begin(); now = 120_001
        controller.tick(); assertNull(controller.state.value.edit)
    }
    @Test fun saveReloadCancelRestoreAndIoFailure() = runTest {
        val repository = Repository()
        val controller = DashboardCustomization(repository, Clock { 0 }, backgroundScope)
        controller.load(repository.state.value); controller.confirmLaboratory(true); controller.begin()
        controller.change { add(WidgetType.CLOCK) }; controller.cancel()
        assertEquals(0, repository.writes); assertEquals(classic, controller.state.value.layout)
        controller.begin(); controller.change { preset(DashboardPreset.MEDIA) }
        repository.failure = IOException("disk full"); controller.save()
        assertEquals(classic, controller.state.value.layout); assertNotNull(controller.state.value.edit)
        assertNotNull(controller.state.value.error)
        repository.failure = null; controller.save()
        assertEquals(1, repository.writes)
        val restarted = DashboardCustomization(repository, Clock { 0 }, backgroundScope)
        restarted.load(repository.state.value)
        assertEquals(DashboardPreset.MEDIA, restarted.state.value.layout.preset)
        restarted.confirmLaboratory(true); restarted.begin(); restarted.change { restore() }; restarted.save()
        assertEquals(classic, restarted.state.value.layout)
    }
    @Test fun cancellationPropagatesAndDoesNotSave() = runTest {
        val repository = Repository().apply { failure = CancellationException() }
        val controller = DashboardCustomization(repository, Clock { 0 }, backgroundScope)
        controller.load(repository.state.value); controller.confirmLaboratory(true); controller.begin()
        try { controller.save(); fail("cancellation swallowed") } catch (_: CancellationException) { }
        assertEquals(0, repository.writes); assertFalse(controller.state.value.saving)
    }
}
