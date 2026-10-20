# CopilotDrive-AI

Conventional Android APK for open devices, not Android Auto, AAOS or CarPlay.
Keep the Python/web prototype functional. Android must work without Python or API keys.

## Architecture
`android/core` is pure Kotlin: contracts, policy, tools, orchestration, voice state machine.
`android/testing` contains deterministic fakes. `android/app` owns Compose, ViewModel,
DataStore and Android adapters. Inject dependencies through constructors; no domain Android imports.
Sprint 2 AI contracts, response validation and ephemeral conversation live in `android/core`;
HTTP, Android Keystore and consent UI live in `android/app`.
Specifications live in `specs/000-product` through `specs/015-performance-readiness`.
JSON schema version 1 uses epoch milliseconds, nullable metrics, explicit source and availability.

## Safety and privacy
Never add vehicle writes, arbitrary ELM commands, actuator tools or privileged permissions.
Only allowlisted read PIDs; simulation must be labeled in UI AND spoken output.
Unknown motion is not parked. Sprint 6 layout editing requires explicit session-only
laboratory mode and confirmation of a stationary test environment (never proof of PARKED).
All external actions pass deterministic tool policy; provider text never grants permission.
No background microphone loop. Stop recognition/TTS on backgrounding. No audio/history logging.
Contacts stay local. ACTION_DIAL only, with fresh confirmation. No implicit network consent.
External AI sends text and bounded session history only after explicit consent; never send telemetry,
contacts or location. Remote endpoints require HTTPS. Debug local HTTP is loopback only. Do not log
request bodies or credentials. Model tool proposals must pass ToolRegistry and PolicyEngine.

## Build and test
Python: `python -m unittest discover -s tests -v`.
Android: JDK 17, SDK platform/build-tools 35; from `android` run
`./gradlew :core:test :testing:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
Windows uses `gradlew.bat`. `:app:connectedDebugAndroidTest` requires an emulator/device.
See `docs/TEST_RESULTS.md` for actual evidence; a test definition is not a passing test.

## Conventions and definition of done
Specify behavior and failure cases before implementing. Add deterministic tests before platform adapters.
Pin stable dependencies verified against official documentation; avoid speculative modules/services.
Use coroutines/StateFlow and cancellation; never swallow CancellationException.
No secrets, build artifacts, SDKs or release signing keys in commits. No push without authorization.
Changes require relevant tests, build/lint where available, and updated status/limitations.
Continue from `docs/NEXT_STEPS.md`; distinguish implemented, tested, simulated and pending.

Sprint 4: `DiagnosticCheck` in pure core separates check status from evidence level. Never promote
fake/emulator checks to physical verification. Device Lab passive checks may run without hardware;
network, acoustic and OBD actions require explicit user initiation. Diagnostic history is bounded,
session-only and redacted; no automatic export. Use monotonic time for durations and wall time for
timestamps. See `docs/PHYSICAL_VALIDATION_MATRIX.md` before claiming compatibility.

Sprint 5: `PlacesController` in pure core owns session-only results and expiring selection. Android
requests coarse location only when a search starts; the Google Places SDK is optional and requires
separate session consent. Keep provider text out of AI prompts and diagnostics. Demo results and
navigation must stay visibly simulated. Never infer ETA from straight-line distance. Keep the
physical OBD, voice, GPS and Maps/Waze validation matrix pending until observed on devices.

Sprint 6: closed native WidgetRegistry; pure Kotlin layout contracts, codec/migration,
first-fit projection and edit sessions in core. DataStore stores only non-sensitive v1
layout; no drafts or safety confirmations. One layout coordinator, no provider/ViewModel
per widget. Layout editor abandons on movement, lab revocation, TTL expiry or background;
in-flight writes are cancelled when safety is revoked. No layout tool exposed to AI.
Always retain fixed voice status/PTT, settings and telemetry-source indicator. Preserve
LegacyDashboardScreen as the Sprint 5 reference. Execute DashboardLayoutTest before
editor changes; record actual dp/resolution/fontScale evidence separately from hardware.

Sprint 8: distinguish StartupTimingMetric TTID/TTFD from external am-start/UI probe
bounds. Compose ReportDrawnWhen requires loaded preferences, onboarding, restored
layout and first vehicle snapshot; optional voice/network never gate textual use.
Macrobenchmark is opt-in (-PenableMacrobenchmark), release-derived, non-debuggable,
debug-signed and isolated as ai.copilotdrive.app.benchmark. Never add benchmark
warnings or unstable budgets to required CI. Only explicit EMULATOR quality warning
may be suppressed for emulator evidence. See docs/BENCHMARK_RUNBOOK.md.
Run ADB consumers sequentially. stability_android and benchmark_android refuse physical
targets; preserve preferences/layout and restore display/permission overrides. PSS
growth alone cannot prove a leak. Keep raw Perfetto/device logs ignored and review
before sharing. No emulator/automation can produce a human acoustic observation.
Provider credential lookup may suspend: recheck consent AFTER the lookup and before
transport. Keystore IO is serialized off Main; never cache plaintext credentials.

Sprint 7: `DeviceQualification` reuses diagnostic EvidenceLevel and enforces build/API/model,
preconditions, method and observation provenance. CompatibilityStatus is independent of
availability; emulator/automated checks must never produce PHYSICALLY_VERIFIED. Manual
physical observation requires an exact APK/commit acta. See specs/014-device-qualification.
`scripts/qualify_android.py` is passive by default; use explicit serial and dry-run before
active work. Physical installation/activity requires fresh human confirmation, then the
separate allow flags. No data clear, reboot, audio/OBD/network tests or screenshots in runner.
Display matrix is emulator-only and restores overrides/font/rotation in finally. Keep numeric
samples in ignored artifacts; p50/p95 require 20 identical-condition observations.
Voice cancellation preserves completed written answers; old STT/TTS cleanup must not close
a new session. Device Lab speech is foreground/cancellable. Recheck Places consent after
location suspension and before provider invocation. Run VoiceReliabilityTest, PlacesScenarioTest,
DeviceQualificationTest and AccessibilityTest for changes at these boundaries.
