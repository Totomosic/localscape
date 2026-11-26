"""Screen old Ranger primaries against subsequently observed births and actual advance observations.

A future birth alone is not foresight. Known dying-body+7 is conservative; exact
born+55 natural expiry is recorded separately. Reach does not prove future ray.
"""
import argparse
import collections
import csv
import json
from pathlib import Path

def distance(xy,body):
 size=2 if body['big'] else 1
 return max(body['x']-xy[0],xy[0]-body['x']-size+1,body['y']-xy[1],xy[1]-body['y']-size+1)

def audit(run):
 rows=list(csv.DictReader((run/'trials.csv').open()));rooms=[];room=None;origins={};previous={};nulls={};forecast={}
 for line in(run/'server.log').open():
  a=line.lstrip().rstrip('\r\n').split('\t')
  if a[0]=='NYLO_TRACE' and a[1:3]==['0','0']:
   room=dict(seed=int(rows[len(rooms)]['seed']),births={},oldshots=[],opened=set(),dead_bigs={});rooms.append(room);origins={};previous={};nulls={};forecast={}
  if room is None:continue
  if a[0]=='NYLO_NULL':nulls[int(a[1]),int(a[2])]=set(map(int,a[3].split(','))) if len(a)>3 and a[3] else set()
  elif a[0]=='NYLO_FORECAST':forecast[int(a[1]),int(a[2])]=list(map(int,a[3:5]))
  elif a[0]=='NYLO_TRACE' and a[2]=='0':
   tick,wave=int(a[1]),int(a[3]);crabs={};xy=list(map(int,a[6].split(',')))
   for raw in a[11].split(';'):
    if not raw:continue
    c=raw.split(',');key,birth,age=map(int,c[:3]);x,y=map(int,c[6:8]);origins.setdefault(key,'west' if x==3281 else 'east' if x in[3309,3310] else 'south' if y==4233 else 'split')
    body=dict(key=key,wave=birth,born=tick-age,lane=origins[key],style=c[3],big=c[4]=='true',hp=int(c[5]),x=x,y=y,dying=c[10]=='true',forecast=forecast.get((tick,key)))
    crabs[key]=body
    if age==0 and key not in room['births']:room['births'][key]=dict(**body,parents=[])
    if body['big'] and(body['hp']<=0 or body['dying']):room['dead_bigs'].setdefault(key,dict(first=tick,wave=birth,born=tick-age,lane=origins[key]))
   newborn=[b for key,b in crabs.items() if key not in previous and b['born']==tick and not b['big'] and b['lane']=='split']
   for key,old in previous.items():
    if key in crabs or not old['big']:continue
    info=room['dead_bigs'].get(key,dict(first=None,wave=old['wave'],born=old['born'],lane=old['lane']))
    for child in newborn:
     if(child['x'],child['y'])in[(old['x'],old['y']),(old['x']+1,old['y']+1)]:room['births'][child['key']]['parents'].append(dict(key=key,removed=tick,**info))
   selected=crabs.get(int(a[8]))
   if a[5]=='true' and selected:
    if wave<20 and not selected['big'] and a[9]=='null' and wave-selected['wave']>=4:
     room['oldshots'].append(dict(tick=tick,wave=wave,target=selected,xy=xy,already_hit=selected['key']in room['opened'],other_null=selected['key']in nulls.get((tick,1),set()),population=len(crabs),weapon=a[7],reason=a[10]))
    room['opened'].add(selected['key'])
   previous=crabs
 events=[];counts=collections.Counter()
 for room in rooms:
  for shot in room['oldshots']:
   if shot['already_hit'] or shot['other_null']:continue
   future=[]
   for body in room['births'].values():
    delta=body['born']-shot['tick']
    if not 1<=delta<=2:continue
    p=body['parents'][0] if len(body['parents'])==1 else None
    # The screen asks whether a matching own attack could physically be approached in two ticks.
    weapon_range=8 if body['style']=='MAGIC' else 5 if body['style']=='RANGE' else 1
    reachable=distance(shot['xy'],body)<=weapon_range+delta*2
    known_due=None
    if p is not None and p['first'] is not None and p['first']<=shot['tick']:known_due=p['first']+7
    elif p is not None and p['first'] is None and p['born']+55==body['born']:known_due=p['born']+55
    future.append(dict(body=body,delta=delta,reachable=reachable,model_delta=None if known_due is None else known_due-shot['tick']))
   relevant=[b for b in future if b['reachable'] and b['model_delta'] is not None]
   if relevant:
    counts['unhit_old_before_observed_local_split']+=1
    counts['model_due_within_two' if any(b['model_delta']<=2 for b in relevant) else 'conservative_model_due_later']+=1
    events.append(dict(seed=room['seed'],shot=shot,future=relevant))
 return dict(run=str(run),counts=dict(counts),events=events,caveat=__doc__)
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();d=audit(a.run);a.output.write_text(json.dumps(d,indent=2)+'\n');print(json.dumps(d['counts'],indent=2))
