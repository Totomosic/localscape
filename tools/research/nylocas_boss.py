#!/usr/bin/env python3
"""Inspect boss forms/attacks in the checksum-verified pinned Blert corpus.

Run from repository root: python tools/research/nylocas_boss.py
Writes docs/research/nylocas/boss-summary.json. Uses only the standard library.
Boss HP is a recorder estimate with periodic varbit corrections; recorded zero
must not be treated as an exact server lethal-hit tick.
"""

import argparse
import collections
import json
from pathlib import Path

from nylocas_replays import load_checked

BOSS_IDS = {8354, 8355, 8356, 8357}
STYLES = {8354: 'dropping', 8355: 'melee', 8356: 'magic', 8357: 'ranged'}


def analyze(root):
    manifest = json.loads((root / 'manifest.json').read_text())
    counters = collections.defaultdict(collections.Counter)
    recordings = []
    for recording in manifest['recordings']:
        raid = load_checked(root, recording['files']['raid'])
        events = load_checked(root, recording['files']['events'])
        room = raid['tobRooms']['nylocas']
        if raid['mode'] != 11 or raid['scale'] not in (1, 2) or room['ticksLost']:
            raise ValueError('Expected regular solo/duo corpus with zero lost ticks')
        samples = [e for e in events if e['type'] in (7, 8)
                   and e.get('npc', {}).get('id') in BOSS_IDS]
        changes = [samples[0]] + [b for a, b in zip(samples, samples[1:])
                                  if a['npc']['id'] != b['npc']['id']]
        attacks = [e for e in events if e['type'] == 10
                   and e.get('npc', {}).get('id') in BOSS_IDS]
        cleanup = next(e['tick'] for e in events if e['type'] == 122)
        marker = next(e['tick'] for e in events if e['type'] == 123)
        first_wave = next(e['tick'] for e in events if e['type'] == 120)
        spawn, melee, switch = changes[:3]
        death = next(e['tick'] for e in events if e['type'] == 9
                     and e.get('npc', {}).get('id') in BOSS_IDS)
        first_zero = next((e['tick'] for e in samples if e['npc']['hitpoints'] >> 16 == 0), None)
        if spawn['tick'] != marker or marker != cleanup + 16 + (first_wave - cleanup) % 4:
            raise ValueError('Boss spawn does not match prior cleanup/phase formula')
        if spawn['npc']['id'] != 8354 or melee['npc']['id'] != 8355:
            raise ValueError('Boss did not start as dropping then combat melee')
        if melee['tick'] != marker + 2 or switch['tick'] != marker + 11:
            raise ValueError('Unexpected initial form timing')
        counters['cleanup_to_drop'][str(marker - cleanup)] += 1
        counters['drop_to_melee'][str(melee['tick'] - marker)] += 1
        counters['initial_melee_duration'][str(switch['tick'] - melee['tick'])] += 1
        counters['drop_to_first_attack'][str(attacks[0]['tick'] - marker)] += 1
        counters['initial_max_hp'][str(spawn['npc']['hitpoints'] & 65535)] += 1
        counters['drop_anchors'][f"{spawn['xCoord']},{spawn['yCoord']}"] += 1
        counters['first_zero_to_despawn'][str(death - first_zero)] += 1
        phases = []
        for index, (start, end) in enumerate(zip(changes[1:], changes[2:])):
            if index:
                counters['later_style_intervals'][str(end['tick'] - start['tick'])] += 1
                if end['tick'] - start['tick'] != 10:
                    raise ValueError('Unexpected later style interval')
            offsets = [e['tick'] - start['tick'] for e in attacks
                       if start['tick'] <= e['tick'] < end['tick']]
            kind = 'initial' if index == 0 else 'later'
            counters[kind + '_phase_attack_offsets'][str(offsets)] += 1
            phases.append(dict(tick=start['tick'], style=STYLES[start['npc']['id']],
                               duration=end['tick'] - start['tick'], attack_offsets=offsets))
        recordings.append(dict(uuid=raid['uuid'], scale=raid['scale'], cleanup=cleanup,
                               drop=marker, melee=melee['tick'], first_switch=switch['tick'],
                               first_attack=attacks[0]['tick'], first_zero=first_zero,
                               despawn=death, complete_phases=phases))
    return dict(recordings=recordings,
                counters={key: dict(value) for key, value in counters.items()})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--corpus', type=Path,
                        default=Path(__file__).resolve().parents[2] / 'docs/research/nylocas')
    args = parser.parse_args()
    result = analyze(args.corpus)
    (args.corpus / 'boss-summary.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result['counters'], indent=2))


if __name__ == '__main__':
    main()
