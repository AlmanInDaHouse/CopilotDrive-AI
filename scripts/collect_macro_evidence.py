"""Pull only native numeric JSON and its referenced traces from an opted-in emulator.

Local ignored evidence only. Never share raw Perfetto without privacy review.
No installation, settings changes, clear or uninstall.
"""
import argparse
import json
from pathlib import Path, PurePosixPath
from qualify_android import Device, QualificationError, find_adb
from stability_android import require_emulator, require_framework


def collect(device, out):
    require_emulator(device, True)
    require_framework(device)
    root = '/sdcard/Android/media/ai.copilotdrive.macrobenchmark'
    files = device.shell('find', root, '-type', 'f').splitlines()
    sources = [p for p in files if p.endswith('-benchmarkData.json')]
    if len(sources) != 1:
        raise QualificationError('UNIQUE_NATIVE_JSON_REQUIRED')
    out.mkdir(parents=True, exist_ok=False)  # Never overwrite an earlier attempt.
    source = sources[0]
    native = json.loads(device.shell('cat', source))
    # Native benchmark context/metrics only; no instrumentation logs or UI are exported.
    (out / PurePosixPath(source).name).write_text(json.dumps(native, indent=2)+'\n', encoding='utf-8')
    for bench in native['benchmarks']:
        for profiler in bench.get('profilerOutputs', []):
            if profiler['type'] != 'PerfettoTrace':
                continue
            name = profiler['filename']
            if PurePosixPath(name).name != name or not name.endswith('.perfetto-trace'):
                raise QualificationError('UNSAFE_TRACE_REFERENCE')
            path = str(PurePosixPath(source).parent / name)
            if path not in files:
                raise QualificationError('REFERENCED_TRACE_MISSING')
            device.call('pull', path, out / name, timeout=120)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--serial', required=True)
    p.add_argument('--allow-active', action='store_true')
    p.add_argument('--out', type=Path, required=True)
    args = p.parse_args()
    if not args.allow_active:
        raise QualificationError('EXPLICIT_EMULATOR_ACTIVITY_REQUIRED')
    collect(Device(find_adb(), args.serial, 'device'), args.out)
    print(json.dumps(dict(status='COLLECTED_LOCAL_ONLY', folder=args.out.name)))
