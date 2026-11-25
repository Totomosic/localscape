import csv
from pathlib import Path
import tempfile
import unittest

from tools.research.nylocas_ranger_cleanup_rescue_audit import audit


class RangerCleanupRescueTimestampTests(unittest.TestCase):
    def test_late29_null_child_uses_the_selected_actual_birth_cutoff(self):
        with tempfile.TemporaryDirectory() as temporary:
            folder = Path(temporary)
            with (folder / 'trials.csv').open('w', newline='') as stream:
                writer = csv.DictWriter(stream, fieldnames=['seed'])
                writer.writeheader()
                writer.writerow(dict(seed=123))

            def trace(tick, role, wave, crabs=''):
                return '\t'.join(['NYLO_TRACE', str(tick), str(role), str(wave),
                    'true', 'false', '3295,4248,0', 'EYE', '-1', 'null', 'Reserve next packet', crabs])

            crab = '1,29,5,MAGIC,false,8,3296,4248,0,0,false'
            (folder / 'server.log').write_text('\n'.join([
                trace(0, 0, 0), trace(0, 1, 0),
                trace(100, 0, 28), trace(100, 1, 28),
                trace(112, 0, 29), trace(112, 1, 29),
                'NYLO_FORECAST\t118\t1\t8\t0',
                'NYLO_NULL\t118\t1\t1',
                trace(118, 0, 30, crab.replace(',5,MAGIC,', ',3,MAGIC,')),
                trace(118, 1, 30, crab.replace(',5,MAGIC,', ',3,MAGIC,')),
                'NYLO_ENGINE\t118\t0,0',
                'NYLO_FORECAST\t120\t1\t8\t0',
                'NYLO_NULL\t120\t1\t1',
                trace(120, 0, 31, crab), trace(120, 1, 31, crab),
                'NYLO_ENGINE\t120\t0,0',
            ]) + '\n')
            historical = audit(folder)
            timestamp29 = audit(folder, timestamp_wave29=True)
            combined = audit(folder, dim28plus16=True, timestamp_wave29=True)
            pre31 = audit(folder, timestamp_wave29=True, include_wave30=True)
            self.assertEqual(historical['counts']['partner_null_fresh_lifetimes'], 0)
            self.assertEqual(timestamp29['counts']['partner_null_fresh_lifetimes'], 1)
            self.assertEqual(combined['counts']['partner_null_fresh_lifetimes'], 0)
            self.assertEqual(timestamp29['partner_null'][0]['first_ready_nonlethal_screen']['tick'], 120)
            self.assertEqual(pre31['partner_null'][0]['first_ready_nonlethal_screen']['tick'], 118)
            self.assertEqual(pre31['partner_null'][0]['first_actual_wave'], 30)
            self.assertEqual(historical['sources'], timestamp29['sources'])


if __name__ == '__main__':
    unittest.main()
