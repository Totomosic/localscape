#!/usr/bin/env python3
"""Measure native packet staging and physical cleanup tails across fresh cohorts."""
import argparse
from collections import Counter
import csv
import json
from pathlib import Path


def audit(directory):
    rows = list(csv.DictReader((directory / 'trials.csv').open()))
    trials, current, origins, nulls, forecasts = [], None, {}, set(), {}
    for line in (directory / 'server.log').open():
        a = line.strip(' \r\n').split('\t')
        if a[0] == 'NYLO_PLAYERS' and a[1] == '0':
            row = rows[len(trials)]
            current = dict(seed=int(row['seed']), boss_ticks=int(row['ticks']),
                           spawns={}, launches=[], north_attempt=False, north_approach=False,
                           south29_staging=False, south29_at31=None, last_positive=None)
            trials.append(current)
            origins, nulls, forecasts = {}, set(), {}
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
                              lane=origins[key], dying=c[10] == 'true', mage_null=key in nulls,
                              pending_damage=forecasts.get((tick, key), -1))
        if role != 1:
            continue
        current['spawns'].setdefault(wave, tick)
        if '21 north sulphur' in a[9:11]:
            current['north_attempt'] = True
        if '21 north sulphur' in a[10]:
            current['north_attempt'] = True
        if a[10] == 'Approach 21 north sulphur':
            current['north_approach'] = True
        if '29 south stage' in a[10]:
            current['south29_staging'] = True
        if a[5] == 'true' and int(a[8]) in crabs:
            current['launches'].append(dict(tick=tick, wave=wave, rule=a[9], weapon=a[7],
                                            position=a[6], target=crabs[int(a[8])]))
        if wave == 31 and current['south29_at31'] is None:
            current['south29_at31'] = [c for c in crabs.values()
                                      if c['big'] and c['wave'] == 29 and c['lane'] == 'south']
        live = [c for c in crabs.values() if c['hp'] > 0 and not c['dying']]
        if live:
            current['last_positive'] = dict(tick=tick, current_wave=wave, crabs=live,
                                            mage_position=a[6], mage_reason=a[10])
    counts, tails = Counter(), []
    rules = ('12 east scythe', '12 west Dinh', '21 west barrage', '21 north sulphur',
             '22 south Dinh', '26 Dinh on 28')
    for trial in trials:
        for rule in rules:
            if any(a['rule'] == rule for a in trial['launches']):
                counts[rule + '_rooms'] += 1
        counts['north_approach_rooms'] += trial['north_approach']
        counts['north_attempt_rooms'] += trial['north_attempt']
        counts['south29_stage_rooms'] += trial['south29_staging']
        south = [a for a in trial['launches'] if a['target']['big']
                 and a['target']['wave'] == 29 and a['target']['lane'] == 'south']
        if south:
            counts['south29_attacked_rooms'] += 1
            counts['south29_first_actual_wave_' + str(south[0]['wave'])] += 1
        else:
            counts['south29_no_attack_rooms'] += 1
        if any(c['hp'] > 0 and not c['dying'] for c in trial['south29_at31'] or []):
            counts['south29_physically_alive_at31'] += 1
        last = trial['last_positive']
        if last and last['current_wave'] == 31:
            births = {c['wave'] for c in last['crabs']}
            counts['final_positive_only_old29_or_earlier' if max(births) <= 29
                   else 'final_positive_includes_fresh30_or31'] += 1
        if trial['boss_ticks'] > 350:
            tails.append(dict(seed=trial['seed'], boss_ticks=trial['boss_ticks'],
                              south29_attacks=south, last_positive=last))
    return dict(directory=str(directory), trials=len(trials), counts=counts,
                slow_tail_over350_ticks=tails, per_trial=trials,
                caveat='Launch counts are observed native attacks. Positive HP includes old ignored '
                       'expiry paths and can precede later children; neither physical survivor '
                       'presence nor a final body alone proves an actionable lost tick. Fresh '
                       'independent cohorts do not isolate causal speed changes.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('directory', type=Path)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = audit(args.directory)
    if args.output:
        args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(dict(trials=result['trials'], counts=result['counts'],
                          tails=result['slow_tail_over350_ticks']), indent=2))
