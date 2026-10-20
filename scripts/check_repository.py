"""Dependency-free repository policy checks, not a substitute for a security audit."""
import json
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'


def main():
    manifest = ET.parse(ROOT / 'android/app/src/main/AndroidManifest.xml').getroot()
    permissions = {p.attrib[ANDROID + 'name'] for p in manifest.findall('uses-permission')}
    assert permissions == {
        'android.permission.RECORD_AUDIO', 'android.permission.READ_CONTACTS', 'android.permission.INTERNET',
        'android.permission.BLUETOOTH', 'android.permission.BLUETOOTH_CONNECT',
    }, permissions
    legacy = next(p for p in manifest.findall('uses-permission') if p.attrib[ANDROID + 'name'] == 'android.permission.BLUETOOTH')
    assert legacy.attrib[ANDROID + 'maxSdkVersion'] == '30'
    assert not any(p.attrib[ANDROID + 'name'] in {'android.permission.BLUETOOTH_SCAN', 'android.permission.ACCESS_FINE_LOCATION'} for p in manifest.findall('uses-permission'))
    assert manifest.find('application').attrib[ANDROID + 'usesCleartextTraffic'] == 'false'
    assert manifest.find('application').attrib[ANDROID + 'allowBackup'] == 'false'
    main_network = ET.parse(ROOT / 'android/app/src/main/res/xml/network_security_config.xml').getroot()
    assert main_network.find('base-config').attrib['cleartextTrafficPermitted'] == 'false'
    debug_network = ET.parse(ROOT / 'android/app/src/debug/res/xml/network_security_config.xml').getroot()
    assert debug_network.find('base-config').attrib['cleartextTrafficPermitted'] == 'false'
    assert {d.text for d in debug_network.findall('domain-config/domain')} == {'localhost', '127.0.0.1', '10.0.2.2'}
    assert all(d.attrib.get('includeSubdomains') == 'false' for d in debug_network.findall('domain-config/domain'))
    for source in (ROOT / 'android/core/src/main').rglob('*.kt'):
        assert 'import android.' not in source.read_text(encoding='utf-8'), source
    wire = (ROOT / 'android/core/src/main/kotlin/ai/copilotdrive/core/BluetoothObd.kt').read_text(encoding='utf-8')
    assert '"04"' not in wire and '"03"' not in wire and 'VIN' not in wire
    app_dependencies = (ROOT / 'android/app/build.gradle.kts').read_text(encoding='utf-8')
    assert 'implementation(project(":testing"))' not in app_dependencies
    for source in (ROOT / 'android/app/src/main').rglob('*.kt'):
        assert 'ai.copilotdrive.testing' not in source.read_text(encoding='utf-8'), source
    for folder in (ROOT / 'specs').iterdir():
        if folder.is_dir():
            for filename in ('spec.md', 'contracts.md', 'acceptance.md', 'test-plan.md'):
                assert (folder / filename).is_file(), folder / filename
    schema = json.loads((ROOT / 'contracts/vehicle-snapshot-v1.schema.json').read_text(encoding='utf-8'))
    fixture = json.loads((ROOT / 'contracts/fixtures/vehicle-simulated-v1.json').read_text(encoding='utf-8'))
    assert set(fixture) == set(schema['required'])
    assert fixture['schemaVersion'] == schema['properties']['schemaVersion']['const']
    files = subprocess.check_output(['git', 'ls-files'], cwd=ROOT, text=True).splitlines()
    forbidden = [p for p in files if p.endswith(('.jks', '.keystore', 'local.properties', '.apk')) or p.startswith('.tools/') or p == '.env']
    assert not forbidden, forbidden
    print('Repository checks passed: permissions, network policy, module boundaries, specifications, fixture fields, generated/secret file exclusions.')


if __name__ == '__main__':
    main()
