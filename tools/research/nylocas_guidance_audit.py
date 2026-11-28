#!/usr/bin/env python3
"""Audit Jordan's ranger guidance against confirmed native launches and pinned humans.

Run from the repository root. Timing/coverage describe observations; a missing
launch does not by itself prove a ready native input opportunity. Trace HP and
position are pre-decision, leases post-cycle. This audit does not infer damage
forecasts from those mixed-phase leases.
"""
import argparse
from collections import Counter, defaultdict
import csv
import gzip
import hashlib
import json
from pathlib import Path
import statistics


def distribution(values):
    return dict(mean=statistics.mean(values), min=min(values), max=max(values)) if values else None


def read_run(directory):
    with (directory / 'trials.csv').open() as stream:
        rows = list(csv.DictReader(stream))
    frames, launches = [], []
    trial, previous, origins, identities = -1, None, {}, {}
    nulls = {}
    with (directory / 'server.log').open() as stream:
        for line in stream:
            a = line.strip().split('\t')
            if a[0] == 'NYLO_NULL':
                nulls[int(a[1]),int(a[2])] = set(map(int,a[3].split(','))) if len(a)>3 and a[3] else set()
                continue
            if a[0] != 'NYLO_TRACE':
                continue
            tick, role, wave = map(int, a[1:4])
            if previous is None or tick < previous:
                trial += 1
                origins, identities, nulls = {}, {}, {}
            previous = tick
            crabs = {}
            for item in (a[11].split(';') if len(a) > 11 else []):
                if not item:
                    continue
                c = item.split(',')
                key, born_wave, age = map(int, c[:3])
                x, y = map(int, c[6:8])
                if key not in origins:
                    # Match the controller's exact spawn identity. Children can appear
                    # just outside the room and must not inherit a lane packet.
                    origins[key] = 'west' if x == 3281 else 'east' if 3309 <= x <= 3310 else 'south' if y == 4233 else 'split'
                    slot = ('W' if x == 3295 else 'E') if origins[key] == 'south' else ('N' if y == 4249 else 'S')
                    identities[key] = dict(key=key, wave=born_wave, born=tick-age,
                                           lane=origins[key], slot=slot, big=c[4]=='true')
                crabs[key] = dict(identities[key], age=age, style=c[3], hp=int(c[5]),
                                 x=x,y=y, claimed_until=int(c[8]), own_hit_until=int(c[9]),
                                 dying=c[10]=='true', nulled=key in nulls.get((tick,role),set()))
            frame = dict(trial=trial, seed=int(rows[trial]['seed']), tick=tick,role=role,
                         wave=wave,ready=a[4]=='true',attack=a[5]=='true',player=a[6],
                         weapon=a[7],target_key=int(a[8]),rule=a[9],reason=a[10],crabs=crabs)
            frames.append(frame)
            if frame['attack']:
                if frame['target_key'] not in crabs:
                    raise ValueError('Confirmed primary absent from observation')
                launches.append(dict(frame, target=crabs[frame['target_key']]))
    if trial+1 != len(rows):
        raise ValueError('Trace/CSV trial mismatch')
    return rows, frames, launches


