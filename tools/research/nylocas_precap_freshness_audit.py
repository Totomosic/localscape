#!/usr/bin/env python3
"""Measure original wave-1 melee targeting and pre-cap timing against fresh births."""

import argparse
import csv
import json
from collections import Counter
from pathlib import Path


def read_rooms(directory):
    with (directory / "trials.csv").open() as stream:
        trials = list(csv.DictReader(stream))
    rooms = []
    with (directory / "server.log").open() as stream:
        for line in stream:
            fields = line.lstrip().rstrip("\r\n").split("\t")
            kind = fields[0]
            if kind == "NYLO_PLAYERS" and fields[1] == "0":
                room = dict(seed=int(trials[len(rooms)]["seed"]), spawns={}, births={},
                            actions=[], fresh_stages=[], primaries=[], rolls=[], impacts=[],
                            split_clock={}, observed_bodies={})
                previous_bigs = {}
                rooms.append(room)
            if kind == "NYLO_SPLIT_CLOCK":
                tick, key, due = map(int, fields[1:4])
                clock = room["split_clock"].setdefault(key, dict(first=tick, due=set()))
                clock["due"].add(due)
            elif kind == "NYLO_TRACE":
                tick, role, wave = map(int, fields[1:4])
                room["spawns"].setdefault(wave, tick)
                if wave > 20:
                    continue
                if role == 0:
                    bigs, new_smalls = {}, set()
                    for body in filter(None, fields[11].split(";")):
                        key, born_wave, age, style, big, hp, x, y, *_ = body.split(",")
                        if big == "true":
                            bigs[int(key)] = (int(x), int(y))
                        elif age == "0":
                            new_smalls.add((int(x), int(y)))
                        if age == "0":
                            room["births"][int(key)] = dict(tick=tick, wave=int(born_wave),
                                style=style, big=big == "true", x=int(x), y=int(y))
                    for key in previous_bigs.keys() - bigs.keys():
                        if key in room["split_clock"]:
                            x, y = previous_bigs[key]
                            room["observed_bodies"][key] = (tick, (x, y) in new_smalls
                                                            and (x + 1, y + 1) in new_smalls)
                    previous_bigs = bigs
                key = int(fields[8])
                if key >= 0:
                    room["actions"].append((tick, role, wave, key, fields[9], fields[10]))
                if fields[10].startswith(("Wait for fresh crab", "Approach fresh during cooldown",
                                          "Prepare finite fresh opportunity")):
                    room["fresh_stages"].append((tick, role, fields[4] == "true", fields[10]))
            elif kind in ("NYLO_PRIMARY", "NYLO_ROLL", "NYLO_IMPACT") and 21 not in room["spawns"]:
                tick, role, key = map(int, fields[1:4])
                if kind == "NYLO_PRIMARY":
                    room["primaries"].append((tick, role, key, int(fields[4]), fields[6]))
                elif kind == "NYLO_ROLL":
                    room["rolls"].append((tick, role, key, int(fields[5])))
                else:
                    room["impacts"].append((tick, role, key, int(fields[5]), fields[7] == "true"))
    if len(rooms) != len(trials):
        raise ValueError("Trace room count differs from the trial CSV")
    return rooms


