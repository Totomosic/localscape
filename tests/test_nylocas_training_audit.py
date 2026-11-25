import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location(
    "nylocas_training_audit", Path(__file__).parents[1] / "tools/analyze-nylocas-training.py"
)
audit_module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit_module)


class TrainingTraceAuditTests(unittest.TestCase):
    def test_audit_uses_confirmed_attacks_births_and_actual_wave_markers(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            root.joinpath("trials.csv").write_text("seed,status,ticks\n7,completed,315\n")
            root.joinpath("server.log").write_text(
                "    NYLO_PLAYERS\t0\t99,99\t10000,10000\t20,20\n"
                "    NYLO_TRACE\t0\t0\t0\ttrue\tfalse\t3295,4254,0\tBOW\t-1\tnull\tOpening\t\n"
                "    NYLO_TRACE\t100\t0\t21\ttrue\tfalse\t3295,4248,0\tPIPE\t2\tnull\tApproach\t2,18,8,RANGE,true,16,3281,4249,0,0,false\n"
                "    NYLO_TRACE\t300\t0\t30\ttrue\ttrue\t3295,4248,0\tPIPE\t3\tnull\tFinish\t3,29,8,RANGE,false,8,3281,4249,0,0,false\n"
                "    NYLO_PLAYERS\t301\t85,90\t9800,9900\t51,51\n"
                "    NYLO_NULL\t301\t1\t3\n"
                "    NYLO_TRACE\t301\t1\t31\ttrue\ttrue\t3295,4248,0\tEYE\t3\tnull\tFinish\t3,29,9,MAGIC,false,8,3282,4249,0,0,false\n"
                "    NYLO_TRACE\t302\t0\t31\ttrue\ttrue\t3295,4248,0\tPIPE\t4\tnull\tNew split\t4,31,0,RANGE,false,8,3295,4249,0,0,false\n"
            )
            result = audit_module.audit(root)
            self.assertEqual(result["old_cleanup_attacks"], 1)
            self.assertEqual(result["player_resources"][0]["minimum_hp"], [85, 90])
            self.assertEqual(result["player_resources"][0]["minimum_energy"], [9800, 9900])
            self.assertEqual(result["player_resources"][0]["prayer_increases"], [1, 1])
            self.assertEqual(result["null_observation_records"], 1)
            self.assertEqual(result["observed_null_target_lifetimes"], [0, 1])
            self.assertEqual(result["confirmed_attacks_on_known_nulled_targets"], 1)
            self.assertEqual(result["transition_goal_at_wave21"], {"met": 0, "observed": 1})
            self.assertEqual(result["confirmed_attacks_by_birth"]["0:29:west:small:PIPE"], 1)
            self.assertEqual(result["confirmed_rule_attacks"]["0:null"], 2)
            self.assertEqual(result["per_trial"][0]["old_cleanup_attacks"][0]["birth_wave"], 29)

    def test_incomplete_trace_cannot_be_reported_as_a_clean_audit(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            root.joinpath("trials.csv").write_text("seed,status,ticks\n7,completed,315\n")
            root.joinpath("server.log").write_text("")
            with self.assertRaisesRegex(ValueError, "0 traced trials, 1 CSV trials"):
                audit_module.audit(root)


if __name__ == "__main__":
    unittest.main()
