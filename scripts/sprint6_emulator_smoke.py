"""Local emulator evidence only; no physical compatibility claims.

Prerequisites: install both debug APKs and run
DashboardCustomizationTest#addReorderResizeHidePreviewSaveRecreateRestoreAndCancel
directly with adb am instrument (Gradle's connected task uninstalls the app).
Run from the repository root with the emulator initially at 1280x800/160 dpi.
"""
from pathlib import Path
import hashlib
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ADB = str(ROOT / '.tools/android-sdk/platform-tools/adb.exe')
SERIAL = subprocess.check_output([ADB, 'get-serialno'], text=True).strip()
if not SERIAL.startswith('emulator-'):
    raise RuntimeError('This smoke script changes display/network settings and only supports an emulator.')
OUT = ROOT / 'artifacts/sprint6'
OUT.mkdir(parents=True, exist_ok=True)

def adb(*args, binary=False):
    p = subprocess.run([ADB, '-s', SERIAL, *args], capture_output=True, check=True)
    return p.stdout if binary else p.stdout.decode('utf-8', errors='replace').strip()

def layout_hash():
    return hashlib.sha256(adb('exec-out', 'run-as', 'ai.copilotdrive.app', 'cat',
        'files/datastore/dashboard_layout.preferences_pb', binary=True)).hexdigest()

def capture(name):
    (OUT / f'{name}.png').write_bytes(adb('exec-out', 'screencap', '-p', binary=True))

def dump():
    adb('shell', 'uiautomator', 'dump', '/sdcard/Download/copilot-sprint6-ui.xml')
    return adb('shell', 'cat', '/sdcard/Download/copilot-sprint6-ui.xml')

def validate_fixed(xml, width, height):
    nodes = list(ET.fromstring(xml).iter('node'))
    rectangles = []
    for wanted in ['Pulsar para hablar', 'Ajustes', 'SIMULADOR']:
        found = [n for n in nodes if wanted in n.get('text', '') or wanted in n.get('content-desc', '')]
        assert found, f'Missing essential control: {wanted}'
        bounds = re.findall(r'\d+', found[0].get('bounds', ''))
        x1, y1, x2, y2 = map(int, bounds)
        assert 0 <= x1 < x2 <= width and 0 <= y1 < y2 <= height, (wanted, bounds)
        for other, (ox1, oy1, ox2, oy2) in rectangles:
            assert x2 <= ox1 or ox2 <= x1 or y2 <= oy1 or oy2 <= y1, f'Overlapping fixed controls: {wanted}/{other}'
        rectangles.append((wanted, (x1, y1, x2, y2)))

initial_size = adb('shell', 'wm', 'size')
initial_density = adb('shell', 'wm', 'density')
initial_font = adb('shell', 'settings', 'get', 'system', 'font_scale')
wifi = adb('shell', 'settings', 'get', 'global', 'wifi_on')
data = adb('shell', 'settings', 'get', 'global', 'mobile_data')
results = []
try:
    for name in ['editor', 'preview']:
        (OUT / f'{name}.png').write_bytes(adb('exec-out', 'run-as', 'ai.copilotdrive.app', 'cat', f'files/sprint6-{name}.png', binary=True))
    before = layout_hash()
    adb('logcat', '-c')
    for width, height, density, font in [(800,480,160,1),(1024,600,160,1),(1280,720,160,1),
        (1920,1080,160,1),(360,640,160,1),(1280,800,160,1),(800,480,160,1.5),(1280,720,240,1)]:
        name = f'dashboard-{width}x{height}-dpi{density}-font{font}'
        adb('shell', 'wm', 'size', f'{width}x{height}')
        adb('shell', 'wm', 'density', str(density))
        adb('shell', 'settings', 'put', 'system', 'font_scale', str(font))
        adb('shell', 'am', 'force-stop', 'ai.copilotdrive.app')
        adb('shell', 'am', 'start', '-W', '-n', 'ai.copilotdrive.app/.MainActivity')
        time.sleep(1)
        xml = dump()
        validate_fixed(xml, width, height)
        assert layout_hash() == before, 'Layout changed during resize/process restart'
        capture(name)
        (OUT / f'{name}.xml').write_text(xml, encoding='utf-8')
        results.append(dict(widthPx=width,heightPx=height,densityDpi=density,fontScale=font,
            fixedControls='PASS',persistedAfterProcessRestart=True,screenshot=f'{name}.png'))
        print(name, 'PASS', flush=True)
    # Existing local query must still work when both network transports are disabled.
    adb('shell', 'wm', 'size', '1280x800')
    adb('shell', 'wm', 'density', '160')
    adb('shell', 'settings', 'put', 'system', 'font_scale', '1.0')
    adb('shell', 'svc', 'wifi', 'disable')
    adb('shell', 'svc', 'data', 'disable')
    offline = adb('shell','am','instrument','-w','-e','class',
        'ai.copilotdrive.app.AppFlowTest#onboardingToFuelQueryThroughRealViewModel',
        'ai.copilotdrive.app.test/androidx.test.runner.AndroidJUnitRunner')
    (OUT / 'offline-query.txt').write_text(offline, encoding='utf-8')
    assert 'OK (1 test)' in offline, offline
    adb('shell', 'am', 'start', '-W', '-n', 'ai.copilotdrive.app/.MainActivity')
    time.sleep(1)
    capture('offline-fuel')
    crash = adb('logcat','-d','-b','crash')
    (OUT/'crash-log.txt').write_text(crash,encoding='utf-8')
    assert 'FATAL EXCEPTION' not in crash, crash
    print('offline query PASS; crash buffer clean', flush=True)
finally:
    for what, initial in [('size',initial_size),('density',initial_density)]:
        override = re.search(r'Override (?:size|density): (\S+)', initial)
        adb('shell','wm',what,override.group(1) if override else 'reset')
    adb('shell','settings','put','system','font_scale',initial_font if initial_font != 'null' else '1.0')
    adb('shell','svc','wifi','enable' if wifi != '0' else 'disable')
    adb('shell','svc','data','enable' if data != '0' else 'disable')
    (OUT/'screen-evidence.json').write_text(json.dumps(dict(screens=results,
        initial=dict(size=initial_size,density=initial_density,fontScale=initial_font,wifi=wifi,data=data),
        restored=dict(size=adb('shell','wm','size'),density=adb('shell','wm','density'),
        fontScale=adb('shell','settings','get','system','font_scale'),
        wifi=adb('shell','settings','get','global','wifi_on'),data=adb('shell','settings','get','global','mobile_data'))),indent=2),encoding='utf-8')
