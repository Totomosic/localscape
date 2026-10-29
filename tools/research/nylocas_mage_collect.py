#!/usr/bin/env python3
"""Refresh the public, date-filtered Drummie mage corpus (network required)."""

import datetime
import gzip
import hashlib
import json
import shutil
import urllib.parse
from pathlib import Path

from nylocas_ranger_collect import retrieve


ROOT = Path(__file__).resolve().parents[2] / 'docs/research/nylocas/mage'
SEARCH_URL = ('https://blert.io/search/challenges?party=Drummie&scale=2&mode=11'
              '&type=1&startTime=%3E%3D1788192000000&stage=ge12&split%3A42=ge0'
              '&sort=%2Bsplits%3A42%2C-startTime')
QUERY = dict(type=1, mode=11, scale=2, party='Drummie', stage='ge12',
             startTime='ge1788192000000', **{'split:42': 'le326',
             'sort': '+splits:42,-startTime', 'extraFields': 'splits:42', 'limit': 100})


def main():
    (ROOT / 'recordings').mkdir(parents=True, exist_ok=True)
    query = urllib.parse.urlencode(QUERY)
    search = retrieve('https://blert.io/api/v1/challenges?' + query)
    rows = json.loads(search)
    if len(rows) >= QUERY['limit']:
        raise ValueError('Result limit reached; implement pagination before claiming coverage')
    (ROOT / 'search-results.json').write_bytes(search)
    ranger_root = ROOT.parent / 'ranger'
    definitions = json.loads((ranger_root / 'manifest.json').read_text())['attackDefinitions'].copy()
    shutil.copyfile(ranger_root / definitions['path'], ROOT / definitions['path'])
    manifest = dict(retrievedDate=datetime.date.today().isoformat(), selectionUrl=SEARCH_URL,
                    query=query, cutoffTicks=326, startTimeMillis=1788192000000,
                    attackDefinitions=definitions, recordings=[])
    for row in rows:
        split = row['splits']['42']
        assert split['accurate'] and split['ticks'] <= 326
        entry = dict(uuid=row['uuid'], startTime=row['startTime'], party=row['party'],
                     bossSpawn=split, files={})
        for kind, suffix in [('raid', ''), ('events', '/events?stage=12')]:
            url = 'https://blert.io/api/v1/raids/tob/' + row['uuid'] + suffix
            path = ROOT / 'recordings' / f"{row['uuid']}.{kind}.json.gz"
            cached = ranger_root / 'recordings' / path.name
            if path.exists():
                raw = gzip.decompress(path.read_bytes())
            elif cached.exists():
                raw = gzip.decompress(cached.read_bytes())
                path.write_bytes(gzip.compress(raw, mtime=0))
            else:
                raw = retrieve(url)
                path.write_bytes(gzip.compress(raw, mtime=0))
            entry['files'][kind] = dict(path=str(path.relative_to(ROOT)), url=url,
                                       sha256=hashlib.sha256(raw).hexdigest(),
                                       uncompressedBytes=len(raw))
        manifest['recordings'].append(entry)
        (ROOT / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
        print(f"Collected {len(manifest['recordings'])}/{len(rows)}", flush=True)


if __name__ == '__main__':
    main()
