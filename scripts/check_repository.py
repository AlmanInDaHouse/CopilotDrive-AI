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
    assert permissions == {'android.permission.RECORD_AUDIO', 'android.permission.READ_CONTACTS'}, permissions
    assert manifest.find('application').attrib[ANDROID + 'usesCleartextTraffic'] == 'false'
    assert manifest.find('application').attrib[ANDROID + 'allowBackup'] == 'false'
    for source in (ROOT / 'android/core/src/main').rglob('*.kt'):
        assert 'import android.' not in source.read_text(encoding='utf-8'), source
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
