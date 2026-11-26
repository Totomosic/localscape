"""Screen observed shots compatible with the isolated one-tick parent hold.

This is source-compatible native history, not instrumented branch attribution.
"""
import collections
import json
from pathlib import Path

root = Path(__file__).parent
for name in ['law-cap-v2-200', 'law-cap-v2-300', 'dual-fresh-cap-v2-200', 'dual-fresh-cap-v2-300', 'cap-nearby-melee-200', 'dual-nearby-melee-200']:
    source = root / (name + '-dynamic.json')
    if not source.exists():
        continue
    data = json.loads(source.read_text())
    selected = []
    for room in data['rooms']:
        frame = next(f for f in room['frames'] if f['age19'] == 6)
        target = next((c for c in frame['pre20_crabs'] if c['key'] == frame['selected']), None)
        opened = {f['selected'] for f in room['frames'] if f['tick'] < frame['tick'] and f['attack']}
        if not target or not frame['ready'] or not frame['attack'] or frame['population'] != 10:
            continue
        if not target['big'] or target['wave'] != 18 or target['hp'] != 16 or target['forecast'] != [16, 0] or target['key'] in opened:
            continue
        bodies = [c for c in frame['pre20_crabs'] if c['big'] and c['wave'] == 18 and c['lane'] == 'west' and c['hp'] <= 0 and c['dying']]
        smalls = [c for c in frame['pre20_crabs'] if not c['big'] and c['hp'] > 0 and not c['dying'] and not c['own_null'] and (not c['forecast'] or c['forecast'][1] < c['hp'])]
        parents = [c for c in frame['pre20_crabs'] if c['big'] and c['wave'] == 18 and c['hp'] > 0 and not c['dying'] and not c['own_null'] and c['key'] in opened and (not c['forecast'] or c['forecast'][1] < c['hp'])]
        if not bodies or smalls or parents:
            continue
        births = [f['age19'] for f in room['frames'] for c in f['born'] if c.get('parents') and any(p['key'] == bodies[0]['key'] for p in c['parents'])]
        selected.append(dict(seed=room['seed'], failed19=room['failed19'], births=births))
    print(name, 'eligible', len(selected), 'failed', sum(bool(q['failed19']) for q in selected), 'births', dict(collections.Counter(q['births'][0] if q['births'] else 'unknown' for q in selected)))
    (root / (name + '-one-tick-hold-screen.json')).write_text(json.dumps(selected, indent=2) + '\n')
