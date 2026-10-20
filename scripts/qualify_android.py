"""Redacted Android qualification. Passive by default; no audio/OBD/network queries.

Run --help. Artifacts stay under ignored artifacts/sprint7. No app data is cleared.
Physical activity requires explicit --allow-physical AND --allow-active after human
confirmation. This program can never emit PHYSICALLY_VERIFIED.
"""
from __future__ import annotations

import argparse
from contextlib import contextmanager
import hashlib
import json
import math
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'ai.copilotdrive.app'
COMPONENT = PACKAGE + '/.MainActivity'


class QualificationError(Exception):
    """Only fixed, non-sensitive error codes may cross the export boundary."""


def command(argv, timeout=30):
    try:
        result = subprocess.run(list(map(str, argv)), capture_output=True, timeout=timeout)
    except (OSError, subprocess.TimeoutExpired):
        raise QualificationError('COMMAND_UNAVAILABLE_OR_TIMEOUT') from None
    if result.returncode:
        raise QualificationError('COMMAND_FAILED')
    return result.stdout.decode('utf-8', errors='replace').strip()


def find_adb(explicit=None):
    candidates = [explicit, shutil.which('adb')]
    for env in ('ANDROID_SDK_ROOT', 'ANDROID_HOME'):
        if os.getenv(env):
            candidates.append(str(Path(os.environ[env]) / 'platform-tools/adb.exe'))
    properties = ROOT / 'android/local.properties'
    if properties.exists():
        for line in properties.read_text(encoding='utf-8').splitlines():
            if line.startswith('sdk.dir='):
                sdk = line.split('=', 1)[1].replace('\\:', ':').replace('\\\\', '\\')
                candidates.append(str(Path(sdk) / 'platform-tools/adb.exe'))
    candidates.append(str(ROOT / '.tools/android-sdk/platform-tools/adb.exe'))
    for item in candidates:
        if item and Path(item).is_file():
            return str(Path(item).resolve())
    raise QualificationError('ADB_NOT_FOUND')


def devices(output):
    # Do not export serials, transport ids or raw adb error text.
    parsed = []
    for line in output.splitlines()[1:]:
        columns = line.split()
        if len(columns) >= 2 and not line.startswith('*'):
            parsed.append((columns[0], columns[1]))
    return parsed


class Device:
    def __init__(self, adb, serial, state):
        self.adb, self.serial, self.state = adb, serial, state

    def call(self, *args, timeout=30):
        return command([self.adb, '-s', self.serial, *args], timeout)

    def shell(self, *args, timeout=30):
        return self.call('shell', *args, timeout=timeout)

    def kind(self):
        # Require positive emulator properties; a serial prefix alone is insufficient.
        return 'EMULATOR' if (self.serial.startswith('emulator-') and
            self.shell('getprop', 'ro.kernel.qemu') == '1') else 'PHYSICAL'

    def environment(self):
        return dict(kind=self.kind(), androidApi=int(self.shell('getprop', 'ro.build.version.sdk')),
            model=self.shell('getprop', 'ro.product.model')[:80],
            androidVersion=self.shell('getprop', 'ro.build.version.release')[:20],
            size=self.shell('wm', 'size'), density=self.shell('wm', 'density'),
            fontScale=self.shell('settings', 'get', 'system', 'font_scale'))


def authorize(kind, args):
    if args.install and not args.allow_install:
        raise QualificationError('INSTALL_REQUIRES_ALLOW_INSTALL')
    active = args.install or args.launch or args.samples > 0 or args.matrix
    if active and not args.allow_active:
        raise QualificationError('ACTIVE_TEST_REQUIRES_ALLOW_ACTIVE')
    if active and kind == 'PHYSICAL' and not args.allow_physical:
        raise QualificationError('PHYSICAL_TEST_REQUIRES_CONFIRMATION')
    if args.matrix and kind != 'EMULATOR':
        raise QualificationError('DISPLAY_MATRIX_EMULATOR_ONLY')


def verify_apk(path, expected):
    digest = hashlib.sha256(Path(path).read_bytes()).hexdigest()
    if expected and digest.lower() != expected.lower():
        raise QualificationError('APK_HASH_MISMATCH')
    return digest


