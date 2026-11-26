#!/usr/bin/env python3
"""Audit cleanup against the measured dim, using existing native traces only."""
import argparse
from collections import Counter
import csv
import hashlib
import json
import statistics
from pathlib import Path


def distribution(values):
    values = list(values)
    return dict(mean=statistics.mean(values) if values else None,
                min=min(values) if values else None, max=max(values) if values else None)


def is_dimmed(crab, spawn28, offset, spawn29=None, timestamp_wave29=False):
    by29 = crab['born'] <= spawn29 if timestamp_wave29 and spawn29 is not None else crab['wave'] <= 29
    return by29 or (offset is not None and spawn28 is not None and crab['born'] <= spawn28 + offset)


def expiry_status(crabs):
    if not crabs:
        return 'unobserved'
    ignored = [c for c in crabs if c['dimmed']]
    natural = bool(ignored) and all(c['age'] >= (52 if c['big'] else 51) for c in ignored)
    if len(ignored) == len(crabs) and natural:
        return 'made_dim_expiry'
    if ignored and len(ignored) < len(crabs) and natural:
        return 'same_tick_boundary'
    if len(ignored) < len(crabs):
        return 'undimmed_targets_last_alive'
    return 'other_ignored_terminal'


def audit(directory, offset=None, timestamp_wave29=False):
    with (directory / 'trials.csv').open() as source:
        rows = list(csv.DictReader(source))
    trials, current, spawns = [], None, {}
    with (directory / 'server.log').open() as source:
        for line in source:
            fields = line.lstrip(' ').rstrip('\r\n').split('\t')
            if fields[0] == 'NYLO_PLAYERS' and fields[1] == '0':
                row = rows[len(trials)]
                stalls = list(map(int, row['stalls_by_last_spawned_wave'].split(';')))
                current = dict(seed=int(row['seed']), status=row['status'], boss_ticks=int(row['ticks']),
                               stalls19=stalls[19], stalls28=stalls[28], stalls29=stalls[29],
                               stalls30=stalls[30], last_living_tick=None, terminal=[],
                               last_seen={}, dimmed_at31=[], dimmed_primary_attacks=[])
                trials.append(current)
                spawns = {}
                south30 = set()
                assigned_parents = {}
                counted_south_chin = False
            if current is None or fields[0] != 'NYLO_TRACE':
                continue
            tick, role, wave = map(int, fields[1:4])
            if role == 0:
                spawns.setdefault(wave, tick)
                if wave in (23, 24, 25, 26, 29, 30) and tick == spawns[wave]:
                    for raw in (fields[11] if len(fields) > 11 else '').split(';'):
                        c = raw.split(',')
                        if len(c) < 8 or c[1:3] != [str(wave), '0']:
                            continue
                        lane = 'W' if c[6] == '3281' else 'E' if c[6] in ('3309', '3310') else 'S' if c[7] == '4233' else ''
                        if wave == 30 and c[4] == 'false' and lane == 'S' and c[6] in ('3295', '3296'):
                            south30.add(int(c[0]))
                        if c[4] == 'true' and lane:
                            if (wave, lane) == (30, 'W'):
                                assigned_parents[int(c[0])] = 0
                            if (wave, lane) in {(23, 'S'), (24, 'W'), (24, 'E'), (24, 'S'),
                                               (25, 'W'), (26, 'W'), (29, 'S'), (30, 'E')}:
                                assigned_parents[int(c[0])] = 1
            if wave != 31:
                continue
            current.setdefault('spawn31', tick)
            current['spawn28'] = spawns.get(28)
            current['spawn29'] = spawns.get(29)
            crabs = []
            for raw in (fields[11] if len(fields) > 11 else '').split(';'):
                if not raw:
                    continue
                c = raw.split(',')
                crab = dict(key=int(c[0]), wave=int(c[1]), age=int(c[2]), born=tick-int(c[2]),
                            style=c[3], big=c[4] == 'true', hp=int(c[5]), dying=c[10] == 'true')
                crab['dimmed'] = is_dimmed(crab, spawns.get(28), offset, spawns.get(29), timestamp_wave29)
                crab['additional_time_dim'] = crab['dimmed'] and not is_dimmed(
                    crab, spawns.get(28), None, spawns.get(29), timestamp_wave29)
                crabs.append(crab)
                if role == 0:
                    current['last_seen'][crab['key']] = dict(tick=tick, **crab)
            if fields[5] == 'true':
                target = next((c for c in crabs if c['key'] == int(fields[8])), None)
                if target and target['dimmed']:
                    scheduled_chin = (not counted_south_chin and role == 0 and fields[7] == 'CHIN'
                                      and fields[9] == 'South chin flicker 30'
                                      and target['key'] in south30 and target['style'] == 'RANGE'
                                      and target['age'] in (5, 6))
                    if scheduled_chin:
                        counted_south_chin = True
                    assigned_recovery = (assigned_parents.get(target['key']) == role
                        and (role == 1 or target['age'] >= 7 or counted_south_chin))
                    current['dimmed_primary_attacks'].append(dict(tick=tick, role=role,
                        weapon=fields[7], target=target, scheduled_south30_chin=scheduled_chin,
                        assigned_parent_recovery=assigned_recovery))
            if role != 0:
                continue
            living = [c for c in crabs if c['hp'] > 0 and not c['dying']]
            if tick == current['spawn31']:
                current['dimmed_at31'] = [c for c in living if c['dimmed']]
            if living:
                current['last_living_tick'] = tick
                current['terminal'] = living
    if len(trials) != len(rows):
        raise ValueError(f'Trace covers {len(trials)} trials, CSV covers {len(rows)}')
    for trial in trials:
        trial['expiry_status'] = expiry_status(trial['terminal']) if trial['status'] == 'completed' else 'incomplete'
        last = list(trial.pop('last_seen').values())
        trial['undimmed_expiring_lifetimes'] = [c for c in last if not c['dimmed'] and c['hp'] > 0
            and not c['dying'] and c['age'] >= (54 if c['big'] else 51)]
        trial['dimmed_expiring_big_lifetimes'] = [c for c in last if c['dimmed'] and c['big']
            and c['hp'] > 0 and not c['dying'] and c['age'] >= 54]

    def stats(group):
        eligible = [t for t in group if t['status'] == 'completed' and t['stalls19'] == 0]
        dimmed_primaries = [hit for t in group for hit in t['dimmed_primary_attacks']]
        scheduled_chins = sum(hit['scheduled_south30_chin'] for hit in dimmed_primaries)
        parent_recoveries = sum(hit['assigned_parent_recovery'] for hit in dimmed_primaries)
        return dict(trials=len(group), completed=sum(t['status'] == 'completed' for t in group),
                    eligible=len(eligible), wave19_failure_rooms=sum(t['stalls19'] > 0 for t in group),
                    reported_seconds=distribution(t['boss_ticks'] * .6 for t in eligible),
                    within_3m22=sum(t['status'] == 'completed' and t['boss_ticks'] * 3 <= 1010 for t in group),
                    expiry_status=dict(Counter(t['expiry_status'] for t in group)),
                    dimmed_primary_attacks=len(dimmed_primaries), scheduled_dimmed_south30_chins=scheduled_chins,
                    assigned_dimmed_parent_attacks=parent_recoveries,
                    other_dimmed_primary_attacks=len(dimmed_primaries) - scheduled_chins - parent_recoveries,
                    additional_time_dim_rooms=sum(any(c['additional_time_dim'] for c in t['dimmed_at31']) for t in group),
                    undimmed_expiry_rooms=sum(bool(t['undimmed_expiring_lifetimes']) for t in group),
                    dimmed_big_expiry_rooms=sum(bool(t['dimmed_expiring_big_lifetimes']) for t in group),
                    additional_time_dim_big_expiry_rooms=sum(any(c['additional_time_dim']
                        for c in t['dimmed_expiring_big_lifetimes']) for t in group))

    return dict(directory=str(directory), dim=dict(wave=29, wave29_mode='timestamp' if timestamp_wave29 else 'historical_wave_label', additional_wave28_offset=offset,
                activation='actual31'), all_trials=stats(trials),
                eligible_trials=stats([t for t in trials if t['status'] == 'completed' and t['stalls19'] == 0]),
                by_wave28_stalls={str(n): stats([t for t in trials if t['stalls28'] == n])
                                  for n in sorted({t['stalls28'] for t in trials})},
                per_trial=trials,
                caveat='Last positive-HP/non-dying snapshots distinguish natural expiry from combat death. '
                       'A terminal queued-lethal fresh target is still living in that snapshot; no lost tick is inferred. '
                       'Same-tick old/fresh endings are kept separate. AoE secondary hits are not primary dim violations.',
                sources={p.name: hashlib.sha256(p.read_bytes()).hexdigest()
                         for p in (directory / 'trials.csv', directory / 'server.log')})


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('directory', type=Path)
    parser.add_argument('--wave28-offset', type=int)
    parser.add_argument('--timestamp-wave29', action='store_true', help='Use actual wave29 spawn instant; default preserves historical whole-wave audits')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = audit(args.directory, args.wave28_offset, args.timestamp_wave29)
    output = args.output or args.directory / 'cleanup-dim-audit.json'
    output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({k: v for k, v in result.items() if k not in ('per_trial', 'sources')}, indent=2))
