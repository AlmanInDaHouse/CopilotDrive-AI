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
    private fun MacrobenchmarkScope.waitDashboard() {
        check(device.wait(Until.hasObject(By.res("dashboard")), 15_000)) { "DASHBOARD_NOT_READY" }
        check(device.wait(Until.hasObject(By.res("push-to-talk")), 5_000)) { "CONTROLS_NOT_READY" }
        check(device.wait(Until.hasObject(By.res("dashboard-ready")), 10_000)) { "PERSISTED_STATE_NOT_READY" }
        device.waitForIdle()
    }
    private fun MacrobenchmarkScope.prepare() {
        startActivityAndWait()
        val accept = device.wait(Until.findObject(By.res("accept-onboarding")), 2_000)
        accept?.click()
        waitDashboard()
        pressHome()
    }
    private fun startup(mode: StartupMode) = benchmark.measureRepeated(
        packageName = target, metrics = listOf(StartupTimingMetric()), iterations = iterations,
        compilationMode = CompilationMode.None(), startupMode = mode,
        setupBlock = { prepare() },
    ) { startActivityAndWait(); waitDashboard() }
    @Test fun coldStartup() = startup(StartupMode.COLD)
    @Test fun warmStartup() = startup(StartupMode.WARM)
    @Test fun hotStartup() = startup(StartupMode.HOT)

    private fun journey(action: MacrobenchmarkScope.() -> Unit) = benchmark.measureRepeated(
        packageName = target, metrics = listOf(FrameTimingMetric()), iterations = iterations,
        compilationMode = CompilationMode.None(), setupBlock = { prepare(); startActivityAndWait(); waitDashboard() },
        measureBlock = action,
    )
    @Test fun dashboardScroll() = journey {
        val dashboard = device.findObject(By.res("dashboard"))
        dashboard.setGestureMargin(device.displayWidth / 5)
        dashboard.swipe(Direction.UP, .7f); dashboard.swipe(Direction.DOWN, .7f)
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
        checkNotNull(device.wait(Until.findObject(By.res("layout-laboratory")), 5_000)).click()
        device.findObject(By.res("confirm-layout-laboratory")).click()
        checkNotNull(device.wait(Until.findObject(By.res("preset-MEDIA")), 5_000)).click()
        device.pressBack(); waitDashboard() // Draft discarded; no persistent layout mutation.
    }
}
