# Device qualification — Sprint 7

Scope: reproducible qualification of an exact debug APK, accessibility corrections and
foreground voice recovery. No new editor features, vehicle writes, commercial calls,
background microphone, physical certification or release publishing.

Qualification records identify commit, APK SHA-256, Android API/model (no serial),
environment, preconditions, scenario, result, evidence and validation method.
Availability and successful callbacks never imply physical verification.
Reuse EvidenceLevel, DiagnosticCheck and LatencySummary from core diagnostics.

Voice: one active recognition, cancellation invalidates late callbacks, resources close
independently, written answers survive every audio failure/cancellation. A new explicit
request can recover after failure/focus loss. Focus recovery never restarts microphone.

UI: label toggles/sliders, contextual editor actions, readable state and headings.
Keep PTT, settings, microphone state and source visible outside scrolling widgets.
No live region on telemetry or timers. Compose semantics do not certify TalkBack.

Runner: passive default, explicit target, dry-run, hash validation before install,
separate active/install/physical authorization. No clearing data, reboot, license
acceptance, audio/OBD/external queries or automatic screenshots. Emulator display/font
changes restore exact prior values in finally. Export allowlisted aggregate data only.
