# Drummie duo mage recording corpus

Collected 2026-10-29 for the [strategy hierarchy](../../../nylocas-duo-mage-strategy.md) and [wave guide](../../../nylocas-duo-mage-waves.md). This is one mage's fast successful wave rooms, across multiple partners; it is not a random sample or proof of optimal play.

## Selection

Start from the [requested search](https://blert.io/search/challenges?party=Drummie&scale=2&mode=11&type=1&startTime=%3E%3D1788192000000&stage=ge12&split%3A42=ge0&sort=%2Bsplits%3A42%2C-startTime). Preserve its party, mode, scale, type, stage and **startTime ≥1788192000000** filters. Narrow split 42 to ≤326 ticks, the largest 0.6-second tick count ≤3:16. The read-only API request is:

```text
https://blert.io/api/v1/challenges?type=1&mode=11&scale=2&party=Drummie&stage=ge12&startTime=ge1788192000000&split%3A42=le326&sort=%2Bsplits%3A42%2C-startTime&extraFields=splits%3A42&limit=100
```

This returns **62 rooms**, below the 100-result limit, so every returned qualifying recording is included. Dates span 2026-09-05 to 2026-10-29. Retain raids that later wipe/reset if their Nylocas wave room reaches the qualifying boss spawn. Drummie appears under that name throughout. Partners are cutely meows (19), lmperatrice (13), Unluckers (8), Lacking S19 (7), Eres Un Amor (7), B asher (5), A rsh (2) and Selssul (1).

| Boss spawn | Ticks | Rooms |
| --- | --- | --- |
| 3:04.8 | 308 | 2 |
| 3:07.2 | 312 | 2 |
| 3:09.6 | 316 | 14 |
| 3:12.0 | 320 | 21 |
| 3:14.4 | 324 | 23 |

All 62 have accurate qualifying split flags, zero reported lost ticks, all 31 recognized wave markers and event-123 boss ticks equal to raid split 42. The dataset contains **4,554 mage attacks, 6,889 ranger attacks and 19,810 mage position samples** before boss spawn, filtered to regular wave/split NPCs for attack counts. Repeated actions by one player and repeated partner/session combinations are dependent observations.

## Preserved inputs and derived evidence

- [search-results.json](search-results.json): complete qualifying selection snapshot.
- [manifest.json](manifest.json): source API URLs, dates, parties, cutoff, split flags, original uncompressed SHA-256 checksums and file paths for all 62 rooms.
- `recordings/<uuid>.raid.json.gz` and `.events.json.gz`: raw public raid metadata and stage-12 events; deterministic gzip timestamps. Nineteen pairs are reused from the earlier ranger collection; the other 43 rooms were downloaded for this study. The overlap is not additional sample size.
- [attack-definitions.json](attack-definitions.json): the pinned [Blert primary definitions](https://github.com/blert-io/protos/blob/3c5bc5cfc315ff4266128601829e66615f3ef83d/attack_definitions.json), reused byte-for-byte from the ranger study with its revision/checksum retained.
- [attacks.csv](attacks.csv): both players' observed attacks, last wave, target birth identity/age, current style, weapon/category, estimated HP and player/target coordinates.
- [positions.csv](positions.csv): pre-boss mage tick-position samples and recorded cooldown field.
- [waves.csv](waves.csv): 1,922 room-wave rows, actual spawn tick, duration, room cap, first attack and modal mage tile.
- [lane-npcs.csv](lane-npcs.csv): lane-spawn identities, removals/lifetimes and primary-target attack counts for both roles, including targets that are never selected.
- [summary.json](summary.json): per-room data, attack/target/tile distributions by last-spawned wave, weapons and old-target exceptions.
- [critical-packets.json](critical-packets.json): per-room wave-12 pair, wave-21 barrage/sulphur, first due checks, late Dinh's and post-29 exceptions.
- [validation.json](validation.json) and [validation.md](validation.md): exact observable predicates, matching counts, missing rooms and replay witnesses. The wave guide embeds the generated table.
- [Independent check](../mage-independent-check.md): separate raw-event analysis of the 19 date-eligible rooms shared with the ranger corpus.

Every recording is viewable as `https://blert.io/raids/tob/<uuid>/nylocas`. The public endpoints require no account credentials.

## Interpretation

The [primary-schema and recorder audit](../ranger-event-interpretation.md) explains event meanings, coordinate normalization, attack recognition and split birth-wave semantics. Identify NPCs by raid-local `roomId`; derive immutable lane/slot/size/initial style from raid spawn metadata and current style from native IDs in the event stream. Apply all same-tick NPC updates before joining attacks, preserving partial fields. Last-spawned wave is the greatest spawn tick ≤attack tick, independent of response-array order. Verify every regular NPC's birth wave against that timetable.

Lane slots N/S are world north/south for west/east lanes; W/E are world west/east for the south lane. A big's slot suffix in an attack label merely describes its southwest spawn anchor. A target initially magic can correctly receive a melee attack after flickering; wave-22 south-W is the important example. `UNKNOWN_BARRAGE` is included as a recognized barrage observation even without a recognized equipped staff. No primary-target event is treated as a complete list of secondary victims.

`since_arena_anchor_entry` is deliberately only an operational position diagnostic: first recorded anchor inside x3290–3301, y4243–4254. It does **not** define the first actionable tick or prove the user's “one tick after entering” timing. Wave-12 scythe can hit at x3302 before that filter; the usual wave-22 Dinh's hits the target at y4243 on its first interior sample. The guide therefore uses the actual ages/positions and retains the user's relative action requirement separately.

The first **due** wave-14 check is actual `s13+8`; the first due wave-22 check is `s21+8`. Compare removals with these rather than a possibly delayed successful spawn. A same-tick removal and spawn still do not reveal within-tick server order. `deathTick` is removal, not the attack's impact tick. Recorded wave-stall events are inferred by the client. Wave-20 events report cap 24 in all rooms, following cap 12 on wave 19.

## Findings that limit literal imitation

The most common wave-12 primary is the north east small (61/62), despite the requested preferred south primary. First-tick-age-eight southern primaries have 0/62 support. Wave-21 barrage recognition is incomplete (44/62 explicit events), and a north sulphur at offset six has 40/62 support. Do not convert missing animation recognition into proof of a deliberate skipped action.

Wave-22 south-west Dinh's has 62/62 support at age ten or eleven; the user's alternative fighting-big special has 0/62 primary-target observations. That fighting big is the **east spawn**, initially magic and then fighting melee (native ID 8351), rather than the pillar-bound west melee spawn; the wave guide includes a raw transition witness. The named wave-26 south-big special appears in 53/62, of which six occur after wave 29. Events cannot fully establish the intended west-in/south-out splash victim set.

All 62 rooms contain attacks on ≤29 targets after 29 spawns: 286 mage attack observations, including the south wave-29 big in every room. The requested immediate cutoff is preserved in the proposed strategy as an explicit deviation. The older ranger study's delayed cleanup cutoff does not override the current instruction. New children retain their own birth wave.

Wave-28 inferred stall counts are zero (6 rooms), one (4), two (25), three (15), four (10), five (2). These frequencies support tolerating some retries; they do not identify a causal optimum or guaranteed maximum.

## Reproduce

From repository root, Python 3 with no external packages:

```bash
python tools/research/nylocas_mage.py
```

This works offline, verifies all 124 raw-response checksums and the attack definitions, asserts the selection/date/scale/mode/split/boss/wave/lost-tick invariants, and regenerates the derived artifacts. The independent note was calculated separately; it is not regenerated by this command.

To intentionally refresh the dynamic selection, run `python tools/research/nylocas_mage_collect.py` with network access, then the analysis. The collector caches recording files, checkpoints the manifest, spaces requests and backs off on errors using the existing public-replay retrieval helper. It refuses a result count at the API limit rather than claiming complete coverage without pagination. A refreshed selection requires rechecking prose counts and witnesses.

These recordings are structured tick-by-tick replay data, not video or a mouse-click log. Attack animation, estimated HP, removal, split birth and wave admission are separate observations. Missing events and merged observers limit precision; zero reported lost ticks is not proof of exhaustive recording. The research validates factual rotation anchors and movement choices, not an optimal causal hierarchy or performance of the requested policy refinements.
