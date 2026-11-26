"""Measure actual Ranger natural-stage movement, child requests, and first19 removals.

Birth-square matches are diagnostic; simultaneous overlapping splits are flagged.
"""
import argparse,csv,json,re,collections
from pathlib import Path

def audit(run):
 rows=list(csv.DictReader((run/'trials.csv').open()));rooms=[];room=None;previous={};parents={};forecast={}
 for line in(run/'server.log').open():
  a=line.lstrip().rstrip('\r\n').split('\t')
  if a[0]=='NYLO_TRACE' and a[1:3]==['0','0']:
   room=dict(seed=int(rows[len(rooms)]['seed']),spawns={},stages={},children={},attacks=[],positions={});rooms.append(room);previous={};parents={};forecast={}
  if room is None:continue
  if a[0]=='NYLO_FORECAST':forecast[int(a[1]),int(a[2])]=list(map(int,a[3:5]))
  elif a[0]=='NYLO_TRACE':
   tick,role,wave=map(int,a[1:4]);crabs={}
   if role==0:room['spawns'].setdefault(wave,tick);room['positions'][tick]=a[6]
   for raw in a[11].split(';'):
    if not raw:continue
    c=raw.split(',');key,birth,age=map(int,c[:3]);x,y=map(int,c[6:8]);crabs[key]=dict(key=key,wave=birth,born=tick-age,big=c[4]=='true',style=c[3],hp=int(c[5]),x=x,y=y,dying=c[10]=='true',forecast=forecast.get((tick,key)))
    if role==0 and c[4]=='true' and birth in[12,15] and tick-age+55>=tick:parents[key]=dict(wave=birth,born=tick-age)
   if a[5]=='true':room['attacks'].append(dict(tick=tick,role=role,key=int(a[8]),weapon=a[7],reason=a[10]))
   if role!=0:continue
   match=re.fullmatch(r'Prepare natural South(12|15) children @(\d+)',a[10])
   if match:
    parentwave,birth=map(int,match.groups());entry=room['stages'].setdefault(birth,dict(parent_wave=parentwave,birth=birth,frames=[],children=[],ambiguous=False))
    entry['frames'].append(dict(tick=tick,ready=a[4]=='true',player=a[6]))
   for birth,entry in room['stages'].items():
    if tick==birth:
     children=[c for key,c in crabs.items() if key not in previous and not c['big'] and c['born']==tick and(c['x'],c['y'])in[(3292,4243),(3293,4244)]]
     entry['children']=[c['key']for c in children];entry['at_birth_position']=a[6];entry['ambiguous']=len(children)!=2
     for child in children:room['children'][child['key']]=dict(born=tick,wave=child['wave'],style=child['style'],first_zero=None,removed=None)
   for key,c in room['children'].items():
    if key in crabs and(crabs[key]['hp']<=0 or crabs[key]['dying'])and c['first_zero']is None:c['first_zero']=tick
    if key in previous and key not in crabs:c['removed']=tick
   previous=crabs
 counts=collections.Counter();cases=[]
 for room in rooms:
  for birth,entry in room['stages'].items():
   counts['stage_events']+=1;counts['stage_frames']+=len(entry['frames']);counts['South'+str(entry['parent_wave'])+'_events']+=1;counts['ambiguous_events']+=entry['ambiguous'];birthshots=[]
   for key in entry['children']:
    child=room['children'][key];requests=[a for a in room['attacks']if a['key']==key]
    first=next((a for a in requests if a['role']==0),None);counts['children']+=1
    if first:counts['ranger_child_requests']+=1;counts['ranger_request_birth_plus_'+str(first['tick']-birth)]+=1
    first19=room['spawns'].get(19);first20=room['spawns'].get(20)
    if first19 is not None:counts['children_removed_before_first19']+=child['removed']is not None and child['removed']<=first19
    birthshots.append(dict(key=key,**child,requests=requests))
   cases.append(dict(seed=room['seed'],spawns=room['spawns'],**entry,outcomes=birthshots))
 counts['rooms_with_stage']=sum(bool(q['stages'])for q in rooms)
 return dict(run=str(run),counts=dict(counts),cases=cases,caveat=__doc__)
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();d=audit(a.run);a.output.write_text(json.dumps(d,indent=2)+'\n');print(json.dumps(d['counts'],indent=2))
