"""Authentic APK screenshots; display overrides only on a named disposable AVD.

Run after the instrumented REAL journey has prepared test preferences. No phone
installation, microphone, paid provider calls or physical qualification.
"""
import argparse
import json
from pathlib import Path
import re
import time
from qualify_android import Device, find_adb, PACKAGE, COMPONENT, QualificationError
from stability_android import require_emulator, require_framework


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--allow-disposable-display', action='store_true')
    args = parser.parse_args()
    device = Device(find_adb(), args.serial, 'device')
    require_emulator(device, args.allow_disposable_display)
    name = device.call('emu', 'avd', 'name').splitlines()[0]
    if not re.fullmatch(r'CopilotDrive_Sprint9_API(26|30|35)', name):
        raise QualificationError('DEDICATED_DISPOSABLE_AVD_REQUIRED')
    require_framework(device)
    args.out.mkdir(parents=True, exist_ok=True)
    original_size = device.shell('wm', 'size')
    original_density = device.shell('wm', 'density')
    original_font = device.shell('settings', 'get', 'system', 'font_scale')
    original_rotation = {key: device.shell('settings', 'get', 'system', key)
                         for key in ('accelerometer_rotation', 'user_rotation')}
    report = dict(physicalQualification='PENDING', avd=name, samples=[], restored=False)
    try:
        device.shell('settings', 'put', 'system', 'accelerometer_rotation', '0')
        device.shell('settings', 'put', 'system', 'user_rotation', '0')
        for label, size, density, font in (
            ('compare-1280x800', '1280x800', '160', '1.0'),
            ('portrait-1080x2412', '1080x2412', '420', '1.0'),
            ('landscape-2412x1080', '2412x1080', '420', '1.0'),
            ('landscape-800x480', '800x480', '160', '1.0'),
            ('landscape-1024x600', '1024x600', '160', '1.0'),
            ('landscape-1280x720', '1280x720', '160', '1.0'),
            ('portrait-font150', '1080x2412', '420', '1.5'),
        ):
            device.shell('am', 'force-stop', PACKAGE)
            device.shell('wm', 'size', size)
            device.shell('wm', 'density', density)
            device.shell('settings', 'put', 'system', 'font_scale', font)
            device.shell('am', 'start', '-W', '-n', COMPONENT)
            time.sleep(2)
            remote = '/sdcard/sprint11-matrix.png'
            device.shell('screencap', '-p', remote)
            device.call('pull', remote, args.out/(label+'.png'))
            report['samples'].append(dict(label=label, physicalPixels=size, densityDpi=int(density),
                                         fontScale=float(font), evidence='EMULATOR_SCREENSHOT'))
            print(label, flush=True)
    finally:
        device.shell('am', 'force-stop', PACKAGE)
        for command, original, kind in (('size', original_size, r'Override size: (\d+x\d+)'),
                                         ('density', original_density, r'Override density: (\d+)')):
            override = re.search(kind, original)
            device.shell('wm', command, override[1] if override else 'reset')
        for key, value in dict(original_rotation, font_scale=original_font).items():
            device.shell('settings', 'delete', 'system', key) if value == 'null' else device.shell('settings', 'put', 'system', key, value)
        report['restored'] = (device.shell('wm', 'size') == original_size and
                              device.shell('wm', 'density') == original_density and
                              device.shell('settings', 'get', 'system', 'font_scale') == original_font and
                              all(device.shell('settings', 'get', 'system', key) == value for key, value in original_rotation.items()))
        (args.out/'matrix.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
    if not report['restored']:
        raise QualificationError('DISPLAY_RESTORATION_FAILED')


if __name__ == '__main__':
    main()
