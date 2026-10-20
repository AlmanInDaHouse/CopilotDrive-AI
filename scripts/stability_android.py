"""Opt-in emulator stability runner. No data clear, paid queries, speech or real OBD.

Requires installed Sprint 8 debug + AndroidTest APKs. Uses the qualification target
guard and hash provenance. Settings are restored in finally; no raw UI/log export.
"""
from __future__ import annotations

import argparse
from contextlib import contextmanager
import hashlib
import json
from pathlib import Path
import re
import time

from qualify_android import (ROOT, PACKAGE, COMPONENT, Device, QualificationError,
    command, find_adb, devices, verify_apk, layout_hash, temporary_display)


def require_emulator(device, allow_active):
    if not allow_active or device.kind() != 'EMULATOR':
        raise QualificationError('EXPLICIT_EMULATOR_ACTIVITY_REQUIRED')


def require_framework(device):
    # ADB and boot_completed may still respond after system_server has failed.
    if device.shell('service', 'check', 'activity') != 'Service activity: found':
        raise QualificationError('ANDROID_FRAMEWORK_UNAVAILABLE')


def pss_kib(output):
    match = re.search(r'TOTAL PSS:\s*(\d+)', output) or re.search(r'^\s*TOTAL\s+(\d+)', output, re.M)
    if not match or int(match[1]) <= 0:
        raise QualificationError('PSS_UNAVAILABLE')
    return int(match[1])


def exit_events(output):
    """Return fingerprints + fixed reason numbers; discard process details immediately."""
    result = {}
    for block in re.split(r'ApplicationExitInfo #', output)[1:]:
        reason = re.search(r'reason=(\d+)', block)
        stamp = re.search(r'timestamp=([^\r\n]+)', block)
        pid = re.search(r'pid=(\d+)', block)
        if reason and stamp:
            fingerprint = hashlib.sha256((stamp[1] + ':' + (pid[1] if pid else '') + ':' + reason[1]).encode()).hexdigest()
            result[fingerprint] = int(reason[1])
    return result


