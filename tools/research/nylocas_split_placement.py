#!/usr/bin/env python3
"""Compare recorded split birth anchors with a fixed SW/NE pair.

Uses checksum-pinned Blert responses, independently of simulator execution.
The baseline is NylocasBehaviour.split's parent+(0,0)/(1,1) strategy. Compare
against the FINAL parent anchor, so a parent's last step cannot explain a
remaining discrepancy. Birth coordinates are first observations, not proof of
the internal creation tile. Parent IDs are recorder inferences.
"""

import argparse
import collections
import json
from pathlib import Path

from nylocas_replays import load_checked


def position(event):
    return event['xCoord'], event['yCoord']


def point(value):
    return value['x'], value['y']


def location(anchor):
    x, y = anchor
    if x < 3290:
        return 'west_lane'
    if x > 3301:
        return 'east_lane'
    if y < 4243:
        return 'south_lane'
    return 'arena'


def sample(event):
    return dict(tick=event['tick'], anchor=position(event),
                hp=event['npc']['hitpoints'] >> 16)


def analyze(root):
    manifest = json.loads((root / 'manifest.json').read_text())
    counts = collections.Counter()
    pairs = collections.Counter()
    observations = []
    for recording in manifest['recordings']:
        raid = load_checked(root, recording['files']['raid'])
        events = load_checked(root, recording['files']['events'])
        room = raid['tobRooms']['nylocas']
        if raid['mode'] != 11 or raid['scale'] not in (1, 2) or room['ticksLost']:
            raise ValueError('Expected complete normal-mode solo/duo room')
        npcs = {int(k): v for k, v in room['npcs'].items()
                if 8342 <= v['spawnNpcId'] <= 8353}
        histories = collections.defaultdict(dict)
        births, removals = {}, {}
        children = collections.defaultdict(list)
        for event in events:
            if event['type'] not in (7, 8, 9):
                continue
            rid = event['npc']['roomId']
            if rid not in npcs:
                continue
            if event['type'] == 9:
                removals[rid] = event
            else:
                histories[rid][event['tick']] = event
                if event['type'] == 7:
                    births[rid] = event
        for rid, npc in npcs.items():
            if npc['nylo']['parentRoomId']:
                children[npc['nylo']['parentRoomId']].append(rid)
        for rid, npc in npcs.items():
            if not npc['nylo']['big']:
                continue
            counts['bigs'] += 1
            path = [e for _, e in sorted(histories[rid].items())]
            anchor = position(path[-1])
            removal = removals[rid]
            if (anchor != point(npc['deathPoint']) or position(removal) != anchor
                    or removal['tick'] != npc['deathTick']):
                raise ValueError('Parent final snapshot/removal/metadata disagree')
            group = location(anchor)
            counts[group + ':bigs'] += 1
            if len(children[rid]) != 2:
                counts['incomplete_parent_associations'] += 1
                continue
            child_events = [births[c] for c in children[rid]]
            for child, event in zip(children[rid], child_events):
                if (event['tick'] != removal['tick']
                        or point(npcs[child]['spawnPoint']) != position(event)
                        or npcs[child]['spawnTick'] != event['tick']):
                    raise ValueError('Child event/metadata or parent timing disagree')
            observed = sorted(position(e) for e in child_events)
            expected = [anchor, (anchor[0] + 1, anchor[1] + 1)]
            offsets = tuple((x - anchor[0], y - anchor[1]) for x, y in observed)
            matches = observed == expected
            counts['complete_pairs'] += 1
            counts['matching_pairs' if matches else 'mismatching_pairs'] += 1
            counts[group + (':matches' if matches else ':mismatches')] += 1
            pairs[group, offsets] += 1
            if matches and group == 'arena':
                continue
            zero = next((e for e in path if e['npc']['hitpoints'] >> 16 == 0), None)
            start = zero['tick'] - 2 if zero else removal['tick'] - 9
            simultaneous = [key for key, n in npcs.items()
                            if n['nylo']['big'] and n['deathTick'] == removal['tick']]
            observations.append(dict(
                uuid=raid['uuid'], url=recording['url'], room_id=rid,
                wave=npc['nylo']['wave'], origin_lane=npc['nylo']['spawnType'],
                final_location=group, final_anchor=anchor,
                zero_tick=zero['tick'] if zero else None,
                removal_tick=removal['tick'], expected_pair=expected,
                observed_pair=observed, offsets=offsets, matches=matches,
                simultaneous_big_removals=simultaneous,
                parent_samples=[sample(e) for e in path if e['tick'] >= start],
                children=[dict(room_id=e['npc']['roomId'], birth=sample(e),
                               next_sample=(sample(histories[e['npc']['roomId']][e['tick'] + 1])
                                            if e['tick'] + 1 in histories[e['npc']['roomId']]
                                            else None)) for e in child_events],
            ))
        counts['recordings'] += 1
    return dict(
        method=__doc__.strip(), counts=dict(sorted(counts.items())),
        pair_offsets=[dict(location=group, offsets=offsets, parents=count)
                      for (group, offsets), count in sorted(pairs.items())],
        observations=observations,
        limitations=[
            'Selected 13-recording normal-mode solo/duo corpus; not all modes or scales.',
            'Parent IDs are inferred; simultaneous nearby big deaths can be ambiguous.',
            'First recorded child anchor can include movement during its birth tick.',
            'Recorded zero HP does not identify the exact lethal server phase.',
            'The fixed-pair baseline is a strategy comparison, not simulator execution.',
        ],
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--corpus', type=Path,
                        default=Path(__file__).resolve().parents[2] / 'docs/research/nylocas')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = analyze(args.corpus)
    output = args.output or args.corpus / 'split-placement-observations.json'
    output.write_text(json.dumps(result, indent=2, sort_keys=True) + '\n')
    print(json.dumps({k: result[k] for k in ('counts', 'pair_offsets')}, indent=2))


if __name__ == '__main__':
    main()
