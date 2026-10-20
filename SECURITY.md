# Security policy (experimental)

Use only in simulation or on a stationary vehicle during development.
Do not expose this unauthenticated dashboard to the internet or untrusted Wi-Fi.
No vehicle actuation, ECU writing, CAN frame injection, steering, braking, driving, locking or HVAC controls are supported.
The OBD serial implementation uses only allowlisted service-01 read PIDs and adapter setup commands.
Never blindly follow LLM output as operational or vehicle safety advice.
Microphone recognition is delegated to the browser; some implementations may send audio to a third-party speech service.
Optional LLM queries send questions and current telemetry to the configured endpoint.
This research prototype is NOT a certified driver assistance or automotive safety system.
