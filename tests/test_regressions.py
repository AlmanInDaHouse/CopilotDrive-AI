import json
import threading
import unittest
from http.server import ThreadingHTTPServer
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from app.assistant import handle
from app.server import Handler
from app.telemetry import Elm327Serial, Simulator


class RegressionTests(unittest.TestCase):
    def test_truncated_rpm_is_unknown(self):
        self.assertIsNone(Elm327Serial.decode('010C', '41 0C 1A >'))

    def test_simulation_is_spoken(self):
        for question in ('gasolina', 'velocidad', 'rpm', 'temperatura', 'voltaje'):
            self.assertIn('simulad', handle(question, Simulator().snapshot())['reply'].lower())

    def test_disconnected_value_not_presented_as_current(self):
        snapshot = Simulator().snapshot()
        snapshot.update(status='disconnected', fuel_percent=42)
        self.assertIn('no disponible', handle('gasolina', snapshot)['reply'])

    def test_non_object_json_is_bad_request(self):
        class TestHandler(Handler):
            source = Simulator()

            def log_message(self, *args):
                pass

        server = ThreadingHTTPServer(('127.0.0.1', 0), TestHandler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            for body in ([], None, 'text', 42):
                req = Request(f'http://127.0.0.1:{server.server_port}/api/assistant',
                              data=json.dumps(body).encode(), headers={'Content-Type': 'application/json'})
                with self.assertRaises(HTTPError) as error:
                    urlopen(req, timeout=2)
                self.assertEqual(400, error.exception.code)
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=2)
