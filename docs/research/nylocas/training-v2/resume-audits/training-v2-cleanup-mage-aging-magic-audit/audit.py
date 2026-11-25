from pathlib import Path
from collections import Counter
import csv, json, hashlib

RUNS = ('training-v2-flight-mage-idle-pipe-100', 'training-v2-flight-mage-idle-pipe-validation-200')
ROTATIONS = {
    30: ('A',None,'ARMA','MRMA','RMRM',None),
    31: ('RMRM','ARAR','AMRM','MARM','RMRA','MRAR'),
}


def matches_next(crab, birth, age):
    if birth['lane']=='split':
        return True
    lane=birth['lane']
    index=(1 if birth['slot']=='N' else 0) if lane=='east' else (3 if birth['slot']=='E' else 2) if lane=='south' else (5 if birth['slot']=='N' else 4)
    rotation=ROTATIONS[crab['wave']][index]
    if rotation is None:
        return True
    phase=3 if age>=26 else 2 if age>=7 else 1 if age>=5 else 0
    phase_next=3 if age+1>=26 else 2 if age+1>=7 else 1 if age+1>=5 else 0
    current=rotation[min(phase,len(rotation)-1)]
    return current!='R' or rotation[min(phase_next,len(rotation)-1)]=='R'


def available(crab, previous, tick):
    old=previous.get(crab['key'],{})
    own=old.get('own',0)
    return not crab['dying'] and crab['hp']>0 and crab['age']<49 and old.get('claim',0)<=tick and not(own>0 and own>=tick)


def main():
    opportunities=[]
    totals=Counter()
    sources={}
    for name in RUNS:
        run=Path('.state/simulations')/name
        rows=list(csv.DictReader((run/'trials.csv').open()))
        trial=-1
        births={}
        nulls={}
        previous={}
        primaries={}
        ranger_points={}
        for line in (run/'server.log').open():
            f=line.strip(' \r\n').split('\t')
            if f[0]=='NYLO_PLAYERS'and f[1]=='0':
                trial+=1;births={};nulls={};previous={};primaries={};ranger_points={}
            if f[0]=='NYLO_PRIMARY':
                primaries.setdefault((int(f[1]),int(f[2])),[]).append(dict(key=int(f[3]),weapon=int(f[4])))
            if f[0]=='NYLO_NULL':
                nulls[int(f[1]),int(f[2])]=set(map(int,f[3].split(',')))
            if f[0]!='NYLO_TRACE':
                continue
            tick,role,wave=map(int,f[1:4])
            crabs={}
            for value in (f[11]if len(f)>11 else'').split(';'):
                if not value:continue
                key,bw,age,style,big,hp,x,y,claim,own,dying=value.split(',')
                key,x,y=int(key),int(x),int(y)
                crab=dict(key=key,wave=int(bw),age=int(age),style=style,big=big=='true',hp=int(hp),x=x,y=y,claim=int(claim),own=int(own),dying=dying=='true')
                crabs[key]=crab
                if key not in births:
                    lane='west'if x==3281 else'east'if x>=3309 else'south'if y==4233 else'split'
                    slot=('W'if x==3295 else'E')if lane=='south'else('N'if y==4249 else'S')if lane!='split'else''
                    births[key]=dict(crab,lane=lane,slot=slot)
            if role==0:
                ranger_points[tick]=tuple(map(int,f[6].split(',')))
                continue
            px,py,_=map(int,f[6].split(','))
            attacks=[a for a in primaries.get((tick,1),[])if a['weapon']in(29084,22325)]
            if wave==31 and attacks:
                totals['actualCleanupMeleePrimaries']+=len(attacks)
                ranges=[c for c in crabs.values()if c['wave']>=30 and c['style']=='RANGE'and c['key']not in nulls.get((tick,0),())and available(c,previous,tick)and matches_next(c,births[c['key']],c['age'])]
                magics=[]
                for c in crabs.values():
                    if c['wave']<30 or c['big']or births[c['key']]['lane']!='split'or c['style']!='MAGIC'or c['age']<10 or c['key']in nulls.get((tick,1),())or not available(c,previous,tick):continue
                    if c['key']in nulls.get((tick,0),()):continue # Existing +5000 rescue remains separate.
                    distance=max(abs(c['x']-px),abs(c['y']-py))
                    if distance>8:continue
                    old=previous.get(c['key'],{})
                    magics.append(dict(c,distance=distance,preDecisionClaim=old.get('claim',0),preDecisionOwn=old.get('own',0)))
                if ranges and magics:
                    for attack in attacks:
                        selected=crabs.get(attack['key'])
                        opportunities.append(dict(run=name,seed=int(rows[trial]['seed']),tick=tick,magePoint=(px,py),selected=selected,weapon=attack['weapon'],magics=magics,rangeWork=[dict(c,lane=births[c['key']]['lane'],preDecisionClaim=previous.get(c['key'],{}).get('claim',0),preDecisionOwn=previous.get(c['key'],{}).get('own',0))for c in ranges],rangerPoint=ranger_points.get(tick)))
            previous=crabs
        sources[name]=hashlib.sha256((run/'server.log').read_bytes()).hexdigest()
    result=dict(rooms=300,actualCleanupMeleePrimaries=totals['actualCleanupMeleePrimaries'],opportunityPrimaries=len(opportunities),opportunityRooms=len({(o['run'],o['seed'])for o in opportunities}),weaponCounts=dict(Counter(o['weapon']for o in opportunities)),sourceLogSha256=sources,opportunities=opportunities,semantics='Actual NYLO_PRIMARY mage sulphur/scythe launch at31. Alternative magic is birth>=30 SPLIT small age10..48, currentHP>0/notdying/notMageNULL, notRangerNULL(existing rescue separate), geometricEye8. Fresh RANGE work is currentrange/notRangerNULL/lifetime-valid and matches next style. OWN and PARTNER lease exclusions read PRIOR Mage snapshot(postpreviouscycle), while HP/style/position/NULL read current planning snapshot. Range target position/LOS feasibility for ranger is not guaranteed; no new model field. Current native food/LOS and full candidate scoring/timing must be validated in simulation.')
    Path(__file__).with_name('opportunities.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({k:v for k,v in result.items()if k!='opportunities'},indent=2))
    print(json.dumps(opportunities[:2],indent=2))


if __name__=='__main__':
    main()
