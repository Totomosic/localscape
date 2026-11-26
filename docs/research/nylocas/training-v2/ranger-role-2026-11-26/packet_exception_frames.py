#!/usr/bin/env python3
"""Pin compact native frames for observed Ranger packet omissions/survivors."""
import argparse,csv,json
from pathlib import Path

def audit(run,guidance):
 report=json.loads(guidance.read_text())['ranger'];rows=list(csv.DictReader((run/'trials.csv').open()))
 wanted=set(report['south30_chin_missing_seeds'])|{r['seed'] for r in report['west24_eye_without_prior_pipe']}|{r['seed'] for r in report['wave31_alive_at_permanent_melee']}
 rooms={};trial=-1;forecast={};hp={};delays={};nulls={};room=None
 for line in (run/'server.log').open():
  a=line.lstrip().rstrip('\r\n').split('\t')
  if a[0]=='NYLO_TRACE' and a[1:3]==['0','0']:
   trial+=1;forecast={};hp={};delays={};nulls={};seed=int(rows[trial]['seed']);room=dict(seed=seed,spawns={},frames=[]) if seed in wanted else None
   if room is not None:rooms[seed]=room
  if room is None:continue
  if a[0]=='NYLO_NULL':nulls[int(a[1]),int(a[2])]=set(map(int,a[3].split(','))) if len(a)>3 and a[3] else set()
  elif a[0]=='NYLO_FORECAST':forecast[int(a[1]),int(a[2])]=[int(a[3]),int(a[4])]
  elif a[0]=='NYLO_PLAYERS':hp[int(a[1])]=list(map(int,a[2].split(',')))
  elif a[0]=='NYLO_ENGINE':
   delays[int(a[1])]=list(map(int,a[2].split(',')))
   for f in room['frames']:
    if f['tick']==int(a[1]):f['native_delay']=delays[int(a[1])]

  elif a[0]=='NYLO_TRACE':
   tick,role,wave=map(int,a[1:4])
   if role==0:room['spawns'].setdefault(wave,tick)
   crabs=[]
   for raw in a[11].split(';'):
    if not raw:continue
    c=raw.split(',');key,birth,age=map(int,c[:3]);x,y=map(int,c[6:8]);crabs.append(dict(key=key,wave=birth,age=age,born=tick-age,style=c[3],big=c[4]=='true',hp=int(c[5]),x=x,y=y,dying=c[10]=='true',claimed_until=int(c[8]),own_hit_until=int(c[9]),own_null=key in nulls.get((tick,role),set()),partner_null=key in nulls.get((tick,1-role),set()),forecast=forecast.get((tick,key))))
   match=any(c['wave']==24 and c['big'] and 1<=c['age']<=6 for c in crabs) and seed in {r['seed'] for r in report['west24_eye_without_prior_pipe']}
   match=match or any(c['wave']==30 and not c['big'] and 0<=c['age']<=7 for c in crabs) and seed in report['south30_chin_missing_seeds']
   match=match or any(c['wave']==31 and 23<=c['age']<=28 for c in crabs) and seed in {r['seed'] for r in report['wave31_alive_at_permanent_melee']}
   if match:room['frames'].append(dict(tick=tick,role=role,wave=wave,ready=a[4]=='true',attack=a[5]=='true',player=a[6],weapon=a[7],target=int(a[8]),rule=a[9],reason=a[10],previous_hp=hp.get(tick-1),native_delay=delays.get(tick),crabs=crabs))
 for room in rooms.values():
  s=room['spawns'];room['cutoff']=max(s.get(29,-999),s.get(28,-999)+16);room['native_delay_caveat']='Native action delays are emitted in the engine row after both trace rows. Prior HP is from preceding cycle.'
 return dict(run=str(run),exceptions=list(rooms.values()))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('guidance',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();r=audit(a.run,a.guidance);a.output.write_text(json.dumps(r,indent=2)+'\n');print('Saved',len(r['exceptions']),'room exceptions')
