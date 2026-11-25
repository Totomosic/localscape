#!/usr/bin/env python3
"""Find terminal closed-work melee assistance during Ranger ready/empty cleanup decisions.

Claims/own leases use previous after-cycle snapshots. HP/style/positions use current
pre-cycle copied fields. Mage-ready same-target plans conservatively exclude
provisional claims. Native current LOS and remaining final-rule completion are not
in this trace; candidates require policy replay/native confirmation.
"""
from pathlib import Path
from collections import Counter
import csv,json,hashlib,sys
run=Path(sys.argv[1]);out=Path(sys.argv[2]);csvrows=list(csv.DictReader((run/'trials.csv').open()));trial=-1;prior={};pending=None;rooms=[];nulls={};births={}
for raw in (run/'server.log').open():
 line=raw.lstrip(' ').rstrip('\n')
 if line.startswith('NYLO_NULL\t'):
  _,tick,role,keys=line.split('\t');nulls[int(tick),int(role)]=set(map(int,keys.split(','))) if keys else set();continue
 if not line.startswith('NYLO_TRACE\t'):continue
 f=line.split('\t');tick,role,wave=map(int,f[1:4])
 if tick==0 and role==0:
  trial+=1;room=dict(seed=int(csvrows[trial]['seed']),windows=[],births={},lastPositive={},lastPresent={},mageStates={});rooms.append(room);prior={};nulls={};births={}
 if role==0:
  cs={}
  for item in f[11].split(';'):
   if not item:continue
   z=item.split(',');k=int(z[0]);c=dict(key=k,wave=int(z[1]),age=int(z[2]),style=z[3],big=z[4]=='true',hp=int(z[5]),x=int(z[6]),y=int(z[7]),claim=int(z[8]),own=int(z[9]),dead=z[10]=='true')
   room['births'].setdefault(k,dict(wave=c['wave'],born=tick-c['age']));room['lastPresent'][k]=tick
   if c['hp']>0 and not c['dead']:room['lastPositive'][k]=tick
   cs[k]=c
  if wave==31 and f[4]=='true' and int(f[8])==-1 and not any(c['big'] for c in cs.values()) and not any(c['hp']>0 and not c['dead'] and c['style'] in ('RANGE','MAGIC') and k not in nulls.get((tick,0),set()) for k,c in cs.items()):
   px,py,_=map(int,f[6].split(','));candidates=[]
   for k,c in cs.items():
    previous=prior.get(k,{})
    claim=previous.get('claim',0);own=previous.get('own',0)
    distance=max(abs(c['x']-px),abs(c['y']-py))
    if c['wave']>=30 and not c['big'] and c['style']=='MELEE' and c['hp']>0 and not c['dead'] and c['age']<49 and distance>1 and claim<=tick and not(own>0 and own>=tick) and k not in nulls.get((tick,0),set()):
     candidates.append(dict(c,distance=distance,preClaim=claim,preOwn=own,mageNull=k in nulls.get((tick,1),set())))
   if candidates:pending=dict(tick=tick,position=f[6],rangerReason=f[10],candidates=candidates)
  prior=cs
 elif pending and pending['tick']==tick:
  target=int(f[8]);mageReady=f[4]=='true'
  pending['mage']=dict(ready=mageReady,attacked=f[5]=='true',target=target,weapon=f[7],reason=f[10])
  for c in pending['candidates']:c['mageProvisionalPlanExcluded']=mageReady and target==c['key']
  room['windows'].append(pending);pending=None
for room in rooms:
 fresh=[k for k,b in room['births'].items() if b['wave']>=30];old=[k for k,b in room['births'].items() if b['wave']<=29]
 lastFresh=max((room['lastPresent'][k] for k in fresh),default=-1);lastOld=max((room['lastPresent'][k] for k in old),default=-1)
 terminalFresh={k for k in fresh if room['lastPresent'][k]==lastFresh}
 room['lastFreshPresent']=lastFresh;room['lastOldPresent']=lastOld;room['freshOutlastsOld']=lastFresh>lastOld
 for window in room['windows']:
  for c in window['candidates']:
   c['terminalFresh']=c['key'] in terminalFresh;c['lastPresent']=room['lastPresent'][c['key']];c['afterAllOldGone']=window['tick']>lastOld
  window['candidates']=[c for c in window['candidates'] if not c['mageProvisionalPlanExcluded']]
 room['windows']=[w for w in room['windows'] if w['candidates']]
 for key in ('births','lastPositive','lastPresent','mageStates'):room.pop(key,None)
valid=[r for r in rooms if r['windows']]
summary=dict(run=str(run),semantics=__doc__,trials=len(rooms),opportunityRooms=len(valid),frames=sum(len(r['windows']) for r in valid),terminalFreshRooms=sum(any(c['terminalFresh'] for w in r['windows'] for c in w['candidates']) for r in valid),freshOutlastsOldRooms=sum(r['freshOutlastsOld'] for r in valid),terminalFreshAfterOldRooms=sum(r['freshOutlastsOld'] and any(c['terminalFresh'] for w in r['windows'] for c in w['candidates']) for r in valid),framesAfterAllOldGone=sum(w['tick']>r['lastOldPresent'] for r in valid for w in r['windows']),rooms=valid,sourceLogSha256=hashlib.sha256((run/'server.log').read_bytes()).hexdigest())
out.write_text(json.dumps(summary,indent=2)+'\n')
print(run.name,{k:summary[k] for k in ('trials','opportunityRooms','frames','terminalFreshRooms','freshOutlastsOldRooms','terminalFreshAfterOldRooms','framesAfterAllOldGone')})
for r in valid[:8]:
 w=r['windows'][0];print(r['seed'],w['tick'],'oldLast',r['lastOldPresent'],'freshLast',r['lastFreshPresent'],'candidates',[(c['key'],c['distance'],c['age'],c['terminalFresh']) for c in w['candidates']],w['mage'])
