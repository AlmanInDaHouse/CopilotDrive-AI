"""Offline, numeric-only analysis of local Macrobenchmark evidence. No ADB/mutations."""
import argparse
import csv
import hashlib
import io
import json
import math
from pathlib import Path
import subprocess
import sys

from qualify_android import ROOT, QualificationError, command


def metric_summary(runs):
    if not runs or any(not row for row in runs):
        raise QualificationError('MISSING_FRAME_ITERATION')
    values = [value for row in runs for value in row]
    if any(type(v) not in (float, int) or not math.isfinite(v) for v in values):
        raise QualificationError('INVALID_FRAME_VALUE')
    values.sort()
    return dict(iterations=len(runs), frames=len(values),
        p50=values[math.ceil(len(values) * .5)-1], p95=values[math.ceil(len(values) * .95)-1],
        positivePercent=100 * sum(v > 0 for v in values) / len(values))


def parse_window(raw):
    rows = list(csv.DictReader(io.StringIO(raw)))
    windows = [r for r in rows if r['category'] == 'window']
    if len(windows) != 1 or int(windows[0]['n']) != 1 or float(windows[0]['value_ms']) <= 0:
        raise QualificationError('UNIQUE_MEASUREMENT_WINDOW_REQUIRED')
    if not any(r['category'] == 'scheduling' and r['label'].startswith('Main:') for r in rows):
        raise QualificationError('TARGET_SCHEDULING_MISSING')
    return [dict(category=r['category'], label=r['label'], count=int(r['n']),
        durationMs=None if r['value_ms'] in ('[NULL]', '') else round(float(r['value_ms']), 4)) for r in rows]


def analyze(root, processor, code_commit, apk, limit=None, run_root=None):
    import re
    if not re.fullmatch('[0-9a-f]{40}', code_commit):
        raise QualificationError('EXACT_CODE_COMMIT_REQUIRED')
    result = dict(schemaVersion=1, codeCommit=code_commit,
        apkSha256=hashlib.sha256(apk.read_bytes()).hexdigest(),
        runnerCommit=command(['git', '-C', ROOT, 'rev-parse', 'HEAD']),
        workingTreeDirty=bool(command(['git', '-C', ROOT, 'status', '--porcelain'])),
        analysisScriptSha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
        querySha256=hashlib.sha256(Path(__file__).with_name('jank_window.sql').read_bytes()).hexdigest(),
        traceScope='target package; clipped to measureBlock; inclusive slices overlap',
        verdict='INCONCLUSIVE', hypotheses=[], implementedChange=None, subsequentEvidence=None, scenarios=[])
    for folder in sorted(root.iterdir()):
        sources = list(folder.glob('*benchmarkData.json')) if folder.is_dir() else []
        if not sources:
            continue
        native = json.loads(sources[0].read_text(encoding='utf-8'))
        for bench in native['benchmarks']:
            if not bench.get('sampledMetrics'):
                continue  # Startup populations are a different metric, never treated as frames.
            entry = dict(scenario=bench['name'], context=native['context'],
                iterations=bench['repeatIterations'], metrics={}, traces=[])
            report_path = run_root / ('repeat-' + bench['name'] + '.json') if run_root else None
            entry['runEvidence'] = json.loads(report_path.read_text(encoding='utf-8')) if report_path and report_path.is_file() else None
            for name in ('frameDurationCpuMs', 'frameOverrunMs'):
                metric = bench['sampledMetrics'].get(name)
                entry['metrics'][name] = metric_summary(metric['runs']) if metric else None
                if metric and name == 'frameDurationCpuMs':
                    if any(value < 0 for row in metric['runs'] for value in row):
                        raise QualificationError('NEGATIVE_FRAME_CPU_DURATION')
                    del entry['metrics'][name]['positivePercent']
                if metric and len(metric['runs']) != bench['repeatIterations']:
                    raise QualificationError('ITERATION_COUNT_MISMATCH')
            paths = sorted(folder.glob('*.perfetto-trace'))
            if not paths:
                raise QualificationError('TRACES_MISSING')
            for path in paths[:limit] if limit else paths:
                run = subprocess.run([sys.executable, str(processor), 'query', '-f',
                    str(Path(__file__).with_name('jank_window.sql')), str(path)],
                    capture_output=True, timeout=120)
                if run.returncode:
                    raise QualificationError('TRACE_QUERY_FAILED')
                measurements = parse_window(run.stdout.decode('utf-8'))
                entry['traces'].append(dict(file=str(path),
                    sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                    quality='REVIEW_REQUIRED' if any(m['category'] == 'traceErrors' for m in measurements) else 'PARSED',
                    measurements=measurements))
            result['scenarios'].append(entry)
    if not result['scenarios']:
        raise QualificationError('NO_INTERACTION_EVIDENCE')
    return result


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--trace-root', type=Path, required=True)
    p.add_argument('--processor', type=Path, required=True)
    p.add_argument('--code-commit', required=True)
    p.add_argument('--apk', type=Path, required=True)
    p.add_argument('--limit', type=int)
    p.add_argument('--run-root', type=Path)
    p.add_argument('--out', type=Path, required=True)
    args = p.parse_args()
    report = analyze(args.trace_root, args.processor, args.code_commit, args.apk, args.limit, args.run_root)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(dict(status='ANALYZED', scenarios=len(report['scenarios']), evidence=args.out.name)))


if __name__ == '__main__':
    main()
