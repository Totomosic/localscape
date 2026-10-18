# Current pillar movement engine audit

Audited 2026-10-18 against the checked-out Kotlin source. Scope: ordinary pillar-directed Nylocas, including bigs and split children; player-directed fighting forms are included only where they affect collision. This file describes the implementation and deductions from it. Recording observations are identified separately. No simulation implementation changed.

## Findings that change the interpretation of recordings

1. **Big pillar attackers already move and reserve collision as size one.** Their full size two is used to generate pillar contact anchors, render them, and perform ordinary avatar/bounds checks. Changing all big movement to size one is therefore not a new fix. The relevant separation already exists in [`add`, `updateAppearance`, `movementAvatar`](../../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt#L150), especially lines 151, 335 and 473–474. [`StepFactory` lines 111–123](../../upstream/rsmod/api/route/src/main/kotlin/org/rsmod/api/route/StepFactory.kt#L111) uses that collision size for executed steps too.
2. **The engine has no persistent chosen pillar face or preferred destination.** Its `faces` groups represent vertical versus horizontal orientation, not west/east/north/south. It searches all free contact anchors again every AI tick. This is a direct mechanism for wrapping toward another side instead of waiting near the original side. [`approachPillar` lines 374–427](../../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt#L374), [`State` lines 501–517](../../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt#L501).
3. **Explicit pacing only chooses cardinal steps, but diagonal movement is possible elsewhere.** A recording diagonal disproves that particular `pace` branch as its explanation, not the whole engine: BFS routes and ordinary step validation also move diagonally. [`pace` lines 434–458](../../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt#L434), [`StepFactory` lines 55–108](../../upstream/rsmod/api/route/src/main/kotlin/org/rsmod/api/route/StepFactory.kt#L55).
4. **`Route.success` does not mean the requested endpoint was reached.** `findRoute` defaults `moveNear=true` and returns `success=true, alternative=!pathFound` even for a closest-approach route. Its KDoc incorrectly says alternatives have `success=false`. Since the pillar code only checks `success`, the first nominally free but unreachable candidate may suppress every subsequent candidate and `pace`, including when the returned waypoint list is empty. [`RouteFinding` lines 68–93, 204–244](../../upstream/rsmod/engine/routefinder/src/main/kotlin/org/rsmod/routefinder/RouteFinding.kt#L204), [`approachPillar` lines 406–412](../../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt#L406).
5. **NPC reservations have no owner count.** Collision adds OR bits and removes clear bits. Two actors on exactly the same reservation tile cannot be distinguished. Removing one can clear collision for the other. This is distinct from normal big footprint overlap at different anchors, which is already supported. **The new recording analysis supports persistent collision holes as an authenticity hypothesis, so do not automatically replace these bits with reference counts.** [`CollisionFlagMap` lines 40–57](../../upstream/rsmod/engine/routefinder/src/main/kotlin/org/rsmod/routefinder/collision/CollisionFlagMap.kt#L40), [`PathingEntity` lines 413–428](../../upstream/rsmod/engine/game/src/main/kotlin/org/rsmod/game/entity/PathingEntity.kt#L413), [admission observations](summary.json).

## Current algorithm, including execution order

An AI timer of one repeats each processing cycle; `tick` is skipped on the birth cycle, during expiry, or at zero HP. Before selecting movement, it calls `noneMode()`, which clears existing movement. The actor therefore does not commit to the remainder of yesterday's BFS route; it calculates movement again from its current anchor. Before room entry it walks straight toward a scripted entry coordinate with `BlockedNormal`, then switches to `Normal`. Ordinary pillar attackers retain their incoming forms and collision size one. [`tick` lines 280–305](../../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt#L280), [`Npc.noneMode` lines 440–444](../../upstream/rsmod/engine/game/src/main/kotlin/org/rsmod/game/entity/Npc.kt#L440), [`AiTimerProcessor` lines 17–25](../../upstream/rsmod/api/game-process/src/main/kotlin/org/rsmod/api/game/process/npc/AiTimerProcessor.kt#L17).

```text
on each actor's AI turn:
    clear previous movement
    if not at entry yet:
        walk one step toward scripted entry; stop decision
    set entered, normal terrain restriction, incoming appearance
    face assigned 2x2 pillar

    if anchor is in this size's valid contact-anchor list:
        stop immediately, regardless of other occupants

    split contact anchors into {vertical sides, horizontal sides}
    order groups by minimum Chebyshev distance from current anchor
    within each group prefer its outer-end alignment
    remove terrain-blocked and NPC-occupied destination anchors

    temporarily clear own reservation
    for candidate in that order:
        route = full BFS to candidate, treating NPC anchors as obstacles
        if route.success:           # also true for a moveNear alternative
            clear waitingReturn
            enqueue route; restore own reservation; stop decision

    if not already pacing and more than one tile from closest contact anchor:
        route = BFS to touch pillar, using size-one movement avatar
        if route nonempty:
            enqueue route; restore own reservation; stop decision
    restore own reservation

    if remembered return tile is valid:
        walk toward it; stop decision
    clear return tile
    choose among W, E, S, N, preferring greatest distance from contact set
    remember current tile as return tile; walk toward chosen tile

later in SAME actor's turn:
    clear own collision bit
    validate and execute one walking step toward first queued waypoint
    set own collision bit at resulting anchor
continue to next actor
```

The contact and pace logic comes directly from [`NylocasBehaviour` lines 357–459](../../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt#L357). Execution uses [`NpcMovementProcessor` lines 65–98](../../upstream/rsmod/api/game-process/src/main/kotlin/org/rsmod/api/game/process/npc/NpcMovementProcessor.kt#L65).

All NPC logic and movement are processed **actor by actor in ascending NPC slot order**, not as an all-decisions-then-all-moves phase. `NpcMainProcess` runs AI, queues, modes and interactions for each actor; interaction processing executes movement before returning to the next actor. Thus later actors can use reservations released by earlier actors that tick, but earlier actors still see later actors' previous anchors. [`NpcMainProcess` lines 30–45](../../upstream/rsmod/api/game-process/src/main/kotlin/org/rsmod/api/game/process/npc/NpcMainProcess.kt#L30), [`NpcInteractionProcessor` lines 62–106](../../upstream/rsmod/api/game-process/src/main/kotlin/org/rsmod/api/game/process/npc/NpcInteractionProcessor.kt#L62), [`EntityList` lines 54–72](../../upstream/rsmod/engine/game/src/main/kotlin/org/rsmod/game/entity/EntityList.kt#L54). Slot allocation advances from the last used slot and wraps; creation order is therefore a useful local approximation, not an unconditional global ordering. [`EntityList` lines 11–24](../../upstream/rsmod/engine/game/src/main/kotlin/org/rsmod/game/entity/EntityList.kt#L11).

`StepFactory` first tries the direct diagonal toward a waypoint, then horizontal, then vertical. BFS itself uses a special `diagonalFirst` tie break for `NpcAvatar` sources, retaining equally short predecessors and choosing cardinal predecessors during backward reconstruction. That explains ordinary diagonal-then-straight approaches; it does not supply a local congestion retry rule. [`StepFactory` lines 64–108](../../upstream/rsmod/api/route/src/main/kotlin/org/rsmod/api/route/StepFactory.kt#L64), [`RouteFactory` lines 27–65](../../upstream/rsmod/api/route/src/main/kotlin/org/rsmod/api/route/RouteFactory.kt#L27), [`RouteFinding` lines 1685–1704](../../upstream/rsmod/engine/routefinder/src/main/kotlin/org/rsmod/routefinder/RouteFinding.kt#L1685).

## Contact geometry and the wave 25/27 lead

For pillar southwest coordinate `(px,pz)` and actor visual size `s`, candidate generation is:

```text
for offset = 1-s ... 1:
    west:  (px-s, pz+offset)
    east:  (px+2, pz+offset)
    south: (px+offset, pz-s)
    north: (px+offset, pz+2)
retain anchor if x in 3289..3302, z in 4242..4254
retain anchor if its terrain-blocker mask is clear
```

It checks terrain at the southwest anchor only. A settled actor never runs `canOccupy` again, and can remain after another actor is born on its anchor. The pillar support reserves 2×2 even though its native visual NPC is size three. [`pillarTiles` lines 357–371](../../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt#L357), [`approachPillar` line 375](../../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt#L375), [`NylocasPillars` lines 41–49](../../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasPillars.kt#L41).

For a big targeting SE `(3300,4243)`, the anchor candidates before terrain filtering are:

| Actual face | Anchors after coordinate clipping |
| --- | --- |
| North | `(3299,4245)`, `(3300,4245)`, `(3301,4245)` |
| West | `(3298,4242)`, `(3298,4243)`, `(3298,4244)` |
| East | `(3302,4242)`, `(3302,4243)`, `(3302,4244)` |
| South | None: `z=4241` is outside the candidate range |

Both wave 25 and 27 east bigs have this target and approach entry `(3301,4248)`. The horizontal group is nearer, so an uncongested approach prefers north anchor `(3300,4245)`, then `(3299,4245)`, then `(3301,4245)`. North-side arrival itself is therefore **already predicted**; it does not establish a special wave rule. The last anchor extends a size-two visual footprint to `x=3302`, so poking beyond the usual arena edge is also already possible without widening the movement footprint. [`NylocasWaves` lines 316–335](../../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasWaves.kt#L316), [`entry coordinates` lines 43–51](../../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasWaves.kt#L43).

The supplied duo recording is consistent with that mechanism for wave 25: `36500` follows `(3301,4248)` at 236 → `(3300,4247)` at 237 → `(3300,4246)` at 238 → `(3299,4245)` at 239, then stays. At tick 236 north anchors `(3300,4245)` and `(3301,4245)` have other actors, while `(3299,4245)` is available in the recorded snapshots. This supports retaining the basic empty-room approach and size separation. Source: [supplied recording](https://blert.io/raids/tob/653f90c7-176b-4fd4-a0eb-1cbd818481ce/nylocas), archived [events](recordings/653f90c7-176b-4fd4-a0eb-1cbd818481ce.events.json.gz), NPC `36500`, ticks 236–239.

Wave 27 `37040` takes the same first two room steps at 260–262, then moves `(3299,4246)` at 263 → `(3300,4246)` at 264 → `(3299,4245)` at 265. On tick 262, wave 25's big is removed and births children `37239` at `(3299,4245)` and `37240` at `(3300,4246)`: the latter shares the approaching big's anchor. The contact child moves away by tick 263. Matching this sequence requires decisions against changing occupancy and actor order, not a fixed wave-27 destination exception. Source: same archived events, NPCs `36500`, `37040`, `37239`, `37240`, ticks 260–265. Snapshots alone do not reveal the exact sub-tick order.

## What the old congestion fixtures do and do not prove

The source data are [movement-fixtures.json](../research/nylocas/movement-fixtures.json), extracted from first-party replay events. Predictions below are source-level deductions, **not an executed comparison with actual cache collision and full actor state**.

| Fixture | Recorded behavior | Current source prediction / limitation |
| --- | --- | --- |
| A: solo `eb3793f3`, small `33313`, NE, 53–59 | Holds near west side, alternates `(3298,4254)` and `(3298,4253)`, then retreats diagonally to `(3297,4253)` and returns diagonally. | Cardinal `pace` alone cannot make the two diagonal steps. With only the west endpoints occupied, the global candidate search is free to seek south-side endpoints. At 53–55, `(3300,4252)` remains represented by a zero-HP actor; exact vacancy depends on death stage, not HP alone. Proving a particular wrong next step requires complete collision and whether a candidate route is exact or alternative. |
| B: duo `1073635f`, big `64113`, SE, 238–256 | All north anchors occupied; oscillates west/east at `z=4246`, sometimes diagonally through `(3299,4247)`, despite other possible pillar approaches. | If a west/east contact anchor is reachable, the global search attempts a route to it rather than restricting waiting to north. If that first candidate is unreachable, an alternative route can be accepted and suppress both further candidates and explicit `pace`. The diagonal phase cannot come from the explicit `pace` branch. |
| C: solo `a37210fe`, big `34729`, SW, 339–349 | Repeats `(3293,4244)` → `(3293,4243)` → `(3294,4243)` → `(3293,4244)` before taking an east-side vacancy. | A fixed remembered out-and-back tile cannot independently produce a persistent triangle. But `waitingReturn` can be reset by routing, so the whole engine must be instrumented before claiming triangles are mathematically impossible. Global routing to other free contact anchors remains the competing behavior to examine. |

The strongest whole-engine mismatch is not “a diagonal exists.” It is: **given a reachable vacant contact on another face, this code will attempt to route there even while the recording keeps retrying the crowded original approach.** To make that mismatch unconditional in a replay fixture, capture the actual map flags, full surrounding occupancy, actor processing order and selected candidate. The report's recording analysis can then distinguish free-and-reachable contact from apparent free visual space.

## Occupancy ownership and lifetime boundaries

Plain collision bits create this failure sequence:

```text
actor A and newborn B share anchor q
collision[q] contains BLOCK_NPCS once
A temporarily removes its reservation to route or execute a step
collision[q] no longer contains BLOCK_NPCS, although B still occupies q
A moves to r and sets collision[r]
B stays settled / delayed, so it does not restore q by moving
later C may now enter q
```

`approachPillar` says “overlapping neighbours keep theirs,” but the underlying map only preserves neighbours on **different** anchors. The same issue applies to removal, death, teleport, hide/reveal and collision resizing whenever reservations overlap. Registration and deletion call the same add/remove methods. [`approachPillar` lines 403–425](../../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt#L403), [`NpcMovementProcessor` lines 72–95](../../upstream/rsmod/api/game-process/src/main/kotlin/org/rsmod/api/game/process/npc/NpcMovementProcessor.kt#L72), [`NpcRegistry` lines 24–46](../../upstream/rsmod/api/registry/src/main/kotlin/org/rsmod/api/registry/npc/NpcRegistry.kt#L24).

This behavior can make stacks larger after legitimate births/overlap, but **does not authorize allowing every actor to enter every occupied anchor**. The current movement mask intentionally rejects an occupied destination anchor. Bigs can already overlap other actors through their other three visual tiles without sharing anchors. Existing tests explicitly cover the distinction. [`NylocasOccupancyTest` lines 146–194, 219–258](../../upstream/rsmod/content/areas/dungeons/nylocas/src/integration/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasOccupancyTest.kt#L146).

The new corpus analysis finds 157 filtered occupied-anchor arrivals: the newcomer moved, the incumbent stayed continuously on the tile for at least two ticks before and after, both are ordinary forms with positive observed HP and away from natural-expiry ages. In all 157, a candidate flag-clearing event occurred during the incumbent's continuous hold, including its first arrival tick: 91 have exact-anchor departure/removal only, 26 have removal of a big whose other visual footprint tiles cover the anchor only, and 40 have both. These are **correlations with candidate release events**, not direct observations of server collision flags or proof of the exact cleanup instruction. See [`occupied_anchor_admissions` in summary.json](summary.json) and the extraction method in [analyze.py](analyze.py).

One especially useful witness is duo `1073635f`: small split `63282` holds `(3299,4243)` from tick 172 with positive HP; ordinary big `62545` at `(3298,4243)` disappears at 183; incoming `63412` enters the small's anchor at 193 and both remain through 198. The candidate removal covers the incumbent through the big's eastern visual tile, with **no exact-anchor departure during the incumbent's hold before entry**. A reference-counted occupancy model would keep the live incumbent blocking throughout; the current size-one removal model cannot clear its bit through a big on the neighbouring anchor. [Replay](https://blert.io/raids/tob/1073635f-5326-4bac-a147-8fdf6299f761/nylocas), [extracted witness](summary.json).

The fundamental candidate is therefore **different footprints for movement/addition and removal**, combined with persistent bitfield clearing, rather than uniformly size-two movement or blanket same-tile permission:

```text
ordinary big movement check: test southwest anchor (plus crossed edges)
ordinary big reservation addition: candidate size-one anchor behavior
ordinary big deletion/cleanup: candidate full native 2x2 bit clear
stationary actors: do not periodically repair cleared bits
later arrival: accept if destination collision bit is clear
```

The current code uses `collisionSize` for **all three** purposes: BFS/step size, collision addition, collision removal. Hence merely setting `collisionSize=2` is not a viable expression of this candidate. It would reintroduce full-footprint movement blocking. Evaluate a separate cleanup footprint or deletion policy while preserving ordinary size-one movement, and verify exactly when cleanup runs relative to splits and later actors. The same-anchor departure cases show why a departure on the incumbent's first tick must be included; otherwise persistent holes can be mistaken for unconditional stacking.

An existing integration test explicitly requires removing/killing a big to preserve small reservations in its other three visual tiles. That is the exact invariant the covering-big-removal witnesses challenge. Treat its passing result as confirmation of the current implementation, not independent evidence that this behavior is authentic; any later change should replace or qualify this expectation with the recording-derived witness. [`NylocasOccupancyTest` lines 262–295](../../upstream/rsmod/content/areas/dungeons/nylocas/src/integration/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasOccupancyTest.kt#L262).

Natural expiry starts at small age 51 / big age 52 and removes at ages 52 / 55. The expiry-start code stops movement and sets delay but does not release collision; registration is removed only at expiry removal. Player-kill death similarly retains collision through the death sequence until deletion. Thus the current source has no explicit independent “nonblocking dying body” transition. [`processLifetimes` lines 164–200](../../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt#L164), [`NylocasSize` lines 16–19](../../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasWaves.kt#L16), [`NpcDeath` lines 109–149](../../upstream/rsmod/api/death/src/main/kotlin/org/rsmod/api/death/NpcDeath.kt#L109). Recording replacement before a despawn event motivates investigating that transition, but HP/despawn snapshots do not alone identify its exact server phase.

## Fundamental corrections to evaluate together

These are proposals for the later implementation, not claims that the complete real algorithm has been recovered.

| Change | Why it addresses a mechanism | Discriminating observation |
| --- | --- | --- |
| Preserve the full visual/contact size versus size-one movement distinction. | Current broad footprint overlap already agrees with relevant fixtures. | Big moves while another actor occupies its north/east visual tile, but does not freely enter that actor's anchor. |
| Replace searches over every free contact anchor with a stable desired approach/contact and a local retry step. Make changing approach a consequence of geometry/retry rules, not availability of any distant free endpoint. | Reduces wrapping and makes repeated attempts against the same crowded side possible. | Actor remains near original side while another side's contact is verified reachable; later side switches occur under specific geometry changes rather than an arbitrary wait count. |
| Derive retry direction from target displacement, local blocked step checks and deterministic direction ordering, rather than choosing the cardinal tile furthest from the contact set. | Can produce both straight and diagonal reversals and three-step cycles from changing local occupancy. | Same actor alternates straight and diagonal oscillation as a neighbour approaches; known blockers predict the transition without a per-wave rule. |
| Distinguish exact endpoint routes from `moveNear` routes explicitly (`!alternative`, or disable `moveNear` when exact contact is required). | Prevents an unreachable first choice silently defeating all later choices or pacing. | Log shows a nominally free endpoint has `alternative=true` and no progress while another endpoint is reachable. |
| Preserve plain-bit clearing as a leading authenticity candidate; separate movement, reservation-addition and removal footprints so full-big cleanup can be evaluated independently. Use owner/count tracking as diagnostic ground truth, not automatically as the simulated collision rule. | New admissions correlate with exact-anchor departure and covering-big removal. Uniform size-one removal misses the latter; reference counting would prevent both from creating persistent holes. | Duo `1073635f`: big `62545` removed at 183, small `63282` keeps holding its east footprint tile, incoming `63412` stacks at 193. Compare that with wave-27 `37040`/child `37240` overlap on birth tick 262. |
| Separate collision release from visible body removal only where recording evidence supports it. | Can admit the replacement seen just before a dying occupant disappears without weakening all live collision. | Repeated same-anchor handoff at the same lifetime phase, versus durable unrelated live same-anchor pairs. |

Suggested shape:

```text
process one NPC at a time in known order:
    update lifecycle and its collision bits using operation-specific footprints
    if already making valid pillar contact: stay
    desired = choose geometric approach from position, visual size, pillar geometry
    attempted = local_step_toward(desired, movement_size=1,
                                  movement_collision_bits)
    if blocked:
        attempted = deterministic_local_retry(desired, local collision)
    execute at most one validated step; update movement reservation bits
```

The exact `choose geometric approach` and `deterministic_local_retry` remain the research targets. A proposal should reproduce A/B/C, uncongested early arrivals and wave-25/27 trajectories with the same rules. Preserve sequential actor processing while testing that model; changing movement policy and tick scheduling simultaneously would make cause difficult to identify.

## Minimum useful instrumentation for a later comparison

For each focal actor and tick, capture visual size, movement/reservation/removal sizes, lifecycle state, slot order, current anchor, contact membership, ordered candidates with rejection reasons, selected route's `success`/`alternative`/endpoint/first waypoint, pace branch/return tile, and destination collision bits plus actual occupants before/after movement. Log every collision clear/set with its operation and footprint. Static terrain masks must be available independently of actor occupancy. This is sufficient to turn “the endpoint looks free” into a falsifiable current-engine prediction and to separate routing policy, persistent bit clearing and death timing failures.
