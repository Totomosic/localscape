from pathlib import Path
import csv,json,gzip,hashlib,collections
root=Path('docs/research/nylocas/mage');manifest=json.loads((root/'manifest.json').read_text());v=json.loads(Path('docs/research/nylocas/vetting/summary.json').read_text());ids={x['uuid'] for x in v['eligiblePinnedRecordings'] if 'mage' in x['roles']};att=list(csv.DictReader((root/'attacks.csv').open()));casts=[r for r in att if r['uuid'] in ids and r['role']=='mage' and r['attack_type']=='24' and 28<=int(r['wave'])<31];out=[];co=collections.Counter()
for uid in dict.fromkeys(r['uuid'] for r in casts):
 rec=next(r for r in manifest['recordings'] if r['uuid']==uid);data={}
 for name in ('raid','events'):
  e=rec['files'][name];raw=gzip.decompress((root/e['path']).read_bytes());assert hashlib.sha256(raw).hexdigest()==e['sha256'];data[name]=json.loads(raw)
 meta=data['raid']['tobRooms']['nylocas']['npcs'];selected={k:n for k,n in meta.items() if 'nylo'in n and n['nylo']['big'] and 23<=n['nylo']['wave']<=27};states={};snap={};by=collections.defaultdict(list)
 for e in data['events']:by[e['tick']].append(e)
 wanted={int(r['tick'])+d for r in casts if r['uuid']==uid for d in (-1,0,1,2)}
 for tick,batch in sorted(by.items()):
  for e in batch:
   if e['type']not in(7,8):continue
   n=e['npc'];key=str(n['roomId'])
   if key not in selected:continue
   states.setdefault(key,{}).update(n);states[key]['x']=e['xCoord'];states[key]['y']=e['yCoord']
  if tick in wanted:snap[tick]={k:s.copy() for k,s in states.items()}
 for cast in (r for r in casts if r['uuid']==uid):
  tick=int(cast['tick']);victims=[]
  for key,n in selected.items():
   if not n['spawnTick']<=tick<n['deathTick']:continue
   before=snap.get(tick-1,{}).get(key,{});now=snap.get(tick,{}).get(key,before);after=snap.get(tick+2,{}).get(key,{});hp0=before.get('hitpoints',0)>>16;hp1=after.get('hitpoints',before.get('hitpoints',0))>>16
   if hp0<=0:continue
   style=(now.get('id',n['spawnNpcId'])-8342)%3;distance=max(abs(int(cast['x'])-now['x']),abs(int(cast['y'])-now['y']));other=[r for r in att if r['uuid']==uid and r['target_id']==key and tick-8<=int(r['tick'])<=tick+2 and not(r['role']=='mage'and int(r['tick'])==tick and r['attack_type']=='24')]
   item=dict(key=key,wave=n['nylo']['wave'],lane={1:'west',2:'south',3:'east'}.get(n['nylo']['spawnType'],'split'),age=tick-n['spawnTick'],style=('melee','range','magic')[style],hp_before=hp0,hp_after2=hp1,anchor_distance=distance,other_primaries=other,position=(now['x'],now['y']))
   if style==0 and distance<=5:
    co['inradius_melee_big_w'+str(n['nylo']['wave'])+'_'+item['lane']]+=1
    if hp0-hp1>=4 and not other:co['large_drop_without_other_primary_w'+str(n['nylo']['wave'])+'_'+item['lane']]+=1
   if n['nylo']['wave']==27 and item['lane']=='east':co['27East_current_'+item['style']]+=1
   victims.append(item)
  out.append(dict(uuid=uid,tick=tick,cast=cast,victims=victims))
result=dict(casts=len(casts),rooms=len({r['uuid'] for r in casts}),counts=co,cases=out,caveat='Checksum-vetted raw snapshots. HPdrop>=4 withinT-1..T+2 and no other primary on that NPC inT-8..T+2 is compatible with Dinh AoE, not proof of attribution; other secondary effects and snapshot phases remain possible. Anchor distance<=5 is geometric screening, not native ray/search replay. Only first-level bigs23..27 inspected.');Path('.state/simulations/training-v3-mage-guidance-checks/human-late-dinh-review.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(dict(casts=result['casts'],rooms=result['rooms'],counts=co),indent=2))
for e in out:
 hits=[x for x in e['victims'] if x['wave']==25 and x['lane']=='east' and x['style']=='melee' and x['anchor_distance']<=5 and x['hp_before']-x['hp_after2']>=4 and not x['other_primaries']]
 if hits:print('witness',e['uuid'],e['tick'],(e['cast']['x'],e['cast']['y']),hits);break
