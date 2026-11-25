#!/usr/bin/env python3
"""Vet whole-pair boss-time averages before filtering individual training rooms.

Default mode is offline and verifies pinned API responses plus every old corpus
raid/event checksum. --collect intentionally refreshes only the separate vetting
snapshot; network access is required. No strategy or simulator code is changed.
"""

import argparse
import collections
import datetime
import gzip
import hashlib
import json
from pathlib import Path
import time
import urllib.parse

from nylocas_ranger_collect import retrieve


ROOT = Path(__file__).resolve().parents[2] / 'docs/research/nylocas'
OUT = ROOT / 'vetting'


def canonical(party):
    return tuple(sorted(p.get('currentUsername', p['username']).casefold() for p in party))


def original_corpus():
    rooms, names = {}, {}
    for role in ('mage', 'ranger'):
        manifest = json.loads((ROOT / role / 'manifest.json').read_text())
        for recording in manifest['recordings']:
            for player in recording['party']:
                name = player.get('currentUsername', player['username'])
                names[name.casefold()] = name
            pair = canonical(recording['party'])
            assert len(pair) == 2
            prior = rooms.setdefault(recording['uuid'], dict(pair=pair, recording=recording, roots=[]))
            assert prior['pair'] == pair
            prior['roots'].append(role)
    return rooms, names


