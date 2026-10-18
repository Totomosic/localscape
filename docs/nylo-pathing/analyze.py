#!/usr/bin/env python3
"""Offline observation extraction; never runs or changes the simulator.

Run: python docs/nylo-pathing/analyze.py
Inputs are SHA-256 checked. No interpolation, inferred missing frames, or network.
Target/aggro classification comes from the prior 120-fingerprint audit, while
all counts and trajectories below are recomputed from raw recording snapshots.
"""
import collections
import csv
import gzip
import hashlib
import itertools
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
OLD = HERE.parent / 'research/nylocas'
PILLARS = {'SW': (3290, 4243), 'SE': (3300, 4243),
           'NW': (3290, 4253), 'NE': (3300, 4253)}
LANES = {1: 'west', 2: 'south', 3: 'east'}
STYLES = {0: 'melee', 1: 'ranged', 2: 'magic'}
CASE_SPECS = [
    ('eb3793f3-ce4a-4fcf-8ef9-e2098d39914a', 'small-NE-congestion', 49, 61, [33313]),
    ('1073635f-5326-4bac-a147-8fdf6299f761', 'big-SE-mixed-pacing', 236, 259, [64113]),
    ('a37210fe-5737-480f-8fca-518e56205e9b', 'big-SW-three-step-loop', 337, 352, [34729]),
    ('1073635f-5326-4bac-a147-8fdf6299f761', 'big-removal-persistent-hole', 170, 199, [62545,63282,63412]),
    ('df03c4dc-1e1d-4f57-8e6c-403803205659', 'same-tick-release-persistent-hole', 303, 312, [59710,59665,59667]),
    ('65c9f142-f240-4284-9384-d32fa313067e', 'three-small-stack', 412, 432, [53449,53579,53734]),
    ('653f90c7-176b-4fd4-a0eb-1cbd818481ce', 'supplied-duo-wave25', 236, 255, [36500]),
    ('653f90c7-176b-4fd4-a0eb-1cbd818481ce', 'supplied-duo-wave27', 260, 270, [37040,37239,37240]),
]


def checked(root, spec):
    raw = gzip.decompress((root / spec['path']).read_bytes())
    assert hashlib.sha256(raw).hexdigest() == spec['sha256'], spec['path']
    return json.loads(raw)


def pos(e):
    return e['xCoord'], e['yCoord']


def face(point, size, pillar):
    x, y = point
    px, py = PILLARS[pillar]
    if y < py + 2 and y + size > py:
        if x + size == px:
            return 'W'
        if x == px + 2:
            return 'E'
    if x < px + 2 and x + size > px:
        if y + size == py:
            return 'S'
        if y == py + 2:
            return 'N'
    return None


def contact_anchors(size, pillar):
    # Geometric candidates only. Terrain is not reconstructed from replay.
    px, py = PILLARS[pillar]
    return [(x, y) for x in range(px-size, px+3)
            for y in range(py-size, py+3) if face((x, y), size, pillar)]


def near(point, size, pillar):
    return min(max(abs(point[0]-x), abs(point[1]-y))
               for x, y in contact_anchors(size, pillar)) <= 2


def fingerprint(n):
    y, p = n['nylo'], n['spawnPoint']
    return (y['wave'], LANES[y['spawnType']], STYLES[y['style']],
            y['big'], p['x'], p['y'])


