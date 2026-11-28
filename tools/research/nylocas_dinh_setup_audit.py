#!/usr/bin/env python3
"""Measure the opening step and the wave 12 and 22 Dinh specials in packed-map rooms."""

import argparse
from collections import Counter, defaultdict
from dataclasses import dataclass, field
import json
from pathlib import Path


@dataclass
class Room:
    births: dict = field(default_factory=dict)
    east12: set = field(default_factory=set)
    positions: dict = field(default_factory=dict)
    waves: dict = field(default_factory=dict)
    attacks: dict = field(default_factory=dict)
    impacts: dict = field(default_factory=lambda: defaultdict(list))
    rolls: dict = field(default_factory=lambda: defaultdict(set))
    clear_shots: dict = field(default_factory=dict)

    def observe(self, fields):
        kind, tick = fields[0], int(fields[1])
        if kind == "NYLO_ENGINE":
            x, y, _, mx, my, _ = map(int, fields[4].split(","))
            self.positions[tick] = (x, y, mx, my)
        elif kind == "NYLO_PRIMARY" and fields[2] == "1":
            self.attacks[tick] = (int(fields[3]), int(fields[4]), fields[6])
        elif kind == "NYLO_IMPACT" and fields[2] == "1" and fields[7] == "false":
            key, attack, damage, hp = map(int, fields[3:7])
            self.impacts[tick].append((key, attack, damage, hp))
        elif kind == "NYLO_ROLL" and fields[2] == "1" and fields[4] == "21015":
            self.rolls[tick].add(int(fields[3]))
        elif kind == "NYLO_TRACE":
            if fields[2] == "1":
                if fields[10] == "Clear south magic before 22 Dinh" and fields[5] == "true":
                    self.clear_shots[tick] = int(fields[8])
                return
            wave = int(fields[3])
            self.waves.setdefault(wave, tick)
            if wave not in (12, 22):
                return
            for raw in fields[11].split(";"):
                if not raw:
                    continue
                key, born, age, _, big, _, x, y, *_ = raw.split(",")
                if born == "12" and age == "0" and big == "false" and int(x) >= 3309:
                    self.east12.add(int(key))
                if int(age) == 0 and big == "true":
                    lane = "west" if x == "3281" else "east" if int(x) >= 3309 else "south" if y == "4233" else None
                    if lane:
                        self.births[int(born), lane] = int(key)

    def summarize(self, totals):
        totals["rooms"] += 1
        south_eyes = {
            tick: key for tick, key in self.clear_shots.items()
            if self.attacks.get(tick, ())[:2] == (key, 31113)
        }
        if south_eyes:
            totals[f"wave22_rooms_with_{len(south_eyes)}_south_eyes"] += 1
        opening = next((t for t, a in self.attacks.items() if a[1:] == (27275, "Opening south")), None)
        if opening is not None:
            totals["opening_shadow"] += 1
            totals["west_step_next_tick"] += self.positions[opening + 1][2] < self.positions[opening][2]
        for tick, (primary, weapon, rule) in self.attacks.items():
            if weapon == 22325 and rule == "12 east scythe":
                hits = self.impacts[tick + 1]
                ids = {attack for key, attack, _, _ in hits if key == primary}
                targets = {key for key, attack, _, _ in hits if attack in ids}
                totals["wave12_east_scythes"] += 1
                totals["wave12_east_both_hit_by_scythe"] += len(self.east12 & targets) == 2
                row = self.positions[tick][3]
                totals[f"wave12_east_scythe_from_row_{row}"] += 1
                totals[f"wave12_east_both_hit_from_row_{row}"] += len(self.east12 & targets) == 2
                continue
            if weapon != 21015 or rule not in ("12 west Dinh", "22 south Dinh"):
                continue
            wave = 12 if rule.startswith("12 ") else 22
            prefix = f"wave{wave}"
            totals[prefix + "_specials"] += 1
            # The primary identifies this special's native attack ID, shared by all its splats.
            following = self.impacts[tick + 1]
            ids = {attack for key, attack, _, _ in following if key == primary}
            hits = [hit for hit in following if hit[1] in ids]
            kills = {key for key, _, damage, hp in hits if damage > 0 and hp == 0}
            totals[prefix + "_attack_ids_identified"] += bool(ids)
            totals[prefix + "_victims_killed"] += len(kills)
            if wave == 12:
                big = self.births[12, "south"]
                totals["wave12_big_rolled_on"] += big in self.rolls[tick]
                totals["wave12_big_killed_by_special"] += big in kills
                _, ry, _, my = self.positions[tick]
                totals["wave12_ranger_north_at_launch"] += ry >= 4249
                totals["wave12_mage_north_at_launch"] += my >= 4249
                totals["wave12_both_north_at_launch"] += min(ry, my) >= 4249
            else:
                age = tick - self.waves[22]
                totals[f"wave22_launch_age_{age}"] += 1
                if south_eyes:
                    totals["wave22_special_after_south_clear"] += 1
                    totals["wave22_kills_on_delayed_specials"] += len(kills)
                    totals["wave22_delayed_big_primary"] += primary in (
                        self.births[22, "east"], self.births[22, "west"])
                    for lane in ("east", "west"):
                        if primary == self.births[22, lane]:
                            totals[f"wave22_delayed_{lane}_big_primary"] += 1
                            totals[f"wave22_delayed_{lane}_big_killed_as_primary"] += primary in kills
        totals["wave22_rooms_with_south_clear"] += bool(south_eyes)
        for tick, key in south_eyes.items():
            # Only Eye's primary at its legal flight time can be credited for the kill.
            later = [(t, k, attack, damage, hp) for t in range(tick + 2, tick + 4)
                     for k, attack, damage, hp in self.impacts[t] if k == key]
            totals["wave22_south_clear_shots"] += 1
            totals["wave22_south_clear_lethal_impacts"] += any(damage > 0 and hp == 0 for _, _, _, damage, hp in later)


def audit(path):
    totals = Counter()
    room = None
    with (path / "server.log").open() as log:
        for line in log:
            fields = line.lstrip().rstrip("\r\n").split("\t")
            if fields[0] == "NYLO_PLAYERS" and fields[1] == "0":
                if room is not None:
                    room.summarize(totals)
                room = Room()
            if room is not None and fields[0] in {"NYLO_ENGINE", "NYLO_PRIMARY", "NYLO_IMPACT", "NYLO_ROLL", "NYLO_TRACE"}:
                room.observe(fields)
    if room is not None:
        room.summarize(totals)
    return dict(sorted(totals.items()))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("rooms", type=Path, nargs="+")
    args = parser.parse_args()
    print(json.dumps({p.name: audit(p) for p in args.rooms}, indent=2))
