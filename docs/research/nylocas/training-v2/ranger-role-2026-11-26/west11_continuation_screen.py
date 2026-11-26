#!/usr/bin/env python3
"""Screen valid survivingWest11 identities just after13; distance is not LOS."""
import argparse,csv,json
from pathlib import Path
from collections import Counter

def audit(run):
 rows=list(csv.DictReader((run/'trials.csv').open()));trial=-1;spawn12=None;forecasts={};origins={};nulls={};counts=Counter();samples=[];last=None
 for line in (run/'server.log').open():
  a=line.lstrip().rstrip('\r\n').split('\t')
  if a[0]=='NYLO_FORECAST':forecasts[int(a[1]),int(a[2])]=[int(a[3]),int(a[4])]
  elif a[0]=='NYLO_NULL':nulls[int(a[1]),int(a[2])]=set(map(int,a[3].split(','))) if len(a)>3 and a[3] else set()
  elif a[0]=='NYLO_TRACE':
   tick,role,wave=map(int,a[1:4])
   if tick==0 and role==0:trial+=1;spawn12=None;forecasts={};origins={};nulls={}
   if wave==12 and role==0 and spawn12 is None:spawn12=tick
   crabs=[]
   for raw in a[11].split(';'):
    if not raw:continue
    c=raw.split(',');key,birth,age=map(int,c[:3]);x,y=map(int,c[6:8]);origins.setdefault(key,'west' if x==3281 else 'east' if 3309<=x<=3310 else 'south' if y==4233 else 'split');crabs.append(dict(key=key,wave=birth,age=age,born=tick-age,lane=origins[key],big=c[4]=='true',style=c[3],hp=int(c[5]),x=x,y=y,dying=c[10]=='true',forecast=forecasts.get((tick,key)),ranger_null=key in nulls.get((tick,0),set()),mage_null=key in nulls.get((tick,1),set())))
   if role==1 and last and last['tick']==tick and a[5]=='true':last['mage_native']=[a[7],int(a[8]),a[9]]
   if role!=0 or wave<=12 or wave>=20 or spawn12 is None or tick>spawn12+15:continue
   for c in crabs:
    if c['wave']!=11 or c['lane']!='west' or not c['big'] or c['hp']<=0 or c['dying'] or c['ranger_null'] or c['style']!='MAGIC':continue
    if c['forecast'] and c['forecast'][1]>=c['hp']:continue
    px,py=map(int,a[6].split(',')[:2]);distance=max(c['x']-px,px-c['x']-1,c['y']-py,py-c['y']-1);counts['target_frames']+=1
    if a[4]=='true':counts['ready_frames']+=1
    if distance<=8:counts['eye8_frames']+=1
    if a[5]=='true' and int(a[8])!=c['key']:counts['other_native_attack_frames']+=1
    if len(samples)<50:
     last=dict(seed=int(rows[trial]['seed']),tick=tick,wave=wave,age12=tick-spawn12,target=c,ready=a[4]=='true',attack=a[5]=='true',selected=int(a[8]),weapon=a[7],player=a[6],distance=distance,reason=a[10]);samples.append(last)
 return dict(run=str(run),counts=dict(counts),examples=samples,caveat=__doc__)
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();r=audit(a.run);a.output.write_text(json.dumps(r,indent=2)+'\n');print(json.dumps(r['counts'],indent=2));print(json.dumps(r['examples'][:2],indent=2))
