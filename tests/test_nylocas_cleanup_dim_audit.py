import csv
from pathlib import Path
import tempfile
import unittest

from tools.research.nylocas_cleanup_dim_audit import audit, expiry_status, is_dimmed


class CleanupDimAuditTests(unittest.TestCase):
    def test_union_preserves_wave29_and_uses_inclusive_actual_birth_boundary(self):
        self.assertTrue(is_dimmed(dict(wave=29, born=117), 100, 16))
        self.assertTrue(is_dimmed(dict(wave=30, born=116), 100, 16))
        self.assertFalse(is_dimmed(dict(wave=30, born=117), 100, 16))
        self.assertFalse(is_dimmed(dict(wave=30, born=116), None, 16))

    def test_fresh_natural_expiry_is_not_successful_dim_expiry(self):
        crab = dict(dimmed=False, age=51, big=False)
        self.assertEqual(expiry_status([crab]), 'undimmed_targets_last_alive')
        ignored = dict(crab, dimmed=True)
        self.assertEqual(expiry_status([ignored]), 'made_dim_expiry')
        self.assertEqual(expiry_status([ignored, crab]), 'same_tick_boundary')

    def test_timestamp29_preserves_later_splits_and_combines_actual_anchors(self):
        late = dict(wave=29, born=252)
        self.assertTrue(is_dimmed(late, 224, 16))  # Historical whole-wave rule.
        self.assertFalse(is_dimmed(late, 224, 16, 240, True))
        self.assertTrue(is_dimmed(dict(wave=29, born=240), 224, 16, 240, True))
        self.assertTrue(is_dimmed(dict(wave=30, born=240), 224, 16, 236, True))
        self.assertFalse(is_dimmed(late, 224, None, 240, True))
        self.assertTrue(is_dimmed(late, 224, 16, None, True))  # Unknown29 anchor stays conservative.

    def test_same_native_trace_is_classified_against_the_selected_dim(self):
        with tempfile.TemporaryDirectory() as temporary:
            folder = Path(temporary)
            row = dict(seed='123', status='completed', ticks='184',
                       stalls_by_last_spawned_wave=';'.join(['0'] * 31))
            with (folder / 'trials.csv').open('w', newline='') as source:
                writer = csv.DictWriter(source, fieldnames=list(row))
                writer.writeheader()
                writer.writerow(row)
            trace = lambda tick, wave, crabs: '\t'.join([
                'NYLO_TRACE', str(tick), '0', str(wave), 'true', 'false',
                '3295,4248,0', 'PIPE', '-1', 'null', 'Clear', crabs])
            (folder / 'server.log').write_text('\n'.join([
                'NYLO_PLAYERS\t0\t99,99\t10000,10000\t99,99',
                trace(100, 28, ''),
                trace(120, 31, '1,30,4,RANGE,false,8,3295,4248,0,0,false'),
                trace(167, 31, '1,30,51,RANGE,false,8,3295,4248,0,0,false'),
                trace(168, 31, ''),
            ]) + '\n')
            wave_only = audit(folder)
            combined = audit(folder, 16)
            self.assertEqual(wave_only['all_trials']['expiry_status'],
                             {'undimmed_targets_last_alive': 1})
            self.assertEqual(combined['all_trials']['expiry_status'], {'made_dim_expiry': 1})
            self.assertEqual(wave_only['all_trials']['undimmed_expiry_rooms'], 1)
            self.assertEqual(combined['all_trials']['undimmed_expiry_rooms'], 0)
            self.assertEqual(wave_only['sources'], combined['sources'])

    def test_extra_offset_counts_late_wave29_births_between_actual_anchors(self):
        with tempfile.TemporaryDirectory() as temporary:
            folder = Path(temporary)
            with (folder / 'trials.csv').open('w', newline='') as stream:
                writer = csv.DictWriter(stream, fieldnames=['seed', 'status', 'ticks', 'stalls_by_last_spawned_wave'])
                writer.writeheader()
                writer.writerow(dict(seed=1, status='completed', ticks=292,
                                     stalls_by_last_spawned_wave=';'.join(['0'] * 31)))
            lines = ['NYLO_PLAYERS\t0\t99,99\t10000,10000\t99,99']
            for tick, wave, crab in [(224, 28, ''), (236, 29, ''),
                                     (244, 31, '1,29,6,RANGE,true,16,3295,4248,0,0,false'),
                                     (292, 31, '1,29,54,RANGE,true,16,3295,4248,0,0,false')]:
                lines.append('\t'.join(['NYLO_TRACE', str(tick), '0', str(wave), 'true', 'false',
                                        '3295,4248,0', 'PIPE', '-1', 'null', 'Clear', crab]))
            (folder / 'server.log').write_text('\n'.join(lines) + '\n')
            control, combined = audit(folder, None, True), audit(folder, 16, True)
            self.assertEqual(control['all_trials']['additional_time_dim_rooms'], 0)
            self.assertEqual(combined['all_trials']['additional_time_dim_rooms'], 1)
            self.assertEqual(combined['all_trials']['additional_time_dim_big_expiry_rooms'], 1)
            self.assertEqual(combined['per_trial'][0]['spawn29'], 236)


if __name__ == '__main__':
    unittest.main()
