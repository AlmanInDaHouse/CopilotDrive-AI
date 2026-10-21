# CopilotDrive-AI

Sprint 11: REAL is the default and a persisted OperationMode independent of connection.
No silent fallback to vehicle/media/Places demo. Existing unversioned defaults migrate
to REAL with an acknowledged notice; preserve voice, consents and dashboard DataStore.
Demo must be explicit and labelled in visible AND spoken responses. A mode transition
clears previous source answers/results, cancels audio and closes OBD; ordinary voice
cancellation still preserves the written answer. Do not auto-connect Bluetooth.
Distinguish missing PID, NO DATA, disconnected and expired telemetry without changing
the v1 wire contract. Use Cockpit tokens/components for native UI; keep eight widget
types, presets and lab safety. Follow docs/SPRINT11_REPORT.md for evidence and pending
physical validation of versionCode 2; Sprint10.1 acoustics do not qualify a new APK.
Run RealModeTest, OperationModeTest, VoiceReliabilityTest, DashboardPresentationTest,
RealModeJourneyTest and the existing accessibility/layout suites for these boundaries.
RealModeJourneyTest and sprint11_visual_matrix write fixtures/display overrides and
belong only on dedicated disposable AVDs; preserve the original emulator and phone.

Conventional Android APK for open devices, not Android Auto, AAOS or CarPlay.
Keep the Python/web prototype functional. Android must work without Python or API keys.

## Architecture
`android/core` is pure Kotlin: contracts, policy, tools, orchestration, voice state machine.
`android/testing` contains deterministic fakes. `android/app` owns Compose, ViewModel,
DataStore and Android adapters. Inject dependencies through constructors; no domain Android imports.
Sprint 2 AI contracts, response validation and ephemeral conversation live in `android/core`;
HTTP, Android Keystore and consent UI live in `android/app`.
Specifications live in `specs/000-product` through `specs/017-physical-pilot`.
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
ADB and boot_completed do not establish framework readiness: check ActivityManager
before instrumentation. Runners never reboot automatically. CompilationMode.None
is guarded to API34+ because older resets may reinstall data; prepare Ignore safely
before API26/30 benchmarking. Scroll gestures must move content and produce frames;
do not turn zero captured frames into zero jank. Preserve failed attempts.
connectedDebugAndroidTest may uninstall the app/data when finished. For an emulator
whose state must survive, use install -r and am instrument directly; connected CI
tests belong on disposable AVDs. Record any preservation incident explicitly.

Sprint 9: base is Sprint 8 merged by PR #9 at 504e9a7; both merge workflows passed.
Use jank_evidence.py/jank_window.sql to clip target-thread slices to measureBlock.
Inclusive wall slices overlap; guest Running is not exclusive physical-host CPU.
FrameTimeline labels and PSS deltas are correlations, not causal/leak proof.
Keep parse errors, failed attempts and absent metrics explicit; raw traces stay ignored.
benchmark_android observes host memory/commit/paging before/after and refuses severe
pressure; no builds, SDK downloads, installs or bulk trace processing during measures.
None is guarded on API34+ in Kotlin AND Python; older APIs require explicit Ignore,
which must never be compared to None as equivalent compilation conditions.
Direct instrumentation preserves packages but full tests overwrite fixture credentials,
layout and notification grants: use compatibility_android on dedicated disposable
CopilotDrive_Sprint9_API26/30/35 AVDs, never a valuable or physical target. Preserve
original package/DataStore fingerprints before stopping it and verify after return.
Memory stress separates sustained-process and restart populations; weak references and
job counts are observations, not a substitute for heap dominators. See Sprint 9 docs.
Reacquire recycled accessibility nodes after scroll; repeated stale observations fail
explicitly and never count as proof of movement. Keep runner APK hashes in comparisons.

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


Sprint10: Sprint9 merged through PR#10 at793a353; Android/Tests merge CI success.
PhysicalPilot reuses qualification enums. Human acoustic confirmation is independent
of callback and bound to exact session/build/attempt; the session-only UI cannot
emit physical qualification. New attempt/cancel invalidates old confirmation. Acoustic
probe foreground only, focus/20s timeout/cancellation; no recording or background mic.
VoiceJourneyStage is closed and carries no transcript/tool arguments/provider reasoning;
keep at most32 stages in memory. Normal written response survives every audio failure.
Inspect PACKAGED permissions: Places adds FINE transitively; remove it explicitly
and keep packaged-manifest regression. Never relax the coarse-only app policy.
pilot_package.py is read-only, checks signature conflicts conservatively and never
installs/launches/grants. Physical activity still needs explicit human authorization;
performance consent is independent. No uninstall, downgrade or clear-data fallback.
api26_offline.py requires named disposable AVD/API26 and opt-in, persists prior state,
checks flags AND ConnectivityService before/after query, restores network including
connected-network count. svc wifi137 here has a recorded SecurityException for missing
shell CHANGE_WIFI_STATE: no root/grant workaround and no OOM inference. Settings UI
is an explicit disposable alternative; overlays block rather than auto-dismiss.
Test A fake-provider independence never proves test B Android radios disconnected.
Pico TTS API26 crashed in libttspico; preserve the limitation and written fallback.
Physical absence keeps voice/OBD/performance/interoperability/TalkBack PENDING.
Do not repeat jank campaigns on a host failing memory/commit/paging guards.
