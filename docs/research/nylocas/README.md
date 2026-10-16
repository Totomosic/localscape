# Nylocas research corpus

Collected 2026-10-16 for the [accuracy report](../../nylocas-simulation-accuracy-report.md). The manifest pins13 public normal-mode completions: the user-provided duo plus six recent completed solos and six recent completed duos returned by Blert's API. There are repeated players and partners; this is not a random sample. All rooms report `ticksLost=0`, which does not guarantee every client observation is authoritative server state.

## Inputs and provenance

- [manifest.json](manifest.json) contains source/API/replay URLs, scales, party names, retrieval date, source commits and SHA-256 checksums of the original uncompressed response bytes.
- `recordings/<uuid>.raid.json.gz` preserves the public raid metadata, including room NPC spawn/removal summaries.
- `recordings/<uuid>.events.json.gz` preserves the public Nylocas-stage event response. Compression uses a zero timestamp for stable artifact bytes.
- [blert-waves-reference.json](blert-waves-reference.json) preserves the guide's published wave data at website commit `e5f44caceebd3a0979c9a89742f964f65841b670`, from [common/data/nylocas-waves.json](https://github.com/blert-io/blert/blob/e5f44caceebd3a0979c9a89742f964f65841b670/common/data/nylocas-waves.json). It is a reference input with known defects; it is not the verified result.

Public retrieval endpoints are read-only:

```text
https://blert.io/api/v1/raids/tob/<uuid>
https://blert.io/api/v1/raids/tob/<uuid>/events?stage=12
https://blert.io/api/v1/challenges?type=1&mode=11&scale=1&status=1&limit=6
https://blert.io/api/v1/challenges?type=1&mode=11&scale=2&status=1&limit=6
```

The last two endpoints are dynamic selection queries, not stable replay fixtures. The pinned response snapshots and manifest UUIDs are the research inputs.

## Event interpretation

| Field / value | Meaning |
| --- | --- |
| `tick` | Room-relative recorded tick. Instance clock phase must be inferred separately. |
| `xCoord`,`yCoord` | Source-map server-tile anchor, normalized from an instance, rather than interpolated model center. Big coordinates use the southwest convention. |
| `npc.roomId` | Recorder room identity. Do not assume this is the engine NPC slot or globally unique across raids. |
| `hitpoints >> 16` | Current HP estimated by the client recorder from its observations. |
| `hitpoints & 65535` | Recorder maximum HP. |
| Event7 /8 /9 | NPC spawn/update/removal. Event9 and metadata `deathTick` mean removal, not lethal impact. |
| Event120 /121 | Recognized nylo wave / inferred room-cap stall; `nylosAlive` includes newly spawned NPCs and NPCs not yet removed. |
| Event122 /123 | Cleanup / recorded boss-spawn marker. |
| NPC IDs 8342–8353 | Regular wave/split forms, ordinary and aggro. Boss IDs are excluded from the main totals. |
| `nylo.spawnType`1/2/3 | West/south/east lane. Other types represent split/unknown origins in this corpus. |
| `nylo.parentRoomId` | Inferred by Blert's time/position matching. Zero can mean an unassigned split parent; it does not reliably identify a lane spawn. |

The [source audit](../../nylocas-mechanics-source-audit.md) links the exact schema, normalization helpers, ID definitions and recorder methods. Each analysis document describes which data is observed and which interpretation is inferred.

## Reproduce offline

From the repository root, with Python 3 and no third-party packages:

```bash
python tools/research/nylocas_replays.py --output /tmp/nylocas-occupancy-summary.json
cmp /tmp/nylocas-occupancy-summary.json docs/research/nylocas/occupancy-summary.json
python tools/research/nylocas_targets.py
python tools/research/nylocas_lifecycle.py
python tools/research/nylocas_entry_contact.py
```

The latter three commands regenerate their CSV/JSON outputs in this directory. All four use checksum verification of the pinned raw responses before analysis. See each script's module documentation for its filters and limitations. None executes simulator code, contacts the website, or changes the server.

The entry/contact follow-up records [entry pause evidence](../../nylocas-entry-timing-evidence.md)
and [big arrivals below occupied north-pillar faces](../../nylocas-occupancy-evidence.md#follow-up-big-arrives-below-a-small-already-at-a-north-pillar).
Its JSON preserves entry counts, every qualifying aggro movement counterexample,
representative entry observations for the remaining categories, and all qualifying
north-pillar arrival pairs. First run `nylocas_targets.py` if regenerating the
fingerprint classification CSV from scratch.

The report's acceptance tests remain a future implementation plan. Passing the offline analysis checks means the reported statistics can be reproduced from these snapshots; it does not establish a complete routing algorithm or game-server correctness.
