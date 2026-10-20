"""Bounded Android connectivity-disabled test B, restricted to the disposable API26 AVD.

Does not install, root, reboot, clear data or call remote services. Original transport
flags are persisted before mutation. Flags AND ConnectivityService must agree offline.
Exit137 is retained as a command failure, never interpreted as proof of OOM.
"""
import argparse
from contextlib import contextmanager
import json
from pathlib import Path
import re
import subprocess
import time
from qualify_android import Device, QualificationError, find_adb, verify_apk, PACKAGE, ROOT, command, ui_nodes
from compatibility_android import installed_hash, test_count
from stability_android import require_framework, require_emulator


def connectivity(raw):
    default = re.search(r'^Active default network: (none|\d+)\s*$', raw, re.M)
    current = re.search(r'Current Networks:\s*(.*?)\s*Network Requests:', raw, re.S)
    if not default or not current:
        raise QualificationError('CONNECTIVITY_FORMAT_UNSUPPORTED')
    return dict(activeDefault=default[1] != 'none',
        connectedNetworks=len(re.findall(r'NetworkAgentInfo\{[^\n]*state: CONNECTED/CONNECTED', current[1])))


def snapshot(device):
    flags = {key: device.shell('settings', 'get', 'global', name)
        for key, name in [('wifi', 'wifi_on'), ('data', 'mobile_data')]}
    if any(value not in ('0', '1') for value in flags.values()):
        raise QualificationError('NETWORK_STATE_NOT_RESTORABLE')
    return dict(flags=flags, **connectivity(device.shell('dumpsys', 'connectivity')))


def is_offline(state):
    return state['flags'] == dict(wifi='0', data='0') and not state['activeDefault'] and state['connectedNetworks'] == 0


def svc(device, transport, value):
    try:
        run = subprocess.run([device.adb, '-s', device.serial, 'shell', 'svc', transport,
            'enable' if value == '1' else 'disable'], capture_output=True, timeout=30)
        return dict(transport=transport, requested=value, exitCode=run.returncode)
    except (subprocess.TimeoutExpired, OSError):
        return dict(transport=transport, requested=value, exitCode=None, errorCode='COMMAND_UNAVAILABLE_OR_TIMEOUT')


