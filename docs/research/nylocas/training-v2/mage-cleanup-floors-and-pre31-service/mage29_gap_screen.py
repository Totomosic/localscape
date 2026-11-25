from pathlib import Path
import csv,json,collections,sys
b=Path(sys.argv[1]);rows=list(csv.DictReader((b/'trials.csv').open()));i=-1;orig={};slots={};null=[set(),set()];pd={};spawns={};history=[];pending=[];engines={};cases=[];co=collections.Counter()
def finish():
 for e in pending:
  eng=engines.get(e['tick'])
  if not eng or int(eng[0].split(',')[1])>0:continue
  co['native_actionable_ready_no_launch_range29_review_frames']+=1
  co['with_future_own_target' if e['selected'] else 'without_own_target']+=1
  if len(cases)<15:e['engine']=eng;cases.append(e)
for l in (b/'server.log').open():
 a=l.strip(' \r\n').split('\t')
 if a[0]=='NYLO_PLAYERS'and a[1]=='0':
  if i>=0:finish()
  i+=1;orig={};slots={};null=[set(),set()];pd={};spawns={};history=[];pending=[];engines={}
 if i<0:continue
 if a[0]=='NYLO_NULL'and a[3]:null[int(a[2])].update(map(int,a[3].split(',')))
 if a[0]=='NYLO_FORECAST':pd[int(a[1]),int(a[2])]=int(a[4])
 if a[0]=='NYLO_ENGINE':engines[int(a[1])]=a[2:]
 if a[0]!='NYLO_TRACE':continue
 tick,role,wave=int(a[1]),int(a[2]),int(a[3]);spawns.setdefault(wave,tick);cs={}
 for raw in a[11].split(';'):
  if not raw:continue
  c=raw.split(',');key=int(c[0]);x,y=int(c[6]),int(c[7]);orig.setdefault(key,'west'if x==3281 else'east'if x>=3309 else'south'if y==4233 else'split');slots.setdefault(key,('W'if x==3295 else'E')if orig[key]=='south'else('N'if y==4249 else'S')if orig[key]!='split'else'');cs[key]=dict(key=key,born=tick-int(c[2]),wave=int(c[1]),age=int(c[2]),style=c[3],big=c[4]=='true',hp=int(c[5]),x=x,y=y,lane=orig[key],slot=slots[key],claim=int(c[8]),flight=int(c[9]),dying=c[10]=='true',pending=pd.get((tick,key),-1),mageNull=key in null[1],rangerNull=key in null[0])
 if role!=1:continue
 selected=cs.get(int(a[8]))
 if a[5]=='true'and selected:history.append(dict(tick=tick,weapon=a[7],rule=a[9],target=selected))
 if wave not in(29,30)or a[4]!='true'or a[5]=='true':continue
 px,py,_=map(int,a[6].split(','));targets=[]
 for c in cs.values():
  if c['wave']!=29 or c['lane']!='split'or c['style']!='RANGE'or c['hp']<=0 or c['dying']or c['mageNull']or c['age']>=49 or c['pending']>=c['hp']:continue
  if c['pending']<0 and(c['claim']>tick or c['flight']>=tick):continue
  if max(abs(c['x']-px),abs(c['y']-py))>5:continue
  targets.append(c)
 if targets:pending.append(dict(seed=rows[i]['seed'],tick=tick,wave=wave,position=a[6],selected=selected,reason=a[10],weapon=a[7],spawns=spawns.copy(),targets=targets,crabs=list(cs.values()),history=history.copy()))
finish();result=dict(counts=co,cases=cases,caveat='Potential native idle/gap frames only. Exact Java candidate timing, return path and protected primary budgets must be replayed before calling a hit free. Only stable-range SPLITs within5 are screened.');(b/'mage29-gap-screen.json').write_text(json.dumps(result,indent=2)+'\n');print(co);print([(e['seed'],e['tick'],e['wave'],e['position'],e['reason'],[(c['key'],c['hp'],c['pending'])for c in e['targets']])for e in cases[:8]])
