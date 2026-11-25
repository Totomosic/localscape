#!/usr/bin/env python3
"""Audit actual SBS requests against Jordan's utility guidance.

Preparation outside the room is reported separately. This reads confirmed native
launches and utility snapshots, rather than assuming a selected attack launched.
"""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path


COOLDOWNS = {20997: 5, 12926: 2, 11959: 3, 31113: 3, 13652: 4,
             27275: 5, 27624: 5, 22325: 5, 29084: 4, 21015: 5}


def audit_lines(lines):
    counts, examples = Counter(), []
    latest, snapshots = {}, {}
    trial = -1
    for line in lines:
        f = line.strip().split('\t')
        if f[0] == 'NYLO_UTILITY_STATE':
            clock, tick, role = map(int, f[1:4])
            if tick == 0 and role == 0:
                latest, snapshots = {}, {}
                trial += 1
            snapshots[clock, role] = dict(tick=tick, wave=int(f[4]), book=int(f[5]),
                energy=int(f[6]), dc_cooldown=int(f[9]), thrall_active=int(f[10]))
        elif f[0] == 'NYLO_PRIMARY':
            tick, role = map(int, f[1:3])
            latest[role] = dict(tick=tick, weapon=int(f[4]), rule=f[6])
        elif f[0] == 'NYLO_UTILITY' and f[3] == 'SWAP_REQUEST':
            clock, role, destination = int(f[1]), int(f[2]), int(f[4])
            state = snapshots.get((clock, role))
            if state is None:
                counts['preparation_swaps_without_combat_snapshot'] += 1
                continue
            counts['combat_swaps'] += 1
            counts[f'role{role}_destination{destination}'] += 1
            attack = latest.get(role)
            cooldown = COOLDOWNS.get(attack['weapon'], 0) if attack else 0
            remaining = attack['tick'] + cooldown - state['tick'] if attack else 0
            safe = cooldown == 5 and remaining >= 3
            if not safe:
                counts['swaps_outside_confirmed_five_tick_window'] += 1
            dc_ready = state['dc_cooldown'] == 0 and state['energy'] <= 700
            forbidden_renewal = destination == 3 and not dc_ready
            if forbidden_renewal:
                counts['arceuus_swaps_without_death_charge_ready'] += 1
            if (not safe or forbidden_renewal) and len(examples) < 30:
                examples.append(dict(trial=trial, role=role, destination=destination,
                    state=state, last_attack=attack, remaining_cooldown=remaining))
    return dict(trials=trial + 1, counts=dict(counts), examples=examples,
        caveat='Arceuus readiness excludes optional experimental pre-staging. '
               'Outside-window ancient requests may be mandatory barrage recovery; inspect examples. '
               'Pre-entry preparation is exempt from combat cooldown guidance.')


def audit(directory):
    source = directory / 'server.log'
    with source.open() as lines:
        result = audit_lines(lines)
    result.update(run=str(directory), source_sha256=hashlib.sha256(source.read_bytes()).hexdigest())
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('directory', type=Path)
    args = parser.parse_args()
    result = audit(args.directory)
    output = args.directory / 'utility-guidance-audit.json'
    output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({k: v for k, v in result.items() if k != 'examples'}, indent=2))


if __name__ == '__main__':
    main()
