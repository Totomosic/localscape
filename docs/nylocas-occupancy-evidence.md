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

## Follow-up: big arrives below a small already at a north pillar

Verified 2026-10-17 against the same checksum-pinned recordings. **A small
already on a north pillar's south face does not prevent a big from settling
one anchor row farther south, overlapping that small with its northern row.**
Pillar contact uses the big's full 2×2 footprint; its southwest anchor does not
have to be adjacent to the pillar. This is direct arrival evidence for the
specific north-pillar case, in addition to the parked overlaps above.

In the [supplied duo](https://blert.io/raids/tob/c77441a7-3caf-4d9d-a482-8ffe8d7f293d/nylocas),
big `43983` approaches NE while small `44058` already holds its south face:

| Room tick | Big `43983` anchor | Small `44058` anchor |
| --- | --- | --- |
| 202 | `(3300,4250)` | `(3300,4252)` |
| 203–246 | `(3300,4251)` | `(3300,4252)` |

Both have positive recorded HP throughout the 44-tick hold. The big occupies
X 3300–3301, Y 4251–4252; its northern row touches the pillar at Y 4253 and
overlaps the small at `(3300,4252)`. The southwest anchors remain distinct.
Small `43826` already occupies the other south-face tile `(3301,4252)` at tick
202 and holds through 230 with positive recorded HP. Thus **both small
south-face anchors are occupied when the big arrives**, and its northern row
overlaps both smalls.
The same pattern occurs at NW: in
[duo df03c4dc](https://blert.io/raids/tob/df03c4dc-1e1d-4f57-8e6c-403803205659/nylocas),
big `59101` moves from `(3290,4250)` to `(3290,4251)` at tick 232 while small
`59173` holds `(3291,4252)`; both hold with positive recorded HP through 274.

Run `python tools/research/nylocas_entry_contact.py` to regenerate
[entry-contact-observations.json](research/nylocas/entry-contact-observations.json).
It finds **60 big/small arrival pairs** across the corpus with a stationary
positive-HP small on the preceding tick and at least three consecutive
stationary positive-HP observations of both after the big arrives. A big may
overlap two smalls, so pairs are not unique arrivals or independent trials.
The filter requires an ordinary-form big, and matches overlapping footprint
tiles immediately south of NE or NW. It does not establish every legal approach,
terrain check, or diagonal collision rule.

The simulator already generated size-aware pillar contact positions, including
Y 4251 for bigs below northern pillars. Before the implementation follow-up
below, its `canOccupy` check rejected NPC blockers anywhere in the big's
footprint, and shared movement validated and reserved that whole footprint.
Those rules could not reproduce these witnesses; merely adding another pillar
destination would not fix the problem. See also the separate
[entry timing findings](nylocas-entry-timing-evidence.md), which do not justify
an unconditional one-tick pause for every player aggro.

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

At the audit snapshot, code used `npc.size` for candidate occupancy checks and
step validation in `NylocasBehaviour.canOccupy`/`pace`, rejecting Witness A/B
under full-footprint NPC reservations. See the [source audit](nylocas-mechanics-source-audit.md)
and [final report](nylocas-simulation-accuracy-report.md) for that baseline and
the wider implementation plan.

## Implementation follow-up, 2026-10-17

The user clarified that big Nylocas also use their southwest tile for terrain
checks. This is an implementation requirement supplied by the user, rather
than an additional terrain rule independently established by the corpus.

Lane spawns now set `Npc.collisionSize = 1` before world registration. The real
avatar remains size two. Shared movement, collision reservation, teleport and
removal use the configured collision footprint; other NPCs retain their normal
size by default. Encounter route searches, direct steps, pacing and contact-tile
terrain checks all use one-tile movement geometry. Pillar contact candidates,
melee contact, ranged/magic reach and explosion geometry retain the real size.
The preferred outer endpoint remains aligned with the big's full footprint;
permitting terrain under its other tiles does not force an overhanging endpoint.

Integration regressions cover both occupied small south-face tiles at NE and
NW, overlapping living bigs, reservation movement, occupied-anchor exclusion,
terrain under each of the other three footprint tiles, ordinary-NPC defaults,
and deletion/player death beside overlapping smalls. The death cases check that
the surviving smalls keep their collision flags.

The suggestion that a big's death clears every occupied flag under its 2×2
extent remains unverified. The old simulator did clear that full extent, but
that is not evidence of the original game's rule. Removal now releases only
the big's southwest reservation; it does not erase living neighbours' flags.
The recording's early natural-death handoffs and exact collision-release phase
remain separate timing questions.

## Southwest pillar collision correction, 2026-10-17

Starting the encounter without attacking exposed immediate pacing beside the
empty southwest pillar. The native support NPC (8358) has size three, while
pillar scenery and the encounter's contact positions use 2x2 geometry. Its
default 3x3 collision reservation extended north and east across all four
southwest small attack anchors and the big contact anchors on those faces.
Northwest and southeast retained one unblocked inward face; northeast retained
both. This was an oversized live reservation, rather than stale expiry flags.

Support spawns now set a 2x2 collision footprint before registration. The native
NPC size remains unchanged. Integration tests reproduce both small and big
arrivals at all four pillars with the actual support NPCs enabled; both southwest
cases failed before the correction. The full 31-wave occupancy check now also
includes supports and checks that the first southwest big reaches contact.
The earlier movement and expiry checks isolated the wave scripts without
`NylocasPillars`, so they could not detect the live support's obstruction.
