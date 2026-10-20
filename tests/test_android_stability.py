import importlib.util
from pathlib import Path
import sys
from types import SimpleNamespace
import unittest
from unittest.mock import Mock, patch

SCRIPTS = Path(__file__).resolve().parents[1] / 'scripts'
sys.path.insert(0, str(SCRIPTS))
import stability_android as stability
import benchmark_android as benchmark
from qualify_android import QualificationError


class AndroidStabilityTests(unittest.TestCase):
    def test_older_api_cannot_implicitly_reinstall_benchmark_data(self):
        for api in (26, 30, 33):
            with self.assertRaises(QualificationError):
                benchmark.require_safe_compilation_api(api)
        benchmark.require_safe_compilation_api(34)
        benchmark.require_safe_compilation_api(35)

    def test_adb_response_does_not_imply_android_framework_is_running(self):
        for response in ('Service activity: not found', '', 'Service package: found'):
            with self.assertRaises(QualificationError):
                stability.require_framework(SimpleNamespace(shell=Mock(return_value=response)))
        stability.require_framework(SimpleNamespace(shell=Mock(return_value='Service activity: found')))

    def test_rejects_physical_before_mutation(self):
        device = SimpleNamespace(kind=Mock(return_value='PHYSICAL'), shell=Mock())
        with self.assertRaises(QualificationError):
            stability.require_emulator(device, True)
        device.shell.assert_not_called()

    def test_requires_explicit_active_opt_in(self):
        with self.assertRaises(QualificationError):
            stability.require_emulator(SimpleNamespace(kind=lambda: 'EMULATOR'), False)

    def test_pss_parser_does_not_confuse_rss_or_missing_with_zero(self):
        self.assertEqual(12345, stability.pss_kib('TOTAL PSS: 12345 TOTAL RSS: 99999'))
        self.assertEqual(12345, stability.pss_kib('  TOTAL 12345 22 33'))
        for raw in ('', 'TOTAL RSS: 999', 'TOTAL PSS: 0'):
            with self.assertRaises(QualificationError):
                stability.pss_kib(raw)

    def test_exit_events_are_fingerprinted_and_reason_only(self):
        events = stability.exit_events('ApplicationExitInfo #0:\n timestamp=2026-10-20 11:20:00\n pid=111\n reason=6 (ANR)\n process=private')
        self.assertEqual([6], list(events.values()))
        self.assertTrue(all(len(key) == 64 and 'private' not in key for key in events))

    def test_permission_restore_even_on_scenario_failure(self):
        granted = True
        calls = []
        def shell(*args):
            nonlocal granted
            calls.append(args)
            if args[0] == 'pm':
                granted = args[1] == 'grant'
                return ''
            return 'android.permission.RECORD_AUDIO: granted=' + str(granted).lower() + ', flags=[]'
        restoration = {}
        with self.assertRaises(RuntimeError):
            with stability.revoked_microphone(SimpleNamespace(shell=shell), restoration):
                self.assertFalse(granted)
                raise RuntimeError('injected')
        self.assertTrue(granted)
        self.assertEqual('RESTORED', restoration['status'])

    def test_never_grants_previously_denied_permission(self):
        shell = Mock(return_value='android.permission.RECORD_AUDIO: granted=false, flags=[]')
        with stability.revoked_microphone(SimpleNamespace(shell=shell), {}) as tested:
            self.assertTrue(tested)
        self.assertTrue(all(call.args[0] == 'dumpsys' for call in shell.call_args_list))

    def test_fixed_permission_flags_skip_before_modification(self):
        shell = Mock(return_value='android.permission.RECORD_AUDIO: granted=true, flags=[ USER_FIXED ]')
        restoration = {}
        with stability.revoked_microphone(SimpleNamespace(shell=shell), restoration) as tested:
            self.assertFalse(tested)
        self.assertEqual('PENDING', restoration['status'])
        self.assertEqual(1, shell.call_count)

    def test_denied_permission_with_flags_needs_no_mutation(self):
        shell = Mock(return_value='android.permission.RECORD_AUDIO: granted=false, flags=[ USER_SET ]')
        restoration = {}
        with stability.revoked_microphone(SimpleNamespace(shell=shell), restoration) as tested:
            self.assertTrue(tested)
        self.assertEqual('RESTORED', restoration['status'])
        self.assertTrue(all(call.args[0] == 'dumpsys' for call in shell.call_args_list))


if __name__ == '__main__':
    unittest.main()
