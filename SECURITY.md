# Security policy (experimental)

Use only in simulation or on a stationary vehicle during development.
Do not expose this unauthenticated dashboard to the internet or untrusted Wi-Fi.
No vehicle actuation, ECU writing, CAN frame injection, steering, braking, driving, locking or HVAC controls are supported.
The OBD serial implementation uses only allowlisted service-01 read PIDs and adapter setup commands.
Never blindly follow LLM output as operational or vehicle safety advice.
Microphone recognition is delegated to the browser; some implementations may send audio to a third-party speech service.
Optional LLM queries send questions and current telemetry to the configured endpoint.
This research prototype is NOT a certified driver assistance or automotive safety system.


## Android MVP
The Android app has no INTERNET, CALL_PHONE, Bluetooth or privileged media-control permission.
It uses foreground push-to-talk only. Local STT is preferred; system STT fallback requires separate
consent because the installed recognition service may transmit audio. The app itself stores no audio,
transcripts, contact history or telemetry history. TTS uses installed non-network voices only.
DataStore contains onboarding, voice choices and recognition consent. Backup/device transfer are excluded.

Notification listener access is used only to query MediaSessionManager; notification content is not read.
Tool policy is deny-by-default with closed arguments, capabilities, timeouts and one-use dial confirmation.
No ECU transport exists in the APK. ObdDecoder accepts an enum of four read PIDs, not arbitrary commands.
The Python prototype retains its optional network provider; its privacy behavior differs as described above.

This is a debug laboratory build, not a production security audit or a certified driving application.
Dependency updates remain reviewed work: fixed versions are documented in docs/COMPATIBILITY_MATRIX.md.
