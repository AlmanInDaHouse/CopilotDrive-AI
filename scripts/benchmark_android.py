"""Run prebuilt isolated Macrobenchmark APKs on one explicitly authorized emulator.

Build with -PenableMacrobenchmark first. No data clear; no connectedCheck device
auto-selection. Raw Perfetto/JSON stay on device or in ignored local artifacts and
must be reviewed before sharing. Physical testing uses the guided consent procedure.
"""
import argparse
import json
from pathlib import Path
import re
import time
from datetime import datetime, timezone
from qualify_android import ROOT, Device, QualificationError, command, devices, find_adb, verify_apk
from stability_android import require_emulator, require_framework, protected_debug_state
from host_health import observe

TARGET = 'ai.copilotdrive.app.benchmark'
TEST = 'ai.copilotdrive.macrobenchmark'
METHODS = ('coldStartup', 'warmStartup', 'hotStartup', 'dashboardScroll', 'settingsAndLab', 'presetDraft', 'backgroundRestore')


def require_safe_compilation_api(api, compilation='None'):
    # CompilationMode.None may reinstall the target before API34. Never clear data implicitly.
    if compilation not in ('None', 'Ignore'):
        raise QualificationError('UNKNOWN_COMPILATION_STRATEGY')
    if api < 34 and compilation != 'Ignore':
        raise QualificationError('OLDER_API_REQUIRES_NONDESTRUCTIVE_COMPILATION_STRATEGY')


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--adb'); p.add_argument('--serial', required=True)
    p.add_argument('--allow-active', action='store_true')
    p.add_argument('--allow-install', action='store_true')
    p.add_argument('--dry-run', action='store_true')
    p.add_argument('--method', choices=METHODS, required=True)
    p.add_argument('--iterations', type=int, default=20)
    p.add_argument('--compilation', choices=('None', 'Ignore'), default='None')
    p.add_argument('--artifact-code-commit', help='Exact source commit of the prebuilt target, distinct from runner commit')
    p.add_argument('--out', type=Path, required=True)
    args = p.parse_args(argv)
    report = dict(status='PENDING', physicalQualification='PENDING', method=args.method,
        iterations=args.iterations, suppressedErrors=['EMULATOR'], compilation=args.compilation,
        commit=command(['git', '-C', ROOT, 'rev-parse', 'HEAD']))
    report.update(artifactCodeCommit=args.artifact_code_commit, startedAtUtc=datetime.now(timezone.utc).isoformat())
    report['workingTreeDirty'] = bool(command(['git', '-C', ROOT, 'status', '--porcelain']))
    try:
        if not 1 <= args.iterations <= 100:
            raise QualificationError('INVALID_ITERATIONS')
        if args.artifact_code_commit is not None and not re.fullmatch('[0-9a-f]{40}', args.artifact_code_commit):
            raise QualificationError('EXACT_ARTIFACT_CODE_COMMIT_REQUIRED')
        adb = find_adb(args.adb)
        if dict(devices(command([adb, 'devices', '-l']))).get(args.serial) != 'device':
            raise QualificationError('TARGET_NOT_AUTHORIZED')
        device = Device(adb, args.serial, 'device')
        require_emulator(device, args.allow_active)
        require_framework(device)
        report['environment'] = device.environment()
        report['renderer'] = device.shell('getprop', 'ro.hardware.egl')
        report['gles'] = next((line.strip() for line in device.shell('dumpsys', 'SurfaceFlinger').splitlines()
            if 'GLES:' in line), 'UNAVAILABLE')
        require_safe_compilation_api(report['environment']['androidApi'], args.compilation)
        # Fixed package/build outputs. Never install or replace the normal debug app.
        target_apk = ROOT / 'android/app/build/outputs/apk/benchmark/app-benchmark.apk'
        test_apk = ROOT / 'android/macrobenchmark/build/outputs/apk/benchmark/macrobenchmark-benchmark.apk'
        for key, apk, package in [('target', target_apk, TARGET), ('test', test_apk, TEST)]:
            report[key + 'Sha256'] = verify_apk(apk, None)
            if not args.dry_run:
                if args.allow_install:
                    device.call('install', '-r', apk, timeout=120)
                installed = re.search(r'^package:(.+base\.apk)$', device.shell('pm', 'path', package), re.M)
                if not installed or device.shell('sha256sum', installed[1]).split()[0] != report[key + 'Sha256']:
                    raise QualificationError('INSTALLED_APK_HASH_MISMATCH')
        if args.dry_run:
            report['status'] = 'DRY_RUN'
        else:
            report['hostBefore'] = observe()
            if report['hostBefore']['invalidReason']:
                raise QualificationError(report['hostBefore']['invalidReason'])
            # Gradle builds/installations must already have finished. No global data resets.
            started = time.monotonic()
            with protected_debug_state(device, report):
                result = device.shell('am', 'instrument', '-w', '-e', 'class',
                    'ai.copilotdrive.macrobenchmark.DashboardBenchmark#' + args.method,
                    '-e', 'iterations', str(args.iterations),
                    '-e', 'compilation', args.compilation,
                    '-e', 'androidx.benchmark.suppressErrors', 'EMULATOR',
                    TEST + '/androidx.test.runner.AndroidJUnitRunner', timeout=120 + args.iterations * 90)
            report['elapsedMs'] = int((time.monotonic() - started) * 1000)
            require_framework(device)
            report['hostAfter'] = observe()
            for key, package in [('target', TARGET), ('test', TEST)]:
                installed = re.search(r'^package:(.+base\.apk)$', device.shell('pm', 'path', package), re.M)
                if not installed or device.shell('sha256sum', installed[1]).split()[0] != report[key + 'Sha256']:
                    raise QualificationError('POST_RUN_APK_HASH_MISMATCH')
            if report['hostAfter']['invalidReason']:
                report['measurementValidity'] = 'INVALID'
                raise QualificationError(report['hostAfter']['invalidReason'])
            # Do not export raw instrumentation output: it may contain trace/device paths.
            passed = 'OK (1 test)' in result and 'FAILURES!!!' not in result and 'INSTRUMENTATION_FAILED' not in result
            report['status'] = 'EMULATOR_BENCHMARKED' if passed else 'FAILED'
            if not passed:
                report['errorCode'] = 'MACROBENCHMARK_FAILED_REVIEW_LOCAL_DEVICE_LOG'
                # Local ignored diagnostic only; review/redact before any export.
                args.out.parent.mkdir(parents=True, exist_ok=True)
                args.out.with_suffix('.failure.log').write_text(result, encoding='utf-8')
    except (QualificationError, ValueError, OSError) as error:
        report.update(status='BLOCKED', errorCode=str(error) if isinstance(error, QualificationError) else 'INVALID_ENVIRONMENT')
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(dict(status=report['status'], evidence=args.out.name)))
    return 1 if report['status'] in ('FAILED', 'BLOCKED') else 0


if __name__ == '__main__':
    raise SystemExit(main())
