#!/usr/bin/env python3
"""Run unpaced RSMod duo wave trials and produce CSV, JSON, and Markdown reports."""

import argparse
import csv
from datetime import datetime, timezone
import hashlib
import json
import math
from pathlib import Path
import statistics
import subprocess
import time

import dev


STALL_METRICS = {
    "pre_cap_stalls": "Pre-cap stalls (waves 1–20)",
    "post_cap_stalls": "Post-cap stalls (waves 21–31)",
    "pre_29_stalls": "Pre-29 stalls (waves 1–28)",
    "wave_29_stalls": "Wave-29 stalls",
}


def distribution(values):
    return {
        "mean": statistics.mean(values) if values else None,
        "median": statistics.median(values) if values else None,
        "min": min(values) if values else None,
        "max": max(values) if values else None,
        "p95": sorted(values)[math.ceil(len(values) * 0.95) - 1] if values else None,
    }


def summarize(rows):
    completed = [row for row in rows if row["status"] == "completed"]
    ticks = [int(row["ticks"]) for row in completed]
    seconds = sum(float(row["wall_seconds"]) for row in rows)
    return {
        "endpoint": "Nylocas boss spawn (waves and cleanup only)",
        "trials": len(rows),
        "completed": len(completed),
        "deaths": sum(row["status"] == "death" for row in rows),
        "timeouts": sum(row["status"] == "timeout" for row in rows),
        "completion_rate": len(completed) / len(rows) if rows else 0,
        "completion_ticks": distribution(ticks),
        "completion_game_seconds": distribution([tick * 0.6 for tick in ticks]),
        "trial_wall_seconds": seconds,
        "trials_per_second": len(rows) / seconds if seconds else None,
        "completed_ready_idle": {
            role: {
                **distribution([int(row[f"{role}_ready_idle_ticks"]) for row in completed]),
                "under_20": sum(int(row[f"{role}_ready_idle_ticks"]) < 20 for row in completed),
                "trials": len(completed),
                "under_20_rate": sum(int(row[f"{role}_ready_idle_ticks"]) < 20 for row in completed) / len(completed) if completed else None,
            }
            for role in ("mage", "ranger")
        },
        "metrics_all_trials": {
            metric: distribution([int(row[metric]) for row in rows])
            for metric in (
                "ranger_attacks", "mage_attacks", "ranger_idle_ticks", "mage_idle_ticks",
                "ranger_ready_idle_ticks", "mage_ready_idle_ticks", "same_tick_double_ups",
                "shared_targets", "positive_damage_rolls",
            )
        } | {
            metric: distribution([int(row[metric]) for row in rows if row.get(metric) not in (None, "")])
            for metric in STALL_METRICS
        },
    }


