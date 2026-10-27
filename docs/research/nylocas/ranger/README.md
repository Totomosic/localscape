# Cutely meows duo ranger recording corpus

Collected 2026-10-27 for the [strategy hierarchy](../../../nylocas-duo-ranger-strategy.md) and [wave breakdown](../../../nylocas-duo-ranger-waves.md). These are observations of one player's successful fast wave rooms, not a random sample or proof of an optimal policy.

## Selection and coverage

Start from the [user's search](https://blert.io/search/challenges?party=cutely+meows&scale=2&mode=11&type=1&stage=ge12&split%3A42=ge0&sort=%2Bsplits%3A42%2C-startTime). Apply boss-spawn split 42 ≤326 ticks: the largest 0.6-second tick count at or below 3:16. Request that split explicitly in the response. The read-only request was:

```text
https://blert.io/api/v1/challenges?type=1&mode=11&scale=2&party=cutely%20meows&stage=ge12&split%3A42=le326&sort=%2Bsplits%3A42%2C-startTime&extraFields=splits%3A42&limit=100
```

It returned **93 results**, below the 100-result limit, so no pagination or fastest-N subsample was needed. All 93 have an accurate qualifying split, a full set of 31 recognized wave markers, zero reported lost ticks, normal mode, and scale two. Boss-marker ticks independently match the raid's split 42 in every room. Raids that subsequently wipe or reset are retained; the completed Nylocas wave room is what matters. This includes the latest qualifying room even where an earlier raid has a faster boss spawn.

| Boss spawn | Ticks | Rooms |
| --- | --- | --- |
| 3:04.8 | 308 | 1 |
| 3:07.2 | 312 | 4 |
| 3:09.6 | 316 | 12 |
| 3:12.0 | 320 | 25 |
| 3:14.4 | 324 | 51 |

Room dates run from 2026-07-13 to 2026-10-26. The ranger appears as `cutely meows` in 51 and `FishxBubble` in 42; both resolve to `currentUsername=cutely meows`. Partners are Exhailing (48), Drummie (36), Lacking IQ (6), and intrr (3). There are **10,416 ranger attack events**, 6,954 partner attack events, and 29,872 ranger position samples before the boss-spawn marker, after filtering attacks to regular nylos. This is broad coverage of the requested search, with substantial dependence between observations from the same player/partner/session.

## Preserved inputs

- [search-results.json](search-results.json): the qualifying search result snapshot, including accurate split flags.
- [manifest.json](manifest.json): source URLs, roster, dates, boss split, file paths and original uncompressed SHA-256 checksums for all 93 rooms.
- `recordings/<uuid>.raid.json.gz`: raw raid responses, including spawn summaries.
- `recordings/<uuid>.events.json.gz`: raw stage-12 event responses. Gzip timestamps are zero; checksums refer to the original uncompressed bytes.
- [attack-definitions.json](attack-definitions.json): pinned [primary attack definitions](https://github.com/blert-io/protos/blob/3c5bc5cfc315ff4266128601829e66615f3ef83d/attack_definitions.json), identifying weapons and cooldowns. Its checksum and revision are in the manifest.

Every replay is directly accessible as `https://blert.io/raids/tob/<uuid>/nylocas`. API inputs are public, read-only, and require no account credentials. Original snapshots are retained to keep the analysis reproducible if the live search, player names, merged recordings or website change.

## Interpretation and analysis

The [event-interpretation note](../ranger-event-interpretation.md) links the exact primary schema/recorder definitions and independently examines eight fast rooms with their complementary mage actions. The full analysis uses the raid NPC table for immutable birth identity, the NPC event stream for attack-time style/position, event 5 for observed attack animations, event 4 for player tiles, event 120 for recognized wave births and event 123 for boss spawn.

The wave current at an attack is computed by the greatest spawn tick ≤attack tick, **not array order**. This handles attacks recorded before a same-tick wave event. Target birth wave is checked against the wave current at the target's birth; there are no mismatches across the regular NPC births in this corpus. Split birth wave is the child's own wave, not its parent's. NPC room identity is joined within each raid and is not globally unique.

The target's initial style and current style are kept separately. A target named `w20 west-S big melee` is still that same spawn when attacked in ranged form. Lane slot suffixes distinguish two smalls, while a big's S/W suffix only reflects its southwest spawn anchor. Missing parent assignment is not treated as proof that a crab is lane-spawned.

The analysis yields:

| Artifact | Use |
| --- | --- |
| [attacks.csv](attacks.csv) | Both players' tick, last wave, target identity/birth/age/current style, weapon and attack type, player/target tiles, observed HP and distance. |
| [waves.csv](waves.csv) | 2,883 room-wave rows: actual spawn tick, interval, ranger first attack, attack count and modal tile. A first attack on an old target is not a claim that it is that wave's only priority. |
| [positions.csv](positions.csv) | Full pre-boss ranger tick-position samples and recorded cooldown field. |
| [summary.json](summary.json) | Per-room boss/wave/stall details; per-wave first-action, target and movement distributions. Target counts deduplicate rooms, not attacks. |
| [validation.json](validation.json) | Explicit target/weapon/age predicates, support counts, current-wave distributions, exact witnesses and missing-room IDs. |
| [validation.md](validation.md) | Human-readable predicate report, also included in the wave guide. |

Validation counts test a particular timing/weapon/current-style window, **not whether the target was ever attacked by any means**. For example, the wave-23 immediate-bow predicate matches 92/93; the remaining room bows the same target at age three instead of ages one or two. Similarly, an absent exact-age opening bow may be a delayed or differently recognized attack, not evidence that the lane assignment changed. The wave-6 east-big predicate explicitly requires claw-special attack type 20.

For movement, `(3293,4248)` is the per-room modal tile in 92/93 wave-19 intervals and 90/93 wave-20 intervals. The strategy documents list observed staging tiles rather than claiming an exact click path can be recovered. For age, the median ranger target age is eight ticks before wave 20 and seven during waves 20–29. This supports a focus on young work but does not prove pairwise preference when multiple viable targets were available.

## The late cutoff

All 93 rooms contain ranger attacks on birth-wave ≤29 targets after wave 29 spawns. The wave-29 west north ranged-flicker shot at target age six appears in 92/93. The user clarified the desired baseline: **preserve fresh lane attacks, apply the ≤29 cutoff in final cleanup**. The strategy follows that clarification instead of deleting the observed lane packet.

Some human exceptions remain even late: 31/93 rooms include an attack on a birth-wave ≤29 target more than 13 ticks after wave 31's spawn. This is a descriptive time threshold, not a recorded “final lane packet complete” state. Such attacks are not promoted into mandatory agent actions. Fresh children born in wave 30+ remain eligible even if their parent is older.

Wave-28 stall counts in these qualifying rooms are 0 (6 rooms), 1 (6), 2 (38), 3 (27), 4 (12), and 5 (4). Thus accepting two or three retries fits a common successful outcome, but the source does not establish a universal limit or a causal optimum.

## Reproduce

From the repository root, Python 3 and no external packages:

```bash
python tools/research/nylocas_ranger.py
```

This runs offline, verifies all 186 raw-response checksums, asserts scale/mode/cutoff, all 31 wave markers and boss/split agreement, and regenerates the six derived artifacts. The wave guide's embedded table is the contents of `validation.md`; compare them when refreshing the corpus.

To intentionally refresh the dynamic public selection, use `python tools/research/nylocas_ranger_collect.py` with network access, then re-run the offline analysis. The collector reuses existing recording files, checkpoints the manifest, spaces requests, and backs off on rate limits. It refuses to claim complete coverage if the search reaches the result limit. It does not refresh the separately pinned attack definitions. Updating the corpus requires rechecking prose counts and examples; do not silently equate a later selection with this pinned study.

## Limits

Blert attack events infer animations, not intention, successful damage, exact mouse clicks or every AoE victim. Client HP is estimated. Zero reported lost ticks does not prove every action is captured or every merged timestamp is authoritative. NPC removal, HP zero, projectile launch and cap-slot release are different events. A wave's `nylosAlive` is its observed count after admission, not a before-check server count; stalls are inferred. See the [mechanics audit](../../../nylocas-mechanics-source-audit.md).

The strategy is an inferred, implementable hierarchy supported by observed rotation anchors and movements. It has not been run as an agent or compared experimentally against other policies. Frequency alone cannot establish optimality, missed actions can have several causes, and partner adaptation cannot be inferred solely from one primary-target event.
