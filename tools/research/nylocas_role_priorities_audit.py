#!/usr/bin/env python3
"""Audit original bigs, ranger shots after the south chin, and manual movement in native traces."""

import argparse
from collections import Counter, defaultdict
import csv
from dataclasses import dataclass, field
import hashlib
import json
import math
from pathlib import Path
from statistics import mean, median


PARENTS = {"20S", "23S", "24W", "24S", "24E", "25W", "26W", "27W", "29S", "30E", "30W"}
RANGES = {"BOW": 10, "PIPE": 5, "CHIN": 9, "EYE": 8, "SHADOW": 10, "BARRAGE": 10,
          "CLAWS": 1, "DINH": 1, "SCYTHE": 1, "SULPHUR": 1}
ROLES = ("ranger", "mage")


@dataclass
class Room:
    row: dict
    parents: dict = field(default_factory=dict)
    south_smalls: set = field(default_factory=set)
    first_dead: dict = field(default_factory=dict)
    last_hp: dict = field(default_factory=dict)
    pending: dict = field(default_factory=dict)
    unavailable: set = field(default_factory=set)
    primaries: dict = field(default_factory=dict)
    impacts: dict = field(default_factory=dict)
    inputs: dict = field(default_factory=dict)
    moving_from: dict = field(default_factory=dict)
    precap: Counter = field(default_factory=Counter)
    waits: list = field(default_factory=lambda: [0, 0])
    max_wait: list = field(default_factory=lambda: [0, 0])
    movement: Counter = field(default_factory=Counter)
    west27_selections: int = 0
    west27_unavailable_selections: int = 0
    west8: tuple | None = None
    west8_retry_due: bool = False

    def frame(self, parts):
        tick, role, wave = map(int, parts[1:4])
        ready = parts[4] == "true"
        selected = int(parts[8])
        bodies = [raw.split(",") for raw in parts[11].split(";") if raw]
        selected_body = next((b for b in bodies if int(b[0]) == selected), None)
        if role == 0 and wave in (8, 9):
            for b in bodies:
                if b[1:3] == ["8", "0"] and b[4] == "true" and b[6] == "3281":
                    self.west8 = (int(b[0]), tick)
                if self.west8 == (int(b[0]), tick - 3):
                    self.west8_retry_due = b[10] == "false" and int(b[5]) > self.pending.get((tick, int(b[0])), 0)
        if role == 0 and wave >= 20:
            for b in bodies:
                key, birth_wave, age = map(int, b[:3])
                hp, x, y = map(int, b[5:8])
                if age == 0:
                    lane = "W" if x == 3281 else "E" if x >= 3309 else "S" if y == 4233 else ""
                    tag = f"{birth_wave}{lane}"
                    if b[4] == "true" and tag in PARENTS:
                        self.parents[tag] = (key, tick)
                    if birth_wave == 30 and lane == "S" and b[4] == "false":
                        self.south_smalls.add(key)
                self.last_hp[key] = hp
                if hp <= 0 or b[10] == "true":
                    self.first_dead.setdefault(key, tick)
                if self.parents.get("30W", (-1,))[0] == key:
                    if key in self.first_dead or self.pending.get((tick, key), 0) >= hp:
                        self.unavailable.add(tick)
        if selected >= 0 and selected == self.parents.get("27W", (-1,))[0]:
            self.west27_selections += 1
            other_alive = any(int(b[0]) != selected and int(b[5]) > 0 and b[10] == "false" for b in bodies)
            x, y = map(int, selected_body[6:8])
            in_lane = x <= 3291 and (y == 4248 or x >= 3290 and abs(y - 4248) <= 1)
            if other_alive or int(selected_body[2]) > 9 or not in_lane:
                self.west27_unavailable_selections += 1
        name = ROLES[role]
        if 1 <= wave < 20:
            if parts[10].startswith(("Wait for fresh crab", "Prepare finite fresh opportunity")) and ready:
                self.precap[f"{name}_fresh_ready_waits"] += 1
                self.waits[role] += 1
                self.max_wait[role] = max(self.max_wait[role], self.waits[role])
            shot = self.primaries.get((tick, role))
            if shot:
                self.waits[role] = 0
                primary = next((b for b in bodies if int(b[0]) == shot[0]), None)
                if primary and primary[4] == "false" and shot[2] == "null" and wave - int(primary[1]) >= 2:
                    self.precap[f"{name}_ordinary_small_two_or_more_waves_old"] += 1
        action = self.inputs.get((tick, role))
        if action:
            key, kind, sight, dest = action
            if kind == "attack":
                if key != selected or selected_body is None:
                    raise ValueError("NPC input and policy target disagree")
                x, y = map(int, parts[6].split(",")[:2])
                nx, ny = map(int, selected_body[6:8])
                size = int(selected_body[4] == "true")
                distance = max(nx - x, x - nx - size, ny - y, y - ny - size)
                self.movement[f"{name}_npc_clicks"] += 1
                self.movement[f"{name}_blocked_npc_clicks"] += sight != "true"
                self.movement[f"{name}_beyond_one_cycle_npc_clicks"] += distance > RANGES[parts[7]] + 3
            elif 1 <= wave < 20 and dest != parts[6] and not ready:
                self.movement[f"{name}_precap_cooldown_ground_moves"] += 1
                self.moving_from[tick, role] = parts[6]