def write_report(csv_path, output, metadata):
    with csv_path.open(newline="") as source:
        rows = list(csv.DictReader(source))
    if not rows:
        raise ValueError("The trial CSV contains no results.")
    report = summarize(rows)
    report["run"] = metadata
    output.mkdir(parents=True, exist_ok=True)
    (output / "summary.json").write_text(json.dumps(report, indent=2) + "\n")
    fmt = lambda value: "n/a" if value is None else f"{value:.2f}"
    completion = report["completion_ticks"]
    lines = [
        "# Duo Nylocas simulation report", "",
        f"Endpoint: {report['endpoint']}.", "",
        f"Trials: {report['trials']}; completed: {report['completed']}; deaths: {report['deaths']}; timeouts: {report['timeouts']}.", "",
        f"Completion ticks (successful trials): mean **{fmt(completion['mean'])}**, median **{fmt(completion['median'])}**.", "",
        "Average stalls per trial: " + "; ".join(
            f"{STALL_METRICS[metric]} **{fmt(report['metrics_all_trials'][metric]['mean'])}**"
            for metric in STALL_METRICS
        ) + ".", "",
        f"Throughput including per-trial setup: **{fmt(report['trials_per_second'])} trials/s**. Cache startup and Gradle build excluded.", "",
        "| Metric (all trials) | Mean | Median | Min | Max |",
        "| --- | ---: | ---: | ---: | ---: |",
    ]
    for metric, stats in report["metrics_all_trials"].items():
        lines.append(f"| {STALL_METRICS.get(metric, metric)} | " + " | ".join(fmt(stats[key]) for key in ("mean", "median", "min", "max")) + " |")
    for role, stats in report["completed_ready_idle"].items():
        lines.append("")
        lines.append(f"{role.capitalize()} completed trials with fewer than 20 ready-idle ticks: **{stats['under_20']}/{stats['trials']}**; 95th percentile **{fmt(stats['p95'])}**.")
    lines += ["", "Idle ticks count ticks without a confirmed primary attack, including cooldown and movement. Ready idle ticks exclude policy cooldown. Current simulations count from each role's first confirmed attack through the final living nylo's death, excluding entrance and terminal death-animation/boss waits. Older reports used the entire encounter; check the run metadata before comparing.", "",
              "Stalls count failed population-cap admission checks, each delaying the incoming wave by four ticks. Pre-cap covers incoming waves 1–20; post-cap covers 21–31. Pre-29 covers 1–28; wave-29 counts delays admitting wave 29. Averages include failed and timed-out trials. Legacy CSVs without stall counters show n/a; missing values are excluded from stall statistics.", "",
              "Same-tick double ups count both agents launching a primary attack at the same NPC on one tick. Shared targets count distinct NPC lifetimes attacked by both agents at any time; this can include useful follow-up hits.", "",
              "Positive damage rolls include secondary AoE targets and multiple splats; they measure rolled damage before impact, not kills. Completion can include natural NPC expiry.", "",
              "Fixed equipment and 99 base stats are defined in the server adapter. Reports cover server model behavior; see docs/nylocas-simulations.md for assumptions.", ""]
    (output / "report.md").write_text("\n".join(lines))
    print("\n".join(lines[:12]))
    print(f"Reports: {output}")
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--trials", type=int, default=100)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--max-ticks", type=int, default=2000)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--report-only", type=Path, metavar="CSV", help="Regenerate reports without running the server")
    parser.add_argument("--offline", action="store_true", help="Use only cached Gradle dependencies")
    parser.add_argument("--trace", action="store_true", help="Log per-tick agent decisions and observations to server.log")
    args = parser.parse_args()
    if args.trials <= 0 or args.max_ticks <= 0:
        parser.error("--trials and --max-ticks must be positive")
    if not -(2**63) <= args.seed <= 2**63 - args.trials:
        parser.error("trial seeds must fit signed 64-bit integers")
    output = (args.output or dev.STATE / "simulations/nylocas" / datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")).resolve()
    output.mkdir(parents=True, exist_ok=True)
    if args.report_only:
        write_report(args.report_only, output, {"source_csv": str(args.report_only.resolve())})
        return
    csv_path = output / "trials.csv"
    env = dev.java_env()
    command = ["bash", "gradlew", "--console=plain"]
    if args.offline:
        command.append("--offline")
    command += [":api:combat:combat-scripts:simulateNylocas", f"-Pnylo.trials={args.trials}",
                f"-Pnylo.seed={args.seed}", f"-Pnylo.maxTicks={args.max_ticks}", f"-Pnylo.output={csv_path}"]
    if args.trace:
        command.append("-Pnylo.trace=true")
    print(f"Running {args.trials} trials, seeds {args.seed}..{args.seed + args.trials - 1}. Log: {output / 'server.log'}", flush=True)
    started = time.monotonic()
    with (output / "server.log").open("w") as log:
        result = subprocess.run(command, cwd=dev.UPSTREAM / "rsmod", env=env, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode:
        raise SystemExit(f"Server batch failed ({result.returncode}); inspect {output / 'server.log'}")
    source_root = dev.UPSTREAM / "rsmod/api/nylocas-agents/src/main/java"
    hashes = {str(path.relative_to(source_root)): hashlib.sha256(path.read_bytes()).hexdigest()
              for path in sorted(source_root.rglob("*.java"))}
    report = write_report(csv_path, output, {
        "seed": args.seed, "max_ticks": args.max_ticks,
        "idle_window": "each role's first confirmed attack through the final living nylo's death",
        "invocation_seconds": time.monotonic() - started, "policy_sha256": hashes,
        "fixture": {"base_stats": 99, "starting_hitpoints": 250,
                    "prayer_potions_four_dose_per_player": 4, "prayer_threshold": 25,
                    "damage": "randomized", "gear": "fixed style sets in NylocasServerAgents.kt"},
        "adapter_sha256": {
            name: hashlib.sha256((dev.UPSTREAM / "rsmod/api/combat/combat-scripts/src/integration/kotlin/org/rsmod/api/combat" / name).read_bytes()).hexdigest()
            for name in ("NylocasServerAgents.kt", "NylocasSimulationTest.kt")
        },
    })
    if report["trials"] != args.trials:
        raise SystemExit("Incomplete batch: trial count differs from requested count.")


if __name__ == "__main__":
    main()
