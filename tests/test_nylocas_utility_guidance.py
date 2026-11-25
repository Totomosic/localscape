import unittest

from tools.research.nylocas_utility_guidance_audit import audit_lines


class UtilityGuidanceAuditTests(unittest.TestCase):
    def trace(self, weapon, energy=500, dc_cooldown=0, destination=3):
        return [
            'NYLO_UTILITY_STATE\t24\t0\t0\t0\t2\t1000\t0\t0\t0\t1\t0\t-1',
            f'NYLO_PRIMARY\t5\t1\t1\t{weapon}\t1\tnull',
            f'NYLO_UTILITY_STATE\t30\t6\t1\t13\t2\t{energy}\t0\t0\t{dc_cooldown}\t0\t0\t-1',
            f'NYLO_UTILITY\t30\t1\tSWAP_REQUEST\t{destination}',
        ]

    def test_confirmed_five_tick_attack_covers_death_charge_swap(self):
        result = audit_lines(self.trace(22325))
        self.assertEqual(result['trials'], 1)
        self.assertEqual(result['counts']['combat_swaps'], 1)
        self.assertEqual(result['examples'], [])

    def test_four_tick_sulphur_does_not_qualify_even_with_three_ticks_left(self):
        result = audit_lines(self.trace(29084))
        self.assertEqual(result['counts']['swaps_outside_confirmed_five_tick_window'], 1)
        self.assertEqual(result['examples'][0]['remaining_cooldown'], 3)

    def test_renewal_without_death_charge_ready_is_distinct_from_cooldown_violation(self):
        result = audit_lines(self.trace(22325, energy=1000))
        self.assertEqual(result['counts']['arceuus_swaps_without_death_charge_ready'], 1)
        self.assertNotIn('swaps_outside_confirmed_five_tick_window', result['counts'])

    def test_ancient_recovery_after_eye_is_still_a_cooldown_violation(self):
        result = audit_lines(self.trace(31113, destination=1))
        self.assertEqual(result['counts']['swaps_outside_confirmed_five_tick_window'], 1)
        self.assertNotIn('arceuus_swaps_without_death_charge_ready', result['counts'])

    def test_preentry_summon_swap_is_exempt(self):
        result = audit_lines(['NYLO_UTILITY\t17\t1\tSWAP_REQUEST\t3'])
        self.assertEqual(result['counts']['preparation_swaps_without_combat_snapshot'], 1)
        self.assertNotIn('combat_swaps', result['counts'])


if __name__ == '__main__':
    unittest.main()
