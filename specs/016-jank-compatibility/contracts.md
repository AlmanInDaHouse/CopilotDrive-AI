# Evidence contract

Reuse BenchmarkEnvironment, BenchmarkSample, FrameTimingMeasurement,
MemoryMeasurement, QualificationEnvironment, EvidenceLevel and CompatibilityStatus.
The forensic JSON envelope is schemaVersion=1 and contains scenario, codeCommit,
APK SHA256, runnerCommit, workingTreeDirty, Android environment, renderer,
hostBefore/hostAfter, monotonic duration, compilation, iterations, trace paths/hashes,
native metric summaries, hypothesis, implementedChange, subsequentEvidence and verdict.
Null is unavailable, never zero. Raw traces and numeric samples remain ignored artifacts.

Trace analysis is restricted to the target package and clipped to measureBlock.
Inclusive slice times overlap and must not be added as independent CPU work.
Main/RenderThread scheduling states distinguish Running, runnable wait and sleeping;
FrameTimeline jank reasons are labels, not proof of an app or host root cause.
Missing trace windows/target/frame metrics fail closed. CPU duration and overrun are
different metrics. Frame populations are not iteration populations.

Comparisons require same scenario, metric/unit, APK variant, API/device/display,
renderer, compilation strategy, iteration count and acceptable host preconditions.
Different renderer is an environment experiment, never an app improvement claim.
Hypothesis verdict: SUPPORTED_CORRELATION, REJECTED, INCONCLUSIVE or BLOCKED;
causal attribution requires a controlled intervention and equivalent repeat.

Installed package/hash and layout/preferences fingerprints are checked around active
procedures; restoration failures stop further work. connectedAndroidTest is disposable
only. Direct am instrument avoids Gradle's uninstall, but individual tests may write
preferences: audit them and use disposable targets for the full suite when necessary.
Physical serials, media metadata, contacts, coordinates and credentials are excluded.