def settings_wifi(device, transport, value):
    """Ordinary Settings UI on the gated disposable AVD; never grants shell privileges."""
    if transport != 'wifi':
        return svc(device, transport, value)
    try:
        device.shell('am', 'start', '-W', '-a', 'android.settings.WIFI_SETTINGS')
        for _ in range(3):
            switches = [n for n in ui_nodes(device) if n.get('resource-id') == 'com.android.settings:id/switch_widget'
                and n.get('class') == 'android.widget.Switch' and n.get('clickable') == 'true']
            if len(switches) != 1:
                raise QualificationError('WIFI_SETTINGS_SWITCH_NOT_AVAILABLE')
            node = switches[0]
            if node.get('checked') == ('true' if value == '1' else 'false'):
                flag = device.shell('settings', 'get', 'global', 'wifi_on')
                wifi = device.shell('dumpsys', 'wifi')
                if flag == value and ('Wi-Fi is enabled' if value == '1' else 'Wi-Fi is disabled') in wifi.splitlines()[0]:
                    return dict(transport='wifi', requested=value, exitCode=0, method='SETTINGS_UI')
            else:
                bounds = list(map(int, re.findall(r'\d+', node.get('bounds', ''))))
                if len(bounds) != 4 or bounds[2] <= bounds[0] or bounds[3] <= bounds[1]:
                    raise QualificationError('WIFI_SWITCH_BOUNDS_INVALID')
                device.shell('input', 'tap', str((bounds[0]+bounds[2])//2), str((bounds[1]+bounds[3])//2))
            time.sleep(.25)
        raise QualificationError('WIFI_SETTINGS_STATE_NOT_OBSERVED')
    except (QualificationError, OSError, ValueError):
        return dict(transport='wifi', requested=value, exitCode=None, method='SETTINGS_UI', errorCode='WIFI_UI_UNAVAILABLE')


def await_state(device, predicate):
    deadline = time.monotonic() + 15
    while True:
        state = snapshot(device)
        if predicate(state) or time.monotonic() >= deadline:
            return state
        time.sleep(.25)


@contextmanager
def disconnected(device, report, persist, action=svc):
    original = snapshot(device)
    report['original'] = original
    report['restoration'] = 'PENDING'
    persist()  # Must succeed BEFORE any active command.
    report['commands'] = []
    try:
        for transport in ('wifi', 'data'):
            result = action(device, transport, '0')
            report['commands'].append(result)
            persist()
            if result['exitCode'] != 0:
                raise QualificationError('TRANSPORT_COMMAND_FAILED')
        report['disabled'] = await_state(device, is_offline)
        persist()
        if not is_offline(report['disabled']):
            raise QualificationError('ANDROID_OFFLINE_NOT_OBSERVED')
        yield
    finally:
        report['restoreCommands'] = []
        for transport in ('wifi', 'data'):
            report['restoreCommands'].append(action(device, transport, original['flags'][transport]))
        persist()
        report['final'] = await_state(device, lambda s: s['flags'] == original['flags'] and s['activeDefault'] == original['activeDefault'])
        restored = report['final']['flags'] == original['flags'] and report['final']['activeDefault'] == original['activeDefault']
        report['restoration'] = 'RESTORED' if restored and all(r['exitCode'] == 0 for r in report['restoreCommands']) else 'FAILED'
        persist()
        if report['restoration'] != 'RESTORED':
            raise QualificationError('NETWORK_RESTORATION_FAILED')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--allow-disposable-network-test', action='store_true')
    parser.add_argument('--wifi-settings-ui', action='store_true', help='Use observed Settings switch instead of unsupported shell Wi-Fi operation')
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    report = dict(schemaVersion=1, status='BLOCKED', testA='SEPARATE_JVM_EVIDENCE', testB='BLOCKED',
        physicalQualification='PENDING', runnerCommit=command(['git', '-C', ROOT, 'rev-parse', 'HEAD']),
        workingTreeDirty=bool(command(['git', '-C', ROOT, 'status', '--porcelain'])), network={})
    args.out.parent.mkdir(parents=True, exist_ok=True)
    def persist():
        args.out.write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
    try:
        device = Device(find_adb(), args.serial, 'device')
        require_emulator(device, args.allow_disposable_network_test)
        if device.call('emu', 'avd', 'name').splitlines()[0] != 'CopilotDrive_Sprint9_API26' or device.shell('getprop', 'ro.build.version.sdk') != '26':
            raise QualificationError('DEDICATED_DISPOSABLE_API26_REQUIRED')
        require_framework(device)
        report.update(environment=device.environment(), apkSha256=verify_apk(args.apk, None),
            testApkSha256=installed_hash(device, PACKAGE+'.test'))
        if installed_hash(device, PACKAGE) != report['apkSha256'] or not report['testApkSha256']:
            raise QualificationError('INSTALLED_APKS_REQUIRED')
        report['wifiMethod'] = 'SETTINGS_UI' if args.wifi_settings_ui else 'SVC'
        with disconnected(device, report['network'], persist, settings_wifi if args.wifi_settings_ui else svc):
            require_framework(device)
            started = time.monotonic()
            output = device.shell('am', 'instrument', '-w', '-e', 'class',
                'ai.copilotdrive.app.AppFlowTest#onboardingToFuelQueryThroughRealViewModel',
                PACKAGE+'.test/androidx.test.runner.AndroidJUnitRunner', timeout=120)
            report['testsPassed'] = test_count(output)
            report['elapsedMs'] = int((time.monotonic()-started)*1000)
            report['network']['afterQuery'] = snapshot(device)
            if report['testsPassed'] != 1 or not is_offline(report['network']['afterQuery']):
                raise QualificationError('OFFLINE_QUERY_NOT_VERIFIED')
            report['testB'] = 'PASS'
        report['status'] = 'EMULATOR_TESTED'
    except (QualificationError, OSError, ValueError) as error:
        report['errorCode'] = str(error) if isinstance(error, QualificationError) else 'ENVIRONMENT_UNAVAILABLE'
        report['testB'] = 'FAILED' if report['errorCode'] == 'INSTRUMENTATION_SUITE_FAILED' else 'BLOCKED'
    persist()
    print(json.dumps(dict(status=report['status'], testB=report['testB'], evidence=args.out.name)))
    return 0 if report['status'] == 'EMULATOR_TESTED' else 1


if __name__ == '__main__':
    raise SystemExit(main())
