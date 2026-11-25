import unittest

from tools.research.nylocas_primary_attribution_audit import audit_lines


class PrimaryAttributionTests(unittest.TestCase):
    weapons = {0: {"PIPE": "12926"}, 1: {"EYE": "31113"}}

    def trace(self, tick, target="1", weapon="PIPE", rule="null", attacked="true"):
        return f"NYLO_TRACE\t{tick}\t0\t1\ttrue\t{attacked}\t3295,4248,0\t{weapon}\t{target}\t{rule}\tPlan\t\n"

    def test_missing_native_telemetry_is_unknown(self):
        result = audit_lines([self.trace(0)], self.weapons)
        self.assertIsNone(result["mismatched_frames"])

    def test_native_target_and_weapon_catch_a_mislabelled_planner_attack(self):
        result = audit_lines([
            self.trace(0, attacked="false"),
            "NYLO_PRIMARY\t5\t0\t2\t31113\t2\tnull\n",
            self.trace(5, target="1"),
        ], self.weapons)
        self.assertEqual(result["mismatched_frames"], 1)
        self.assertEqual(result["rooms_with_matched_rule"], {})

    def test_launches_before_tick_zero_trace_start_only_one_new_trial(self):
        result = audit_lines([
            "NYLO_PRIMARY\t0\t0\t1\t12926\t1\tpacket\n",
            self.trace(0, rule="packet"), self.trace(5, attacked="false"),
            "NYLO_PRIMARY\t0\t0\t1\t12926\t1\tpacket\n",
            self.trace(0, rule="packet"),
        ], self.weapons)
        self.assertEqual(result["trials"], 2)
        self.assertEqual(result["mismatched_frames"], 0)
        self.assertEqual(result["matched_rule_launches"]["0:packet"], 2)
        self.assertEqual(result["rooms_with_matched_rule"]["0:packet"], 2)


if __name__ == "__main__":
    unittest.main()
