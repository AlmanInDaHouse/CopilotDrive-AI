# Test plan

Baseline and final: Python unittest, repository checks, core/testing/app unit tests,
lintDebug, assembleDebug, assembleDebugAndroidTest, connectedDebugAndroidTest on
authorized emulator. Parse XML reports for actual counts (baseline separately).

Core: qualification invariant failures, sample thresholds, voice recovery/races and
cleanup faults. Python: runner authorization, serial redaction, hash and percentile
checks, restoration under injected failure. Compose: controls named by semantics,
states/roles, headings, contextual editor labels, response dialog, essential geometry.

Emulator smoke: source/PTT/settings/microphone bounds, layout persistence across
force-stop, display/font/density/rotation configurations; restore settings. Run local
fuel query with transports disabled and restore network. Avoid exporting screenshots
unless explicitly permitted. Cold-process am start -W measurements (>=20), ready
upper bound via UI probe and dumpsys meminfo; debug/emulator caveats documented.

Physical: inspect adb inventory only. Ask user before install/data changes/active
testing; stopped vehicle plus adapter authorization before OBD. TalkBack and audible
Spanish response require guided human observation. No physical device: PENDING.
