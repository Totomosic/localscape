#!/usr/bin/env python3
"""Check the narrow west24 pipe-to-Eye flight window using native roll/impact telemetry.

Missing roll telemetry remains unknown. Leases use the previous snapshot because
current-row flight release occurs after the decision. This makes no damage rolls.
"""
from pathlib import Path
import argparse,json,sys,collections,hashlib
sys.path.insert(0,'tools/research');from nylocas_guidance_audit import read_run
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('runs',nargs='+',type=Path);parser.add_argument('--output',type=Path,required=True);args=parser.parse_args()
result=[]
for p in args.runs:
 n=str(p);rows,frames,shots=read_run(p);west={};byframe={}
 for f in frames:
  byframe[f['trial'],f['tick'],f['role']]=f
  if f['role']==0:
   for c in f['crabs'].values():
    if c['wave']==24 and c['lane']=='west' and c['big']:west[f['trial']]=c['key']
 pipes={}
 for s in shots:
  if s['role']==0 and s['target_key']==west.get(s['trial']) and s['weapon']=='PIPE':pipes.setdefault(s['trial'],s)
 trial=-1;previous=None;rolls={};impacts=collections.defaultdict(list)
 for l in (p/'server.log').open():
  a=l.strip().split('\t');tag=a[0]
  if tag=='NYLO_TRACE':
   t=int(a[1])
   if previous is None or t<previous:trial+=1
   previous=t
  if trial<0:continue
  if tag=='NYLO_ROLL' and a[2]=='0' and int(a[3])==west.get(trial) and a[4]=='12926':rolls.setdefault(trial,dict(tick=int(a[1]),damage=int(a[5]),maximum=int(a[7])))
  if tag=='NYLO_IMPACT' and int(a[3])==west.get(trial):impacts[trial].append(dict(tick=int(a[1]),role=int(a[2]),damage=int(a[5]),hp=int(a[6]),thrall=a[7]=='true'))
 counts=collections.Counter(confirmed_first_pipes=len(pipes));examples=[]
 for trial,key in west.items():
  pipe=pipes.get(trial);roll=rolls.get(trial)
  if not pipe or not roll or roll['tick']!=pipe['tick']:continue
  counts['first_pipes_with_known_roll']+=1
  if roll['damage']>=pipe['target']['hp']:counts['rolled_lethal']+=1;continue
  counts['rolled_nonlethal']+=1
  age5=byframe.get((trial,pipe['target']['born']+5,0));prior=byframe.get((trial,pipe['target']['born']+4,0));old=age5['crabs'].get(key) if age5 else None
  blocked=bool(old and old['hp']>0 and prior and prior['crabs'].get(key,{}).get('own_hit_until',0)>age5['tick'])
  eye=any(s['trial']==trial and s['role']==0 and s['target_key']==key and s['weapon']=='EYE' and s['target']['age'] in (5,6) for s in shots)
  if eye:counts['nonlethal_magic_window_eye']+=1
  if blocked and not eye:
   counts['nonlethal_airborne_at_age5_without_eye']+=1
   if len(examples)<8:examples.append(dict(seed=rows[trial]['seed'],key=key,born=pipe['target']['born'],pipe_tick=pipe['tick'],roll=roll,
        age5_tick=age5['tick'],age5_predecision_hp=old['hp'],previous_frame_flight_until=prior['crabs'][key]['own_hit_until'],age5_selected_weapon=age5['weapon'],age5_selected_target=age5['target_key'],native_impacts=impacts[trial]))
 counts['first_pipes_without_matching_roll']=len(pipes)-counts['first_pipes_with_known_roll']
 result.append(dict(run=n,counts=dict(counts),examples=examples,sources={str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in [p/'server.log',p/'trials.csv']}))
out=args.output;out.write_text(json.dumps(result,indent=2)+'\n')
for r in result:print(r['run'],r['counts'])
