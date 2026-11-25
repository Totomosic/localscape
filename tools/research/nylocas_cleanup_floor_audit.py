#!/usr/bin/env python3
"""Measure observed dimmed-small expiry floors and exact old-parent lineages.

The floor is conditional on the observed natural expiry and dim policy; earlier
pre31 service or incidental AoE can change the trajectory. It is not a universal
encounter bound. Boss alignment follows NylocasEncounter.checkCleanup.
"""
import argparse
from collections import Counter
import csv
import hashlib
import json
from pathlib import Path
import statistics


def distribution(values):
    values = list(values)
    return dict(n=len(values), mean=statistics.mean(values) if values else None,
                min=min(values) if values else None, max=max(values) if values else None,
                median=statistics.median(values) if values else None)


def audit(directory, offset=None, timestamp_dim=False):
    rows = list(csv.DictReader((directory / 'trials.csv').open()))
    trials, current, previous, origins, lineage = [], None, {}, {}, {}
    for line in (directory / 'server.log').open():
        f = line.strip(' \r\n').split('\t')
        if f[0] == 'NYLO_PLAYERS' and f[1] == '0':
            row = rows[len(trials)]
            current = dict(seed=int(row['seed']), boss_ticks=int(row['ticks']),
                           stalls19=int(row['stalls_by_last_spawned_wave'].split(';')[19]),
                           spawns={}, natural_dimmed_smalls=[], old_parent_removals=[],
                           unattributed_birth_pairs=0)
            trials.append(current)
            previous, origins, lineage = {}, {}, {}
        if current is None or f[0] != 'NYLO_TRACE' or f[2] != '1':
            continue
        tick, wave = int(f[1]), int(f[3])
        current['spawns'].setdefault(wave, tick)
        crabs = {}
        for raw in f[11].split(';'):
            if not raw:
                continue
            c = raw.split(',')
            key, x, y = int(c[0]), int(c[6]), int(c[7])
            origins.setdefault(key, 'west' if x == 3281 else 'east' if x >= 3309
                               else 'south' if y == 4233 else 'split')
            crabs[key] = dict(key=key, wave=int(c[1]), age=int(c[2]), born=tick-int(c[2]),
                              style=c[3], big=c[4] == 'true', hp=int(c[5]), x=x, y=y,
                              dying=c[10] == 'true', lane=origins[key])
        newborn = [c for key, c in crabs.items() if key not in previous
                   and not c['big'] and c['age'] == 0]
        for key, parent in previous.items():
            if key in crabs or not parent['big']:
                continue
            sw = [c for c in newborn if (c['x'], c['y']) == (parent['x'], parent['y'])]
            ne = [c for c in newborn if (c['x'], c['y']) == (parent['x']+1, parent['y']+1)]
            if parent['wave'] in (25, 26, 27) or parent['wave'] == 23 and parent['lane'] == 'east':
                current['old_parent_removals'].append(dict(parent=parent, tick=tick,
                    current_wave=wave, path='natural_expiry' if parent['hp'] > 0 and not parent['dying']
                    else 'combat_death', exact_children=sw+ne if len(sw) == len(ne) == 1 else []))
            if len(sw) == len(ne) == 1:
                for child in sw + ne:
                    lineage[child['key']] = dict(parent=parent, removal_tick=tick,
                        path='natural_expiry' if parent['hp'] > 0 and not parent['dying']
                        else 'combat_death', pair=[sw[0]['key'], ne[0]['key']])
            elif newborn:
                current['unattributed_birth_pairs'] += 1
        if wave == 31:
            for key, c in previous.items():
                if key in crabs or c['big'] or c['hp'] <= 0 or c['dying'] or c['age'] < 51:
                    continue
                time_dim = offset is not None and current['spawns'].get(28) is not None \
                    and c['born'] <= current['spawns'][28] + offset
                spawn29 = current['spawns'].get(29)
                baseline_dim = c['born'] <= spawn29 if spawn29 is not None else c['wave'] <= 29
                ignored = (baseline_dim or time_dim) if timestamp_dim else c['wave'] <= 29 or time_dim
                if not ignored:
                    continue
                extra_time_dim = time_dim and not baseline_dim if timestamp_dim else c['wave'] > 29
                current['natural_dimmed_smalls'].append(dict(target=c,
                    observed_removal_tick=tick, removal_age=tick-c['born'],
                    raw_floor_tick=c['born']+52+16,
                    extra_time_dim=extra_time_dim, lineage=lineage.get(key)))
        previous = crabs
    counts = Counter()
    for trial in trials:
        for removal in trial['old_parent_removals']:
            p = removal['parent']
            if p['wave'] == 23 and p['lane'] == 'east':
                counts[f"parent23east_{removal['path']}_during{removal['current_wave']}"] += 1
        smalls = trial['natural_dimmed_smalls']
        if not smalls:
            trial.update(floor_tick=None, boss_minus_floor=None, latest_floor_targets=[])
            counts['rooms_without_observed_dimmed_small_expiry'] += 1
            continue
        raw_floor = max(c['raw_floor_tick'] for c in smalls)
        phase = trial['spawns'].get(1, 0) % 4
        floor = raw_floor + (phase - raw_floor) % 4
        latest = [c for c in smalls if c['raw_floor_tick'] == raw_floor]
        trial.update(raw_floor_tick=raw_floor, floor_tick=floor,
                     boss_minus_floor=trial['boss_ticks']-floor, latest_floor_targets=latest)
        counts['rooms_with_observed_floor'] += 1
        if trial['boss_ticks'] < floor:
            counts['negative_boss_minus_floor_review'] += 1
        if floor > 336:
            counts['floor_alone_above_3m22'] += 1
            counts['floor_above_3m22_wave19_failure' if trial['stalls19'] else
                   'floor_above_3m22_zero_wave19'] += 1
        if trial['boss_ticks'] == floor:
            counts['boss_at_observed_small_floor'] += 1
        if any(c['extra_time_dim'] for c in latest):
            counts['latest_floor_includes_new_time_dim'] += 1
        for item in latest:
            parent = item['lineage']
            tag = 'lane_small_or_unattributed' if parent is None else \
                f"parent{parent['parent']['wave']}_{parent['parent']['lane']}_{parent['path']}"
            counts['latest_floor_lifetime_' + tag] += 1
        for item in smalls:
            counts['natural_dimmed_small_removal_age_' + str(item['removal_age'])] += 1

    def stats(group):
        return dict(rooms=len(group), boss_ticks=distribution(t['boss_ticks'] for t in group),
                    floor_ticks=distribution(t['floor_tick'] for t in group if t['floor_tick'] is not None),
                    boss_minus_floor=distribution(t['boss_minus_floor'] for t in group
                                                 if t['boss_minus_floor'] is not None),
                    floor_above336=sum(t['floor_tick'] is not None and t['floor_tick'] > 336 for t in group))
    source_hashes = {}
    for p in (directory / 'trials.csv', directory / 'server.log'):
        digest = hashlib.sha256()
        with p.open('rb') as stream:
            for block in iter(lambda: stream.read(1024*1024), b''):
                digest.update(block)
        source_hashes[p.name] = digest.hexdigest()
    return dict(directory=str(directory), dim_wave28_offset=offset,
                dim_semantics='wave29_spawn_timestamp' if timestamp_dim else 'historical_whole_wave29_label',
                counts=counts,
                all_trials=stats(trials), zero_wave19=stats([t for t in trials if not t['stalls19']]),
                per_trial=trials, sources=source_hashes,
                caveat='Observed positive-HP, non-dying small removal at its natural lifetime. '
                       'Floor is birth+52 removal+16 boss delay, aligned to the encounter phase. '
                       'Exact SW/NE newborn pair attributes lineage; ambiguous/missing pairs are not guessed. '
                       'Earlier service/incidental AoE can change this observed-run floor; dimmed bigs '
                       'and later fresh cleanup can add separate constraints.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('directory', type=Path)
    parser.add_argument('--wave28-offset', type=int)
    parser.add_argument('--wave29-timestamp', action='store_true',
                        help='Use intended born<=spawn29 timestamp dim; default labels historical whole-wave runs.')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = audit(args.directory, args.wave28_offset, args.wave29_timestamp)
    output = args.output or args.directory / 'cleanup-floor-audit.json'
    output.write_text(json.dumps(result, indent=2)+'\n')
    print(json.dumps({k:v for k,v in result.items() if k not in ('per_trial','sources')}, indent=2))