def apk_metadata(adb, path):
    sdk = Path(adb).parent.parent
    builds = sorted((sdk / 'build-tools').glob('*'), reverse=True)
    build = next((b for b in builds if (b / 'aapt.exe').is_file() and (b / 'apksigner.bat').is_file()), None)
    if build is None:
        raise QualificationError('APK_INSPECTION_TOOLS_NOT_FOUND')
    if not os.getenv('JAVA_HOME'):
        jdks = list((ROOT / '.tools/jdk').glob('*'))
        if jdks:
            os.environ['JAVA_HOME'] = str(jdks[0])
    badging = command([build / 'aapt.exe', 'dump', 'badging', path])
    minimum, target = re.search(r"sdkVersion:'(\d+)'", badging), re.search(r"targetSdkVersion:'(\d+)'", badging)
    package = re.search(r"package: name='([^']+)'", badging)
    if not minimum or not target or not package or package[1] != PACKAGE:
        raise QualificationError('APK_METADATA_INVALID')
    signature = command([build / 'apksigner.bat', 'verify', '--verbose', '--print-certs', path])
    certificate = re.search(r'certificate SHA-256 digest: ([0-9a-fA-F]+)', signature)
    if not certificate or 'CN=Android Debug' not in signature:
        raise QualificationError('EXPECTED_SIGNED_DEBUG_APK')
    return dict(minSdk=int(minimum[1]), targetSdk=int(target[1]), debugSignatureVerified=True,
        certificateSha256=certificate[1])


def summary(values):
    values = sorted(values)
    result = dict(count=len(values))
    if len(values) >= 20:
        result.update(p50=values[math.ceil(len(values) * .5) - 1], p95=values[math.ceil(len(values) * .95) - 1])
    return result


def ui_nodes(device):
    # Stream only; no screenshots or provider/conversation text retained in evidence.
    temporary = '/data/local/tmp/copilot-qualification.xml'
    try:
        output = device.shell('uiautomator', 'dump', temporary)
        if 'dumped' not in output.lower():
            raise QualificationError('UI_DUMP_FAILED')
        return list(ET.fromstring(device.shell('cat', temporary)).iter('node'))
    finally:
        device.shell('rm', '-f', temporary)


def essential_bounds(nodes, width, height):
    rectangles = []
    # Explicit labels, independent of telemetry values or conversation text.
    for label in ('Pulsar para hablar', 'Ajustes', 'SIMULADOR', 'Micrófono apagado'):
        found = [n for n in nodes if label in n.get('text', '') or label in n.get('content-desc', '')]
        if not found:
            raise QualificationError('ESSENTIAL_CONTROL_MISSING')
        bounds = list(map(int, re.findall(r'\d+', found[0].get('bounds', ''))))
        if len(bounds) != 4:
            raise QualificationError('INVALID_BOUNDS')
        x1, y1, x2, y2 = bounds
        if not (0 <= x1 < x2 <= width and 0 <= y1 < y2 <= height):
            raise QualificationError('ESSENTIAL_CONTROL_CLIPPED')
        for ox1, oy1, ox2, oy2 in rectangles:
            if not (x2 <= ox1 or ox2 <= x1 or y2 <= oy1 or oy2 <= y1):
                raise QualificationError('ESSENTIAL_CONTROLS_OVERLAP')
        rectangles.append(bounds)


@contextmanager
def temporary_display(device):
    before = dict(size=device.shell('wm', 'size'), density=device.shell('wm', 'density'),
        font=device.shell('settings', 'get', 'system', 'font_scale'),
        rotation=device.shell('settings', 'get', 'system', 'accelerometer_rotation'),
        userRotation=device.shell('settings', 'get', 'system', 'user_rotation'))
    restoration = {}
    try:
        yield restoration
    finally:
        errors = []
        for key in ('size', 'density'):
            match = re.search(r'Override (?:size|density): (\S+)', before[key])
            try:
                device.shell('wm', key, match.group(1) if match else 'reset')
            except QualificationError:
                errors.append(key)
        for name, key in [('font_scale', 'font'), ('accelerometer_rotation', 'rotation'), ('user_rotation', 'userRotation')]:
            try:
                if before[key] == 'null':
                    device.shell('settings', 'delete', 'system', name)
                else:
                    device.shell('settings', 'put', 'system', name, before[key])
            except QualificationError:
                errors.append(name)
        restoration['status'] = 'FAILED' if errors else 'RESTORED'
        restoration['failedSettings'] = errors
        # Verification, not just attempted restore.
        if not errors:
            after = dict(size=device.shell('wm', 'size'), density=device.shell('wm', 'density'),
                font=device.shell('settings', 'get', 'system', 'font_scale'),
                rotation=device.shell('settings', 'get', 'system', 'accelerometer_rotation'),
                userRotation=device.shell('settings', 'get', 'system', 'user_rotation'))
            restoration['status'] = 'RESTORED' if after == before else 'FAILED'
        if restoration['status'] != 'RESTORED':
            raise QualificationError('DISPLAY_RESTORE_FAILED')


