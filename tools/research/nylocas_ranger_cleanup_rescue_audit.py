#!/usr/bin/env python3
"""Screen native cleanup NULL rescue and literal-last cooperation opportunities.

Predecision queued damage is authoritative. Range/travel screens do not establish
target line of sight or a missed native launch. Mage's temporary same-cycle claim
is reconstructed from its actual ready primary; it is absent from final leases.
"""
import argparse
from collections import Counter
import csv
import hashlib
import json
import math
from pathlib import Path

if __package__:
    from .nylocas_cleanup_dim_audit import is_dimmed
else:
    from nylocas_cleanup_dim_audit import is_dimmed


def audit(directory, dim28plus16=False, timestamp_wave29=False, include_wave30=False):
    with (directory/'trials.csv').open() as stream:
        rows = list(csv.DictReader(stream))
    native_delays, trial = {}, -1
    with (directory/'server.log').open() as stream:
        for line in stream:
            a = line.strip().split('\t')
            if a[0] == 'NYLO_TRACE' and a[1:3] == ['0', '0']:
                trial += 1
            elif a[0] == 'NYLO_ENGINE':
                native_delays[trial, int(a[1])] = list(map(int, a[2].split(',')))
    trial, forecasts, nulls, first_null, pending, spawn28, spawn29 = -1, {}, {}, {}, None, None, None
    rescues, final_claims, counts, idle_parents, bad_primaries, doubleups, expired = {}, [], Counter(), {}, [], [], {}
    with (directory/'server.log').open() as stream:
        for line in stream:
            a = line.lstrip().rstrip('\n').split('\t')
            if a[0] == 'NYLO_FORECAST':
                forecasts[int(a[1]), int(a[2])] = (int(a[3]), int(a[4]))
            elif a[0] == 'NYLO_NULL':
                tick, role = map(int, a[1:3])
                keys = set(map(int, a[3].split(','))) if len(a) > 3 and a[3] else set()
                nulls[tick, role] = keys
                for key in keys:
                    first_null.setdefault((role, key), tick)
            elif a[0] == 'NYLO_TRACE':
                tick, role, wave = map(int, a[1:4])
                if tick == 0 and role == 0:
                    trial += 1
                    forecasts, nulls, first_null, pending, spawn28, spawn29 = {}, {}, {}, None, None, None
                if role == 0:
                    if wave == 28 and spawn28 is None:
                        spawn28 = tick
                    if wave == 29 and spawn29 is None:
                        spawn29 = tick
                    pending = a if wave == 31 or include_wave30 and wave == 30 else None
                    continue
                if pending is None:
                    continue
                f, pending = pending, None
                px, py = map(int, f[6].split(',')[:2])
                living = []
                for raw in f[11].split(';'):
                    if not raw:
                        continue
                    c = raw.split(',')
                    if int(c[5]) <= 0 or c[10] == 'true':
                        continue
                    key, birth, age = map(int, c[:3])
                    living.append(dict(key=key, wave=birth, born=tick-age, age=age, style=c[3],
                        big=c[4] == 'true', hp=int(c[5]), x=int(c[6]), y=int(c[7]),
                        claimed_until=int(c[8]), own_hit_until=int(c[9]),
                        dimmed=is_dimmed(dict(wave=birth, born=tick-age), spawn28,
                            16 if dim28plus16 else None, spawn29, timestamp_wave29),
                        ranger_null=key in nulls.get((tick, 0), set()),
                        mage_null=key in nulls.get((tick, 1), set()),
                        forecast=forecasts.get((tick, key), (-1, -1))))
                actual_wave = int(f[3])
                ranger = dict(ready=f[4] == 'true', attack=f[5] == 'true', player=f[6],
                    actual_wave=actual_wave,
                    weapon=f[7], target=int(f[8]), reason=f[10], native_action_delay=native_delays.get((trial, tick), [None, None])[0])
                mage = dict(ready=a[4] == 'true', attack=a[5] == 'true', player=a[6],
                    weapon=a[7], target=int(a[8]), reason=a[10], native_action_delay=native_delays.get((trial, tick), [None, None])[1])
                if ranger['attack']:
                    if ranger['reason'] in ('Idle melee cleanup helper', 'Partner NULL cleanup rescue', 'Last living cleanup target'):
                        counts[ranger['reason']+'_native_launches'] += 1
                        counts[ranger['reason']+'_native_launches_actual'+str(actual_wave)] += 1
                    target = next((c for c in living if c['key'] == ranger['target']), None)
                    if target and (actual_wave == 31 and target['dimmed'] or target['ranger_null'] or target['forecast'][1] >= target['forecast'][0] > 0):
                        bad_primaries.append(dict(seed=rows[trial]['seed'], tick=tick, target=target, ranger=ranger))
                if ranger['attack'] and mage['attack'] and ranger['target'] == mage['target']:
                    counts['same_cycle_doubleup'] += 1
                    doubleups.append(dict(seed=rows[trial]['seed'], tick=tick, key=ranger['target'], living_count=len(living)))
                for c in living:
                    if not c['dimmed'] and c['age'] >= (52 if c['big'] else 51):
                        expired.setdefault((trial, c['key']), dict(seed=rows[trial]['seed'], tick=tick,
                            target=c, ranger=ranger, mage=mage))
                    if c['dimmed'] or c['age'] >= 49 or c['ranger_null']:
                        continue
                    hp, damage = c['forecast']
                    known_kill = damage >= hp > 0
                    if c['mage_null']:
                        record = rescues.setdefault((trial, c['key']), dict(seed=rows[trial]['seed'],
                            key=c['key'], first_partner_null=first_null.get((1, c['key'])),
                            first_actual_wave=actual_wave,
                            first_cleanup_seen=tick, first_selected=None, first_native_attack=None,
                            first_ready_nonlethal_screen=None))
                        if ranger['target'] == c['key']:
                            if record['first_selected'] is None:
                                record['first_selected'] = tick
                            if ranger['attack'] and record['first_native_attack'] is None:
                                record['first_native_attack'] = tick
                        if ranger['ready'] and ranger['native_action_delay'] is not None and ranger['native_action_delay'] <= 0 and not known_kill and record['first_ready_nonlethal_screen'] is None:
                            radius = {'RANGE': 5, 'MAGIC': 8, 'MELEE': 1}[c['style']]
                            size = 2 if c['big'] else 1
                            distance = max(c['x']-px, px-c['x']-size+1, c['y']-py, py-c['y']-size+1)
                            earliest = tick + max(0, math.ceil(max(0, distance-radius)/2)-1)
                            if c['age'] + earliest-tick < 49:
                                record['first_ready_nonlethal_screen'] = dict(tick=tick, target=c,
                                    earliest_request=earliest, living_count=len(living), ranger=ranger, mage=mage)
                    if (c['big'] and not c['mage_null'] and ranger['ready'] and not ranger['attack']
                        and ranger['target'] < 0 and ranger['reason'] == 'Reserve next packet'
                        and ranger['native_action_delay'] is not None and ranger['native_action_delay'] <= 0
                        and mage['target'] != c['key'] and not known_kill):
                        radius = {'RANGE': 5, 'MAGIC': 8, 'MELEE': 1}[c['style']]
                        distance = max(c['x']-px, px-c['x']-1, c['y']-py, py-c['y']-1)
                        earliest = tick + max(0, math.ceil(max(0, distance-radius)/2)-1)
                        if c['age'] + earliest-tick < 49:
                            idle_parents.setdefault((trial, c['key']), dict(seed=rows[trial]['seed'], tick=tick,
                                target=c, earliest_request=earliest, ranger=ranger, mage=mage))
                    if (len(living) == 1 and ranger['ready'] and not ranger['attack']
                        and mage['ready'] and mage['attack'] and mage['target'] == c['key'] and not known_kill):
                        counts['literal_last_ready_ranger_mage_primary'] += 1
                        if len(final_claims) < 30:
                            final_claims.append(dict(seed=rows[trial]['seed'], tick=tick, target=c,
                                ranger=ranger, mage=mage, living_count=len(living)))
    records = list(rescues.values())
    counts['partner_null_fresh_lifetimes'] = len(records)
    counts['partner_null_lifetimes_with_ready_nonlethal_screen'] = sum(r['first_ready_nonlethal_screen'] is not None for r in records)
    counts['partner_null_lifetimes_never_selected'] = sum(r['first_selected'] is None for r in records)
    counts['undimmed_expiring_lifetimes'] = len(expired)
    return dict(run=str(directory), trials=len(rows), dim28plus16=dim28plus16,
        timestamp_wave29=timestamp_wave29, include_wave30=include_wave30,
        counts=dict(counts), partner_null=records,
        literal_last_partner_primary_examples=final_claims, healthy_parent_ready_idle_screens=list(idle_parents.values()),
        unsafe_ranger_primaries=bad_primaries, same_cycle_doubleups=doubleups,
        undimmed_expiring_lifetimes=list(expired.values()),
        caveat=__doc__+' Positive native action delays are excluded from ready opportunity screens; '
               'same-cycle food requests and target LOS still require review. Stage-only close moves '
               'do not expose a target key. Helpers may act on a genuinely sole target while other bodies are dying.',
        sources={str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in (directory/'server.log', directory/'trials.csv')})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('runs', nargs='+', type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--dim28plus16', action='store_true')
    parser.add_argument('--timestamp-wave29', action='store_true',
                        help='Use actual wave-29 spawn time; preserve later wave-29 children.')
    parser.add_argument('--include-wave30', action='store_true',
                        help='Also screen actual30 fresh rescue; dimmed ordinary work is legal until31.')
    args = parser.parse_args()
    results = [audit(directory, args.dim28plus16, args.timestamp_wave29, args.include_wave30) for directory in args.runs]
    args.output.write_text(json.dumps(results, indent=2) + '\n')
    for result in results:
        print(result['run'], result['counts'])


if __name__ == '__main__':
    main()