def read_rooms(directory):
    with (directory / "trials.csv").open() as source:
        rows = list(csv.DictReader(source))
    room = None
    index = 0
    with (directory / "server.log").open() as source:
        for line in source:
            p = line.lstrip(" ").rstrip("\r\n").split("\t")
            kind = p[0]
            if kind == "NYLO_PLAYERS" and p[1] == "0":
                if room is not None:
                    yield room
                room = Room(rows[index])
                index += 1
            if room is None:
                continue
            if kind == "NYLO_TRACE":
                room.frame(p)
            elif kind == "NYLO_INPUT":
                room.inputs[int(p[1]), int(p[2])] = (int(p[3]), *p[4:7])
            elif kind == "NYLO_ENGINE":
                coords = p[4].split(",")
                for role in (0, 1):
                    before = room.moving_from.get((int(p[1]), role))
                    if before and before != ",".join(coords[3*role:3*role+3]):
                        room.movement[f"{ROLES[role]}_precap_cooldown_ground_moves_with_step"] += 1
            elif kind == "NYLO_PRIMARY":
                room.primaries[int(p[1]), int(p[2])] = (int(p[3]), int(p[4]), p[6])
            elif kind == "NYLO_FORECAST":
                room.pending[int(p[1]), int(p[2])] = int(p[4])
            elif kind == "NYLO_NULL" and p[2] == "0":
                if room.parents.get("30W", (-1,))[0] in map(int, p[3].split(",")):
                    room.unavailable.add(int(p[1]))
            elif kind == "NYLO_IMPACT":
                tick, role, key, attack, damage = map(int, p[1:6])
                room.impacts[tick, role, key, attack, p[7]] = damage
    if room is not None:
        yield room
    if index != len(rows):
        raise ValueError("Trial count differs from trace")


