#!/usr/bin/env python3
"""Offline ranger strategy evidence from checksum-pinned Cutely meows replays.

Run: python tools/research/nylocas_ranger.py
Outputs attacks.csv (both players), waves.csv (per-room wave/first action),
positions.csv (ranger tick positions), and summary.json in the ranger corpus.
Attack events describe observed animations, not hits or successful kills.
"""

import bisect
import collections
import csv
import gzip
import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2] / 'docs/research/nylocas/ranger'
STYLE = {0: 'melee', 1: 'range', 2: 'mage'}
LANE = {1: 'west', 2: 'south', 3: 'east'}


def load_checked(entry):
    raw = gzip.decompress((ROOT / entry['path']).read_bytes())
    if hashlib.sha256(raw).hexdigest() != entry['sha256']:
        raise ValueError('Checksum mismatch: ' + entry['path'])
    return json.loads(raw)


def write_csv(name, rows):
    with (ROOT / name).open('w') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)


def main():
    manifest = json.loads((ROOT / 'manifest.json').read_text())
    search = json.loads((ROOT / 'search-results.json').read_text())
    assert len(search) == len(manifest['recordings']) < 100
    assert {r['uuid'] for r in search} == {r['uuid'] for r in manifest['recordings']}
    assert all(r['splits']['42']['accurate'] and r['splits']['42']['ticks'] <= 326 for r in search)
    definitions = manifest['attackDefinitions']
    raw_definitions = (ROOT / definitions['path']).read_bytes()
    assert hashlib.sha256(raw_definitions).hexdigest() == definitions['sha256']
    by_attack = {d['protoId']: d for d in json.loads(raw_definitions)}
    for attack_type, weapon, cooldown in [(12, 12926, 2), (16, 11959, 3),
                                           (19, 13652, 4), (20, 13652, 4),
                                           (54, 20997, 5), (104, 31113, 3)]:
        assert weapon in by_attack[attack_type]['weaponIds']
        assert by_attack[attack_type]['cooldown'] == cooldown
    attacks, wave_rows, positions, rooms = [], [], [], []
    for recording in manifest['recordings']:
        raid = load_checked(recording['files']['raid'])
        events = load_checked(recording['files']['events'])
        room = raid['tobRooms']['nylocas']
        assert raid['scale'] == 2 and raid['mode'] == 11
        assert raid['splits']['42'] <= manifest['cutoffTicks']
        ranger = next(p['username'] for p in raid['party']
                      if p['currentUsername'].casefold() == 'cutely meows')
        waves = sorted((e['tick'], e['nyloWave']['wave']) for e in events
                       if e['type'] == 120)
        assert [w for _, w in waves] == list(range(1, 32))
        ticks = [t for t, _ in waves]
        boss = next(e['tick'] for e in events if e['type'] == 123)
        assert boss == raid['splits']['42']
        metadata = room['npcs']
        for meta in metadata.values():
            if 'nylo' not in meta or not 8342 <= meta['spawnNpcId'] <= 8353:
                continue
            born_index = bisect.bisect_right(ticks, meta['spawnTick']) - 1
            assert born_index >= 0
            assert meta['nylo']['wave'] == waves[born_index][1]
        states = {}
        for event in events:
            if event['type'] in (7, 8):
                states.setdefault(event['tick'], {})[event['npc']['roomId']] = event
        # All updates of the same tick are available for the animation observation.
        current, local_attacks, local_positions = {}, [], []
        by_tick = collections.defaultdict(list)
        for event in events:
            by_tick[event['tick']].append(event)
        for tick in sorted(by_tick):
            current.update(states.get(tick, {}))
            wave_index = bisect.bisect_right(ticks, tick) - 1
            wave = waves[wave_index][1] if wave_index >= 0 else 0
            for event in by_tick[tick]:
                if event['type'] == 4 and event['player']['name'] == ranger and tick < boss:
                    row = dict(uuid=raid['uuid'], tick=tick, wave=wave,
                               x=event['xCoord'], y=event['yCoord'],
                               off_cooldown=event['player'].get('offCooldownTick', ''))
                    positions.append(row)
                    local_positions.append(row)
                if event['type'] != 5 or tick >= boss:
                    continue
                attack = event['attack']
                rid = attack.get('target', {}).get('roomId')
                meta = metadata.get(str(rid))
                if not meta or 'nylo' not in meta:
                    continue
                nylo = meta['nylo']
                if not 8342 <= meta['spawnNpcId'] <= 8353:
                    continue
                state = current.get(rid, {})
                npc = state.get('npc', {})
                # Updates may omit nylo entirely or only include changed fields.
                style = npc.get('nylo', {}).get('style')
                if style is None and 'id' in npc:
                    style = (npc['id'] - 8342) % 3
                lane = LANE.get(nylo.get('spawnType'), 'split')
                point = meta['spawnPoint']
                slot = ('N' if point['y'] == 4249 else 'S') if lane in ('west', 'east') else (
                    'W' if point['x'] == 3295 else 'E') if lane == 'south' else ''
                target = f"w{nylo['wave']} {lane}{('-' + slot) if slot else ''} " + (
                    'big ' if nylo['big'] else 'small ') + STYLE[nylo['style']]
                row = dict(uuid=raid['uuid'], player=event['player']['name'],
                           role='ranger' if event['player']['name'] == ranger else 'mage',
                           tick=tick, wave=wave, offset=tick - ticks[wave_index],
                           x=event['xCoord'], y=event['yCoord'],
                           attack_type=attack['type'], weapon=attack.get('weapon', {}).get('id', ''),
                           target_id=rid, target=target, target_wave=nylo['wave'],
                           lane=lane, slot=slot, big=nylo['big'],
                           initial_style=STYLE[nylo['style']], current_style=STYLE.get(style, ''),
                           spawn_tick=meta['spawnTick'], age=tick - meta['spawnTick'],
                           parent=nylo.get('parentRoomId', 0),
                           target_x=state.get('xCoord', ''), target_y=state.get('yCoord', ''),
                           hp=(npc['hitpoints'] >> 16) if 'hitpoints' in npc else '',
                           distance=attack.get('distanceToTarget', ''))
                attacks.append(row)
                local_attacks.append(row)
        for tick, wave in waves:
            end = ticks[wave] if wave < 31 else boss
            actions = [a for a in local_attacks if a['role'] == 'ranger' and a['wave'] == wave]
            coords = [(p['x'], p['y']) for p in local_positions if tick <= p['tick'] < end]
            first = actions[0] if actions else {}
            wave_rows.append(dict(uuid=raid['uuid'], wave=wave, tick=tick, duration=end-tick,
                                  first_tick=first.get('tick', ''), first_target=first.get('target', ''),
                                  first_weapon=first.get('weapon', ''), first_offset=first.get('offset', ''),
                                  first_x=first.get('x', ''), first_y=first.get('y', ''),
                                  modal_tile=str(collections.Counter(coords).most_common(1)[0][0]) if coords else '',
                                  ranger_attacks=len(actions)))
        rooms.append(dict(uuid=raid['uuid'], ranger=ranger, boss_spawn=boss,
                          ticks_lost=room['ticksLost'], stalled_waves=room.get('stalledWaves', []),
                          wave29=ticks[28], wave31=ticks[30],
                          ranger_attacks=sum(a['role'] == 'ranger' for a in local_attacks)))
    write_csv('attacks.csv', attacks)
    write_csv('waves.csv', wave_rows)
    write_csv('positions.csv', positions)
    summary = dict(recordings=len(rooms), rooms=rooms, waves={})
    for wave in range(1, 32):
        rows = [r for r in wave_rows if r['wave'] == wave]
        actions = [a for a in attacks if a['wave'] == wave and a['role'] == 'ranger']
        def counts(values):
            return collections.Counter(values).most_common()
        summary['waves'][wave] = dict(
            first_actions=counts((r['first_target'], r['first_weapon']) for r in rows),
            targets_by_rooms=counts(target for target in sorted(set(a['target'] for a in actions))
                                   for _ in {a['uuid'] for a in actions if a['target'] == target}),
            first_tiles=counts((r['first_x'], r['first_y']) for r in rows),
            modal_tiles=counts(r['modal_tile'] for r in rows))
    (ROOT / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
    ranger_actions = [a for a in attacks if a['role'] == 'ranger']
    # Independent predicates for the named rotation anchors. These validate
    # observations, not the counterfactual performance of the proposed policy.
    rules = [
        ('Opening west bow', 1, 'west', False, 'range', 20997, 1, 1),
        ('Second east bow', 2, 'east', False, 'range', 20997, 3, 3),
        ('South chin from wave 3', 3, 'south', False, 'range', 11959, 4, 4),
        ('South melee scratch', 2, 'south', False, 'melee', 13652, 11, 11),
        ('Wave 5 west big blowpipe', 5, 'west', True, 'range', 12926, 3, 5),
        ('Wave 7 south big bow', 7, 'south', True, 'range', 20997, 1, 4),
        ('Wave 6 east big claw spec', 6, 'east', True, 'melee', 13652, 9, 14),
        ('Wave 8 east chin', 8, 'east', False, 'range', 11959, 1, 3),
        ('Wave 9 west big blowpipe', 9, 'west', True, 'range', 12926, 6, 12),
        ('Wave 10 lane chins', 10, None, False, 'range', 11959, 1, 12),
        ('Wave 13 west range', 13, 'west', False, 'range', 12926, 0, 51),
        ('Wave 14 south range', 14, 'south', False, 'range', 12926, 0, 51),
        ('Wave 15 east range', 15, 'east', False, 'range', 12926, 0, 51),
        ('Wave 16 east range after flicker', 16, 'east', False, 'mage', 12926, 7, 25),
        ('Wave 16 south range after flicker', 16, 'south', False, 'melee', 12926, 7, 25),
        ('Wave 16 west range after flicker', 16, 'west', False, 'range', 12926, 7, 25),
        ('Wave 18 south big range', 18, 'south', True, 'range', 12926, 0, 51),
        ('Wave 18 east big range', 18, 'east', True, 'range', 12926, 0, 51),
        ('Wave 19 west big Eye', 19, 'west', True, 'mage', 31113, 0, 25),
        ('Wave 20 west bow flicker', 20, 'west', True, 'melee', 20997, 6, 6),
        ('Wave 21 east chin', 21, 'east', False, 'range', 11959, 1, 1),
        ('Wave 23 south bow', 23, 'south', True, 'range', 20997, 1, 2),
        ('Wave 23 west north ranged flicker', 23, 'west', False, 'mage', 12926, 6, 6),
        ('Wave 23 west south ranged flicker', 23, 'west', False, 'range', 12926, 8, 10),
        ('Wave 24 west big ranged window', 24, 'west', True, 'range', None, 1, 4),
        ('Wave 25 south big range', 25, 'south', True, 'range', 12926, 0, 25),
        ('Wave 27 south big ranged flicker', 27, 'south', True, 'melee', 12926, 7, 12),
        ('Wave 28 south east ranged flicker', 28, 'south', False, 'melee', 12926, 6, 6),
        ('Wave 28 south west ranged flicker', 28, 'south', False, 'mage', 12926, 8, 8),
        ('Wave 29 west north ranged flicker', 29, 'west', False, 'melee', 12926, 6, 6),
        ('Wave 30 west big range', 30, 'west', True, 'range', 12926, 1, 15),
        ('Wave 30 south east chin on ranged flicker', 30, 'south', False, 'melee', 11959, 6, 6),
        ('Wave 31 west north ranged flicker', 31, 'west', False, 'melee', 12926, 6, 6),
        ('Wave 31 south ranged chins', 31, 'south', False, None, 11959, 8, 15),
    ]
    validation = []
    for label, tw, lane, big, initial, weapon, low, high in rules:
        matching_style = 'mage' if weapon == 31113 else 'melee' if weapon == 13652 else 'range'
        found = [a for a in ranger_actions if a['target_wave'] == tw
                 and (lane is None or a['lane'] == lane) and a['big'] == big
                 and (initial is None or a['initial_style'] == initial)
                 and (weapon is None or a['weapon'] == weapon) and low <= a['age'] <= high
                 and a['current_style'] == matching_style
                 and (label != 'Wave 6 east big claw spec' or a['attack_type'] == 20)]
        ids = {a['uuid'] for a in found}
        validation.append(dict(rule=label, rooms=len(ids), denominator=len(rooms),
                               predicate=dict(target_wave=tw, lane=lane, big=big,
                                              initial_style=initial, weapon=weapon,
                                              current_style=matching_style,
                                              min_age=low, max_age=high),
                               age_counts=dict(collections.Counter(a['age'] for a in found)),
                               last_wave_counts=dict(collections.Counter(a['wave'] for a in found)),
                               examples=found[:3], missing_rooms=[r['uuid'] for r in rooms if r['uuid'] not in ids]))
    (ROOT / 'validation.json').write_text(json.dumps(validation, indent=2) + '\n')
    report = ['| Observed rotation anchor | Target age tested | Rooms matching | Example |',
              '| --- | --- | --- | --- |']
    for result in validation:
        rule = result['predicate']
        age = str(rule['min_age']) if rule['min_age'] == rule['max_age'] else (
            f"{rule['min_age']}–{rule['max_age']}")
        example = result['examples'][0]
        url = f"https://blert.io/raids/tob/{example['uuid']}/nylocas"
        report.append(f"| {result['rule']} | {age} ticks | {result['rooms']}/{len(rooms)} | "
                      f"[{example['uuid'][:6]}]({url}), tick {example['tick']}, "
                      f"last wave {example['wave']} |")
    (ROOT / 'validation.md').write_text('\n'.join(report) + '\n')
    print(f"Validated {len(rooms)} rooms; {len(attacks)} attacks; {len(positions)} ranger positions")


if __name__ == '__main__':
    main()
