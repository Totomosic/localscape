#!/usr/bin/env python3
"""Compare pinned vetted human primary attacks with an instrumented bot run.

No simulation/build/network needed. Run from the repository root. This reports
launches, not damage attribution: a primary launch does not prove a splash kill.
"""
import argparse
import collections
import csv
import gzip
import hashlib
import json
import statistics
from pathlib import Path


def hist(values):
    return dict(sorted(collections.Counter(values).items()))


def mean(values):
    return statistics.mean(values) if values else None


def timing(rows):
    rooms = sorted({r['uuid'] for r in rows})
    examples = []
    ownership = []
    for uid in rooms:
        room = [r for r in rows if r['uuid'] == uid]
        west = [r for r in room if r['target_wave'] == '19'
                and r['big'] == 'True' and r['lane'] == 'west']
        ownership.append('+'.join(sorted({r['role'] for r in west})) or 'neither')
        ranger = [r for r in west if r['role'] == 'ranger']
        if not ranger:
            continue
        first = min(ranger, key=lambda r: int(r['tick']))
        differences = {}
        for lane in ('west', 'south', 'east'):
            prior = [int(r['tick']) for r in room if r['role'] == 'ranger'
                     and r['target_wave'] == '18' and r['big'] == 'True'
                     and r['lane'] == lane]
            differences[lane] = int(first['tick']) - min(prior) if prior else None
        examples.append(dict(uuid=uid, firstTick=int(first['tick']),
                             age=int(first['age']), currentWave=int(first['wave']),
                             deltaFromFirstWave18=differences))
    return dict(rooms=len(rooms), ownershipRooms=hist(ownership),
                west19PrimaryAttacks=hist(r['role'] for r in rows
                    if r['target_wave'] == '19' and r['big'] == 'True'
                    and r['lane'] == 'west'),
                rangerWest19Rooms=len(examples),
                firstRangerAge=hist(e['age'] for e in examples),
                firstRangerCurrentWave=hist(e['currentWave'] for e in examples),
                medianDeltaFromFirstWave18={lane: statistics.median(
                    e['deltaFromFirstWave18'][lane] for e in examples
                    if e['deltaFromFirstWave18'][lane] is not None)
                    for lane in ('west', 'south', 'east')}, examples=examples)


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--run', type=Path, default=Path(
        '.state/simulations/training-v2-utility-instrumented-30'))
    ap.add_argument('--output', type=Path, default=Path(
        'docs/research/nylocas/training-v2-precap-gap.json'))
    args = ap.parse_args()
    root = Path('docs/research/nylocas')
    pin = root / 'vetting/summary.json'
    ids = {r['uuid'] for r in json.loads(pin.read_text())['eligiblePinnedRecordings']}
    sources = [pin, args.run / 'server.log', args.run / 'trials.csv']
    sources.extend(sorted((args.run / 'policy-source').rglob('*.java')))
    result = {'scope': 'actual current wave <20; confirmed primary launches only',
              'human': {}, 'bot': {}}
    for role in ('mage', 'ranger'):
        attack_file = root / role / 'attacks.csv'
        wave_file = root / role / 'waves.csv'
        sources.extend((attack_file, wave_file))
        rows = [r for r in csv.DictReader(attack_file.open()) if r['uuid'] in ids]
        attacks = [r for r in rows if r['role'] == role and int(r['wave']) < 20]
        waves = [r for r in csv.DictReader(wave_file.open()) if r['uuid'] in ids]
        names = {'12926': 'PIPE', '31113': 'EYE', '11959': 'CHIN',
                 '20997': 'BOW', '13652': 'CLAWS'}
        result['human'][role] = dict(
            rooms=len({r['uuid'] for r in rows}), precapAttacks=len(attacks),
            precapWeapons=hist(r['attack_name'] if role == 'mage'
                else names.get(r['weapon'], r['weapon']) for r in attacks),
            meanSpawnTicks={str(n): mean(int(r['tick']) for r in waves
                if int(r['wave']) == n) for n in (8, 10, 12, 14, 16, 18, 19, 20, 21, 31)},
            west19=timing(rows))
        if role == 'ranger':
            result['human'][role]['precapChinPrimaryBirthLane'] = hist(
                f"{r['target_wave']}:{r['lane']}" for r in attacks if r['weapon'] == '11959')
        else:
            # Spell ids are pinned in training-v2-spell-definitions.json:
            # 15=GreaterGhost, 16=GreaterSkeleton. Keep each cast and ordinal so
            # a delayed first summon cannot be misreported as a renewal.
            spells = []
            for uid in sorted({r['uuid'] for r in rows}):
                roles = {r['player']: r['role'] for r in rows if r['uuid'] == uid}
                event_file = root / 'mage/recordings' / f'{uid}.events.json.gz'
                sources.append(event_file)
                events = json.load(gzip.open(event_file, 'rt'))
                waves = {e['nyloWave']['wave']: e['tick'] for e in events if e['type'] == 120}
                boss = next(e['tick'] for e in events if e['type'] == 123)
                ordinal = collections.Counter()
                for e in events:
                    if e['type'] != 11 or e['tick'] >= boss or e['spell']['type'] not in (15, 16):
                        continue
                    player = e['player']['name']
                    ordinal[player] += 1
                    spells.append(dict(uuid=uid, role=roles[player], tick=e['tick'],
                        ordinal=ordinal[player], spell=e['spell']['type'],
                        currentWave=max((w for w, t in waves.items() if t <= e['tick']), default=0),
                        relative21=e['tick'] - waves[21], relative31=e['tick'] - waves[31],
                        relativeBoss=e['tick'] - boss))
            result['human']['thrallCastsFromMage46'] = spells
            sources.append(root / 'training-v2/training-v2-spell-definitions.json')
    trial = 0
    origins = {}
    weapons = [collections.Counter(), collections.Counter()]
    west19 = collections.Counter()
    chin = collections.Counter()
    spawns = collections.defaultdict(list)
    seen_waves = set()
    examples = []
    renewal_requests = []
    clock_base = None
    last_wave = 0
    for line in (args.run / 'server.log').open():
        a = line.strip(' \r\n').split('\t')
        if a[0] == 'NYLO_UTILITY_STATE':
            clock_base = int(a[1]) - int(a[2])
        if a[0] == 'NYLO_UTILITY' and a[3] == 'THRALL_REQUEST' and clock_base is not None:
            local = int(a[1]) - clock_base
            if local >= 0:
                renewal_requests.append(dict(role=int(a[2]), tick=local, wave=last_wave))
        if a[0] == 'NYLO_PLAYERS' and a[1] == '0':
            trial += 1
            origins, seen_waves = {}, set()
        if a[0] != 'NYLO_TRACE':
            continue
        tick, role, wave = int(a[1]), int(a[2]), int(a[3])
        last_wave = wave
        if role == 0 and wave not in seen_waves:
            spawns[wave].append(tick)
            seen_waves.add(wave)
        crabs = {c.split(',')[0]: c.split(',') for c in a[11].split(';') if c}
        for key, crab in crabs.items():
            if key not in origins:
                x, y = int(crab[6]), int(crab[7])
                origins[key] = ('west' if x < 3285 else 'east' if x > 3305
                                else 'south' if y < 4237 else 'split')
        if a[5] != 'true':
            continue
        crab = crabs.get(a[8])
        if wave < 20:
            weapons[role][a[7]] += 1
            if role == 0 and a[7] == 'CHIN' and crab:
                chin[f'{crab[1]}:{origins[a[8]]}'] += 1
        if crab and crab[1] == '19' and crab[4] == 'true' and origins[a[8]] == 'west':
            west19[f'{role}:{wave}:{a[7]}'] += 1
            examples.append(dict(seed=trial, tick=tick, role=role, wave=wave,
                                 age=int(crab[2]), rule=a[9], hp=int(crab[5])))
    result['bot'] = dict(trials=trial, precapWeapons={
        'ranger': dict(weapons[0]), 'mage': dict(weapons[1])},
        precapChinPrimaryBirthLane=dict(chin), west19PrimaryAttacks=dict(west19),
        west19Examples=examples,
        meanSpawnTicks={str(w): mean(v) for w, v in sorted(spawns.items())},
        thrallRenewalRequests=renewal_requests)
    trials = list(csv.DictReader((args.run / 'trials.csv').open()))
    stalls = [[int(v) for v in r['stalls_by_last_spawned_wave'].split(';')] for r in trials]
    result['bot']['meanPrecapStalls'] = mean(int(r['pre_cap_stalls']) for r in trials)
    result['bot']['meanStallsByLastSpawnedWave'] = {
        str(w): mean(r[w] for r in stalls) for w in range(1, 31)}
    result['sources'] = {str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(args.output)


if __name__ == '__main__':
    main()
