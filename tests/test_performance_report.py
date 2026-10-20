import copy
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
from performance_report import compare
from qualify_android import QualificationError


class PerformanceReportTests(unittest.TestCase):
    def fixture(self):
        return dict(status='EMULATOR_TESTED', environment=dict(kind='EMULATOR', androidApi=35),
            performance=dict(method='external', conditions='debug', samples=[dict(activityStartMs=10,
                dashboardReadyUpperBoundMs=20, memoryPssKb=30) for _ in range(20)]))

    def test_compares_same_conditions_and_keeps_physical_pending(self):
        before = self.fixture()
        after = copy.deepcopy(before)
        for item in after['performance']['samples']:
            item['activityStartMs'] = 12
        result = compare(before, after)
        self.assertEqual(20, result['metrics']['activityStartMs']['p50ChangePercent'])
        self.assertEqual('PENDING', result['physicalQualification'])

    def test_rejects_failed_environment_or_method_mismatch(self):
        for category, key, value in [('environment', 'kind', 'PHYSICAL'), ('environment', 'androidApi', 30),
            ('performance', 'conditions', 'benchmark'), ('performance', 'method', 'TTFD')]:
            after = self.fixture(); after[category][key] = value
            with self.assertRaises(QualificationError):
                compare(self.fixture(), after)
        after = self.fixture(); after['status'] = 'BLOCKED'
        with self.assertRaises(QualificationError):
            compare(self.fixture(), after)

    def test_small_sample_has_no_percentile_or_percentage(self):
        after = self.fixture(); after['performance']['samples'] = after['performance']['samples'][:19]
        result = compare(self.fixture(), after)['metrics']['activityStartMs']
        self.assertNotIn('p50', result['after'])
        self.assertNotIn('p50ChangePercent', result)
