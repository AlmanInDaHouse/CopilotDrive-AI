# Sprint 9: jank forensics and Android compatibility

Baseline: Sprint 8 merged by PR #9, main 504e9a776177d78d70a085dcb7b4cacc914a069a.
Characterize dashboardScroll, settingsAndLab, presetDraft and backgroundRestore
independently. Reuse specs/014 qualification and specs/015 performance contracts.

Each finding links original samples, commit/APK, device/API/display, compilation,
renderer, host observations, measurement window, hypothesis, intervention and repeat.
Correlation is not causal attribution. At most three measured app fixes in the first
iteration; no speculative UI rewrite when profiles cannot attribute app work.

Preserve raw failed attempts. Missing frames, severe host paging, unavailable
ActivityManager, restoration failure or missing provenance invalidate a comparison.
An emulator result never implies physical verification. No rigid noisy CI budgets.

API26/30 require actual independent official-image execution, separately authorized
significant downloads/licenses. Passive discovery precedes physical-device consent.
No device data clear, uninstall, secrets/conversation export, ECU writes, paid calls,
automatic speech, or permanent microphone. Hardware absence does not block emulator work.
