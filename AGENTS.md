# CopilotDrive-AI

Conventional Android APK for open devices, not Android Auto, AAOS or CarPlay.
Keep the Python/web prototype functional. Android must work without Python or API keys.

## Architecture
`android/core` is pure Kotlin: contracts, policy, tools, orchestration, voice state machine.
`android/testing` contains deterministic fakes. `android/app` owns Compose, ViewModel,
DataStore and Android adapters. Inject dependencies through constructors; no domain Android imports.
Sprint 2 AI contracts, response validation and ephemeral conversation live in `android/core`;
HTTP, Android Keystore and consent UI live in `android/app`.
Specifications live in `specs/000-product` through `specs/012-widgets-future`.
JSON schema version 1 uses epoch milliseconds, nullable metrics, explicit source and availability.

## Safety and privacy
Never add vehicle writes, arbitrary ELM commands, actuator tools or privileged permissions.
Only allowlisted read PIDs; simulation must be labeled in UI AND spoken output.
Unknown motion is not parked. No advanced layout editor in this increment.
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
