#!/usr/bin/env python3
"""Capture the real-engine JUnit replay output and compare it with the baseline.

Run after NylocasReplayTest. --baseline captures a baseline instead of revised
output; obtaining the original policy is a separate explicit checkout/restore.
All per-transition disagreements remain in the TSV artifacts for inspection.
"""
import argparse
import json
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
EVIDENCE=ROOT/'docs/nylo-pathing/verification'
MODULE=ROOT/'upstream/rsmod/content/areas/dungeons/nylocas'


def capture(target):
    xml=MODULE/'build/test-results/integration/TEST-org.rsmod.content.areas.dungeons.nylocas.NylocasReplayTest.xml'
    suite=ET.parse(xml).getroot()
    lines=[line+'-' if line.endswith('\t') else line
           for line in (suite.findtext('system-out') or '').splitlines() if line.startswith('NYLO_REPLAY\t')]
    assert len(lines)==62,len(lines)
    target.write_text('\n'.join(sorted(lines))+'\n')


def compare():
    steps={}
    for line in (MODULE/'src/integration/resources/nylocas-pathing.tsv').read_text().splitlines():
        f=line.split('\t')
        if f[0]=='C':name=f[1];steps[name]={}
        elif f[0]=='T':steps[name][int(f[1])]=f[2:4]!=f[4:6]
    def load(path):
        rows={}
        for line in path.read_text().splitlines():
            f=line.split('\t');name,phase=f[1],int(f[2]);missed={int(m.split(':')[0]) for m in f[5].split(';') if m and m!='-'}
            counts={'matches':int(f[3]),'transitions':int(f[4])}
            assert counts['transitions']==len(steps[name])
            assert counts['matches']==len(steps[name])-len(missed)
            for category,moving in [('moving',True),('stationary',False)]:
                selected={t for t,value in steps[name].items() if value==moving}
                counts[category+'_transitions']=len(selected)
                counts[category+'_matches']=len(selected-missed)
            rows[name,phase]=counts
        return rows
    old=load(EVIDENCE/'baseline.tsv');new=load(EVIDENCE/'revised.tsv');assert old.keys()==new.keys()
    totals={}
    for label,rows in [('baseline',old),('revised',new)]:
        totals[label]={key:sum(row[key] for row in rows.values()) for key in next(iter(rows.values()))}
    result=dict(method=__doc__,totals=totals,cases=[dict(name=name,phase=phase,baseline=old[name,phase],revised=new[name,phase]) for name,phase in sorted(old)])
    (EVIDENCE/'comparison.json').write_text(json.dumps(result,indent=2,sort_keys=True)+'\n')
    print(json.dumps(totals,indent=2))


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--baseline',action='store_true')
    args=parser.parse_args()
    capture(EVIDENCE/('baseline.tsv' if args.baseline else 'revised.tsv'))
    if not args.baseline:compare()