def digest(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def audit(directories):
    counts, statuses, precap, movement = Counter(), Counter(), Counter(), Counter()
    ages, max_wait = defaultdict(list), defaultdict(int)
    expiries, exceptions, ticks, eligible, seeds = [], [], [], [], set()
    cohorts = {}
    sources = None
    for directory in directories:
        run = json.loads((directory / "summary.json").read_text())["run"]
        if run["max_hit_diagnostic"]:
            raise ValueError("Expected normal random damage")
        identity = {k: run[k] for k in ("fixture", "policy_sha256", "adapter_sha256")}
        if sources is not None and identity != sources:
            raise ValueError("Fixture or sources changed between runs")
        sources = identity
        for snapshot, hashes in (("policy-source", run["policy_sha256"]), ("adapter-source", run["adapter_sha256"])):
            if any(digest(directory / snapshot / path) != value for path, value in hashes.items()):
                raise ValueError(f"Source snapshot mismatch in {directory}")
        batch_seeds = []
        for room in read_rooms(directory):
            seed = int(room.row["seed"])
            if seed in seeds:
                raise ValueError(f"Overlapping seed {seed}")
            seeds.add(seed)
            batch_seeds.append(seed)
            statuses[room.row["status"]] += 1
            precap.update(room.precap)
            movement.update(room.movement)
            for i, role in enumerate(ROLES):
                max_wait[role] = max(max_wait[role], room.max_wait[i])
            failures19 = int(room.row["stalls_by_last_spawned_wave"].split(";")[19])
            counts["wave19_failure_rooms"] += failures19 > 0
            if room.row["status"] != "completed":
                continue
            boss = int(room.row["ticks"])
            ticks.append(boss)
            if not failures19:
                eligible.append(boss)
            if room.parents.keys() != PARENTS or len(room.south_smalls) != 2 or room.west8 is None:
                raise ValueError(f"Missing originals in completed seed {seed}")
            west8, born8 = room.west8
            counts["west8_eye_at_birth"] += room.primaries.get((born8, 1), ())[:2] == (west8, 31113)
            if room.west8_retry_due:
                counts["west8_uncovered_three_ticks_later"] += 1
                counts["west8_eye_retry_on_that_tick"] += room.primaries.get((born8 + 3, 1), ())[:2] == (west8, 31113)
            counts["south20_post_barrage_scythe"] += any(
                rule == "21 South20 scythe" for (_, role), (_, _, rule) in room.primaries.items() if role == 1)
            for tag, (key, born) in room.parents.items():
                if key in room.first_dead:
                    ages[tag].append(room.first_dead[key] - born)
                elif tag != "27W":
                    expiries.append(dict(seed=seed, parent=tag, last_hp=room.last_hp[key]))
            west27 = room.parents["27W"][0]
            counts["west27_selections"] += room.west27_selections
            counts["west27_outside_exception_selections"] += room.west27_unavailable_selections
            counts["west27_positive_impacts"] += sum(k == west27 and damage > 0
                for (_, _, k, _, _), damage in room.impacts.items())
            west30, born30 = room.parents["30W"]
            ranger = [(t, k, w, rule) for (t, role), (k, w, rule) in sorted(room.primaries.items()) if role == 0]
            throws = [t for t, k, w, rule in ranger if rule == "South chin flicker 30"
                      and k in room.south_smalls and w == 11959]
            if len(throws) != 1:
                exceptions.append(dict(seed=seed, south30_throws=throws))
                continue
            chin = throws[0]
            counts["south30_first_green"] += chin == born30 + 5
            impact_ids = [{aid for t, role, k, aid, thrall in room.impacts
                           if t == chin + 3 and role == 0 and k == original and aid >= 0 and thrall == "false"}
                          for original in room.south_smalls]
            counts["south30_both_hit_by_same_chin"] += bool(set.intersection(*impact_ids))
            # A lethal pending roll or own NULL releases the ranger's obligation on that tick.
            # Once the NPC disappears it can no longer be a serviceable parent.
            active = [t for t in range(chin + 1, born30 + 49)
                      if (t, west30) in room.pending and t not in room.unavailable]
            if active:
                counts["west30_uncovered_after_chin"] += 1
            active_shots = [(t, k) for t, k, _, _ in ranger if t in active]
            if active_shots:
                counts["west30_rooms_with_uncovered_ranger_shot"] += 1
                counts["west30_first_uncovered_shot_on_parent"] += active_shots[0][1] == west30
            others = [t for t, k in active_shots if k != west30]
            counts["west30_rooms_with_intervening_filler"] += bool(others)
            if others:
                exceptions.append(dict(seed=seed, other_ranger_primaries_while_west30_uncovered=others))
        cohorts[directory.name] = dict(rooms=len(batch_seeds), first_seed=batch_seeds[0], last_seed=batch_seeds[-1],
                                       trials_sha256=digest(directory / "trials.csv"), trace_sha256=digest(directory / "server.log"))
    def rank(ordered, proportion):
        return ordered[math.ceil(len(ordered) * proportion) - 1]
    eligible.sort()
    return dict(rooms=len(seeds), statuses=statuses, counts=counts, precap=precap, movement=movement,
                max_fresh_ready_ticks_between_primaries=dict(max_wait),
                parents={tag: dict(combat_deaths=len(ages[tag]), mean_age_at_zero_hp=round(mean(ages[tag]), 2)
                                   if ages[tag] else None, median_age_at_zero_hp=median(ages[tag]) if ages[tag] else None)
                         for tag in sorted(PARENTS)},
                expired_required_parents=expiries, order_exceptions=exceptions,
                boss=dict(eligible=len(eligible), min_ticks=min(eligible), mean_ticks=round(mean(eligible), 4),
                          p5_ticks=rank(eligible, .05), p95_ticks=rank(eligible, .95), max_ticks=max(eligible),
                          eligible_tick_histogram=dict(sorted(Counter(eligible).items())),
                          all_by_328=sum(t <= 328 for t in ticks), all_by_3m22=sum(t <= 336 for t in ticks))
                if eligible else None, cohorts=cohorts, sources=sources)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directories", nargs="+", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    args.output.write_text(json.dumps(audit(args.directories), indent=2) + "\n")
