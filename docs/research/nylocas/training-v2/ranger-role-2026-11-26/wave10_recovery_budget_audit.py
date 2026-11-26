"""Measure native time between required10 chins and the remaining West pair.

Actual attacks define cadence; this does not assume an available attack during cooldown.
"""
import collections
import csv
import json
from pathlib import Path

root = Path(__file__).parent
for name in ['newer-cleanup-200', 'newer-cleanup-300', 'named-single-chins-200']:
    run = root.parent / ('training-v5-20261126-' + name)
    rows = list(csv.DictReader((run / 'trials.csv').open()))
    rooms = []
    room = None
    births = {}
    forecast = {}
    for line in (run / 'server.log').open():
        a = line.lstrip().rstrip('\r\n').split('\t')
        if a[0] == 'NYLO_FORECAST':
            forecast[int(a[1]), int(a[2])] = list(map(int, a[3:5]))
        if a[0] != 'NYLO_TRACE' or a[2] != '0':
            continue
        tick, wave = int(a[1]), int(a[3])
        if tick == 0:
            room = dict(seed=int(rows[len(rooms)]['seed']), spawn10=None, spawn11=None, casts={}, frames=[])
            rooms.append(room)
            births = {}
            forecast = {}
        if wave == 10 and room['spawn10'] is None:
            room['spawn10'] = tick
        if wave == 11 and room['spawn11'] is None:
            room['spawn11'] = tick
        crabs = []
        for raw in a[11].split(';'):
            if not raw:
                continue
            c = raw.split(',')
            key, birth, age = map(int, c[:3])
            x, y = map(int, c[6:8])
            births.setdefault(key, 'west' if x == 3281 else 'east' if x in [3309,3310] else 'south' if y == 4233 else 'split')
            if birth == 10 and births[key] == 'west':
                crabs.append(dict(key=key, age=age, hp=int(c[5]), dying=c[10] == 'true', forecast=forecast.get((tick,key)), x=x, y=y))
        if a[5] == 'true' and a[9] in ['West chin 10','East chin 10','South chin 10']:
            room['casts'].setdefault(a[9], tick)
        if room['spawn10'] is not None and tick <= room['spawn10'] + 14:
            room['frames'].append(dict(tick=tick, wave=wave, ready=a[4]=='true', attack=a[5]=='true', target=int(a[8]), weapon=a[7], rule=a[9], reason=a[10], west=crabs))
    cadence = collections.Counter()
    remaining = collections.Counter()
    for room in rooms:
        c = room['casts']
        if len(c) != 3:
            cadence['missing'] += 1
            continue
        cadence[str((c['East chin 10']-c['West chin 10'],c['South chin 10']-c['East chin 10']))] += 1
        f = next((f for f in room['frames'] if f['tick'] == room['spawn10'] + 6), None)
        if f:
            alive = sum(c['hp'] > 0 and not c['dying'] for c in f['west'])
            bodies = len(f['west'])
            remaining[str((alive,bodies))] += 1
    summary = dict(run=name, cadence_gaps=dict(cadence), west_alive_and_bodies_at10age6=dict(remaining), rooms=rooms)
    (root/(name+'-wave10-recovery-budget.json')).write_text(json.dumps(summary,indent=2)+'\n')
    print(name, 'cadence', dict(cadence), 'West at6', dict(remaining))
