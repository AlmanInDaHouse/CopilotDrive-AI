"""Read-only candidate inspection and optional installed-signature conflict check.

Never installs, launches, grants permissions, changes phone data or records audio.
Physical installation and activity still require the user's explicit authorization.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import tempfile
from qualify_android import Device, QualificationError, find_adb, command, devices, apk_metadata, PACKAGE, ROOT


def signature_conflict(candidate, installed):
    return installed is not None and set(candidate) != set(installed)


def inspect_apk(adb, path):
    metadata = apk_metadata(adb, path)
    sdk = Path(adb).parent.parent
    build = next(b for b in sorted((sdk/'build-tools').glob('*'), reverse=True) if (b/'aapt.exe').is_file())
    manifest_permissions = command([build/'aapt.exe', 'dump', 'permissions', path])
    permissions = sorted(set(re.findall(r"uses-permission(?:-sdk-\d+)?: name='([^']+)'", manifest_permissions)))
    expected = {f'android.permission.{name}' for name in ('RECORD_AUDIO', 'READ_CONTACTS', 'INTERNET',
        'ACCESS_COARSE_LOCATION', 'BLUETOOTH', 'BLUETOOTH_CONNECT', 'ACCESS_NETWORK_STATE', 'ACCESS_WIFI_STATE')}
    expected.add(PACKAGE+'.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION')
    if set(permissions) != expected:
        raise QualificationError('APK_PERMISSION_ALLOWLIST_MISMATCH')
    metadata.update(packageId=PACKAGE, sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
        bytes=path.stat().st_size, permissions=permissions)
    return metadata


def installed_certificates(device):
    # Some Android versions return exit 1 (with no output) for pm path when
    # the app is absent. Establish absence through a successful package query;
    # never treat a failed query or an inaccessible installed APK as absence.
    listing = device.shell('pm', 'list', 'packages', PACKAGE)
    packages = listing.splitlines()
    if any(not re.fullmatch(r'package:[A-Za-z0-9_.]+', line) for line in packages):
        raise QualificationError('INSTALLED_PACKAGE_STATE_UNAVAILABLE')
    if 'package:'+PACKAGE not in packages:
        return None
    paths = re.findall(r'^package:(.+)$', device.shell('pm', 'path', PACKAGE), re.M)
    if not paths:
        raise QualificationError('INSTALLED_BASE_APK_UNAVAILABLE')
    base = next((p.strip() for p in paths if p.strip().endswith('/base.apk')), None)
    if base is None:
        raise QualificationError('INSTALLED_BASE_APK_UNAVAILABLE')
    build = next(b for b in sorted((Path(device.adb).parent.parent/'build-tools').glob('*'), reverse=True)
        if (b/'apksigner.bat').is_file())
    with tempfile.TemporaryDirectory(prefix='copilot-pilot-signature-') as folder:
        path = Path(folder)/'installed.apk'
        device.call('pull', base, path, timeout=120)
        result = command([build/'apksigner.bat', 'verify', '--print-certs', path])
        certs = re.findall(r'certificate SHA-256 digest: ([0-9a-fA-F]+)', result)
        if not certs:
            raise QualificationError('INSTALLED_SIGNATURE_NOT_VERIFIED')
        return [c.lower() for c in certs]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', required=True, type=Path)
    parser.add_argument('--serial', help='Optional authorized target for passive model/API/package/signature check')
    parser.add_argument('--out', required=True, type=Path)
    args = parser.parse_args()
    report = dict(schemaVersion=1, physicalQualification='PENDING', status='PENDING',
        codeCommit=command(['git','-C',ROOT,'rev-parse','HEAD']),
        workingTreeDirty=bool(command(['git','-C',ROOT,'status','--porcelain'])),
        activeActionsExecuted=False, physicalPerformanceAuthorization='PENDING')
    try:
        adb = find_adb()
        report['candidate'] = inspect_apk(adb, args.apk)
        if args.serial:
            if dict(devices(command([adb,'devices','-l']))).get(args.serial) != 'device':
                raise QualificationError('TARGET_NOT_AUTHORIZED')
            device = Device(adb, args.serial, 'device')
            report['environment'] = device.environment()
            report['installedCertificatesSha256'] = installed_certificates(device)
            report['signatureConflict'] = signature_conflict([report['candidate']['certificateSha256']], report['installedCertificatesSha256'])
            report['minSdkCompatible'] = report['environment']['androidApi'] >= report['candidate']['minSdk']
            if report['signatureConflict'] or not report['minSdkCompatible']:
                raise QualificationError('SIGNATURE_CONFLICT' if report['signatureConflict'] else 'MIN_SDK_UNSUPPORTED')
        report['preflight'] = 'PASS'
    except (QualificationError, OSError, ValueError, StopIteration) as error:
        report.update(status='BLOCKED', errorCode=str(error) if isinstance(error, QualificationError) else 'INSPECTION_UNAVAILABLE')
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report))
    return 1 if report['status']=='BLOCKED' else 0


if __name__ == '__main__':
    raise SystemExit(main())
