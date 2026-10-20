# Contracts

Pure core implementation: `PerformanceReadiness.kt`. QualificationEnvironment,
EvidenceLevel, DeviceQualification and CompatibilityStatus reuse Sprint 7.

| Contract | Required semantics |
| --- | --- |
| PerformanceScenario | Closed journey identifier and explicit preconditions |
| BenchmarkEnvironment | Exact commit/APK, qualification environment, variant, compilation, display conditions |
| BenchmarkSample | Iteration, nonnegative finite value, explicit unit and measurement kind |
| StartupMeasurement | Start mode, TTID, nullable TTFD, separate external UI bound |
| FrameTimingMeasurement | CPU durations; nullable overrun (requires API 31); sampled frame count |
| MemoryMeasurement | PSS KiB and elapsed monotonic observation time |
| StabilityScenario | Closed local scenario, repetition count and restoration requirement |
| RegressionThreshold | Minimum 20 comparable samples, allowed relative change; advisory |
| PhysicalTestRecord | Reuses DeviceQualification; verified physical result requires human observation |
| ReleaseCandidateReadiness | Per-check state; missing checks stay pending; physical status is independently derived |

No timings, unavailable readings or unexecuted scenarios default to zero or PASS.
Instrumented metric, external upper bound, functional assertion and human observation
are distinct measurement kinds. Durations use monotonic time; timestamps use UTC.
No serial, credential, conversation, media title, contact or coordinate enters reports.
