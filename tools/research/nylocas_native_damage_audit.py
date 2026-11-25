#!/usr/bin/env python3
"""Audit real native roll/impact telemetry without altering simulations.

The attack ID is present only on application. A matching role, target, single
impact, singleton attack-ID group and clipped roll establish compatibility;
they are not an invented launch-to-attack-ID join.
"""
import argparse
import collections
import csv
import hashlib
import json
from pathlib import Path


def histogram(values):
    return dict(sorted(collections.Counter(str(v) for v in values).items()))


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--run', type=Path, default=Path('.state/simulations/training-v2-flight-damage-instrumented-100'))
    ap.add_argument('--output', type=Path, default=Path('docs/research/nylocas/training-v2/native-damage-audit.json'))
    ap.add_argument('--baseline', type=Path, default=Path('.state/simulations/training-v2-flight-mage-idle-pipe-100'))
    args = ap.parse_args()
    trials = list(csv.DictReader((args.run / 'trials.csv').open()))
    baseline = list(csv.DictReader((args.baseline / 'trials.csv').open()))
    measured = [{k:v for k,v in row.items() if k != 'wall_seconds'} for row in trials]
    reference = [{k:v for k,v in row.items() if k != 'wall_seconds'} for row in baseline]
    assert measured == reference, 'Instrumented gameplay rows differ from canonical baseline'
    rooms = []
    room = None
    for lineno, line in enumerate((args.run / 'server.log').open(), 1):
        a = line.strip().split('\t')
        tag = a[0]
        if tag == 'NYLO_TRACE' and a[1:3] == ['0', '0']:
            room = dict(seed=int(trials[len(rooms)]['seed']), births={}, primaries=[], rolls=[], impacts=[])
            rooms.append(room)
        if room is None:
            continue
        if tag == 'NYLO_TRACE':
            if len(a) < 12:
                continue
            for raw in a[11].split(';'):
                if not raw:
                    continue
                c = raw.split(',')
                x, y = int(c[6]), int(c[7])
                room['births'].setdefault(int(c[0]), dict(wave=int(c[1]),
                    tick=int(a[1])-int(c[2]), style=c[3], big=c[4]=='true',
                    origin='lane' if x < 3285 or x > 3305 or y < 4237 else 'room'))
        elif tag == 'NYLO_PRIMARY':
            room['primaries'].append(dict(tick=int(a[1]),role=int(a[2]),target=int(a[3]),
                weapon=int(a[4]),hp=int(a[7]),magic=int(a[8]),ranged=int(a[9]),gear=a[10],line=lineno))
        elif tag == 'NYLO_ROLL':
            room['rolls'].append(dict(tick=int(a[1]),role=int(a[2]),target=int(a[3]),
                weapon=int(a[4]),damage=int(a[5]),minimum=int(a[6]),maximum=int(a[7]),
                raiseZero=a[8]=='true',line=lineno))
        elif tag == 'NYLO_IMPACT':
            room['impacts'].append(dict(tick=int(a[1]),role=int(a[2]),target=int(a[3]),
                attackId=int(a[4]),damage=int(a[5]),hp=int(a[6]),thrall=a[7]=='true',
                npcId=int(a[8]),line=lineno))
    assert len(rooms) == len(trials)
    cohorts, roll_groups, primary_groups = [], collections.defaultdict(list), collections.defaultdict(list)
    for r in rooms:
        impacts_by_target, primaries_by_target = collections.defaultdict(list), collections.defaultdict(list)
        rolls_by_launch = collections.defaultdict(list)
        group_targets = collections.defaultdict(set)
        for h in r['impacts']:
            impacts_by_target[h['target']].append(h)
            if not h['thrall']:
                group_targets[(h['role'],h['attackId'])].add(h['target'])
        for h in r['rolls']:
            rolls_by_launch[(h['tick'],h['role'],h['target'],h['weapon'])].append(h)
            if h['weapon'] in (31113,12926):
                roll_groups[(h['role'],h['weapon'])].append(h)
        for p in r['primaries']:
            primaries_by_target[p['target']].append(p)
            if p['weapon'] in (31113,12926):
                primary_groups[(p['role'],p['weapon'])].append(p)
        for target, pp in primaries_by_target.items():
            p = pp[0]
            b = r['births'].get(target)
            if not b or b['wave'] > 15 or b['origin'] != 'lane' or p['weapon'] not in (31113,12926):
                continue
            if b['style'] != ('MAGIC' if p['weapon']==31113 else 'RANGE'):
                continue
            full_hp = 16 if b['big'] else 8
            if p['hp'] != full_hp:
                continue
            hits = impacts_by_target[target]
            death = next((h['tick'] for h in hits if h['hp']==0), None)
            rr = rolls_by_launch[(p['tick'],p['role'],target,p['weapon'])]
            window = [h for h in hits if p['tick'] <= h['tick'] <= p['tick']+12]
            first_non_thrall = next((h for h in window if not h['thrall']), None)
            through_first = [h for h in hits if first_non_thrall and h['line'] <= first_non_thrall['line']]
            positive_assists = [h for h in through_first if h['damage'] > 0 and
                                (h['thrall'] or h['role'] != p['role'] or h['line'] < p['line'])]
            matching = bool(len(rr)==1 and first_non_thrall and
                first_non_thrall['role']==p['role'] and
                group_targets[(first_non_thrall['role'],first_non_thrall['attackId'])]=={target} and
                first_non_thrall['damage']==min(rr[0]['damage'],full_hp) and not positive_assists and
                sum(not h['thrall'] for h in through_first)==1)
            one_primary_removal = bool(death is not None and p['tick'] < death <= p['tick']+12 and
                                      sum(q['tick'] < death for q in pp)==1)
            cohorts.append(dict(seed=r['seed'],birth=b,primary=p,rolls=rr,
                firstNonThrallImpact=first_non_thrall,positiveAssistsThroughFirst=positive_assists,
                primaryCompatibleSingleImpact=matching,deathTick=death,
                removedWithin12OnePrimary=one_primary_removal))
    profiles = {}
    for (role,weapon), pp in sorted(primary_groups.items()):
        rr=roll_groups[(role,weapon)]
        profiles[f'{role}:{weapon}'] = dict(primaries=len(pp),rolls=len(rr),
            primaryStats=histogram((p['magic'],p['ranged']) for p in pp),
            primaryGear=histogram(p['gear'] for p in pp),
            rollRanges=histogram((h['minimum'],h['maximum'],h['raiseZero']) for h in rr),
            rolledDamage=histogram(h['damage'] for h in rr))
    summaries = {}
    for role in (0,1):
        for weapon in (31113,12926):
            for big in (False,True):
                cc=[c for c in cohorts if c['primary']['role']==role and
                    c['primary']['weapon']==weapon and c['birth']['big']==big]
                if not cc:
                    continue
                clean=[c for c in cc if c['primaryCompatibleSingleImpact']]
                summaries[f'{role}:{weapon}:big={big}'] = dict(n=len(cc),
                    rooms=len({c['seed'] for c in cc}),
                    removedWithin12OnePrimary=sum(c['removedWithin12OnePrimary'] for c in cc),
                    uniqueLaunchRoll=len([c for c in cc if len(c['rolls'])==1]),
                    rollAtLeastFullHP=sum(len(c['rolls'])==1 and c['rolls'][0]['damage'] >=
                                         c['primary']['hp'] for c in cc),
                    primaryCompatibleSingleImpacts=len(clean),
                    cleanFirstImpactKills=sum(c['firstNonThrallImpact']['hp']==0 for c in clean),
                    positiveAssistanceBeforeFirst=sum(bool(c['positiveAssistsThroughFirst']) for c in cc),
                    unresolvedRollOrImpact=len(cc)-len(clean),
                    cleanRolledDamage=histogram(c['rolls'][0]['damage'] for c in clean))
    paths = [args.run/'server.log',args.run/'trials.csv',args.run/'summary.json',
             args.baseline/'trials.csv',Path(__file__).resolve()]
    paths += sorted((args.run/'adapter-source').rglob('*.kt'))
    attribution_path = args.run/'primary-attribution-audit.json'
    attribution = None
    if attribution_path.exists():
        a = json.loads(attribution_path.read_text())
        attribution = {k:a[k] for k in ('trials','native_launches','attack_tagged_planner_rows',
                                       'attribution','mismatched_frames')}
        paths.append(attribution_path)
    result = dict(semantics='Native launched primary full-HP first-primary lane cohorts, birth<=15; applied damage is clipped; missing roll is unresolved, not automatically a miss. Primary-compatible groups are conservative checks, not an exact attack-ID launch join.',
        run=str(args.run),baseline=str(args.baseline),
        pairedGameplayRowsIdenticalExcludingWallSeconds=len(trials),
        primaryAttribution=attribution,
        rooms=len(rooms),profiles=profiles,summaries=summaries,cohorts=cohorts,
        sources={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in paths})
    args.output.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(summaries,indent=2))


if __name__ == '__main__':
    main()
