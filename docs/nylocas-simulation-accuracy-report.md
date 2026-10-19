# Nylocas simulation accuracy: final research report

2026-10-16. Scope: the normal-mode waves room, especially solo/duo movement, fixed destinations, small/big differences, congestion, and death/cap interactions. The study analyzed **13 completed recordings: six solos and seven duos**, including the supplied Drummie/B asher raid. All report zero lost room ticks. Findings are based on recorded NPC events, Blert's complete mechanics-guide source, and recorder implementation; this is not access to Jagex's server scripts.

**The southwest-anchor observation is strongly supported for inter-NPC occupancy.** Two full-HP bigs in the supplied recording park for 14 consecutive ticks at `(3292,4243)` and `(3292,4244)`, overlapping two tiles of their 2×2 footprints. Small nylocas also move into a living big's other footprint tiles. However, bigs retain size-dependent pillar contact, and spawn/lane/death states can allow identical anchors. Implementing size one for every spatial operation would introduce other inaccuracies.

This task produced research, a preserved evidence corpus, and offline analysis tools. It did not change the simulator or implement the tests proposed below.

Implementation follow-up: ranged/magic aggro reach has since been reduced from the provisional four tiles to **two tiles from the nearest occupied NPC tile**, based on [recorded approach/hold traces](nylocas-movement-evidence.md#aggro-ranged-and-magic-reach). References to the old range-four behavior below describe the audit snapshot; target choice and exact NPC attack-launch timing remain open questions.

## Evidence package

| Document / artifact | Purpose |
| --- | --- |
| [Movement and fixed-target evidence](nylocas-movement-evidence.md) | Human-readable wave destinations, contact geometry, lane movement, and crowded-pillar traces for smalls and bigs. |
| [Southwest-anchor investigation](nylocas-occupancy-evidence.md) | Explicit NPC/tick witnesses, rival occupancy hypotheses, corpus counts, and identical-anchor exceptions. |
| [Lifecycle evidence](nylocas-lifecycle-evidence.md) | Death/removal timing, split placement, four-stage style observations, caps/stalls, and cleanup/boss timing. |
| [Mechanics source audit](nylocas-mechanics-source-audit.md) | Recovered guide appendices, complete published wave data, source discrepancies, current simulator audit, and unknown parameters. |
| [Corpus manifest](research/nylocas/manifest.json) | All 13 replay/API URLs, party scale, source revisions, and uncompressed SHA-256 checksums. |
| [120 initial spawn destinations](research/nylocas/lane-spawn-targets.csv) | Physical lane anchor, size, initial style, guide-backed initial aggro flag, inferred destination, and supporting NPC/tick. |
| [85 pillar-target observations](research/nylocas/target-observations.csv) | Filtered fixed-target evidence, including counts of recordings in which contact was actually observed. |
| [Style observations](research/nylocas/lifecycle-waves.csv) | Initial/age 5/age 7/age 26 styles and sample counts; unknown observations remain empty. |
| [Congestion fixtures](research/nylocas/movement-fixtures.json) | Three local tick traces with the focal NPC and surrounding blockers. |
| [Lifecycle rows](research/nylocas/lifecycle-summary.csv) | Per-NPC spawn, first observed HP zero, removal, movement, and inferred parent offsets. |
| [Corpus instructions](research/nylocas/README.md) | Schema, limitations, and commands to reproduce the results offline. |

## What the room does

### Fixed waves, targets, and approach

Each run contains **120 lane-spawned nylocas: 77 small and 43 big**. The 43 bigs produce 86 children, giving **206 regular nylocas per complete room**. Across 13 rooms, the corpus contains 2,678 such NPC lifetimes. Boss forms are excluded from these counts. The initial assignment table has **85 pillar attackers and 35 player aggros**. The destinations of all 85 pillar-spawn identities were recovered from at least one recording; some are often killed in the lane, so “seen in 13 runs” does not mean “reached its pillar in 13 runs.” [Target evidence](nylocas-movement-evidence.md), [lifecycle totals](research/nylocas/lifecycle-summary.json).

Assignments are fixed by **the particular wave spawn**, not by lane alone. Style and physical spawn anchor also matter when identifying a spawn. For example:

| Wave | Physical spawn | Observed initial destination |
| --- | --- | --- |
| 1 | West small ranged `(3281,4248)` | Player aggro |
| 1 | South small magic `(3296,4233)` | Northeast pillar |
| 1 | East small melee `(3310,4249)` | Northwest pillar |
| 2 | East small ranged `(3310,4248)` | Southeast pillar |
| 2 | West small magic `(3281,4249)` | Northeast pillar |
| 4 | South big magic `(3295,4233)` | Southwest pillar |

These rows have explicit supporting recordings, room IDs, and tick intervals in the [destination CSV](research/nylocas/lane-spawn-targets.csv). They already contradict the simulator's first-wave south→SW and east→NE development assignments.

Lane travel advances at most one tile on each axis per tick, with straight incoming travel followed by diagonal/cardinal approach through the arena. Most big east spawns use southwest anchor **`(3309,4248)`**; wave 30 is the recorded exception at **`(3310,4248)`** (confirmed during implementation from the frozen fingerprint CSV). The supplied room's wave-9 west small melee and big ranged share **`(3281,4248)`** at spawn and along the incoming lane. A six-slot representation must allow size-dependent anchors and this shared-anchor pair. Preserve physical coordinates rather than deriving them blindly from guide column headings. [Movement evidence](nylocas-movement-evidence.md), [source provenance](nylocas-mechanics-source-audit.md).

### Big occupancy and pillar contact are different rules

The recorded overlap witnesses reject full-footprint exclusion between nylocas. Across the corpus there are **6,936 positive-observed-HP pair-ticks with overlapping footprints but distinct anchors**, including **5,024** where both positions also remain unchanged on the preceding and following tick. These are repeated observations, not independent experiments. The decisive example is the supplied duo's big `42465` and big `42806` at ticks 125–138. [Detailed proof and reproduction](nylocas-occupancy-evidence.md).

Pillar contact still uses the larger big footprint. For the northeast pillar, a big can stop at **`(3300,4251)`** while a small stops on the next row, e.g. **`(3300,4252)`**. A big's anchor need not itself touch the pillar; its northern row does. This is the main reason to distinguish visual/combat size, contact geometry, and inter-NPC occupancy. [Contact coordinates and formulas](nylocas-movement-evidence.md).

### Full pillars create local waiting motion

A full pillar does not make a nylo select an arbitrary different pillar. Recorded approach paths develop repeated movement near the assigned pillar and change when a slot becomes available. The shape depends on size, approach, and nearby NPCs; a universal “alternate between two tiles” rule is too narrow.

Two particularly useful fixtures are:

- **Small, northeast:** solo naldon, NPC `33313`, ticks 53–60. With all four small contact anchors occupied, it repeatedly moves between `(3298,4254)` and `(3298,4253)`, with an extra retreat to `(3297,4253)` at 58. At tick 60 the blocker at `(3299,4254)` leaves and the waiting small takes that tile. [Replay](https://blert.io/raids/tob/eb3793f3-ce4a-4fcf-8ef9-e2098d39914a/nylocas).
- **Big, southeast:** duo go bestie/Michael41000, NPC `64113`, ticks 238–257. It first alternates `(3299,4246)` and `(3300,4246)`, then includes `(3299,4247)` before ultimately settling at `(3300,4245)`. The trace includes the neighboring big and small blockers, demonstrating motion through another big's non-anchor footprint. [Replay](https://blert.io/raids/tob/1073635f-5326-4bac-a147-8fdf6299f761/nylocas).

These are output traces to match, not a recovered complete routing algorithm. Exact retry order, diagonal choices, and processing-order tie breaks remain partly inferred. The [movement document](nylocas-movement-evidence.md) and [fixture JSON](research/nylocas/movement-fixtures.json) preserve the evidence needed to test a proposed rule.

### Initial aggro, later aggro, and style are independent state

The IDs previously described locally as “incoming” and “fighting” distinguish ordinary and aggro forms: normal small/big IDs 8342–8347, aggro forms8348–8353. Initial aggros also start with ordinary IDs while walking in. Pillar attackers can remain in ordinary IDs after entering and parking at their pillar. Transforming every nylo to an aggro form on arena entry is inaccurate. [Native ID definitions and recorder sources](nylocas-mechanics-source-audit.md).

The initial fixed aggro flag must also be distinguished from later conversion. In some solo recordings, several initial pillar attackers assigned to the **same pillar** change to aggro forms on the same tick at different ages. This is consistent with the guide's pillar-loss rule; the replays do not record pillar HP/destruction directly, so the precise triggering event cannot be independently certified. New splits can also later show aggro IDs. “Splits are not initial aggros” should not become “splits can never attack players under any circumstances.” [Movement observations](nylocas-movement-evidence.md), [guide behavior](https://blert.io/guides/tob/nylocas/mechanics).

Style changes occur at recorded ages **5, 7, and 26**, where age is `tick − spawnTick`. There are 689, 676, and 341 observed changes respectively, including 331 age 26 changes while observed HP remains positive. Surviving observations of each spawn's style at these ages are deterministic across this corpus. Age 26 is not covered by a simple initial→middle→final lane-flicker implementation. Three flickering spawn identities have no positive-HP survivor observation at 26 in this corpus. Their age-26 styles were subsequently user-confirmed as magic (wave 18 west big), melee (wave 19 east big), and ranged (wave 20 south big); the server table and integration fixture now include these values without changing the raw observation counts. [Style evidence and caveats](nylocas-lifecycle-evidence.md), [120-row observations](research/nylocas/lifecycle-waves.csv).

### Death is a sequence, not a boolean

Keep lethal impact, first observed zero HP, movement stop, animation, collision release, NPC removal, split birth, and cap removal separate. The guide's recovered appendix specifies player-killed small removal at impact+2; stationary big at impact+6; moving big at impact+7. The raw recordings show first-observed-zero→removal deltas1/2 for smalls and6/7 for bigs. These do not conflict automatically: Blert's HP is estimated from client observations, and the first recorded zero can lag impact. [Source death matrix](nylocas-mechanics-source-audit.md), [empirical distributions](nylocas-lifecycle-evidence.md).

For all NPCs without an observed zero before removal, small removal is at **spawn+52** (778 cases) and big removal at **spawn+55** (166 cases). These match natural lifetime ordinals53 and56 when the spawn tick is ordinal1. Natural expiry need not set the recorded HP to zero. Treat these as strong natural-expiry evidence, not a server reason code. Some killed NPCs make one additional movement step after the first observed zero. [Lifecycle evidence](nylocas-lifecycle-evidence.md).

The 559 bigs yield **1,118 split births**. Blert infers a parent for 1,110 and leaves 8 unknown. Every linked child appears on the parent's removal tick; that equality is also part of Blert's association heuristic, so it is corroboration rather than an independent server linkage. Most linked pairs appear at parent offsets `(0,0)` and `(1,1)`, with four other observed offsets. Do not constrain births to the parent's 2×2 footprint: `(-1,0)` and `(2,1)` also occur. The recordings do not establish a uniform distribution, exact selection algorithm, or equal style probabilities. [Split observations](nylocas-lifecycle-evidence.md).

### Room clock, cap, and cleanup

Wave checks use a four-tick phase established before barrier entry. Eleven recordings begin wave 1 at room tick 4; one solo begins at 6 and another at 7. Subsequent waves remain on that run's phase. A fixed “barrier pass +4” implementation is incomplete. The natural-delay schedule predicts wave 31 at first-wave tick+232 in the absence of cap stalls. [Clock sources](nylocas-mechanics-source-audit.md), [measured phase and intervals](nylocas-lifecycle-evidence.md).

The normal cap is12 before the increase and24 afterward. It is a **wave-admission threshold**, not a hard maximum population. In **202 of403** successful wave events the post-spawn count exceeds the cap; one room reaches34. Bigs, smalls, and splits each count one, including dying NPCs until their recorded removal. The analysis reconstructs all 403 spawn counts and367 stall counts from spawn/removal intervals. **92 stalls** have fewer positive-observed-HP NPCs than the cap, exposing why counting only living combat targets fails. Recorder stall markers are themselves inferred, so this is strong consistency evidence rather than raw server instrumentation. [Cap evidence](nylocas-lifecycle-evidence.md).

In all 13 runs, cleanup equals the last regular NPC removal, and the recorded boss-spawn marker satisfies:

```text
bossTick = cleanupTick + 16 + ((wavePhase - cleanupTick) mod 4)
```

The final term is the nonnegative remainder0–3. Thus the marker occurs16–19 ticks after cleanup, aligned to the room's wave phase. Keep this marker distinct from boss animation, drop/combat forms, and first attack until each is tested. [Lifecycle timing](nylocas-lifecycle-evidence.md).

## Source errors and omissions to avoid importing

1. **Guide wave 30:** the south-west small actually spawns magic; the south-east small spawns melee. The guide JSON lists both as melee. All 13 recordings agree on the corrected initial IDs.
2. **Guide wave 7:** the south array contains three items including a trailing null, despite a two-item schema. Importing it requires a documented normalization.
3. **Compass headings:** guide array order is not a safe physical-coordinate contract. Use actual observed anchors, especially for bigs.
4. **Wave 9 recorder signature:** it appears to contain only two NPCs, but the actual wave has three. Two west NPCs share one anchor and overwrite the same signature slot. This is a lossy recorder encoding, not proof of a missing spawn.
5. **Style lifecycle:** the guide's two lane flickers omit observed changes at age 26.

The [source audit](nylocas-mechanics-source-audit.md) preserves the published table as published and explains these discrepancies. The empirical CSVs supersede it where observations resolve a conflict.

## What to implement and test

The audit baseline is RSMod commit `967b00da5b688090fddfa442b3bb6d68b4c4ec3f`. It has three configured development waves, full-size footprint occupancy, a local pacing heuristic, no real pillar damage/collapse, and no complete nylo lifecycle or cap scheduler. Some existing movement tests assert footprint non-overlap and therefore encode behavior contradicted by recordings. Revise those expectations; making them pass unchanged would preserve the wrong model. [Current-code audit and links](nylocas-mechanics-source-audit.md).

P0 means needed before calling the waves simulation accurate. P1 covers additional mechanics and unresolved edges. “Source contract” means a published rule awaiting an independent discriminating recording; it should not be reported as empirically proven.

| Priority | Test contract | Concrete acceptance criterion / evidence |
| --- | --- | --- |
| P0 | All 31 spawn batches | Match all 120 fingerprints: wave, same-tick batching, physical anchor, size, initial style, initial aggro and fixed target. Include source wave 7/9/30 regressions. |
| P0 | First-wave phase | Cover barrier entry at all four instance phases. Match recorded first-wave delays4/6/7; cover the remaining phase as a source contract. Duo shares one instance phase. |
| P0 | Natural delays and retries | Each measured inter-wave gap equals its natural delay plus a nonnegative multiple of4; retry stalls without advancing wave index. |
| P0 | Initial target fidelity | Reproduce the full85-row pillar map and35 initial aggros. In particular, first-wave south→NE and east→NW. Do not replace per-spawn targets with nearest pillar or one target per lane. |
| P0 | Lane travel and anchors | Match unblocked straight travel, east big x3309, and wave 9 west overlapping lane anchors. Check incoming→arena timing before using generic pathfinding. |
| P0 | Big/big NPC occupancy | Match supplied-duo NPCs42465/42806 at ticks 122–138 with full HP; allow overlapping footprints and preserve both as size2. |
| P0 | Small/big NPC occupancy | Match NPC42643's path through the big's east and north tiles at 108/109. |
| P0 | Size-dependent pillar reach | Big NE contact at y4251 and small at y4252; test corresponding sides and all four pillars, using observed coordinate sets. |
| P0 | Small congestion | Match naldon33313 positions and blockers at 49–61, including immediate movement into the vacancy at 60. |
| P0 | Big congestion | Match go bestie/Michael41000 NPC64113 at 236–259, including the changing waiting path and final contact anchor. |
| P0 | Collision release timing | Reproduce the natural-death handoffs documented in the occupancy evidence while retaining the dying NPC in cap membership until removal. Server sub-phase ordering remains an explicit hypothesis. |
| P0 | Native ordinary/aggro forms | Pillar attackers retain ordinary form in the arena; fixed aggros transform at observed approach stage. Keep style changes separate from aggro-form changes. |
| P0 | Style stages | Match observed styles at 0/5/7/26 with stable NPC identity, HP and target assignment. Unobserved late styles must remain marked unknown rather than filled by a guess. |
| P0 | Player-kill lifecycle | Cover moving/stationary small and big with known impact ticks, stop/animation/removal sequence, and last movement. Do not use XP-drop tick as impact. |
| P0 | Natural expiry | Match observed removal ages52 small/55 big, independent child ages, natural-vs-player death effects, and positive-HP expiry. |
| P0 | Big splitting | Two children per big including natural expiry; same removal/birth tick, observed offset envelope, initial pillar behavior, and explicit handling of occupied birth tiles. Randomness needs seeded contracts without claiming an inferred distribution. |
| P0 | Cap lifecycle and overshoot | At threshold reject the entire due wave; below threshold allow the entire wave even if post-spawn count exceeds cap. Count dying NPCs and all smalls/bigs/splits equally. |
| P0 | Measured cap fixtures | Match a stall with HP-zero bodies, a big→two-child count transition, and reconstructed counts at all 770 wave/stall events. Include admission/removal on the same tick. |
| P0 | Cleanup/boss scheduling | No cleanup before all regular NPCs/splits are removed; match all 13 cleanup and boss marker times using the phase formula above. |
| P1 | Wave 19→20 admission boundary | Find or construct a discriminating fixture at counts12–23. The recorder's after-spawn cap 24 alone cannot prove which cap the server checks to admit20. |
| P1 | Pillar HP, attacks and collapse | Establish authoritative HP, damage, attack interval and collapse consequences, then replay same-target aggro conversion cohorts. Current replay data alone cannot supply those combat parameters. |
| P1 | Aggro targeting and range | Use duo position/target evidence to distinguish nearest-player, random-player and retained-target rules; test melee/ranged/magic reach, LOS and departure. Current nearest/retain/range4 behavior is unverified. |
| P1 | Damage matching and nulling | Source contract: matching styles damage normally; wrong-style null state belongs to that player and NPC. Check flicker-boundary attack/impact timing and another player's continued ability to damage. |
| P1 | Explosion geometry and damage | Measure radius/metric, origin for bigs, affected-player timing, damage range and prayer interaction. Do not infer these from the visible effect or import Verzik-crab parameters. |
| P1 | Remaining spatial rules | Independently establish terrain footprint, player collision, diagonal blocking and tie ordering. The NPC anchor finding does not establish these. |
| P1 | Boss and room completion | Validate drop/combat NPC transitions,10-tick style periods, attack timing/targeting, matching damage, wrong-style healing/reflection, despawn and final completion signal. Guide rules are starting contracts, not a completed boss audit. |

Start with recording-backed fixtures that fail under the current full-footprint collision and incorrect first-wave destinations. Add integration tests against actual cache collision and engine movement/death processing, because a standalone movement model can pass while the live server's generic collision flags still reject its steps. Keep fixture RNG, player actions and blocker state explicit; do not expect an uncontrolled combat rerun to reproduce an unrelated raid's damage rolls.

## Remaining uncertainty and confidence

The strongest findings are exact spawn coordinates/composition, repeated fixed destinations, footprint-overlap witnesses, measured lifecycle ages, cycle alignment, and recorded counts. Pillar destinations are geometric inferences, not transmitted target IDs. Native aggro labels support initial classifications but later conversions require lifecycle context.

The largest missing facts are precise congested-routing tie breaks, collision release sub-phase, terrain/player blocking, aggro target selection/range, pillar combat parameters, explosion mechanics, independent recording corroboration of the three user-confirmed late styles, and the wave 20 admission boundary. The corpus is a recent completion sample with repeated players; it does not establish all possible room states, wipes, hard mode, or scaling above two players. These limits are reasons to keep the proposed P1 measurements explicit, rather than silently promote defaults into facts.

## Verification performed

All 26 compressed recording files are checksum verified by the offline scripts. The scripts reconstruct occupancy summaries,120 spawn fingerprints,85 fixed destinations, three congestion fixtures,2,678 lifecycle rows, style observations, and403 wave/367 stall population counts. Reproduction commands and generated-file checks are listed in the [corpus README](research/nylocas/README.md). This validates the research artifacts; it does not certify the simulator, whose implementation tests remain proposed work.