def ranger_audit(rows, frames, launches):
    shots=[s for s in launches if s['role']==0]
    by_trial=defaultdict(list)
    for s in shots: by_trial[s['trial']].append(s)
    forbidden=[s for s in shots if s['weapon']=='BOW' and not s['target']['big'] and s['target']['wave']>2]
    def select(wave,lane,big=None,weapon=None):
        return [s for s in shots if s['target']['wave']==wave and s['target']['lane']==lane
                and (big is None or s['target']['big']==big) and (weapon is None or s['weapon']==weapon)]
    def coverage(selected):
        return dict(launches=len(selected),rooms=len({s['trial'] for s in selected}),
                    ages=dict(sorted(Counter(s['target']['age'] for s in selected).items())),
                    weapons=dict(Counter(s['weapon'] for s in selected)))
    def sample(selected):
        return [dict(seed=s['seed'],tick=s['tick'],wave=s['wave'],target_wave=s['target']['wave'],
                     lane=s['target']['lane'],age=s['target']['age'],weapon=s['weapon'],
                     hp=s['target']['hp'],rule=s['rule'],reason=s['reason']) for s in selected]
    target24=select(24,'west',True)
    eye_without_pipe=[]
    for s in target24:
        if s['weapon']=='EYE' and not any(p['target_key']==s['target_key'] and p['weapon']=='PIPE'
                and p['tick']<s['tick'] for p in by_trial[s['trial']]): eye_without_pipe.append(s)
    target31=[s for s in shots if s['target']['wave']==31 and s['target']['lane']!='split']
    late31={}
    for f in frames:
        if f['role']!=0:continue
        for c in f['crabs'].values():
            if c['wave']==31 and c['lane'] in ('west','east') and not c['big'] and c['age']==26 and c['hp']>0 and c['style']=='MELEE':
                late31.setdefault((f['trial'],c['key']),dict(seed=f['seed'],tick=f['tick'],lane=c['lane'],hp=c['hp'],key=c['key']))
    south30=select(30,'south',False,'CHIN')
    scratch=[s for s in select(2,'south',False) if s['weapon'] in ('SULPHUR','CLAWS')]
    expected=['west1_bow','east2_bow','south3_chin','south2_scratch','west5_pipe','west4_pipe']
    def packet(shot):
        c=shot['target'];identity=(c['wave'],c['lane'],c['big'],shot['weapon'])
        return {(1,'west',False,'BOW'):expected[0],(2,'east',False,'BOW'):expected[1],
                (3,'south',False,'CHIN'):expected[2],(2,'south',False,'CLAWS'):expected[3],
                (2,'south',False,'SULPHUR'):expected[3],
                (5,'west',True,'PIPE'):expected[4],(4,'west',False,'PIPE'):expected[5]}.get(identity,'other')
    opening_matches=[]
    for trial,ss in by_trial.items():
        sequence=[]
        for shot in ss:
            value=packet(shot)
            if not sequence or sequence[-1]!=value:sequence.append(value)
            if len(sequence)>=6:break
        if sequence==expected:opening_matches.append(trial)
    first_pipes=[]
    repeat_scratch_seeds=[]
    for trial,ss in by_trial.items():
        first=next((s for s in ss if packet(s)=='west5_pipe'),None)
        if first is None:continue
        first_pipes.append(first['target']['age'])
        scratches=[s for s in ss if s['tick']<first['tick'] and packet(s)=='south2_scratch']
        if len(scratches)>1:repeat_scratch_seeds.append(int(rows[trial]['seed']))
    # Classify decisions separately from native launches so food/SBS failures are visible.
    attempts30=[f for f in frames if f['role']==0 and f['rule']=='South chin flicker 30' and f['target_key']>=0]
    opening_rules=('Opening west','Opening east','South chin 3','Opening scratch','West big 5','West small 4')
    return dict(forbidden_small_bows=coverage(forbidden),forbidden_small_bow_examples=sample(forbidden[:12]),
       opening_named_rule_rooms={r:len({s['trial'] for s in shots if s['rule']==r}) for r in opening_rules},
       opening_actual_scratch=coverage(scratch),opening_first_six_in_order_rooms=len(opening_matches),
       west5_first_pipe_request_ages=dict(sorted(Counter(first_pipes).items())),
       repeated_scratch_before_first_west5_seeds=repeat_scratch_seeds,
       opening_actual_scratch_missing_seeds=[int(rows[t]['seed']) for t in range(len(rows)) if t not in {s['trial'] for s in scratch}],
       opening_first_six= {str(rows[t]['seed']):sample(ss[:6]) for t,ss in by_trial.items()},
       west5=coverage(select(5,'west',True)),west11_backup_actual12=coverage([s for s in select(11,'west',True) if s['wave']==12]),
       south6_small_bows=coverage(select(6,'south',False,'BOW')),
       west24=coverage(target24),west24_eye_without_prior_pipe=sample(eye_without_pipe),
       south30_chin=coverage(south30),south30_chin_missing_seeds=[int(rows[t]['seed']) for t in range(len(rows)) if t not in {s['trial'] for s in south30}],
       south30_chin_selected_rooms=len({f['trial'] for f in attempts30}),
       south30_selected_without_launch_rooms=sorted({f['seed'] for f in attempts30}-{s['seed'] for s in south30}),
       wave31_lane_attacks=coverage(target31),wave31_alive_at_permanent_melee=list(late31.values()),
       wave31_alive_at_permanent_melee_rooms=len({k[0] for k in late31}),
       caveat='Attack coverage, not guaranteed available opportunities; HP/position predecision and flight leases postcycle. Permanent-melee count uses observed west/east lane survivors at age26; the current observed survivors are east-south. Inspect examples before causal attribution.')


