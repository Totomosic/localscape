# Nylocas occupancy: testing the southwest-anchor hypothesis

Research date: 2026-10-16. Normal mode, six solo and seven duo completions. See the [corpus manifest](research/nylocas/manifest.json) for every recording, retrieval URL, party size, and checksum. This document concerns wave nylocas, not Vasilias or Prinkipas.

## Finding and its limits

**The recordings decisively reject exclusive 2×2 inter-NPC occupancy for big nylocas. They strongly support using the southwest anchor tile for nylo-versus-nylo occupancy decisions.** A big can walk through the other tiles of another big's footprint, and two living bigs can remain parked with overlapping footprints. A small can occupy the north or east portion of a living big.

This does **not** establish that every spatial check uses size one. Pillar contact still depends on the big's size, and terrain, player combat distance, area effects, and spawn placement need their own rules. It also does not establish an invariant that two anchors can never coincide: identical anchors occur in lanes, on split birth, and around removal of a blocking nylo. Treat collision, attack contact, visual size, lifetime membership, and spawn placement as separate concepts.

The strongest evidence is below. References use the replay's room tick, not the absolute instance tick. NPC identifiers are Blert `roomId`, not reusable server NPC indices.

## Coordinate provenance

Blert records the NPC actor's world location and normalizes instanced coordinates into source-map coordinates; it does not subtract a tile to create the overlap. Its own split-parent code explicitly describes the big's location as its southwest tile. The replay renderer likewise treats coordinates as an origin and adds a size offset for visual positioning. [Recorder location normalization](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/core/DataTracker.java#L352), [instance conversion](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/util/Location.java#L33), [explicit southwest convention](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/Nylo.java#L144), [renderer](https://github.com/blert-io/blert/blob/e5f44caceebd3a0979c9a89742f964f65841b670/web/app/utils/boss-room-state.ts).

For anchor `(x,y)`, the 2×2 footprint is `{(x,y),(x+1,y),(x,y+1),(x+1,y+1)}`. East increases x; north increases y. Positive HP is obtained from `hitpoints >> 16`, with maximum in the low 16 bits. HP observations do not expose every server sub-tick action; the long stable examples avoid depending on one transient health observation.

## Witness A: two bigs parked one tile apart

[User-supplied duo, Drummie and B asher](https://blert.io/raids/tob/c77441a7-3caf-4d9d-a482-8ffe8d7f293d/nylocas).

| NPC | Origin | Position and interval | Observed HP |
| --- | --- | --- | --- |
| `42465` | Wave 12, south, big melee | `(3292,4243)` continuously at ticks 98–138 | 16 throughout |
| `42806` | Wave 15, south, big magic | `(3292,4244)` continuously at ticks 125–142, and longer | 16 throughout this interval |

For every tick **125–138 inclusive**, the bigs coexist at their stationary anchors, with full recorded HP. Their footprints overlap on **both `(3292,4244)` and `(3293,4244)`**. In particular, the second big's anchor is inside the first big's footprint. A rule that blocks any overlap of full 2×2 footprints cannot reproduce this state.

The approach provides stronger evidence than a single screenshot:

| Tick | Big `42465` | Big `42806` |
| --- | --- | --- |
| 122 | `(3292,4243)` | `(3295,4243)` |
| 123 | `(3292,4243)` | `(3294,4243)` |
| 124 | `(3292,4243)` | `(3293,4243)` |
| 125 | `(3292,4243)` | `(3292,4244)` |
| 126–138 | `(3292,4243)` | `(3292,4244)` |
| 139 | despawn event at `(3292,4243)` | `(3292,4244)` |

At 124, the incoming big walks into the eastern portion of the parked big's footprint. At 125 it takes the diagonal step to the northern anchor. Full-footprint occupancy blocks both observations. Checking distinct southwest anchor tiles permits them. This establishes the occupancy distinction without depending on split spawning or a dying NPC.

## Witness B: small moves through a big's eastern and northern tiles

Same [user-supplied duo](https://blert.io/raids/tob/c77441a7-3caf-4d9d-a482-8ffe8d7f293d/nylocas). Big `42465` stays at `(3292,4243)` with 16 HP. Wave-13 small melee `42643` follows:

| Tick | Small `42643` | Recorded HP |
| --- | --- | --- |
| 106 | `(3295,4243)` | 8 |
| 107 | `(3294,4243)` | 8 |
| 108 | `(3293,4243)` | 8 |
| 109 | `(3292,4244)` | 8 |
| 110 | `(3292,4244)` | 0 |
| 111 | despawn at `(3292,4244)` | 0 |

Both 108 and 109 occupy the big's footprint. This also illustrates why collision removal and room-cap removal need separate timing: lethal HP, last movement, and despawn are separate observable events. Do not infer the precise collision release tick from HP alone.

## Witness C: a second, solo recording

[Solo Caywu](https://blert.io/raids/tob/ebaeaf20-9862-4548-af9f-1a37c52b020a/nylocas), ticks **138–139**:

| NPC | Description | Anchor | HP |
| --- | --- | --- | --- |
| `41493` | Wave-12 south big melee | `(3292,4243)` | 16 |
| `41618` | Wave-13 south small melee | `(3292,4244)` | 8 |

Both anchors remain unchanged on the preceding and following tick around these observations. This repeats the big/small overlap with one player and a different run. The accompanying [movement evidence](nylocas-movement-evidence.md) includes another duo in which a big repeatedly walks through the northern portion of a parked big while its assigned pillar is full.

## Corpus-level check

The offline script [nylocas_replays.py](../tools/research/nylocas_replays.py) verifies snapshot hashes and counts **pair-ticks**, restricting both anchors to `x3290..3301, y4243..4254`, requiring both observed HP values positive, and requiring at least one member of the pair to be big. It excludes all boss IDs. It does not assume an inferred target or parent relationship.

Across the 13 recordings:

| Measure | Count |
| --- | --- |
| Regular wave/split NPC lifetimes | 2,678 |
| Big lifetimes | 559 |
| Small lifetimes, including splits | 2,119 |
| Overlapping 2×2/1×1 footprints with distinct anchors | 6,936 pair-ticks |
| Above overlaps with both anchors unchanged at t−1, t, t+1 and positive observed HP on all three ticks | 5,024 pair-ticks |
| Big-involving pairs with the same anchor in the arena | 576 pair-ticks |

Repeated observations of the same pair are not independent confirmations. These counts characterize the corpus; the explicit multi-tick witnesses establish the behavioral contradiction. Full per-recording counts are in [occupancy-summary.json](research/nylocas/occupancy-summary.json).

Reproduce without network access:

```bash
python tools/research/nylocas_replays.py --output /tmp/occupancy-summary.json
cmp /tmp/occupancy-summary.json docs/research/nylocas/occupancy-summary.json
```

## Why identical anchors do not disprove the useful hypothesis

**Lane overlap:** in the supplied duo, wave-9 west small `41986` and big `41987` both have anchor `(3290,4248)` at tick 65. Their lane paths already share y4248 before reaching the arena. The big then changes to ID8352 and reaches observed zero HP at 66; the small keeps moving northeast. Spawn/lane movement cannot be implemented as generic exclusive occupancy at every step. [Replay](https://blert.io/raids/tob/c77441a7-3caf-4d9d-a482-8ffe8d7f293d/nylocas).

**Natural-death handoff:** in solo Pw5, small `33638` stays at `(3292,4244)` with observed 8 HP through tick 350 and despawns at 351. Big `34729`, after pacing, reaches that same anchor at 350 and remains there. In duo go bestie/Michael41000, split `63815` stays at `(3300,4245)` with observed 8 HP through 257, despawns at 258, and big `64113` arrives at that anchor on 257. These are evidence that collision availability can precede the replay's despawn event, plausibly during the natural-death turn/animation. They are not sufficient to establish exact server processing order. [Solo](https://blert.io/raids/tob/a37210fe-5737-480f-8fca-518e56205e9b/nylocas), [duo](https://blert.io/raids/tob/1073635f-5326-4bac-a147-8fdf6299f761/nylocas).

**Splits:** newly spawned children can start on a parent's anchor or nearby occupied tiles. Birth placement is not an ordinary movement decision. Parent-child metadata is inferred by the recorder; see the [lifecycle evidence](nylocas-lifecycle-evidence.md) before treating it as authoritative server state.

The supported claim is therefore specific: **a big's other three footprint tiles are not continuously reserved against other nylocas by the room's inter-NPC collision rule.** A southwest-anchor occupancy model with explicit spawn/lane and death-state behavior is a strong candidate. A universal one-tile mutex is not yet established.

## Implementation acceptance tests

1. **Big/big parked overlap:** arrange the Witness A approach. Permit `42806` to walk through `(3293,4243)` and settle at `(3292,4244)` while `42465` stays at `(3292,4243)`. Both retain size two. Reject a collision implementation that reserves the first big's full footprint against other nylocas.
2. **Small/big overlap:** replay Witness B's positions through tick109. Keep big HP positive throughout. Do not allow a passing implementation merely to kill or despawn the blocker early.
3. **Occupancy versus contact:** preserve size-two contact with the pillar. For example, a big can stop one anchor row farther south of the northeast pillar than a small. Passing overlap tests must not shrink all geometry to size one.
4. **Full-pillar waiting:** replay the small and big congestion fixtures from the movement report. Check every tick, including the tile that becomes available and the ensuing move. Merely asserting that the NPC eventually reaches a pillar cannot validate the waiting rule.
5. **Distinct blocker at its southwest tile:** use observed blocked approaches to constrain reservations at the anchor. This needs more than the positive overlap witnesses; reproduce a congested trace with simultaneous blockers and document processing order assumptions.
6. **Birth and lane exceptions:** allow the observed wave-9 lane coincidence and child birth overlaps. Do not apply an unconditional global uniqueness assertion to all nylocas.
7. **Collision release versus despawn:** reproduce the two natural-death handoffs without removing the blocker from room-cap membership early. Keep the timing model explicit until server-phase ordering is resolved.
8. **Unproven geometry:** separately test terrain collision, player blocking, diagonals, combat range and area damage. These recordings do not establish that each uses the same anchor rule.

Current code uses `npc.size` for both candidate occupancy checks and step validation in `NylocasBehaviour.canOccupy`/`pace`; this will reject Witness A/B under full-footprint NPC reservations. Change the encounter-specific occupancy behavior while preserving independently validated geometry. See the [source audit](nylocas-mechanics-source-audit.md) and [final report](nylocas-simulation-accuracy-report.md) for the wider implementation plan.
