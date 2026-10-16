#!/usr/bin/env python3
"""Reproduce Nylocas lane fingerprints, target inferences and congestion fixtures.

Run from workspace root: python tools/research/nylocas_targets.py
Uses the checksum-verified solo/duo corpus in docs/research/nylocas. Output targets
are inferred from geometric contact, not NPC target events. The reference guide
provides aggro flags and guide_slot only; physical spawn coordinates come from
recordings. Source: blert-io/blert common/data/nylocas-waves.json at manifest commit.
"""
import argparse
import collections
import csv
import hashlib
import json
from pathlib import Path

from nylocas_replays import load_checked

PILLARS = {'SW': (3290, 4243), 'SE': (3300, 4243),
           'NW': (3290, 4253), 'NE': (3300, 4253)}
LANES = {1: 'west', 2: 'south', 3: 'east'}
STYLES = {0: 'melee', 1: 'ranged', 2: 'magic'}


def adjacent(x, y, pillar, size):
    px, py = pillar
    return (((x + size == px or x == px + 2) and y < py + 2 and y + size > py)
            or ((y + size == py or y == py + 2) and x < px + 2 and x + size > px))


def contact_runs(path, big):
    runs, run = [], None
    for event in path:
        t, x, y = event['tick'], event['xCoord'], event['yCoord']
        contacts = [p for p, point in PILLARS.items()
                    if event['type'] != 9 and event['npc']['hitpoints'] >> 16 > 0
                    and adjacent(x, y, point, 2 if big else 1)]
        if contacts:
            p = contacts[0]
            if run and run[:3] == [p, x, y] and run[4] == t - 1:
                run[4] = t
            else:
                if run:
                    runs.append(run)
                run = [p, x, y, t, t]
        elif run:
            runs.append(run)
            run = None
    if run:
        runs.append(run)
    # Two consecutive positive-HP observations reduce accidental transit contact.
    sustained = [r for r in runs if r[4] > r[3]]
    return sustained[0] if sustained else (runs[0] if runs else None)


