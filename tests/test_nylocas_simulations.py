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
