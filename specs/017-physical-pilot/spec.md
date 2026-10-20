# Sprint 10: controlled physical pilot

Reuse DeviceQualification, CompatibilityStatus, EvidenceLevel and benchmark provenance.
Passive discovery precedes any physical installation or activity. No phone means all
physical observations remain PENDING. Human observations require exact build/session,
scenario, attempt, timestamp and a redacted record; automated callbacks cannot supply them.

Device Lab provides an explicit, foreground, cancellable acoustic phrase and human
heard/not-heard confirmation after completion. Each retry invalidates the prior answer.
No recording, persistent conversation, background listening or automatic external action.
The session-only UI observation is not a signed, build-bound physical qualification.

Show the existing voice coordinator stages and policy decisions through typed events,
bounded in memory. No transcript, provider reasoning, arguments, contacts or coordinates
enter this diagnostic view. Preserve the normal written answer when audio fails.

API26 offline distinguishes controlled provider independence (A) from Android transport
disabled (B). Only disposable AVDs may be mutated; persist previous network state before
commands, bound retries, verify connectivity independently and restore on every exit.
No settings flag alone establishes disconnected Android. Keep failed attempts.

No benchmarks under severe host pressure, speculative optimization, commercial CI,
ECU writes, arbitrary CAN, VIN, automatic calls, push, merge or release.
