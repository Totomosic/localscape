#!/usr/bin/env python3
"""Find normal-wave21-return opportunities for settled transition parent recovery.

These are review candidates, not claims that every frame had an unobstructed ray.
A same-tick native Eye at another target provides stronger evidence of readiness.
"""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path

from nylocas_guidance_audit import read_run


def audit(directory):
    rows, frames, shots = read_run(directory)
    forecast, trial = {}, -1
    for line in (directory/'server.log').open():
        a=line.strip().split('\t')
        if a[0]=='NYLO_TRACE' and a[1:3]==['0','0']:trial+=1
        elif a[0]=='NYLO_FORECAST':forecast[trial,int(a[1]),int(a[2])]=(int(a[3]),int(a[4]))
    spawns, bows = {}, {}
    for f in frames:
        if f['role']==0:spawns.setdefault((f['trial'],f['wave']),f['tick'])
    for s in shots:
        c=s['target']
        if s['role']==0 and s['weapon']=='BOW' and c['wave']==20 and c['lane']=='west' and c['big']:
            bows.setdefault(s['trial'],s['tick'])
    counts, rooms, examples = Counter(), {'18':set(),'19':set()}, {'18':[],'19':[]}
    for f in frames:
        if f['role']!=0 or f['wave']!=20 or f['trial'] not in bows or f['tick']<bows[f['trial']]+5:continue
        px,py=map(int,f['player'].split(',')[:2]);due=spawns[f['trial'],20]+16
        if f['tick']+max(3,(max(abs(px-3301),abs(py-4248))+1)//2)>due:continue
        for c in f['crabs'].values():
            if not c['big'] or c['wave'] not in (18,19) or c['dying'] or c['hp']<=0 or c['nulled'] or c['style']!='MAGIC':continue
            if c['wave']==18 and not 26<=c['age']<=35:continue
            if c['wave']==19 and not 7<=c['age']<=24:continue
            hp,damage=forecast.get((f['trial'],f['tick'],c['key']),(-1,-1))
            if damage>=hp>0:continue
            if damage<0 and (c['claimed_until']>f['tick'] or c['own_hit_until']>f['tick']):continue
            distance=max(c['x']-px,px-c['x']-1,c['y']-py,py-c['y']-1)
            if distance>8:continue
            group=str(c['wave']);counts[group+'_frames']+=1;rooms[group].add(f['trial'])
            if f['ready']:counts[group+'_policy_ready_frames']+=1
            if f['attack']:counts[group+'_other_native_attack_frames']+=f['target_key']!=c['key']
            if f['attack'] and f['weapon']=='EYE' and f['target_key']!=c['key']:counts[group+'_other_native_eye_frames']+=1
            if len(examples[group])<12 and f['ready']:
                examples[group].append(dict(seed=f['seed'],tick=f['tick'],target=c,forecast=[hp,damage],
                    bow_tick=bows[f['trial']],normal21_due=due,player=f['player'],
                    selected=[f['weapon'],f['target_key'],f['rule']],native_attack=f['attack'],reason=f['reason']))
    return dict(run=str(directory),trials=len(rows),counts=dict(counts),rooms={k:len(v) for k,v in rooms.items()},examples=examples,
                caveat=__doc__,sources={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in (directory/'server.log',directory/'trials.csv')})


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('runs',nargs='+',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
    results=[]
    for directory in a.runs:
        result=audit(directory);results.append(result);print(directory,result['counts'],result['rooms'])
    a.output.write_text(json.dumps(results,indent=2)+'\n')
if __name__=='__main__':main()
