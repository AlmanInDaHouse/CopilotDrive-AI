# Acceptance

- Exact build/environment provenance; impossible emulator-to-physical promotion tested.
- Passive discovery and authorization boundaries tested with no attached hardware.
- Accessible names, contextual repeated actions, state, dialog dismissal and 48dp
  targets tested; fontScale 2, portrait, landscape and high density exercised.
- No duplicate STT; denial/revocation/empty/silence/timeout/cancellation/focus/TTS
  failure recover without erasing completed answers; cleanup failure cannot skip focus release.
- Existing layout, DataStore, tool policy, Keystore, MediaSession, navigation, Places,
  OBD, diagnostics and privacy suites pass without relaxing safety checks.
- Debug build, lint, instrumentation, signature and hash verified locally.
- Measurements have samples/method/environment or explicit blocker; no physical claims.
- User data preserved; no push/merge/release. Physical and paid integration work PENDING.
