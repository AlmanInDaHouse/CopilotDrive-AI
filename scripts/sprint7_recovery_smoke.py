"""Opt-in emulator-only rotation, foreground and offline local-query recovery.

Requires both debug APKs installed, completed onboarding and a saved layout.
Does not clear data or export UI/log text. Restores temporary rotation/network state.
"""
import argparse
from contextlib import contextmanager
import json
from pathlib import Path
import re
import time
from qualify_android import (ROOT, PACKAGE, COMPONENT, QualificationError, Device,
    command, find_adb, devices, verify_apk, layout_hash, temporary_display, ui_nodes, essential_bounds)


def transport_state(device):
    return dict(wifi=device.shell('settings', 'get', 'global', 'wifi_on'),
        data=device.shell('settings', 'get', 'global', 'mobile_data'))


def wait_transports(device, expected):
    deadline = time.monotonic() + 10
    while time.monotonic() < deadline:
        if transport_state(device) == expected:
            return True
        time.sleep(.1)
    return False


@contextmanager
def offline_transports(device, restoration):
    wifi = device.shell('settings', 'get', 'global', 'wifi_on')
    data = device.shell('settings', 'get', 'global', 'mobile_data')
    if wifi not in ('0', '1') or data not in ('0', '1'):
        raise QualificationError('NETWORK_STATE_NOT_RESTORABLE')
    restoration['original'] = dict(wifi=wifi, data=data)
    try:
        device.shell('svc', 'wifi', 'disable')
        device.shell('svc', 'data', 'disable')
        if not wait_transports(device, dict(wifi='0',data='0')):
            raise QualificationError('NETWORK_DISABLE_FAILED')
        restoration['disabledObserved'] = True
        yield
    finally:
        errors = []
        for transport, original in [('wifi', wifi), ('data', data)]:
            try:
                device.shell('svc', transport, 'enable' if original == '1' else 'disable')
            except QualificationError:
                errors.append(transport)
        restored = wait_transports(device, restoration['original'])
        restoration['final'] = transport_state(device)
        restoration['failedCommands'] = errors
        restoration['status'] = 'RESTORED' if restored and not errors else 'FAILED'
        if restoration['status'] != 'RESTORED':
            raise QualificationError('NETWORK_RESTORE_FAILED')


def display_observation(window, legacy_display=''):
    bounds = re.search(r'mBounds=Rect\(0, 0 - (\d+), (\d+)\)', window)
    rotation = re.search(r'mDisplayRotation=ROTATION_(\d+)', window)
    if bounds and rotation:
        width, height, degrees = int(bounds[1]), int(bounds[2]), int(rotation[1])
    else:
        # Android 8 has no `window displays` format. Use the logical override's
        # real dimensions, not base/natural or app dimensions excluding system bars.
        overrides = re.findall(r'mOverrideDisplayInfo=DisplayInfo\{([^\r\n]+)', legacy_display)
        if len(overrides) != 1:
            raise QualificationError('DISPLAY_OBSERVATION_UNAVAILABLE')
        real = re.search(r'\breal (\d+)\s*x\s*(\d+)', overrides[0])
        turn = re.search(r'\brotation ([0-3])(?:,|\s)', overrides[0])
        if not real or not turn:
            raise QualificationError('DISPLAY_OBSERVATION_UNAVAILABLE')
        width, height, degrees = int(real[1]), int(real[2]), int(turn[1])*90
    if width <= 0 or height <= 0 or degrees not in (0,90,180,270):
        raise QualificationError('DISPLAY_OBSERVATION_UNAVAILABLE')
    return dict(widthPx=width, heightPx=height, rotation=degrees)


def check_window(device):
    observed = display_observation(device.shell('dumpsys', 'window', 'displays'), device.shell('dumpsys', 'display'))
    essential_bounds(ui_nodes(device), observed['widthPx'], observed['heightPx'])
    return observed


