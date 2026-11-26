"""Find last-cooldown movement that can keep the selected small covered while staging West9."""
import collections
import json
from pathlib import Path
root=Path(__file__).parent
for name in ['newer-cleanup-200','newer-cleanup-300','named-single-chins-200','east7-forecast-v3-200','east7-opening-v3-200']:
 data=json.loads((root/(name+'-west9.json')).read_text());found=[]
 for room in data['detail']:
  ready=-1
  for f in room['frames']:
   if f['attack']:ready=f['tick']+dict(PIPE=2,EYE=3,CHIN=3,CLAWS=4,BOW=5)[f['weapon']]
   if f['age9'] is None or not 0<=f['age9']<=6 or f['ready'] or ready!=f['tick']+1:continue
   target=next((c for c in f['crabs']if c['key']==f['target']),None)
   parent=next((c for c in f['crabs']if c['key']==room['parent']),None)
   if not target or target['big'] or target['style'] not in['RANGE','MAGIC'] or not parent or parent['hp']<=0 or parent['dying'] or parent['ranger_null'] or room['parent_first']['tick']<=f['tick']:continue
   if target['ranger_null'] or target['hp']<=0 or target['dying'] or not target['forecast'] or target['forecast'][1]>=target['hp']:continue
   player=list(map(int,f['player'].split(',')));stage=[max(3292,player[0]-2),max(4247,min(4249,player[1]))]
   weapon_range=dict(PIPE=5,EYE=8).get(f['weapon'],0)
   if stage[0]>=player[0] or max(abs(stage[0]-player[0]),abs(stage[1]-player[1]))>2 or max(abs(stage[0]-target['x']),abs(stage[1]-target['y']))>weapon_range-1:continue
   found.append(dict(seed=room['seed'],tick=f['tick'],age=f['age9'],player=player,stage=stage,weapon=f['weapon'],target=target,first=room['parent_first']))
 print(name,len(found),'rooms',len({f['seed']for f in found}),'firstages',dict(collections.Counter(f['first']['age']for f in found)))
 (root/(name+'-west9-stage-screen.json')).write_text(json.dumps(found,indent=2)+'\n')
