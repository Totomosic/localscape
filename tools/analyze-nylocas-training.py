#!/usr/bin/env python3
"""Audit confirmed policy actions and wave milestones in a traced simulation batch."""
import argparse
from collections import Counter
import csv
import json
from pathlib import Path
import statistics


def audit(directory):
    trials = []
    current = None
    null_records = 0
    player_resources = []
    previous_prayer = None
    with (directory / 'server.log').open() as source:
        for line in source:
            line = line.lstrip(' ').rstrip('\n')
            if line.startswith('NYLO_PLAYERS\t'):
                _, tick, hp, energy, prayer = line.split('\t')
                hp, energy, prayer = (list(map(int, pair.split(','))) for pair in (hp, energy, prayer))
                if int(tick) == 0:
                    player_resources.append({'minimum_hp': hp.copy(), 'minimum_energy': energy.copy(), 'prayer_increases': [0, 0]})
                    previous_prayer = prayer
                if not player_resources:
                    raise ValueError('Player telemetry does not start at tick zero')
                record = player_resources[-1]
                for role in (0, 1):
                    record['minimum_hp'][role] = min(record['minimum_hp'][role], hp[role])
                    record['minimum_energy'][role] = min(record['minimum_energy'][role], energy[role])
                    if prayer[role] > previous_prayer[role]:
                        record['prayer_increases'][role] += 1
                previous_prayer = prayer
                continue
            if line.startswith('NYLO_NULL\t'):
                _, tick, role, keys = line.split('\t')
                if current is None:
                    raise ValueError('NULL observation before trial start')
                role, tick = int(role), int(tick)
                keys = set(map(int, keys.split(','))) if keys else set()
                current['nulled_tick'][tick, role] = keys
                current['null_targets_seen'][role].update(keys)
                null_records += 1
                continue
            if not line.startswith('NYLO_TRACE\t'):
                continue
            fields = line.split('\t')
            if len(fields) < 12:
                raise ValueError(f'Incomplete trace: {line}')
            _, tick, role, wave, ready, attacked, position, weapon, target, rule, reason, *rest = fields
            tick, role, wave, target = map(int, (tick, role, wave, target))
            if tick == 0 and role == 0:
                current = {'wave_ticks': {}, 'packets': Counter(), 'primary_births': Counter(), 'ready_idle_reasons': [Counter(), Counter()],
                           'old_cleanup_attacks': [], 'births': {}, 'transition_remaining': None,
                           'nulled_tick': {}, 'null_targets_seen': [set(), set()], 'null_primary_attacks': []}
                trials.append(current)
            if current is None:
                raise ValueError('Trace does not begin at trial tick zero')
            crabs = {}
            snapshot = rest[0] if rest else ''
            for crab in snapshot.split(';'):
                if not crab:
                    continue
                key, birth_wave, age, style, big, hp, x, y, claim, ownhit, dying = crab.split(',')
                key = int(key)
                crabs[key] = {'wave': int(birth_wave), 'age': int(age), 'big': big == 'true',
                              'hp': int(hp), 'dying': dying == 'true', 'x': int(x), 'y': int(y)}
                current['births'].setdefault(key, crabs[key])
            current['wave_ticks'].setdefault(wave, tick)
            if ready == 'true' and attacked == 'false':
                current['ready_idle_reasons'][role][reason] += 1
            if attacked == 'true':
                current['packets'][f'{role}:{rule}'] += 1
                if target in current['nulled_tick'].get((tick, role), set()):
                    current['null_primary_attacks'].append({'tick': tick, 'role': role, 'target': target})
                if target in crabs:
                    birth = current['births'][target]
                    lane = 'west' if birth['x'] == 3281 else 'east' if birth['x'] >= 3309 else 'south' if birth['y'] == 4233 else 'split'
                    identity = f"{role}:{birth['wave']}:{lane}:{'big' if birth['big'] else 'small'}:{weapon}"
                    current['primary_births'][identity] += 1
                if wave == 31 and target in crabs and crabs[target]['wave'] <= 29:
                    current['old_cleanup_attacks'].append({'tick': tick, 'role': role, 'birth_wave': crabs[target]['wave'], 'rule': rule})
            if wave == 21 and current['transition_remaining'] is None:
                remaining = []
                for key, crab in crabs.items():
                    if not crab['big'] or crab['dying'] or crab['hp'] <= 0:
                        continue
                    birth = current['births'][key]
                    lane = 'west' if birth['x'] == 3281 else 'east' if birth['x'] >= 3309 else 'south' if birth['y'] == 4233 else 'unknown'
                    if crab['wave'] in (18, 19) or crab['wave'] == 20 and lane in ('south', 'west'):
                        remaining.append({'key': key, 'birth_wave': crab['wave'], 'lane': lane, 'hp': crab['hp']})
                current['transition_remaining'] = remaining
    with (directory / 'trials.csv').open(newline='') as source:
        rows = list(csv.DictReader(source))
    if len(rows) != len(trials):
        raise ValueError(f'{len(trials)} traced trials, {len(rows)} CSV trials')
    if player_resources and len(player_resources) != len(trials):
        raise ValueError('Player telemetry does not cover every traced trial')
    waves = sorted({w for t in trials for w in t['wave_ticks'] if w > 0})
    totals = Counter()
    births = Counter()
    idle = [Counter(), Counter()]
    for t in trials:
        totals.update(t['packets'])
        births.update(t['primary_births'])
        for role in (0, 1):
            idle[role].update(t['ready_idle_reasons'][role])
    return {'trials': len(trials), 'mean_wave_spawn_ticks': {str(w): statistics.mean(t['wave_ticks'][w] for t in trials if w in t['wave_ticks']) for w in waves},
            'confirmed_rule_attacks': dict(totals), 'confirmed_attacks_by_birth': dict(births),
            'ready_idle_reason_window': 'all traced ticks including entrance and terminal waits; use summary ready_idle metrics for combat window', 'ready_idle_reasons': [dict(x.most_common()) for x in idle],
            'old_cleanup_attacks': sum(len(t['old_cleanup_attacks']) for t in trials),
            'null_observation_records': null_records,
            'player_resources': player_resources or None,
            'observed_null_target_lifetimes': [sum(len(t['null_targets_seen'][role]) for t in trials) for role in (0, 1)],
            'confirmed_attacks_on_known_nulled_targets': sum(len(t['null_primary_attacks']) for t in trials),
            'transition_goal_at_wave21': {'met': sum(t['transition_remaining'] == [] for t in trials),
                                          'observed': sum(t['transition_remaining'] is not None for t in trials)},
            'per_trial': [dict(seed=int(r['seed']), status=r['status'], ticks=int(r['ticks']),
                               wave_spawn_ticks=t['wave_ticks'], transition_remaining=t['transition_remaining'],
                               old_cleanup_attacks=t['old_cleanup_attacks']) for r, t in zip(rows, trials)]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('directory', type=Path)
    args = parser.parse_args()
    result = audit(args.directory)
    output = args.directory / 'training-audit.json'
    output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({key: value for key, value in result.items() if key != 'per_trial'}, indent=2))
    print(f'Audit: {output}')


if __name__ == '__main__':
    main()
