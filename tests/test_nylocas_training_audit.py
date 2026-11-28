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

    def test_tome_special_counts_require_an_energy_debit(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            root.joinpath("trials.csv").write_text("seed,status,ticks,strategy\n7,completed,315,2\n")
            root.joinpath("server.log").write_text(
                "NYLO_TRACE\t0\t0\t0\ttrue\tfalse\t3295,4254,0\tBOW\t-1\tnull\tOpening\t\n"
                "NYLO_TOME\t41\t7\t24\ttrue\t1000\t500\t3\n"
                "NYLO_TOME\t44\t7\t26\tfalse\t500\t500\t2\n"
                "NYLO_TOME\t48\t8\t27\tfalse\t500\t500\t0\t0\n"
                "NYLO_TRACE\t77\t0\t11\ttrue\tfalse\t3291,4248,0\tTOME\t46\tnull\tApproach\t46,11,0,MAGIC,true,16,3281,4248,0,0,false\n"
                "NYLO_TOME\t78\t11\t46\ttrue\t700\t200\t4\n"
                "NYLO_TOME\t200\t24\t91\ttrue\t400\t400\t0\n"
                "NYLO_TOME\t205\t24\t91\ttrue\t500\t0\t0\n"
                "NYLO_TRACE\t205\t0\t24\ttrue\ttrue\t3295,4248,0\tTOME\t91\tnull\tSouth\t91,24,6,MAGIC,true,16,3295,4233,0,0,false\n"
            )
            stats = audit_module.audit(root)["ranger_tome_specials"]
            self.assertEqual(stats["rooms_with_planned_three_specials"], 1)
            self.assertEqual(stats["pre_cap_casts"], {"special": 2, "stored": 1, "ordinary": 1})
            self.assertEqual([cast["tick"] for cast in stats["per_seed"]["7"]], [41, 78, 205])

    def test_planned_tome_specials_must_hit_the_assigned_waves_on_time(self):
        births = {46: dict(big=True, wave=11, x=3281), 91: dict(big=True, wave=24, x=3281)}
        casts = [dict(wave=7, target=24), dict(wave=11, target=46), dict(wave=24, target=91)]
        self.assertTrue(audit_module.planned_tome_rotation(births, casts))
        self.assertFalse(audit_module.planned_tome_rotation(births, [casts[1], casts[1], casts[2]]))
        self.assertFalse(audit_module.planned_tome_rotation(births, [casts[1], dict(wave=12, target=46), casts[2]]))
        self.assertFalse(audit_module.planned_tome_rotation(births, [casts[0], dict(wave=12, target=46), casts[2]]))
        self.assertFalse(audit_module.planned_tome_rotation(births, [*casts[:2], dict(wave=25, target=91)]))

    def test_incomplete_trace_cannot_be_reported_as_a_clean_audit(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            root.joinpath("trials.csv").write_text("seed,status,ticks\n7,completed,315\n")
            root.joinpath("server.log").write_text("")
            with self.assertRaisesRegex(ValueError, "0 traced trials, 1 CSV trials"):
                audit_module.audit(root)


if __name__ == "__main__":
    unittest.main()
