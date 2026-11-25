#!/usr/bin/env python3
"""Check attack-tagged planner rows against independent native primary launches."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import re


def audit_lines(lines, weapon_ids):
    native, planned = {}, {}
    trial, last_tick = -1, None
    for line in lines:
        fields = line.lstrip(' ').rstrip('\r\n').split('\t')
        if fields[0] not in ('NYLO_PRIMARY', 'NYLO_TRACE'):
            continue
        tick, role = map(int, fields[1:3])
        if last_tick is None or tick < last_tick:
            if tick != 0:
                raise ValueError('Trial does not start at tick zero')
            trial += 1
        last_tick = tick
        frame = trial, tick, role
        if fields[0] == 'NYLO_PRIMARY':
            native.setdefault(frame, []).append(fields[3:7])
        elif fields[5] == 'true':
            planned.setdefault(frame, []).append(fields[7:10])
    if not native:
        return dict(trials=trial + 1, native_launches=0,
                    attribution='unknown: legacy or absent native launch telemetry',
                    mismatched_frames=None, mismatches=[])
    mismatches, packets, packet_rooms = [], Counter(), {}
    for frame in sorted(native.keys() | planned.keys()):
        actual, selected = native.get(frame, []), planned.get(frame, [])
        matches = len(actual) == len(selected) == 1
        if matches:
            target, weapon, pending_target, pending_rule = actual[0]
            selected_weapon, selected_target, selected_rule = selected[0]
            matches = (target == selected_target == pending_target
                       and weapon == weapon_ids[frame[2]].get(selected_weapon)
                       and pending_rule == selected_rule)
        if not matches:
            mismatches.append(dict(trial=frame[0], tick=frame[1], role=frame[2],
                                   native=actual, planned=selected))
        else:
            label = f'{frame[2]}:{selected[0][2]}'
            packets[label] += 1
            packet_rooms.setdefault(label, set()).add(frame[0])
    return dict(trials=trial + 1, native_launches=sum(map(len, native.values())),
                attack_tagged_planner_rows=sum(map(len, planned.values())),
                attribution='compared to native target, weapon and pending decision',
                mismatched_frames=len(mismatches), mismatches=mismatches,
                matched_rule_launches=dict(packets),
                rooms_with_matched_rule={key: len(value) for key, value in packet_rooms.items()})


def audit(directory):
    policies = directory / 'policy-source/org/localscape/nylocas'
    weapon_ids = {}
    for role, path in enumerate(('ranger/RangerStrategy.java', 'mage/MageStrategy.java')):
        weapon_ids[role] = dict(re.findall(r'\b([A-Z]+)\((\d+), Style\.', (policies / path).read_text()))
    log = directory / 'server.log'
    with log.open() as source:
        result = audit_lines(source, weapon_ids)
    result.update(run=str(directory), source_sha256=hashlib.sha256(log.read_bytes()).hexdigest())
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('directory', type=Path)
    args = parser.parse_args()
    result = audit(args.directory)
    output = args.directory / 'primary-attribution-audit.json'
    output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({key: value for key, value in result.items()
                      if key not in ('mismatches', 'matched_rule_launches', 'rooms_with_matched_rule')}, indent=2))
    print(output)


if __name__ == '__main__':
    main()
