"""Full functional suite on a named disposable Sprint 9 AVD only.

Tests overwrite test credentials, layout and listener grants. Direct instrumentation
preserves packages, NOT arbitrary user preferences; refuse valuable/physical targets.
"""
import argparse
import json
from pathlib import Path
import re
import time
from qualify_android import (ROOT, PACKAGE, COMPONENT, Device, QualificationError,
    command, find_adb, verify_apk, apk_metadata)
from stability_android import require_emulator, require_framework, exit_events


def test_count(output):
    match = re.search(r'OK \((\d+) tests?\)', output)
    if not match or any(token in output for token in ('FAILURES!!!', 'INSTRUMENTATION_FAILED', 'Process crashed')):
        raise QualificationError('INSTRUMENTATION_SUITE_FAILED')
    return int(match[1])


def installed_hash(device, package):
    match = re.search(r'^package:(.+base\.apk)$', device.shell('pm', 'path', package), re.M)
    return device.shell('sha256sum', match[1]).split()[0] if match else None


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--serial', required=True)
    p.add_argument('--allow-disposable-install-and-tests', action='store_true')
    p.add_argument('--apk', type=Path, required=True)
    p.add_argument('--test-apk', type=Path, required=True)
    p.add_argument('--out', type=Path, required=True)
    args = p.parse_args()
    report = dict(schemaVersion=1, status='PENDING', physicalQualification='PENDING',
        runnerCommit=command(['git', '-C', ROOT, 'rev-parse', 'HEAD']),
        workingTreeDirty=bool(command(['git', '-C', ROOT, 'status', '--porcelain'])),
        destructiveTestScope='disposable AVD test credentials/layout/listener settings',
        scenarios=['onboarding', 'dashboard', 'widgets/presets/persistence', 'passive Device Lab',
            'voice catalog/text fallback', 'permissions/Bluetooth declarations', 'fixture MediaSession',
            'local Jarvis', 'Places demo', 'lifecycle/recreate'], testsPassed=None)
    try:
        device = Device(find_adb(), args.serial, 'device')
        require_emulator(device, args.allow_disposable_install_and_tests)
        avd = device.call('emu', 'avd', 'name').splitlines()[0]
        if not re.fullmatch(r'CopilotDrive_Sprint9_API(26|30|35)', avd):
            raise QualificationError('DEDICATED_DISPOSABLE_SPRINT9_AVD_REQUIRED')
        require_framework(device)
        report['environment'] = device.environment()
        report['apkSha256'] = verify_apk(args.apk, None)
        report['testApkSha256'] = verify_apk(args.test_apk, None)
        report['apkMetadata'] = apk_metadata(device.adb, args.apk)
        before_exits = exit_events(device.shell('dumpsys', 'activity', 'exit-info', PACKAGE)) if report['environment']['androidApi'] >= 30 else None
        device.call('install', '-r', args.apk, timeout=120)
        device.call('install', '-r', args.test_apk, timeout=120)
        for package, expected in ((PACKAGE, report['apkSha256']), (PACKAGE+'.test', report['testApkSha256'])):
            if installed_hash(device, package) != expected:
                raise QualificationError('PRE_TEST_APK_HASH_MISMATCH')
        def listeners():
            raw = device.shell('settings', 'get', 'secure', 'enabled_notification_listeners')
            return frozenset(value for value in raw.split(':') if value and value != 'null')
        original_listeners = listeners()  # Compare locally; never export other apps' identities.
        started = time.monotonic()
        output = device.shell('am', 'instrument', '-w', PACKAGE+'.test/androidx.test.runner.AndroidJUnitRunner', timeout=1200)
        # Local ignored failure logs only; never export conversations or arbitrary test output.
        try:
            report['testsPassed'] = test_count(output)
        except QualificationError:
            args.out.parent.mkdir(parents=True, exist_ok=True)
            args.out.with_suffix('.failure.log').write_text(output, encoding='utf-8')
            raise
        report['elapsedMs'] = int((time.monotonic()-started)*1000)
        report['notificationListenerRestoration'] = 'RESTORED' if listeners() == original_listeners else 'FAILED'
        if report['notificationListenerRestoration'] != 'RESTORED':
            raise QualificationError('NOTIFICATION_LISTENER_RESTORE_FAILED')
        for package, expected in ((PACKAGE, report['apkSha256']), (PACKAGE+'.test', report['testApkSha256'])):
            if installed_hash(device, package) != expected:
                raise QualificationError('POST_TEST_APK_HASH_MISMATCH')
        report['packagesPreserved'] = True
        device.shell('am', 'start', '-W', '-n', COMPONENT)
        require_framework(device)
        after_exits = exit_events(device.shell('dumpsys', 'activity', 'exit-info', PACKAGE)) if before_exits is not None else None
        reasons = [reason for key,reason in after_exits.items() if key not in before_exits] if after_exits is not None else None
        report['crashesObserved'] = sum(reason in (4,5) for reason in reasons) if reasons is not None else None
        report['anrsObserved'] = reasons.count(6) if reasons is not None else None
        report['crashObservation'] = 'bounded exit-info history' if reasons is not None else 'UNSUPPORTED_API; suite assertions only'
        report['status'] = 'FAILED' if report['crashesObserved'] or report['anrsObserved'] else 'EMULATOR_TESTED'
    except (QualificationError, OSError, ValueError) as error:
        report.update(status='FAILED' if str(error)=='INSTRUMENTATION_SUITE_FAILED' else 'BLOCKED',
            errorCode=str(error) if isinstance(error, QualificationError) else 'INVALID_ENVIRONMENT')
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
    print(json.dumps(dict(status=report['status'], tests=report['testsPassed'], evidence=args.out.name)))
    return 0 if report['status']=='EMULATOR_TESTED' else 1


if __name__ == '__main__':
    raise SystemExit(main())
