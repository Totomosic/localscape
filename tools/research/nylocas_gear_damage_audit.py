#!/usr/bin/env python3
"""Decode pinned Blert launch equipment/stats; no gameplay or source mutations.

Uses the executable ItemDelta masks, not the inconsistent preceding bit-layout
comment. Produces separate overlapping corpus estimates, never an 88-room pool.
"""
import collections
import csv
import gzip
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / 'docs/research/nylocas'
OUT = DATA / 'training-v2'
SOURCES = OUT / 'gear-audit-sources'
SLOTS = ['head', 'cape', 'amulet', 'ammo', 'weapon', 'torso', 'shield',
         'legs', 'gloves', 'boots', 'ring', 'quiver']
STATS = ['attack', 'strength', 'defence', 'ranged', 'magic', 'hitpoints', 'prayer']


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def apply_delta(equipment, raw):
    quantity = raw & 0x7fffffff
    added = bool(raw & (1 << 31))
    item = (raw >> 32) & 0xffff
    slot = (raw >> 48) & 0x1f
    current = equipment.get(slot)
    if added:
        equipment[slot] = (item, quantity + current[1]
                           if current and current[0] == item else quantity)
    elif current and current[0] == item and quantity < current[1]:
        equipment[slot] = (item, current[1] - quantity)
    else:
        equipment.pop(slot, None)


def counts(values):
    return dict(sorted(collections.Counter(str(v) for v in values).items()))


