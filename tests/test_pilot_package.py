import sys
from pathlib import Path
import unittest
from unittest.mock import Mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
from pilot_package import installed_certificates
from qualify_android import PACKAGE, QualificationError


class InstalledPackagePreflightTest(unittest.TestCase):
    def test_absent_package_does_not_call_pm_path_which_can_exit_one(self):
        device = Mock()
        def shell(*args):
            if args == ('pm', 'list', 'packages', PACKAGE):
                return ''
            raise QualificationError('COMMAND_FAILED')
        device.shell.side_effect = shell
        self.assertIsNone(installed_certificates(device))
        device.shell.assert_called_once_with('pm', 'list', 'packages', PACKAGE)
        device.call.assert_not_called()

    def test_package_listing_failure_blocks_instead_of_assuming_absence(self):
        device = Mock()
        device.shell.side_effect = QualificationError('COMMAND_FAILED')
        with self.assertRaises(QualificationError):
            installed_certificates(device)
        device.call.assert_not_called()

    def test_unexpected_listing_output_blocks_instead_of_assuming_absence(self):
        device = Mock()
        device.shell.return_value = 'Error: package manager unavailable'
        with self.assertRaisesRegex(QualificationError, 'INSTALLED_PACKAGE_STATE_UNAVAILABLE'):
            installed_certificates(device)
        device.call.assert_not_called()

    def test_package_present_but_missing_apk_path_blocks(self):
        device = Mock()
        device.shell.side_effect = [f'package:{PACKAGE}', '']
        with self.assertRaisesRegex(QualificationError, 'INSTALLED_BASE_APK_UNAVAILABLE'):
            installed_certificates(device)
        device.call.assert_not_called()


if __name__ == '__main__':
    unittest.main()
