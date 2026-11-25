from pathlib import Path
import csv,json,collections,sys
b=Path(sys.argv[1]);rows=list(csv.DictReader((b/'trials.csv').open()));i=-1;orig={};slots={};null=set();forecast={};spawns={};history=[];pending=[];engines={};cases=[];co=collections.Counter();lastframes=[]
def finish():
 for f in pending:
  e=engines.get(f['tick'])
  if e is None or int(e[0].split(',')[1])>0:continue
  co['actionable_no_launch_split_chase_with_nearby_own_magic']+=1;co['wave_'+str(f['wave'])]+=1
  if len(cases)<30:f['engine']=e;cases.append(f)
for line in (b/'server.log').open():
 f=line.strip(' \r\n').split('\t')
 if f[0]=='NYLO_PLAYERS'and f[1]=='0':
  if i>=0:finish()
  i+=1;orig={};slots={};null=set();forecast={};spawns={};history=[];pending=[];engines={};lastframes=[]
 if i<0:continue
 if f[0]=='NYLO_NULL'and f[2]=='1'and f[3]:null.update(map(int,f[3].split(',')))
 if f[0]=='NYLO_FORECAST':forecast[int(f[1]),int(f[2])]=int(f[4])
 if f[0]=='NYLO_ENGINE':engines[int(f[1])]=f[2:]
 if f[0]!='NYLO_TRACE'or f[2]!='1':continue
 t,w=int(f[1]),int(f[3]);spawns.setdefault(w,t);cr={}
 for raw in f[11].split(';'):
  if not raw:continue
  c=raw.split(',');k=int(c[0]);x,y=int(c[6]),int(c[7]);orig.setdefault(k,'west'if x==3281 else'east'if x>=3309 else'south'if y==4233 else'split');slots.setdefault(k,('W'if x==3295 else'E')if orig[k]=='south'else('N'if y==4249 else'S')if orig[k]!='split'else'');cr[k]=dict(key=k,born=t-int(c[2]),wave=int(c[1]),age=int(c[2]),style=c[3],big=c[4]=='true',hp=int(c[5]),x=x,y=y,lane=orig[k],slot=slots[k],claim=int(c[8]),flight=int(c[9]),pending=forecast.get((t,k),-1),dying=c[10]=='true',mageNull=k in null)
 selected=cr.get(int(f[8]));snapshot=dict(tick=t,wave=w,position=f[6],ready=f[4],launch=f[5],weapon=f[7],rule=f[9],reason=f[10],selected=selected,crabs=list(cr.values()),spawns=spawns.copy())
 if f[5]=='true'and selected:history.append(dict(tick=t,weapon=f[7],rule=f[9],target=selected))
 if w<20 and f[4]=='true'and f[5]=='false'and f[7]=='SULPHUR'and f[9]=='null'and selected and selected['lane']=='split':
  px,py=map(int,f[6].split(',')[:2]);dist=lambda c:max(abs(c['x']-px),abs(c['y']-py))
  if dist(selected)>1:
   co['ready_no_launch_nonadjacent_split_sulphur']+=1
   targets=[c for c in cr.values()if c['lane']=='split'and c['style']=='MAGIC'and not c['big']and c['hp']>0 and not c['dying']and not c['mageNull']and w-c['wave']<=4 and c['age']<49 and c['pending']<c['hp']and not(c['pending']<0 and(c['claim']>t or c['flight']>=t))and dist(c)<8]
   if targets:pending.append(dict(seed=rows[i]['seed'],**snapshot,targets=targets,history=history.copy(),previous=lastframes.copy()))
 lastframes=(lastframes+[snapshot])[-2:]
finish();out=dict(counts=co,cases=cases,caveat='Potential ready ordinary Mage Eye split versus nonadjacent ordinary splitSulphur chase. Native action delay checked; exact LOS, packet budgets and Java eligibility must still be verified. No primary/parent/AoE candidates are declared mistakes by this screen.');(b/'mage-precap-split-chase-screen.json').write_text(json.dumps(out,indent=2)+'\n');print(co);print([(e['seed'],e['tick'],e['wave'],e['position'],e['selected']['key'],[(c['key'],c['wave'],c['born'],c['x'],c['y'])for c in e['targets']])for e in cases[:15]])