def human_audit():
    root=Path('docs/research/nylocas')
    vetted=json.loads((root/'vetting/summary.json').read_text())
    ids={r['uuid'] for r in vetted['eligiblePinnedRecordings']}
    rows=[r for r in csv.DictReader((root/'ranger/attacks.csv').open()) if r['uuid'] in ids and r['role']=='ranger']
    def check(wave,lane,weapon=None):
        rr=[r for r in rows if r['target_wave']==str(wave) and r['lane']==lane and (weapon is None or r['weapon']==weapon)]
        return dict(attacks=len(rr),rooms=len({r['uuid'] for r in rr}),weapons=dict(Counter(r['weapon'] for r in rr)),ages=dict(Counter(r['age'] for r in rr)),
                    examples=[{k:r[k] for k in ('uuid','tick','weapon','age','x','y','target')} for r in rr[:4]])
    manifest=json.loads((root/'ranger/manifest.json').read_text())
    checked=0
    for r in manifest['recordings']:
        if r['uuid'] not in ids: continue
        for source in r['files'].values():
            p=root/'ranger'/source['path']
            if hashlib.sha256(gzip.decompress(p.read_bytes()) if p.suffix=='.gz' else p.read_bytes()).hexdigest()!=source['sha256']: raise ValueError(f'Human source checksum mismatch: {p}')
            checked+=1
    return dict(vetted_rooms=len({r['uuid'] for r in rows}),verified_raw_sources=checked,
                opening_scratch=check(2,'south','13652'),west5=check(5,'west'),west24=check(24,'west'),south30_chin=check(30,'south','11959'),east31_pipe=check(31,'east','12926'))


def audit(directory):
    rows,frames,launches=read_run(directory)
    completed=[int(r['ticks']) for r in rows if r['status']=='completed']
    return dict(run=str(directory),trials=len(rows),completed=len(completed),deaths=sum(r['status']=='death' for r in rows),
       boss_ticks=distribution(completed),boss_seconds=distribution([t*.6 for t in completed]),
       ranger=ranger_audit(rows,frames,launches),human=human_audit(),
       sources={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in [directory/'server.log',directory/'trials.csv',*sorted((directory/'policy-source').rglob('*.java'))]})


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('directory',type=Path);parser.add_argument('--output',type=Path);args=parser.parse_args()
    result=audit(args.directory);out=args.output or args.directory/'ranger-guidance-audit.json';out.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({k:result[k] for k in ('run','trials','completed','deaths','boss_ticks','boss_seconds')},indent=2))
    print(json.dumps({k:v for k,v in result['ranger'].items() if k not in ('opening_first_six','forbidden_small_bow_examples','wave31_alive_at_permanent_melee')},indent=2));print(out)
if __name__=='__main__':main()
