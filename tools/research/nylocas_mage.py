#!/usr/bin/env python3
"""Rebuild mage evidence offline from checksum-pinned public Blert recordings.

Attack animations are observations, not successful hits or all AoE victims.
The primary target's birth identity and attack-time style are kept separate.
"""

import bisect
import collections
import csv
import datetime
import gzip
import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2] / 'docs/research/nylocas/mage'
STYLE = {0: 'melee', 1: 'range', 2: 'mage'}
LANE = {1: 'west', 2: 'south', 3: 'east'}


def load_checked(entry):
    raw = gzip.decompress((ROOT / entry['path']).read_bytes())
    assert hashlib.sha256(raw).hexdigest() == entry['sha256'], entry['path']
    return json.loads(raw)


def write_csv(name, rows):
    with (ROOT / name).open('w') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)


def counts(values):
    return collections.Counter(values).most_common()


def main():
    manifest = json.loads((ROOT / 'manifest.json').read_text())
    search = json.loads((ROOT / 'search-results.json').read_text())
    assert len(search) == len(manifest['recordings']) < 100
    assert {r['uuid'] for r in search} == {r['uuid'] for r in manifest['recordings']}
    assert all(r['splits']['42']['accurate'] and r['splits']['42']['ticks'] <= 326 for r in search)
    definition = manifest['attackDefinitions']
    raw = (ROOT / definition['path']).read_bytes()
    assert hashlib.sha256(raw).hexdigest() == definition['sha256']
    by_attack = {d['protoId']: d for d in json.loads(raw)}
    for attack, weapon, cooldown in [(24, 21015, 5), (38, 22325, 5), (40, 27275, 5),
                                     (74, 11230, 2), (101, 29084, 4), (104, 31113, 3)]:
        assert weapon in by_attack[attack]['weaponIds']
        assert cooldown == by_attack[attack]['cooldown']
    attacks, positions, wave_rows, rooms, lane_npcs, packets = [], [], [], [], [], []
    for recording in manifest['recordings']:
        raid, events = (load_checked(recording['files'][kind]) for kind in ('raid', 'events'))
        assert raid['mode'] == 11 and raid['scale'] == 2
        start = datetime.datetime.fromisoformat(recording['startTime'].replace('Z', '+00:00'))
        assert start.timestamp() * 1000 >= manifest['startTimeMillis']
        mage = next(p['username'] for p in raid['party']
                    if p['currentUsername'].casefold() == 'drummie')
        room = raid['tobRooms']['nylocas']
        assert room['ticksLost'] == 0
        metadata = room['npcs']
        wave_events = sorted((e for e in events if e['type'] == 120), key=lambda e: e['tick'])
        assert [e['nyloWave']['wave'] for e in wave_events] == list(range(1, 32))
        ticks = [e['tick'] for e in wave_events]
        boss = next(e['tick'] for e in events if e['type'] == 123)
        assert boss == raid['splits']['42'] == recording['bossSpawn']['ticks'] <= 326
        for meta in metadata.values():
            if 'nylo' in meta and 8342 <= meta['spawnNpcId'] <= 8353:
                assert meta['nylo']['wave'] == bisect.bisect_right(ticks, meta['spawnTick'])
        # First recorded anchor inside the arena. This is not a universal
        # actionable boundary: ranged attacks can hit down lanes beforehand.
        entry_ticks = {}
        for e in events:
            if e['type'] in (7, 8) and 3290 <= e['xCoord'] <= 3301 and 4243 <= e['yCoord'] <= 4254:
                rid = e['npc']['roomId']
                entry_ticks[rid] = min(e['tick'], entry_ticks.get(rid, e['tick']))
        by_tick = collections.defaultdict(list)
        for e in events:
            by_tick[e['tick']].append(e)
        states, local_attacks, local_positions = {}, [], []
        for tick, batch in sorted(by_tick.items()):
            # Join same-tick state regardless of serialization order. Retain
            # partial NPC fields; a missing nylo payload is not a style reset.
            for e in batch:
                if e['type'] not in (7, 8):
                    continue
                rid = e['npc']['roomId']
                state = states.setdefault(rid, {})
                state.update(x=e['xCoord'], y=e['yCoord'])
                state.update({k: v for k, v in e['npc'].items() if k != 'nylo'})
                state.setdefault('nylo', {}).update(e['npc'].get('nylo', {}))
            wave = bisect.bisect_right(ticks, tick)
            for e in batch:
                if e['type'] == 4 and e['player']['name'] == mage and tick < boss:
                    p = dict(uuid=raid['uuid'], tick=tick, wave=wave, x=e['xCoord'],
                             y=e['yCoord'], off_cooldown=e['player'].get('offCooldownTick', ''))
                    positions.append(p)
                    local_positions.append(p)
                if e['type'] != 5 or tick >= boss:
                    continue
                attack = e['attack']
                rid = attack.get('target', {}).get('roomId')
                meta = metadata.get(str(rid))
                if not meta or 'nylo' not in meta or not 8342 <= meta['spawnNpcId'] <= 8353:
                    continue
                nylo, state = meta['nylo'], states.get(rid, {})
                # Native ID expresses CURRENT color, including flickers.
                current_id = state.get('id', attack['target'].get('id'))
                style = STYLE[(current_id - 8342) % 3]
                lane = LANE.get(nylo.get('spawnType'), 'split')
                point = meta['spawnPoint']
                slot = ('N' if point['y'] == 4249 else 'S') if lane in ('west', 'east') else (
                    'W' if point['x'] == 3295 else 'E') if lane == 'south' else ''
                target = f"w{nylo['wave']} {lane}{('-' + slot) if slot else ''} " + (
                    'big ' if nylo['big'] else 'small ') + STYLE[nylo['style']]
                weapon = attack.get('weapon', {}).get('id', '')
                row = dict(uuid=raid['uuid'], player=e['player']['name'],
                           role='mage' if e['player']['name'] == mage else 'ranger',
                           tick=tick, wave=wave, offset=tick - ticks[wave - 1],
                           x=e['xCoord'], y=e['yCoord'], attack_type=attack['type'],
                           attack_name=by_attack.get(attack['type'], {}).get('name', 'UNKNOWN'),
                           attack_category=by_attack.get(attack['type'], {}).get('category', 'UNKNOWN'),
                           weapon=weapon, target_id=rid, target=target,
                           target_wave=nylo['wave'], lane=lane, slot=slot, big=nylo['big'],
                           initial_style=STYLE[nylo['style']], current_style=style,
                           current_npc_id=current_id,
                           spawn_tick=meta['spawnTick'], age=tick - meta['spawnTick'],
                           since_arena_anchor_entry=tick - entry_ticks[rid] if rid in entry_ticks else '',
                           parent=nylo.get('parentRoomId', 0),
                           target_x=state.get('x', ''), target_y=state.get('y', ''),
                           hp=(state['hitpoints'] >> 16) if 'hitpoints' in state else '',
                           distance=attack.get('distanceToTarget', ''),
                           death_tick=meta.get('deathTick', ''))
                attacks.append(row)
                local_attacks.append(row)
        for index, tick in enumerate(ticks):
            wave, end = index + 1, ticks[index + 1] if index < 30 else boss
            actions = [a for a in local_attacks if a['role'] == 'mage' and a['wave'] == wave]
            coords = [(p['x'], p['y']) for p in local_positions if tick <= p['tick'] < end]
            first = actions[0] if actions else {}
            wave_rows.append(dict(uuid=raid['uuid'], wave=wave, tick=tick, duration=end-tick,
                                  first_tick=first.get('tick', ''), first_target=first.get('target', ''),
                                  first_attack=first.get('attack_name', ''),
                                  first_offset=first.get('offset', ''),
                                  first_x=first.get('x', ''), first_y=first.get('y', ''),
                                  modal_tile=str(counts(coords)[0][0]) if coords else '',
                                  mage_attacks=len(actions), room_cap=wave_events[index]['nyloWave']['roomCap']))
        rooms.append(dict(uuid=raid['uuid'], mage=mage, start_time=recording['startTime'],
                          partner=next(p['currentUsername'] for p in raid['party'] if p['username'] != mage),
                          boss_spawn=boss, ticks_lost=room['ticksLost'],
                          stalled_waves=room.get('stalledWaves', []),
                          wave29=ticks[28], wave31=ticks[30],
                          mage_attacks=sum(a['role'] == 'mage' for a in local_attacks)))
        for rid, meta in metadata.items():
            nylo = meta.get('nylo', {})
            if nylo.get('spawnType') not in LANE or not 8342 <= meta['spawnNpcId'] <= 8353:
                continue
            aimed = [a for a in local_attacks if a['target_id'] == int(rid)]
            lane_npcs.append(dict(uuid=raid['uuid'], target_id=rid, wave=nylo['wave'],
                                  lane=LANE[nylo['spawnType']], big=nylo['big'],
                                  initial_style=STYLE[nylo['style']], spawn_tick=meta['spawnTick'],
                                  death_tick=meta.get('deathTick', ''),
                                  lifetime=meta['deathTick']-meta['spawnTick'] if 'deathTick' in meta else '',
                                  mage_primary_attacks=sum(a['role'] == 'mage' for a in aimed),
                                  ranger_primary_attacks=sum(a['role'] == 'ranger' for a in aimed)))
        ma = [a for a in local_attacks if a['role'] == 'mage']
        scythes = [a for a in ma if a['target_wave'] == 12 and a['lane'] == 'east' and a['attack_type'] == 38]
        dinhs = [a for a in ma if a['target_wave'] == 12 and a['lane'] == 'west' and a['slot'] == 'S' and a['attack_type'] == 24]
        barrages = [a for a in ma if a['target_wave'] == 21 and a['lane'] == 'west' and 'BARRAGE' in a['attack_name']]
        sulphurs = [a for a in ma if ticks[20] <= a['tick'] < ticks[21] and a['attack_type'] == 101]
        late_dinhs = [a for a in ma if a['target_wave'] == 26 and a['lane'] == 'south' and a['attack_type'] == 24]
        s, d, b, su, ld = (x[0] if x else None for x in (scythes, dinhs, barrages, sulphurs, late_dinhs))
        packets.append(dict(uuid=raid['uuid'],
                            scythe12=s, dinh12=d,
                            dinh12_after_scythe=d['tick']-s['tick'] if s and d else None,
                            first14_due=ticks[12]+8, actual14=ticks[13],
                            dinh12_target_removed=d['death_tick'] if d else None,
                            barrage21=b, sulphur21=su,
                            sulphur_after_barrage=su['tick']-b['tick'] if b and su else None,
                            first22_due=ticks[20]+8, actual22=ticks[21],
                            sulphur_target_removed=su['death_tick'] if su else None,
                            dinh26=ld, dinh26_offset28=ld['tick']-ticks[27] if ld else None,
                            wave28=ticks[27], actual29=ticks[28],
                            old_after29=[a for a in ma if a['wave'] >= 29 and a['target_wave'] <= 29]))
    write_csv('attacks.csv', attacks)
    write_csv('positions.csv', positions)
    write_csv('waves.csv', wave_rows)
    write_csv('lane-npcs.csv', lane_npcs)
    (ROOT / 'critical-packets.json').write_text(json.dumps(packets, indent=2) + '\n')
    summary = dict(recordings=len(rooms), rooms=rooms, waves={})
    for wave in range(1, 32):
        rows = [r for r in wave_rows if r['wave'] == wave]
        actions = [a for a in attacks if a['wave'] == wave and a['role'] == 'mage']
        summary['waves'][wave] = dict(
            first_actions=counts((r['first_target'], r['first_attack']) for r in rows),
            targets_by_rooms=counts(target for target in sorted(set(a['target'] for a in actions))
                                   for _ in {a['uuid'] for a in actions if a['target'] == target}),
            first_tiles=counts((r['first_x'], r['first_y']) for r in rows),
            modal_tiles=counts(r['modal_tile'] for r in rows))
    mage_actions = [a for a in attacks if a['role'] == 'mage']
    summary['weapons'] = counts(a['attack_name'] for a in mage_actions)
    summary['old_after_29'] = counts(a['uuid'] for a in mage_actions if a['wave'] >= 29 and a['target_wave'] <= 29)
    (ROOT / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
    # Predicates validate specific observable facts, not intention or optimality.
    # Timing uses target age, so a task can cross a last-spawned-wave boundary.
    rules = [
        ('1 south magic opener', dict(target_wave=1, lane='south', big=False, current_style='mage'), None, (0, 12)),
        ('2 west magic', dict(target_wave=2, lane='west', big=False, current_style='mage'), None, (0, 15)),
        ('3 east magic', dict(target_wave=3, lane='east', big=False, current_style='mage'), None, (0, 15)),
        ('4 south magic big', dict(target_wave=4, lane='south', big=True, current_style='mage'), None, (0, 30)),
        ('5 east magic', dict(target_wave=5, lane='east', big=False, current_style='mage'), None, (0, 20)),
        ('4 east melee during 5', dict(target_wave=4, lane='east', big=False, wave=5, attack_type=101), None, (8, 16)),
        ('5 south melee', dict(target_wave=5, lane='south', big=False, attack_type=101), None, (8, 20)),
        ('6 west magic', dict(target_wave=6, lane='west', big=False, current_style='mage'), None, (0, 20)),
        ('7 south magic', dict(target_wave=7, lane='south', big=False, current_style='mage'), None, (0, 20)),
        ('8 west magic big', dict(target_wave=8, lane='west', big=True, current_style='mage'), None, (0, 35)),
        ('9 east magic', dict(target_wave=9, lane='east', big=False, current_style='mage'), None, (0, 20)),
        ('8 south melee during 9', dict(target_wave=8, lane='south', big=False, wave=9, attack_type=101), None, (8, 20)),
        ('9 west melee during 10', dict(target_wave=9, lane='west', big=False, wave=10, attack_type=101), None, (8, 25)),
        ('11 east barrage', dict(target_wave=11, lane='east', big=False, current_style='mage'), 'BARRAGE', (0, 12)),
        ('11 south west magic small', dict(target_wave=11, lane='south', slot='W', big=False, current_style='mage'), None, (0, 20)),
        ('11 west magic big', dict(target_wave=11, lane='west', big=True, current_style='mage'), None, (0, 30)),
        ('12 east scythe birth+8', dict(target_wave=12, lane='east', big=False, attack_type=38), None, (8, 8)),
        ('12 east scythe south target', dict(target_wave=12, lane='east', slot='S', big=False, attack_type=38), None, (8, 8)),
        ('12 west south Dinh at +13', dict(target_wave=12, lane='west', slot='S', big=False, attack_type=24), None, (13, 13)),
        ('13 west magic assistance', dict(target_wave=13, lane='west', big=False, current_style='mage'), None, (0, 20)),
        ('13 south melee', dict(target_wave=13, lane='south', big=False, attack_type=101), None, (8, 30)),
        ('14 west magic', dict(target_wave=14, lane='west', big=False, current_style='mage'), None, (0, 20)),
        ('14 west melee', dict(target_wave=14, lane='west', big=False, current_style='melee', attack_type=101), None, (8, 25)),
        ('15 west melee', dict(target_wave=15, lane='west', big=False, current_style='melee'), None, (0, 25)),
        ('15 west ranged darts', dict(target_wave=15, lane='west', big=False, attack_type=74, current_style='range'), None, (0, 25)),
        ('13 east melee big deferred', dict(target_wave=13, lane='east', big=True, attack_type=38), None, (25, 40)),
        ('19 east magic big', dict(target_wave=19, lane='east', big=True, current_style='mage'), None, (0, 25)),
        ('19 south magic big', dict(target_wave=19, lane='south', big=True, current_style='mage'), None, (0, 25)),
        ('20 south magic big', dict(target_wave=20, lane='south', big=True, current_style='mage'), None, (0, 5)),
        ('21 west barrage birth+1', dict(target_wave=21, lane='west', big=False, current_style='mage'), 'BARRAGE', (1, 1)),
        ('21 north sulphur at +6', dict(wave=21, offset=6, attack_type=101, current_style='melee', target_y_min=4250), None, (0, 51)),
        ('22 south west Dinh birth+10 or +11', dict(target_wave=22, lane='south', slot='W', big=False, attack_type=24), None, (10, 11)),
        ('22 east fighting melee big Dinh fallback', dict(target_wave=22, lane='east', big=True, attack_type=24, current_npc_id=8351), None, (0, 30)),
        ('24 south magic big', dict(target_wave=24, lane='south', big=True, current_style='mage'), None, (0, 15)),
        ('24 east melee big scythe', dict(target_wave=24, lane='east', big=True, attack_type=38), None, (7, 18)),
        ('25 west melee big', dict(target_wave=25, lane='west', big=True, current_style='melee'), None, (7, 18)),
        ('26 south big Dinh during 28', dict(target_wave=26, lane='south', big=True, attack_type=24, wave=28), None, (0, 51)),
        ('26 west magic big', dict(target_wave=26, lane='west', big=True, current_style='mage'), None, (0, 25)),
        ('27 west magic big', dict(target_wave=27, lane='west', big=True, current_style='mage'), None, (0, 25)),
        ('29 south big scythe observed exception', dict(target_wave=29, lane='south', big=True, attack_type=38), None, (0, 25)),
        ('30 east magic big', dict(target_wave=30, lane='east', big=True, current_style='mage'), None, (0, 25)),
        ('31 east magic small', dict(target_wave=31, lane='east', big=False, current_style='mage'), None, (0, 25)),
    ]
    validation = []
    for name, fields, name_contains, (low, high) in rules:
        if 'current_style' in fields:
            fields.setdefault('attack_category', {'mage': 'MAGIC', 'range': 'RANGED', 'melee': 'MELEE'}[fields['current_style']])
        found = [a for a in mage_actions if all(
                 (a[k[:-4]] >= v if k.endswith('_min') else a[k] == v) for k, v in fields.items())
                 and (name_contains is None or name_contains in a['attack_name']) and low <= a['age'] <= high]
        ids = {a['uuid'] for a in found}
        validation.append(dict(rule=name, rooms=len(ids), denominator=len(rooms),
                               predicate=dict(fields=fields, attack_name_contains=name_contains,
                                              min_age=low, max_age=high),
                               age_counts=counts(a['age'] for a in found),
                               offset_counts=counts(a['offset'] for a in found),
                               last_wave_counts=counts(a['wave'] for a in found),
                               tile_counts=counts((a['x'], a['y']) for a in found),
                               examples=found[:3], missing_rooms=[r['uuid'] for r in rooms if r['uuid'] not in ids]))
    (ROOT / 'validation.json').write_text(json.dumps(validation, indent=2) + '\n')
    report = ['| Observed anchor (exact predicate in JSON) | Target age tested | Rooms | Replay witness |',
              '| --- | --- | --- | --- |']
    for v in validation:
        low, high = v['predicate']['min_age'], v['predicate']['max_age']
        age = str(low) if low == high else f'{low}–{high}'
        a = v['examples'][0] if v['examples'] else None
        witness = (f"[{a['uuid'][:6]}](https://blert.io/raids/tob/{a['uuid']}/nylocas), "
                   f"tick {a['tick']}, last wave {a['wave']}") if a else 'No matching observation'
        report.append(f"| {v['rule']} | {age} | {v['rooms']}/{len(rooms)} | {witness} |")
    (ROOT / 'validation.md').write_text('\n'.join(report) + '\n')
    print(f'Validated {len(rooms)} rooms, {len(mage_actions)} mage / '
          f'{len(attacks)-len(mage_actions)} ranger attacks, {len(positions)} mage positions')


if __name__ == '__main__':
    main()
