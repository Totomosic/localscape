#!/usr/bin/env python3
"""Run unpaced RSMod duo wave trials and produce CSV, JSON, and Markdown reports."""

import argparse
import csv
from datetime import datetime, timezone
import hashlib
import json
import math
import shutil
import secrets
from pathlib import Path
import statistics
import subprocess
import time

import dev


STALL_METRICS = {
    "pre_cap_stalls": "Pre-cap stalls (last spawned 1–19)",
    "post_cap_stalls": "Post-cap stalls (last spawned 20–30)",
    "pre_29_stalls": "Pre-29 stalls (waves 1–28)",
    "wave_29_stalls": "Wave-28 stalls (admitting wave 29)",
}


def distribution(values):
    return {
        "mean": statistics.mean(values) if values else None,
        "median": statistics.median(values) if values else None,
        "min": min(values) if values else None,
        "max": max(values) if values else None,
        "p95": sorted(values)[math.ceil(len(values) * 0.95) - 1] if values else None,
    }


def training_metrics(rows):
    """Brief-specific metrics; failures never count as meeting a consistency goal."""
    completed = [row for row in rows if row["status"] == "completed"]
    measured = [row for row in completed if row.get("stalls_by_last_spawned_wave")]
    by_wave = [list(map(int, row["stalls_by_last_spawned_wave"].split(";"))) for row in measured]
    if any(len(counts) != 31 for counts in by_wave):
        raise ValueError("Expected stall counts for last-spawned waves 0 through 30")
    timely = sum(int(row["ticks"]) * 3 <= 1010 for row in completed)
    return {
        "stall_naming": "last wave actually spawned; wave 28 retries admission to 29",
        "completed_with_wave_counters": len(measured),
        "stalls_by_last_spawned_wave": {str(wave): distribution([counts[wave] for counts in by_wave]) for wave in range(1, 31)},
        "completed_pre_cap_at_most_one": sum(int(row["pre_cap_stalls"]) <= 1 for row in completed if row.get("pre_cap_stalls") not in (None, "")),
        "completed_without_wave_21_to_27_stalls": sum(sum(counts[21:28]) == 0 for counts in by_wave),
        "completed_with_two_or_three_wave_28_stalls": sum(counts[28] in (2, 3) for counts in by_wave),
        "completed_boss_3m07_to_3m12": sum(187 <= int(row["ticks"]) * 0.6 <= 192 for row in completed),
        "completed_boss_over_3m14": sum(int(row["ticks"]) * 0.6 > 194 for row in completed),
        "consistency_denominator_all_trials": len(rows),
        "boss_mean_target_ticks": 315,
        "boss_reliability_target_seconds": 202,
        "boss_reliability_target_rate": 0.95,
        "completed_boss_at_most_3m22": timely,
        "boss_at_most_3m22_rate_all_trials": timely / len(rows) if rows else None,
        "boss_95pct_at_most_3m22_goal_met": bool(rows) and timely * 100 >= len(rows) * 95,
    }


def summarize(rows):
    completed = [row for row in rows if row["status"] == "completed"]
    ticks = [int(row["ticks"]) for row in completed]
    seconds = sum(float(row["wall_seconds"]) for row in rows)
    return {
        "endpoint": "Nylocas boss spawn (waves and cleanup only)",
        "training": training_metrics(rows),
        "trials": len(rows),
        "completed": len(completed),
        "deaths": sum(row["status"] == "death" for row in rows),
        "timeouts": sum(row["status"] == "timeout" for row in rows),
        "completion_rate": len(completed) / len(rows) if rows else 0,
        "terminal_resources_all_trials": {
            metric: distribution([int(row[metric]) for row in rows if row.get(metric) not in (None, "")])
            for metric in ("ranger_final_hp", "mage_final_hp", "ranger_final_energy", "mage_final_energy", "ranger_food_remaining", "mage_food_remaining", "ranger_final_magic", "mage_final_magic")
        },
        "thralls_all_trials": {
            role: {
                "impacts": distribution([int(row[f"{role}_thrall_impacts"]) for row in rows if row.get(f"{role}_thrall_impacts") not in (None, "")]),
                "applied_damage": distribution([int(row[f"{role}_thrall_damage"]) for row in rows if row.get(f"{role}_thrall_damage") not in (None, "")]),
            } for role in ("ranger", "mage")
        },
        "completion_ticks": distribution(ticks),
        "completion_game_seconds": distribution([tick * 0.6 for tick in ticks]),
        "boss_time_benchmark": boss_time_benchmark(rows),
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
            for metric in (*STALL_METRICS, "overlapping_primary_flights")
        },
    }


