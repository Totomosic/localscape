import csv
import importlib.util
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

from tools import dev


with patch.dict(sys.modules, {"dev": dev}):
    spec = importlib.util.spec_from_file_location(
        "nylocas_simulations", Path(__file__).parents[1] / "tools/simulate-nylocas.py"
    )
    simulation = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(simulation)


class SimulationReportTests(unittest.TestCase):
    def row(self, status, ticks):
        row = {key: "2" for key in simulation.summarize([])["metrics_all_trials"]}
        return dict(row, status=status, ticks=str(ticks), wall_seconds="0.5")

    def test_completion_statistics_exclude_failed_and_timed_out_trials(self):
        report = simulation.summarize([
            self.row("completed", 300), self.row("completed", 400),
            self.row("death", 5), self.row("timeout", 2000),
        ])
        self.assertEqual(report["completion_ticks"]["mean"], 350)
        self.assertEqual(report["completion_ticks"]["median"], 350)
        self.assertEqual(report["completion_game_seconds"]["median"], 210)
        self.assertEqual(report["completion_rate"], 0.5)
        self.assertEqual(report["deaths"], 1)
        self.assertEqual(report["timeouts"], 1)
        self.assertEqual(report["trials_per_second"], 2)

    def test_no_completions_produce_null_times_instead_of_success_like_zero(self):
        report = simulation.summarize([self.row("death", 100)])
        self.assertIsNone(report["completion_ticks"]["mean"])
        self.assertIsNone(report["completion_ticks"]["median"])
        self.assertEqual(report["metrics_all_trials"]["shared_targets"]["mean"], 2)

    def test_idle_goal_reports_completed_trials_and_includes_the_20_tick_boundary(self):
        rows = [self.row("completed", 300) for _ in range(3)] + [self.row("death", 5)]
        for row, mage, ranger in zip(rows, [19, 20, 50, 0], [0, 19, 20, 0]):
            row["mage_ready_idle_ticks"] = str(mage)
            row["ranger_ready_idle_ticks"] = str(ranger)
        report = simulation.summarize(rows)["completed_ready_idle"]
        self.assertEqual(report["mage"]["under_20"], 1)
        self.assertEqual(report["mage"]["trials"], 3)
        self.assertEqual(report["mage"]["p95"], 50)
        self.assertEqual(report["ranger"]["under_20_rate"], 2 / 3)
        self.assertEqual(report["ranger"]["p95"], 20)

    def test_stall_averages_include_zero_stalls_and_incomplete_trials(self):
        rows = [self.row("completed", 300), self.row("death", 100), self.row("timeout", 1000)]
        for metric, counts in zip(simulation.STALL_METRICS, [(0, 2, 4), (2, 4, 6), (1, 3, 5), (0, 0, 3)]):
            for row, count in zip(rows, counts):
                row[metric] = str(count)
        metrics = simulation.summarize(rows)["metrics_all_trials"]
        for metric, expected in zip(simulation.STALL_METRICS, [2, 4, 3, 1]):
            self.assertEqual(metrics[metric]["mean"], expected)
        self.assertEqual(metrics["wave_29_stalls"]["median"], 0)
        self.assertEqual(metrics["wave_29_stalls"]["max"], 3)

    def test_legacy_csv_stall_counts_are_unknown_instead_of_zero(self):
        legacy = self.row("completed", 300)
        for metric in simulation.STALL_METRICS:
            del legacy[metric]
        metrics = simulation.summarize([legacy])["metrics_all_trials"]
        for metric in simulation.STALL_METRICS:
            self.assertIsNone(metrics[metric]["mean"])
        current = self.row("completed", 400)
        metrics = simulation.summarize([legacy, current])["metrics_all_trials"]
        self.assertEqual(metrics["pre_cap_stalls"]["mean"], 2)

    def test_written_reports_include_all_stall_averages(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            row = dict(self.row("completed", 300), strategy="2")
            source = root / "trials.csv"
            with source.open("w", newline="") as output:
                writer = csv.DictWriter(output, fieldnames=list(row))
                writer.writeheader()
                writer.writerow(row)
            report = simulation.write_report(source, root, {})
            markdown = (root / "report.md").read_text()
            self.assertEqual(report["run"]["strategy"], 2)
            self.assertIn("Bot strategy: 2", markdown)
            with self.assertRaisesRegex(ValueError, "Requested strategy"):
                simulation.write_report(source, root, {"strategy": 1})
            for metric, label in simulation.STALL_METRICS.items():
                self.assertEqual(report["metrics_all_trials"][metric]["mean"], 2)
                self.assertIn(f"{label} **2.00**", markdown)

    def test_max_hit_report_is_explicitly_excluded_from_randomized_benchmark(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            row = self.row("completed", 304)
            source = root / "trials.csv"
            with source.open("w", newline="") as output:
                writer = csv.DictWriter(output, fieldnames=list(row))
                writer.writeheader()
                writer.writerow(row)
            simulation.write_report(source, root, {"max_hit_diagnostic": True})
            markdown = (root / "report.md").read_text()
            self.assertIn("always-max-hit diagnostic", markdown)
            self.assertIn("excluded from randomized benchmarks", markdown)
            self.assertIn("P5 **182.40** and P95 **182.40**", markdown)
            self.assertIn("P5 **n/a** and P95 **n/a**", markdown)

    def test_reported_boss_percentiles_use_nearest_ranks_among_eligible_rooms(self):
        clean = dict(self.row("completed", 300), stalls_by_last_spawned_wave=";".join(["0"] * 31))
        rows = [dict(clean, ticks=str(tick)) for tick in reversed(range(300, 340))]
        stalled = ["0"] * 31
        stalled[19] = "1"
        rows.extend([dict(clean, ticks="900", stalls_by_last_spawned_wave=";".join(stalled)),
                     dict(clean, ticks="950", stalls_by_last_spawned_wave=""),
                     dict(clean, status="death", ticks="1"), dict(clean, status="timeout", ticks="1000")])
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "trials.csv"
            with source.open("w", newline="") as output:
                writer = csv.DictWriter(output, fieldnames=list(clean))
                writer.writeheader()
                writer.writerows(rows)
            report = simulation.write_report(source, root, {})
            markdown = (root / "report.md").read_text()

        benchmark = report["boss_time_benchmark"]
        self.assertEqual(benchmark["eligible_completed_trials"], 40)
        self.assertEqual(benchmark["completion_ticks"]["p5"], 301)
        self.assertEqual(benchmark["completion_ticks"]["p95"], 337)
        self.assertIn("P5 **180.60** and P95 **202.20**", markdown)
        self.assertIn("P5 **181.20** and P95 **203.40**", markdown)

    def test_training_uses_last_spawned_names_and_failures_cannot_meet_goals(self):
        completed = self.row("completed", 315)
        completed["pre_cap_stalls"] = "1"
        counts = [0] * 31
        counts[12], counts[28] = 1, 2
        completed["stalls_by_last_spawned_wave"] = ";".join(map(str, counts))
        failed = dict(completed, status="death", ticks="100")
        goals = simulation.summarize([completed, failed])["training"]
        self.assertEqual(goals["completed_pre_cap_at_most_one"], 1)
        self.assertEqual(goals["completed_without_wave_21_to_27_stalls"], 1)
        self.assertEqual(goals["completed_with_two_or_three_wave_28_stalls"], 1)
        self.assertEqual(goals["stalls_by_last_spawned_wave"]["28"]["mean"], 2)
        self.assertEqual(goals["consistency_denominator_all_trials"], 2)
        self.assertEqual(goals["completed_boss_3m07_to_3m12"], 1)

    def test_training_legacy_missing_wave_counters_remain_unknown(self):
        goals = simulation.summarize([self.row("completed", 315)])["training"]
        self.assertEqual(goals["completed_with_wave_counters"], 0)
        self.assertIsNone(goals["stalls_by_last_spawned_wave"]["28"]["mean"])

    def test_boss_average_excludes_whole_wave19_stall_rooms_but_keeps_raw_results(self):
        clean = self.row("completed", 310)
        clean["stalls_by_last_spawned_wave"] = ";".join(map(str, [0] * 31))
        counts = [0] * 31
        counts[19] = 4
        stalled = dict(clean, ticks="350", stalls_by_last_spawned_wave=";".join(map(str, counts)))
        failed = dict(stalled, status="death", ticks="100")
        report = simulation.summarize([clean, stalled, failed])
        benchmark = report["boss_time_benchmark"]
        self.assertEqual(benchmark["completion_ticks"]["mean"], 310)
        self.assertEqual(benchmark["eligible_completed_trials"], 1)
        self.assertEqual(benchmark["excluded_completed_wave19_stall_trials"], 1)
        self.assertEqual(benchmark["wave19_policy_failure_trials"], 2)
        self.assertFalse(benchmark["wave19_zero_stall_goal_met"])
        self.assertEqual(report["completion_ticks"]["mean"], 330)
        self.assertEqual(report["training"]["stalls_by_last_spawned_wave"]["19"]["mean"], 2)
        self.assertEqual(report["completion_rate"], 2 / 3)

    def test_unknown_wave_counters_cannot_establish_a_clean_boss_time_sample(self):
        benchmark = simulation.summarize([self.row("completed", 300)])["boss_time_benchmark"]
        self.assertEqual(benchmark["eligible_completed_trials"], 0)
        self.assertEqual(benchmark["completed_without_wave_counter_evidence"], 1)
        self.assertFalse(benchmark["wave19_zero_stall_goal_met"])
        self.assertIsNone(benchmark["completion_ticks"]["mean"])
        self.assertIsNone(benchmark["mean_target_met"])
        self.assertIsNone(benchmark["p95_target_met"])

    def test_eligible_mean_goal_is_independent_of_the_two_deadlines(self):
        clean = dict(self.row("completed", 314), stalls_by_last_spawned_wave=";".join(["0"] * 31))
        counts = [0] * 31
        counts[19] = 1
        stalled = dict(clean, ticks="336", stalls_by_last_spawned_wave=";".join(map(str, counts)))
        rows = [clean, dict(clean, ticks="316"), stalled]
        self.assertTrue(simulation.summarize(rows)["boss_time_benchmark"]["mean_target_met"])
        report = simulation.summarize(rows + [dict(clean, ticks="316")])
        self.assertFalse(report["boss_time_benchmark"]["mean_target_met"])
        self.assertTrue(report["boss_time_benchmark"]["p95_target_met"])
        self.assertTrue(report["training"]["boss_95pct_at_most_3m22_goal_met"])

    def test_3m16_8_target_uses_eligible_p95_and_reports_all_trial_successes(self):
        boundary = dict(self.row("completed", 328), stalls_by_last_spawned_wave=";".join(["0"] * 31))
        over = dict(boundary, ticks="329")
        stalls = [0] * 31
        stalls[19] = 1
        stalled = dict(boundary, stalls_by_last_spawned_wave=";".join(map(str, stalls)))
        rows = [boundary] * 19 + [over, stalled, self.row("death", 100)]
        report = simulation.summarize(rows)
        self.assertTrue(report["boss_time_benchmark"]["p95_target_met"])
        self.assertEqual(report["training"]["completed_boss_at_most_3m16_8"], 20)
        self.assertFalse(simulation.summarize(rows + [over])["boss_time_benchmark"]["p95_target_met"])

    def test_3m22_reliability_counts_all_rooms_and_respects_tick_boundary(self):
        clean = self.row("completed", 336)
        counts = [0] * 31
        counts[19] = 1
        stalled = dict(clean, stalls_by_last_spawned_wave=";".join(map(str, counts)))
        late = self.row("completed", 337)
        failed = self.row("death", 100)
        goals = simulation.summarize([clean, stalled, late, failed])["training"]
        self.assertEqual(goals["completed_boss_at_most_3m22"], 2)
        self.assertEqual(goals["boss_at_most_3m22_rate_all_trials"], 0.5)
        self.assertFalse(goals["boss_95pct_at_most_3m22_goal_met"])

    def test_3m22_goal_requires_95_percent_and_does_not_pass_empty_samples(self):
        fast = self.row("completed", 336)
        slow = self.row("completed", 340)
        self.assertTrue(simulation.summarize([fast] * 19 + [slow])["training"]["boss_95pct_at_most_3m22_goal_met"])
        self.assertFalse(simulation.summarize([fast] * 18 + [slow] * 2)["training"]["boss_95pct_at_most_3m22_goal_met"])
        self.assertFalse(simulation.summarize([])["training"]["boss_95pct_at_most_3m22_goal_met"])

    def test_thrall_statistics_preserve_zero_damage_and_missing_legacy_measurements(self):
        legacy = self.row("completed", 315)
        current = dict(legacy, ranger_thrall_impacts="4", ranger_thrall_damage="0",
                       mage_thrall_impacts="7", mage_thrall_damage="9")
        report = simulation.summarize([legacy, current])["thralls_all_trials"]
        self.assertEqual(report["ranger"]["applied_damage"]["mean"], 0)
        self.assertEqual(report["mage"]["applied_damage"]["mean"], 9)
        self.assertEqual(report["ranger"]["impacts"]["mean"], 4)
        self.assertIsNone(simulation.summarize([legacy])["thralls_all_trials"]["mage"]["impacts"]["mean"])

    def test_overlapping_flights_preserve_zero_and_missing_legacy_measurements(self):
        legacy = self.row("completed", 315)
        legacy.pop("overlapping_primary_flights")
        current = dict(legacy, overlapping_primary_flights="0")
        metric = "overlapping_primary_flights"
        self.assertEqual(simulation.summarize([legacy, current])["metrics_all_trials"][metric]["mean"], 0)
        self.assertIsNone(simulation.summarize([legacy])["metrics_all_trials"][metric]["mean"])

    def test_client_sync_keeps_policy_tests_and_purges_retired_agent_plugins(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            upstream = root / "upstream"
            client_root = upstream / "runelite/runelite-client"
            target = client_root / "src/main/java"
            target.mkdir(parents=True)
            shared = upstream / "rsmod/api/nylocas-agents/src/main/java/org/localscape/nylocas"
            shared.mkdir(parents=True)
            (shared / "Tile.java").write_text("shared policy")
            stale = []
            for package in ("nyloranger", "nylomage", "nylocommon"):
                for folder, suffix in (("src/main/java", "java"), ("src/test/java", "java"),
                                       ("target/classes", "class"), ("target/test-classes", "class")):
                    old = client_root / folder / "net/runelite/client/plugins" / package / f"Old.{suffix}"
                    old.parent.mkdir(parents=True, exist_ok=True)
                    old.write_text("retired plugin")
                    stale.append(old)
            for folder, suffix in (("src/main/java", "java"), ("target/classes", "class")):
                old = client_root / folder / "org/localscape/nylocas" / f"Tile.{suffix}"
                old.parent.mkdir(parents=True, exist_ok=True)
                old.write_text("old shipped policy")
                stale.append(old)
            keep = target / "net/runelite/client/plugins/localdev/LocalDevPlugin.java"
            keep.parent.mkdir(parents=True)
            keep.write_text("keep plugin")
            with patch.object(dev, "ROOT", root), patch.object(dev, "UPSTREAM", upstream):
                dev.sync_client()
            policy = client_root / "src/test/java/org/localscape/nylocas/Tile.java"
            self.assertEqual(policy.read_text(), "shared policy")
            self.assertEqual(keep.read_text(), "keep plugin")
            for old in stale:
                self.assertFalse(old.exists(), old)


if __name__ == "__main__":
    unittest.main()
