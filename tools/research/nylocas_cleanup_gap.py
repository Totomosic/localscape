#!/usr/bin/env python3
"""Offline, primary-event cleanup comparison; never infer human NULL status."""
import argparse
import collections
import csv
import gzip
import hashlib
import json
import statistics
from pathlib import Path


def dist(values):
    values = list(values)
    return dict(n=len(values), mean=statistics.mean(values) if values else None,
                median=statistics.median(values) if values else None,
                histogram=dict(sorted(collections.Counter(values).items())))


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--run', type=Path, default=Path('.state/simulations/training-v2-nine-utilities-fresh-100'))
    ap.add_argument('--output', type=Path, default=Path('docs/research/nylocas/training-v2-cleanup-gap.json'))
    args = ap.parse_args()
    root = Path('docs/research/nylocas')
    pin = root / 'vetting/summary.json'
    ids = {r['uuid'] for r in json.loads(pin.read_text())['eligiblePinnedRecordings']}
    sources = [pin, args.run / 'server.log', args.run / 'trials.csv']
    sources.extend(sorted((args.run / 'policy-source').rglob('*.java')))
    human = {'rooms': [], 'freshMageShots': [], 'magicStatWitnesses': [], 'magicReportedValues': {}}
    for corpus in ('mage', 'ranger'):
        attack_file = root / corpus / 'attacks.csv'
        sources.append(attack_file)
        rows = [r for r in csv.DictReader(attack_file.open()) if r['uuid'] in ids]
        stat_values = collections.defaultdict(collections.Counter)
        for uid in sorted({r['uuid'] for r in rows}):
            room = [r for r in rows if r['uuid'] == uid]
            roles = {r['player']: r['role'] for r in room}
            path = root / corpus / 'recordings' / f'{uid}.events.json.gz'
            sources.append(path)
            events = json.load(gzip.open(path, 'rt'))
            s31 = next(e['tick'] for e in events if e['type'] == 120 and e['nyloWave']['wave'] == 31)
            boss = next(e['tick'] for e in events if e['type'] == 123)
            complete = next(e['tick'] for e in events if e['type'] == 122)
            if corpus == 'mage':
                record = dict(uuid=uid, wave31=s31, boss=boss, complete=complete,
                              tail=boss-s31, cleanup=complete-s31, roles={})
                for role in ('mage', 'ranger'):
                    aa = [r for r in room if r['role'] == role and s31 <= int(r['tick']) < boss]
                    record['roles'][role] = dict(primaryCount=len(aa),
                        lastPrimaryOffset=max((int(r['tick'])-s31 for r in aa), default=None),
                        weapons=dict(collections.Counter(r['attack_name'] for r in aa)),
                        oldPrimaryCount=sum(int(r['target_wave']) <= 29 for r in aa))
                human['rooms'].append(record)
                for r in room:
                    if r['role'] == 'mage' and s31 <= int(r['tick']) < boss and int(r['target_wave']) > 29:
                        human['freshMageShots'].append(dict(uuid=uid, tick=int(r['tick']),
                            weapon=r['attack_name'], age=int(r['age']), hp=int(r['hp']),
                            target=r['target_id'], birth=int(r['target_wave']), lane=r['lane'],
                            player=[int(r['x']), int(r['y'])],
                            point=[int(r['target_x']), int(r['target_y'])]))
            for name, role in roles.items():
                updates = [e for e in events if e['type'] == 4 and e['player']['name'] == name
                           and 'magic' in e['player'] and e['tick'] < boss]
                stat_values[role].update(e['player']['magic'] >> 16 for e in updates)
                attacks = [r for r in room if r['player'] == name and int(r['tick']) < boss]
                eyes = [r for r in attacks if r['weapon'] == '31113']
                marks = {'firstAttack': min((int(r['tick']) for r in attacks), default=0),
                         'wave31': s31, 'lastEye': max((int(r['tick']) for r in eyes), default=s31)}
                witness = dict(corpus=corpus, uuid=uid, player=name, role=role, observations={})
                for label, tick in marks.items():
                    before = [e for e in updates if e['tick'] <= tick]
                    e = max(before, key=lambda e: e['tick']) if before else None
                    witness['observations'][label] = dict(tick=tick,
                        updateTick=e['tick'] if e else None,
                        current=e['player']['magic'] >> 16 if e else None,
                        base=e['player']['magic'] & 65535 if e else None)
                human['magicStatWitnesses'].append(witness)
        human['magicReportedValues'][corpus] = {r: dict(c) for r, c in stat_values.items()}
    trials = list(csv.DictReader((args.run / 'trials.csv').open()))
    bot_rooms, bot_shots, opportunities = [], [], []
    room = None
    for line in (args.run / 'server.log').open():
        a = line.strip(' \r\n').split('\t')
        if a[0] == 'NYLO_TRACE' and a[1:3] == ['0', '0']:
            row = trials[len(bot_rooms)]
            room = dict(seed=int(row['seed']), boss=int(row['ticks']), wave31=None,
                        origins={}, nulls=[set(), set()], lastSeen={}, lastLiving=[], shots=[[], []])
            bot_rooms.append(room)
        if a[0] == 'NYLO_NULL' and room is not None:
            room['nulls'][int(a[2])].update(a[3].split(','))
        if a[0] != 'NYLO_TRACE':
            continue
        tick, role, wave = int(a[1]), int(a[2]), int(a[3])
        crabs = {c.split(',')[0]: c.split(',') for c in a[11].split(';') if c}
        for k, v in crabs.items():
            if k not in room['origins']:
                x, y = int(v[6]), int(v[7])
                room['origins'][k] = 'split' if 3285 <= x <= 3305 and y >= 4237 else 'lane'
        if wave != 31:
            continue
        if room['wave31'] is None:
            room['wave31'] = tick
        if role == 0:
            for k, v in crabs.items():
                room['lastSeen'][k] = (tick, v)
            live = [v for v in crabs.values() if int(v[5]) > 0 and v[10] == 'false']
            if live:
                room['lastLiving'] = live
                room['lastLivingTick'] = tick
        if a[5] != 'true':
            continue
        room['shots'][role].append((tick, a[7]))
        t = crabs.get(a[8])
        if role != 1 or t is None or int(t[1]) <= 29:
            continue
        bot_shots.append(dict(seed=room['seed'], tick=tick, weapon=a[7], target=a[8],
                              birth=int(t[1]), age=int(t[2]), hp=int(t[5])))
        if a[7] != 'SULPHUR':
            continue
        px, py, _ = map(int, a[6].split(','))
        candidates = []
        for k, v in crabs.items():
            distance = max(abs(int(v[6])-px), abs(int(v[7])-py))
            if (v[3] != 'MAGIC' or v[4] != 'false' or int(v[1]) <= 29
                or room['origins'][k] != 'split' or int(v[5]) <= 0 or v[10] != 'false'
                or int(v[8]) > tick or int(v[9]) > tick or int(v[2]) >= 49
                or k in room['nulls'][1] or not 1 <= distance <= 8):
                continue
            candidates.append(dict(key=k, birth=int(v[1]), age=int(v[2]), hp=int(v[5]),
                point=[int(v[6]), int(v[7])], distance=distance,
                claimedUntil=int(v[8]), ownHitUntil=int(v[9]), observedMageNull=False))
            candidates[-1]['observedRangerNull'] = k in room['nulls'][0]
        if candidates:
            opportunities.append(dict(seed=room['seed'], tick=tick, player=[px, py],
                selectedMeleeKey=a[8], selectedMeleeAge=int(t[2]), selectedMeleeHp=int(t[5]),
                magicCandidates=candidates))
    terminal = collections.Counter()
    for r in bot_rooms:
        for v in r['lastLiving']:
            terminal[f"{v[1]}:{v[3]}:mageNull={v[0] in r['nulls'][1]}:rangerNull={v[0] in r['nulls'][0]}"] += 1
        r['lastDisappearanceEstimate'] = max(t for t, v in r['lastSeen'].values()) + 1
        r['lastLiving'] = [dict(key=v[0], birth=int(v[1]), style=v[3], hp=int(v[5]),
            mageNull=v[0] in r['nulls'][1], rangerNull=v[0] in r['nulls'][0]) for v in r['lastLiving']]
        for k in ('origins', 'nulls', 'lastSeen'):
            del r[k]
    result = dict(scope='actual wave31, fresh birth>29; human NULL never inferred',
        feasibilityLimit='Opportunity witnesses prove snapshot range, style, age, claim, own-flight and observed native NULL gates. Collision/LOS and full planner travel feasibility are not logged; no guaranteed alternative launch is claimed.',
        human=human, bot=dict(rooms=bot_rooms, freshMageShots=bot_shots,
            sulphurWithMagicOpportunities=opportunities, terminalLastLivingStatuses=dict(terminal)),
        summary=dict(humanBoss=dist(r['boss'] for r in human['rooms']),
            humanWave31=dist(r['wave31'] for r in human['rooms']),
            humanTail=dist(r['tail'] for r in human['rooms']),
            humanCleanup=dist(r['cleanup'] for r in human['rooms']),
            botBoss=dist(r['boss'] for r in bot_rooms), botWave31=dist(r['wave31'] for r in bot_rooms),
            botTail=dist(r['boss']-r['wave31'] for r in bot_rooms),
            botCleanupEstimate=dist(r['lastDisappearanceEstimate']-r['wave31'] for r in bot_rooms)))
    result['sources'] = {str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(args.output)


if __name__ == '__main__':
    main()