def main():
    pin = DATA / 'vetting/summary.json'
    vetting = json.loads(pin.read_text())
    eligible = {r['uuid']: r for r in vetting['eligiblePinnedRecordings']}
    names = json.loads((SOURCES / 'runelite-item-names.json').read_text())
    provenance = json.loads((SOURCES / 'provenance.json').read_text())
    for source in provenance['sources']:
        raw = (SOURCES / source['path']).read_bytes()
        if 'gitBlobSha' in source:
            blob = b'blob ' + str(len(raw)).encode() + b'\0' + raw
            assert hashlib.sha1(blob).hexdigest() == source['gitBlobSha'], source['path']
        if 'sha256' in source:
            assert sha(raw) == source['sha256'], source['path']
    script = Path(__file__).resolve()
    hashes = {str(pin.relative_to(ROOT)): sha(pin.read_bytes()),
              str(script.relative_to(ROOT)): sha(script.read_bytes())}
    decoded, summaries = [], {}
    for corpus in ('mage', 'ranger'):
        folder = DATA / corpus
        manifest_path = folder / 'manifest.json'
        manifest = json.loads(manifest_path.read_text())
        hashes[str(manifest_path.relative_to(ROOT))] = sha(manifest_path.read_bytes())
        attack_path = folder / 'attacks.csv'
        hashes[str(attack_path.relative_to(ROOT))] = sha(attack_path.read_bytes())
        aa = [r for r in csv.DictReader(attack_path.open()) if r['uuid'] in eligible]
        rooms = sorted({r['uuid'] for r in aa})
        assert len(rooms) == vetting['eligiblePinnedByCorpus'][corpus]
        for uid in rooms:
            room = [r for r in aa if r['uuid'] == uid]
            roles = {r['player']: r['role'] for r in room}
            rec = next(r for r in manifest['recordings'] if r['uuid'] == uid)
            raw_by_kind = {}
            for kind in ('raid', 'events'):
                f = rec['files'][kind]
                path = folder / f['path']
                raw = gzip.decompress(path.read_bytes())
                assert sha(raw) == f['sha256'], path
                hashes[str(path.relative_to(ROOT)) + ':uncompressed'] = sha(raw)
                raw_by_kind[kind] = json.loads(raw)
            events, raid = raw_by_kind['events'], raw_by_kind['raid']
            boss = next(e['tick'] for e in events if e['type'] == 123)
            npcs = raid['tobRooms']['nylocas']['npcs']
            assert boss == eligible[uid]['bossTicks'] <= 326
            raw_attacks = collections.Counter(
                (e['player']['name'], e['tick'], e['attack']['weapon']['id'],
                 str(e['attack']['target']['roomId']), e['attack']['type'])
                for e in events if e['type'] == 5 and e['tick'] < boss
                and 'target' in e['attack'] and e['attack']['target'].get('roomId')
                and str(e['attack']['target']['roomId']) in npcs)
            csv_attacks = collections.Counter(
                (r['player'], int(r['tick']), int(r['weapon']), r['target_id'], int(r['attack_type']))
                for r in room)
            assert csv_attacks == raw_attacks, (corpus, uid, csv_attacks-raw_attacks,
                                               raw_attacks-csv_attacks)
            updates = collections.defaultdict(list)
            for index, e in enumerate(events):
                if e['type'] == 4:
                    updates[e['player']['name']].append((e['tick'], index, e['player']))
            equipment, state, at = {}, {}, {}
            for name in roles:
                equipment[name], state[name], at[name] = {}, {}, 0
                updates[name].sort(key=lambda u: (u[0], u[1]))
            attacks_by_target = collections.defaultdict(list)
            for r in room:
                attacks_by_target[r['target_id']].append(r)
            for r in sorted(room, key=lambda r: int(r['tick'])):
                name, tick = r['player'], int(r['tick'])
                while at[name] < len(updates[name]) and updates[name][at[name]][0] <= tick:
                    utick, _, p = updates[name][at[name]]
                    if p.get('snapshot'):
                        equipment[name].clear()
                    for d in p.get('equipmentDeltas', []):
                        apply_delta(equipment[name], d)
                    for stat in STATS:
                        if stat in p:
                            state[name][stat] = dict(current=p[stat] >> 16,
                                                     base=p[stat] & 0xffff, tick=utick)
                    if 'prayerSet' in p:
                        state[name]['prayerSet'] = p['prayerSet']
                    state[name]['updateTick'] = utick
                    at[name] += 1
                equip = {SLOTS[k] if k < len(SLOTS) else str(k): v[0]
                         for k, v in sorted(equipment[name].items())}
                npc = npcs.get(r['target_id'], {})
                history = sorted(attacks_by_target[r['target_id']], key=lambda r: int(r['tick']))
                death = npc.get('deathTick')
                first = history[0] is r
                small_hp = 16 if r['big'] == 'True' else 8
                lane_eye = (int(r['weapon']) == 31113 and int(r['target_wave']) <= 15
                            and int(r['parent']) == 0 and int(r['hp']) == small_hp
                            and first and r['initial_style'] == r['current_style'] == 'mage')
                # CSV uses 'magic' in some older extractor versions.
                lane_eye = lane_eye or (int(r['weapon']) == 31113 and int(r['target_wave']) <= 15
                            and int(r['parent']) == 0 and int(r['hp']) == small_hp
                            and first and r['initial_style'] == r['current_style'] == 'magic')
                removed = bool(death is not None and tick < death <= tick + 12
                               and sum(int(a['tick']) < death for a in history) == 1)
                decoded.append(dict(corpus=corpus, uuid=uid, role=r['role'], player=name,
                    tick=tick, weapon=int(r['weapon']), attackType=int(r['attack_type']),
                    target=r['target_id'], targetWave=int(r['target_wave']), big=r['big'] == 'True',
                    hp=int(r['hp']), equipment=equip, stats=dict(state[name]),
                    weaponMatchesUpdate=equip.get('weapon') == int(r['weapon']),
                    ordinaryLaneEye=lane_eye, removedWithin12OnePrimary=removed,
                    deathTick=death))
        corpus_rows = [r for r in decoded if r['corpus'] == corpus]
        by_role = {}
        for role in ('mage', 'ranger'):
            rows = [r for r in corpus_rows if r['role'] == role]
            by_weapon = {}
            for weapon in sorted({r['weapon'] for r in rows}):
                ww = [r for r in rows if r['weapon'] == weapon]
                profiles = collections.defaultdict(list)
                for r in ww:
                    profile = json.dumps(dict(equipment=r['equipment'],
                        stats={s: r['stats'].get(s, {}).get('current') for s in STATS[:5]},
                        prayerSet=r['stats'].get('prayerSet')), sort_keys=True)
                    profiles[profile].append(r)
                by_weapon[str(weapon)] = dict(name=names.get(str(weapon)), n=len(ww),
                    weaponUpdateMismatches=sum(not r['weaponMatchesUpdate'] for r in ww),
                    slots={s: counts(r['equipment'].get(s, 'unreported') for r in ww) for s in SLOTS},
                    stats={s: counts(r['stats'].get(s, {}).get('current', 'unreported') for r in ww)
                           for s in STATS[:5]},
                    profiles=[dict(profile=json.loads(k), n=len(v), rooms=len({r['uuid'] for r in v}))
                              for k,v in sorted(profiles.items(), key=lambda kv: (-len(kv[1]), kv[0]))])
            outcomes = []
            strata = collections.defaultdict(list)
            for r in rows:
                if r['ordinaryLaneEye']:
                    key = json.dumps(dict(big=r['big'], equipment=r['equipment'],
                        magic=r['stats'].get('magic', {}).get('current'),
                        prayerSet=r['stats'].get('prayerSet')), sort_keys=True)
                    strata[key].append(r)
            for k,v in sorted(strata.items(), key=lambda kv: (-len(kv[1]), kv[0])):
                outcomes.append(dict(stratum=json.loads(k), n=len(v),
                    removedWithin12OnePrimary=sum(r['removedWithin12OnePrimary'] for r in v),
                    rooms=len({r['uuid'] for r in v})))
            by_role[role] = dict(n=len(rows), weapons=by_weapon, ordinaryLaneEye=outcomes)
        summaries[corpus] = dict(rooms=len(rooms), roles=by_role)
    item_ids = {i for r in decoded for i in r['equipment'].values()} | {r['weapon'] for r in decoded}
    for p in SOURCES.iterdir():
        if p.is_file():
            hashes[str(p.relative_to(ROOT))] = sha(p.read_bytes())
    native_paths = [
        'content/other/nylocas-bots/src/main/kotlin/org/rsmod/content/other/nylocas/bots/NylocasBotController.kt',
        'api/config/src/main/kotlin/org/rsmod/api/config/editors/ObjEdits.kt',
        'api/player/src/main/kotlin/org/rsmod/api/player/bonus/WornBonuses.kt',
        'api/player/src/main/kotlin/org/rsmod/api/player/worn/EquipmentChecks.kt',
        'api/combat/combat-formulas/src/main/kotlin/org/rsmod/api/combat/formulas/accuracy/magic/MagicAccuracyOperations.kt',
        'api/combat/combat-formulas/src/main/kotlin/org/rsmod/api/combat/formulas/maxhit/magic/MagicMaxHitOperations.kt',
        'content/other/special-weapons/src/main/kotlin/org/rsmod/content/other/special/weapons/magic/EyeOfAyakWeapons.kt',
        'content/other/special-weapons/src/integration/kotlin/org/rsmod/content/other/special/weapons/SeekingDragonArrowsTest.kt',
    ]
    for rel in native_paths:
        p = ROOT / 'upstream/rsmod' / rel
        hashes[str(p.relative_to(ROOT))] = sha(p.read_bytes())
    result = dict(scope='vetted pinned recordings; launch tick through latest same-tick update; corpora overlap',
        corpusCounts=vetting['eligiblePinnedByCorpus'], uniqueRooms=len({r['uuid'] for r in decoded}),
        overlapRooms=len({r['uuid'] for r in decoded if r['corpus']=='mage'} &
                         {r['uuid'] for r in decoded if r['corpus']=='ranger'}),
        summaries=summaries, itemNames={str(i): names.get(str(i)) for i in sorted(item_ids)}, sources=hashes)
    path = OUT / 'gear-damage-audit.json'
    path.write_text(json.dumps(result, indent=2) + '\n')
    with gzip.GzipFile(filename=str(OUT / 'gear-damage-launches.json.gz'), mode='wb', mtime=0) as f:
        f.write((json.dumps(decoded, separators=(',', ':')) + '\n').encode())
    print(path.relative_to(ROOT))
    print(json.dumps({c:{role:{w:{k:d[k] for k in ('n','slots','stats','weaponUpdateMismatches')}
                     for w,d in r['weapons'].items() if w in ('31113','12926','20997')}
                     for role,r in s['roles'].items()} for c,s in summaries.items()}, indent=2))


if __name__ == '__main__':
    main()