def analyze(root):
    manifest = json.loads((root / 'manifest.json').read_text())
    reference = manifest['referenceInputs']['guideWaves']
    wave_bytes = (root / reference['path']).read_bytes()
    if hashlib.sha256(wave_bytes).hexdigest() != reference['sha256']:
        raise ValueError('Published wave reference checksum mismatch')
    waves = json.loads(wave_bytes)
    groups, fixtures, transitions = collections.defaultdict(list), [], []
    intervals, oversized_steps = 0, 0
    big_contacts = collections.defaultdict(set)
    fixture_specs = {'eb3793f3-ce4a-4fcf-8ef9-e2098d39914a': (33313, 49, 61),
                     '1073635f-5326-4bac-a147-8fdf6299f761': (64113, 236, 259),
                     'a37210fe-5737-480f-8fca-518e56205e9b': (34729, 337, 352)}
    for recording in manifest['recordings']:
        raid = load_checked(root, recording['files']['raid'])
        events = load_checked(root, recording['files']['events'])
        npcs = raid['tobRooms']['nylocas']['npcs']
        paths, lane_groups = collections.defaultdict(list), collections.defaultdict(list)
        for event in events:
            if event['type'] in (7, 8, 9):
                paths[event['npc']['roomId']].append(event)
        for npc in npcs.values():
            if npc['type'] == 2 and npc['nylo']['spawnType'] in LANES:
                nylo = npc['nylo']
                lane_groups[nylo['wave'], LANES[nylo['spawnType']]].append(npc)
        for (wave, lane), spawns in lane_groups.items():
            spawns.sort(key=lambda n: -n['spawnPoint']['x' if lane == 'south' else 'y'])
            for coordinate_slot, npc in enumerate(spawns):
                nylo, point = npc['nylo'], npc['spawnPoint']
                updates = [e for e in paths[npc['roomId']] if e['type'] in (7, 8)]
                for a, b in zip(updates, updates[1:]):
                    if b['tick'] == a['tick'] + 1:
                        intervals += 1
                        oversized_steps += max(abs(a['xCoord'] - b['xCoord']), abs(a['yCoord'] - b['yCoord'])) > 1
                specs = waves[wave - 1][lane][:2]
                matches = [i for i, spec in enumerate(specs) if spec
                           and spec['big'] == nylo['big']
                           and spec['rotation'][0] == STYLES[nylo['style']]]
                guide_slot = matches[0] if len(matches) == 1 else coordinate_slot
                spec = specs[guide_slot]
                fingerprint = (wave, lane, STYLES[nylo['style']], nylo['big'], point['x'], point['y'])
                groups[fingerprint].append({
                    'raid': raid['uuid'], 'scale': raid['scale'], 'room_id': npc['roomId'],
                    'guide_slot': guide_slot, 'aggro': spec['aggro'],
                    'contact': contact_runs(paths[npc['roomId']], nylo['big']),
                    'native_aggro': next((e for e in paths[npc['roomId']]
                                          if e['type'] in (7, 8) and 8348 <= e['npc']['id'] <= 8353), None),
                    'spawn_tick': npc['spawnTick']})
        if raid['uuid'] in fixture_specs:
            room_id, start, end = fixture_specs[raid['uuid']]
            # Include all regular-NPC states near the focal actor, preserving
            # spawn/HP/despawn observations so handoff timing is auditable.
            frames = []
            for tick in range(start, end + 1):
                focal = next(e for e in paths[room_id] if e['tick'] == tick)
                x, y = focal['xCoord'], focal['yCoord']
                frame = []
                for other_id, path in paths.items():
                    if npcs[str(other_id)]['type'] != 2:
                        continue
                    for event in path:
                        if event['tick'] == tick and abs(event['xCoord'] - x) <= 4 and abs(event['yCoord'] - y) <= 4:
                            frame.append({'room_id': other_id, 'big': npcs[str(other_id)]['nylo']['big'],
                                          'event_type': event['type'], 'x': event['xCoord'], 'y': event['yCoord'],
                                          'hp': event['npc'].get('hitpoints', 0) >> 16})
                frames.append({'tick': tick, 'npcs': frame})
            fixtures.append({'raid': raid['uuid'], 'scale': raid['scale'],
                             'focal_room_id': room_id, 'frames': frames})
    rows = []
    for key, observations in sorted(groups.items()):
        wave, lane, style, big, x, y = key
        assert len(observations) == 13, (key, len(observations))
        aggro_flags = {o['aggro'] for o in observations}
        assert len(aggro_flags) == 1
        aggro = aggro_flags.pop()
        counts = collections.Counter(o['contact'][0] for o in observations if o['contact'])
        target = 'PLAYER' if aggro else counts.most_common(1)[0][0]
        supporters = [o for o in observations if o['contact'] and o['contact'][0] == target]
        best = max(supporters, key=lambda o: o['contact'][4] - o['contact'][3]) if supporters else observations[0]
        contact = best['contact'] if supporters else None
        tick = contact[3] if contact else ''
        native = [o for o in observations if o['native_aggro']]
        if big and not aggro:
            for observation in observations:
                observed_contact = observation['contact']
                if observed_contact and observed_contact[4] > observed_contact[3]:
                    big_contacts[observed_contact[0]].add(tuple(observed_contact[1:3]))
        if aggro:
            assert native, ('Guide aggro never observed with native aggro ID', key)
        for observation in native:
            event = observation['native_aggro']
            transitions.append(dict(raid=observation['raid'], scale=observation['scale'],
                                    room_id=observation['room_id'], wave=wave, lane=lane,
                                    initial_style=style, big=big, spawn_x=x, spawn_y=y,
                                    initial_aggro=aggro, inferred_initial_target=target,
                                    tick=event['tick'], age=event['tick']-observation['spawn_tick'],
                                    native_id=event['npc']['id'], hp=event['npc']['hitpoints'] >> 16,
                                    x=event['xCoord'], y=event['yCoord']))
        rows.append(dict(wave=wave, lane=lane, initial_style=style, big=big,
                         spawn_x=x, spawn_y=y, guide_slot=best['guide_slot'], aggro=aggro,
                         target=target, replays_seen=len(observations),
                         aggro_transform_replays=len(native),
                         aggro_transform_positive_hp_replays=sum(o['native_aggro']['npc']['hitpoints'] >> 16 > 0 for o in native),
                         replays_with_target_contact=len(supporters),
                         other_first_contact_counts=json.dumps({k: v for k, v in sorted(counts.items()) if k != target}),
                         evidence_raid=best['raid'], evidence_scale=best['scale'],
                         evidence_room_id=best['room_id'], evidence_tick_start=tick,
                         evidence_tick_end=contact[4] if contact else '',
                         evidence_anchor_x=contact[1] if contact else '',
                         evidence_anchor_y=contact[2] if contact else '',
                         evidence_url=f"https://blert.io/raids/tob/{best['raid']}/nylocas"))
    assert len(rows) == 120
    assert sum(not r['aggro'] for r in rows) == 85
    summary = {'lane_npc_instances': len(rows) * 13, 'distinct_fingerprints': len(rows),
               'pillar_fingerprints': 85, 'initial_aggro_fingerprints': 35,
               'consecutive_lane_npc_tick_intervals': intervals,
               'steps_exceeding_one_tile_per_axis': oversized_steps,
               'observed_big_first_stationary_contact_anchors': {p: sorted(v) for p, v in sorted(big_contacts.items())}}
    return rows, fixtures, transitions, summary


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--corpus', type=Path, default=Path(__file__).resolve().parents[2] / 'docs/research/nylocas')
    args = parser.parse_args()
    rows, fixtures, transitions, summary = analyze(args.corpus)
    for filename, selected in [('lane-spawn-targets.csv', rows),
                               ('target-observations.csv', [r for r in rows if not r['aggro']])]:
        with (args.corpus / filename).open('w', newline='') as file:
            writer = csv.DictWriter(file, fieldnames=list(rows[0]))
            writer.writeheader()
            writer.writerows(selected)
    with (args.corpus / 'native-aggro-transitions.csv').open('w', newline='') as file:
        writer = csv.DictWriter(file, fieldnames=list(transitions[0]))
        writer.writeheader()
        writer.writerows(transitions)
    (args.corpus / 'target-summary.json').write_text(json.dumps(summary, indent=2) + '\n')
    (args.corpus / 'movement-fixtures.json').write_text(json.dumps(fixtures, indent=2) + '\n')
    print(f"120 fingerprints across 13 recordings; 85 pillar targets; {len(fixtures)} congestion fixtures")


if __name__ == '__main__':
    main()
