import sys
from pathlib import Path
import unittest
from types import SimpleNamespace
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
from host_health import assess
from jank_evidence import metric_summary, parse_window
from qualify_android import QualificationError
from compatibility_android import test_count
from sprint7_recovery_smoke import display_observation, offline_transports


class JankEvidenceTest(unittest.TestCase):
    def test_network_transitions_finish_before_query_and_before_restore_is_claimed(self):
        state = dict(wifi='1',data='1')
        pending = {}
        def shell(*args):
            if args[0] == 'svc':
                key = args[1]
                pending[key] = ['1' if args[2]=='enable' else '0', 2]
                return ''
            key = 'wifi' if args[-1]=='wifi_on' else 'data'
            if key in pending:
                pending[key][1] -= 1
                if pending[key][1] == 0:
                    state[key] = pending.pop(key)[0]
            return state[key]
        restoration = {}
        with patch('sprint7_recovery_smoke.time.sleep'):
            with offline_transports(SimpleNamespace(shell=shell), restoration):
                self.assertEqual(dict(wifi='0',data='0'), state)
            self.assertEqual(dict(wifi='1',data='1'), state)
        self.assertEqual('RESTORED', restoration['status'])
        self.assertTrue(restoration['disabledObserved'])

    def test_legacy_display_uses_rotated_real_override_instead_of_base_or_app(self):
        text = 'mBaseDisplayInfo=DisplayInfo{real 1280 x 800, rotation 0,}\n' + 'mOverrideDisplayInfo=DisplayInfo{app 800 x 1208, real 800 x 1280, modes [{id=1}], rotation 1,}'
        self.assertEqual(dict(widthPx=800,heightPx=1280,rotation=90), display_observation('', text))
        self.assertEqual(dict(widthPx=1280,heightPx=800,rotation=0), display_observation('mBounds=Rect(0, 0 - 1280, 800) mDisplayRotation=ROTATION_0'))

    def test_missing_ambiguous_or_invalid_display_does_not_invent_bounds(self):
        record = 'mOverrideDisplayInfo=DisplayInfo{real 800 x 1280, rotation 1,}'
        for raw in ('', record+'\n'+record, record.replace('rotation 1','rotation 9'), record.replace('800 x','0 x')):
            with self.assertRaises(QualificationError):
                display_observation('', raw)

    def test_missing_iteration_is_not_zero_jank(self):
        for runs in ([], [[]], [[1], []], [[float('nan')]]):
            with self.assertRaises(QualificationError):
                metric_summary(runs)

    def test_negative_overrun_and_frame_population_are_preserved(self):
        result = metric_summary([[-2, 0], [1, 3]])
        self.assertEqual((2, 4, 0, 3, 50), tuple(result.values()))

    def test_missing_or_multiple_windows_or_target_reject_trace(self):
        header = 'category,label,n,value_ms\n'
        for raw in (header, header+'window,measureBlock,2,100\n',
                    header+'window,measureBlock,1,100\n'):
            with self.assertRaises(QualificationError):
                parse_window(raw)
        result = parse_window(header+'window,measureBlock,1,100\nscheduling,Main:Running,2,20\ntraceErrors,missing,1,[NULL]\n')
        self.assertIsNone(result[-1]['durationMs'])

    def test_host_extreme_paging_and_commit_exhaustion_fail_closed(self):
        good = dict(availableMiB=2048, commitHeadroomMiB=4096, pagesInputPerSec=0, cpuPercent=20)
        self.assertIsNone(assess([good]*3))
        self.assertEqual('HOST_SAMPLES_UNAVAILABLE', assess([]))
        self.assertEqual('HOST_COMMIT_EXHAUSTION_RISK', assess([dict(good, commitHeadroomMiB=100)]*3))
        self.assertEqual('HOST_SUSTAINED_PAGING', assess([dict(good, pagesInputPerSec=1001)]*3))
        self.assertEqual('HOST_PHYSICAL_MEMORY_PRESSURE', assess([dict(good, availableMiB=100)]*3))

    def test_instrumentation_requires_explicit_success_without_crash(self):
        self.assertEqual(32, test_count('Time: 30\nOK (32 tests)\n'))
        for output in ('', 'FAILURES!!!', 'OK (32 tests)\nProcess crashed', 'OK (1 test)\nINSTRUMENTATION_FAILED'):
            with self.assertRaises(QualificationError):
                test_count(output)


if __name__ == '__main__':
    unittest.main()
