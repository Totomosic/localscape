from pathlib import Path
from collections import Counter
import csv, json, statistics, hashlib

BASE = 'training-v2-flight-mage-idle-pipe-100'
CANDIDATE = 'training-v2-flight-mage-east19-staging-100'


def read_run(name):
    run = Path('.state/simulations') / name
    rows = list(csv.DictReader((run / 'trials.csv').open()))
    rooms = []
    room = None
    for line in (run / 'server.log').open():
        f = line.strip(' \r\n').split('\t')
        if f[0] == 'NYLO_PLAYERS' and f[1] == '0':
            room = dict(row=rows[len(rooms)], births={}, spawns={}, frames={}, primaries=[])
            rooms.append(room)
        if f[0] == 'NYLO_PRIMARY':
            room['primaries'].append(dict(tick=int(f[1]), role=int(f[2]), key=int(f[3]), weapon=int(f[4])))
        if f[0] != 'NYLO_TRACE':
            continue
        tick, role, wave = map(int, f[1:4])
        room['spawns'].setdefault(wave, tick)
        crabs = {}
        for value in (f[11] if len(f) > 11 else '').split(';'):
            if not value:
                continue
            key, born_wave, age, style, big, hp, x, y, claim, own, dying = value.split(',')
            key, x, y = int(key), int(x), int(y)
            crab = dict(key=key, wave=int(born_wave), age=int(age), style=style, big=big == 'true',
                        hp=int(hp), x=x, y=y, claim=int(claim), own=int(own), dying=dying == 'true')
            crabs[key] = crab
            if key not in room['births']:
                room['births'][key] = dict(crab, born=tick-int(age),
                    lane='west' if x == 3281 else 'east' if x >= 3309 else 'south' if y == 4233 else 'split')
        if role == 1 and 18 <= wave <= 21:
            room['frames'][tick] = dict(wave=wave, crabs=crabs, point=f[6], reason=f[10])
    outcomes = []
    checks = []
    for room in rooms:
        seed = int(room['row']['seed'])
        s19, s20 = room['spawns'][19], room['spawns'][20]
        first = {}
        for attack in room['primaries']:
            birth = room['births'].get(attack['key'])
            if attack['role'] == 1 and birth and birth['wave'] == 19 and birth['big']:
                first.setdefault(birth['lane'], dict(attack, age=attack['tick']-s19))
        stalls = int(room['row']['stalls_by_last_spawned_wave'].split(';')[19])
        assert s20-s19 == 12+4*stalls
        room_checks = []
        for attempt in range(stalls):
            tick = s19+12+4*attempt
            frame = room['frames'][tick]
            crabs = frame['crabs']
            assert len(crabs) >= 12
            groups = Counter()
            big19 = []
            for key, crab in crabs.items():
                birth = room['births'][key]
                state = 'body' if crab['dying'] or crab['hp'] <= 0 else 'living'
                if crab['big'] and crab['wave'] == 19:
                    groups['19big:'+birth['lane']+':'+state] += 1
                    big19.append(dict(crab, lane=birth['lane']))
                elif birth['lane'] == 'split':
                    groups[str(crab['wave'])+'split:'+state] += 1
                else:
                    groups[str(crab['wave'])+('big:' if crab['big'] else 'small:')+state] += 1
            check = dict(seed=seed, tick=tick, age19=tick-s19, attempt=attempt,
                         population=len(crabs), living=sum(not c['dying'] and c['hp']>0 for c in crabs.values()),
                         bodies=sum(c['dying'] or c['hp']<=0 for c in crabs.values()), groups=dict(groups), big19=big19)
            room_checks.append(check)
            checks.append(check)
        # Launch/death/removal are observations; no inference about which RNG roll caused a kill.
        lifetimes = {}
        for key, birth in room['births'].items():
            if birth['wave'] != 19 or not birth['big']:
                continue
            seen = [(t, f['crabs'][key]) for t, f in room['frames'].items() if key in f['crabs']]
            dying = [t for t,c in seen if c['dying'] or c['hp']<=0]
            lifetimes[birth['lane']] = dict(key=key, firstDyingAge=min(dying)-s19 if dying else None,
                                          lastSeenAge=max(t for t,c in seen)-s19)
        outcomes.append(dict(seed=seed, ticks=int(room['row']['ticks']), stalls19=stalls,
                             s19=s19, s20=s20, first=first, lifetimes19=lifetimes,
                             failedChecks=room_checks))
    return dict(run=name, rooms=outcomes, checks=checks,
                sha256=hashlib.sha256((run/'server.log').read_bytes()).hexdigest())


