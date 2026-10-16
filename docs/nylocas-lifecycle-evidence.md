# Nylocas lifecycle evidence from solo and duo recordings

Research date: 2026-10-16. Scope: regular mode, six solo and seven duo completed recordings, including the user-supplied [Drummie/B asher recording](https://blert.io/raids/tob/c77441a7-3caf-4d9d-a482-8ffe8d7f293d/nylocas). The corpus contains 2,678 wave Nylocas: 1,560 lane births and 1,118 split births. All recordings report zero lost ticks. This is a selected corpus, with repeated players, rather than a random sample or an independent experiment.

The important simulation distinction is between becoming lethally damaged, stopping movement, playing an animation, leaving the NPC population, and spawning splits. These are separate moments. Counting only positive-HP NPCs or deleting a big immediately on a lethal hit produces observable errors in cap checks, split births, and cleanup.

## Reproduction and field interpretation

Run from the repository root:

```sh
python tools/research/nylocas_lifecycle.py
```

The script uses the checksum-verified compressed recordings listed in [the corpus manifest](research/nylocas/manifest.json). It produces:

- [lifecycle-summary.csv](research/nylocas/lifecycle-summary.csv): one row per wave NPC, including spawn/despawn, first recorded zero HP, later movement, style transitions, and inferred split offsets.
- [lifecycle-summary.json](research/nylocas/lifecycle-summary.json): aggregate counters and per-recording wave/cleanup/boss timings.
- [lifecycle-waves.csv](research/nylocas/lifecycle-waves.csv): 120 lane spawn slots, with observed styles at relative ages 0, 5, 7, and 26; each age includes sample counts and a separate positive-HP sample column.

Only regular wave NPC IDs 8342–8353 enter generic counts; the boss is excluded. Event types 7/8/9 mean spawn/update/despawn. Types 120/121/122/123 mean wave/stall/cleanup/boss-spawn markers. A metadata field called `deathTick` records NPC despawn here; it must not be mistaken for the lethal damage tick.

Relative age in this document is `recordedTick - spawnTick`, so the spawn snapshot has age 0. Inclusive lifetime tick 53 is therefore relative age 52. Replay ticks are room recording ticks, not the instance's absolute cycle counter. The cycle phase comes from the recording's wave ticks.

Decode `npc.hitpoints` as current = `packed >> 16`, maximum = `packed & 65535`. These values are the recorder's accumulated hitpoints estimate. The [recorder's hitsplat handler](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/core/DataTracker.java#L854) drains HP using observed hitsplat amounts, or boosts for healing. This is not an authoritative server death flag, and zero-HP observation timing need not identify every internal processing phase. Coordinates alone also cannot identify whether an NPC is in the server's walking state.

## Expiry has different observable removal times for smalls and bigs

Among NPCs never observed at zero HP before removal:

| Size | NPCs | Relative age at despawn | Other ages observed |
|---|---:|---:|---|
| Small, including splits | 778 | 52 | None |
| Big | 166 | 55 | None |

This sharp separation is strong evidence for a scheduled timeout with different removal animations. It does not mean that a big gets three extra ticks of normal attacking lifetime. The [guide's death-timing appendix](https://blert.io/guides/tob/nylocas/mechanics#death-timing) distinguishes expiry animation from despawn: inclusive lifetime 52/53 for a small's animation/removal, and 53/56 for a big. In zero-based snapshot ages these removals are 52 and 55.

Specific independently inspectable examples in the [user-supplied recording](https://blert.io/raids/tob/c77441a7-3caf-4d9d-a482-8ffe8d7f293d/nylocas):

| roomId | Kind | Spawn | Despawn | Terminal recorded HP | Observation |
|---:|---|---:|---:|---:|---|
| 41370 | Small lane spawn | 4 | 56 | 8/8 | Exactly 52 ticks after spawn |
| 42465 | Big lane spawn | 84 | 139 | 16/16 | Exactly 55 ticks after spawn |
| 43046 | Big lane spawn | 124 | 179 | 16/16 | Exactly 55 ticks, despite later style changes |
| 42282 | Small split, parent 41987 | 72 | 124 | 8/8 | Own 52-tick clock begins on split birth |
| 43605 | Small split, parent 43317 | 161 | 213 | 2/8 | Expiry still occurs after partial damage |

Classification caveat: absence of observed zero HP is an operational filter, not a server-supplied cause-of-death label. Its exact age concentration, across both sizes and both scales, supports the expiry interpretation. Some kills near expiry have zero HP and the same final age; they are intentionally excluded from this table.

Implementation tests should distinguish the expiry action from actual removal; verify the small and big removal snapshots independently; give each split a fresh age counter; and retain a big in the cap population through its expiry animation.

## Lethal damage does not immediately remove an NPC or always stop its next step

Observed delays from the **first recorded zero HP snapshot** to removal:

| Size | Delay | NPCs |
|---|---:|---:|
| Small | 1 tick | 237 |
| Small | 2 ticks | 1,104 |
| Big | 6 ticks | 108 |
| Big | 7 ticks | 285 |

These are replay observations, not four independent server timer settings. The guide's appendix describes killed small removal at lethal tick +2, and big removal at +6 when stationary or +7 when walking. Replay first-zero HP is not sufficient to choose those branches by itself; even a +7 big can have unchanged coordinates around its first zero snapshot. Preserve this uncertainty when comparing server-oriented code to recorder output.

Out of 1,341 smalls and 393 bigs observed at zero HP, 342 smalls and 74 bigs subsequently change their recorded anchor once before removal. No NPC in these groups changes anchor on more than one later tick. The remaining zero-HP snapshots are stationary. This directly contradicts an implementation that immediately stops all recorded movement when first seeing zero HP, although it does not expose the sub-tick cause of the final step.

Examples from the [user-supplied recording](https://blert.io/raids/tob/c77441a7-3caf-4d9d-a482-8ffe8d7f293d/nylocas):

| roomId | Size | Last positive HP | First zero HP | Next snapshot | Removal |
|---:|---|---|---|---|---|
| 41368 | Small | Tick 8: (3285,4248), HP 8 | Tick 9: (3286,4248) | Tick 10: (3287,4248), HP 0 | Tick 11 |
| 41551 | Big | Tick 24: (3285,4248), HP 16 | Tick 25: (3286,4248) | Tick 26: (3287,4248), HP 0 | Tick 32, with split births |

Another small, roomId 41426, first records zero HP at tick 16 and disappears at 17. Big 41821 first records zero HP at 45 and disappears at 51. These are useful compatibility fixtures for observed timing variation.

Tests should model zero HP, movement stop, death animation, removal, and cap release separately. Replay tests should compare observed snapshots; an assertion that every first-zero snapshot is exactly the lethal server tick would overstate the evidence.

## Every big produces two splits, at its removal tick

Across the complete corpus there are 559 big births and 1,118 split births: exactly two splits per big in aggregate. Blert associates 1,110 children with a parent; eight splits have no parent ID. Among parents, 553 have two associated children, four have one, and two have none. Unknown metadata is not evidence that a big produced fewer splits.

Every one of the 1,110 associated splits spawns on the **parent's recorded despawn tick**, including expired parents. This aligns split birth with removal rather than first zero HP. However, parent timing is not independently discovered by that metadata: the [recorder's parent matcher](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/Nylo.java#L139) requires simultaneous parent despawn and child birth. The aggregate ratio and unambiguous individual examples supply additional evidence. When several bigs die nearby on the same tick, parent identity is genuinely ambiguous in this data.

Offsets relative to the associated parent's despawn anchor:

| Offset (dx,dy) | Children |
|---|---:|
| (0,0) | 534 |
| (1,1) | 535 |
| (1,0) | 18 |
| (0,1) | 8 |
| (-1,0) | 3 |
| (2,1) | 12 |

For 553 parents with two associated children, the birth pairs are:

| Unordered pair of offsets | Parents |
|---|---:|
| {(0,0),(1,1)} | 527 |
| {(1,0),(2,1)} | 12 |
| {(1,0),(1,1)} | 6 |
| {(0,0),(0,1)} | 5 |
| {(-1,0),(0,1)} | 3 |

The common southwest/northeast pair is therefore a default observation, not a universal placement rule. In the [user-supplied recording](https://blert.io/raids/tob/c77441a7-3caf-4d9d-a482-8ffe8d7f293d/nylocas), big 41551 despawns at tick 32 at (3287,4248); children 41694 and 41695 appear that tick at (3288,4248) and (3289,4249), offsets (1,0) and (2,1). The latter lies outside the parent's recorded 2×2 footprint.

Important limitation: these six offset positions are also explicitly accepted by Blert's matching heuristic. The metadata-conditioned frequency table cannot prove other positions impossible. It does prove that an implementation restricting every split birth to exactly (0,0)/(1,1), or even strictly within the dead parent's four displayed tiles, would fail identified replay cases. Determining *why* a pair shifts needs a separate collision/order study; these observations alone do not establish a nearest-free-tile algorithm.

Both equal-style and mixed-style split pairs occur. Among the 553 complete associated pairs, there are 66 melee/melee, 57 ranged/ranged, 55 magic/magic, and 375 mixed pairs. This rules out forcing two different styles. It does not establish random-generator independence or exact probabilities.

Tests should cover two births on both killed and expired big removal, the resulting net +1 population change, equal-style children, fresh child lifetime, missing recorder parent IDs, and the observed shifted birth pairs. Treat the unexplained placement-selection rule as unresolved rather than hard-coding a random choice among six offsets.

## The room cap counts animation bodies and permits overshoot

Reconstructing the active population as `spawnTick <= tick < despawnTick` reproduces **all 403 wave-event counts and all 367 stall-event counts exactly**. This count includes splits and zero-HP bodies; it excludes the boss. No weighting by 2×2 size is needed. This is a direct empirical check of population accounting, not an inference from HP bars.

At 92 of the 367 stalls, the number of positive recorded HP NPCs is below the stated cap while the full spawn/despawn population is at or above it. Example: [duo 1073635f…](https://blert.io/raids/tob/1073635f-5326-4bac-a147-8fdf6299f761/nylocas), tick 108, wave 14: 12 present against cap 12, but only 11 have positive recorded HP; roomId 61591 is the zero-HP body. Counting alive-by-HP would incorrectly allow a wave there.

Of 403 wave-spawn markers, **202 report a population above the cap after the wave spawns**. Cap is a gate for a wave attempt, rather than a population clamp or an instruction to spawn only as many new NPCs as fit. In the [user-supplied recording](https://blert.io/raids/tob/c77441a7-3caf-4d9d-a482-8ffe8d7f293d/nylocas), wave 11 at tick 76 reports 15 against cap 12, and wave 31 at tick 268 reports 29 against cap 24. Splits can also increase population independently of lane-wave spawning.

Tests should count every retained wave NPC once regardless of size, retain zero-HP bodies until removal, allow whole-wave and split overshoot, and make spawn/despawn and the cap check's order explicit. Event counts are end-of-observation snapshots and cannot by themselves identify every ordering branch when birth and removal share a tick.

## Wave checks, first-wave phase, cleanup and boss timing

Every adjacent wave interval in all 13 recordings equals its natural interval plus four ticks per recorded cap stall, with no exceptions across 390 intervals. All 367 stall markers are at or above the cap and lie on that recording's four-tick wave phase.

The natural interval after each wave 1–30 is:

```text
wave  1–10: 4 4 4 4 16 4 12 4 12 8
wave 11–20: 8 8 8 8 8 4 12 8 12 16
wave 21–30: 8 12 8 8 8 4 8 4 4 4
```

A simulator should not assume every room recording starts at phase 0. Eleven recordings have first-wave tick 4; [solo 65c9f142…](https://blert.io/raids/tob/65c9f142-f240-4284-9384-d32fa313067e/nylocas) starts at 6, and [solo a37210fe…](https://blert.io/raids/tob/a37210fe-5737-480f-8fca-518e56205e9b/nylocas) at 7. Subsequent checks remain aligned to their own first-wave phase modulo 4.

In all 13 runs, cleanup equals the last wave NPC's despawn tick. Boss-spawn marker timing is exactly:

```text
bossSpawn = cleanup + 16 + ((wavePhase - cleanup) mod 4)
wavePhase = firstWaveTick mod 4
```

Here `mod` returns a result from 0 through 3. Observed cleanup-to-boss gaps are 16–19 ticks. The boss-spawn marker denotes the dropping boss's appearance; it should not automatically be treated as the first attackable boss tick.

| Recording | Scale | First wave | Wave 31 | Cleanup | Boss marker | Gap |
|---|---:|---:|---:|---:|---:|---:|
| [c77441a7…](https://blert.io/raids/tob/c77441a7-3caf-4d9d-a482-8ffe8d7f293d/nylocas) | 2 | 4 | 268 | 304 | 320 | 16 |
| [1073635f…](https://blert.io/raids/tob/1073635f-5326-4bac-a147-8fdf6299f761/nylocas) | 2 | 4 | 272 | 311 | 328 | 17 |
| [4ed0a471…](https://blert.io/raids/tob/4ed0a471-7c3a-46d7-8f9c-e807f0275be3/nylocas) | 2 | 4 | 300 | 342 | 360 | 18 |
| [bef6ad10…](https://blert.io/raids/tob/bef6ad10-3ef3-48b7-97a5-d019cf9ba303/nylocas) | 2 | 4 | 344 | 425 | 444 | 19 |
| [65c9f142…](https://blert.io/raids/tob/65c9f142-f240-4284-9384-d32fa313067e/nylocas) | 1 | 6 | 450 | 545 | 562 | 17 |
| [a37210fe…](https://blert.io/raids/tob/a37210fe-5737-480f-8fca-518e56205e9b/nylocas) | 1 | 7 | 427 | 526 | 543 | 17 |

Test all four cleanup residues relative to the chosen wave phase, keep cleanup distinct from wave 31, and avoid hard-coded tick-4 first spawn when reproducing actual room starts.

## Additional wave-data discrepancies discovered while checking lifecycle

### Flickers can change style again at age 26

The data contains 689 changes at age 5, 676 at age 7, and **341 at age 26**, of which 331 occur with positive recorded HP. No other style-change ages occur in this corpus. Changes between pillar and aggressive NPC IDs that preserve style are excluded from these counts.

A particularly clean case is the [user-supplied recording](https://blert.io/raids/tob/c77441a7-3caf-4d9d-a482-8ffe8d7f293d/nylocas), big 43046, wave 17:

| Tick | Age | NPC ID | Style | Anchor | HP |
|---:|---:|---:|---|---|---:|
| 124 | 0 | 8347 | Magic | (3281,4248) | 16 |
| 129 | 5 | 8345 | Melee | (3286,4248) | 16 |
| 131 | 7 | 8347 | Magic | (3288,4248) | 16 |
| 149 | 25 | 8347 | Magic | (3290,4245) | 16 |
| 150 | 26 | 8345 | Melee | (3290,4245) | 16 |
| 178 | 54 | 8345 | Melee | (3290,4245) | 16 |
| 179 | 55 | 8345 | Removed | (3290,4245) | 16 |

Sister bigs 43047 and 43048 independently show the same three changes at those ticks. There is no HP change or movement at the third transformation, so it cannot be dismissed as an aggro-ID swap or movement artifact. This is stronger evidence than the guide's prose suggesting an age-7 style is final.

Across positive-HP samples, each observed slot's style at ages 0/5/7/26 is consistent across recordings. The fourth style need not equal the first or second. For example, wave 26 south big's positive-HP sequence is melee → magic → melee → ranged. [lifecycle-waves.csv](research/nylocas/lifecycle-waves.csv) records these values without inventing unobserved fourth states. Seventeen slots lack positive-HP age-26 observations, including three flickering slots: wave 18 west big, wave 19 east big, and wave 20 south big. Samples from dying bodies sometimes retain an earlier style, which is why the table separates positive-HP and all-sample counts.

Tests should include the fourth change where directly observed; preserve uncertainty for unobserved slots; and distinguish style transformations from aggressive-form ID changes. Do not infer damage vulnerability during death animation from a visible style alone.

### Wave 9 west spawns share an anchor

All 13 recordings place the wave-9 west small melee (8342) and west big ranged (8346) at **the same spawn anchor (3281,4248)**. The east small mage (8344) starts at (3310,4249). In the user-supplied recording these are roomIds 41986, 41987, and 41988 at tick 56. Thus a generic north/south lane-slot renderer or wave table cannot be translated mechanically into distinct occupancy anchors for this wave. Birth stacking is itself observable and should have a fixture.

### Wave 30 south west-slot initial style differs from the guide table

All 13 recordings show:

| South slot | Spawn anchor | Initial ID/style | Age 5 | Age 7 | Age 26, positive-HP survivors |
|---|---|---|---|---|---|
| West | (3295,4233) | 8344 / magic | Ranged | Melee | Magic, 10 samples |
| East | (3296,4233) | 8342 / melee | Ranged | Melee | Magic, 10 samples |

The [guide's backing wave data](https://github.com/blert-io/blert/blob/e5f44caceebd3a0979c9a89742f964f65841b670/common/data/nylocas-waves.json) lists both south entries as initially melee. The recording contradicts the west entry. In the user-supplied run, roomIds 44946/44947 spawn at tick 256, switch at 261 and 263, and surviving 44947 switches again at 282. The west spawn's fourth state is established by other runs where it survives. Prefer the repeated direct observations for simulation fixtures, and record the source-table discrepancy explicitly.

### Additional corrections checked during wave implementation

The frozen fingerprint CSV places the wave-30 east big mage at **(3310,4248)**
in all 13 recordings. Other east big lane spawns use (3309,4248). The general
size-dependent east anchor rule therefore needs this explicit exception.

The wave-31 west south-anchor small ranged has positive-HP styles
**ranged → melee → ranged → magic** at ages 0/5/7/26. The guide reference lists
ranged → magic → ranged for its first three stages; the age-5 entry disagrees
with the recorded melee observations. The implementation and its fixture use
the positive-HP lifecycle matrix.

## What remains unproven

- The exact server phase behind 1-tick versus 2-tick first-zero-to-removal delays for smalls, and a complete stationary/walking classifier for big death delays. Replay HP and anchors do not expose the necessary internal state.
- The collision and update-order rule selecting shifted split birth tiles. Parent metadata is conditioned on a known six-position acceptance rule.
- Exact split-style probabilities or independence. Both duplicate and mixed pairs are established.
- Fourth styles for flickering spawn slots killed in every sampled recording before that state can be observed.
- Server ordering when a cap check, despawn, and split/wave birth share a tick. End-of-tick counts constrain the result but do not identify every intermediate decision.
- Changes outside regular solo/duo mode, including hard-mode princes. Those require a different corpus.

These are research gaps, not reasons to relax already confirmed invariants. The pinned corpus and the per-NPC CSV give concrete replay fixtures for the established behavior and preserve the ambiguous cases for later targeted experiments.
