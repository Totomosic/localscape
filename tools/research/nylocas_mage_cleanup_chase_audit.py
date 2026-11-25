#!/usr/bin/env python3
"""Observed native Mage cleanup attacks, split chase attempts, and fresh expiry.

Uses the confirmed actual31 timestamp cutoff. Replacement opportunities are a
bounded screen, not exact counterfactual policy replay or guaranteed legal hits.
"""
import argparse
from collections import Counter
import csv
import json
from pathlib import Path


def audit(directory, offset=16):
    rows = list(csv.DictReader((directory / 'trials.csv').open()))
    trials = []
    current = None
    origins, nulls, pending, previous, engines, observations = {}, set(), {}, {}, {}, []

    def finish():
        if current is None:
            return
        for observation in observations:
            engine = engines.get(observation['tick'])
            if engine is None or int(engine[0].split(',')[1]) > 0:
                continue
            current['counts']['actionable_no_launch_nonadjacent_split_sulphur'] += 1
            if len(current['chase_examples']) < 3:
                current['chase_examples'].append(observation)
        current['counts'] = dict(current['counts'])

    for line in (directory / 'server.log').open():
        f = line.strip(' \r\n').split('\t')
        if f[0] == 'NYLO_PLAYERS' and f[1] == '0':
            finish()
            row = rows[len(trials)]
            current = dict(seed=int(row['seed']), counts=Counter(), chase_examples=[],
                           eye_screen_examples=[], fresh_expiries=[], own_null_keys=[],
                           late29_null_keys=[])
            trials.append(current)
            origins, nulls, pending, previous, engines, observations = {}, set(), {}, {}, {}, []
            spawns = {}
        if current is None:
            continue
        if f[0] == 'NYLO_NULL' and f[2] == '1' and f[3]:
            nulls.update(map(int, f[3].split(',')))
        if f[0] == 'NYLO_FORECAST':
            pending[int(f[1]), int(f[2])] = int(f[4])
        if f[0] == 'NYLO_ENGINE':
            engines[int(f[1])] = f[2:]
        if f[0] != 'NYLO_TRACE' or f[2] != '1':
            continue
        tick, wave = int(f[1]), int(f[3])
        spawns.setdefault(wave, tick)
        crabs = {}
        for raw in f[11].split(';'):
            if not raw:
                continue
            z = raw.split(',')
            key, x, y = int(z[0]), int(z[6]), int(z[7])
            origins.setdefault(key, 'west' if x == 3281 else 'east' if x >= 3309
                               else 'south' if y == 4233 else 'split')
            crabs[key] = dict(key=key, wave=int(z[1]), born=tick-int(z[2]), age=int(z[2]),
                              style=z[3], big=z[4] == 'true', hp=int(z[5]), x=x, y=y,
                              claim=int(z[8]), flight=int(z[9]), dying=z[10] == 'true',
                              pending=pending.get((tick, key), -1), lane=origins[key],
                              own_null=key in nulls)
        if wave == 31:
            cutoff = spawns.get(29)
            if offset is not None and 28 in spawns:
                cutoff = max(cutoff if cutoff is not None else -1, spawns[28]+offset)

            def fresh(c):
                return c['born'] > cutoff if cutoff is not None else c['wave'] > 29

            def viable(c):
                return fresh(c) and c['hp'] > 0 and not c['dying'] and not c['own_null'] \
                    and c['age'] < 49 and c['pending'] < c['hp'] \
                    and not (c['pending'] < 0 and (c['claim'] > tick or c['flight'] >= tick))

            for key, c in previous.items():
                if key not in crabs and c['hp'] > 0 and not c['dying'] \
                        and c['age'] >= (54 if c['big'] else 51) and fresh(c):
                    current['fresh_expiries'].append(c)
                    current['counts']['fresh_expiry_'+c['style']+('_big' if c['big'] else '_small')] += 1
                    if c['wave'] == 29:
                        current['counts']['late29_expiry_'+c['style']] += 1
                    if c['own_null']:
                        current['counts']['fresh_expiry_own_null'] += 1
            target = crabs.get(int(f[8]))
            x, y = map(int, f[6].split(',')[:2])

            def distance(c):
                return max(abs(c['x']-x), abs(c['y']-y))

            if f[4] == 'true' and target is not None and target['lane'] == 'split' \
                    and f[7] == 'SULPHUR' and distance(target) > 1:
                current['counts']['ready_nonadjacent_split_sulphur_attempts'] += 1
                if f[5] == 'true':
                    current['counts']['launched_nonadjacent_split_sulphur'] += 1
                else:
                    observations.append(dict(tick=tick, target=target, position=f[6]))
            if f[5] == 'true' and target is not None and f[7] == 'EYE':
                current['counts']['cleanup_eye_launches'] += 1
                current['counts']['cleanup_eye_birth'+str(target['wave'])] += 1
                if f[9] == 'null' and not target['big'] and viable(target):
                    choices = [c for c in crabs.values() if c['lane'] == 'split'
                               and c['style'] == 'MELEE' and not c['big'] and viable(c)
                               and 1 < distance(c) <= 3 and c['born'] > target['born']]
                    if choices:
                        current['counts']['eye_launch_with_nearby_newer_split_chase_screen'] += 1
                        if len(current['eye_screen_examples']) < 3:
                            current['eye_screen_examples'].append(dict(tick=tick, position=f[6],
                                eye_target=target, newer_melee=choices))
            for c in crabs.values():
                if fresh(c) and c['own_null'] and c['key'] not in current['own_null_keys']:
                    current['own_null_keys'].append(c['key'])
                    if c['wave'] == 29:
                        current['late29_null_keys'].append(c['key'])
        previous = crabs
    finish()
    counts = Counter()
    for trial in trials:
        counts.update(trial['counts'])
    return dict(directory=str(directory), timestamp_wave29=True, wave28_offset=offset,
                counts=counts, rooms=len(trials),
                own_null_fresh_lifetimes=sum(len(t['own_null_keys']) for t in trials),
                own_null_late29_lifetimes=sum(len(t['late29_null_keys']) for t in trials),
                trials=trials,
                caveat='Eye with newer melee is a candidate screen, not exact replacement attribution. '
                       'Current reach alone does not establish future LOS or interaction legality. '
                       'Own NULL is per NPC; natural expiry positiveHP/no deathqueue is observed. '
                       'All summaries use intended inclusive timestamp dim activated at actual31.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('directory', type=Path)
    parser.add_argument('--wave28-offset', type=int, default=16)
    parser.add_argument('--wave29-only', action='store_true')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = audit(args.directory, None if args.wave29_only else args.wave28_offset)
    if args.output:
        args.output.write_text(json.dumps(result, indent=2)+'\n')
    print(json.dumps({k:v for k,v in result.items() if k != 'trials'}, indent=2))
