# Contracts

Pure Kotlin contracts live in PhysicalPilot.kt; no new evidence enum.

| Contract | Semantics |
| --- | --- |
| TestDeviceProfile | Alias, existing environment and minSdk compatibility; no serial |
| PhysicalPilotSession | Exact commit/APK/device, session identifier, start timestamp |
| HumanObservation | Session/scenario/attempt, outcome, timestamp, redacted record reference |
| AcousticConfirmation | Callback and independent human outcome, bound to one attempt |
| VoiceQualificationResult | Existing DeviceQualification plus session-bound acoustic observation |
| IntegrationQualification | Existing DeviceQualification, physical effect requires human record |
| PerformanceEnvironment | Existing BenchmarkEnvironment and host/resource preconditions |
| PilotReadinessResult | Per-scenario qualifications; missing gates derive PENDING |

PHYSICALLY_VERIFIED requires a physical device, manual method, physical evidence,
positive human observation, matching session/build/scenario and executed timestamp.
The UI acoustic probe never creates a DeviceQualification by itself.
VoiceJourneyStage carries only closed stage identifiers. ToolRegistry emits requested,
policy-authorized or rejected; authorization is not observation of external effect.
