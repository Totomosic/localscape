#!/usr/bin/env python3
"""Reproduce occupancy evidence from the pinned public Blert recordings.

Run from the workspace root:
    python tools/research/nylocas_replays.py

No network or third-party packages required. Each recording is checked against
its original uncompressed SHA-256 before analysis. NPC_DEATH is despawn, not
the lethal hit. Pair-ticks count repeated observations, not independent trials.
"""

import argparse
import collections
import gzip
import hashlib
import itertools
import json
from pathlib import Path


def load_checked(root, entry):
    data = gzip.decompress((root / entry['path']).read_bytes())
    actual = hashlib.sha256(data).hexdigest()
    if actual != entry['sha256']:
        raise ValueError(f"Snapshot checksum mismatch: {entry['path']}")
    return json.loads(data)


def analyze(root):
    manifest = json.loads((root / 'manifest.json').read_text())
    totals = collections.Counter()
    recordings = []
    for recording in manifest['recordings']:
        raid = load_checked(root, recording['files']['raid'])
        events = load_checked(root, recording['files']['events'])
        if raid['scale'] not in (1, 2) or raid['mode'] != 11:
            raise ValueError('Corpus must contain only regular solo/duo raids')
        if raid['tobRooms']['nylocas']['ticksLost'] != 0:
            raise ValueError('Corpus contains reported lost ticks')
        frames = collections.defaultdict(dict)
        spawns = {}
        counts = collections.Counter()
        for event in events:
            if event['type'] not in (7, 8):
                continue
            npc = event['npc']
            if not 8342 <= npc['id'] <= 8353:
                continue
            rid = npc['roomId']
            if event['type'] == 7:
                spawns[rid] = event
                # parentRoomId can be unknown: use lane/spawn classification.
                counts['big_spawns' if npc['nylo']['big'] else 'small_spawns'] += 1
                counts['lane_spawns' if npc['nylo']['spawnType'] in (1, 2, 3)
                       else 'split_spawns'] += 1
            # Health observations can lag a hit. This is an operational filter,
            # not proof of life at every sub-tick phase.
            if npc['hitpoints'] >> 16 > 0:
                frames[event['tick']][rid] = event
        for tick, frame in frames.items():
            for a, b in itertools.combinations(sorted(frame), 2):
                ea, eb = frame[a], frame[b]
                sa = 2 if spawns[a]['npc']['nylo']['big'] else 1
                sb = 2 if spawns[b]['npc']['nylo']['big'] else 1
                if sa == sb == 1:
                    continue
                x, y = ea['xCoord'], ea['yCoord']
                u, v = eb['xCoord'], eb['yCoord']
                # Restrict BOTH anchors to the arena. This excludes lane
                # stacking, but includes split births and aggressive NPCs.
                if not (3290 <= x <= 3301 and 4243 <= y <= 4254
                        and 3290 <= u <= 3301 and 4243 <= v <= 4254):
                    continue
                if not (x < u + sb and u < x + sa
                        and y < v + sb and v < y + sa):
                    continue
                if (x, y) == (u, v):
                    counts['same_anchor_pair_ticks'] += 1
                    continue
                counts['distinct_anchor_overlap_pair_ticks'] += 1
                def remains(rid, position):
                    for adjacent in (tick - 1, tick + 1):
                        other = frames.get(adjacent, {}).get(rid)
                        if other is None or (other['xCoord'], other['yCoord']) != position:
                            return False
                    return True
                if remains(a, (x, y)) and remains(b, (u, v)):
                    counts['three_tick_stationary_overlap_pair_ticks'] += 1
        counts['recordings'] = 1
        counts['solo' if raid['scale'] == 1 else 'duo'] = 1
        totals.update(counts)
        recordings.append({'uuid': recording['uuid'], 'counts': dict(counts)})
    return {'method': 'See script docstring and occupancy evidence report',
            'totals': dict(totals), 'recordings': recordings}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--corpus', type=Path,
                        default=Path(__file__).resolve().parents[2] / 'docs/research/nylocas')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = json.dumps(analyze(args.corpus), indent=2, sort_keys=True) + '\n'
    if args.output:
        args.output.write_text(result)
    else:
        print(result, end='')


if __name__ == '__main__':
    main()
