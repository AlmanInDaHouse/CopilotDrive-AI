import importlib.util
from pathlib import Path
from types import SimpleNamespace
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('qualification', Path(__file__).resolve().parents[1] / 'scripts/qualify_android.py')
q = importlib.util.module_from_spec(spec)
spec.loader.exec_module(q)
recovery_spec = importlib.util.spec_from_file_location('recovery', Path(__file__).resolve().parents[1] / 'scripts/sprint7_recovery_smoke.py')
recovery = importlib.util.module_from_spec(recovery_spec)
with patch.dict('sys.modules', {'qualify_android': q}):
    recovery_spec.loader.exec_module(recovery)


class AndroidQualificationTests(unittest.TestCase):
    def test_offline_failure_restores_both_network_transports(self):
        states = {'wifi_on': '1', 'mobile_data': '0'}
        calls = []
        def shell(*args):
            calls.append(args)
            if args[:3] == ('settings', 'get', 'global'):
                return states[args[3]]
            if args[0] == 'svc':
                key = 'wifi_on' if args[1] == 'wifi' else 'mobile_data'
                states[key] = '1' if args[2] == 'enable' else '0'
            return ''
        restoration = {}
        with self.assertRaises(RuntimeError):
            with recovery.offline_transports(SimpleNamespace(shell=shell), restoration):
                raise RuntimeError('injected')
        self.assertEqual(dict(wifi_on='1', mobile_data='0'), states)
        self.assertEqual('RESTORED', restoration['status'])
        self.assertIn(('svc', 'wifi', 'enable'), calls)
        self.assertIn(('svc', 'data', 'disable'), calls)

    def test_unknown_network_state_is_blocked_before_modification(self):
        device = SimpleNamespace(shell=unittest.mock.Mock(return_value='null'))
        with self.assertRaises(q.QualificationError):
            with recovery.offline_transports(device, {}):
                self.fail('must not start scenario')
        self.assertTrue(all(call.args[0] == 'settings' for call in device.shell.call_args_list))

    def args(self, **overrides):
        values = dict(install=False, launch=False, matrix=False, samples=0,
            allow_install=False, allow_active=False, allow_physical=False)
        values.update(overrides)
        return SimpleNamespace(**values)

    def test_physical_actions_need_separate_confirmation(self):
        q.authorize('PHYSICAL', self.args())
        for args in [self.args(launch=True), self.args(launch=True, allow_active=True),
            self.args(install=True, allow_active=True, allow_physical=True)]:
            with self.assertRaises(q.QualificationError):
                q.authorize('PHYSICAL', args)
        q.authorize('PHYSICAL', self.args(launch=True, allow_active=True, allow_physical=True))
        with self.assertRaises(q.QualificationError):
            q.authorize('PHYSICAL', self.args(matrix=True, allow_active=True, allow_physical=True))

    def test_emulator_serial_alone_does_not_authorize_display_changes(self):
        d = q.Device('adb', 'emulator-5554', 'device')
        with patch.object(d, 'shell', return_value='0'):
            self.assertEqual('PHYSICAL', d.kind())
        with patch.object(d, 'shell', return_value='1'):
            self.assertEqual('EMULATOR', d.kind())

    def test_hash_mismatch_blocks_installation(self):
        with tempfile.TemporaryDirectory() as folder:
            apk = Path(folder) / 'app.apk'; apk.write_bytes(b'example')
            self.assertEqual(64, len(q.verify_apk(apk, None)))
            with self.assertRaises(q.QualificationError):
                q.verify_apk(apk, '0' * 64)

    def test_no_percentiles_for_small_samples(self):
        self.assertEqual({'count': 19}, q.summary(list(range(19))))
        self.assertEqual(dict(count=20, p50=10, p95=19), q.summary(list(range(1, 21))))

    def test_inventory_parses_unauthorized_and_offline_without_treating_as_ready(self):
        result = q.devices('List of devices attached\nprivate-serial unauthorized\nemulator-5554 device model:emulator\nx offline\n')
        self.assertEqual([('private-serial', 'unauthorized'), ('emulator-5554', 'device'), ('x', 'offline')], result)

    def test_display_restores_all_values_even_when_scenario_fails(self):
        values = {('wm', 'size'): 'Physical size: 1280x800\nOverride size: 800x480',
            ('wm', 'density'): 'Physical density: 160',
            ('settings', 'get', 'system', 'font_scale'): 'null',
            ('settings', 'get', 'system', 'accelerometer_rotation'): '1',
            ('settings', 'get', 'system', 'user_rotation'): '0'}
        calls = []
        d = SimpleNamespace(shell=lambda *args: calls.append(args) or values.get(args, ''))
        with self.assertRaisesRegex(RuntimeError, 'scenario'):
            with q.temporary_display(d) as restoration:
                raise RuntimeError('scenario')
        self.assertEqual('RESTORED', restoration['status'])
        self.assertIn(('wm', 'size', '800x480'), calls)
        self.assertIn(('wm', 'density', 'reset'), calls)
        self.assertIn(('settings', 'delete', 'system', 'font_scale'), calls)
        self.assertIn(('settings', 'put', 'system', 'accelerometer_rotation', '1'), calls)

    def test_export_does_not_include_device_serial_or_raw_adb_failure(self):
        with tempfile.TemporaryDirectory() as folder, patch.object(q, 'find_adb', return_value='adb'), patch.object(q, 'command') as command:
            command.side_effect = ['a' * 40, '', 'List of devices attached\nprivate-serial unauthorized\n']
            out = Path(folder) / 'report.json'
            self.assertEqual(0, q.main(['--out', str(out)]))
            text = out.read_text()
            self.assertNotIn('private-serial', text)
            self.assertIn('unauthorized', text)

    def test_dry_run_never_installs_launches_or_runs_measurements(self):
        with tempfile.TemporaryDirectory() as folder, patch.object(q, 'find_adb', return_value='adb'), patch.object(q, 'command') as command, \
            patch.object(q, 'apk_metadata', return_value=dict(minSdk=26)), \
            patch.object(q.Device, 'environment', return_value=dict(kind='EMULATOR', androidApi=35)), patch.object(q.Device, 'call') as call:
            apk = Path(folder) / 'app.apk'; apk.write_bytes(b'example')
            command.side_effect = ['a' * 40, '', 'List of devices attached\nemulator-5554 device\n']
            result = q.main(['--serial', 'emulator-5554', '--dry-run', '--install', '--allow-install', '--allow-active',
                '--apk', str(apk), '--expected-sha256', q.verify_apk(apk, None), '--out', str(Path(folder) / 'report.json')])
            self.assertEqual(0, result); call.assert_not_called()

    def test_command_errors_do_not_export_sensitive_stderr(self):
        with patch.object(q.subprocess, 'run', return_value=SimpleNamespace(returncode=1, stderr=b'token private', stdout=b'')):
            with self.assertRaisesRegex(q.QualificationError, '^COMMAND_FAILED$'):
                q.command(['adb'])