def collect():
    OUT.mkdir(parents=True, exist_ok=True)
    old, names = original_corpus()
    cutoff = int(time.time() * 1000)
    manifest = dict(retrievedAt=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                    asOfStartTimeMillis=cutoff, pairs=[], sources=[])
    for filename, source in [('blert-query.ts', 'web/app/api/v1/challenges/query.ts'),
                             ('blert-route.ts', 'web/app/api/v1/challenges/route.ts')]:
        raw = (OUT / filename).read_bytes()
        manifest['sources'].append(dict(path=filename,
            url='https://github.com/blert-io/blert/blob/master/' + source,
            sha256=hashlib.sha256(raw).hexdigest()))
    for number, pair in enumerate(sorted({r['pair'] for r in old.values()}), 1):
        query = dict(type=1, mode=11, scale=2, party=','.join(names[p] for p in pair),
                     stage='ge12', startTime='le' + str(cutoff),
                     **{'split:42': 'ge0', 'sort': '-startTime',
                        'extraFields': 'splits:42', 'limit': 100})
        entry = dict(pair=[names[p] for p in pair], pages=[])
        for page in range(1, 1001):
            url = 'https://blert.io/api/v1/challenges?' + urllib.parse.urlencode(query)
            raw = retrieve(url)
            rows = json.loads(raw)
            assert isinstance(rows, list)
            filename = f'pair-{number:02d}-page-{page:03d}.json.gz'
            (OUT / filename).write_bytes(gzip.compress(raw, mtime=0))
            entry['pages'].append(dict(path=filename, url=url, rows=len(rows),
                                      sha256=hashlib.sha256(raw).hexdigest()))
            if len(rows) < 100:
                break
            # Primary Blert parser accepts after=<epoch-ms> for -startTime.
            # Refuse a boundary tie rather than silently omitting tied rows.
            assert rows[-1]['startTime'] != rows[-2]['startTime']
            stamp = datetime.datetime.fromisoformat(rows[-1]['startTime'].replace('Z', '+00:00'))
            query['after'] = str(round(stamp.timestamp() * 1000))
        else:
            raise ValueError('Pagination did not terminate')
        manifest['pairs'].append(entry)
        (OUT / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
        print(entry['pair'], sum(p['rows'] for p in entry['pages']), flush=True)


def checked(root, file):
    raw = (root / file['path']).read_bytes()
    if file['path'].endswith('.gz'):
        raw = gzip.decompress(raw)
    assert hashlib.sha256(raw).hexdigest() == file['sha256'], file['path']
    return json.loads(raw) if not file['path'].endswith('.ts') else raw


def analyze():
    old, _ = original_corpus()
    snapshot = json.loads((OUT / 'manifest.json').read_text())
    for source in snapshot['sources']:
        checked(OUT, source)
    pairs, eligible = [], set()
    for entry in snapshot['pairs']:
        pair = tuple(sorted(p.casefold() for p in entry['pair']))
        rows = []
        for index, page in enumerate(entry['pages']):
            local = checked(OUT, page)
            assert len(local) == page['rows']
            assert len(local) == 100 if index < len(entry['pages']) - 1 else len(local) < 100
            rows.extend(local)
        assert len({r['uuid'] for r in rows}) == len(rows)
        assert all(canonical(r['party']) == pair and r['type'] == 1 and r['mode'] == 11
                   and r['scale'] == 2 and r['splits']['42']['accurate']
                   and r['splits']['42']['ticks'] >= 0 for r in rows)
        assert all(rows[i]['startTime'] >= rows[i+1]['startTime'] for i in range(len(rows)-1))
        values = [r['splits']['42']['ticks'] for r in rows]
        # Avoid rounding the average or flooring its threshold: average is a
        # rational number, even though each observed room time is integral.
        vetted = len(values) > 5 and sum(values) * 3 <= len(values) * 980
        if vetted:
            eligible.add(pair)
        pinned = [uuid for uuid, r in old.items() if r['pair'] == pair]
        assert set(pinned) <= {r['uuid'] for r in rows}
        pairs.append(dict(pair=entry['pair'], count=len(rows), sumTicks=sum(values),
                          meanTicks=sum(values)/len(values) if values else None,
                          meanSeconds=sum(values)*0.6/len(values) if values else None,
                          vetted=vetted, fastRooms=sum(t <= 326 for t in values),
                          pinnedRooms=len(pinned),
                          oldest=rows[-1]['startTime'] if rows else None,
                          newest=rows[0]['startTime'] if rows else None,
                          recordings=[dict(uuid=r['uuid'], startTime=r['startTime'],
                                           bossTicks=r['splits']['42']['ticks']) for r in rows]))
    valid, checksums = [], 0
    for uuid, info in old.items():
        r = info['recording']
        for role in info['roots']:
            for file in r['files'].values():
                checked(ROOT / role, file)
                checksums += 1
        root = ROOT / info['roots'][0]
        raid, events = (checked(root, r['files'][k]) for k in ('raid', 'events'))
        assert canonical(raid['party']) == info['pair'] and raid['mode'] == 11 and raid['scale'] == 2
        boss = [e['tick'] for e in events if e['type'] == 123]
        assert boss == [r['bossSpawn']['ticks']] and boss[0] <= 326
        assert boss[0] == raid['splits']['42']
        assert raid['tobRooms']['nylocas']['ticksLost'] == 0
        if info['pair'] in eligible:
            valid.append(dict(uuid=uuid, pair=list(info['pair']), bossTicks=boss[0], roles=info['roots']))
    result = dict(asOf=snapshot['retrievedAt'], denominator='All public regular duo ToB recordings '
                  'with an accurate nonnegative boss-spawn split, for each named exact pair, '
                  'startTime no later than the pinned as-of bound; no boss upper-time filter.',
                  pairs=pairs, oldUniqueRecordings=len(old), verifiedRawChecksums=checksums,
                  eligiblePinnedRecordings=valid,
                  eligiblePinnedByCorpus={role: sum(role in r['roles'] for r in valid)
                                          for role in ('mage', 'ranger')})
    (OUT / 'summary.json').write_text(json.dumps(result, indent=2) + '\n')
    for p in pairs:
        print(p['pair'], p['count'], p['meanSeconds'], 'VETTED' if p['vetted'] else 'excluded')
    print('Pinned eligible by corpus:', result['eligiblePinnedByCorpus'])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--collect', action='store_true')
    args = parser.parse_args()
    if args.collect:
        collect()
    analyze()