def main():
    provenance = json.loads((HERE / 'manifest.json').read_text())
    for label in ('classification', 'previousCorpus'):
        spec = provenance[label]
        assert hashlib.sha256((HERE/spec['path']).read_bytes()).hexdigest() == spec['sha256'], label
    with (OLD / 'lane-spawn-targets.csv').open() as f:
        assignments = {(int(r['wave']), r['lane'], r['initial_style'],
                        r['big'] == 'True', int(r['spawn_x']), int(r['spawn_y'])):
                       r for r in csv.DictReader(f)}
    manifests = [(OLD, json.loads((OLD / 'manifest.json').read_text())),
                 (HERE, json.loads((HERE / 'manifest.json').read_text()))]
    report, trajectories, se_rows, cases = [], [], [], []
    phase_counts = collections.defaultdict(collections.Counter)
    switches, mixed, stacks, doubles, admissions = [], [], [], [], []
    for root, manifest in manifests:
        for rec in manifest['recordings']:
            raid = checked(root, rec['files']['raid'])
            events = checked(root, rec['files']['events'])
            assert raid['mode'] == 11 and raid['scale'] in (1, 2)
            assert raid['tobRooms']['nylocas']['ticksLost'] == 0
            npcs = {n['roomId']: n for n in raid['tobRooms']['nylocas']['npcs'].values()
                    if n['type'] == 2}
            paths = collections.defaultdict(dict)
            frames = collections.defaultdict(dict)
            for e in events:
                if e['type'] in (7, 8, 9) and e['npc']['roomId'] in npcs:
                    paths[e['npc']['roomId']][e['tick']] = e
                    if e['type'] != 9:
                        frames[e['tick']][e['npc']['roomId']] = e
            def alive(e):
                return (e is not None and e['type'] != 9
                        and e['npc']['hitpoints'] >> 16 > 0
                        and 8342 <= e['npc']['id'] <= 8347)
            for uuid, name, start, end, focal in CASE_SPECS:
                if uuid == rec['uuid']:
                    cases.append(dict(name=name,raid=uuid,scale=raid['scale'],
                                      start=start,end=end,focal_room_ids=focal,
                                      metadata=[npcs[r] for r in focal],
                                      events=[e for e in events if e['type'] in (7,8,9)
                                              and e['npc']['roomId'] in npcs
                                              and start <= e['tick'] <= end]))
            def safe(t, rid):
                # Exclude births, fighting forms and the end-of-life window.
                n = npcs[rid]
                return (alive(paths[rid].get(t))
                        and 2 <= t - n['spawnTick'] < (50 if n['nylo']['big'] else 49))
            count = collections.Counter()
            selected = []
            for rid, n in sorted(npcs.items()):
                if n['nylo']['spawnType'] not in LANES:
                    continue
                assignment = assignments[fingerprint(n)]
                if assignment['aggro'] == 'True':
                    continue
                count['pillar_lane_lifetimes'] += 1
                pillar = assignment['target']
                size = 2 if n['nylo']['big'] else 1
                phase = '1-9' if n['nylo']['wave'] < 10 else ('10-20' if n['nylo']['wave'] <= 20 else '21-31')
                key = phase + '/' + ('big' if size == 2 else 'small')
                path = paths[rid]
                def active(e):
                    return alive(e) and e['tick'] - n['spawnTick'] < (52 if size == 2 else 51)
                holds = []
                run = None
                for t, e in sorted(path.items()):
                    if alive(e):
                        f = face(pos(e), size, pillar)
                        if f:
                            if run and run['end'] == t-1 and run['anchor'] == list(pos(e)):
                                run['end'] = t
                            else:
                                if run and run['end'] > run['start']:
                                    holds.append(run)
                                run = dict(start=t, end=t, face=f, anchor=list(pos(e)))
                        else:
                            if run and run['end'] > run['start']:
                                holds.append(run)
                            run = None
                    else:
                        if run and run['end'] > run['start']:
                            holds.append(run)
                        run = None
                    previous = path.get(t-1)
                    if (active(e) and active(previous) and near(pos(e), size, pillar)
                            and near(pos(previous), size, pillar)):
                        c = phase_counts[key]
                        count['near_intervals'] += 1
                        c['near_intervals'] += 1
                        moving = pos(e) != pos(previous)
                        c['moving' if moving else 'stationary'] += 1
                        count['near_moving' if moving else 'near_stationary'] += 1
                        if not moving and f:
                            c['stationary_at_target_contact'] += 1
                    # A->B->A->C->A with one cardinal and one diagonal excursion.
                    seq = [path.get(t+i) for i in range(5)]
                    if all(active(v) for v in seq):
                        pp = [pos(v) for v in seq]
                        if (pp[0] == pp[2] == pp[4] and pp[1] != pp[3]
                                and all(near(p, size, pillar) for p in pp)):
                            dd = [(abs(p[0]-pp[0][0]), abs(p[1]-pp[0][1])) for p in (pp[1], pp[3])]
                            if sorted(sum(d) for d in dd) == [1, 2] and all(max(d) == 1 for d in dd):
                                count['mixed_reversal_windows'] += 1
                                mixed.append(dict(raid=rec['uuid'], room_id=rid, wave=n['nylo']['wave'],
                                                  big=size == 2, pillar=pillar, start=t, positions=pp))
                if run and run['end'] > run['start']:
                    holds.append(run)
                for a, b in zip(holds, holds[1:]):
                    if a['face'] != b['face']:
                        switches.append(dict(raid=rec['uuid'], room_id=rid, wave=n['nylo']['wave'],
                                             big=size == 2, pillar=pillar, before=a, after=b))
                        count['settled_face_switches'] += 1
                if holds:
                    count['lifetimes_with_sustained_target_contact'] += 1
                if size == 2 and n['nylo']['wave'] in (25, 27) and n['nylo']['spawnType'] == 3:
                    first = holds[0] if holds else None
                    se_rows.append(dict(raid=rec['uuid'], scale=raid['scale'], room_id=rid,
                                        wave=n['nylo']['wave'], first_hold=first,
                                        overhanging_contact_ticks=sum(alive(e) and face(pos(e),size,pillar) is not None
                                                                     and (pos(e)[0]<3290 or pos(e)[1]<4243
                                                                          or pos(e)[0]+size-1>3301 or pos(e)[1]+size-1>4254)
                                                                     for e in path.values())))
                # One audit row per pillar-directed lane NPC, including no-contact outcomes.
                selected.append(dict(room_id=rid, wave=n['nylo']['wave'], big=size==2,
                                     lane=LANES[n['nylo']['spawnType']], pillar=pillar, holds=holds))
            # Stable same-anchor pairs: all sizes and origins, strictly ordinary IDs.
            # Record center ticks only where both survive the birth/expiry filter at t-1..t+1.
            for t, frame in sorted(frames.items()):
                anchors = collections.defaultdict(list)
                for rid, e in frame.items():
                    if safe(t, rid) and 3290 <= pos(e)[0] <= 3301 and 4243 <= pos(e)[1] <= 4254:
                        anchors[pos(e)].append(rid)
                for point, ids in anchors.items():
                    for a, b in itertools.combinations(sorted(ids), 2):
                        if all(safe(q, rid) and pos(paths[rid][q]) == point
                               for q in (t-1,t,t+1) for rid in (a,b)):
                            item=dict(raid=rec['uuid'],tick=t,anchor=point,room_ids=[a,b])
                            doubles.append(item)
                            count['stable_same_anchor_pair_ticks'] += 1
                    for incoming in ids:
                        previous = paths[incoming].get(t-1)
                        if not alive(previous) or pos(previous) == point:
                            continue
                        for incumbent in ids:
                            if incumbent == incoming:
                                continue
                            if not all(safe(q,rid) and pos(paths[rid][q]) == point
                                       for q, rid in [(t-2,incumbent),(t-1,incumbent),
                                                      (t,incumbent),(t+1,incumbent),(t+2,incumbent),
                                                      (t+1,incoming),(t+2,incoming)]):
                                continue
                            held_since = t-2
                            while alive(paths[incumbent].get(held_since-1)) and pos(paths[incumbent][held_since-1]) == point:
                                held_since -= 1
                            releases = []
                            for other, other_path in paths.items():
                                if other in (incumbent,incoming):
                                    continue
                                # A release on the incumbent's arrival tick can
                                # clear its freshly set bit in a later server phase.
                                for q in range(held_since,t+1):
                                    before, after = other_path.get(q-1), other_path.get(q)
                                    if before is None or after is None:
                                        continue
                                    if pos(before) == point and (after['type'] == 9 or pos(after) != point):
                                        releases.append(dict(room_id=other,tick=q,kind='exact_anchor_departure'))
                                    if (npcs[other]['nylo']['big'] and after['type'] == 9
                                            and 8342 <= after['npc']['id'] <= 8347
                                            and pos(after) != point
                                            and pos(after)[0] <= point[0] < pos(after)[0]+2
                                            and pos(after)[1] <= point[1] < pos(after)[1]+2):
                                        releases.append(dict(room_id=other,tick=q,kind='big_nonanchor_footprint_removal',anchor=pos(after)))
                            admissions.append(dict(raid=rec['uuid'],tick=t,anchor=point,
                                                   incoming=incoming,incumbent=incumbent,
                                                   incumbent_held_since=held_since,
                                                   candidate_releases=releases))
                    if len(ids)>=3 and all(safe(q,rid) and pos(paths[rid][q])==point
                                           for q in (t-1,t,t+1) for rid in ids):
                        stacks.append(dict(raid=rec['uuid'],tick=t,anchor=point,room_ids=ids))
                        count['stable_three_plus_anchor_ticks'] += 1
            report.append(dict(raid=rec['uuid'],scale=raid['scale'],counts=dict(count)))
            trajectories.append(dict(raid=rec['uuid'],scale=raid['scale'],npcs=selected))
    summary = dict(method=__doc__, recordings=report,
                   phase_counts={k:dict(v) for k,v in sorted(phase_counts.items())},
                   se_wave25_27=se_rows, settled_face_switches=switches,
                   mixed_cardinal_diagonal_reversal_windows=mixed,
                   stable_same_anchor_pairs=doubles, stable_three_plus_stacks=stacks,
                   occupied_anchor_admissions=admissions)
    (HERE/'summary.json').write_text(json.dumps(summary,indent=2,sort_keys=True)+'\n')
    (HERE/'contact-runs.json').write_text(json.dumps(trajectories,indent=2,sort_keys=True)+'\n')
    (HERE/'cases.json').write_text(json.dumps(cases,indent=2,sort_keys=True)+'\n')
    print(json.dumps(dict(recordings=len(report),phase_counts=summary['phase_counts'],
                         totals=dict(sum((collections.Counter(r['counts']) for r in report),collections.Counter())),
                         face_switch_examples=switches[:6],mixed_examples=mixed[:4],
                         stack_examples=stacks[:4],double_examples=doubles[:4],
                         se_wave25_27=se_rows),indent=2))


if __name__ == '__main__':
    main()
