#!/usr/bin/env python3
"""Reproduce Nylocas lifecycle evidence from the pinned solo/duo Blert corpus.

Run: python tools/research/nylocas_lifecycle.py
Writes lifecycle-summary.csv, lifecycle-summary.json and lifecycle-waves.csv
under docs/research/nylocas. No network or third-party packages needed.

Ages are zero-based: recorded tick minus spawn tick. Packed current HP is
hitsplat-accumulated recorder HP, not an authoritative server death flag.
Parent relationships are Blert inferences, not independent server metadata.
"""

import argparse
import collections
import csv
import json
from pathlib import Path

from nylocas_replays import load_checked

NATURAL_INTERVALS = [0, 4, 4, 4, 4, 16, 4, 12, 4, 12, 8, 8, 8, 8, 8, 8,
                     4, 12, 8, 12, 16, 8, 12, 8, 8, 8, 4, 8, 4, 4, 4]
STYLES = ['melee', 'ranged', 'magic']


def style(event):
    return STYLES[(event['npc']['id'] - 8342) % 3]


def position(event):
    return event['xCoord'], event['yCoord']


def write_csv(path, rows):
    fields = list(dict.fromkeys(key for row in rows for key in row))
    with path.open('w', newline='') as out:
        writer = csv.DictWriter(out, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)


