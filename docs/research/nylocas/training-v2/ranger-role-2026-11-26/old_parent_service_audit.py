"""Trace native12South/13East/15South parent service and child physical removals."""
import argparse
import collections
import csv
import hashlib
import json
from pathlib import Path

def audit(run):
    rows=list(csv.DictReader((run/'trials.csv').open()))
    rooms=[]; room=None; previous={}; origins={}; forecast={}; nulls={}
    for line in (run/'server.log').open():
        a=line.lstrip().rstrip('\r\n').split('\t')
        if a[0]=='NYLO_TRACE' and a[1:3]==['0','0']:
            room=dict(seed=int(rows[len(rooms)]['seed']),failed19=int(rows[len(rooms)]['stalls_by_last_spawned_wave'].split(';')[19]),spawns={},parents={},children={},snapshots={})
            rooms.append(room);previous={};origins={};forecast={};nulls={}
        if room is None:continue
        if a[0]=='NYLO_FORECAST':forecast[int(a[1]),int(a[2])]=list(map(int,a[3:5]))
        elif a[0]=='NYLO_NULL':nulls[int(a[1]),int(a[2])]=set(map(int,a[3].split(','))) if len(a)>3 and a[3] else set()
        elif a[0]=='NYLO_TRACE':
            tick,role,wave=map(int,a[1:4]);crabs={}
            if role==0:room['spawns'].setdefault(wave,tick)
            for raw in a[11].split(';'):
                if not raw:continue
                c=raw.split(',');key,birth,age=map(int,c[:3]);x,y=map(int,c[6:8]);origins.setdefault(key,'west' if x==3281 else 'east' if x in[3309,3310] else 'south' if y==4233 else 'split')
                crab=dict(key=key,wave=birth,born=tick-age,age=age,lane=origins[key],big=c[4]=='true',style=c[3],hp=int(c[5]),x=x,y=y,dying=c[10]=='true',forecast=forecast.get((tick,key)),ranger_null=key in nulls.get((tick,0),set()),mage_null=key in nulls.get((tick,1),set()))
                crabs[key]=crab
                tag = str(birth)+':'+origins[key]
                if crab['big'] and tag in ['12:south','13:east','15:south']:
                    p=room['parents'].setdefault(key,dict(tag=tag,born=tick-age,first_zero=None,removed=None,shots=[],null_ticks=[0,0],nearby_ranger=[],mage_frames=[]))
                    if role==0:
                        if crab['hp']<=0 or crab['dying']:p['first_zero']=p['first_zero'] if p['first_zero'] is not None else tick
                        p['null_ticks'][0]+=crab['ranger_null'];p['null_ticks'][1]+=crab['mage_null']
                        if tag=='13:east' and 29<=age<=48 and crab['hp']>0 and not crab['dying']:
                            xy=list(map(int,a[6].split(',')));distance=max(x-xy[0],xy[0]-x-1,y-xy[1],xy[1]-y-1)
                            if distance<=3:p['nearby_ranger'].append(dict(tick=tick,age=age,distance=distance,ready=a[4]=='true',attack=a[5]=='true',weapon=a[7],selected=int(a[8]),reason=a[10],ranger_xy=xy,parent=crab))
                    if a[5]=='true' and int(a[8])==key:p['shots'].append(dict(tick=tick,age=age,role=role,weapon=a[7],hp=crab['hp'],forecast=crab['forecast'],xy=a[6]))
                    if role==1 and tag=='13:east' and 29<=age<=48 and crab['hp']>0 and not crab['dying']:
                        p['mage_frames'].append(dict(tick=tick,age=age,ready=a[4]=='true',attack=a[5]=='true',weapon=a[7],selected=int(a[8]),reason=a[10],parent=crab))
            if role!=0:continue
            # Track children only when the removed tracked parent's last squares uniquely identify them.
            newborn=[c for k,c in crabs.items() if k not in previous and c['lane']=='split' and not c['big'] and c['born']==tick]
            for key,p in room['parents'].items():
                if key in previous and key not in crabs:
                    p['removed']=tick
                    old=previous[key]
                    for offset in[0,1]:
                        matches=[c for c in newborn if c['x']==old['x']+offset and c['y']==old['y']+offset]
                        if len(matches)==1:
                            child=matches[0];room['children'][child['key']]=dict(parent=key,parent_tag=p['tag'],born=tick,birth_wave=child['wave'],style=child['style'],first_zero=None,removed=None)
            for key,p in room['children'].items():
                if key in crabs and (crabs[key]['hp']<=0 or crabs[key]['dying']):p['first_zero']=p['first_zero'] if p['first_zero'] is not None else tick
                if key in previous and key not in crabs:p['removed']=tick
            if wave==19 and str(19)not in room['snapshots']:room['snapshots']['19']=dict(tick=tick,children={k:c for k,c in crabs.items() if k in room['children']})
            if 19 in room['spawns'] and tick==room['spawns'][19]+12:room['snapshots']['20check']=dict(tick=tick,children={k:c for k,c in crabs.items() if k in room['children']})
            previous=crabs
    summary={}
    for tag in ['12:south','13:east','15:south']:
        parents=[(q,p) for q in rooms for p in q['parents'].values() if p['tag']==tag]
        counts=collections.Counter();first=collections.Counter()
        for q,p in parents:
            natural=p['removed'] is not None and p['removed']-p['born']==55 and p['first_zero'] is None
            counts['parents']+=1;counts['natural55' if natural else 'combat_or_other']+=1
            counts['mage_null_parents']+=p['null_ticks'][1]>0
            counts['no_primary_shot']+=not p['shots']
            if natural:counts['natural55_mage_null']+=p['null_ticks'][1]>0
            if p['shots']:first[str(p['shots'][0]['age'])+':'+str(p['shots'][0]['role'])+':'+p['shots'][0]['weapon']]+=1
        children=[(q,c) for q in rooms for c in q['children'].values() if c['parent_tag']==tag]
        counts['children']=len(children)
        for q,c in children:
            for k in ['19','20check']:
                if k not in q['snapshots']:continue
                counts['children_present_at_'+k]+=c['born']<=q['snapshots'][k]['tick'] and (c['removed'] is None or c['removed']>q['snapshots'][k]['tick'])
        summary[tag]=dict(counts=dict(counts),first_requests=dict(first))
    return dict(run=str(run),summary=summary,rooms=rooms,caveat=__doc__,sources={str(run/p):hashlib.sha256((run/p).read_bytes()).hexdigest()for p in['trials.csv','server.log']})
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();d=audit(a.run);a.output.write_text(json.dumps(d,indent=2)+'\n');print(json.dumps(d['summary'],indent=2))
