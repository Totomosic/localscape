"""Find Ranger literal idle pre-birth frames for known12/15 South natural splits."""
import csv
import json
from pathlib import Path
root=Path(__file__).parent
for name in ['newer-cleanup-200','newer-cleanup-300','fixed24-south15-risk-200']:
    data=json.loads((root/(name+'-old-parent-service.json')).read_text())
    wanted={}
    for room in data['rooms']:
        for key,parent in room['parents'].items():
            if parent['tag'] not in ['12:south','15:south'] or parent['removed'] is None or parent['removed']-parent['born']!=55 or parent['first_zero'] is not None:continue
            wanted.setdefault(room['seed'],[]).append(dict(parent=int(key),tag=parent['tag'],birth=parent['removed'],spawns=room['spawns'],children={k:c for k,c in room['children'].items()if c['parent']==int(key)},frames=[]))
    run=root.parent/('training-v5-20261126-'+name);rows=list(csv.DictReader((run/'trials.csv').open()));trial=-1;seed=None;forecast={}
    for line in(run/'server.log').open():
        a=line.lstrip().rstrip('\r\n').split('\t')
        if a[0]=='NYLO_TRACE' and a[1:3]==['0','0']:
            trial+=1;seed=int(rows[trial]['seed']);forecast={}
        if seed not in wanted:continue
        if a[0]=='NYLO_FORECAST':forecast[int(a[1]),int(a[2])]=list(map(int,a[3:5]))
        if a[0]!='NYLO_TRACE' or a[2]!='0':continue
        tick=int(a[1])
        for target in wanted[seed]:
            if not target['birth']-2<=tick<=target['birth']+3:continue
            crabs=[]
            for raw in a[11].split(';'):
                if not raw:continue
                c=raw.split(',');key=int(c[0])
                if key==target['parent']or str(key)in target['children']:crabs.append(dict(key=key,wave=int(c[1]),age=int(c[2]),style=c[3],big=c[4]=='true',hp=int(c[5]),x=int(c[6]),y=int(c[7]),forecast=forecast.get((tick,key))))
            target['frames'].append(dict(tick=tick,wave=int(a[3]),ready=a[4]=='true',attack=a[5]=='true',player=a[6],weapon=a[7],selected=int(a[8]),reason=a[10],crabs=crabs))
    found=[]
    for seed,targets in wanted.items():
        for target in targets:
            prior=[f for f in target['frames']if f['tick']<target['birth']and f['wave']<20and not f['attack']and f['selected']==-1]
            birth=next((f for f in target['frames']if f['tick']==target['birth']),None)
            if prior and birth:found.append(dict(seed=seed,**target))
    (root/(name+'-natural-child-stage-witnesses.json')).write_text(json.dumps(found,indent=2)+'\n')
    print(name,len(found))
    for q in found[:3]:print(q['seed'],q['tag'],q['birth'],[(f['tick'],f['ready'],f['attack'],f['player'],f['reason'])for f in q['frames']])