def boss_time_benchmark(rows):
    """Jordan's review rule: any wave-19 stall is a policy failure, not a timed sample."""
    completed = [row for row in rows if row["status"] == "completed"]
    measured = [row for row in rows if row.get("stalls_by_last_spawned_wave")]
    stalled = [row for row in measured if int(row["stalls_by_last_spawned_wave"].split(";")[19]) > 0]
    eligible = [row for row in completed if row.get("stalls_by_last_spawned_wave")
                and int(row["stalls_by_last_spawned_wave"].split(";")[19]) == 0]
    ticks = [int(row["ticks"]) for row in eligible]
    return {
        "rule": "Exclude entire rooms with any last-spawned-wave-19 stall; require known wave counters",
        "eligible_completed_trials": len(eligible),
        "excluded_completed_wave19_stall_trials": sum(row["status"] == "completed" for row in stalled),
        "completed_without_wave_counter_evidence": sum(not row.get("stalls_by_last_spawned_wave") for row in completed),
        "wave19_policy_failure_trials": len(stalled),
        "all_trials_with_wave_counter_evidence": len(measured),
        "wave19_zero_stall_goal_met": bool(rows) and len(measured) == len(rows) and not stalled,
        "completion_ticks": distribution(ticks),
        "completion_game_seconds": distribution([tick * 0.6 for tick in ticks]),
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
    benchmark = report["boss_time_benchmark"]
    boss_seconds = benchmark["completion_game_seconds"]
    lines = [
        "# Duo Nylocas simulation report", "",
        f"Endpoint: {report['endpoint']}.", "",
        "Damage profile: **always-max-hit diagnostic — excluded from randomized benchmarks**." if metadata.get("max_hit_diagnostic") else "Damage profile: randomized (unless legacy metadata states otherwise).", "",
        f"Trials: {report['trials']}; completed: {report['completed']}; deaths: {report['deaths']}; timeouts: {report['timeouts']}.", "",
        f"Reported boss spawn game seconds (completed rooms with zero wave-19 stalls): average **{fmt(boss_seconds['mean'])}**, minimum **{fmt(boss_seconds['min'])}**, maximum **{fmt(boss_seconds['max'])}**; eligible **{benchmark['eligible_completed_trials']}/{report['completed']}**, excluded for wave-19 stalls **{benchmark['excluded_completed_wave19_stall_trials']}**, unknown wave counters **{benchmark['completed_without_wave_counter_evidence']}**.", "",
        f"Wave-19 policy failures: **{benchmark['wave19_policy_failure_trials']}** (target zero; exclusion from the time average does not excuse these failures).", "",
        f"Raw completion ticks (all successful trials): mean **{fmt(completion['mean'])}**, median **{fmt(completion['median'])}**, minimum **{fmt(completion['min'])}**, maximum **{fmt(completion['max'])}**.", "",
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
    goals = report["training"]
    lines += ["", f"Boss-time reliability: **{goals['completed_boss_at_most_3m22']}/{len(rows)}** completed at **3:22** or faster, **{fmt(None if goals['boss_at_most_3m22_rate_all_trials'] is None else goals['boss_at_most_3m22_rate_all_trials'] * 100)}%** (target **95%**). This denominator includes wave-19 failure rooms, deaths and timeouts; only the reported average excludes wave-19 rooms."]
    lines += ["", f"Raw boss spawn game seconds (all completed rooms): average **{fmt(report['completion_game_seconds']['mean'])}**, minimum **{fmt(report['completion_game_seconds']['min'])}**, maximum **{fmt(report['completion_game_seconds']['max'])}**. Reported target average is189 seconds over completed rooms with known zero wave-19 stalls; the wave-19 policy-failure target remains zero.", "", f"Completed trials with at most one pre-cap stall: **{goals['completed_pre_cap_at_most_one']}/{len(rows)}**. Completed without wave 21–27 stalls: **{goals['completed_without_wave_21_to_27_stalls']}/{len(rows)}**. Completed with two or three wave-28 stalls: **{goals['completed_with_two_or_three_wave_28_stalls']}/{len(rows)}**. Wave-counter coverage: {goals['completed_with_wave_counters']} completed trials."]
    lines += ["", "Idle ticks count ticks without a confirmed primary attack, including cooldown and movement. Ready idle ticks exclude policy cooldown. Current simulations count from each role's first confirmed attack through the final living nylo's death, excluding entrance and terminal death-animation/boss waits. Older reports used the entire encounter; check the run metadata before comparing.", "",
              "Stalls count failed population-cap admission checks, each delaying admission by four ticks. Training names stalls by the last wave spawned: pre-cap 1–19, post-cap 20–30, and wave 28 delays admission to 29. Legacy CSV column wave_29_stalls retains its incoming-wave name. The wave 21–27 consistency check excludes the intentional wave-28 retries. Averages include failed and timed-out trials; legacy missing counters are unknown.", "",
              "Same-tick double ups count both agents launching a primary attack at the same NPC on one tick. Overlapping primary flights count confirmed launches while the partner still awaits a real primary impact; overlaps are not proof of wasted damage. Legacy missing overlap counters are unknown. Shared targets count distinct NPC lifetimes attacked by both agents at any time; this can include useful follow-up hits.", "",
              "Positive damage rolls include secondary AoE targets and multiple splats; they measure rolled damage before impact, not kills. Completion can include natural NPC expiry.", "",
              "Fixed equipment and 99 base stats are defined in the server adapter. Reports cover server model behavior; see docs/nylocas-simulations.md for assumptions.", ""]
    (output / "report.md").write_text("\n".join(lines))
    print("\n".join(lines[:12]))
    print(f"Reports: {output}")
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--trials", type=int, default=100)
    parser.add_argument("--seed", type=int, help="Optional recorded seed; default selects a new random seed range for every batch")
    parser.add_argument("--max-ticks", type=int, default=2000)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--report-only", type=Path, metavar="CSV", help="Regenerate reports without running the server")
    parser.add_argument("--offline", action="store_true", help="Use only cached Gradle dependencies")
    parser.add_argument("--starting-hitpoints", type=int, choices=(99, 250), default=250, help="Use normal 99 HP or the server's existing 250 HP raid option")
    parser.add_argument("--utilities", action="store_true", help="Enable native thrall and DeathCharge scheduling for controlled comparison")
    parser.add_argument("--extra-dinh-25", action="store_true", help="Experiment with the recorded optional wave-25 Dinh packet; requires --utilities")
    parser.add_argument("--defer-mage-thrall-renewal", action="store_true", help="Experiment with retaining only the initial mage thrall before boss spawn; requires --utilities")
    parser.add_argument("--prepot", action="store_true", help="Drink supported combat potions before raid entry through normal handlers")
    parser.add_argument("--server-boosts", action="store_true", help="Load the live world's existing login boosts and native regeneration scripts as an explicit profile")
    parser.add_argument("--policy-source", type=Path, help="Use an alternate canonical Java source tree for controlled comparisons")
    parser.add_argument("--trace", action="store_true", help="Log per-tick agent decisions and observations to server.log")
    parser.add_argument("--max-hit-diagnostic", action="store_true", help="Diagnostic only: existing AllWeapons max-hit setting; never a randomized training benchmark")
    parser.add_argument("--queued-damage-forecast", action=argparse.BooleanOptionalAction, default=True, help="Observe already-rolled native hits (live default); disable only for controlled comparison")
    args = parser.parse_args()
    if args.seed is None:
        args.seed = secrets.randbelow(2**48)
    if args.extra_dinh_25 and not args.utilities:
        parser.error("--extra-dinh-25 requires --utilities")
    if args.defer_mage_thrall_renewal and not args.utilities:
        parser.error("--defer-mage-thrall-renewal requires --utilities")
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
                f"-Pnylo.seed={args.seed}", f"-Pnylo.maxTicks={args.max_ticks}", f"-Pnylo.output={csv_path}", f"-Pnylo.hitpoints={args.starting_hitpoints}", f"-Pnylo.utilities={str(args.utilities).lower()}",
                f"-Pnylo.extraDinh25={str(args.extra_dinh_25).lower()}",
                f"-Pnylo.deferMageThrallRenewal={str(args.defer_mage_thrall_renewal).lower()}",
                f"-Pnylo.serverBoosts={str(args.server_boosts).lower()}",
                f"-Pnylo.maxHitDiagnostic={str(args.max_hit_diagnostic).lower()}",
                f"-Pnylo.queuedDamageForecast={str(args.queued_damage_forecast).lower()}"]
    if args.trace:
        command.append("-Pnylo.trace=true")
    if args.prepot:
        command.append("-Pnylo.prepot=true")
    print(f"Running {args.trials} trials, seeds {args.seed}..{args.seed + args.trials - 1}. Log: {output / 'server.log'}", flush=True)
    source_root = args.policy_source or dev.UPSTREAM / "rsmod/api/nylocas-agents/src/main/java"
    snapshot = output / "policy-source"
    if snapshot.exists():
        raise SystemExit("Output directory already contains a policy snapshot; choose a new output directory.")
    shutil.copytree(source_root, snapshot)
    command.append(f"-Pnylo.policySource={snapshot}")
    source_root = snapshot
    hashes = {str(path.relative_to(source_root)): hashlib.sha256(path.read_bytes()).hexdigest()
              for path in sorted(source_root.rglob("*.java"))}
    adapter_root = dev.UPSTREAM / "rsmod/api/combat/combat-scripts/src/integration/kotlin/org/rsmod/api/combat"
    adapter_files = [adapter_root / name for name in (
        "NylocasServerAgents.kt", "NylocasSimulationTest.kt", "NylocasSimulationAreas.kt", "NylocasSimulationStalls.kt", "NylocasSimulationSupplies.kt")]
    adapter_files += sorted((dev.UPSTREAM / "rsmod/content/other/nylocas-bots/src/main").rglob("*.kt"))
    adapter_files += [dev.UPSTREAM / "rsmod" / path for path in (
        "content/other/login/src/main/kotlin/org/rsmod/content/other/login/LoginScript.kt",
        "api/stats-plugin/src/main/kotlin/org/rsmod/api/stats/plugin/StatRegenScript.kt",
    )]
    adapter_hashes = {str(path.relative_to(dev.UPSTREAM / "rsmod")): hashlib.sha256(path.read_bytes()).hexdigest() for path in adapter_files}
    adapter_snapshot = output / "adapter-source"
    for path in adapter_files:
        target = adapter_snapshot / path.relative_to(dev.UPSTREAM / "rsmod")
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(path, target)
    started = time.monotonic()
    with (output / "server.log").open("w") as log:
        result = subprocess.run(command, cwd=dev.UPSTREAM / "rsmod", env=env, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode:
        raise SystemExit(f"Server batch failed ({result.returncode}); inspect {output / 'server.log'}")
    if args.trace and args.queued_damage_forecast:
        with (output / "server.log").open() as source:
            if not any(line.lstrip().startswith("NYLO_FORECAST\t") for line in source):
                raise SystemExit("Requested queued-damage forecast has no native trace coverage; refusing to label this run enabled.")
    report = write_report(csv_path, output, {
        "seed": args.seed, "max_ticks": args.max_ticks,
        "max_hit_diagnostic": args.max_hit_diagnostic,
        "queued_damage_forecast": args.queued_damage_forecast,
        "idle_window": "each role's first confirmed attack through the final living nylo's death",
        "invocation_seconds": time.monotonic() - started, "policy_sha256": hashes,
        "fixture": {"base_stats": 99, "agility": 99, "starting_run_energy": 10000, "starting_hitpoints": args.starting_hitpoints,
                    "prayer_potions_four_dose_per_player": 4, "prayer_threshold": 25,
                    "special_energy": "normal regeneration with Lightbearer",
                    "inventory_input_bridge": "HeldOpScript; shared supply-fixture contract verified",
                    "prepot": args.prepot,
                    "server_boosts": args.server_boosts,
                    "native_utilities": args.utilities,
                    "queued_damage_forecast": args.queued_damage_forecast,
                    "extra_dinh_25": args.extra_dinh_25,
                    "defer_mage_thrall_renewal": args.defer_mage_thrall_renewal,
                    "manta_rays_once_per_player": 5 if args.starting_hitpoints == 99 else 0,
                    "food_hp_threshold": 65,
                    "damage": "always-max-hit diagnostic" if args.max_hit_diagnostic else "randomized", "gear": "role loadouts from mage-strategy.md and range-strategy.md; see adapter"},
        "adapter_sha256": adapter_hashes,
    })
    if report["trials"] != args.trials:
        raise SystemExit("Incomplete batch: trial count differs from requested count.")


if __name__ == "__main__":
    main()
