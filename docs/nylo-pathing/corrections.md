# Proposed corrections, considered together

These proposals explain the mechanisms exposed by the [observations](observations.md). They are research conclusions and pseudocode, not implemented changes or a claim of exact server parity. The current implementation is audited at rsmod revision `bbb4b2a654fcf3813bf127c14682f571cdfb4dd0`; [engine-audit.md](engine-audit.md) links the exact code.

## 1. Separate collision cleanup from movement and reservation addition

**Evidence: strong for persistent occupied-anchor admission; a broader big cleanup clear is the leading mechanism, not directly observed server code.**

Keep ordinary big movement/terrain checks at its southwest anchor, and keep full 2×2 contact geometry. Those rules already explain distinct-anchor overlaps and the empty approaches. The missing distinction is between the footprint tested/set during movement and the footprint cleared during lifecycle cleanup. `collisionSize` currently controls all of them. Simply restoring full size-two collision would prevent recorded visual overlap and would not address the actual discrepancy. [Source operations](engine-audit.md#occupancy-ownership-and-lifetime-boundaries).

The 26 arrival/incumbent pairs with a covering-big removal and no exact-anchor departure are the clearest reason to evaluate wider cleanup. A reference-counted occupancy model would preserve a stationary incumbent indefinitely; it cannot produce those admissions without an additional explicit rule. The existing “big deletion preserves neighboring small reservations” test encodes the current behavior, and is challenged by these observations. [Witnesses and filters](observations.md#persistent-stacks-and-collision-history).

Candidate semantics to evaluate, scoped to ordinary Nylocas rather than applied to all NPCs:

```text
ordinary_nylo.movement_footprint = southwest 1x1
ordinary_nylo.reservation_add_footprint = southwest 1x1
ordinary_nylo.contact_footprint = native size (1x1 or 2x2)

on a movement departure:
    clear movement reservation at old anchor
    validate/execute one step using movement geometry
    set destination anchor reservation

on collision cleanup / removal:
    clear collision bits in native footprint       # leading hypothesis
    # Preserve recorder-visible body/cap membership until their own phase.
    # Exact cleanup phase relative to animation, removal and births is unresolved.

while stationary at contact:
    stay
    do not rebuild reservation from the list of visible actors

on another actor's attempted arrival:
    decide using the current collision bits, including any persistent clears
```

This intentionally distinguishes the collision map from a census of physical occupants. A stationary small can retain its visible position after its collision bit is cleared. Another nylo may enter; a subsequent clear can allow a rare third. That gives a common explanation for better grouping, fewer congested retries, usual doubles, and occasional triples. It does not require a pillar capacity, pair permission, random stack chance, or a per-wave exception.

Keep a diagnostic map of actual occupants when testing the proposal, but do not automatically use owner counts to repair authentic collision holes. Do not weaken terrain or pillar support blocking when clearing dynamic NPC bits. Exact bit masks, the lifecycle phase, whether all big deaths behave alike, and whether walking/turning also has broader clearing need independent discrimination; a 2×2 clear **at observed despawn** is a candidate that explains the delayed witness, not a fully recovered rule for early handoffs.

## 2. Use a geometric approach with local retries instead of global vacancy chasing

**Evidence: strong for local congestion paths, mixed directions and settled stability; incomplete for the exact retry selector and every pre-contact side change.**

Current `approachPillar` searches all unoccupied contact anchors with BFS each tick. Availability on another side can redirect a nylo around the pillar. The separate `pace` routine instead chooses the cardinal neighbor furthest from the contact set and remembers one return tile. This split can create artificial excursions and omits diagonal waiting steps in that branch. [Actual algorithm](engine-audit.md#current-algorithm-including-execution-order).

Replace those competing policies with one consistent target-driven step decision. An approach should be based on geometry and history, with a local retry when blocked. Retrying need not be an independent “pacing mode,” and a distant vacancy need not be a new global destination. Settled actors already stop; preserve that behavior. Correct collision history first, because otherwise the retry policy is being evaluated against the wrong blocked tiles.

One compact geometric goal worth comparing is the pillar's outer corner, aligned to the actor's native footprint. It reproduces the existing preference for outer endpoints without enumerating “good slots.” For a pillar at `(px,py)` and native size `s`:

```text
goal.x = px + (2-s if pillar is on room's east side else 0)
goal.y = py + (2-s if pillar is on room's north side else 0)
```

For smalls this is the pillar's outermost corner tile; for bigs it is the pillar's southwest anchor. The goal is **inside** the pillar: it is a direction reference, not a tile the nylo may occupy. Full-size cardinal contact stops the approach first. An alternative formulation keeps an initially selected inward face/contact alignment. Empty approaches do not distinguish the two; congestion and split approaches must choose between them. Do not lock a face forever, because the task's observations allow occasional wrapping and this sample does not establish a universal ban.

Proposed decision shape:

```text
on this actor's turn:
    apply lifecycle / collision operations in the chosen, explicit order
    if dead, expiring or delayed: return
    if native footprint makes valid cardinal pillar contact: stay; return

    goal = outer_aligned_pillar_goal(pillar, native_size)
    toward = one_step_sign_delta(anchor, goal)
    if toward is legal against terrain, crossed edges and current NPC bits:
        take toward
    else:
        take choose_local_retry(anchor, goal, local_collision, previous_step)
        # Retry selector is unresolved; include diagonal and cardinal neighbors.
        # Do not silently search every other free pillar contact here.
```

`choose_local_retry` must be constrained by the three full-context congestion fixtures, not filled in with an invented direction table. Compare these hypotheses using the **same** geometry/collision history: fixed ordering of the eight neighbors relative to the desired step; orientation-dependent ordering with previous-step state; and a stochastic choice only if deterministic state cannot explain repeated decisions. If they imply different first steps under identical observed local blockers, preserve those cases as discriminators. Snapshots do not reveal slot order or all movement state, so different outcomes alone do not prove randomness.

A successful selector must permit `A→B→A→C→A` with one straight and one diagonal excursion, the SW three-step triangle, and repeated straight or repeated diagonal reversals. A timer that blindly alternates the two kinds fails those requirements. A fixed maximum wait before wrapping is likewise unsupported. If a later implementation chooses either as an approximation, label it and measure its discrepancy rather than presenting it as recovered game behavior.

## 3. Give route results honest semantics and preserve operation order

**Evidence: direct source finding.** `Route.success` is true for `moveNear` alternatives; current pillar code treats it as exact endpoint success. This lets an unoccupied but unreachable first choice prevent later choices or local retries, even with an empty returned path. Distinguish “exact contact reached,” “progress toward goal,” and “no progress.” If exact endpoint searches remain, require a nonalternative result or disable `moveNear`; otherwise use a partial result only as explicit progress. [Route implementation and call site](engine-audit.md#findings-that-change-the-interpretation-of-recordings).

```text
route = find_route(request)
if route reaches requested valid contact:
    use exact route
else if partial progress is allowed and first step is useful:
    use progress step
else:
    make local retry decision
```

Keep actor processing sequential while evaluating movement changes. The engine currently processes AI and movement actor by actor in slot order; a later actor can see an earlier actor's collision changes. Blert `roomId` is not a proven server slot, and the metadata's insertion order is not a substitute. Reordering all moves into a simultaneous phase could remove the release/set interactions that the stacking witnesses suggest. Birth collision sets, movement unsets/sets, death cleanup and visual despawn need separate log entries. [Scheduling source and instrumentation](engine-audit.md#minimum-useful-instrumentation-for-a-later-comparison).

## Acceptance cases for one coherent correction

These are proposed regression cases for a later implementation. No server tests or mechanics were changed in this research task.

| Case | Required observed behavior | What a failure distinguishes |
| --- | --- | --- |
| Existing wave1 empty approaches, both sizes' established endpoint cases | Retain diagonal-then-straight movement, native-size contact and outer alignment. | Global geometry regression versus congestion-only improvement. |
| Supplied duo wave25 `36500`,236–254 | North approach and `(3299,4245)` hold with the other north anchors occupied. | A supposed new wave-specific fix that breaks an already explained case. |
| Supplied duo wave27 `37040`,260–270 | Preserve child birth overlap at262 and north-side arrival at265; surrounding child motion is part of the input. | Processing order and birth effects versus arbitrary pacing delays. |
| Small NE `33313`,53–60 | Local straight/diagonal retries, then `(3299,4254)` vacancy. | Wrong face search, retry direction or release phase. |
| Big SE `64113`,238–257 | Straight/diagonal local retries, permitted visual overlap, eventual north contact. | Full-footprint movement, global wrapping or cardinal-only retry. |
| Big SW `34729`,339–350 | Three-step loop before east-face entry. | Overfitting to paired out-and-back motion. |
| Big62545 removed183; small63282 holds; small63412 enters193 | Permit durable two-smalls-on-one-anchor without killing the incumbent or changing its HP. | Missing wider cleanup clear; uniform refcount or anchor mutex. |
| Small59710 removed305 as59665 arrives;59667 enters308 | Retain the first replacement visibly while a later one stacks. | Cleanup phase and persistent exact-anchor holes. |
| Three smalls53449/53579/53734 atSE by427 | Permit the observed triple through at least431. | Artificial maximum of two or blanket uniqueness. |
| All14 replay classifications and event windows | Same rules, no UUID/wave/NPC exceptions; 27/27 qualifying east big25/27 arrivals remain north. | A collection of local patches without a common engine explanation. |

Exact fixture events and neighboring actors are in [cases.json](cases.json); the 14-recording aggregate baselines are in [summary.json](summary.json). Use matched spawn/kill/lifecycle inputs when comparing aggregate movement rates. A no-attacks simulator run is not directly comparable to raids where players kill incoming actors.

## Remaining limits

The evidence now clearly separates three concerns: missing collision-history behavior can overfill the apparent pillar; global contact searches can encourage wrapping; the dedicated pacing branch cannot by itself explain observed retry shapes. The cleanup-footprint hypothesis is substantially stronger than it was in the earlier audit, but the replay does not uniquely identify its phase. The local retry selector also remains unresolved. Those are explicit boundaries of the suggested correction, not reasons to add per-scenario rules or claim exact parity prematurely.

The wave27 boundary-overhang lead remains unconfirmed by coordinates. There is no justification here for expanding the room or forcing that wave's anchor beyond its current contact candidates. Normal fighting bigs also have separate full-footprint behavior in the current code; this ordinary-nylo study does not authorize changing it.
