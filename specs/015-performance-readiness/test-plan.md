# Validation plan

1. Verify PR #8 merge CI and fast-forward clean main; preserve user changes.
2. Baseline Python, core, harness, app unit/instrumentation, lint and APKs.
3. Capture 20 sequential external debug startup/UI-bound/PSS samples before changes.
   Keep interrupted or failed attempts, exclude them from successful comparisons.
4. Test readiness transitions, invalid metrics, provenance and threshold comparability.
5. Build opt-in Macrobenchmark; run cold/warm/hot startup, scroll/settings/lab/preset
   journeys with StartupTimingMetric/FrameTimingMetric. Record emulator suppression
   explicitly; never suppress production privacy/security checks.
6. Repeat local queries, TTS/OBD/media fake failures in harness; emulator UI lifecycle,
   preset drafts, lab, Places without provider, rotation and permission recovery.
7. Compare identical external debug runs after minimal optimization. Record no change
   or regression without claiming causality from noisy host/emulator observations.
8. Run all regressions, 11 display cases, signature/hash and repository policy checks.
9. Document manual physical/paid prerequisites and exact pending scenarios.
