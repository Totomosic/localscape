#!/usr/bin/env python3
"""Screen confirmed cleanup shots at older smalls while newer same-style work exists.

Distance and observed readiness are screens, not proof of an unobstructed ray.
Read-only native forecasts include queued damage already rolled by the server.
"""
import argparse
from collections import Counter
import csv
import hashlib
import json
from pathlib import Path


def audit(directory, phase='cleanup'):
    rows = list(csv.DictReader((directory/'trials.csv').open()))
    mage_claims, trial = {}, -1
    for line in (directory/'server.log').open():
        a = line.strip().split('\t')
        if a[0] != 'NYLO_TRACE':
            continue
        if a[1:3] == ['0', '0']:
            trial += 1
        if a[2] == '1' and a[4] == 'true' and int(a[8]) >= 0 and not a[10].startswith('Plan '):
            # Conservative reconstruction of the temporary planning claim. Native
            # food/utility rejection can make this screen exclude extra cases.
            mage_claims[trial, int(a[1])] = int(a[8])
    trial, identities, forecasts, nulls, last_attack = -1, {}, {}, {}, None
    previous_ranger = None
    counts, examples, rooms = Counter(), [], set()
    styles = {'PIPE': ('RANGE', 5), 'CHIN': ('RANGE', 9), 'EYE': ('MAGIC', 8)}
    for line in (directory/'server.log').open():
        a = line.lstrip().rstrip('\n').split('\t')
        if a[0] == 'NYLO_FORECAST':
            forecasts[int(a[1]), int(a[2])] = (int(a[3]), int(a[4]))
        elif a[0] == 'NYLO_NULL':
            nulls[int(a[1]), int(a[2])] = set(map(int, a[3].split(','))) if len(a) > 3 and a[3] else set()
        elif a[0] == 'NYLO_TRACE':
            tick, role, wave = map(int, a[1:4])
            if tick == 0 and role == 0:
                trial += 1
                identities, forecasts, nulls, last_attack = {}, {}, {}, None
                previous_ranger = None
            crabs = {}
            for raw in a[11].split(';'):
                if not raw:
                    continue
                c = raw.split(',')
                key, birth, age = map(int, c[:3])
                x, y = map(int, c[6:8])
                if key not in identities:
                    lane = 'west' if x == 3281 else 'east' if 3309 <= x <= 3310 else 'south' if y == 4233 else 'split'
                    identities[key] = dict(key=key, wave=birth, born=tick-age, lane=lane,
                        slot=('W' if x == 3295 else 'E') if lane == 'south' else ('N' if y == 4249 else 'S'), big=c[4] == 'true')
                crabs[key] = dict(identities[key], age=age, style=c[3], hp=int(c[5]), x=x, y=y,
                    claimed_until=int(c[8]), own_hit_until=int(c[9]), dying=c[10] == 'true',
                    nulled=key in nulls.get((tick, role), set()))
            previous_attack = last_attack
            previous_plan = previous_ranger
            if role == 0:
                previous_ranger = dict(tick=tick, key=int(a[8]), weapon=a[7], reason=a[10])
            if role == 0 and a[5] == 'true':
                last_attack = int(a[8])
            scope = wave == 31 if phase == 'cleanup' else 0 < wave < 20
            if role != 0 or not scope or a[5] != 'true' or a[7] not in styles:
                continue
            target = crabs.get(int(a[8]))
            if not target or target['big'] or phase == 'cleanup' and target['wave'] < 30:
                continue
            counts['fresh_small_native_shots'] += 1
            if phase == 'cleanup' and previous_plan and previous_plan['tick'] == tick-1:
                old = crabs.get(previous_plan['key'])
                old_hp, old_damage = forecasts.get((tick, previous_plan['key']), (-1, -1))
                if (old and old['lane'] == 'split' and not old['big'] and old['hp'] > 0
                    and not old['dying'] and not old['nulled'] and old['age'] < 49
                    and previous_plan['reason'].startswith('Plan ')
                    and previous_plan['weapon'] == a[7] and target['born'] > old['born']
                    and target['lane'] == 'split' and target['style'] == old['style']
                    and old_damage >= 0 and old_damage < old_hp
                    and mage_claims.get((trial, tick)) != old['key']):
                    counts['planned_old_to_newer_native_launch'] += 1
            if phase == 'precap' and target['key'] == previous_attack:
                counts['precap_latest_attack_followup'] += 1
                continue
            # Named packets and urgent original31 rangers retain their explicit priority.
            if a[9] != 'null' or phase == 'cleanup' and target['wave'] == 31 and target['lane'] != 'split' and target['age'] < 26:
                continue
            px, py = map(int, a[6].split(',')[:2])
            style, maximum = styles[a[7]]
            alternatives = []
            for candidate in crabs.values():
                if candidate['big'] or candidate['lane'] != 'split' or phase == 'cleanup' and candidate['wave'] < 30 or candidate['hp'] <= 0 or candidate['dying'] or candidate['nulled']:
                    continue
                if candidate['style'] != style or candidate['age'] >= 49 or candidate['born'] < target['born'] + 4:
                    continue
                if mage_claims.get((trial, tick)) == candidate['key']:
                    continue
                hp, damage = forecasts.get((tick, candidate['key']), (-1, -1))
                if damage >= hp > 0:
                    continue
                if damage < 0 and max(candidate['claimed_until'], candidate['own_hit_until']) > tick:
                    continue
                if max(abs(candidate['x']-px), abs(candidate['y']-py)) > maximum:
                    continue
                # Match next-color safety for original31 smalls. Splits keep their birth style.
                if candidate['lane'] != 'split' and candidate['wave'] == 31 and candidate['age'] in (4, 6, 25):
                    continue
                alternatives.append(dict(candidate, forecast=[hp, damage]))
            if alternatives:
                counts['old_small_with_newer_same_style_screen'] += 1
                counts[a[7]+'_screen'] += 1
                rooms.add(trial)
                if len(examples) < 40:
                    examples.append(dict(seed=rows[trial]['seed'], tick=tick, weapon=a[7], target=target,
                        player=a[6], reason=a[10], alternatives=alternatives,
                        observation=dict(wave=wave, crabs=list(crabs.values()))))
    return dict(run=str(directory), phase=phase, trials=len(rows), counts=dict(counts), rooms=len(rooms), examples=examples,
        caveat=__doc__ + ' Mage ready selections conservatively exclude temporary same-cycle claims; '
              'food/utility rejection may exclude extra alternatives. Planned-old-to-newer transitions '
              'are observed transitions, not an instrumented counter of guard activation.',
        sources={str(p): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in (directory/'server.log', directory/'trials.csv')})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('runs', nargs='+', type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--precap', action='store_true')
    args = parser.parse_args()
    results = [audit(directory, 'precap' if args.precap else 'cleanup') for directory in args.runs]
    args.output.write_text(json.dumps(results, indent=2) + '\n')
    for result in results:
        print(result['run'], result['counts'], 'rooms', result['rooms'])


if __name__ == '__main__':
    main()
