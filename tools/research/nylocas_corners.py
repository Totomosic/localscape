#!/usr/bin/env python3
"""Find ordinary-nylo diagonals past stationary NPC side-tile occupants.

An observed collision clear during the occupant's continuous hold excludes a
witness. This screens the two release mechanisms in the pathing research;
it cannot observe every server sub-phase or prove the absence of other clears.
Both NPCs must be ordinary, positive-HP and away from natural expiry. A side
occupant must hold the tile through t-1..t+2. Each match is a transition/side
pair, not an independent experiment. No simulation or network is used.
"""
import collections
import json
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'docs/nylo-pathing'))
from analyze import checked,pos


def main():
    matches=[]
    for root in (ROOT/'docs/research/nylocas',ROOT/'docs/nylo-pathing'):
        manifest=json.loads((root/'manifest.json').read_text())
        for rec in manifest['recordings']:
            raid=checked(root,rec['files']['raid'])
            events=checked(root,rec['files']['events'])
            ns={n['roomId']:n for n in raid['tobRooms']['nylocas']['npcs'].values() if n['type']==2}
            paths=collections.defaultdict(dict)
            frames=collections.defaultdict(dict)
            for e in events:
                if e['type'] in (7,8,9) and e['npc']['roomId'] in ns:
                    paths[e['npc']['roomId']][e['tick']]=e
                    frames[e['tick']][e['npc']['roomId']]=e
            def alive(e):
                return e is not None and e['type']!=9 and 8342<=e['npc']['id']<=8347 and e['npc']['hitpoints']>>16>0
            def safe(rid,t):
                return alive(paths[rid].get(t)) and 2<=t-ns[rid]['spawnTick']<(50 if ns[rid]['nylo']['big'] else 49)
            for t,frame in sorted(frames.items()):
                for rid,e in sorted(frame.items()):
                    following=frames.get(t+1,{}).get(rid)
                    if not(safe(rid,t) and safe(rid,t+1)):continue
                    x,y=pos(e);u,v=pos(following)
                    if abs(u-x)!=1 or abs(v-y)!=1 or not(3290<=x<=3301 and 4243<=y<=4254):continue
                    for side in ((u,y),(x,v)):
                        for occupant,o in sorted(frame.items()):
                            if occupant==rid or pos(o)!=side:continue
                            if not all(safe(occupant,q) and pos(paths[occupant][q])==side for q in (t-1,t,t+1,t+2)):continue
                            held=t-1
                            while alive(paths[occupant].get(held-1)) and pos(paths[occupant][held-1])==side:held-=1
                            released=False
                            for other,path in paths.items():
                                if other in (rid,occupant):continue
                                for q in range(held,t+2):
                                    a,b=path.get(q-1),path.get(q)
                                    if a is None or b is None:continue
                                    if pos(a)==side and (b['type']==9 or pos(a)!=pos(b)):released=True
                                    if b['type']==9 and ns[other]['nylo']['big'] and pos(b)[0]<=side[0]<pos(b)[0]+2 and pos(b)[1]<=side[1]<pos(b)[1]+2:released=True
                            if not released:
                                matches.append(dict(raid=rec['uuid'],tick=t,room_id=rid,big=ns[rid]['nylo']['big'],
                                                    before=(x,y),after=(u,v),side_occupant=occupant,side=side,held_since=held))
    result=dict(method=__doc__,matches=len(matches),by_size=dict(collections.Counter('big' if m['big'] else 'small' for m in matches)),
                by_recording=dict(collections.Counter(m['raid'] for m in matches)),witnesses=matches)
    target=ROOT/'docs/nylo-pathing/verification/corner-evidence.json'
    target.write_text(json.dumps(result,indent=2,sort_keys=True)+'\n')
    print(f"{len(matches)} transition/side matches: {result['by_size']}")


if __name__=='__main__':main()
