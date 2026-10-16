#!/usr/bin/env python3
"""Check entry pauses and big arrivals below occupied north-pillar faces.

Uses the checksum-verified pinned corpus. Entry means the first SW anchor in
x3290..3301, y4243..4254, matching the lane entry coordinates in the simulator.
Positive recorded HP is an observational filter, not a server lifecycle flag.
Classification comes from the existing fingerprint CSV, cross-checked against
the checksum-pinned guide wave flags. No server target/attack events are inferred.
"""
import argparse
import collections
import csv
import hashlib
import json
from pathlib import Path

from nylocas_replays import load_checked


def position(event):
    return event['xCoord'], event['yCoord']


def sample(event):
    return {'tick': event['tick'], 'anchor': list(position(event)),
            'npc_id': event['npc']['id'], 'hp': event['npc']['hitpoints'] >> 16}


def analyze(root):
    manifest = json.loads((root / 'manifest.json').read_text())
    reference = manifest['referenceInputs']['guideWaves']
    raw = (root / reference['path']).read_bytes()
    if hashlib.sha256(raw).hexdigest() != reference['sha256']:
        raise ValueError('Published wave reference checksum mismatch')
    waves = json.loads(raw)
    fingerprints = {}
    with (root / 'lane-spawn-targets.csv').open() as source:
        for row in csv.DictReader(source):
            aggro = row['aggro'] == 'True'
            wave, lane = int(row['wave']), row['lane']
            spec = waves[wave - 1][lane][int(row['guide_slot'])]
            if spec['aggro'] != aggro or spec['big'] != (row['big'] == 'True'):
                raise ValueError('Fingerprint classification differs from pinned guide')
            key = (wave, lane, row['initial_style'], row['big'] == 'True',
                   int(row['spawn_x']), int(row['spawn_y']))
            fingerprints[key] = aggro
    counts = collections.Counter()
    entries, arrivals = [], []
    for recording in manifest['recordings']:
        raid = load_checked(root, recording['files']['raid'])
        if raid['tobRooms']['nylocas']['ticksLost'] != 0:
            raise ValueError('Recording has reported lost ticks')
        paths, frames, births = collections.defaultdict(list), collections.defaultdict(dict), {}
        for event in load_checked(root, recording['files']['events']):
            if event['type'] not in (7, 8) or not 8342 <= event['npc']['id'] <= 8353:
                continue
            rid = event['npc']['roomId']
            paths[rid].append(event)
            if event['type'] == 7:
                births[rid] = event
            if event['npc']['hitpoints'] >> 16 > 0:
                frames[event['tick']][rid] = event
        for rid, birth in births.items():
            nylo = birth['npc']['nylo']
            lane = {1: 'west', 2: 'south', 3: 'east'}.get(nylo['spawnType'])
            if lane is None:
                continue
            key = (nylo['wave'], lane, ['melee', 'ranged', 'magic'][nylo['style']],
                   nylo['big'], *position(birth))
            kind = 'aggro' if fingerprints[key] else 'pillar'
            path = paths[rid]
            entry = next((i for i, e in enumerate(path)
                          if 3290 <= e['xCoord'] <= 3301 and 4243 <= e['yCoord'] <= 4254), None)
            if entry is None or entry + 1 >= len(path):
                continue
            first, second = path[entry:entry + 2]
            if second['tick'] != first['tick'] + 1 or any(
                    e['npc']['hitpoints'] >> 16 <= 0 for e in (first, second)):
                continue
            outcome = 'stationary' if position(first) == position(second) else 'moved'
            size = 'big' if nylo['big'] else 'small'
            counts[kind, size, outcome] += 1
            entries.append({'raid': recording['uuid'], 'room_id': rid,
                            'wave': nylo['wave'], 'lane': lane, 'kind': kind,
                            'size': size, 'outcome': outcome,
                            'samples': [sample(e) for e in path[max(0, entry - 1):entry + 3]]})
        # A big arrives while a small already holds its northern footprint row.
        # Both then remain at those distinct anchors with positive recorded HP
        # for at least three ticks. Require the small to hold on the prior tick.
        for tick, frame in frames.items():
            bigs = [(r, e) for r, e in frame.items()
                    if births[r]['npc']['nylo']['big'] and e['yCoord'] == 4251
                    and e['xCoord'] in (3289, 3290, 3291, 3299, 3300, 3301)
                    and e['npc']['id'] <= 8347]
            smalls = [(r, e) for r, e in frame.items()
                      if not births[r]['npc']['nylo']['big'] and e['yCoord'] == 4252
                      and e['xCoord'] in (3290, 3291, 3300, 3301)]
            for big, be in bigs:
                previous = frames.get(tick - 1, {}).get(big)
                if previous is None or position(previous) == position(be):
                    continue
                for small, se in smalls:
                    if not be['xCoord'] <= se['xCoord'] < be['xCoord'] + 2:
                        continue
                    before = frames.get(tick - 1, {}).get(small)
                    if before is None or position(before) != position(se):
                        continue
                    held = 0
                    while all(r in frames.get(tick + held, {}) and
                              position(frames[tick + held][r]) == position(e)
                              for r, e in ((big, be), (small, se))):
                        held += 1
                    if held < 3:
                        continue
                    arrivals.append({'raid': recording['uuid'], 'big_room_id': big,
                                     'small_room_id': small,
                                     'pillar': 'NW' if be['xCoord'] < 3295 else 'NE',
                                     'arrival_tick': tick, 'hold_through': tick + held - 1,
                                     'big_before': sample(previous), 'big_at_arrival': sample(be),
                                     'small_at_arrival': sample(se)})
    # Preserve every aggro movement counterexample plus one representative
    # observation per category; the raw recordings retain all other frames.
    examples, represented = [], set()
    for entry in entries:
        category = entry['kind'], entry['size'], entry['outcome']
        if category not in represented or (entry['kind'] == 'aggro' and entry['outcome'] == 'moved'):
            examples.append(entry)
        represented.add(category)
    return {'entry_counts': [{'kind': k, 'size': s, 'outcome': o, 'count': n}
                             for (k, s, o), n in sorted(counts.items())],
            'entry_examples': examples, 'north_pillar_arrivals': arrivals}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--corpus', type=Path,
                        default=Path(__file__).resolve().parents[2] / 'docs/research/nylocas')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = analyze(args.corpus)
    output = args.output or args.corpus / 'entry-contact-observations.json'
    output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({'entry_counts': result['entry_counts'],
                      'north_pillar_arrival_pairs': len(result['north_pillar_arrivals'])}, indent=2))


if __name__ == '__main__':
    main()
