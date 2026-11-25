#!/usr/bin/env python3
"""Audit first-hit-resolved West19 follow-up windows without forecasting hit outcomes."""
from pathlib import Path
import csv,json,math,hashlib,sys
run=Path(sys.argv[1]);out=Path(sys.argv[2]); rows=list(csv.DictReader((run/'trials.csv').open()))
trial=-1;births={};opened=set();pending=None;frames=[];nulled={}
def parse_snapshot(text,tick):
    result={}
    for item in text.split(';'):
        if not item:continue
        z=item.split(',');key=int(z[0]);d=dict(key=key,w=int(z[1]),born=tick-int(z[2]),age=int(z[2]),style=z[3],big=z[4]=='true',hp=int(z[5]),x=int(z[6]),y=int(z[7]),claim=int(z[8]),own=int(z[9]),dead=z[10]=='true')
        births.setdefault(key,'west' if d['x']==3281 else 'east' if d['x']>=3309 else 'south' if d['y']==4233 else 'split');d['lane']=births[key];result[key]=d
    return result
for raw in (run/'server.log').open():
    line=raw.lstrip(' ').rstrip('\n')
    if line.startswith('NYLO_NULL\t'):
        _,tick,role,keys=line.split('\t');nulled[int(tick),int(role)]=set(map(int,keys.split(','))) if keys else set();continue
    if not line.startswith('NYLO_TRACE\t'):continue
    f=line.split('\t');tick,role,wave=map(int,f[1:4])
    if tick==0 and role==0:trial+=1;births={};opened=set();nulled={};pending=None
    if role==0:
        cs=parse_snapshot(f[11],tick)
        if wave==19 and f[4]=='true':
            target=next((c for c in cs.values() if c['w']==19 and c['big'] and c['lane']=='west'),None)
            if target and 3<target['age']<=10 and not target['dead'] and target['hp']>0 and target['style']=='MAGIC' and target['own']<tick and target['key'] in opened and target['key'] not in nulled.get((tick,0),set()):
                px,py,_=map(int,f[6].split(','));tx=target['x']+(target['age']<9);ty=target['y'];dist=max(tx-px,px-tx-1,ty-py,py-ty-1)
                bigs=[c for c in cs.values() if c['w']==18 and c['big'] and not c['dead'] and c['hp']>0]
                required=2+2*sum(math.ceil(c['hp']/8) for c in bigs)
                remaining=min((c['born']+26-tick for c in bigs),default=99)
                pending=dict(seed=int(rows[trial]['seed']),tick=tick,west19=target,westDistanceAfterOneNativeStep=max(0,dist-2),big18=bigs,rawBudget=required,remaining=remaining,fitsRawBudget=3+required<=remaining,current=f[7:11],currentAttacked=f[5]=='true',position=[px,py],nulled18=[c['key'] for c in bigs if c['key'] in nulled.get((tick,0),set())],claimed=target['claim']>tick,withinEyeRange=dist<=10)
        if f[5]=='true':opened.add(int(f[8]))
    elif role==1 and pending and pending['tick']==tick:
        pending['mageShootsWestNow']=f[5]=='true' and int(f[8])==pending['west19']['key']
        if pending['withinEyeRange'] and (not pending['claimed'] or pending['mageShootsWestNow']):frames.append(pending)
        pending=None
result=dict(run=str(run),semantics='Observed ranger opening confirmed + own-flight released during age<=10. A same-tick mage launch claim is ignored only for the counterfactual protected handoff. Raw26 budget reserves actual observed HP work; no pending-hit kills assumed, no travel yet.',frames=len(frames),rooms=len(set(f['seed'] for f in frames)),budgetFrames=sum(f['fitsRawBudget'] for f in frames),budgetRooms=len(set(f['seed'] for f in frames if f['fitsRawBudget'])),witnesses=frames,sourceLogSha256=hashlib.sha256((run/'server.log').read_bytes()).hexdigest())
out.write_text(json.dumps(result,indent=2)+'\n')
print(run.name,{k:result[k] for k in ('frames','rooms','budgetFrames','budgetRooms')})
for r in frames:
    if r['fitsRawBudget']:print('fits',r['seed'],r['tick'],'age',r['west19']['age'],'hp',r['west19']['hp'],'18',[(b['lane'],b['hp'],b['age']) for b in r['big18']],'budget',r['rawBudget'],'left',r['remaining'],'mageNow',r['mageShootsWestNow'],r['current'])
