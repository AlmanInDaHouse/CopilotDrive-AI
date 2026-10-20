import sys
from pathlib import Path
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
from api26_offline import connectivity, is_offline, disconnected
from qualify_android import QualificationError
from pilot_package import signature_conflict


class Api26OfflineTest(unittest.TestCase):
    def test_signature_conflict_blocks_changed_or_additional_signers(self):
        self.assertFalse(signature_conflict(['a'], None))
        self.assertFalse(signature_conflict(['a'], ['a']))
        self.assertTrue(signature_conflict(['a'], ['b']))
        self.assertTrue(signature_conflict(['a'], ['a','b']))
    def test_flags_alone_never_prove_android_offline(self):
        self.assertFalse(is_offline(dict(flags=dict(wifi='0', data='0'), activeDefault=True, connectedNetworks=1)))
        self.assertFalse(is_offline(dict(flags=dict(wifi='0', data='0'), activeDefault=False, connectedNetworks=1)))
        self.assertTrue(is_offline(dict(flags=dict(wifi='0', data='0'), activeDefault=False, connectedNetworks=0)))

    def test_parser_excludes_historical_connected_networks(self):
        raw = 'Active default network: none\nCurrent Networks:\n\nNetwork Requests:\nold NetworkAgentInfo{ state: CONNECTED/CONNECTED'
        self.assertEqual(dict(activeDefault=False, connectedNetworks=0), connectivity(raw))
        self.assertEqual(dict(activeDefault=True, connectedNetworks=1), connectivity(
            'Active default network: 100\nCurrent Networks:\nNetworkAgentInfo{ ni{state: CONNECTED/CONNECTED\nNetwork Requests:'))

    def test_unknown_connectivity_fails_closed(self):
        with self.assertRaises(QualificationError):
            connectivity('Unknown format')

    def test_original_state_is_persisted_before_mutation(self):
        commands = []
        with patch('api26_offline.snapshot', return_value=dict(flags=dict(wifi='1', data='1'), activeDefault=True, connectedNetworks=1)):
            with self.assertRaises(OSError):
                with disconnected(None, {}, lambda: (_ for _ in ()).throw(OSError()), lambda *args: commands.append(args)):
                    self.fail('must not enter')
        self.assertEqual([], commands)

    def test_exit137_restores_both_transports_and_keeps_failed_attempt(self):
        original = dict(flags=dict(wifi='1', data='1'), activeDefault=True, connectedNetworks=1)
        report = {}; calls = []
        def action(device, transport, value):
            calls.append((transport, value))
            return dict(exitCode=137 if value == '0' else 0)
        with patch('api26_offline.snapshot', return_value=original), patch('api26_offline.await_state', return_value=original):
            with self.assertRaisesRegex(QualificationError, 'TRANSPORT_COMMAND_FAILED'):
                with disconnected(None, report, lambda: None, action):
                    self.fail('must not enter')
        self.assertEqual([('wifi','0'), ('wifi','1'), ('data','1')], calls)
        self.assertEqual(137, report['commands'][0]['exitCode'])
        self.assertEqual('RESTORED', report['restoration'])

    def test_restoration_failure_never_passes(self):
        original = dict(flags=dict(wifi='1', data='1'), activeDefault=True, connectedNetworks=1)
        offline = dict(flags=dict(wifi='0', data='0'), activeDefault=False, connectedNetworks=0)
        report = {}
        with patch('api26_offline.snapshot', return_value=original), patch('api26_offline.await_state', side_effect=[offline, offline]):
            with self.assertRaisesRegex(QualificationError, 'NETWORK_RESTORATION_FAILED'):
                with disconnected(None, report, lambda: None, lambda *args: dict(exitCode=0)):
                    pass
        self.assertEqual('FAILED', report['restoration'])


if __name__ == '__main__':
    unittest.main()
