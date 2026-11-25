#!/usr/bin/env python3
"""Audit Jordan's mage guidance in native traces, retaining concrete witnesses.

Reports observed behavior, not an assertion that every surviving target was
attackable. Claims/own flights printed after each cycle have mixed phase; the
previous mage snapshot is used when classifying settled follow-up opportunities.
"""
import argparse
from collections import Counter, defaultdict
import csv
import hashlib
import json
from pathlib import Path


def audit(directory):
    rows = list(csv.DictReader((directory / 'trials.csv').open()))
    trials, current, origins, slots, previous, nulls, forecasts = [], None, {}, {}, {}, {}, {}
    totals, examples = Counter(), defaultdict(list)

    def record(tag, tick, target, **extra):
        totals[tag] += 1
        current['counts'][tag] += 1
        if len(examples[tag]) < 8:
            examples[tag].append(dict(seed=current['seed'], tick=tick, target=target, **extra))

    for line in (directory / 'server.log').open():
        a = line.strip(' \r\n').split('\t')
        if a[0] == 'NYLO_FORECAST':
            forecasts[int(a[1]), int(a[2])] = int(a[4])
            continue
        if a[0] == 'NYLO_NULL':
            nulls[int(a[1]), int(a[2])] = set(map(int, a[3].split(','))) if a[3] else set()
            if current is not None and int(a[2]) == 1:
                current['nulled'].update(nulls[int(a[1]), int(a[2])])
            continue
        if a[0] != 'NYLO_TRACE':
            continue
        tick, role, wave = int(a[1]), int(a[2]), int(a[3])
        if tick == 0 and role == 0:
            current = dict(seed=int(rows[len(trials)]['seed']), counts=Counter(), first={},
                           opened=set(), nulled=set(), final_big_state={})
            trials.append(current)
            origins, slots, previous, nulls, forecasts = {}, {}, {}, {}, {}
        crabs = {}
        for raw in a[11].split(';'):
            if not raw:
                continue
            c = raw.split(',')
            key = int(c[0])
            item = dict(key=key, wave=int(c[1]), age=int(c[2]), style=c[3], big=c[4] == 'true',
                        hp=int(c[5]), x=int(c[6]), y=int(c[7]), claim=int(c[8]),
                        flight=int(c[9]), dying=c[10] == 'true')
            origins.setdefault(key, 'west' if item['x'] == 3281 else 'east' if item['x'] >= 3309
                               else 'south' if item['y'] == 4233 else 'split')
            slots.setdefault(key, ('W' if item['x'] == 3295 else 'E') if origins[key] == 'south'
                             else ('N' if item['y'] == 4249 else 'S'))
            item['lane'] = origins[key]
            item['slot'] = slots[key]
            item['pendingDamage'] = forecasts.get((tick, key), -1)
            crabs[key] = item
        if role != 1:
            continue
        for c in crabs.values():
            if c['big'] and (c['wave'], c['lane']) in ((23, 'south'), (24, 'south'), (24, 'east'), (24, 'west'),
                                                      (25, 'west'), (26, 'west')):
                current['final_big_state'][c['key']] = c
        if a[5] == 'true' and int(a[8]) in crabs:
            c = crabs[int(a[8])]
            current['opened'].add(c['key'])
            name = f"{c['wave']}-{c['lane']}"
            if c['wave'] != 11 or c['lane'] != 'south' or c['slot'] == 'W':
                current['first'].setdefault(name, tick)
            if c['wave'] == 3 and not c['big'] and c['lane'] != 'split' and c['style'] == 'MELEE':
                record('wave3_lane_melee_launches', tick, c)
            if c['big'] and c['wave'] == 27 and c['lane'] == 'west':
                record('west27_launches', tick, c)
                old = [x for x in crabs.values() if x['big'] and x['wave'] == 26 and x['lane'] == 'west'
                       and not x['dying'] and x['hp'] > 0]
                if old:
                    record('west27_while_west26_living', tick, c, west26=old)
                    if all(x['pendingDamage'] >= x['hp'] for x in old):
                        record('west27_while_west26_known_lethal', tick, c, west26=old)
                    else:
                        record('west27_while_west26_without_known_kill', tick, c, west26=old)
                if c['age'] > 9:
                    record('west27_launches_after_entry_age9', tick, c)
            if c['big'] and c['wave'] in (4, 8, 19, 23, 24, 25, 26):
                record(f"big_{c['wave']}_{c['lane']}_launches", tick, c)
            for birth, lane in ((4, 'south'), (8, 'west')):
                survivors = [x for key, x in crabs.items() if x['wave'] == birth and x['lane'] == lane
                             and x['big'] and key != c['key'] and key in current['opened'] and x['hp'] > 0
                             and not x['dying'] and x['age'] < 49 and wave - birth <= 4
                             and key not in nulls.get((tick, role), set())
                             and previous.get(key, {}).get('flight', 0) < tick
                             and previous.get(key, {}).get('claim', 0) <= tick]
                if survivors:
                    record(f'other_launch_while_settled_opened_{birth}_alive', tick, c,
                           survivors=survivors, reason=a[10])
        previous = crabs
    for t in trials:
        for group, earlier, later in ((11, 'south', 'west'), (19, 'east', 'south')):
            first, second = t['first'].get(f'{group}-{earlier}'), t['first'].get(f'{group}-{later}')
            tag = f'{group}_order_' + ('correct' if first is not None and second is not None and first < second
                                     else 'reversed' if first is not None and second is not None else 'missing')
            totals[tag] += 1
        for c in t['final_big_state'].values():
            tag = f"required_{c['wave']}_{c['lane']}_last_observed_" + ('dead' if c['dying'] or c['hp'] <= 0
                                                                 else 'alive')
            totals[tag] += 1
            if not c['dying'] and c['hp'] > 0:
                kind = 'mage_null' if c['key'] in t['nulled'] else 'mage_available'
                totals[tag + '_' + kind] += 1
                if len(examples[tag + '_' + kind]) < 8:
                    examples[tag + '_' + kind].append(dict(seed=t['seed'], target=c,
                                                         mage_opened=c['key'] in t['opened']))
    return dict(run=str(directory), trials=len(trials), counts=dict(totals), examples=dict(examples),
                caveat='Living26 is occupancy, not immediate eligibility; already-rolled lethal hits can cover its HP. '
                       'Unknown pending damage is -1. After-entry27 includes valid followups. '
                       'Native natural expiry preserves positive HP and no death queue; a dead endpoint observes the combat death path, '
                       'without attributing its kill to a player or weapon. '
                       'Other-launch counters are review candidates, including mandatory packets, not proof of a lost tick.',
                source_sha256={str(directory / 'server.log'): hashlib.sha256((directory / 'server.log').read_bytes()).hexdigest()})


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('directory', type=Path)
    p.add_argument('--output', type=Path)
    args = p.parse_args()
    result = audit(args.directory)
    target = args.output or args.directory / 'mage-guidance-audit.json'
    target.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({k: v for k, v in result.items() if k not in ('examples', 'source_sha256')}, indent=2))
    print(target)


if __name__ == '__main__':
    main()
