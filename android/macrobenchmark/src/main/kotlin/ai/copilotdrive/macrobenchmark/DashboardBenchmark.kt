package ai.copilotdrive.macrobenchmark

import androidx.benchmark.macro.*
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated applicationId: never overwrites the user's debug preferences or credentials. */
@RunWith(AndroidJUnit4::class)
class DashboardBenchmark {
    @get:Rule val benchmark = MacrobenchmarkRule()
    private val target = "ai.copilotdrive.app.benchmark"
    private val iterations get() = InstrumentationRegistry.getArguments().getString("iterations", "20").toInt().also { require(it in 1..100) }
    @OptIn(ExperimentalMacrobenchmarkApi::class)
    private val compilation: CompilationMode get() = when (InstrumentationRegistry.getArguments().getString("compilation", "None")) {
        "Ignore" -> CompilationMode.Ignore() // Never reset compilation or reinstall data on old APIs.
        "None" -> { check(android.os.Build.VERSION.SDK_INT >= 34) { "DESTRUCTIVE_RESET_ON_OLDER_API" }; CompilationMode.None() }
        else -> error("UNKNOWN_COMPILATION_STRATEGY")
    }
    private fun MacrobenchmarkScope.waitDashboard() {
        check(device.wait(Until.hasObject(By.res("dashboard")), 15_000)) { "DASHBOARD_NOT_READY" }
        check(device.wait(Until.hasObject(By.res("push-to-talk")), 5_000)) { "CONTROLS_NOT_READY" }
        check(device.wait(Until.hasObject(By.res("dashboard-ready")), 10_000)) { "PERSISTED_STATE_NOT_READY" }
        device.waitForIdle()
    }
    private fun MacrobenchmarkScope.prepare() {
        startActivityAndWait()
        var accept = device.wait(Until.findObject(By.res("accept-onboarding")), 2_000)
        // Fresh disposable AVDs may place the final consent button below the fold.
        // Setup stays outside the measured window; never infer consent from launch.
        val onboarding = device.findObject(By.res("onboarding"))
        if (onboarding != null && accept == null) {
            val bounds = onboarding.visibleBounds
            onboarding.setGestureMargin(minOf(bounds.width(), bounds.height()) / 10)
            repeat(8) {
                if (accept == null) {
                    onboarding.swipe(Direction.UP, .7f, 800)
                    device.waitForIdle()
                    accept = device.findObject(By.res("accept-onboarding"))
                }
            }
        }
        accept?.click()
        waitDashboard()
        pressHome()
    }
    private fun startup(mode: StartupMode) = benchmark.measureRepeated(
        packageName = target, metrics = listOf(StartupTimingMetric()), iterations = iterations,
        compilationMode = compilation, startupMode = mode,
        setupBlock = { prepare() },
    ) { startActivityAndWait(); waitDashboard() }
    @Test fun coldStartup() = startup(StartupMode.COLD)
    @Test fun warmStartup() = startup(StartupMode.WARM)
    @Test fun hotStartup() = startup(StartupMode.HOT)

    private fun journey(action: MacrobenchmarkScope.() -> Unit) = benchmark.measureRepeated(
        packageName = target, metrics = listOf(FrameTimingMetric()), iterations = iterations,
        compilationMode = compilation, setupBlock = { prepare(); startActivityAndWait(); waitDashboard() },
        measureBlock = action,
    )
    @Test fun dashboardScroll() = journey {
        val dashboard = device.findObject(By.res("dashboard"))
        check(dashboard.isScrollable) { "DASHBOARD_NOT_SCROLLABLE" }
        val bounds = dashboard.visibleBounds
        dashboard.setGestureMargin(minOf(bounds.width(), bounds.height()) / 10)
        val headerBefore = checkNotNull(device.findObject(By.res("edit-dashboard"))).visibleBounds.top
        dashboard.swipe(Direction.UP, .8f, 800)
        device.waitForIdle()
        val headerAfter = device.findObject(By.res("edit-dashboard"))?.visibleBounds?.top
        check(headerAfter == null || headerAfter < headerBefore) { "SCROLL_DID_NOT_MOVE" }
        dashboard.swipe(Direction.DOWN, .8f, 800)
        device.waitForIdle()
    }
    @Test fun settingsAndLab() = journey {
        device.findObject(By.text("Ajustes")).click()
        checkNotNull(device.wait(Until.findObject(By.text("Estoy en un entorno detenido")), 5_000)).click()
        checkNotNull(device.wait(Until.findObject(By.res("settings")), 5_000))
        var lab = device.findObject(By.res("open-device-lab"))
        repeat(8) {
            if (lab == null) {
                device.findObject(By.res("settings")).swipe(Direction.UP, .7f)
                lab = device.findObject(By.res("open-device-lab"))
            }
        }
        checkNotNull(lab).click()
        check(device.wait(Until.hasObject(By.res("device-lab")), 5_000))
        device.pressBack(); device.pressBack(); waitDashboard()
    }
    @Test fun backgroundRestore() = journey {
        pressHome(); startActivityAndWait(); waitDashboard()
    }
    @Test fun presetDraft() = journey {
        device.findObject(By.res("edit-dashboard")).click()
        // AlertDialog is a separate window: the Surface's resource-id semantics do not cross it.
        checkNotNull(device.wait(Until.findObject(By.desc("Activar modo laboratorio")), 5_000)).click()
        device.waitForIdle()
        checkNotNull(device.wait(Until.findObject(By.text("Confirmo entorno de prueba detenido")), 5_000)).click()
        val editor = checkNotNull(device.wait(Until.findObject(By.res("dashboard-editor")), 5_000))
        val bounds = editor.visibleBounds
        editor.setGestureMargin(minOf(bounds.width(), bounds.height()) / 10)
        var preset = device.findObject(By.res("preset-MEDIA"))
        repeat(6) {
            if (preset == null) {
                editor.swipe(Direction.UP, .7f, 800)
                device.waitForIdle()
                preset = device.findObject(By.res("preset-MEDIA"))
            }
        }
        checkNotNull(preset) { "PRESET_NOT_VISIBLE" }.click()
        device.waitForIdle()
        device.pressBack(); waitDashboard() // Draft discarded; no persistent layout mutation.
    }
}
