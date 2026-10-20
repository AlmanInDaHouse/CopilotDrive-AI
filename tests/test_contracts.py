import json
import unittest
from pathlib import Path

from app.telemetry import Elm327Serial

FIXTURES = Path(__file__).resolve().parents[1] / 'contracts' / 'fixtures'


class SharedContractTests(unittest.TestCase):
    def test_obd_equivalence_fixtures(self):
        rows = json.loads((FIXTURES / 'obd-read-v1.json').read_text(encoding='utf-8'))
        for row in rows:
            with self.subTest(pid=row['pid'], response=row['response']):
                self.assertEqual(row['value'], Elm327Serial.decode(row['pid'], row['response']))

    def test_v1_simulation_provenance_and_null_range(self):
        data = json.loads((FIXTURES / 'vehicle-simulated-v1.json').read_text(encoding='utf-8'))
        self.assertEqual(1, data['schemaVersion'])
        self.assertTrue(data['simulationMode'])
        self.assertEqual('SIMULATED', data['fuelLevelPercent']['availability'])
        self.assertIsNone(data['vehicleRangeKm']['value'])
