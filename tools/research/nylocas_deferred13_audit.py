#!/usr/bin/env python3
"""Trace deferred13 parent removal, exact SW/NE children and last16 admission occupancy.

CSV stall counters are authoritative. Trace population is an observation near
the check, not a replacement for the engine's admission counter.
"""
import argparse
from pathlib import Path
import csv,json,collections
def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('directory',type=Path)
    parser.add_argument('--output',type=Path)
    args=parser.parse_args()
    b=args.directory;rows=list(csv.DictReader((b/'trials.csv').open()));tr=-1;ts=[];orig={};previous={}
    for line in (b/'server.log').open():
     a=line.strip(' \r\n').split('\t')
     if a[0]=='NYLO_PLAYERS' and a[1]=='0':tr+=1;ts.append(dict(seed=rows[tr]['seed'],firstAttack=None,spawn16=None,spawn17=None,split=None,capFrames=[]));orig={};previous={}
     if a[0]!='NYLO_TRACE' or a[2]!='1':continue
     tick,wave=int(a[1]),int(a[3]);cs={}
     for raw in a[11].split(';'):
      if not raw:continue
      c=raw.split(',');key=int(c[0]);orig.setdefault(key,'west' if c[6]=='3281' else 'east' if int(c[6])>=3309 else 'south' if c[7]=='4233' else 'split');cs[key]=dict(key=key,wave=int(c[1]),age=int(c[2]),style=c[3],big=c[4]=='true',hp=int(c[5]),x=int(c[6]),y=int(c[7]),dying=c[10]=='true',lane=orig[key])
     t=ts[-1]
     if wave==16 and t['spawn16'] is None:t['spawn16']=tick
     if wave==17 and t['spawn17'] is None:t['spawn17']=tick
     parents=[c for c in cs.values() if c['wave']==13 and c['lane']=='east' and c['big']]
     if a[5]=='true' and int(a[8]) in cs:
      c=cs[int(a[8])]
      if c in parents and t['firstAttack'] is None:t['firstAttack']=dict(tick=tick,age=c['age'],currentWave=wave,position=a[6],target=c)
     gone=[c for key,c in previous.items() if key not in cs and c['wave']==13 and c['lane']=='east' and c['big']]
     if gone and t['split'] is None:
      parent=gone[0];new=[c for key,c in cs.items() if key not in previous and not c['big'] and c['age']==0]
      sw=[c for c in new if (c['x'],c['y'])==(parent['x'],parent['y'])];ne=[c for c in new if (c['x'],c['y'])==(parent['x']+1,parent['y']+1)]
      t['split']=dict(tick=tick,wave=wave,parent=parent,matchedBirthPair=sw+ne if len(sw)==1 and len(ne)==1 else [],allNew=new)
     if t['spawn16'] is not None and tick in (t['spawn16']+4,t['spawn16']+8,t['spawn16']+12):
      t['capFrames'].append(dict(tick=tick,wave=wave,populationObservation=len(cs),keys=sorted(cs),parent=parents,matchingPair=t['split']))
     previous=cs
    co=collections.Counter();witness=[]
    for i,t in enumerate(ts):
     stalls=list(map(int,rows[i]['stalls_by_last_spawned_wave'].split(';')))[16];t['last16Stalls']=stalls
     if t['firstAttack']:co['first_request_age_'+str(t['firstAttack']['age'])]+=1
     if t['split']:
      s=t['split'];co['split_current_wave_'+str(s['wave'])]+=1
      if s['matchedBirthPair']:
       co['exact_pair_matched']+=1
       if t['spawn16'] is not None:
        delta=s['tick']-(t['spawn16']+4);co['split_relative_first_17_check_'+str(delta)]+=1
       if s['wave']==16 and stalls>0:
        co['matched_split_in16_with16stall']+=1
        if len(witness)<8:witness.append(t)
    threshold_witnesses=[]
    for t in ts:
     if not t['split'] or not t['split']['matchedBirthPair']:continue
     keys={c['key'] for c in t['split']['matchedBirthPair']}
     for f in t['capFrames']:
      if f['wave']==16 and f['populationObservation']==12 and keys.issubset(f['keys']):
       co['threshold12_with_both13_children']+=1
       threshold_witnesses.append(dict(seed=t['seed'],firstAttack=t['firstAttack'],split=t['split'],frame=f,spawn16=t['spawn16'],spawn17=t['spawn17'],last16Stalls=t['last16Stalls']))
    result=dict(run=str(b),threshold_witnesses=threshold_witnesses,counts=co,witnesses=witness,per_trial=ts,caveat='Trace snapshots are pre-decision; admission processing may occur in the intervening native cycle. CSV stall counters are authoritative. Split attribution requires the exact SW/NE zero-age pair on the observed13-parent removal tick; ambiguous pairs are not attributed.')
    output=args.output or b/'deferred13-cap16-audit.json'
    output.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(dict(run=str(b),trials=len(ts),counts=co,output=str(output)),indent=2))


if __name__ == "__main__":
    main()
