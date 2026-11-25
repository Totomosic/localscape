#!/usr/bin/env python3
"""Evaluate soft known-last-PIPE projection and retain a separate strict future-packet audit."""
from pathlib import Path
import json,math,sys
source=Path(sys.argv[1]);x=json.loads(source.read_text());run=Path(x['run'])
trial=-1;primary={};fallback={}
for raw in (run/'server.log').open():
 line=raw.lstrip(' ').rstrip('\n')
 if line.startswith('NYLO_TRACE\t'):
  f=line.split('\t');tick,role=map(int,f[1:3])
  if tick==0 and role==0:trial+=1
  if role==0 and f[5]=='true':fallback.setdefault(trial,[]).append((tick,int(f[8]),f[7]))
 elif line.startswith('NYLO_PRIMARY\t'):
  f=line.split('\t')
  if int(f[2])==0:primary.setdefault(trial,[]).append((int(f[1]),int(f[3]),'PIPE' if f[4]=='12926' else 'OTHER'))
rows=[]
for row in (run/'trials.csv').read_text().splitlines()[1:]:rows.append(int(row.split(',')[1]))
trials={seed:i for i,seed in enumerate(rows)}
def distance(a,b,size=1):return max(b[0]-a[0],a[0]-b[0]-size+1,b[1]-a[1],a[1]-b[1]-size+1)
def attack_tile(a,b,size,reach):return [max(3290,min(3301,max(b[0]-reach,min(a[0],b[0]+size-1+reach)))),max(4243,min(4254,max(b[1]-reach,min(a[1],b[1]+size-1+reach))))]
accepted=[]
for r in x['witnesses']:
 t=r['tick'];w=r['west19'];events=primary.get(trials[r['seed']],fallback.get(trials[r['seed']],[]));history=[p for p in events if p[0]<t]
 last=history[-1] if history else None
 eye_target=[w['x']+(w['age']<9),w['y']];pos=attack_tile(r['position'],eye_target,2,8)
 if max(abs(pos[i]-r['position'][i]) for i in (0,1))>2 or r['nulled18']:continue
 reservation=w['born']+17 # earliest actual20 spawn at19+12, west20bow request+5
 if t+max(3,math.ceil(max(abs(pos[i]-[3293,4248][i]) for i in (0,1))/2))>reservation:continue
 at=t+3;move_from=t+1;route=[];valid=True;joint=True;known=[]
 for b in sorted(r['big18'],key=lambda c:['west','south','east'].index(c['lane'])):
  due=last is not None and last[1]==b['key'] and last[2]=='PIPE' and last[0]+2<=t
  if b['claim']>t or b['own']>=t and not due:valid=False;break
  if b['own']>=t:known.append(dict(key=b['key'],launch=last[0],normalImpact=last[0]+2,actualLease=b['own']))
  for shot in range(math.ceil(b['hp']/8)):
   target=[b['x'],b['y']];dest=attack_tile(pos,target,2,5)
   travel=math.ceil(max(abs(pos[i]-dest[i]) for i in (0,1))/2)
   at=max(at,move_from+max(0,travel-1))
   if at+2>b['born']+26 or at+1>=b['born']+26:valid=False;break
   returns=at+max(2,math.ceil(max(abs(dest[i]-[3293,4248][i]) for i in (0,1))/2))<=reservation
   joint=joint and returns
   route.append(dict(key=b['key'],request=at,impact=at+2,position=dest,fitsFutureWest20Bow=returns))
   pos=dest;move_from=at+1;at+=3
  if not valid:break
 if valid:accepted.append(dict(**r,lastConfirmedPrimary=last,knownPending=known,prospectiveRoute=route,futureWest20Bow=reservation,strictJointBudgetFits=joint))
result=dict(run=x['run'],scope='SOFT known-lastPIPE projection; real own/partner leases unchanged and full observed18HP reserved. Strict future20 reservation is separately assessed, not promised by variant.',softFrames=len(accepted),softRooms=len(set(r['seed'] for r in accepted)),strictJointFrames=sum(r['strictJointBudgetFits'] for r in accepted),witnesses=accepted)
source.with_name(source.stem+'-known-pipe.json').write_text(json.dumps(result,indent=2)+'\n')
print(run.name,{k:result[k] for k in ('softFrames','softRooms','strictJointFrames')})
