from pathlib import Path
from collections import Counter
import csv, json, hashlib

run=Path('.state/simulations/training-v2-flight-ranger-guidance-100')
rows=list(csv.DictReader((run/'trials.csv').open()))
trial=-1
origins={}
nulls={}
spawns={}
delays={}
observations=[]
for line in (run/'server.log').open():
    f=line.strip(' \r\n').split('\t')
    if f[0]=='NYLO_PLAYERS' and f[1]=='0':
        trial+=1
        origins={}
        nulls={}
        spawns={}
    if f[0]=='NYLO_NULL':
        nulls[int(f[1]),int(f[2])]=set(f[3].split(','))
    if f[0]=='NYLO_ENGINE':
        delays[trial,int(f[1])]=int(f[2].split(',')[1])
    if f[0]!='NYLO_TRACE':
        continue
    tick,role,wave=map(int,f[1:4])
    spawns.setdefault(wave,tick)
    crabs={c.split(',')[0]:c.split(',') for c in (f[11] if len(f)>11 else '').split(';') if c}
    for key,c in crabs.items():
        if key not in origins:
            x,y=map(int,c[6:8])
            origins[key]='west' if x==3281 else 'east' if x>=3309 else 'south' if y==4233 else 'split'
    if role!=1 or not 1<=wave<20 or f[4]!='true' or f[5]!='false' or f[8]!='-1':
        continue
    reason=f[10]
    if reason.startswith(('Opening ','Prepare Deferred')):
        continue
    px,py,_=map(int,f[6].split(','))
    packet=None
    due=None
    stage=None
    if reason=='Reserve East barrage 11':
        packet='East barrage 11'
        stage=(3301,4249)
        due=spawns.get(11)
        if due is None:
            due=spawns[10]+8
            if due<tick:
                due+=((tick-due+3)//4)*4
    elif reason=='Reserve 12 east scythe':
        packet='12 east scythe'
        stage=(3301,4249)
        spawn12=spawns.get(12)
        if spawn12 is None:
            spawn12=spawns[11]+8
            if spawn12<tick:
                spawn12+=((tick-spawn12+3)//4)*4
        due=spawn12+7
    elif reason.startswith('Reserve '):
        # Keep unusual missing/NULL packet cases out of the screen rather than guess.
        continue
    fits=True if due is None else tick+max(2,(max(abs(px-stage[0]),abs(py-stage[1]))+1)//2)<=due
    for key,c in crabs.items():
        if origins[key]!='split' or c[3]!='RANGE' or c[4]!='false' or int(c[5])<=0 or c[10]!='false':
            continue
        if key in nulls.get((tick,1),()) or int(c[8])>tick or int(c[9])>=tick and int(c[9])>0:
            continue
        age=int(c[2])
        distance=max(abs(int(c[6])-px),abs(int(c[7])-py))
        if age>4 or distance>5 or key in nulls.get((tick,0),()):
            continue
        observations.append(dict(trial=trial,seed=int(rows[trial]['seed']),tick=tick,wave=wave,
            key=int(key),age=age,hp=int(c[5]),distance=distance,reason=reason,magePoint=(px,py),
            crabPoint=tuple(map(int,c[6:8])),packet=packet,packetDue=due,returnFits=fits))
ready=[dict(o,nativeDelay=delays[o['trial'],o['tick']]) for o in observations if delays[o['trial'],o['tick']]<=0]
fit=[o for o in ready if o['returnFits']]
result=dict(run=str(run),rooms=len(rows),nativeReadyFreshTargetObservations=len(ready),
    nativeReadyFreshFrames=len({(o['seed'],o['tick']) for o in ready}),
    nativeReadyFreshRooms=len({o['seed'] for o in ready}),
    conservativeReturnTargetObservations=len(fit),conservativeReturnFrames=len({(o['seed'],o['tick']) for o in fit}),
    conservativeReturnRooms=len({o['seed'] for o in fit}),
    byIdleReason=dict(Counter(o['reason'] for o in fit)),opportunities=fit,
    sourceLogSha256=hashlib.sha256((run/'server.log').read_bytes()).hexdigest(),
    semantics='Screen uses targetless normal-policy staging, policy-ready and native action delay<=0, actual1..19, RANGE SPLIT small age0..4, positiveHP/non-dying, neither-role NULL, no ownflight/partnerclaim, distance<=5. Mandatory packet due and conservative current-position return are reconstructed for East11/Scythe12 staging. LOS is not exposed by trace; immediate native feasibility and food blocking must be validated in the candidate run. Target observations can share one frame and are not independent saved attacks.')
Path(__file__).with_name('opportunities.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps({k:v for k,v in result.items() if k!='opportunities'},indent=2))
print(json.dumps(fit[:12],indent=2))
