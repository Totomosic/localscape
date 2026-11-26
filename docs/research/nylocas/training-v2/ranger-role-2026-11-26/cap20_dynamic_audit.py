#!/usr/bin/env python3
"""Track physical cap bodies near the first20 admission; trace-phase caveat applies."""
import argparse,csv,json,hashlib
from pathlib import Path
from collections import Counter

def audit(run):
 rows=list(csv.DictReader((run/'trials.csv').open()));rooms=[];trial=-1;previous={};origins={};nulls={};forecasts={};lineage={}
 for line in (run/'server.log').open():
  a=line.lstrip().rstrip('\r\n').split('\t')
  if a[0]=='NYLO_FORECAST':forecasts[int(a[1]),int(a[2])]=[int(a[3]),int(a[4])]
  elif a[0]=='NYLO_NULL':nulls[int(a[1]),int(a[2])]=set(map(int,a[3].split(','))) if len(a)>3 and a[3] else set()
  elif a[0]=='NYLO_TRACE' and a[2]=='0':
   tick,wave=int(a[1]),int(a[3])
   if tick==0:
    trial+=1;previous={};origins={};forecasts={};nulls={};lineage={};room=dict(seed=int(rows[trial]['seed']),failed19=int(rows[trial]['stalls_by_last_spawned_wave'].split(';')[19]),spawn19=None,spawn18=None,frames=[]);rooms.append(room)
   if wave==18 and room['spawn18'] is None:room['spawn18']=tick
   if wave==19 and room['spawn19'] is None:room['spawn19']=tick
   crabs={}
   for raw in a[11].split(';'):
    if not raw:continue
    c=raw.split(',');key,birth,age=map(int,c[:3]);x,y=map(int,c[6:8]);origins.setdefault(key,'west' if x==3281 else 'east' if 3309<=x<=3310 else 'south' if y==4233 else 'split')
    crabs[key]=dict(key=key,wave=birth,age=age,born=tick-age,lane=origins[key],big=c[4]=='true',style=c[3],hp=int(c[5]),x=x,y=y,dying=c[10]=='true',own_null=key in nulls.get((tick,0),set()),mage_null=key in nulls.get((tick,1),set()),forecast=forecasts.get((tick,key)))
   new_children=[c for k,c in crabs.items() if k not in previous and not c['big'] and c['lane']=='split' and c['born']==tick]
   removed_parents=[c for k,c in previous.items() if k not in crabs and c['big']]
   for parent in removed_parents:
    for offset in (0,1):
     matches=[c for c in new_children if c['x']==parent['x']+offset and c['y']==parent['y']+offset]
     if len(matches)==1:lineage.setdefault(matches[0]['key'],[]).append({k:parent[k] for k in ['key','wave','lane','born','x','y']})
   for key,c in crabs.items():
    if c['lane']=='split':c['parents']=lineage.get(key,[])
   age19=None if room['spawn19'] is None else tick-room['spawn19']
   # Capture incoming18 plus the first20 check and delayed retries.
   if room['spawn18'] is not None and (age19 is None or age19<=32):
    population=list(crabs.values());existing=[c for c in population if c['wave']<20]
    room['frames'].append(dict(tick=tick,wave=wave,age19=age19,population=len(population),pre20_bodies=len(existing),composition=dict(Counter(f"{c['wave']}:{'big' if c['big'] else 'small'}:{'body' if c['dying'] or c['hp']<=0 else 'live'}" for c in existing)),born=[c for k,c in crabs.items() if k not in previous],removed=[c for k,c in previous.items() if k not in crabs],pre20_crabs=existing,ready=a[4]=='true',attack=a[5]=='true',weapon=a[7],selected=int(a[8]),reason=a[10]))
   previous=crabs
 summary={}
 for category in ['fail','pass']:
  subset=[r for r in rooms if bool(r['failed19'])==(category=='fail')];checks=[next((f for f in r['frames'] if f['age19']==12),None) for r in subset];checks=[f for f in checks if f]
  summary[category]=dict(rooms=len(subset),pre20_population=dict(Counter(f['pre20_bodies'] for f in checks)),composition=dict(Counter({})))
  totals=Counter()
  for f in checks:totals.update(f['composition'])
  summary[category]['composition']=dict(totals.most_common())
  parents=Counter()
  for f in checks:
   for c in f['pre20_crabs']:
    if c['big'] or c['lane']!='split':continue
    tag='unknown' if not c['parents'] else 'ambiguous' if len(c['parents'])!=1 else str(c['parents'][0]['wave'])+':'+c['parents'][0]['lane']
    parents[tag]+=1
  summary[category]['split_parent_composition']=dict(parents.most_common())
 return dict(run=str(run),summary=summary,rooms=rooms,caveat=__doc__+' Admission is after NPC processing; CSV stalls are authoritative. Original20 births are excluded from pre20_bodies.',sources={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in [run/'server.log',run/'trials.csv']})
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();r=audit(a.run);a.output.write_text(json.dumps(r,indent=2)+'\n');print(json.dumps(r['summary'],indent=2))