def layout_hash(device):
    # Only hash the non-sensitive layout file, never preferences, credentials or history.
    return device.shell('run-as', PACKAGE, 'sha256sum', 'files/datastore/dashboard_layout.preferences_pb').split()[0]


def matrix(device, report):
    report['screens'] = []
    before = layout_hash(device)
    with temporary_display(device) as restoration:
        report['restoration'] = restoration
        device.shell('settings', 'put', 'system', 'accelerometer_rotation', '0')
        device.shell('settings', 'put', 'system', 'user_rotation', '0')
        for width, height, dpi, font in [(800,480,160,1), (1024,600,160,1), (1280,720,160,1),
            (1920,1080,160,1), (360,640,160,1), (1280,800,160,1), (800,480,160,1.5),
            (1280,720,240,1), (800,480,160,2), (1080,1920,480,1.5), (640,360,160,1)]:
            item = dict(widthPx=width, heightPx=height, densityDpi=dpi, fontScale=font, status='PENDING')
            report['screens'].append(item)
            device.shell('wm', 'size', f'{width}x{height}')
            device.shell('wm', 'density', str(dpi))
            device.shell('settings', 'put', 'system', 'font_scale', str(font))
            device.shell('am', 'force-stop', PACKAGE)
            device.shell('am', 'start', '-W', '-n', COMPONENT)
            time.sleep(.5)
            try:
                essential_bounds(ui_nodes(device), width, height)
                if layout_hash(device) != before:
                    raise QualificationError('LAYOUT_CHANGED_ON_PROCESS_RESTART')
                item['status'] = 'EMULATOR_TESTED'
            except QualificationError as failure:
                item.update(status='FAILED', errorCode=str(failure))
            print(f"screen {width}x{height}/{dpi}/{font}: {item['status']}", flush=True)


def benchmark(device, count, report):
    samples = []
    report['performance'] = dict(method='am start -W after force-stop; UI probe upper bound; dumpsys meminfo',
        conditions='debug APK; process-cold, caches warm; onboarded simulator; no network calls; sequential', samples=samples)
    for index in range(count):
        device.shell('am', 'force-stop', PACKAGE)
        started = time.perf_counter_ns()
        output = device.shell('am', 'start', '-W', '-n', COMPONENT)
        total = re.search(r'TotalTime:\s*(\d+)', output)
        if not total or 'Status: ok' not in output:
            raise QualificationError('START_MEASUREMENT_UNAVAILABLE')
        nodes = ui_nodes(device)
        if not any('Pulsar para hablar' in n.get('text', '') for n in nodes):
            raise QualificationError('DASHBOARD_NOT_READY_OR_NOT_ONBOARDED')
        upper = (time.perf_counter_ns() - started) // 1_000_000
        memory = device.shell('dumpsys', 'meminfo', PACKAGE)
        pss = re.search(r'TOTAL PSS:\s*(\d+)', memory) or re.search(r'^\s*TOTAL\s+(\d+)', memory, re.M)
        if not pss:
            raise QualificationError('MEMORY_MEASUREMENT_UNAVAILABLE')
        samples.append(dict(iteration=index + 1, activityStartMs=int(total[1]), dashboardReadyUpperBoundMs=upper, memoryPssKb=int(pss[1])))
        print(f'sample {index + 1}/{count}', flush=True)
    report['performance']['summary'] = {key: summary([s[key] for s in samples]) for key in
        ('activityStartMs', 'dashboardReadyUpperBoundMs', 'memoryPssKb')}


