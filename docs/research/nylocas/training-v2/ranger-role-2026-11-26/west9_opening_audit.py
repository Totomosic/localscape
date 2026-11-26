#!/usr/bin/env python3
"""Measure West9 parent opening and original melee-small service; no LOS claim."""
import argparse,csv,json
from pathlib import Path
from collections import Counter

def audit(run):
 rows=list(csv.DictReader((run/'trials.csv').open()));rooms=[];trial=-1;origins={};forecasts={};last=None;nulls={}
 for line in (run/'server.log').open():
  a=line.lstrip().rstrip('\r\n').split('\t')
  if a[0]=='NYLO_FORECAST':forecasts[int(a[1]),int(a[2])]=[int(a[3]),int(a[4])]
  elif a[0]=='NYLO_NULL':nulls[int(a[1]),int(a[2])]=set(map(int,a[3].split(','))) if len(a)>3 and a[3] else set()
  elif a[0]=='NYLO_ENGINE':
   if last and last['tick']==int(a[1]):last['native_delays']=list(map(int,a[2].split(',')));last['after_positions']=a[4]
  elif a[0]=='NYLO_TRACE':
   tick,role,wave=map(int,a[1:4])
   if tick==0 and role==0:
    trial+=1;origins={};forecasts={};nulls={};room=dict(seed=int(rows[trial]['seed']),spawn9=None,parent=None,parent_first=None,parent_zero=None,parent_removed=None,small=None,small_first=None,frames=[]);rooms.append(room)
   if wave==9 and room['spawn9'] is None:room['spawn9']=tick
   crabs={}
   for raw in a[11].split(';'):
    if not raw:continue
    c=raw.split(',');key,birth,age=map(int,c[:3]);x,y=map(int,c[6:8]);origins.setdefault(key,'west' if x==3281 else 'east' if 3309<=x<=3310 else 'south' if y==4233 else 'split');crabs[key]=dict(key=key,wave=birth,age=age,born=tick-age,lane=origins[key],big=c[4]=='true',style=c[3],hp=int(c[5]),x=x,y=y,dying=c[10]=='true',forecast=forecasts.get((tick,key)),ranger_null=key in nulls.get((tick,0),set()),mage_null=key in nulls.get((tick,1),set()))
   for c in crabs.values():
    if c['wave']==9 and c['lane']=='west':
     if c['big']:room['parent']=c['key']
     else:room['small']=c['key']
   if role==0 and room['parent'] is not None:
    p=crabs.get(room['parent']);age=tick-room['spawn9']
    if p and (p['dying'] or p['hp']<=0) and room['parent_zero'] is None:room['parent_zero']=age
    if not p and room['parent_removed'] is None:room['parent_removed']=age
   if a[5]=='true' and int(a[8]) in crabs:
    c=crabs[int(a[8])]
    if c['wave']==9 and c['lane']=='west':
     k='parent_first' if c['big'] and role==0 else 'small_first' if not c['big'] else None
     if k and room[k] is None:room[k]=dict(tick=tick,age=c['age'],weapon=a[7],role=role)
   if role==0 and wave in [8,9] and (room['spawn9'] is None or tick<=room['spawn9']+10):
    last=dict(tick=tick,wave=wave,age9=None if room['spawn9'] is None else tick-room['spawn9'],ready=a[4]=='true',attack=a[5]=='true',player=a[6],weapon=a[7],target=int(a[8]),rule=a[9],reason=a[10],crabs=list(crabs.values()));room['frames'].append(last)
 return dict(run=str(run),rooms=len(rows),first_parent_ages=dict(Counter(str(q['parent_first']['age'] if q['parent_first'] else None)for q in rooms)),parent_zero_ages=dict(Counter(q['parent_zero'] for q in rooms)),parent_removal_ages=dict(Counter(q['parent_removed'] for q in rooms)),first_small_ages=dict(Counter(str(q['small_first']['age'] if q['small_first'] else None)for q in rooms)),detail=rooms,caveat=__doc__)
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();r=audit(a.run);a.output.write_text(json.dumps(r,indent=2)+'\n');print(json.dumps({k:v for k,v in r.items() if k!='detail'},indent=2))
