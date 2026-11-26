"""Observed South15 natural expiry and West18 child overlap at first20.

First-check snapshots precede native processing. CSV counters decide failures.
Removal type is observed from HP history and exact birth+55 disappearance,
not inferred solely from the existence of two smalls.
"""
import collections
import hashlib
import json
from pathlib import Path

root = Path(__file__).parent
cohorts = {}
for name in ['law-cap-v2-200', 'law-cap-v2-300', 'dual-fresh-cap-v2-200', 'dual-fresh-cap-v2-300']:
    path = root / (name + '-dynamic.json')
    data = json.loads(path.read_text())
    counts = collections.Counter()
    records = []
    for room in data['rooms']:
        first = room['frames'][0]
        nineteen = next(f for f in room['frames'] if f['age19'] == 0)
        check = next(f for f in room['frames'] if f['age19'] == 12)
        parent = next((c for c in first['pre20_crabs'] if c['big'] and c['wave'] == 15 and c['lane'] == 'south'), None)
        if parent is None:
            counts['parent_absent_at18'] += 1
            continue
        key = parent['key']
        live = any(c['key'] == key and c['hp'] > 0 and not c['dying'] for c in nineteen['pre20_crabs'])
        expiry = parent['born'] + 55
        risky = live and expiry <= room['spawn19'] + 12
        west = [c for c in check['pre20_crabs'] if not c['big'] and any(p['wave'] == 18 and p['lane'] == 'west' for p in c.get('parents', []))]
        south = [c for c in check['pre20_crabs'] if not c['big'] and any(p['key'] == key for p in c.get('parents', []))]
        counts['risky' if risky else 'not_risky'] += 1
        counts[('risky' if risky else 'not_risky') + ('_failed' if room['failed19'] else '_passed')] += 1
        if not risky:
            continue
        counts['risky_both_west_atcheck' if len(west) >= 2 else 'risky_west_cleared'] += 1
        if len(west) >= 2 and room['failed19']:
            counts['risky_both_west_failed'] += 1
        seen = [dict(tick=f['tick'], hp=c['hp'], dying=c['dying']) for f in room['frames'] for c in f['pre20_crabs'] if c['key'] == key]
        removal = next((f['tick'] for f in room['frames'] if f['tick'] >= expiry - 2 and all(c['key'] != key for c in f['pre20_crabs'])), None)
        combat_zero = next((f['tick'] for f in room['frames'] for c in f['pre20_crabs'] if c['key'] == key and (c['hp'] <= 0 or c['dying'])), None)
        nature = removal == expiry and combat_zero is None
        counts['risky_observed_natural55' if nature else 'risky_other_removal'] += 1
        west_shots = []
        for frame in room['frames']:
            if not frame['attack']:
                continue
            crab = next((c for c in frame['pre20_crabs'] if c['key'] == frame['selected']), None)
            if crab and crab['big'] and crab['wave'] == 18 and crab['lane'] == 'west':
                west_shots.append(dict(tick=frame['tick'], age=frame['tick'] - crab['born'], weapon=frame['weapon'], hp=crab['hp'], forecast=crab['forecast']))
        west_births = [dict(tick=f['tick'], crab=c) for f in room['frames'] for c in f['born'] if any(p['wave'] == 18 and p['lane'] == 'west' for p in c.get('parents', []))]
        records.append(dict(seed=room['seed'], failed19=room['failed19'], spawn18=room['spawn18'], spawn19=room['spawn19'], first20=room['spawn19'] + 12, parent=parent, south_expiry=expiry, south_removal=removal, south_combat_zero=combat_zero, south_natural55=nature, south_hp_history=seen, west_shots=west_shots, west_births=west_births, check_population=check['population'], check_composition=check['composition'], west_children_at_check=west, south_children_at_check=south, populations=[{k:f[k] for k in ['tick','age19','population','pre20_bodies','composition']} for f in room['frames'] if f['age19'] is not None and 0 <= f['age19'] <= 12]))
    cohorts[name] = dict(counts=dict(counts), risky_rooms=records, source_sha256=hashlib.sha256(path.read_bytes()).hexdigest())
    print(name, dict(counts))
(root / 'south15-west18-expiry.json').write_text(json.dumps(dict(caveat=__doc__, cohorts=cohorts), indent=2) + '\n')
