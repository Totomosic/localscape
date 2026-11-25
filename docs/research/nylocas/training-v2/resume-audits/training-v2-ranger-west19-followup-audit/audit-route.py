from pathlib import Path
import json,math,sys
source=Path(sys.argv[1]);x=json.loads(source.read_text());accepted=[]
def distance(a,b,size=1):return max(b[0]-a[0],a[0]-b[0]-size+1,b[1]-a[1],a[1]-b[1]-size+1)
def attack_tile(a,b,size,reach):return [max(3290,min(3301,max(b[0]-reach,min(a[0],b[0]+size-1+reach)))),max(4243,min(4254,max(b[1]-reach,min(a[1],b[1]+size-1+reach))))]
for r in x['witnesses']:
 t=r['tick'];w=r['west19'];eye_target=[w['x']+(w['age']<9),w['y']];pos=attack_tile(r['position'],eye_target,2,8)
 if max(abs(pos[i]-r['position'][i]) for i in (0,1))>2 or r['nulled18']:continue
 at=t+3;move_from=t+1;route=[];valid=True
 for b in sorted(r['big18'],key=lambda c:['west','south','east'].index(c['lane'])):
  new_self_lease=r['currentAttacked'] and int(r['current'][1])==b['key'] and b['own']==t+32
  if b['claim']>t or b['own']>=t and not new_self_lease:valid=False;break
  for shot in range(math.ceil(b['hp']/8)):
   target=[b['x'],b['y']];dest=attack_tile(pos,target,2,5)
   travel=math.ceil(max(abs(pos[i]-dest[i]) for i in (0,1))/2)
   at=max(at,move_from+max(0,travel-1))
   if at+2>b['born']+26 or at+1>=b['born']+26:valid=False;break
   route.append(dict(key=b['key'],request=at,impact=at+2,position=dest))
   pos=dest;move_from=at+1;at+=3 #2tick native pipe flight +next observation before retry
  if not valid:break
 if valid:accepted.append(dict(**r,route=route))
result=dict(run=x['run'],semantics='Exact existing movement projection and observed claims. Eye3 cooldown; pipe2 native impact +1 observation before same-primary repeat. Budget workload ceil(observedHP/8) matches existing policy estimates, not guaranteed damage; no pending-hit kill credit. Last impact<=birth18+26.',acceptedFrames=len(accepted),acceptedRooms=len(set(r['seed'] for r in accepted)),witnesses=accepted)
source.with_name(source.stem+'-routed.json').write_text(json.dumps(result,indent=2)+'\n')
print(source.name,result['acceptedFrames'],'frames',result['acceptedRooms'],'rooms')
for r in accepted[:8]:print(r['seed'],r['tick'],'age',r['west19']['age'],'hp',r['west19']['hp'],'mageNow',r['mageShootsWestNow'],r['route'])
