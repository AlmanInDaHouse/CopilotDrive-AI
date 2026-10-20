# Sprint 8: performance and technical test readiness

Scope: reproducible startup, dashboard interaction and stability measurements without
changing vehicle policy, requesting commercial services or claiming physical validation.
Reuse specs/014 qualification provenance and specs/013 diagnostic evidence levels.

TTID is the first rendered Activity frame measured by StartupTimingMetric. Dashboard
TTFD requires loaded voice preferences, completed onboarding, restored layout and a
first vehicle snapshot (including explicitly simulated or unavailable data). Compose
ReportDrawnWhen holds the signal until these conditions are rendered. Optional TTS,
AI, Places and hardware availability do not block a usable textual dashboard.
Onboarding runs are excluded from dashboard startup benchmarks.

The Sprint 7 `am start -W` duration and UIAutomator observation upper bound remain
separate external measurements; neither is relabelled TTFD. Debug and benchmark
variants, cold/warm/hot starts, API, resolution, compilation and sample count must be
recorded separately. No emulator result can imply PHYSICALLY_VERIFIED.

Benchmark module is opt-in, non-debuggable, debug-signed and release-derived. It
uses production network policy and no credentials. Functional CI remains mandatory;
no emulator performance budget becomes a CI gate. Raw samples stay ignored.

Stability runner requires explicit emulator target and opt-in, validates installed
APK hash, never clears app data, uses local deterministic scenarios and restores
temporary display/network overrides even on failure. Report crash/ANR observations,
PSS progression and recovery failures; a bounded run cannot prove absence of leaks.
