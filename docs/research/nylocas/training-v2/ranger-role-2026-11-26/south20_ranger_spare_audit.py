"""Track original20South physical transition and literal Ranger spare Range26 windows."""
import argparse,collections,csv,hashlib,json
from pathlib import Path

def audit(run):
 rows=list(csv.DictReader((run/'trials.csv').open()));rooms=[];room=None;origins={};previous={};forecasts={};nulls={}
 for line in(run/'server.log').open():
  a=line.lstrip().rstrip('\r\n').split('\t')
  if a[0]=='NYLO_TRACE'and a[1:3]==['0','0']:
   room=dict(seed=int(rows[len(rooms)]['seed']),spawns={},target=None,parent=None,shots=[],frames=[],first_zero=None,removed=None,transition=None);rooms.append(room);origins={};previous={};forecasts={};nulls={}
  if room is None:continue
  if a[0]=='NYLO_FORECAST':forecasts[int(a[1]),int(a[2])]=list(map(int,a[3:5]))
  elif a[0]=='NYLO_NULL':nulls[int(a[1]),int(a[2])]=set(map(int,a[3].split(',')))if len(a)>3 and a[3]else set()
  elif a[0]=='NYLO_ENGINE':
   tick=int(a[1])
   for f in room['frames']:
    if f['tick']==tick:f['native_delays']=list(map(int,a[2].split(',')))
  elif a[0]=='NYLO_TRACE':
   tick,role,wave=map(int,a[1:4]);crabs={}
   if role==0:room['spawns'].setdefault(wave,tick)
   for raw in a[11].split(';'):
    if not raw:continue
    c=raw.split(',');key,birth,age=map(int,c[:3]);x,y=map(int,c[6:8]);origins.setdefault(key,'west'if x==3281 else 'east'if x in[3309,3310]else'south'if y==4233 else 'split')
    crabs[key]=dict(key=key,wave=birth,born=tick-age,age=age,lane=origins[key],style=c[3],big=c[4]=='true',hp=int(c[5]),x=x,y=y,dying=c[10]=='true',forecast=forecasts.get((tick,key)),ranger_null=key in nulls.get((tick,0),set()),mage_null=key in nulls.get((tick,1),set()))
    if birth==20 and  origins[key]=='south'and c[4]=='true':room['target']=key;room['parent']=crabs[key]
   parent=crabs.get(room['target'])
   if parent and a[5]=='true'and int(a[8])==parent['key']:room['shots'].append(dict(tick=tick,age=parent['age'],role=role,weapon=a[7],hp=parent['hp'],forecast=parent['forecast']))
   if role!=0:continue
   if parent and(parent['hp']<=0 or  parent['dying'])and room['first_zero']is None:room['first_zero']=tick
   if room['target']in previous and room['target']not in crabs:room['removed']=tick
   if wave==21 and  room['transition']is None:room['transition']=dict(tick=tick,parent=parent)
   if parent and 26<=parent['age']<=48 and  wave in[21,22]:
    xy=list(map(int,a[6].split(',')));distance=max(parent['x']-xy[0],xy[0]-parent['x']-1,parent['y']-xy[1],xy[1]-parent['y']-1)
    room['frames'].append(dict(tick=tick,wave=wave,ready=a[4]=='true',attack=a[5]=='true',selected=int(a[8]),reason=a[10],player=xy,distance=distance,population=len(crabs),parent=parent,known_extra_deaths=sum(c['big']and c['key']!=parent['key']and(c['dying']or c['forecast']and c['hp']>0 and  c['forecast'][1]>=c['hp'])for c in crabs.values())))
   previous=crabs
 counts=collections.Counter();eligible=[]
 for room in rooms:
  transition=room['transition'];parent=transition['parent']if transition else None
  counts['rooms']+=1;counts['physical_clear_at21']+=parent is None;counts['hp0_body_at21']+=parent is not None and(parent['hp']<=0 or  parent['dying']);counts['positive_hp_at21']+=parent is not None and parent['hp']>0 and  not parent['dying']
  counts['natural55']+=room['removed']is not None and room['parent']and room['removed']-room['parent']['born']==55 and  room['first_zero']is None
  frames=[f for f in room['frames']if f['ready']and not f['attack']and f['selected']==-1 and  f['parent']['hp']>0 and  not f['parent']['dying']and not f['parent']['ranger_null']and f['parent']['style']=='RANGE'and f['parent']['forecast']and 0<=f['parent']['forecast'][1]<f['parent']['hp']and f['distance']<=5]
  counts['local_idle_frames']+=len(frames);counts['rooms_local_idle']+=bool(frames)
  safe=[f for f in frames if f['population']+1+f['known_extra_deaths']<=23]
  counts['headroom_idle_frames']+=len(safe);counts['rooms_headroom_idle']+=bool(safe)
  if safe:eligible.append(dict(seed=room['seed'],spawns=room['spawns'],shots=room['shots'],transition=room['transition'],first_zero=room['first_zero'],removed=room['removed'],frames=safe))
 return dict(run=str(run),counts=dict(counts),eligible=eligible,rooms=rooms,caveat=__doc__,sources={str(run/p):hashlib.sha256((run/p).read_bytes()).hexdigest()for p in['server.log','trials.csv']})
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();d=audit(a.run);a.output.write_text(json.dumps(d,indent=2)+'\n');print(json.dumps(d['counts'],indent=2));print('examples',[(q['seed'],q['frames'][0]['tick'],q['frames'][0]['population'])for q in d['eligible'][:5]])
