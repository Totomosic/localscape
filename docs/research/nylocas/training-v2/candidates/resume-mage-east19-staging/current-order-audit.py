from pathlib import Path
from collections import Counter
import csv,json,statistics
results=[]
for name in ['training-v2-flight-mage-idle-pipe-100','training-v2-flight-mage-idle-pipe-validation-200']:
 run=Path('.state/simulations')/name;rows=list(csv.DictReader((run/'trials.csv').open()));rooms=[];r=None
 for line in (run/'server.log').open():
  a=line.strip(' \r\n').split('\t')
  if a[0]=='NYLO_PLAYERS'and a[1]=='0':r=dict(seed=int(rows[len(rooms)]['seed']),births={},spawns={},frames={},native={},primary=[]);rooms.append(r)
  if a[0]=='NYLO_PRIMARY':r['primary'].append(dict(tick=int(a[1]),role=int(a[2]),key=int(a[3]),weapon=int(a[4])))
  if a[0]=='NYLO_ENGINE':r['native'][int(a[1])]=int(a[2].split(',')[1])
  if a[0]!='NYLO_TRACE':continue
  t,role,w=map(int,a[1:4]);r['spawns'].setdefault(w,t);cs={}
  for c in (a[11]if len(a)>11 else'').split(';'):
   if not c:continue
   k,bw,age,style,big,hp,x,y,claim,own,dead=c.split(',');k=int(k);x,y=int(x),int(y);b=dict(wave=int(bw),age=int(age),born=t-int(age),big=big=='true',lane='west'if x==3281 else'east'if x>=3309 else'south'if y==4233 else'split',x=x,y=y,style=style,claim=int(claim),own=int(own),hp=int(hp));cs[k]=b;r['births'].setdefault(k,b)
  if role==1 and 18<=w<=19:r['frames'][t]=dict(wave=w,point=a[6],ready=a[4],attacked=a[5],rule=a[9],reason=a[10],weapon=a[7],key=int(a[8]),crabs=cs)
 for r in rooms:
  s=r['spawns'][19];first={}
  for a in r['primary']:
   b=r['births'].get(a['key'])
   if a['role']==1 and b and b['wave']==19 and b['big']:first.setdefault(b['lane'],a)
  results.append(dict(run=name,seed=r['seed'],s19=s,first={k:dict(v,age=v['tick']-s)for k,v in first.items()},stage18frames=sum(f['reason']=='Prepare West big 19'for f in r['frames'].values()if f['wave']==18),spawn19point=r['frames'][s]['point'],nativeAt19=r['native'][s],readyAt19=r['frames'][s]['ready'],r=r))
print('rooms',len(results));print('first19lane',Counter(min(r['first'],key=lambda lane:r['first'][lane]['tick'])for r in results));print('eastfirstages',Counter(r['first'].get('east',{}).get('age')for r in results));print('southfirstages',Counter(r['first'].get('south',{}).get('age')for r in results));print('spawn19positions',Counter(r['spawn19point']for r in results).most_common());print('priorWest19stageFrames',sum(r['stage18frames']for r in results),'rooms',sum(r['stage18frames']>0 for r in results));print('weststageRoomandNativeReadyAt19',sum(r['stage18frames']>0 and r['nativeAt19']<=0 for r in results))
print('witnesses',[(r['seed'],r['s19'],r['stage18frames'],r['spawn19point'],r['nativeAt19'],{k:v['age']for k,v in r['first'].items()})for r in results if r['stage18frames']>=3][:12])
import pickle;Path('/tmp/nylo-current19-order.pkl').write_bytes(pickle.dumps(results))
