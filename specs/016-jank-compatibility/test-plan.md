# Test plan

1. Preserve clean-state provenance, fetch/fast-forward main; baseline Python and JVM,
   lint/debug/AndroidTest builds, then sequential direct instrumentation on safe target.
2. Analyze historical Sprint 8 traces with official Perfetto trace processor, restrict
   to measureBlock and package. Sample multiple iterations of all four journeys.
3. Capture host memory/commit/page input and emulator framework readiness before and
   after runs. No Gradle/install concurrent with measurements. Preserve failed attempts.
4. Repeat native FrameTimingMetric under identical display/compilation/iteration count;
   capture traces and JSON. Record absent metrics explicitly; do not infer zero jank.
5. Audit Compose/state/ticker/layout/animation/bitmap and adapters/lifecycle against
   profiles; regression tests for any justified fix. Max three first-iteration fixes.
6. Run bounded sustained lifecycle/navigation stress and distinguish instrumented PSS
   from fresh-process PSS. Heap dumps only if necessary, local synthetic data only.
7. Inventory official images and disk; install missing API26/30 only with download/
   license authorization. Independent AVDs, one booted at a time, safe compilation.
8. API-specific install/onboarding/dashboard/editor/persistence/Device Lab/permissions/
   absent voice/Bluetooth/MediaSession/offline Jarvis/Places/rotation/background tests.
9. Final functional suite, build/lint/signature/SDK/hash and offline smoke; package/data
   before/after, restoration verification, diff check and local commits.
