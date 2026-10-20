import unittest
from app.telemetry import Simulator, Elm327Serial, alerts
from app.assistant import handle, normalize


class CoreTests(unittest.TestCase):
    def setUp(self):
        self.s = Simulator().snapshot()

    def test_simulator_is_explicit(self):
        self.assertEqual(self.s['source'], 'simulator')
        self.assertEqual(self.s['status'], 'connected')

    def test_commands(self):
        self.assertEqual(handle('Pon música', self.s)['action'], 'music.play')
        self.assertEqual(handle('Pausa la música', self.s)['action'], 'music.pause')
        self.assertEqual(handle('siguiente canción', self.s)['action'], 'music.next')
        self.assertIn('°C', handle('temperatura del motor', self.s)['reply'])

    def test_missing_data_not_invented(self):
        self.s['coolant_c'] = None
        self.assertIn('no disponible', handle('temperatura', self.s)['reply'])

    def test_alert_threshold(self):
        self.s['coolant_c'] = 114
        self.assertEqual(len(alerts(self.s)), 1)
        self.s['status'] = 'disconnected'
        self.assertEqual(alerts(self.s), [])

    def test_readonly_obd_decoder(self):
        self.assertEqual(Elm327Serial.decode('010C', '41 0C 1A F8 >'), 1726.0)
        self.assertEqual(Elm327Serial.decode('0105', '410580>'), 88.0)
        self.assertIsNone(Elm327Serial.decode('010C', 'NO DATA'))
        self.assertEqual(set(command for command, _ in __import__('app.telemetry', fromlist=['PIDS']).PIDS.values()),
                         {'010C', '010D', '0105', '012F'})

    def test_normalization(self):
        self.assertEqual(normalize('MÚSICA'), 'musica')


if __name__ == '__main__':
    unittest.main()
