from pathlib import Path
from collections import Counter
import hashlib
import json

run=Path('.state/simulations/training-v2-flight-fix-fresh-100')
trial=-1
origins={}
nulls={}
observations=[]
for line in (run/'server.log').open():
    fields=line.strip().split('\t')
    if fields[0]=='NYLO_PLAYERS' and fields[1]=='0':
        trial+=1
        origins={}
        nulls={}
    if fields[0]=='NYLO_NULL':
        nulls[int(fields[1]),int(fields[2])]=set(fields[3].split(','))
    if fields[0]!='NYLO_TRACE' or len(fields)<12:
        continue
    tick,role,wave=map(int,fields[1:4])
    crabs={c.split(',')[0]:c.split(',') for c in fields[11].split(';') if c}
    for key,crab in crabs.items():
        if key not in origins:
            x,y=map(int,crab[6:8])
            origins[key]='west' if x<3285 else 'east' if x>3305 else 'south' if y<4237 else 'split'
    if role!=1 or wave>=20 or fields[5]!='true' or fields[10]!='Fresh MELEE small':
        continue
    chosen=crabs.get(fields[8])
    if chosen is None or int(chosen[2])<7:
        continue
    x,y,_=map(int,fields[6].split(','))
    for key,crab in crabs.items():
        if key in nulls.get((tick,1),()) or origins[key]!='split' or crab[3]!='MAGIC' or crab[4]!='false':
            continue
        age,hp,claim,own=int(crab[2]),int(crab[5]),int(crab[8]),int(crab[9])
        if age>3 or hp<=0 or crab[10]!='false' or claim>tick or own>=tick and own>0:
            continue
        if max(abs(int(crab[6])-x),abs(int(crab[7])-y))>8:
            continue
        observations.append(dict(seed=131+trial,tick=tick,wave=wave,selectedKey=fields[8],
            selectedAge=int(chosen[2]),freshKey=key,freshAge=age,weapon=fields[7]))
        break
result=dict(run=str(run),rooms=trial+1,opportunitySnapshots=len(observations),
    roomsWithOpportunity=len({r['seed'] for r in observations}),
    byWave=dict(sorted(Counter(r['wave'] for r in observations).items())),observations=observations,
    semantics='Post-cycle trace screen, not full counterfactual feasibility: selected ordinary melee small age>=7; unclaimed, non-NULL, non-dying magic split age<=3 within eight tiles. First-observed coordinates infer split identity. LOS, candidate staging, intent and full return budget are not reconstructed; opportunities are hypotheses, not proven lost ticks.',
    serverLogSha256=hashlib.sha256((run/'server.log').read_bytes()).hexdigest())
out=Path(__file__).with_name('fresh-baseline-opportunities.json')
out.write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps({k:v for k,v in result.items() if k!='observations'},indent=2))
print(json.dumps(observations[:8],indent=2))
