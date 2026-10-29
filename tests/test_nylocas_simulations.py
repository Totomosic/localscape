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
            row = self.row("completed", 300)
            source = root / "trials.csv"
            with source.open("w", newline="") as output:
                writer = csv.DictWriter(output, fieldnames=list(row))
                writer.writeheader()
                writer.writerow(row)
            report = simulation.write_report(source, root, {})
            markdown = (root / "report.md").read_text()
            for metric, label in simulation.STALL_METRICS.items():
                self.assertEqual(report["metrics_all_trials"][metric]["mean"], 2)
                self.assertIn(f"{label} **2.00**", markdown)

    def test_client_sync_copies_shared_policy_and_removes_shadowing_legacy_source(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            upstream = root / "upstream"
            target = upstream / "runelite/runelite-client/src/main/java"
            target.mkdir(parents=True)
            shared = upstream / "rsmod/api/nylocas-agents/src/main/java/org/localscape/nylocas"
            shared.mkdir(parents=True)
            (shared / "Tile.java").write_text("shared policy")
            old = target / "net/runelite/client/plugins/nyloranger/RangerStrategy.java"
            old.parent.mkdir(parents=True)
            old.write_text("obsolete policy")
            with patch.object(dev, "ROOT", root), patch.object(dev, "UPSTREAM", upstream):
                dev.sync_client()
            self.assertEqual((target / "org/localscape/nylocas/Tile.java").read_text(), "shared policy")
            self.assertFalse(old.exists())


if __name__ == "__main__":
    unittest.main()
