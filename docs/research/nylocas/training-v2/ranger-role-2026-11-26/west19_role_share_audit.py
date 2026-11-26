"""Measure actual Ranger/Mage19West service and subsequent South19/20 opening."""
import argparse,collections,csv,json,statistics
from pathlib import Path

def audit(run):
 rows=list(csv.DictReader((run/'trials.csv').open()));rooms=[];room=None;origins={};previous={}
 for line in(run/'server.log').open():
  a=line.lstrip().rstrip('\r\n').split('\t')
  if a[0]!='NYLO_TRACE':continue
  tick,role,wave=map(int,a[1:4])
  if tick==0 and role==0:
   room=dict(seed=int(rows[len(rooms)]['seed']),spawns={},parents={},shots=[]);rooms.append(room);origins={};previous={}
  if role==0:room['spawns'].setdefault(wave,tick)
  crabs={}
  for raw in a[11].split(';'):
   if not raw:continue
   c=raw.split(',');key,birth,age=map(int,c[:3]);x,y=map(int,c[6:8]);origins.setdefault(key,'west' if x==3281 else 'east' if x in[3309,3310] else 'south' if y==4233 else 'split')
   crabs[key]=dict(key=key,wave=birth,born=tick-age,lane=origins[key],big=c[4]=='true',hp=int(c[5]),dying=c[10]=='true')
   if c[4]=='true' and birth in[18,19,20]:
    parent=room['parents'].setdefault(key,dict(wave=birth,lane=origins[key],born=tick-age,first_zero=None,removed=None))
    if role==0 and parent['first_zero']is None and(int(c[5])<=0 or c[10]=='true'):parent['first_zero']=tick
  if a[5]=='true':
   c=crabs.get(int(a[8]))
   if c and c['big']and c['wave']in[18,19,20]:room['shots'].append(dict(tick=tick,age=tick-c['born'],role=role,weapon=a[7],wave=c['wave'],lane=c['lane'],key=c['key'],hp=c['hp']))
  if role==0:
   for key,p in room['parents'].items():
    if key in previous and key not in crabs:p['removed']=tick
   previous=crabs
 summary={}
 for role in[0,1]:
  west=[s for q in rooms for s in q['shots']if s['role']==role and s['wave']==19 and s['lane']=='west'];summary['role'+str(role)+'_west19']=dict(requests=len(west),rooms=sum(any(s['role']==role and s['wave']==19 and s['lane']=='west'for s in q['shots'])for q in rooms),ages=dict(collections.Counter(s['age']for s in west)))
 for w in[19,20]:
  first=[min(s['age']for s in q['shots']if s['role']==1 and s['wave']==w and s['lane']=='south')for q in rooms if any(s['role']==1 and s['wave']==w and s['lane']=='south'for s in q['shots'])];summary['mage_south'+str(w)+'_first']=dict(rooms=len(first),mean=statistics.mean(first)if first else None,ages=dict(collections.Counter(first)))
 summary['ranger_west19_followup_rooms']=sum(any(s['role']==0 and s['wave']==19 and s['lane']=='west'and s['age']>3for s in q['shots'])for q in rooms)
 return dict(run=str(run),summary=summary,rooms=rooms,caveat=__doc__)
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();d=audit(a.run);a.output.write_text(json.dumps(d,indent=2)+'\n');print(json.dumps(d['summary'],indent=2))