def parser():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--adb'); p.add_argument('--serial')
    p.add_argument('--apk', type=Path); p.add_argument('--expected-sha256')
    for flag in ('install', 'launch', 'matrix', 'allow-install', 'allow-active', 'allow-physical', 'dry-run'):
        p.add_argument('--' + flag, action='store_true')
    p.add_argument('--samples', type=int, default=0, choices=range(0, 101))
    p.add_argument('--out', type=Path, default=ROOT / 'artifacts/sprint7/qualification.json')
    return p


def main(argv=None):
    args = parser().parse_args(argv)
    report = dict(schemaVersion=1, status='PENDING', evidenceLevel='PENDING',
        timestampUtc=time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
        commit=command(['git', '-C', ROOT, 'rev-parse', 'HEAD']),
        workingTreeDirty=bool(command(['git', '-C', ROOT, 'status', '--porcelain'])),
        preconditions=['explicit target for active work', 'no audio, OBD or external service actions', 'no data clear'],
        validationMethod='ADB_MEASUREMENT', physicalQualification='PENDING')
    try:
        adb = find_adb(args.adb)
        inventory = devices(command([adb, 'devices', '-l']))
        report['inventory'] = [dict(alias=f'target-{i + 1}', authorization=state,
            transport='emulator' if serial.startswith('emulator-') else 'device') for i, (serial, state) in enumerate(inventory)]
        if args.apk:
            report['apkSha256'] = verify_apk(args.apk, args.expected_sha256)
            report['apk'] = apk_metadata(adb, args.apk)
        active = args.install or args.launch or args.matrix or args.samples
        if active and not args.serial:
            raise QualificationError('ACTIVE_TEST_REQUIRES_EXPLICIT_TARGET')
        if args.serial:
            state = dict(inventory).get(args.serial)
            if state != 'device':
                raise QualificationError('TARGET_NOT_AUTHORIZED_OR_OFFLINE')
            device = Device(adb, args.serial, state)
            report['environment'] = device.environment()
            authorize(report['environment']['kind'], args)
            if active and not args.apk:
                raise QualificationError('ACTIVE_TEST_REQUIRES_APK_PROVENANCE')
            if args.install and not args.expected_sha256:
                raise QualificationError('INSTALL_REQUIRES_EXPECTED_HASH')
            if active and report['environment']['androidApi'] < report['apk']['minSdk']:
                raise QualificationError('ANDROID_API_BELOW_MIN_SDK')
            if not args.dry_run:
                if args.install:
                    # Check SDK compatibility before attempting installation. No downgrade or data clear.
                    device.call('install', '-r', str(args.apk.resolve()), timeout=120)
                if active:
                    installed = device.shell('pm', 'path', PACKAGE)
                    match = re.search(r'^package:(.+base\.apk)$', installed, re.M)
                    if not match:
                        raise QualificationError('INSTALLED_APK_NOT_FOUND')
                    installed_hash = device.shell('sha256sum', match[1]).split()[0]
                    if installed_hash != report['apkSha256']:
                        raise QualificationError('INSTALLED_APK_HASH_MISMATCH')
                if args.launch:
                    launched = device.shell('am', 'start', '-W', '-n', COMPONENT)
                    if 'Status: ok' not in launched:
                        raise QualificationError('LAUNCH_FAILED')
                if args.matrix:
                    matrix(device, report)
                if args.samples:
                    benchmark(device, args.samples, report)
            report['status'] = 'DRY_RUN' if args.dry_run else ('EMULATOR_TESTED' if active and device.kind() == 'EMULATOR' else 'PENDING')
            report['evidenceLevel'] = 'EMULATOR' if active and not args.dry_run and device.kind() == 'EMULATOR' else 'PENDING'
            if any(s['status'] == 'FAILED' for s in report.get('screens', [])):
                report['status'] = 'FAILED'
    except (QualificationError, ValueError, ET.ParseError, OSError) as failure:
        report.update(status='BLOCKED', errorCode=str(failure) if isinstance(failure, QualificationError) else 'INVALID_ENVIRONMENT_OR_ARTIFACT')
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report, indent=2, ensure_ascii=True) + '\n', encoding='utf-8')
    print(json.dumps(dict(status=report['status'], evidence=args.out.name)))
    return 1 if report['status'] in ('BLOCKED', 'FAILED') else 0


if __name__ == '__main__':
    raise SystemExit(main())
