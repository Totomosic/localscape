#!/usr/bin/env python3
"""Capture ready native27/28 frames for optional permanent-range23East service.

This records actual actions and creates policy-budget probe fixtures. It does not
infer line of sight from range. Native delay and possible food gates are explicit.
"""
import argparse
from collections import Counter
import csv
import json
from pathlib import Path


def capture(directory):
    rows = list(csv.DictReader((directory/'trials.csv').open()))
    delays, hp_before, trial, previous_hp = {}, {}, -1, None
    for line in (directory/'server.log').open():
        a = line.strip().split('\t')
        if a[0] == 'NYLO_TRACE' and a[1:3] == ['0', '0']:
            trial += 1
        elif a[0] == 'NYLO_ENGINE':
            delays[trial, int(a[1])] = list(map(int, a[2].split(',')))
        elif a[0] == 'NYLO_PLAYERS':
            if int(a[1]) > 0:
                hp_before[trial, int(a[1])] = previous_hp
            previous_hp = list(map(int, a[2].split(',')))
    trial, identities, forecast, nulls, spawns, completed = -1, {}, {}, {}, {}, set()
    results, counts = [], Counter()
    for line in (directory/'server.log').open():
        a = line.lstrip().rstrip('\n').split('\t')
        if a[0] == 'NYLO_FORECAST':
            forecast[int(a[1]), int(a[2])] = (int(a[3]), int(a[4]))
        elif a[0] == 'NYLO_NULL':
            nulls[int(a[1]), int(a[2])] = set(map(int, a[3].split(','))) if len(a) > 3 and a[3] else set()
        elif a[0] == 'NYLO_TRACE':
            tick, role, wave = map(int, a[1:4])
            if tick == 0 and role == 0:
                trial += 1
                identities, forecast, nulls, spawns, completed = {}, {}, {}, {}, set()
            if role == 0:
                spawns.setdefault(wave, tick)
            crabs = []
            for raw in a[11].split(';'):
                if not raw:
                    continue
                c = raw.split(',')
                key, birth, age = map(int, c[:3])
                x, y = map(int, c[6:8])
                if key not in identities:
                    lane = 'WEST' if x == 3281 else 'EAST' if 3309 <= x <= 3310 else 'SOUTH' if y == 4233 else 'SPLIT'
                    slot = ('W' if x == 3295 else 'E') if lane == 'SOUTH' else ('N' if y == 4249 else 'S') if lane != 'SPLIT' else ''
                    identities[key] = dict(key=key, wave=birth, born=tick-age, lane=lane, slot=slot, big=c[4] == 'true')
                hp, damage = forecast.get((tick, key), (-1, -1))
                crabs.append(dict(identities[key], age=age, style=c[3], hp=int(c[5]), x=x, y=y,
                    claimedUntil=int(c[8]), ownHitUntil=int(c[9]), dying=c[10] == 'true', pendingDamage=damage,
                    nulled=key in nulls.get((tick, 0), set()), partnerNulled=key in nulls.get((tick, 1), set())))
            completed_before = sorted(completed)
            if role == 0 and a[5] == 'true' and a[9] != 'null':
                completed.add(a[9])
            if role != 0 or wave not in (27, 28):
                continue
            target = next((c for c in crabs if c['wave'] == 23 and c['big'] and c['lane'] == 'EAST'), None)
            if not target or target['style'] != 'RANGE' or target['age'] < 26 or target['age'] >= 49 or target['hp'] <= 0 or target['dying']:
                continue
            if target['nulled'] or target['pendingDamage'] >= target['hp']:
                continue
            if target['pendingDamage'] < 0 and max(target['claimedUntil'], target['ownHitUntil']) > tick:
                continue
            counts['uncovered_live_parent_frames'] += 1
            if a[4] != 'true':
                counts['policy_cooldown_frames'] += 1
                continue
            delay = delays.get((trial, tick), [None, None])[0]
            previous = hp_before.get((trial, tick))
            if delay is None or delay > 0:
                counts['native_delay_frames'] += 1
                continue
            if previous is None or previous[0] <= 65:
                counts['possible_food_frames'] += 1
                continue
            kind = 'confirmed_useful_attack' if a[5] == 'true' else 'own_approach' if int(a[8]) >= 0 else 'prepare_or_reserve'
            counts[kind] += 1
            results.append(dict(run=directory.name, trial=trial, seed=rows[trial]['seed'], tick=tick, wave=wave,
                target_key=target['key'], player=list(map(int, a[6].split(',')[:2])), spawns=spawns.copy(),
                ready=tick, completed=completed_before, crabs=crabs, current_kind=kind,
                current_weapon=a[7], current_target=int(a[8]), current_rule=a[9], current_reason=a[10],
                native_delay=delay, previous_hp=previous[0]))
    return dict(run=str(directory), trials=len(rows), counts=dict(counts), frames=results, caveat=__doc__)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('runs', nargs='+', type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    results = [capture(directory) for directory in args.runs]
    args.output.write_text(json.dumps(results, indent=2) + '\n')
    for result in results:
        print(result['run'], result['counts'])


if __name__ == '__main__':
    main()
