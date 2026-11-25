#!/usr/bin/env python3
"""Extract exact first-wave-19-big mage launch rows from the pinned vetted corpus.

No network, builds or gameplay changes. Human animation ages are observations,
not native request ticks or damage attribution. Do not pool overlapping corpora.
"""
import argparse
from collections import Counter
import csv
import hashlib
import json
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=Path(
        'docs/research/nylocas/training-v2/training-v2-mage19-first-big.json'))
    args = parser.parse_args()
    pin = Path('docs/research/nylocas/vetting/summary.json')
    source = Path('docs/research/nylocas/mage/attacks.csv')
    ids = {r['uuid'] for r in json.loads(pin.read_text())['eligiblePinnedRecordings']}
    with source.open(newline='') as stream:
        rows = [r for r in csv.DictReader(stream) if r['uuid'] in ids
                and r['role'] == 'mage' and r['target_wave'] == '19' and r['big'] == 'True']
    recordings = []
    for uid in sorted({r['uuid'] for r in rows}):
        room = sorted((r for r in rows if r['uuid'] == uid), key=lambda r: int(r['tick']))
        first_by_lane = {}
        for row in room:
            first_by_lane.setdefault(row['lane'], row)
        recordings.append(dict(uuid=uid, first=room[0], firstByLane=first_by_lane,
                               allMage19BigLaunchRows=room))
    result = dict(
        scope='46 vetted mage-corpus selected rooms only, both source restrictions and qualifying duo vetting retained. This corpus overlaps ranger42 and is not pooled as independent evidence. CSV attacks are recorded animation/launch observations, not actual damage attribution. Human animation age is normally one tick after native request age.',
        sourceFiles={str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in (pin, source)},
        rooms=len(recordings),
        firstLane=dict(Counter(r['first']['lane'] for r in recordings)),
        firstAnimationAge=dict(sorted(Counter(int(r['first']['age']) for r in recordings).items())),
        firstLaunchPosition=dict(Counter(r['first']['x'] + ',' + r['first']['y'] for r in recordings)),
        selectedStagingPoint=dict(
            x=3296, y=4249,
            exactFirstLaunchWitnesses=[r['uuid'] for r in recordings
                                      if r['first']['x'] == '3296' and r['first']['y'] == '4249'],
            rationale='One tile north of room centre makes east19 earlier than south19 under existing projected movement, unlike3297,4248 which ties their earliest requests. Staging changes no actual19 target scores or packet rules.'),
        recordings=recordings,
    )
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({k: result[k] for k in ('rooms', 'firstLane', 'firstAnimationAge')}, indent=2))
    print(args.output)


if __name__ == '__main__':
    main()
