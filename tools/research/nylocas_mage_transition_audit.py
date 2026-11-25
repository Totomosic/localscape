#!/usr/bin/env python3
"""Classify native transition parents, without inferring attackable opportunities.

Physical survivors and already-rolled lethal flights are reported separately.
Claims printed after a cycle are mixed-phase and are not policy replay evidence.
"""
import argparse
from collections import Counter, defaultdict
import csv
import hashlib
import json
from pathlib import Path


def audit(directory):
    rows = list(csv.DictReader((directory / 'trials.csv').open()))
    trials, origins, forecasts, nulls = [], {}, {}, set()
    current = None
    totals, examples = Counter(), defaultdict(list)
    for line in (directory / 'server.log').open():
        a = line.strip(' \r\n').split('\t')
        if a[0] == 'NYLO_PLAYERS' and a[1] == '0':
            current = dict(seed=int(rows[len(trials)]['seed']), attacks=[], parents=None)
            trials.append(current)
            origins, forecasts, nulls = {}, {}, set()
        if current is None:
            continue
        if a[0] == 'NYLO_FORECAST':
            forecasts[int(a[1]), int(a[2])] = int(a[4])
        if a[0] == 'NYLO_NULL' and a[2] == '1' and a[3]:
            nulls.update(map(int, a[3].split(',')))
        if a[0] != 'NYLO_TRACE':
            continue
        tick, role, wave = int(a[1]), int(a[2]), int(a[3])
        crabs = {}
        for raw in a[11].split(';'):
            if not raw:
                continue
            c = raw.split(',')
            key, x, y = int(c[0]), int(c[6]), int(c[7])
            origins.setdefault(key, 'west' if x == 3281 else 'east' if x >= 3309
                               else 'south' if y == 4233 else 'split')
            crabs[key] = dict(key=key, wave=int(c[1]), age=int(c[2]), style=c[3],
                              big=c[4] == 'true', hp=int(c[5]), x=x, y=y,
                              lane=origins[key], dying=c[10] == 'true',
                              pending=forecasts.get((tick, key), -1), mage_null=key in nulls)
        if role != 1:
            continue
        if wave == 20 and a[5] == 'true' and int(a[8]) in crabs:
            c = crabs[int(a[8])]
            if c['big'] and c['wave'] == 20:
                name = f"20_{c['lane']}_{a[7]}_launches"
                totals[name] += 1
                current['attacks'].append(dict(tick=tick, weapon=a[7], target=c))
        if wave != 21 or current['parents'] is not None:
            continue
        parents = [c for c in crabs.values() if c['big'] and
                   (c['wave'] in (18, 19) or c['wave'] == 20 and c['lane'] in ('south', 'west'))]
        current['parents'] = parents
        live = [c for c in parents if c['hp'] > 0 and not c['dying']]
        totals['transition_observed'] += 1
        if not live:
            totals['transition_physical_clear'] += 1
        if not any(c['pending'] < c['hp'] for c in live):
            totals['transition_clear_or_known_lethal'] += 1
        for c in live:
            status = 'known_lethal' if c['pending'] >= c['hp'] else 'mage_null' if c['mage_null'] else 'uncovered'
            tag = f"{c['wave']}_{c['lane']}_{status}"
            totals[tag] += 1
            if len(examples[tag]) < 8:
                examples[tag].append(dict(seed=current['seed'], tick=tick, parent=c,
                                          mage_20_attacks=current['attacks']))
    for trial in trials:
        for lane in ('south', 'west'):
            attacks = [a for a in trial['attacks'] if a['target']['lane'] == lane]
            totals[f'20_{lane}_rooms_with_mage_attack'] += bool(attacks)
            totals[f'20_{lane}_rooms_with_scythe'] += any(a['weapon'] == 'SCYTHE' for a in attacks)
    return dict(directory=str(directory), trials=len(trials), counts=totals, examples=examples,
                source_sha256={p.name: hashlib.sha256(p.read_bytes()).hexdigest()
                               for p in (directory / 'trials.csv', directory / 'server.log')},
                caveat='First mage snapshot of actual21; physical positive-HP non-dying parents '
                       'are distinguished from already-rolled lethal forecasts. Presence is '
                       'not proof of an earlier attackable opportunity. No role kill credit inferred.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('directory', type=Path)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = audit(args.directory)
    if args.output:
        args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(dict(trials=result['trials'], counts=result['counts']), indent=2))
