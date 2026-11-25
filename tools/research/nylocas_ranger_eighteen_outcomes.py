#!/usr/bin/env python3
"""Compare observed wave-18 lifetimes and required West-19 inputs from native logs."""
import argparse
from collections import Counter
import csv
import hashlib
import json
from pathlib import Path
import statistics


def distribution(values):
    return dict(n=len(values), mean=statistics.mean(values), min=min(values), max=max(values)) if values else None


def audit(directory):
    with (directory / 'trials.csv').open() as stream:
        rows = list(csv.DictReader(stream))
    rooms, room = [], None
    with (directory / 'server.log').open() as stream:
        for line in stream:
            a = line.lstrip(' ').rstrip('\r\n').split('\t')
            if a[0] != 'NYLO_TRACE' or a[2] != '0':
                continue
            tick, wave = int(a[1]), int(a[3])
            if tick == 0:
                row = rows[len(rooms)]
                room = dict(seed=row['seed'], wave19_stalls=int(row['stalls_by_last_spawned_wave'].split(';')[19]),
                            spawns={}, parents={}, west19=[], eye5=False)
                rooms.append(room)
            first_wave = wave not in room['spawns']
            room['spawns'].setdefault(wave, tick)
            seen = set()
            for raw in a[11].split(';'):
                if not raw:
                    continue
                c = raw.split(',')
                key, birth, age = map(int, c[:3])
                if birth != 18 or c[4] != 'true':
                    continue
                seen.add(key)
                x, y = map(int, c[6:8])
                if key not in room['parents']:
                    lane = 'west' if x == 3281 else 'east' if 3309 <= x <= 3310 else 'south' if y == 4233 else 'unknown'
                    room['parents'][key] = dict(lane=lane, born=tick-age, zero_hp_age=None,
                                               removed_age=None, alive_at19=None, alive_at20=None, shots=[])
                p = room['parents'][key]
                alive = int(c[5]) > 0 and c[10] != 'true'
                if not alive and p['zero_hp_age'] is None:
                    p['zero_hp_age'] = age
                if first_wave and wave in (19, 20):
                    p['alive_at'+str(wave)] = alive
                if a[5] == 'true' and int(a[8]) == key:
                    p['shots'].append(dict(tick=tick, age=age, weapon=a[7], hp=int(c[5])))
                    if p['lane'] == 'west' and age == 5 and a[7] == 'EYE':
                        room['eye5'] = True
            for key, p in room['parents'].items():
                if key not in seen and p['removed_age'] is None:
                    p['removed_age'] = tick-p['born']
            if a[5] == 'true' and a[9] == 'West magic big 19':
                room['west19'].append(dict(tick=tick, age=tick-room['spawns'][19], weapon=a[7]))
    result = {}
    for lane in ('west', 'south', 'east'):
        parents = [p for r in rooms for p in r['parents'].values() if p['lane'] == lane]
        result[lane] = dict(zero_hp_age=distribution([p['zero_hp_age'] for p in parents if p['zero_hp_age'] is not None]),
                            removed_age=distribution([p['removed_age'] for p in parents if p['removed_age'] is not None]),
                            alive_at19=sum(p['alive_at19'] is True for p in parents),
                            alive_at20=sum(p['alive_at20'] is True for p in parents),
                            no_combat_death=sum(p['zero_hp_age'] is None for p in parents))
    return dict(run=str(directory), trials=len(rows), failed19_rooms=sum(r['wave19_stalls'] > 0 for r in rooms),
                eye5_rooms=sum(r['eye5'] for r in rooms),
                west19_packet_rooms=sum(bool(r['west19']) for r in rooms),
                west19_first_ages=dict(Counter(r['west19'][0]['age'] for r in rooms if r['west19'])),
                parents=result, per_room=rooms,
                caveat='HP/death state and first absence are observed, not inferred damage rolls or exact child birth causes. '
                       'Parent removal can include a death animation or natural expiry. Fresh independent cohorts are not matched seeds.',
                sources={str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in (directory/'server.log', directory/'trials.csv')})


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('runs', nargs='+', type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    results = [audit(run) for run in args.runs]
    args.output.write_text(json.dumps(results, indent=2)+'\n')
    for result in results:
        print(json.dumps({k: v for k, v in result.items() if k not in ('per_room', 'sources', 'caveat')}, indent=2))
