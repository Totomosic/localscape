#!/usr/bin/env python3
"""Collect public Blert ranger evidence; requires network, no account or secrets.

Run only to refresh the corpus intentionally. This updates the dynamic search
snapshot and manifest, reuses existing recording files, and checkpoints each
room. For the pinned offline study run nylocas_ranger.py instead.
"""

import datetime
import gzip
import hashlib
import json
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2] / 'docs/research/nylocas/ranger'
SEARCH_URL = ('https://blert.io/search/challenges?party=cutely+meows&scale=2&mode=11'
              '&type=1&stage=ge12&split%3A42=ge0&sort=%2Bsplits%3A42%2C-startTime')
QUERY = {'type': 1, 'mode': 11, 'scale': 2, 'party': 'cutely meows', 'stage': 'ge12',
         'split:42': 'le326', 'sort': '+splits:42,-startTime',
         'extraFields': 'splits:42', 'limit': 100}


def retrieve(url):
    for attempt in range(8):
        time.sleep(1.5)
        try:
            with urllib.request.urlopen(url, timeout=90) as response:
                raw = response.read()
            json.loads(raw)
            return raw
        except urllib.error.HTTPError as error:
            if error.code not in (429, 500, 502, 503, 504) or attempt == 7:
                raise
            retry = error.headers.get('Retry-After', '')
            delay = min(60, int(retry)) if retry.isdigit() else min(60, 15 * (attempt + 1))
            print(f'HTTP {error.code}; retrying in {delay}s', flush=True)
            time.sleep(delay)
    raise RuntimeError('Retrieval exhausted')


def main():
    (ROOT / 'recordings').mkdir(parents=True, exist_ok=True)
    query = urllib.parse.urlencode(QUERY)
    search = retrieve('https://blert.io/api/v1/challenges?' + query)
    rows = json.loads(search)
    if len(rows) == QUERY['limit']:
        raise ValueError('Result limit reached: add pagination before claiming complete coverage')
    (ROOT / 'search-results.json').write_bytes(search)
    manifest = dict(retrievedDate=datetime.date.today().isoformat(), selectionUrl=SEARCH_URL,
                    query=query, cutoffTicks=326, recordings=[])
    existing_manifest = ROOT / 'manifest.json'
    if existing_manifest.exists():
        previous = json.loads(existing_manifest.read_text())
        if 'attackDefinitions' in previous:
            manifest['attackDefinitions'] = previous['attackDefinitions']
    for row in rows:
        split = row['splits']['42']
        assert split['accurate'] and split['ticks'] <= 326
        entry = dict(uuid=row['uuid'], startTime=row['startTime'], party=row['party'],
                     bossSpawn=split, files={})
        for kind, suffix in [('raid', ''), ('events', '/events?stage=12')]:
            url = 'https://blert.io/api/v1/raids/tob/' + row['uuid'] + suffix
            path = ROOT / 'recordings' / f"{row['uuid']}.{kind}.json.gz"
            if path.exists():
                raw = gzip.decompress(path.read_bytes())
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
