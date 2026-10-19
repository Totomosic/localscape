#!/usr/bin/env python3
"""Independently summarize death timing in the checksum-pinned Blert corpus.

This does not use the simulator or the existing lifecycle analysis. Recorded
zero HP is a client estimate. Coordinates are observations, not walking flags;
an attack event does not expose its individual hitsplats or which one killed.
"""

import argparse
import collections
import json
from pathlib import Path

from nylocas_replays import load_checked


def position(event):
    return event['xCoord'], event['yCoord']


def analyze(root):
    manifest = json.loads((root / 'manifest.json').read_text())
    counts = collections.defaultdict(collections.Counter)
    examples = {}
    stationary_multihit_bigs = []
    # IDs carried in the recordings: sulphur blades and three scythe variants.
    multi_hit_weapons = {29084, 22325, 25736, 25739}
    for recording in manifest['recordings']:
        raid = load_checked(root, recording['files']['raid'])
        events = load_checked(root, recording['files']['events'])
        room = raid['tobRooms']['nylocas']
        if room['ticksLost'] != 0:
            raise ValueError('Unexpected recording with lost ticks')
        histories = collections.defaultdict(dict)
        attacks = collections.defaultdict(list)
        removals = {}
        for event in events:
            if event['type'] in (7, 8):
                histories[event['npc']['roomId']][event['tick']] = event
            elif event['type'] == 9:
                removals[event['npc']['roomId']] = event['tick']
            elif event['type'] == 5 and 'target' in event.get('attack', {}):
                attacks[event['attack']['target']['roomId']].append(event)
        for key, npc in room['npcs'].items():
            if not 8342 <= npc['spawnNpcId'] <= 8353:
                continue
            rid = int(key)
            if removals[rid] != npc['deathTick']:
                raise ValueError('Despawn event and metadata disagree')
            samples = histories[rid]
            ordered = [samples[tick] for tick in sorted(samples)]
            size = 'big' if npc['nylo']['big'] else 'small'
            counts['births'][size] += 1
            zero = next((e for e in ordered if e['npc']['hitpoints'] >> 16 == 0), None)
            if zero is None:
                age = removals[rid] - npc['spawnTick']
                counts['no_recorded_zero_removal_age_' + size][age] += 1
                continue
            tick = zero['tick']
            delay = removals[rid] - tick
            prior = samples.get(tick - 1)
            moved_on_zero = prior is not None and position(prior) != position(zero)
            later = [e for e in ordered if e['tick'] >= tick]
            later_steps = sum(position(a) != position(b) for a, b in zip(later, later[1:]))
            counts['zero_to_removal_' + size][delay] += 1
            counts['later_steps_' + size][later_steps] += 1
            category = f'{size}:moved_on_zero={moved_on_zero}:moved_after_zero={later_steps > 0}'
            counts[category][delay] += 1
            trace = {
                'uuid': raid['uuid'], 'room_id': rid,
                'url': recording['url'], 'zero_tick': tick,
                'removal_tick': removals[rid], 'delay': delay,
                'samples': [
                    {'tick': e['tick'], 'x': e['xCoord'], 'y': e['yCoord'],
                     'hp': e['npc']['hitpoints'] >> 16}
                    for e in ordered if tick - 4 <= e['tick'] <= tick + 1
                ],
            }
            examples.setdefault(f'{category}:delay={delay}', trace)
            window = [samples.get(t) for t in range(tick - 4, tick + 1)]
            stable = all(window) and len({position(e) for e in window}) == 1
            for attack in attacks[rid]:
                weapon = attack['attack'].get('weapon', {}).get('id')
                if attack['tick'] != tick - 1 or weapon not in multi_hit_weapons:
                    continue
                counts[f'attack_previous_tick:{size}:weapon={weapon}'][delay] += 1
                if size == 'big' and stable:
                    stationary_multihit_bigs.append({
                        **trace, 'weapon_id': weapon,
                        'attack_tick': attack['tick'],
                        'hp_before': prior['npc']['hitpoints'] >> 16,
                    })
        counts['recordings']['total'] += 1
    return {
        'method': __doc__.strip(),
        'counts': {key: dict(sorted(value.items())) for key, value in sorted(counts.items())},
        'examples': examples,
        'stationary_multihit_bigs': stationary_multihit_bigs,
        'limitations': [
            'First recorded zero HP need not be the lethal server tick.',
            'Five unchanged anchors do not expose the server walking flag.',
            'Multi-hit weapon use does not prove lethal-first hit order.',
            'No individual hitsplat event or death animation event is saved here.',
        ],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--corpus', type=Path,
                        default=Path(__file__).resolve().parents[2] / 'docs/research/nylocas')
    parser.add_argument('--output', type=Path,
                        default=Path(__file__).resolve().parents[2] /
                        'docs/research/nylocas/death-timing-review.json')
    args = parser.parse_args()
    result = analyze(args.corpus)
    args.output.write_text(json.dumps(result, indent=2, sort_keys=True) + '\n')
    print(json.dumps(result['counts'], indent=2, sort_keys=True))


if __name__ == '__main__':
    main()
