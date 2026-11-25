#!/usr/bin/env python3
"""Audit the explicit west11 split-after13 goal against native phase observations.

CSV stalls are authoritative. Trace populations are snapshots near an admission
check; holding a parent changes other combat, so occupancy is not a counterfactual.
"""
import argparse
from collections import Counter
import csv
import json
from pathlib import Path


def audit(directory):
    rows = list(csv.DictReader((directory / 'trials.csv').open()))
    trials, origins, previous = [], {}, {}
    current = None
    for line in (directory / 'server.log').open():
        a = line.strip(' \r\n').split('\t')
        if a[0] == 'NYLO_PLAYERS' and a[1] == '0':
            current = dict(seed=int(rows[len(trials)]['seed']), spawn12=None,
                           spawn13=None, first_attack=None, split=None, cap_observations=[])
            trials.append(current)
            origins, previous = {}, {}
        if current is None or a[0] != 'NYLO_TRACE' or a[2] != '1':
            continue
        tick, wave = int(a[1]), int(a[3])
        crabs = {}
        for raw in a[11].split(';'):
            if not raw:
                continue
            c = raw.split(',')
            key, x, y = int(c[0]), int(c[6]), int(c[7])
            origins.setdefault(key, 'west' if x == 3281 else 'east' if x >= 3309
                               else 'south' if y == 4233 else 'split')
            crabs[key] = dict(key=key, wave=int(c[1]), age=int(c[2]), big=c[4] == 'true',
                              hp=int(c[5]), x=x, y=y, lane=origins[key], dying=c[10] == 'true')
        for n in (12, 13):
            if wave == n and current[f'spawn{n}'] is None:
                current[f'spawn{n}'] = tick
        def parent(c):
            return c['big'] and c['wave'] == 11 and c['lane'] == 'west'
        if a[5] == 'true' and int(a[8]) in crabs and parent(crabs[int(a[8])]):
            if current['first_attack'] is None:
                current['first_attack'] = dict(tick=tick, weapon=a[7], position=a[6],
                                               target=crabs[int(a[8])])
        gone = [c for key, c in previous.items() if key not in crabs and parent(c)]
        if gone and current['split'] is None:
            p = gone[0]
            newborn = [c for key, c in crabs.items() if key not in previous
                       and not c['big'] and c['age'] == 0]
            sw = [c for c in newborn if (c['x'], c['y']) == (p['x'], p['y'])]
            ne = [c for c in newborn if (c['x'], c['y']) == (p['x'] + 1, p['y'] + 1)]
            current['split'] = dict(tick=tick, current_wave=wave, parent=p,
                                    children=sw + ne if len(sw) == len(ne) == 1 else [])
        if wave == 12 and current['spawn12'] is not None:
            offset = tick - current['spawn12']
            if offset >= 8 and (offset - 8) % 4 == 0:
                current['cap_observations'].append(dict(tick=tick, population=len(crabs), keys=sorted(crabs)))
        previous = crabs
    counts, witnesses = Counter(), []
    for i, trial in enumerate(trials):
        trial['last12_stalls'] = list(map(int, rows[i]['stalls_by_last_spawned_wave'].split(';')))[12]
        first, split = trial['first_attack'], trial['split']
        if first:
            counts[f"first_mage_request_age_{first['target']['age']}"] += 1
        if not split:
            counts['missing_parent_removal'] += 1
            continue
        counts[f"split_current_wave_{split['current_wave']}"] += 1
        if len(split['children']) != 2:
            counts['unattributed_child_pair'] += 1
            continue
        counts['exact_child_pair'] += 1
        if trial['spawn13'] is not None:
            delta = split['tick'] - trial['spawn13']
            counts['split_before13' if delta < 0 else 'split_same13_tick' if delta == 0 else 'split_after13'] += 1
        child_keys = {c['key'] for c in split['children']}
        for frame in trial['cap_observations']:
            if frame['population'] == 12 and child_keys.issubset(frame['keys']):
                counts['threshold12_observation_with_both_children'] += 1
                if len(witnesses) < 8:
                    witnesses.append(dict(seed=trial['seed'], spawn12=trial['spawn12'],
                                          spawn13=trial['spawn13'], first_attack=first,
                                          split=split, frame=frame, last12_stalls=trial['last12_stalls']))
    return dict(directory=str(directory), trials=len(trials), counts=counts,
                witnesses=witnesses, per_trial=trials,
                caveat='Exact SW/NE newborn attribution on parent removal. CSV stalls authoritative; '
                       'trace occupancy is pre-decision and engine admission can run between frames. '
                       'Both children at threshold12 identify a hypothesis, not a causal speed result.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('directory', type=Path)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = audit(args.directory)
    if args.output:
        args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(dict(trials=result['trials'], counts=result['counts']), indent=2))