@contextmanager
def revoked_microphone(device, restoration):
    """Do not grant a previously denied permission or change user-fixed permission flags."""
    state = device.shell('dumpsys', 'package', PACKAGE)
    match = re.search(r'android.permission.RECORD_AUDIO: granted=(true|false), flags=\[([^\]]*)\]', state)
    if not match:
        raise QualificationError('PERMISSION_STATE_UNAVAILABLE')
    granted, flags = match[1] == 'true', match[2].strip()
    # pm grant cannot reconstruct one-time / fixed flags safely: skip before mutation.
    if granted and flags:
        restoration.update(status='PENDING', reason='NONEMPTY_PERMISSION_FLAGS')
        yield False
        return
    try:
        if granted:
            device.shell('pm', 'revoke', PACKAGE, 'android.permission.RECORD_AUDIO')
        restoration['originalGranted'] = granted
        yield True
    finally:
        if granted:
            device.shell('pm', 'grant', PACKAGE, 'android.permission.RECORD_AUDIO')
        restored = device.shell('dumpsys', 'package', PACKAGE)
        current = re.search(r'android.permission.RECORD_AUDIO: granted=(true|false), flags=\[([^\]]*)\]', restored)
        restoration['status'] = 'RESTORED' if current and (current[1] == 'true') == granted and current[2].strip() == flags else 'FAILED'
        if restoration['status'] != 'RESTORED':
            raise QualificationError('PERMISSION_RESTORE_FAILED')


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--adb')
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--expected-sha256', required=True)
    parser.add_argument('--cycles', type=int, default=20)
    parser.add_argument('--allow-active', action='store_true')
    parser.add_argument('--dry-run', action='store_true')
    parser.add_argument('--out', type=Path, default=ROOT / 'artifacts/sprint8/stability.json')
    args = parser.parse_args(argv)
    report = dict(schemaVersion=1, status='PENDING', physicalQualification='PENDING',
        evidenceLevel='PENDING', measurementKind='FUNCTIONAL_AND_EXTERNAL_MEMORY',
        commit=command(['git', '-C', ROOT, 'rev-parse', 'HEAD']), samples=[],
        scenarios=['restart', 'foreground', 'preset draft', 'passive lab', 'Places panel', 'recreate', 'rotation', 'microphone denied'],
        excluded=['real audio', 'real OBD', 'paid providers', 'contacts', 'navigation intents'])
    report['workingTreeDirty'] = bool(command(['git', '-C', ROOT, 'status', '--porcelain']))
    try:
        if not 1 <= args.cycles <= 1000:
            raise QualificationError('INVALID_CYCLE_COUNT')
        adb = find_adb(args.adb)
        if dict(devices(command([adb, 'devices', '-l']))).get(args.serial) != 'device':
            raise QualificationError('TARGET_NOT_AUTHORIZED')
        device = Device(adb, args.serial, 'device')
        require_emulator(device, args.allow_active)
        require_framework(device)
        report['environment'] = device.environment()
        report['apkSha256'] = verify_apk(args.apk, args.expected_sha256)
        installed = re.search(r'^package:(.+base\.apk)$', device.shell('pm', 'path', PACKAGE), re.M)
        if not installed or device.shell('sha256sum', installed[1]).split()[0] != report['apkSha256']:
            raise QualificationError('INSTALLED_APK_HASH_MISMATCH')
        test_apk = ROOT / 'android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
        report['testApkSha256'] = verify_apk(test_apk, None)
        test_installed = re.search(r'^package:(.+base\.apk)$', device.shell('pm', 'path', PACKAGE + '.test'), re.M)
        if not test_installed or device.shell('sha256sum', test_installed[1]).split()[0] != report['testApkSha256']:
            raise QualificationError('INSTALLED_TEST_APK_HASH_MISMATCH')
        if args.dry_run:
            report['status'] = 'DRY_RUN'
        else:
            before = layout_hash(device)
            exit_info_supported = report['environment']['androidApi'] >= 30
            initial_exits = exit_events(device.shell('dumpsys', 'activity', 'exit-info', PACKAGE)) if exit_info_supported else {}
            observed_exits = {}
            started = time.monotonic()
            device.shell('am', 'start', '-W', '-n', COMPONENT)
            report['initialPssKiB'] = pss_kib(device.shell('dumpsys', 'meminfo', PACKAGE))
            report['permissionRestoration'] = {}
            with temporary_display(device) as restoration, revoked_microphone(device, report['permissionRestoration']) as permission_tested:
                report['displayRestoration'] = restoration
                report['permissionDeniedScenario'] = 'EXECUTED' if permission_tested else 'PENDING'
                result = device.shell('am', 'instrument', '-w', '-e', 'class',
                    'ai.copilotdrive.app.StabilityJourneyTest#cyclesPreserveConfigurationAndRecover',
                    '-e', 'cycles', str(args.cycles), PACKAGE + '.test/androidx.test.runner.AndroidJUnitRunner',
                    timeout=60 + args.cycles * 60)
                if 'OK (1 test)' not in result or 'FAILURES!!!' in result or 'INSTRUMENTATION_FAILED' in result:
                    raise QualificationError('STABILITY_JOURNEY_FAILED')
                raw_memory = json.loads(device.shell('run-as', PACKAGE, 'cat', 'files/sprint8-stability-memory.json'))
                # Whitelist the numeric contract even though it comes from our test APK.
                report['sustainedProcessSamples'] = [{key: int(item[key]) for key in
                    ('cycle', 'elapsedMs', 'instrumentedPssKiB')} for item in raw_memory]
                raw_operations = json.loads(device.shell('run-as', PACKAGE, 'cat', 'files/sprint8-operation-bounds.json'))
                allowed_operations = {'presetDraft', 'backgroundRestore', 'settingsWithConfirmation', 'deviceLabWithScroll'}
                report['operationUpperBounds'] = [dict(operation=item['operation'], upperBoundMs=float(item['upperBoundMs']))
                    for item in raw_operations if item['operation'] in allowed_operations]
                if exit_info_supported:
                    observed_exits.update(exit_events(device.shell('dumpsys', 'activity', 'exit-info', PACKAGE)))
                device.shell('settings', 'put', 'system', 'accelerometer_rotation', '0')
                for index in range(args.cycles):
                    device.shell('settings', 'put', 'system', 'user_rotation', str(index % 2))
                    device.shell('am', 'force-stop', PACKAGE)
                    device.shell('am', 'start', '-W', '-n', COMPONENT)
                    device.shell('input', 'keyevent', 'KEYCODE_HOME')
                    device.shell('am', 'start', '-W', '-n', COMPONENT)
                    report['samples'].append(dict(cycle=index + 1, elapsedMs=int((time.monotonic() - started) * 1000),
                        pssKiB=pss_kib(device.shell('dumpsys', 'meminfo', PACKAGE))))
                    if layout_hash(device) != before:
                        raise QualificationError('LAYOUT_CHANGED')
                    if exit_info_supported:
                        observed_exits.update(exit_events(device.shell('dumpsys', 'activity', 'exit-info', PACKAGE)))
                    print(f'cycle {index + 1}/{args.cycles}', flush=True)
            reasons = [reason for key, reason in observed_exits.items() if key not in initial_exits]
            report.update(crashesObserved=(reasons.count(4) + reasons.count(5)) if exit_info_supported else None,
                anrsObserved=reasons.count(6) if exit_info_supported else None,
                crashObservationMethod='new ActivityManager exit-info entries; bounded history',
                crashObservationStatus='OBSERVED' if exit_info_supported else 'UNSUPPORTED_API',
                memoryDeltaKiB=report['samples'][-1]['pssKiB'] - report['initialPssKiB'],
                elapsedMs=int((time.monotonic() - started) * 1000), leakAssessment='INCONCLUSIVE_PSS_IS_NOT_A_HEAP_LEAK_PROOF',
                evidenceLevel='EMULATOR', status='EMULATOR_TESTED')
            if report['crashesObserved'] or report['anrsObserved']:
                report['status'] = 'FAILED'
    except (QualificationError, ValueError, OSError) as failure:
        report.update(status='BLOCKED', errorCode=str(failure) if isinstance(failure, QualificationError) else 'INVALID_ENVIRONMENT')
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(dict(status=report['status'], evidence=args.out.name)))
    return 1 if report['status'] in ('BLOCKED', 'FAILED') else 0


if __name__ == '__main__':
    raise SystemExit(main())
