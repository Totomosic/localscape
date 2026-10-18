#!/usr/bin/env python3
"""Draw coordinate diagrams from pinned cases, using only the standard library."""
import html
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
CASES = {c['name']: c for c in json.loads((ROOT/'cases.json').read_text())}
PARTS = []


def text(x, y, value, size=15, color='#172c43'):
    PARTS.append(f'<text x="{x}" y="{y}" font-family="sans-serif" font-size="{size}" fill="{color}">{html.escape(str(value))}</text>')


def rect(x,y,w,h,color,stroke='#d7e0e8'):
    PARTS.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{color}" stroke="{stroke}"/>')


def start(w,h,title):
    PARTS.clear()
    PARTS.append(f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}" role="img"><title>{html.escape(title)}</title>')
    rect(0,0,w,h,'#f7fafc')
    PARTS.append('<defs><marker id="arrow" markerWidth="8" markerHeight="8" refX="7" refY="3" orient="auto"><path d="M0,0 L0,6 L8,3 z" fill="#b54b24"/></marker></defs>')


def save(name):
    (ROOT/name).write_text('\n'.join(PARTS+['</svg>'])+'\n')


def grid(ox,oy,xmin,xmax,ymin,ymax,cell=62):
    def xy(x,y):
        return ox+(x-xmin)*cell, oy+(ymax-y)*cell
    for x in range(xmin,xmax+1):
        for y in range(ymin,ymax+1):
            a,b=xy(x,y)
            rect(a,b,cell,cell,'white')
    for x in range(xmin,xmax+1):
        a,b=xy(x,ymin)
        text(a+6,b+cell+22,x,12)
    for y in range(ymin,ymax+1):
        a,b=xy(xmin,y)
        text(a-42,b+cell/2+4,y,12)
    return xy


def anchor(xy,p,label,color='#076a78',cell=62):
    x,y=xy(*p)
    PARTS.append(f'<circle cx="{x+cell/2}" cy="{y+cell/2}" r="11" fill="{color}"/>')
    text(x+5,y+16,label,12,color)


def pacing():
    start(980,450,'Observed cardinal and diagonal reversals beside pillars')
    text(25,32,'One waiting pattern changes direction without changing pillar',22)
    specs=[('big-SE-mixed-pacing',64113,242,(3298,3301,4244,4247),65),
           ('small-NE-congestion',33313,54,(3296,3299,4251,4254),555)]
    for name,rid,t,bounds,ox in specs:
        case=CASES[name]
        es={e['tick']:e for e in case['events'] if e['npc']['roomId']==rid}
        if name=='small-NE-congestion':
            ticks=[54,55,56,57,58,59]
        else:
            ticks=list(range(t,t+5))
        text(ox-25,65,f'{"Big SE" if rid==64113 else "Small NE"}: roomId {rid}',17)
        xy=grid(ox,90,*bounds)
        pts=[]
        for tick in ticks:
            e=es[tick];p=(e['xCoord'],e['yCoord']);pts.append(p)
        for a,b in zip(pts,pts[1:]):
            x,y=xy(*a);u,v=xy(*b)
            PARTS.append(f'<line x1="{x+31}" y1="{y+31}" x2="{u+31}" y2="{v+31}" stroke="#b54b24" stroke-width="2" marker-end="url(#arrow)"/>')
        grouped={}
        for tick,p in zip(ticks,pts):grouped.setdefault(p,[]).append(tick)
        for p,tt in grouped.items():anchor(xy,p,','.join(map(str,tt)))
        text(ox-25,390,'Recorded SW anchors; each arrow is one tick.',13)
    text(25,430,'Source: checksum-pinned Blert events; nearby actors and lifecycle events are preserved in cases.json.',13)
    save('pacing.svg')


def hole():
    start(1120,455,'A big removal precedes a persistent same-anchor stack')
    text(25,32,'One collision-history hypothesis explains persistent stacking',22)
    for ox,title in [(65,'t172–182: small beside a big'),(425,'t183: big removed'),(785,'t193–198: two smalls share anchor')]:
        text(ox-35,70,title,16)
        xy=grid(ox,100,3298,3301,4242,4245)
        # Pillar SE at3300..3301,4243..4244.
        x,y=xy(3300,4244);rect(x,y,124,124,'#cbd3dd')
        text(x+14,y+68,'SE pillar',14)
        if ox==65:
            x,y=xy(3298,4244);rect(x+2,y+2,120,120,'#e2f2f3','#18818b')
            anchor(xy,(3298,4243),'big 62545')
            anchor(xy,(3299,4243),'small 63282','#b54b24')
        elif ox==425:
            x,y=xy(3298,4244)
            PARTS.append(f'<rect x="{x+2}" y="{y+2}" width="120" height="120" fill="#ffe9d7" stroke="#b54b24" stroke-dasharray="5 4"/>')
            anchor(xy,(3299,4243),'63282 holds','#b54b24')
        else:
            anchor(xy,(3299,4243),'63282 + 63412','#b54b24')
        text(ox-35,395,('Live visual overlap' if ox==65 else 'Candidate full-footprint flag clear' if ox==425 else 'Both have positive recorded HP'),14)
    text(25,433,'Observed coordinates are solid evidence. The dashed clear area is an inferred mechanism, not a recorded collision map.',13)
    save('stacking.svg')


if __name__=='__main__':
    pacing()
    hole()
