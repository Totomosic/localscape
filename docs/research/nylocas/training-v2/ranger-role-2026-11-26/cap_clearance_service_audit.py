#!/usr/bin/env python3
"""Screen source-compatible cap child launches and subsequent physical removal.

These are guard-compatible launches, not an instrumented branch count. Normal
LOS/eligibility were validated by the actual native launch; alternatives are not
asserted to be legal. The parent service budget is a work estimate, not guaranteed
random damage. Snapshot disappearance counts cap bodies, rather than HP zero.
"""
import argparse,json,csv,statistics
from pathlib import Path
from collections import Counter

def audit(data,v2=False):
 source=json.loads(data.read_text());counts=Counter();examples=[];perroom=[];firstparents={k:[] for k in ['west','south','east']}
 for q in source['rooms']:
  opened=set();first={};attempts=[];check=next((f for f in q['frames'] if f['age19']==12),None)
  for f in q['frames']:
   target=next((c for c in f['pre20_crabs'] if c['key']==f['selected']),None)
   if f['attack'] and target and target['big'] and target['wave']==18:first.setdefault(target['lane'],f['tick']-target['born'])
   if f['age19'] is not None and 5<=f['age19']<=8:
    parents=[c for c in f['pre20_crabs'] if c['big'] and c['wave']==18 and c['hp']>0 and not c['dying'] and not c['own_null'] and (not c['forecast'] or c['forecast'][1]<c['hp'])]
    gates=[]
    if f['population']<11:gates.append('low_population')
    if not parents:gates.append('no_uncovered_18_parent')
    if any(c['key'] in opened for c in parents):gates.append('opened_parent_followup')
    smalls=[c for c in f['pre20_crabs'] if not c['big'] and c['hp']>0 and not c['dying'] and not c['own_null'] and c['forecast'] and 0<=c['forecast'][1]<c['hp'] and (c['style'] in ['RANGE','MAGIC'] and c['born']+49>q['spawn19']+12 if v2 else c['style']=='RANGE' and c['lane']=='split' and c['born']>=q['spawn18'])]
    if not smalls:gates.append('no_known_nonlethal_small_in_tier')
    if f['attack'] and target and target in smalls and not gates:
     cooldown=dict(PIPE=2,CHIN=3,EYE=3,BOW=5,CLAWS=4).get(f['weapon'],99)
     shots=sum(max(1,(c['hp']-max(0,c['forecast'][1] if c['forecast'] else 0)+7)//8) for c in parents)
     budget=f['tick']+cooldown+(shots-1)*2+2
     if budget<=min(c['born']+26 for c in parents):
      removed=check is not None and target['key'] not in {c['key'] for c in check['pre20_crabs']}
      counts['guard_compatible_native_launches']+=1;counts['guard_compatible_'+f['weapon']]+=1;counts['guard_compatible_removed_by_first_check']+=removed
      later=[n for n in q['frames'] if n['tick']>f['tick']];absence=next((n['tick'] for n in later if target['key'] not in {c['key'] for c in n['pre20_crabs']}),None)
      zero=next((n['tick'] for n in later if any(c['key']==target['key'] and (c['dying'] or c['hp']<=0) for c in n['pre20_crabs'])),None)
      shot=dict(seed=q['seed'],tick=f['tick'],age19=f['age19'],weapon=f['weapon'],target=target,removed_by_first_check=removed,observed_removal_tick=absence,observed_zero_tick=zero,failed19=q['failed19']);attempts.append(shot)
      if len(examples)<35:examples.append(shot)
    if q['failed19'] and f['attack']:counts['failed_room_'+('|'.join(gates) if gates else 'guard_screen_available')]+=1
   if f['attack']:opened.add(f['selected'])
  for k,age in first.items():
   if k in firstparents:firstparents[k].append(age)
  perroom.append(dict(seed=q['seed'],failed19=q['failed19'],compatible_attempts=attempts))
 counts['rooms_with_compatible_native_launches']=sum(bool(q['compatible_attempts']) for q in perroom)
 return dict(run=source['run'],tier='v2' if v2 else 'v1',counts=dict(counts),first_parent_request_ages={k:dict(n=len(v),mean=statistics.mean(v),min=min(v),max=max(v)) for k,v in firstparents.items() if v},examples=examples,all_attempts=[s for q in perroom for s in q['compatible_attempts']],failed_rooms=[q for q in perroom if q['failed19']],caveat=__doc__)
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('data',type=Path);p.add_argument('--v2',action='store_true');p.add_argument('--output',type=Path,required=True);a=p.parse_args();r=audit(a.data,a.v2);a.output.write_text(json.dumps(r,indent=2)+'\n');print(json.dumps({k:v for k,v in r.items() if k not in ['examples','all_attempts','failed_rooms','caveat']},indent=2))
