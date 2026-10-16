# Nylocas movement and pillar-target evidence

Research date: 2026-10-16. Scope: normal-mode waves in six solo and seven duo recordings. This document records observed coordinates and separates them from inferred game rules. The [corpus manifest](research/nylocas/manifest.json) pins all 13 recordings, their original API URLs, SHA-256 values, recorder/source commits, party sizes and lost-tick counts. All have zero reported lost ticks. This is a convenience sample, with repeated players; 13 recordings are not 13 independent experiments.

## Results and confidence

- **Observed:** the corpus contains exactly 120 distinct lane-spawn fingerprints, each appearing once in each recording: 1,560 lane NPCs. A fingerprint is `(wave, lane, initial style, big, spawn x, spawn y)`.
- **Guide classification, checked against native transformations:** 35 fingerprints are player aggros and 85 are pillar-directed, using the guide's aggro flags. That gives 455 aggro and 1,105 pillar-directed lane NPCs across the corpus. All 35 guide-designated aggros have at least one later observation with native aggro ID 8348–8353, which validates these joins. Aggros initially spawn using the same incoming NPC IDs as ordinary lane Nylocas; initial ID alone cannot classify them. The aggro flags are not an observed server target field.
- **Strong inference:** all 85 pillar-directed fingerprints have an identifiable pillar target, supported by at least one recorded contact. Most have repeated support. Waves 2 east and 11 east have only one useful arrival each because players usually kill them first. Wave 6 west has only two arrivals lasting two positive-HP observations each; its direction and queueing are additionally visible in the solo example below.
- **Observed:** a lane does not have one universal destination. For example, west-wave 2 targets NE, west-wave 4 targets NW and west-wave 5 big targets SW. Different waves can take the same lane toward any of those pillars. The fixed assignment belongs to the individual wave spawn. This agrees with the [mechanics guide](https://blert.io/guides/tob/nylocas/mechanics).
- **Observed:** both sizes walk at most one tile per coordinate axis in consecutive recorded ticks. In 44,365 consecutive tick intervals for the 1,560 lane NPCs, none moved more than one in either axis. Diagonal movement therefore covers one diagonal tile in one tick, not two walking ticks. This count includes stationary and death-animation intervals.
- **Observed:** a full approach produces repeated sideways and diagonal movements, including reversals. A model that makes blocked Nylocas stand still in a FIFO queue will fail the fixtures below.
- **Important distinction:** big Nylocas contact pillars with their 2×2 extent, while inter-NPC movement occupancy permits overlapping visual extents. A 1×1 anchor occupancy implementation must not reduce big pillar attack reach to 1×1. The separate occupancy report evaluates the SW-anchor hypothesis in more detail.

## Reproduction and interpretation

Run `python tools/research/nylocas_targets.py` from the repository root. It validates the compressed recordings against the manifest's original-content hashes using the shared corpus loader, and regenerates:

- [lane-spawn-targets.csv](research/nylocas/lane-spawn-targets.csv): all 120 stable fingerprints, guide-slot reference, aggro classification, inferred target, support counts and one precise evidence interval per pillar spawn.
- [target-observations.csv](research/nylocas/target-observations.csv): the 85 pillar-directed rows.
- [native-aggro-transitions.csv](research/nylocas/native-aggro-transitions.csv): first later native-aggro ID observation for each qualifying NPC, including tick, lifetime age, HP and inferred initial target.
- [target-summary.json](research/nylocas/target-summary.json): fingerprint totals, movement-step counts and observed stationary big contact anchors.
- [movement-fixtures.json](research/nylocas/movement-fixtures.json): three local trajectories with nearby NPC coordinates, sizes, positive/zero HP observations and despawns on every included tick.

`guide_slot` is a cross-reference to the frozen [Blert wave reference](research/nylocas/blert-waves-reference.json), not a reliable physical north/south description for a big/small pair. The physical fingerprint fields should be used to identify the replay NPC. In wave 9 west, the big ranged and small melee both spawn with anchor `(3281,4248)`, despite occupying distinct guide slots. Size and initial style distinguish them. A generic sort by lane Y would silently swap their aggro flags.

The [pinned guide data](https://github.com/blert-io/blert/blob/e5f44caceebd3a0979c9a89742f964f65841b670/common/data/nylocas-waves.json) supplies reference flags, while observed coordinates and sizes supply the replay fingerprints. The guide's lane-slot comments and some rotation descriptions should not override recorded facts; see the source audit in the overall report.

The target inference uses cardinal edge contact between an NPC's size-appropriate rectangle and one 2×2 pillar rectangle. For each NPC it prefers its first stationary contact lasting two consecutive positive-HP observations, otherwise its first observed contact. Across recordings it chooses the most frequently contacted pillar, preserving contradictory observations in `other_first_contact_counts`. No regular-NPC target or pillar-hit event is available in these records. A contact is evidence of an apparent destination, not direct proof that a pillar damage roll occurred.

Late spawns sometimes first settle beside a different pillar: wave 29 east has one NW outlier; wave 30 south-east small has one NE; wave 30 west big has one NW; wave 31 east-south small has two NE; wave 31 south-east small has one NE and one NW. These outliers must remain visible. Eleven nominal pillar-directed fingerprints from waves 29–31 also later transform to native aggro IDs in at least one recording. Their transformation ages range from 10 to 50 ticks, so this is not a universal lifetime-age switch. In solo 65c9f142, five SW-directed NPCs transform together at room tick 465, with different ages 35, 27, 27, 15 and 15. The same five fingerprints transform together at tick 457 in solo a37210fe (ages 50, 38, 38, 30, 30), and tick 438 in solo eb3793f3 (ages 38, 26, 26, 18, 18). The synchronized same-pillar cohorts support a shared target-state event; they do not prove its exact cause. Other pillar cohorts transform at different times, which argues against a single global cleanup switch. They do not establish randomized initial assignments: the recorder does not track the pillars, and cannot resolve pillar destruction, subsequent target changes or the exact cause of each altered route. The majority assignment is consistent with the guide's fixed-target statement; post-collapse behavior needs dedicated additional evidence.

## Coordinate conventions and attack positions

Coordinates below are world-tile anchors with X increasing east and Y increasing north. Blert's [replay terrain source](https://github.com/blert-io/blert/blob/e5f44caceebd3a0979c9a89742f964f65841b670/web/app/(challenges)/raids/tob/%5Bid%5D/(bosses)/nylocas/page.tsx) defines the arena as `(3290,4243)` through `(3301,4254)` and the pillar rectangles as:

| Pillar | Occupied tiles |
| --- | --- |
| SW | X 3290–3291, Y 4243–4244 |
| SE | X 3300–3301, Y 4243–4244 |
| NW | X 3290–3291, Y 4253–4254 |
| NE | X 3300–3301, Y 4253–4254 |

This gives NE's four ordinary **small** attack anchors on walkable inner faces: `(3299,4253)`, `(3299,4254)`, `(3300,4252)` and `(3301,4252)`. There is no walkable outer north/east side inside the arena. These are not four universal slots for all sizes.

In [duo 1073635f](https://blert.io/raids/tob/1073635f-5326-4bac-a147-8fdf6299f761/nylocas), wave 17 east big `roomId=62546` reaches `(3300,4251)` at tick 139 and remains there through positive-HP tick 182. Its top row is Y=4252, adjacent to NE's bottom row Y=4253. Wave 17 south big `62545` reaches `(3298,4243)` at tick 141 and remains through tick 182: its rightmost occupied column is X=3299, adjacent to SE's left column X=3300. Their anchors are two tiles from the pillar's nearest occupied row/column. An anchor-only attack-range calculation would falsely keep both walking.

Stationary big contact anchors observed across the corpus include NE `(3300,4251)`, `(3299,4251)` and `(3298,4253)`; SW `(3292,4243)`, `(3292,4244)`, `(3290,4245)` and `(3291,4245)`; SE `(3298,4243)`, `(3298,4244)`, `(3299,4245)` and `(3300,4245)`; and NW `(3292,4253)`, `(3290,4251)` and `(3292,4252)`. These are observed anchors, not an exhaustive statement of legal big attack positions or a pillar capacity. Pure rectangle contact yields additional candidates whose legality also depends on static terrain.

Typical unobstructed big approaches in that same replay:

| NPC | Tick-by-tick anchor sequence near arena entry |
| --- | --- |
| Wave 17 east big `62546`, NE | 136 `(3301,4248)`; 137 `(3300,4249)`; 138 `(3300,4250)`; 139 `(3300,4251)` |
| Wave 17 south big `62545`, SE | 138 `(3295,4243)`; 139 `(3296,4243)`; 140 `(3297,4243)`; 141 `(3298,4243)` |

## Full-pillar fixture A: small waits and oscillates, then takes a vacancy

Source: [solo eb3793f3](https://blert.io/raids/tob/eb3793f3-ce4a-4fcf-8ef9-e2098d39914a/nylocas), wave 6 west small mage, `roomId=33313`, spawned tick 36 at `(3281,4249)`, targeting NE. The JSON fixture includes ticks 49–61.

At ticks 53–54 all four small attack anchors are occupied:

| Occupant | Kind | Anchor |
| --- | --- | --- |
| `65461` | Wave 2 west small | `(3299,4254)` |
| `65495` | Wave 3 west small | `(3299,4253)` |
| `65526` | Wave 4 east small | `(3301,4252)` |
| `33357` | Split, parent `65525` | `(3300,4252)` |

`33357` has recorded HP 0 at ticks 53–54 and despawns at 55. Its south-face vacancy is filled by split `33478` at tick 57. The west approach remains blocked until tick 60.

| Tick | Moving NPC `33313` anchor |
| --- | --- |
| 50 | `(3295,4254)` |
| 51 | `(3296,4254)` |
| 52 | `(3297,4254)` |
| 53 | `(3298,4254)` |
| 54 | `(3298,4253)` |
| 55 | `(3298,4254)` |
| 56 | `(3298,4253)` |
| 57 | `(3298,4254)` |
| 58 | `(3297,4253)` |
| 59 | `(3298,4254)` |
| 60 | `(3299,4254)` |
| 61 | `(3299,4254)` |

At tick 60 `65461` emits its despawn event and `33313` occupies the vacated west-face anchor. The path does not retarget another pillar when congested. It oscillates locally and takes a vacancy. The extra retreat at tick 58 occurs while split `33454` approaches through `(3296,4253)`; the fixture records those nearby actors so a later implementation can investigate actor order and local collision effects.

**What this does not prove:** an explicit pillar-capacity counter, universal first-in-first-out priority, random-step probabilities, or a complete direction preference. The four anchor occupancy observations explain why the local approach was congested without inventing a separate capacity mechanic.

## Full-pillar fixture B: big oscillation and anchor overlap during vacancy release

Source: [duo 1073635f](https://blert.io/raids/tob/1073635f-5326-4bac-a147-8fdf6299f761/nylocas), wave 27 east big mage, `roomId=64113`, spawned tick 228 at `(3309,4248)`, targeting SE. Fixture ticks 236–259.

At tick 238 its anchor is `(3300,4246)`. The nearby SE north face has:

- big `63956` anchored `(3299,4245)`;
- split `63815` anchored `(3300,4245)`;
- split `64126` anchored `(3301,4245)`.

The moving big alternates `(3299,4246)` and `(3300,4246)` on ticks 239–244, then `(3299,4247)` and `(3300,4246)` on ticks 245–252. It takes `(3299,4246)` at 253, `(3300,4246)` at 254, `(3299,4246)` at 255 and `(3300,4246)` at 256. At 257 it moves to `(3300,4245)` and stays.

When it is anchored `(3299,4246)`, its 2×2 visual rectangle overlaps big `63956` at `(3299,4245)`. This rules out universally blocking on entire 2×2 rectangles, while keeping distinct SW anchors consistent with the ordinary occupancy hypothesis.

At tick 257 split `63815` is still recorded at `(3300,4245)` with HP 8, so both share an anchor for one observed tick; the split despawns at 258. This is a lifecycle/collision-release boundary case, not evidence that stationary live NPCs may freely stack. The same pattern repeats in fixture C.

## Full-pillar fixture C: three-step oscillation before a big reaches SW

Source: [solo a37210fe](https://blert.io/raids/tob/a37210fe-5737-480f-8fca-518e56205e9b/nylocas), wave 25 south big ranged, `roomId=34729`, spawned at tick 327, targeting SW. Fixture ticks 337–352.

At 337–338 the big approaches `(3295,4243)` then `(3294,4243)`. Ticks 339–349 repeat the three-step loop `(3293,4244)` → `(3293,4243)` → `(3294,4243)` → `(3293,4244)`, ending at `(3293,4243)` on 349. At 350 it enters `(3292,4244)` and remains there. Small `33638` is still recorded on that exact anchor with HP 8 at 350 and despawns at 351.

The distinct two-step and three-step cycles across these fixtures show why a generic “step straight toward pillar, otherwise wait” rule cannot reproduce observed congestion. They do not establish whether the complete movement decision is deterministic under fully known state.

## Fixed target table

Each token is `S/B initial-style → target`, where S=small, B=big, M=melee, R=ranged, G=magic, and P=player aggro. Multiple tokens follow guide-slot order, which is only an index. Physical spawn coordinates in the CSV resolve ambiguous lane sides; wave9's two west spawns share an anchor. The wave-30 east big uses x3310, while the other east bigs use x3309. Styles here are recorded initial styles, including the wave30 south mage that differs from the guide.

| Wave | East | South | West |
| --- | --- | --- | --- |
| 1 | SM → NW | SG → NE | SR → P |
| 2 | SR → SE | SM → P | SG → NE |
| 3 | SG → P | SR → SE | SM → NE |
| 4 | SM → NE | BG → SW | SR → NW |
| 5 | SG → NE | SM → SE | BR → SW |
| 6 | BM → SE | SR → NW | SG → NE |
| 7 | SM → NW | SG → NE; BR → P | — |
| 8 | SR → SE | SM → NW | BG → P |
| 9 | SG → NW | — | BR → P; SM → NE |
| 10 | SR → P; BR → NE | SR → SE; SR → SW | SR → NW; SR → P |
| 11 | SG → P; SG → NE | SG → SE; SG → SW | BG → P |
| 12 | SM → P; SM → NE | BM → SW | SM → NW; SM → P |
| 13 | BM → P | SR → SE; SM → SW | SR → NW; SG → P |
| 14 | BR → P | SG → SE; SR → SW | SG → NW; SM → P |
| 15 | SG → P; SR → NE | BG → SW | SM → NW; SR → P |
| 16 | SG → NE | SM → SW | SR → NW |
| 17 | BG → NE | BG → SE | BG → SW |
| 18 | BR → NW | BR → SW | BR → P |
| 19 | BG → P | BG → SE | BG → NE |
| 20 | BM → NE | BG → P | BM → NW |
| 21 | SR → NE; SR → P | SM → SE; SM → P | SG → NW; SG → SW |
| 22 | BG → P | SR → SE; SG → SW | BM → SW |
| 23 | BG → NE | BR → P | SG → NW; SR → SW |
| 24 | BM → P | BG → P | BR → P |
| 25 | BG → SE | BR → SW | BM → P |
| 26 | BG → NE | BM → SW | BG → P |
| 27 | BG → SE | BM → P | BG → NW |
| 28 | SG → NE; SR → SE | SM → SE; SG → SW | SM → NW; SR → P |
| 29 | SG → NE; SR → P | BM → SE | SM → P; SR → SW |
| 30 | BG → P | SM → SE; SG → SW | BR → SW |
| 31 | SG → NE; SR → SE | SM → SE; SG → SW | SM → NW; SR → SW |

## Implementation tests justified by these observations

1. **Spawn fingerprint and destination matrix:** all 120 fingerprints occur exactly once per complete replay. Validate 85 fixed pillar assignments against physical X/Y + size + style; retain the 35 aggros as a distinct target mode. Specifically test wave 9 west's coincident big/small spawn anchors, which defeat a coordinate-only classifier.
2. **Per-wave destination:** spawning several west waves must send some NE, some NW, and some SW. Changing player positions must not change a normal pillar-directed spawn's initial assignment while its intended pillar exists.
3. **One tile per tick:** unobstructed small and big lane approaches advance by at most one X/Y each tick, including diagonals. Do not make big Nylocas slower merely because of size.
4. **Size-aware pillar contact:** big anchors `(3300,4251)` at NE and `(3298,4243)` at SE stop and can attack; equivalent small anchors are not yet cardinally adjacent. Contact and movement occupancy must be separate rules.
5. **Full small pillar:** replay fixture A with the four occupied NE anchors. Require local movement while blocked and entry to `(3299,4254)` when that vacancy becomes usable. A different pillar is not an automatic overflow destination.
6. **Big/small visual overlap:** reproduce big fixture B with distinct anchors and overlapping 2×2 extents. A full-rectangle inter-NPC collision rule should fail this case.
7. **Lifecycle vacancy release:** fixtures B and C admit a replacement on the same visible tick that the old occupant is still recorded with positive HP, one tick before its despawn event. Test collision release separately from renderer removal and HP snapshots; exact sub-tick release semantics remain an inference.
8. **Congestion direction selection:** keep fixtures A/B/C as coordinate-by-coordinate targets for the eventual movement engine. Until the movement rule is independently established, do not claim exact parity merely because blocked actors remain near their intended pillar. An implementation may need actor processing order and current collision flags to match every tick.
9. **Post-pillar-collapse route changes:** add dedicated fixtures with direct pillar-state evidence before specifying retarget rules. Preserve the outlier counts so fixed initial targeting does not incorrectly become an assertion that an NPC can never later change destination.
