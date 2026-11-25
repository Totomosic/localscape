#!/usr/bin/env python3
"""Screen early wave-18 magic windows without treating distance as a proof of LOS."""
import argparse
from collections import Counter
import csv
import hashlib
import json
import math
from pathlib import Path


def audit(directory):
    rows = list(csv.DictReader((directory / 'trials.csv').open()))
    delays, planning, hp_before, trial, previous_hp = {}, {}, {}, -1, None
    for line in (directory/'server.log').open():
        a = line.strip().split('\t')
        if a[0] == 'NYLO_TRACE' and a[1:3] == ['0', '0']:
            trial += 1
        elif a[0] == 'NYLO_ENGINE':
            delays[trial, int(a[1])] = list(map(int, a[2].split(',')))
        elif a[0] == 'NYLO_PLAYERS':
            tick = int(a[1])
            if tick > 0:
                hp_before[trial, tick] = previous_hp
            previous_hp = list(map(int, a[2].split(',')))
        elif a[0] == 'NYLO_TRACE' and a[2] == '1' and a[4] == 'true' and int(a[8]) >= 0 and not a[10].startswith('Plan '):
            planning[trial, int(a[1])] = int(a[8])
    trial, origins, forecast, nulls = -1, {}, {}, {}
    counts, examples, failed19_windows, spawn18 = Counter(), [], [], None
    failed19 = {i for i, row in enumerate(rows) if int(row['stalls_by_last_spawned_wave'].split(';')[19]) > 0}
    for line in (directory / 'server.log').open():
        a = line.lstrip().rstrip('\n').split('\t')
        if a[0] == 'NYLO_FORECAST':
            forecast[int(a[1]), int(a[2])] = (int(a[3]), int(a[4]))
        elif a[0] == 'NYLO_NULL':
            nulls[int(a[1]), int(a[2])] = set(map(int, a[3].split(','))) if len(a) > 3 and a[3] else set()
        elif a[0] == 'NYLO_TRACE':
            tick, role, wave = map(int, a[1:4])
            if tick == 0 and role == 0:
                trial += 1
                origins = {}
                # Initial tick has no live wave-18 forecast; discard previous room entries.
                forecast, nulls = {}, {}
                spawn18 = None
            if role == 0 and wave == 18 and spawn18 is None:
                spawn18 = tick
            failure_case = None
            if role == 0 and wave == 18 and trial in failed19 and tick == spawn18 + 5:
                failure_case = dict(seed=rows[trial]['seed'], tick=tick,
                    wave19_stalls=int(rows[trial]['stalls_by_last_spawned_wave'].split(';')[19]),
                    target=None, gates=['west_parent_already_removed'])
            for raw in a[11].split(';'):
                if not raw:
                    continue
                c = raw.split(',')
                key, birth, age = map(int, c[:3])
                x, y = map(int, c[6:8])
                origins.setdefault(key, 'west' if x == 3281 else 'east' if 3309 <= x <= 3310 else 'south' if y == 4233 else 'split')
                lane = origins[key]
                hp, damage = forecast.get((tick, key), (-1, -1))
                gates = []
                if role == 0 and wave == 18 and birth == 18 and c[4] == 'true' and age == 5 and lane == 'west':
                    px, py = map(int, a[6].split(',')[:2])
                    distance = max(x-px, px-x-1, y-py, py-y-1)
                    native_delay = delays.get((trial, tick), [None, None])[0]
                    previous = hp_before.get((trial, tick))
                    if c[10] == 'true' or int(c[5]) <= 0: gates.append('dead_or_dying')
                    if c[3] != 'MAGIC': gates.append('not_magic')
                    if key in nulls.get((tick, 0), set()): gates.append('ranger_null')
                    if damage >= hp > 0: gates.append('known_lethal')
                    if damage < 0 and max(map(int, c[8:10])) > tick: gates.append('unknown_lease')
                    if a[4] != 'true': gates.append('policy_cooldown')
                    if native_delay is None or native_delay > 0: gates.append('native_action_delay')
                    if previous is None or previous[0] <= 65: gates.append('food_due_possible')
                    mage_claim = planning.get((trial, tick)) == key and delays.get((trial, tick), [None, 1])[1] <= 0
                    if mage_claim: gates.append('mage_planning_claim_possible')
                    if distance > 8: gates.append('outside_eye8')
                    earliest19 = tick - age + 11
                    return_cost = max(3, math.ceil(max(abs(px-3293), abs(py-4248))/2))
                    if tick + return_cost > earliest19: gates.append('west19_return_budget')
                    record = dict(seed=rows[trial]['seed'], tick=tick, key=key, lane=lane,
                        hp=int(c[5]), style=c[3], forecast=[hp, damage], target=[x, y], player=a[6],
                        selected=[a[7], a[8], a[9]], native_attack=a[5] == 'true', reason=a[10],
                        native_action_delay=native_delay, previous_player_hp=previous,
                        wave19_stalls=int(rows[trial]['stalls_by_last_spawned_wave'].split(';')[19]),
                        earliest_west19_due=earliest19, eye_plus_return=tick+return_cost,
                        gates=gates, matched_human_eye_corridor=py == 4248 and 3291 <= px <= 3293 and y == 4248)
                    if failure_case is not None:
                        failure_case = record
                    if not gates:
                        counts['west_native_gate_screen'] += 1
                        if a[5] == 'true':
                            kind = 'eye5' if a[7] == 'EYE' and int(a[8]) == key else 'other_native_attack'
                            counts['west_screen_' + kind] += 1
                        if trial in failed19: counts['west_native_gate_screen_failed19'] += 1
                        if record['matched_human_eye_corridor']: counts['west_screen_human_corridor'] += 1
                        if len(examples) < 30: examples.append(record)
                if role != 0 or wave != 18 or birth != 18 or c[4] != 'true' or age != 5 or c[3] != 'MAGIC':
                    continue
                if c[10] == 'true' or key in nulls.get((tick, role), set()) or int(c[5]) <= 0:
                    continue
                if damage >= hp > 0:
                    continue
                if damage < 0 and max(map(int, c[8:10])) > tick:
                    continue
                counts['nonlethal_magic5_' + lane] += 1
                if a[4] != 'true':
                    continue
                px, py = map(int, a[6].split(',')[:2])
                distance = max(x-px, px-x-1, y-py, py-y-1)
                if distance > 8:
                    continue
                counts['ready_eye8_' + lane] += 1
                if a[5] == 'true':
                    kind = 'eye5' if a[7] == 'EYE' and int(a[8]) == key else 'other_native_attack'
                    counts[kind + '_' + lane] += 1
            if failure_case is not None:
                failed19_windows.append(failure_case)
    return dict(run=str(directory), trials=len(rows), failed19_rooms=len(failed19), counts=dict(counts), examples=examples,
        failed19_west_magic5_windows=failed19_windows,
        caveat='Eye8 distance and native readiness are opportunity screens; target LOS is not emitted. '
               'Predecision queued damage is authoritative; ordinary trace leases are postcycle. '
               'Native action delay, possible food, same-cycle Mage claim and conservative first19-return '
               'budget gates are checked, but a passing screen is not a proof of target LOS. '
               'Human-corridor matches cite actual vetted Eye launches, not inferred human NULL.',
        sources={str(p): hashlib.sha256(p.read_bytes()).hexdigest()
                 for p in (directory/'server.log', directory/'trials.csv')})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('runs', nargs='+', type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    results = [audit(directory) for directory in args.runs]
    args.output.write_text(json.dumps(results, indent=2) + '\n')
    for result in results:
        print(result['run'], result['counts'])


if __name__ == '__main__':
    main()
