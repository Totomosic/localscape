from pathlib import Path
from collections import Counter,defaultdict
import csv,json,statistics,hashlib
run=Path('.state/simulations/training-v2-flight-fresh-intent-100')
rows=list(csv.DictReader((run/'trials.csv').open()))
rooms=[];current=None
for line in (run/'server.log').open():
 a=line.strip(' \r\n').split('\t')
 if a[0]=='NYLO_TRACE' and a[1]=='0' and a[2]=='0':
  current=dict(seed=int(rows[len(rooms)]['seed']),frames={},births={},spawns={},attacks=[],nulls={})
  rooms.append(current)
 if a[0]=='NYLO_NULL':current['nulls'][int(a[1]),int(a[2])]=set(map(int,a[3].split(',')))
 if a[0]!='NYLO_TRACE':continue
 tick,role,wave=map(int,a[1:4]);cs={}
 for c in (a[11] if len(a)>11 else '').split(';'):
  if not c:continue
  key,w,age,style,big,hp,x,y,claim,own,dying=c.split(',');key=int(key)
  c=dict(key=key,wave=int(w),age=int(age),style=style,big=big=='true',hp=int(hp),x=int(x),y=int(y),claim=int(claim),own=int(own),dying=dying=='true')
  cs[key]=c
  if key not in current['births']:
   c=c.copy();c['born']=tick-c['age'];c['lane']='west' if c['x']==3281 else 'east' if c['x']>=3309 else 'south' if c['y']==4233 else 'split';current['births'][key]=c
 current['spawns'].setdefault(wave,tick)
 if 18<=wave<=21:current['frames'][tick,role]=dict(wave=wave,ready=a[4]=='true',attacked=a[5]=='true',point=a[6],weapon=a[7],key=int(a[8]),rule=a[9],reason=a[10],crabs=cs)
 if a[5]=='true':current['attacks'].append(dict(tick=tick,role=role,weapon=a[7],key=int(a[8]),rule=a[9],reason=a[10]))
checks=[];first=defaultdict(list);orders=Counter();examples=[]
for room,row in zip(rooms,rows):
 s19,s20=room['spawns'][19],room['spawns'][20]
 ordered=[]
 for lane in ['west','east','south']:
  keys=[k for k,b in room['births'].items() if b['wave']==19 and b['big'] and b['lane']==lane]
  aa=[a for a in room['attacks'] if a['role']==1 and a['key'] in keys]
  if aa:first[lane].append(aa[0]['tick']-s19);ordered.append((aa[0]['tick'],lane))
 orders[','.join(l for _,l in sorted(ordered))]+=1
 n=int(row['stalls_by_last_spawned_wave'].split(';')[19]); assert s20-s19==12+4*n,(room['seed'],s19,s20,n)
 for i in range(n):
  t=s19+12+4*i;f=room['frames'].get((t,1));cs=f['crabs']
  bigs=[dict(c,lane=room['births'][k]['lane']) for k,c in cs.items() if c['big']]
  checks.append(dict(seed=room['seed'],tick=t,attempt=i,population=len(cs),live=sum(not c['dying'] for c in cs.values()),big19=bigs,allCrabs=cs,mage=f,ranger=room['frames'][t,0]))
 if n>=3:
  examples.append(dict(seed=room['seed'],s19=s19,s20=s20,stalls=n,attacks=[dict(a,targetBirth=room['births'].get(a['key'])) for a in room['attacks'] if s19-2<=a['tick']<s20]))
print('first mage19 launch ages', {lane:dict(Counter(v)) for lane,v in first.items()});print('orders',orders)
print('checks',len(checks),'population distribution',Counter(c['population'] for c in checks),'living',Counter(c['live'] for c in checks))
print('living big birthlane counts',Counter((b['wave'],b['lane']) for c in checks for b in c['big19'] if not b['dying']))
print('19 big living checks',Counter(b['lane'] for c in checks for b in c['big19'] if b['wave']==19 and not b['dying']))
print('examples',[(e['seed'],e['stalls']) for e in examples])
result=dict(checks=checks,examples=examples)
# Preserve full rich Python structures for quick local exploration; only serializable public fields in JSON.
import pickle
Path('/tmp/nylo-mage-transition.pkl').write_bytes(pickle.dumps((rooms,rows,checks,examples)))
Path('/tmp/nylo-mage-transition.json').write_text(json.dumps(result,indent=2)+'\n')