def wait_window(device, expected_rotation=None):
    deadline = time.monotonic() + 15
    last_error = 'ESSENTIAL_CONTROL_MISSING'
    while time.monotonic() < deadline:
        try:
            observed = check_window(device)
            if expected_rotation is None or observed['rotation'] == expected_rotation:
                return observed
            last_error = 'ROTATION_NOT_OBSERVED'
        except QualificationError as error:
            if str(error) not in ('ESSENTIAL_CONTROL_MISSING', 'UI_DUMP_FAILED'):
                raise
            last_error = str(error)
        time.sleep(.25)
    raise QualificationError(last_error)


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--serial', required=True); p.add_argument('--adb')
    p.add_argument('--apk', type=Path, required=True); p.add_argument('--expected-sha256', required=True)
    p.add_argument('--allow-active', action='store_true'); p.add_argument('--dry-run', action='store_true')
    p.add_argument('--out', type=Path, default=ROOT / 'artifacts/sprint7/recovery.json')
    args = p.parse_args(argv)
    report = dict(schemaVersion=1, status='PENDING', evidenceLevel='EMULATOR', physicalQualification='PENDING',
        commit=command(['git', '-C', ROOT, 'rev-parse', 'HEAD']),
        preconditions=['emulator only; installed test APK; onboarded simulator; saved layout',
            'explicit active opt-in; no microphone/OBD/paid queries; no data clear'],
        validationMethod='ADB_AND_INSTRUMENTATION')
    try:
        adb = find_adb(args.adb)
        if dict(devices(command([adb, 'devices', '-l']))).get(args.serial) != 'device':
            raise QualificationError('TARGET_NOT_AUTHORIZED')
        device = Device(adb, args.serial, 'device')
        report['environment'] = device.environment()
        if device.kind() != 'EMULATOR' or not args.allow_active:
            raise QualificationError('EXPLICIT_EMULATOR_ACTIVITY_REQUIRED')
        report['apkSha256'] = verify_apk(args.apk, args.expected_sha256)
        installed = re.search(r'^package:(.+base\.apk)$', device.shell('pm', 'path', PACKAGE), re.M)
        if not installed or device.shell('sha256sum', installed[1]).split()[0] != report['apkSha256']:
            raise QualificationError('INSTALLED_APK_HASH_MISMATCH')
        if args.dry_run:
            report['status'] = 'DRY_RUN'
        else:
            device.shell('am', 'start', '-W', '-n', COMPONENT)
            wait_window(device)
            before = layout_hash(device)
            report['rotations'] = []
            with temporary_display(device) as restored:
                report['displayRestoration'] = restored
                device.shell('settings', 'put', 'system', 'accelerometer_rotation', '0')
                for value in ('1', '0'):
                    device.shell('settings', 'put', 'system', 'user_rotation', value)
                    item = wait_window(device, int(value)*90)
                    report['rotations'].append(item)
            device.shell('input', 'keyevent', 'KEYCODE_HOME')
            device.shell('am', 'start', '-W', '-n', COMPONENT)
            wait_window(device)
            if layout_hash(device) != before:
                raise QualificationError('LAYOUT_CHANGED_ON_RECOVERY')
            report['foregroundRecovery'] = 'PASS'
            report['networkRestoration'] = {}
            with offline_transports(device, report['networkRestoration']):
                result = device.shell('am', 'instrument', '-w', '-e', 'class',
                    'ai.copilotdrive.app.AppFlowTest#onboardingToFuelQueryThroughRealViewModel',
                    'ai.copilotdrive.app.test/androidx.test.runner.AndroidJUnitRunner', timeout=90)
                if 'OK (1 test)' not in result:
                    raise QualificationError('OFFLINE_QUERY_FAILED')
                report['offlineLocalQuery'] = dict(tests=1, failures=0, networkTransportsDisabled=True)
            device.shell('am', 'start', '-W', '-n', COMPONENT)
            process = device.shell('pidof', PACKAGE)
            if not process.isdigit():
                raise QualificationError('APP_PROCESS_NOT_RUNNING')
            crash = device.call('logcat', '-d', '-b', 'crash', '--pid=' + process)
            if 'FATAL EXCEPTION' in crash:
                raise QualificationError('APP_CRASH_OBSERVED')
            report.update(status='EMULATOR_TESTED', appCrashObserved=False)
    except (QualificationError, ValueError, OSError) as failure:
        report.update(status='BLOCKED', errorCode=str(failure) if isinstance(failure, QualificationError) else 'INVALID_ENVIRONMENT')
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(dict(status=report['status'], evidence=args.out.name)))
    return 1 if report['status'] == 'BLOCKED' else 0


if __name__ == '__main__':
    raise SystemExit(main())