def analyze(root):
    manifest = json.loads((root / 'manifest.json').read_text())
    rows, runs = [], []
    counters = collections.defaultdict(collections.Counter)
    wave_slots = collections.defaultdict(lambda: collections.defaultdict(collections.Counter))
    for recording in manifest['recordings']:
        raid = load_checked(root, recording['files']['raid'])
        events = load_checked(root, recording['files']['events'])
        room = raid['tobRooms']['nylocas']
        if raid['scale'] not in (1, 2) or raid['mode'] != 11 or room['ticksLost']:
            raise ValueError('Expected complete regular solo/duo recordings')
        npcs = {int(key): npc for key, npc in room['npcs'].items()
                if 8342 <= npc['spawnNpcId'] <= 8353}
        histories = collections.defaultdict(list)
        for event in events:
            if event['type'] in (7, 8, 9) and event.get('npc', {}).get('roomId') in npcs:
                histories[event['npc']['roomId']].append(event)
        children = collections.defaultdict(list)
        for rid, npc in npcs.items():
            history = histories[rid]
            samples = [event for event in history if event['type'] in (7, 8)]
            spawn = samples[0]
            zero_samples = [event for event in samples if event['npc']['hitpoints'] >> 16 == 0]
            zero_tick = zero_samples[0]['tick'] if zero_samples else None
            transitions = [(a, b) for a, b in zip(samples, samples[1:]) if style(a) != style(b)]
            after_zero_moves = [(a, b) for a, b in zip(samples, samples[1:])
                                if zero_tick is not None and b['tick'] > zero_tick
                                and position(a) != position(b)]
            row = dict(uuid=raid['uuid'], scale=raid['scale'], room_id=rid,
                       big=int(npc['nylo']['big']), spawn_type=npc['nylo']['spawnType'],
                       wave=npc['nylo']['wave'], parent_room_id=npc['nylo']['parentRoomId'],
                       spawn_tick=npc['spawnTick'], despawn_tick=npc['deathTick'],
                       age=npc['deathTick'] - npc['spawnTick'], first_zero_tick=zero_tick,
                       zero_to_despawn=npc['deathTick'] - zero_tick if zero_tick is not None else None,
                       last_hp_current=samples[-1]['npc']['hitpoints'] >> 16,
                       last_hp_max=samples[-1]['npc']['hitpoints'] & 65535,
                       postzero_moves=len(after_zero_moves),
                       style_changes=len(transitions),
                       style_change_ages=';'.join(str(b['tick'] - npc['spawnTick'])
                                                  for a, b in transitions))
            if row['parent_room_id']:
                parent = npcs[row['parent_room_id']]
                row.update(parent_despawn_tick=parent['deathTick'],
                           split_delay=npc['spawnTick'] - parent['deathTick'],
                           split_dx=npc['spawnPoint']['x'] - parent['deathPoint']['x'],
                           split_dy=npc['spawnPoint']['y'] - parent['deathPoint']['y'])
                children[row['parent_room_id']].append(row)
                counters['split_offsets'][f"{row['split_dx']},{row['split_dy']}"] += 1
                counters['split_delays'][str(row['split_delay'])] += 1
            kind = 'big' if row['big'] else 'small'
            counters['npc_counts'][kind] += 1
            counters['hp_zero_delays_' + kind][str(row['zero_to_despawn'])] += 1
            if zero_tick is None:
                counters['positive_terminal_ages_' + kind][str(row['age'])] += 1
            else:
                counters['postzero_moves_' + kind][str(row['postzero_moves'])] += 1
            if row['spawn_type'] not in (1, 2, 3):
                counters['split_counts']['all'] += 1
                counters['split_counts']['parent_inferred' if row['parent_room_id'] else 'parent_unknown'] += 1
            else:
                slot = (row['wave'], *position(spawn), row['big'], style(spawn))
                for age in (0, 5, 7, 26):
                    event = next((e for e in samples if e['tick'] == npc['spawnTick'] + age), None)
                    if event is not None:
                        wave_slots[slot][age][style(event)] += 1
                        if event['npc']['hitpoints'] >> 16 > 0:
                            wave_slots[slot][f'{age}_positive'][style(event)] += 1
            for a, b in transitions:
                age = b['tick'] - npc['spawnTick']
                counters['style_change_ages'][str(age)] += 1
                counters['style_change_positive_hp_ages' if b['npc']['hitpoints'] >> 16 > 0
                         else 'style_change_zero_hp_ages'][str(age)] += 1
            rows.append(row)
        for rid, npc in npcs.items():
            if npc['nylo']['big']:
                counters['children_per_big'][str(len(children[rid]))] += 1
                if len(children[rid]) == 2:
                    offsets = sorted((r['split_dx'], r['split_dy']) for r in children[rid])
                    counters['split_pair_offsets'][str(offsets)] += 1
        waves = [event for event in events if event['type'] == 120]
        stalls = [event for event in events if event['type'] == 121]
        cleanup = next(event['tick'] for event in events if event['type'] == 122)
        boss = next(event['tick'] for event in events if event['type'] == 123)
        for a, b in zip(waves, waves[1:]):
            delay = b['tick'] - a['tick'] - NATURAL_INTERVALS[a['nyloWave']['wave']]
            if delay < 0 or delay % 4:
                raise ValueError('Wave interval violates natural intervals plus four-tick checks')
            observed_stalls = sum(a['tick'] < event['tick'] < b['tick'] for event in stalls)
            if delay // 4 != observed_stalls:
                raise ValueError('Wave delay does not equal four ticks per recorded stall')
        for event in waves + stalls:
            tick = event['tick']
            present = [rid for rid, npc in npcs.items() if npc['spawnTick'] <= tick < npc['deathTick']]
            if len(present) != event['nyloWave']['nylosAlive']:
                raise ValueError('Recorder count disagrees with spawn/despawn interval reconstruction')
            counters['event_counts']['wave' if event['type'] == 120 else 'stall'] += 1
            if event['type'] == 120 and len(present) > event['nyloWave']['roomCap']:
                counters['event_counts']['spawn_above_cap'] += 1
            if event['type'] == 121:
                if len(present) < event['nyloWave']['roomCap']:
                    raise ValueError('Stall below recorded cap')
                positive = sum(next(e for e in reversed(histories[rid])
                                    if e['type'] in (7, 8) and e['tick'] <= tick)['npc']['hitpoints'] >> 16 > 0
                               for rid in present)
                if positive < event['nyloWave']['roomCap']:
                    counters['event_counts']['stall_with_positive_hp_count_below_cap'] += 1
        phase = waves[0]['tick'] % 4
        if any(event['tick'] % 4 != phase for event in waves + stalls):
            raise ValueError('Wave/stall ticks do not share one four-tick phase')
        if boss != cleanup + 16 + (phase - cleanup) % 4:
            raise ValueError('Boss spawn does not match cleanup plus16 aligned to wave phase')
        if cleanup != max(npc['deathTick'] for npc in npcs.values()):
            raise ValueError('Cleanup does not equal final wave-nylo despawn')
        runs.append(dict(uuid=raid['uuid'], scale=raid['scale'], wave_phase=phase,
                         first_wave=waves[0]['tick'], stalls=len(stalls),
                         last_wave=waves[-1]['tick'], cleanup=cleanup, boss_spawn=boss,
                         cleanup_to_boss=boss - cleanup,
                         maximum_recorded_count=max(e['nyloWave']['nylosAlive'] for e in waves + stalls)))
    wave_rows = []
    for slot, ages in sorted(wave_slots.items()):
        wave, x, y, big, initial = slot
        row = dict(wave=wave, x=x, y=y, big=big, initial_style=initial)
        for age in (0, 5, 7, 26):
            row[f'age_{age}_style_counts'] = json.dumps(dict(ages[age]), sort_keys=True)
            row[f'age_{age}_positive_hp_style_counts'] = json.dumps(dict(ages[f'{age}_positive']), sort_keys=True)
            if len(ages[f'{age}_positive']) > 1:
                raise ValueError('Different positive-HP styles for one wave slot at one age')
        wave_rows.append(row)
    summary = dict(method='Zero-based ages; first zero is a client HP estimate; parents inferred by Blert',
                   recordings=runs, counters={key: dict(value) for key, value in counters.items()},
                   distinct_lane_spawn_slots=len(wave_rows))
    return rows, summary, wave_rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--corpus', type=Path,
                        default=Path(__file__).resolve().parents[2] / 'docs/research/nylocas')
    args = parser.parse_args()
    rows, summary, waves = analyze(args.corpus)
    write_csv(args.corpus / 'lifecycle-summary.csv', rows)
    write_csv(args.corpus / 'lifecycle-waves.csv', waves)
    (args.corpus / 'lifecycle-summary.json').write_text(json.dumps(summary, indent=2, sort_keys=True) + '\n')
    print(json.dumps(summary['counters'], indent=2, sort_keys=True))


if __name__ == '__main__':
    main()
