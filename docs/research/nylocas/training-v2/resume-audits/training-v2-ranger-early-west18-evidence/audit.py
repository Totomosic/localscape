#!/usr/bin/env python3
"""Compare first West18 service and child birth timing without pooling overlapping corpora."""
from pathlib import Path
from collections import Counter
import csv,gzip,hashlib,json,statistics
ROOT=Path('.');VALID={r['uuid'] for r in json.loads((ROOT/'docs/research/nylocas/vetting/summary.json').read_text())['eligiblePinnedRecordings']}
def summarize(rows):
 def stats(key):
  values=[r[key] for r in rows if r.get(key) is not None]
  return dict(n=len(values),mean=statistics.mean(values) if values else None,median=statistics.median(values) if values else None,histogram=dict(sorted(Counter(values).items())))
 return dict(rooms=len(rows),firstRequestAge=stats('firstRequestAge'),splitRelative19=stats('splitRelative19'),splitRelative18=stats('splitRelative18'),firstWeapons=dict(Counter(r.get('weapon') for r in rows)),perRoom=rows)
result={'semantics':'Human request ages = recorded launch age minus1, as role replay convention. Native request ages are controller request ticks. Native child attribution requires exact unambiguous SW/NE pair on West18 parent disappearance; no parent inference from generic split birth wave. Corpora overlap and remain separate.','human':{},'native':{}}
for corpus in ('ranger','mage'):
 root=ROOT/'docs/research/nylocas'/corpus
 manifest=json.loads((root/'manifest.json').read_text())
 attacks=[r for r in csv.DictReader((root/'attacks.csv').open()) if r['uuid'] in VALID and r['role']=='ranger' and r['target_wave']=='18' and r['lane']=='west' and r['big']=='True']
 first={}
 for r in attacks:
  if r['uuid'] not in first or int(r['tick'])<int(first[r['uuid']]['tick']):first[r['uuid']]=r
 rows=[]
 for rec in manifest['recordings']:
  uid=rec['uuid']
  if uid not in first:continue
  entry=rec['files']['raid'];raw=gzip.decompress((root/entry['path']).read_bytes());assert hashlib.sha256(raw).hexdigest()==entry['sha256']
  raid=json.loads(raw);npcs=raid['tobRooms']['nylocas']['npcs']
  west=next((n for n in npcs.values() if n.get('nylo',{}).get('wave')==18 and n['nylo'].get('big') and n['nylo'].get('spawnType')==1),None)
  assert west is not None
  w19=min(n['spawnTick'] for n in npcs.values() if n.get('nylo',{}).get('wave')==19 and not n['nylo'].get('parentRoomId'))
  children=[n for n in npcs.values() if n.get('nylo',{}).get('parentRoomId')==west['roomId']]
  birth=min((n['spawnTick'] for n in children),default=None)
  a=first[uid];rows.append(dict(uuid=uid,firstRequestAge=int(a['age'])-1,weapon=a['weapon'],splitRelative19=None if birth is None else birth-w19,splitRelative18=None if birth is None else birth-west['spawnTick'],children=len(children)))
 result['human'][corpus]=summarize(rows)
for dirname in ('training-v2-flight-mage-idle-pipe-100','training-v2-flight-mage-idle-pipe-validation-200'):
 run=ROOT/'.state/simulations'/dirname;csvrows=list(csv.DictReader((run/'trials.csv').open()));trial=-1;current=None;previous={};births={};rows=[]
 for raw in (run/'server.log').open():
  line=raw.lstrip(' ').rstrip('\n')
  if not line.startswith('NYLO_TRACE\t'):continue
  f=line.split('\t');tick,role,wave=map(int,f[1:4])
  if role:continue
  if tick==0:
   trial+=1;current=dict(seed=int(csvrows[trial]['seed']),firstRequestAge=None,weapon=None,splitRelative19=None,splitRelative18=None);rows.append(current);previous={};births={}
  current.setdefault('wave'+str(wave),tick)
  crabs={}
  for item in f[11].split(';'):
   if not item:continue
   z=item.split(',');key=int(z[0]);c=dict(key=key,wave=int(z[1]),born=tick-int(z[2]),big=z[4]=='true',hp=int(z[5]),x=int(z[6]),y=int(z[7]),dead=z[10]=='true')
   births.setdefault(key,dict(c,lane='west' if c['x']==3281 else 'east' if c['x']>=3309 else 'south' if c['y']==4233 else 'split'));c['lane']=births[key]['lane'];crabs[key]=c
   if c['wave']==18 and c['big'] and c['lane']=='west':current.setdefault('west18Key',key);current.setdefault('west18Born',c['born'])
  key=current.get('west18Key')
  if f[5]=='true' and int(f[8])==key and current['firstRequestAge'] is None:
   current['firstRequestAge']=tick-current['west18Born'];current['weapon']=f[7];current['firstTick']=tick;current['firstActualWave']=wave
  if key in previous and key not in crabs and 'removed' not in current:
   parent=previous[key];current['removed']=tick;current['parentPoint']=[parent['x'],parent['y']]
   new=[c for c in crabs.values() if not c['big'] and c['born']==tick]
   sw=[c for c in new if [c['x'],c['y']]==[parent['x'],parent['y']]]
   ne=[c for c in new if [c['x'],c['y']]==[parent['x']+1,parent['y']+1]]
   otherParents=[c for k,c in previous.items() if k!=key and k not in crabs and c['big'] and [c['x'],c['y']]==[parent['x'],parent['y']]]
   if len(sw)==1 and len(ne)==1 and not otherParents:
    current['splitBirth']=tick;current['splitKeys']=[sw[0]['key'],ne[0]['key']];current['splitRelative18']=tick-current['west18Born']
  previous=crabs
 for r in rows:
  if 'splitBirth' in r:r['splitRelative19']=r['splitBirth']-r['wave19']
 result['native'][dirname]=summarize(rows)
result['nativeCombined']=summarize([r for v in result['native'].values() for r in v['perRoom']])
output=ROOT/'.state/simulations/training-v2-ranger-early-west18-evidence/evidence.json';output.write_text(json.dumps(result,indent=2)+'\n')
for typ in ('human','native'):
 for name,v in result[typ].items():print(typ,name,{k:v[k] for k in ('rooms','firstRequestAge','splitRelative19','firstWeapons')})
print('nativeCombined',{k:result['nativeCombined'][k] for k in ('rooms','firstRequestAge','splitRelative19')})