def metrics(room):
    births = room["births"]
    one, = [key for key, b in births.items() if b["wave"] == 1 and b["style"] == "MELEE"
            and (b["x"], b["y"]) == (3310, 4249)]
    six, = [key for key, b in births.items() if b["wave"] == 6 and b["style"] == "MAGIC"
            and (b["x"], b["y"]) == (3281, 4249)]
    first6 = {role: min((t-births[six]["tick"] for t, r, k, *_ in room["primaries"]
                         if r == role and k == six), default=None) for role in (0, 1)}
    counts = Counter()
    examples = []
    max_wait = {}
    for role in (0, 1):
        launches = sorted(t for t, r, *_ in room["primaries"] if r == role)
        waits = Counter()
        for tick, actor, ready, reason in room["fresh_stages"]:
            if actor != role:
                continue
            counts[f"role{role}_fresh_{'ready_wait' if ready else 'cooldown_stage'}_frames"] += 1
            if ready:
                previous = max((t for t in launches if t < tick), default=-1)
                waits[previous] += 1
        max_wait[role] = max(waits.values(), default=0)
    actions = {(t, r): (w, k, rule, reason) for t, r, w, k, rule, reason in room["actions"]}
    for tick, role, wave, key, rule, reason in room["actions"]:
        if key == one:
            counts[f"role{role}_wave1_melee_selected_frames"] += 1
    shots = {(t, r): k for t, r, k, *_ in room["primaries"]}
    for tick, role, key, weapon, rule in room["primaries"]:
        if key == one:
            counts[f"role{role}_wave1_melee_primary_requests"] += 1
            if abs(births[six]["tick"]-tick) <= 2:
                counts[f"role{role}_wave1_melee_primary_within_two_of_west6"] += 1
        body = births.get(key)
        action = actions.get((tick, role))
        if body and action and action[0] < 20 and 0 <= tick - body["tick"] <= 2:
            counts[f"role{role}_primary_on_age_zero_to_two"] += 1
        if body is None or body["big"] or rule != "null" or action is None:
            continue
        wave = action[0]
        if wave >= 20 or wave-body["wave"] < 4:
            continue
        counts[f"role{role}_ordinary_old_small_requests"] += 1
        upcoming = [(k, b) for k, b in births.items() if tick < b["tick"] <= tick+2]
        if upcoming:
            counts[f"role{role}_old_small_with_observed_birth_next_two"] += 1
            if len(examples) < 4:
                examples.append(dict(tick=tick, role=role, wave=wave, target=key,
                    target_wave=body["wave"], weapon=weapon,
                    births=[dict(key=k, **b) for k, b in upcoming]))
    for tick, role, key, damage in room["rolls"]:
        if key == one and shots.get((tick, role)) != key:
            counts[f"role{role}_wave1_melee_other_primary_rolls"] += 1
            if damage > 0:
                counts[f"role{role}_wave1_melee_other_primary_positive_rolls"] += 1
            if len(examples) < 8:
                examples.append(dict(tick=tick, role=role, wave1_melee=key,
                    same_tick_primary=shots.get((tick, role)), rolled_damage=damage))
    for tick, role, key, damage, thrall in room["impacts"]:
        if key == one and damage > 0:
            source = "thrall" if thrall else "player"
            counts[f"role{role}_wave1_melee_positive_{source}_impacts"] += 1
    clock = Counter()
    for key, (actual, both_children) in room["observed_bodies"].items():
        due = room["split_clock"][key]["due"]
        clock["observed_removals"] += 1
        clock["both_children_at_predicted_squares"] += both_children
        clock["all_forecasts_equal_observed_removal"] += due == {actual}
    return dict(seed=room["seed"], counts=dict(counts), first_west6_request_age=first6,
                split_clock=dict(clock),
                max_explicit_ready_waits_between_primaries=max_wait, examples=examples)


def audit(directory):
    rooms = [metrics(room) for room in read_rooms(directory)]
    counts, ns, clocks = Counter(), Counter(), Counter()
    ages = {0: Counter(), 1: Counter()}
    waits = {0: Counter(), 1: Counter()}
    for room in rooms:
        counts.update(room["counts"])
        clocks.update(room["split_clock"])
        ns.update(room["counts"].keys())
        for role, age in room["first_west6_request_age"].items():
            ages[role]["none" if age is None else str(age)] += 1
            waits[role][room["max_explicit_ready_waits_between_primaries"][role]] += 1
    return dict(rooms=len(rooms), role_order=["ranger", "mage"], counts=dict(counts),
                rooms_with_event=dict(ns), first_west6_request_age_histogram=ages,
                pre_cap_observed_split_clock=dict(clocks),
                maximum_explicit_ready_waits_between_primaries_per_room=waits,
                note="The old-small screen uses subsequently observed births. Their presence alone "
                     "does not establish advance knowledge, line of sight or a spare packet budget.",
                per_room=rooms)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = audit(args.directory)
    (args.output or args.directory / "precap-freshness.json").write_text(
        json.dumps(result, indent=2) + "\n")
    print(json.dumps({k: v for k, v in result.items() if k != "per_room"}, indent=2))
