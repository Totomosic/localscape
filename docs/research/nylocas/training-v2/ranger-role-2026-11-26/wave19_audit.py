#!/usr/bin/env python3
"""Classify first failed wave20 admissions from the existing native trace.

Policy snapshots precede native NPC processing/admission. They are not exact cap
population measurements; authoritative stall counts come from trials.csv.
"""
import argparse,csv,json,hashlib,statistics
from pathlib import Path
from collections import Counter

def audit(run):
 rows=list(csv.DictReader((run/'trials.csv').open()))
 failed={i for i,r in enumerate(rows) if int(r['stalls_by_last_spawned_wave'].split(';')[19])}
 rooms=[];origins={};trial=-1;forecast={};engines={};nulls={};players={}
 summary=Counter();composition=Counter()
 for line in (run/'server.log').open():
  a=line.lstrip().rstrip('\r\n').split('\t')
  if a[0]=='NYLO_ENGINE': engines[int(a[1])]=[int(x) for x in a[2].split(',')]
  elif a[0]=='NYLO_PLAYERS': players[int(a[1])]=[int(x) for x in a[2].split(',')]
  elif a[0]=='NYLO_FORECAST':forecast[int(a[1]),int(a[2])]=[int(a[3]),int(a[4])]
  elif a[0]=='NYLO_NULL':nulls[int(a[1]),int(a[2])]=set(map(int,a[3].split(','))) if len(a)>3 and a[3] else set()
  elif a[0]=='NYLO_TRACE':
   tick,role,wave=map(int,a[1:4])
   if tick==0 and role==0:
    trial+=1;origins={};forecast={};nulls={};engines={};players={}
    room=dict(seed=int(rows[trial]['seed']),stalls19=int(rows[trial]['stalls_by_last_spawned_wave'].split(';')[19]),spawns={},shots=[],snapshot=None)
    rooms.append(room)
   if role==0:room['spawns'].setdefault(wave,tick)
   crabs=[]
   for raw in a[11].split(';'):
    if not raw:continue
    c=raw.split(',');key,birth,age=map(int,c[:3]);x,y=map(int,c[6:8])
    origins.setdefault(key,'west' if x==3281 else 'east' if 3309<=x<=3310 else 'south' if y==4233 else 'split')
    crabs.append(dict(key=key,wave=birth,age=age,born=tick-age,lane=origins[key],style=c[3],big=c[4]=='true',hp=int(c[5]),x=x,y=y,dying=c[10]=='true',forecast=forecast.get((tick,key)),claimed_until=int(c[8]),own_hit_until=int(c[9]),own_null=key in nulls.get((tick,role),set())))
   if a[5]=='true' and wave in range(15,21):
    c=next((c for c in crabs if c['key']==int(a[8])),None)
    room['shots'].append(dict(tick=tick,role=role,target=c,weapon=a[7],rule=a[9],reason=a[10]))
   if role==0 and 19 in room['spawns'] and tick==room['spawns'][19]+12:
    room['snapshot']=dict(tick=tick,crabs=crabs,ranger=dict(ready=a[4]=='true',attack=a[5]=='true',player=a[6],weapon=a[7],target=int(a[8]),rule=a[9],reason=a[10]),native_delays=engines.get(tick),previous_hp=players.get(tick-1))
    for c in crabs:
     key=f"{c['wave']}:{c['lane']}:{'big' if c['big'] else 'small'}:{c['style']}:{'body' if c['dying'] or c['hp']<=0 else 'live'}"
     if trial in failed:composition[key]+=1
    if trial in failed:summary['first_snapshot_population_'+str(len(crabs))]+=1
   if role==1 and room['snapshot'] is not None and tick==room['snapshot']['tick']:
    room['snapshot']['mage']=dict(ready=a[4]=='true',attack=a[5]=='true',player=a[6],weapon=a[7],target=int(a[8]),rule=a[9],reason=a[10])
 eligible=[float(r['ticks'])*.6 for i,r in enumerate(rows) if i not in failed and r['status']=='completed']
 return dict(run=str(run),trials=len(rows),failed19_rooms=len(failed),failed19_stalls=sum(int(rows[i]['stalls_by_last_spawned_wave'].split(';')[19]) for i in failed),failed19_distribution=dict(Counter(int(rows[i]['stalls_by_last_spawned_wave'].split(';')[19]) for i in failed)),eligible_boss_seconds=dict(n=len(eligible),mean=statistics.mean(eligible),min=min(eligible),max=max(eligible)),summary=dict(summary),failed19_first_admission_composition=dict(composition.most_common()),failed19_rooms_detail=[r for i,r in enumerate(rooms) if i in failed],sources={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in [run/'trials.csv',run/'server.log']},caveat=__doc__)
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();r=audit(a.run);a.output.write_text(json.dumps(r,indent=2)+'\n');print(json.dumps({k:v for k,v in r.items() if k not in ('failed19_rooms_detail','sources')},indent=2))
