#!/usr/bin/env python3
"""Audit predecision queued damage, ranger NULL observations and protected mage targets.

Raw overlapping flights are not treated as waste: nonlethal followups can be useful.
The native forecast is emitted before decisions; ordinary trace leases are postcycle.
"""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path

from nylocas_guidance_audit import read_run


def audit(directory):
    rows, frames, shots = read_run(directory)
    frame_map = {(f['trial'], f['tick'], f['role']): f for f in frames}
    forecasts, rolls = {}, []
    trial = -1
    for line in (directory / 'server.log').open():
        a = line.strip().split('\t')
        if a[0] == 'NYLO_TRACE' and a[1:3] == ['0', '0']:
            trial += 1
        elif a[0] == 'NYLO_FORECAST':
            forecasts[trial, int(a[1]), int(a[2])] = (int(a[3]), int(a[4]))
        elif a[0] == 'NYLO_ROLL' and a[2] == '0':
            rolls.append((trial, int(a[1]), int(a[3]), int(a[4]), int(a[5])))
    counts, first_null, examples = Counter(), {}, {}
    for f in frames:
        if f['role'] != 0:
            continue
        for c in f['crabs'].values():
            if c['nulled']:
                first_null.setdefault((f['trial'], c['key']), dict(seed=f['seed'], tick=f['tick'], target=c))
            if c['wave'] != 24 or c['lane'] != 'west' or not c['big'] or c['age'] != 5:
                continue
            forecast = forecasts.get((f['trial'], f['tick'], c['key']))
            if forecast is None:
                continue
            hp, damage = forecast
            eye = f['attack'] and f['target_key'] == c['key'] and f['weapon'] == 'EYE'
            if damage >= hp > 0:
                tag = 'queued_lethal'
                counts[tag] += 1
                counts[tag + ('_eye' if eye else '_no_eye')] += 1
            elif hp > 0 and damage >= 0:
                tag = 'queued_nonlethal'
                counts[tag] += 1
                action = '_eye' if eye else '_other_native_attack' if f['attack'] else '_no_native_attack'
                counts[tag + action] += 1
                if not eye:
                    examples.setdefault('nonlethal_without_eye', [])
                    if len(examples['nonlethal_without_eye']) < 8:
                        examples['nonlethal_without_eye'].append(dict(seed=f['seed'],tick=f['tick'],
                            target=c,forecast=forecast,weapon=f['weapon'],selected=f['target_key'],reason=f['reason']))
    theft = []
    styles = {'BOW': 'RANGE', 'PIPE': 'RANGE', 'CHIN': 'RANGE', 'EYE': 'MAGIC',
              'CLAWS': 'MELEE', 'SULPHUR': 'MELEE'}
    for s in shots:
        if s['role'] != 0:
            continue
        c = s['target']
        if c['nulled']:
            counts['primary_target_nulled_at_observation'] += 1
        if c['style'] != styles[s['weapon']]:
            counts['primary_style_differs_from_predecision_target'] += 1
        saved = (c['wave'] == 22 and c['lane'] == 'south' and c['slot'] == 'W' and c['age'] <= 10
                 or c['wave'] == 11 and c['lane'] == 'south' and c['slot'] == 'W' and c['age'] <= 8
                 or c['wave'] == 12 and c['lane'] == 'west' and c['slot'] == 'S' and c['age'] <= 15)
        mage = frame_map.get((s['trial'],s['tick'],1),{}).get('crabs',{}).get(c['key'])
        if saved and mage and not mage['nulled']:
            theft.append(dict(seed=s['seed'],tick=s['tick'],weapon=s['weapon'],target=c))
    splash = []
    for trial,tick,key,weapon,damage in rolls:
        if weapon != 11959:
            continue
        f = frame_map.get((trial,tick,0))
        c = f['crabs'].get(key) if f else None
        mage = frame_map.get((trial,tick,1),{}).get('crabs',{}).get(key)
        if c and c['wave']==22 and c['lane']=='south' and c['slot']=='W' and c['age']<=10 and mage and not mage['nulled']:
            splash.append(dict(seed=f['seed'],tick=tick,target=c,rolled_damage=damage))
    null_wave = Counter(f"{x['target']['wave']}:{x['target']['lane']}:{'big' if x['target']['big'] else 'small'}"
                        for x in first_null.values())
    return dict(run=str(directory),trials=len(rows),forecast_west24=dict(counts),
        ranger_null_targets=len(first_null),ranger_null_targets_per_trial=len(first_null)/len(rows),
        ranger_null_targets_by_birth=dict(null_wave),first_ranger_null_examples=list(first_null.values())[:10],
        protected_mage_primary_launches=theft,protected22_chin_rolls=splash,examples=examples,
        caveat='NULL onset counts can include secondary AoE victims and are not automatically new primary mistakes. '
               'Style comparison is to the predecision snapshot. Different fresh cohorts are not paired causal estimates.',
        sources={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in (directory/'server.log',directory/'trials.csv')})


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('directory',type=Path);args=parser.parse_args()
    result=audit(args.directory);out=args.directory/'ranger-forecast-audit.json';out.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({k:v for k,v in result.items() if k in ('run','trials','forecast_west24','ranger_null_targets','ranger_null_targets_per_trial')},indent=2))
    print('Protected mage primary launches',len(result['protected_mage_primary_launches']),'Protected22 chin rolls',len(result['protected22_chin_rolls']));print(out)
if __name__=='__main__':main()