def summarize(run):
    rooms=run['rooms']
    first_lane=Counter(min(r['first'], key=lambda lane:r['first'][lane]['tick']) for r in rooms)
    lanes={}
    for lane in ('east','south','west'):
        ages=[r['first'][lane]['age'] for r in rooms if lane in r['first']]
        deaths=[r['lifetimes19'][lane]['firstDyingAge'] for r in rooms
                if lane in r['lifetimes19'] and r['lifetimes19'][lane]['firstDyingAge'] is not None]
        lanes[lane]=dict(primaryRooms=len(ages),meanFirstRequestAge=statistics.mean(ages),
                        medianFirstRequestAge=statistics.median(ages),firstRequestBy3=sum(a<=3 for a in ages),
                        meanFirstDyingAge=statistics.mean(deaths),medianFirstDyingAge=statistics.median(deaths))
    counts=Counter()
    for check in run['checks']:
        counts.update(check['groups'])
    return dict(rooms=len(rooms),meanBossTicks=statistics.mean(r['ticks']for r in rooms),
                meanLast19Stalls=statistics.mean(r['stalls19']for r in rooms),firstLane=dict(first_lane),
                lanes=lanes,failedChecks=len(run['checks']),populationGroupsAcrossFailedChecks=dict(counts),
                meanLivingPerFailedCheck=statistics.mean(c['living']for c in run['checks']),
                meanBodiesPerFailedCheck=statistics.mean(c['bodies']for c in run['checks']))


def main():
    baseline=read_run(BASE)
    candidate=read_run(CANDIDATE)
    by_seed={r['seed']:r for r in baseline['rooms']}
    paired=[]
    for row in candidate['rooms']:
        old=by_seed[row['seed']]
        paired.append(dict(seed=row['seed'],bossDelta=row['ticks']-old['ticks'],
                           last19StallDelta=row['stalls19']-old['stalls19'],baseline=old,candidate=row))
    result=dict(decision='not promoted: activated intended early-east mechanism but regressed timing and pre-cap stalls',
                baseline=summarize(baseline),candidate=summarize(candidate),
                sourceLogSha256={BASE:baseline['sha256'],CANDIDATE:candidate['sha256']},
                semantics='First launch order uses native NYLO_PRIMARY records with zero reported attribution mismatches. Failure-check observations use s19+12+4i and match CSV stall counts. Bodies include HP0/death-queued snapshots; positive-HP natural-expiry animation is not separately exposed. Split cohorts use observed birth wave and first-seen lane, not exact parent linkage. Aggregating only failed checks conditions on different outcomes and does not establish causality. Damage RNG streams change when attack order changes.',
                paired=paired)
    Path(__file__).with_name('outcome-audit.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({k:result[k]for k in('decision','baseline','candidate','semantics')},indent=2))
    for p in sorted(paired,key=lambda p:p['last19StallDelta'],reverse=True)[:6]:
        print('WITNESS',p['seed'],'bossDelta',p['bossDelta'],'stallDelta',p['last19StallDelta'],
              'oldFirst',{k:v['age']for k,v in p['baseline']['first'].items()},
              'newFirst',{k:v['age']for k,v in p['candidate']['first'].items()},
              'oldLifetimes',p['baseline']['lifetimes19'],'newLifetimes',p['candidate']['lifetimes19'])


if __name__ == '__main__':
    main()
