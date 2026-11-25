#!/usr/bin/env python3
"""Preserve the native opening Shadow/partner-flight overlap without a build."""
import argparse
import collections
import hashlib
import json
from pathlib import Path


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--run', type=Path, default=Path('.state/simulations/training-v2-live-boosts-baseline-30'))
    ap.add_argument('--output', type=Path, default=Path('docs/research/nylocas/training-v2-shadow-shared-flight.json'))
    args = ap.parse_args()
    path = args.run / 'server.log'
    rooms = []
    for line in path.open():
        a = line.strip(' \r\n').split('\t')
        if a[0] == 'NYLO_TRACE' and a[1:3] == ['0', '0']:
            room = dict(trial=len(rooms), target=None, shots=[], snapshots=[], thrallHits=[])
            rooms.append(room)
        if a[0] == 'NYLO_THRALL' and room['target'] is not None and a[3] == room['target']:
            room['thrallHits'].append(dict(tick=int(a[1]), role=int(a[2]), damage=int(a[4])))
        if a[0] != 'NYLO_TRACE' or int(a[1]) > 24:
            continue
        tick, role = int(a[1]), int(a[2])
        crabs = {v.split(',')[0]: v.split(',') for v in a[11].split(';') if v}
        for key, v in crabs.items():
            if v[1] == '1' and v[3] == 'MAGIC' and v[4] == 'false' and int(v[6]) == 3296:
                room['target'] = key
        target = crabs.get(room['target'])
        if target is None:
            continue
        room['snapshots'].append(dict(tick=tick, role=role, hp=int(target[5]),
            claimedUntil=int(target[8]), ownHitUntil=int(target[9]), dying=target[10] == 'true'))
        if a[5] == 'true' and a[8] == room['target']:
            room['shots'].append(dict(tick=tick, role=role, weapon=a[7]))
    for room in rooms:
        room['firstHpChange'] = next(v for v in room['snapshots'] if v['hp'] < 8)
        room['partnerLaunchesBeforeHpChange'] = [v for v in room['shots']
            if v['role'] == 0 and v['tick'] < room['firstHpChange']['tick']]
    result = dict(run=str(args.run),
        semantics='HP snapshots are planning observations. Partner launches before first observed HP change prove overlapping primary flights; they do not by themselves prove wasted damage.',
        trials=len(rooms),
        roomsWithPartnerLaunchBeforeHpChange=sum(bool(r['partnerLaunchesBeforeHpChange']) for r in rooms),
        firstHpChangeRemainingHp=dict(collections.Counter(r['firstHpChange']['hp'] for r in rooms)),
        openingThrallHits=sum(len(r['thrallHits']) for r in rooms), rooms=rooms,
        sourceSha256=hashlib.sha256(path.read_bytes()).hexdigest())
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(args.output)


if __name__ == '__main__':
    main()
