#!/usr/bin/env python3
"""Find observed recent ranged split clusters alongside unfinished18 bigs.

Geometry and claim/NULL observations only; native LOS and future damage are not inferred.
The budget reserves3 chin cooldown,2 cycles per ceil(HP/8) pipe work and2 impact buffer.
It is an optimistic timing screen before travel; work is not guaranteed native damage.
"""
from pathlib import Path
from collections import Counter
import csv,json,math,sys,hashlib
run=Path(sys.argv[1]);output=Path(sys.argv[2])
rows=list(csv.DictReader((run/'trials.csv').open()))
trial=-1;births={};nulled={};records=[];wave_ticks={}
for raw in (run/'server.log').open():
    line=raw.lstrip(' ').rstrip('\n')
    if line.startswith('NYLO_NULL\t'):
        _,tick,role,keys=line.split('\t');nulled[int(tick),int(role)]=set(map(int,keys.split(','))) if keys else set();continue
    if not line.startswith('NYLO_TRACE\t'):continue
    f=line.split('\t');tick,role,wave=map(int,f[1:4])
    if tick==0 and role==0:trial+=1;births={};nulled={};wave_ticks={}
    wave_ticks.setdefault(wave,tick)
    cs={}
    for item in f[11].split(';'):
        if not item:continue
        z=item.split(',');key=int(z[0]);d=dict(key=key,w=int(z[1]),born=tick-int(z[2]),age=int(z[2]),style=z[3],big=z[4]=='true',hp=int(z[5]),x=int(z[6]),y=int(z[7]),claim=int(z[8]),own=int(z[9]),dead=z[10]=='true')
        births.setdefault(key,'west' if d['x']==3281 else 'east' if d['x']>=3309 else 'south' if d['y']==4233 else 'split');d['lane']=births[key];cs[key]=d
    if role!=0 or wave not in (18,19) or f[4]!='true':continue
    bigs=[c for c in cs.values() if c['big'] and c['w']==18 and c['hp']>0 and not c['dead']]
    if not bigs:continue
    px,py,_=map(int,f[6].split(','));null=nulled.get((tick,0),set())
    smalls=[c for c in cs.values() if not c['big'] and c['w']>=17 and c['age']<18 and c['hp']>0 and not c['dead'] and c['style']=='RANGE' and c['claim']<=tick and c['own']<tick and c['key'] not in null and c['lane']=='split']
    clusters=[]
    for c in smalls:
        vs=[v for v in smalls if max(abs(v['x']-c['x']),abs(v['y']-c['y']))<=1]
        if len(vs)>=2 and max(abs(c['x']-px),abs(c['y']-py))<=11:clusters.append([v['key'] for v in vs])
    if not clusters:continue
    budget=2+2*sum(math.ceil(c['hp']/8) for c in bigs)
    remaining=min(c['born']+26-tick for c in bigs)
    records.append(dict(seed=int(rows[trial]['seed']),tick=tick,wave=wave,wave19age=None if 19 not in wave_ticks else tick-wave_ticks[19],population=len(cs),big18=bigs,clusters=clusters,launch=f[5],weapon=f[7],target=int(f[8]),decision=f[10],rawPipeBudget=budget,remainingBefore26=remaining,fitsRawBudget=3+budget<=remaining))
result=dict(run=str(run),semantics=__doc__,frames=len(records),rooms=len(set(r['seed'] for r in records)),rawBudgetFrames=sum(r['fitsRawBudget'] for r in records),rawBudgetRooms=len(set(r['seed'] for r in records if r['fitsRawBudget'])),witnesses=records,sourceLogSha256=hashlib.sha256((run/'server.log').read_bytes()).hexdigest())
output.write_text(json.dumps(result,indent=2)+'\n')
print(run.name,{k:result[k] for k in ('frames','rooms','rawBudgetFrames','rawBudgetRooms')})
for r in records:
    if r['fitsRawBudget']:print('fits',r['seed'],r['tick'],'wave',r['wave'],'w19age',r['wave19age'],'pop',r['population'],'budget',r['rawPipeBudget'],'left',r['remainingBefore26'],r['decision'])
