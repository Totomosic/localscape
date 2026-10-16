# Nylocas mechanics: source audit and simulator gaps

Research date: 2026-10-16. This note separates published mechanics, recorder implementation, and the simulator's actual behavior. It supports the recording-based study; source code for a recorder is evidence of what that recorder assumes and observes, not Jagex server code.

The [movement evidence](nylocas-movement-evidence.md), [occupancy evidence](nylocas-occupancy-evidence.md), and [lifecycle evidence](nylocas-lifecycle-evidence.md) resolve several source uncertainties below. The source table is deliberately preserved as published; use the recording-derived fixtures for implementation.

## Sources and reproducibility

| Ref | Primary material | Revision / scope |
| --- | --- | --- |
| G | [Blert mechanics guide](https://blert.io/guides/tob/nylocas/mechanics) and [its complete source, including appendices](https://github.com/blert-io/blert/blob/e5f44caceebd3a0979c9a89742f964f65841b670/web/app/guides/tob/nylocas/mechanics/page.tsx) | Blert website commit `e5f44caceebd3a0979c9a89742f964f65841b670`; a published mechanics account. |
| W | [Wave JSON](https://github.com/blert-io/blert/blob/e5f44caceebd3a0979c9a89742f964f65841b670/common/data/nylocas-waves.json), [wave schema](https://github.com/blert-io/blert/blob/e5f44caceebd3a0979c9a89742f964f65841b670/common/data/nylocas-waves.ts) | Data consumed directly by the guide. |
| R | [Blert room recorder](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/NylocasDataTracker.java), [Nylo model](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/Nylo.java), [spawn positions](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/SpawnType.java), [wave recognition](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/NyloWave.java) | Recorder commit `9d11b1f7bb8f2677d39a56927cd8a255d8995e1c`. Newer recorder code may not match older recordings. |
| P | [Blert NPC definitions](https://github.com/blert-io/protos/blob/3c5bc5cfc315ff4266128601829e66615f3ef83d/npc_definitions.json#L245-L377) | Commit `3c5bc5cfc315ff4266128601829e66615f3ef83d`; native ID, size, and aggro-form metadata. |
| Q | [ToB QoL Nylocas handler](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/rooms/nylocas/NylocasHandler.java), [instance clock](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/InstanceService.java) | Commit `3715d6ae24547fa87dc7ed82665e37b632407ba7`; independent plugin implementation. |
| M | [Mc's original observer notes](https://oldschool.runescape.wiki/w/User%3AMc/Mechanics/ToB#Nylocas) | Player-authored observations; useful where detailed, but not an official specification. |
| S | Local Kotlin simulator under `upstream/rsmod` | Reviewed commit `967b00da5b688090fddfa442b3bb6d68b4c4ec3f`. No simulator edits were made for this audit. |

The web text extractor displays broken “Appendix ?” references and omits their bodies. The complete guide source contains the wave table, natural stalls, and death timing. Treat missing extracted appendices as an extraction limitation, not an absence of mechanics.

## Published mechanics, with timing boundaries made explicit

G describes a four-tick clock established before room start, 31 deterministic waves, fixed aggro/pillar assignments, a 12-to-24 normal cap at wave 20, and 52-tick expiry. Counts include dying NPCs until despawn. Its death appendix distinguishes HP zero from animation start and removal. Bigs produce two random-style smalls on removal; splits are not aggro. G also describes flickers from wave 16, style-matched damage, and per-player nulling after wrong-style attacks. [Guide source, waves/behavior](https://github.com/blert-io/blert/blob/e5f44caceebd3a0979c9a89742f964f65841b670/web/app/guides/tob/nylocas/mechanics/page.tsx#L144-L184), [cap/cycle](https://github.com/blert-io/blert/blob/e5f44caceebd3a0979c9a89742f964f65841b670/web/app/guides/tob/nylocas/mechanics/page.tsx#L193-L288).

| Case | Event sequence in G's appendix |
| --- | --- |
| Small, player kill, stationary or walking | HP zero at t; stop/turn at t+1; NPC removed at t+2. |
| Big, player kill, stationary | Animation t+1; removal and splits t+6. |
| Big, player kill, walking | Stop t+1; animation t+2; removal and splits t+7. |
| Small, natural expiry | Animation at lifetime tick 52; removal at 53. |
| Big, natural expiry | Animation at lifetime tick 53; removal and splits at 56. |

The small's later visible death effect is scenery, after NPC removal. Animation lengths are 1/5/3 ticks for small/big-killed/big-expired. [Death appendix source](https://github.com/blert-io/blert/blob/e5f44caceebd3a0979c9a89742f964f65841b670/web/app/guides/tob/nylocas/mechanics/page.tsx#L413-L504).

G places boss arrival at the fifth cycle after cleanup; boss starts melee, changes style every ten ticks, and room completion follows boss removal. [Boss source](https://github.com/blert-io/blert/blob/e5f44caceebd3a0979c9a89742f964f65841b670/web/app/guides/tob/nylocas/mechanics/page.tsx#L382-L412).

### Clock and cap details corroborated by recorder code

R independently defines four-tick retries and the complete natural-stall array. Its regular room count is the size of the tracked-NPC map, which is reduced by `onNpcDespawn`, not HP updates. Small, big, and split have equal count; a hard-mode prince adds three. The cap reported by R is 12/15 before wave 20 for normal/hard and 24 afterwards. This is strong corroboration for the count's lifecycle boundary. It does not establish server ordering when a despawn and spawn check share one tick. [Recorder count/cap](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/NylocasDataTracker.java#L54-L98), [despawn handling](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/NylocasDataTracker.java#L192-L230).

A spawn event's `roomNyloCount` is emitted **after** newly spawned lane NPCs have entered the map. It is not the count checked immediately before that wave. The recorder decides a due wave stalled only when no recognized wave arrived and its current count is at least its cap. Thus recorded stall events are derived, not a direct server “cap check” signal. Hard-mode waves 10/20/30 override the next interval to 16 ticks. [Recorder tick loop](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/NylocasDataTracker.java#L104-L135), [wave dispatch](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/NylocasDataTracker.java#L297-L313).

Q sets its instance cycle while observing the preceding-room entrance line, then advances modulo four. Its nylo tracker initializes a 52-tick lifetime and recognizes separate big death animations with five- versus three-tick split timers. These checks corroborate the lifetime and distinct killed/expired big animations, but are predictive UI code. [Q clock](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/InstanceService.java#L96-L124), [Q lifetime](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/rooms/nylocas/NylocasHandler.java#L339-L346), [Q animations](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/rooms/nylocas/NylocasHandler.java#L552-L576).

### Timing questions that need explicit recording fixtures

- **Off-by-one convention:** “Lifetime tick 52” is an ordinal label until related to recorded spawn tick s. Store and test actual `despawnTick - spawnTick`; do not assume that ordinal 53 means s+53.
- **Cap overshoot:** A check of current count below cap can admit a complete wave taking the room over cap. Test whole-wave admission below cap versus rejection at cap; do not implement `count + incoming <= cap` without contrary recording evidence.
- **Wave 20 boundary:** R's `waveCap()` uses its already-recognized current wave. That does not prove whether the server admits wave 20 using the old or new cap. A crowded 19→20 recording is needed.
- **Boss timing:** M distinguishes fifth-cycle initiation from the dropping NPC arriving two ticks later. G does not make that distinction. R also distinguishes dropping and fighting NPCs and reuses their room identity. Keep cleanup, boss process start, drop spawn, and combat spawn as separate events until a replay establishes all offsets. [Original observer notes](https://oldschool.runescape.wiki/w/User%3AMc/Mechanics/ToB#Nylocas), [recorder two-stage boss](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/NylocasDataTracker.java#L166-L199).

## Complete source wave data and its limitations

The following table is a compact transcription of W's facts, not a corrected official wave table. M=melee, A=magic, R=ranged; consecutive letters are the successive styles; B=big; *=aggro. A single token spans a lane. Two tokens preserve W's array order, **not verified compass labels**. A dash is no spawn. “Delay” belongs to this wave and means ticks until the **next** wave, not since the previous wave. The last column preserves R's separate recognition signature: i/t/h=small melee/range/mage, capitals=big; six slots are east-south, east-north, south-west, south-east, west-south, west-north. [W schema](https://github.com/blert-io/blert/blob/e5f44caceebd3a0979c9a89742f964f65841b670/common/data/nylocas-waves.ts), [R signatures and indexing](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/NyloWave.java).

W includes 120 lane spawns: 43 bigs, 77 smalls, and 35 aggros. These are dataset totals, pending the discrepancies below. Summed natural delays for waves 1–30 are 232 ticks. With wave 1 at room tick 4, that predicts wave 31 at tick 236 (2:21.6) without cap stalls. Both totals are arithmetic over W; neither certifies Jagex behavior.

| Wave | Delay | East | South | West | Recorder signature |
| --- | --- | --- | --- | --- | --- |
| 1 | 4 | M | A | R* | `_i_ht_` |
| 2 | 4 | R | M* | A | `t_i__h` |
| 3 | 4 | A* | R | M | `_h_ti_` |
| 4 | 4 | M | AB | R | `i_H__t` |
| 5 | 16 | A | M | RB | `_h_iT_` |
| 6 | 4 | MB | R | A | `I_t__h` |
| 7 | 12 | M | A / RB* / – | – | `_iTh__` |
| 8 | 4 | R | M | AB* | `t_i_H_` |
| 9 | 12 | A | – | RB* / M | `_h__T_` |
| 10 | 8 | R* / RB | R / R | R / R* | `Tttttt` |
| 11 | 8 | A* / A | A / A | AB* | `hhhhH_` |
| 12 | 8 | M* / M | MB | M / M* | `iiI_ii` |
| 13 | 8 | MB* | R / M | R / A* | `I_itht` |
| 14 | 8 | RB* | A / R | A / M* | `T_thih` |
| 15 | 8 | A* / R | AB | M / R* | `thH_ti` |
| 16 | 4 | AMR | MAR | RAR | `h_i__t` |
| 17 | 12 | AMAB | AMAB | AMAB | `H_H_H_` |
| 18 | 8 | RARB | RARB | RARB* | `T_T_T_` |
| 19 | 12 | AMAB* | AMAB | AMAB | `H_H_H_` |
| 20 | 16 | MRMB | ARMB* | MRMB | `I_H_I_` |
| 21 | 8 | RMR / RMR* | MAM / MRM* | ARA / AMR | `ttiihh` |
| 22 | 12 | ARMB* | RAM / ARM | MRMB | `H_htI_` |
| 23 | 8 | ARMB | RAMB* | ARM / RAR | `H_T_th` |
| 24 | 8 | MB* | AB* | RAMB* | `I_H_T_` |
| 25 | 8 | AMAB | RB | MB* | `H_T_I_` |
| 26 | 4 | AB | MAMB | AB* | `H_I_H_` |
| 27 | 8 | AMAB | MARB* | AB | `H_I_H_` |
| 28 | 4 | AMA / RAM | MRM / AMR | MAM / RMA* | `thhiti` |
| 29 | 4 | ARM / RMA* | MB | MRA* / RMR | `thI_ti` |
| 30 | 4 | AB* | MRM / MRM | RMRB | `H_hiT_` |
| 31 | 0 | ARA / RMR | MAR / AMR | MRA / RAR | `thhiti` |


### Source conflicts discovered during the audit

1. **W's wave 7 south tuple has three entries** (magic small, ranged big aggro, null), although its TypeScript type permits only two. The guide happens to render the first two. Preserve this discrepancy in imported research data; normalize only with a documented rule.
2. **Wave 9 is a lossy signature, not a missing NPC.** W lists three NPCs, including west melee small. The recordings confirm that small and the west ranged big spawn on the same anchor `(3281,4248)`. R's `encodeSpawn` writes each NPC into `key[index]`, so one replaces the other. Its two-character signature can identify a three-NPC wave because the big occupies the same encoded slot as the small. Recognition depends on iteration order for this collision; the signature is not a complete multiset. [Encoding loop](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/NyloWave.java#L146-L165), [observed lane coincidence](nylocas-occupancy-evidence.md#why-identical-anchors-do-not-disprove-the-useful-hypothesis).
3. **Wave 30:** W lists two south melee smalls. R lists south magic small plus melee small. All 13 recordings support R: south-west starts magic; south-east starts melee. Preserve W's row above as a source defect; use the corrected recording-derived row. [Lifecycle evidence](nylocas-lifecycle-evidence.md).
4. **Array orientation:** G's two-column headings are east N/S, south E/W, west N/S. R indexes its signatures east S/N, south W/E, west S/N. Examples such as wave 10 put its big in opposite east columns. Do not map W indices to world tiles by its headings alone.
5. **No pillar IDs exist in W.** Its schema only stores style rotation, big, and aggro. Fixed pillar selection is stated in G but its entire per-spawn mapping must come from recordings. Join by wave + exact spawn tile + size; color alone is insufficient because of flickers.
6. **Lifetime prose and death table use different event labels.** “Explodes at 52” is not “removed at 52”; the big's natural removal explicitly happens later. M also contains an inconsistent 55th/56th lifetime label. The empirical lifecycle report relates natural despawn to recorded spawn: smalls at `s+52`, bigs at `s+55`, consistent with inclusive lifetime ticks 53/56. [Measured lifecycle](nylocas-lifecycle-evidence.md).
7. **Style schedules are incomplete.** W describes three successive styles. Hundreds of observed changes also occur at age 26, besides the lane changes at ages 5/7; a three-entry lane rotation alone cannot reproduce a complete lifetime. The empirical lifecycle table captures the additional stage. [Style-change evidence](nylocas-lifecycle-evidence.md).

These disagreements make blind generation of the simulator from W unsafe. Retain W as a hypothesis source and use raw recording events to create the verified wave fixture.

## What the recorder can and cannot prove about movement

R classifies spawn origins from exact world tiles:

| Lane | Recorder-recognized anchors |
| --- | --- |
| East | Small anchors (3310,4248)/(3310,4249); special big anchor (3309,4248). |
| West | (3281,4248)/(3281,4249). |
| South | (3295,4233)/(3296,4233). |

These are actual recorder constants, not a guarantee every historical client build recognized all anchors. [SpawnType source](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/SpawnType.java#L34-L62).

R describes observed split origins relative to the parent's **southwest tile**: (-1,0), (0,0), (1,0), (0,1), (1,1), (2,1). Parent removal and child spawn must have equal recorder ticks for its matching heuristic. Multiple possible parents leave the parent ID unset; no valid match can leave type UNKNOWN. Therefore a missing parent ID is not proof that a small was lane-spawned, and proximity to one big is not sufficient when several bigs die together. These six offsets are the recorder's permissive observation envelope, not a probability distribution or proof every offset is equally likely. [Nylo parent matcher](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/Nylo.java#L137-L161), [ambiguity handling](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/NylocasDataTracker.java#L326-L350).

**The source comment alone establishes an anchor convention; the recordings establish footprint overlap.** The [occupancy study](nylocas-occupancy-evidence.md) supplies 6,936 distinct-anchor overlapping pair-ticks, including 5,024 stable observations with positive HP. Its stationary big/big and moving small/big witnesses decisively reject full 2×2 NPC reservations. Equal-anchor episodes also exist, so neither universal anchor exclusivity nor an exact collision algorithm follows. Terrain checks, NPC occupancy, player occupancy, pillar reach, and clickbox size are separate questions. A correct simulator needs separate geometry rules for these operations.

### Position and health provenance

Blert's general tracker obtains `actor.getWorldLocation()` and maps it through the instance template. There is no big-NPC size correction in these helpers. RuneLite documents that actor coordinate as server-side, potentially ahead of the visible interpolation; apparent visual movement is not the same timestamp as a recorded tile change. Combined with the nylo model's explicit southwest description, this establishes that the recording coordinates should be treated as southwest anchors. [Recorder coordinate helper](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/core/DataTracker.java#L352-L357), [instance normalization](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/util/Location.java#L31-L36), [RuneLite server-coordinate contract](https://github.com/runelite/runelite/blob/e98399f931c60bfa8e42f924da850f508ec8e00d/runelite-api/src/main/java/net/runelite/api/Actor.java#L103-L111).

Recorded nylo HP is a client estimate initialized from raid scale, then drained by observed hitsplats or increased by heals. The room tracker can correct HP from a health-bar ratio when scale changes; it does not make every nylo update a direct server HP measurement. Updates are emitted after the tracker tick. Consequently the first packed zero is not automatically an exact independent lethal-impact timestamp across merged observers. Quantify its observed offsets, and keep guide-derived impact timing separate. [Hitsplat accumulation](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/core/DataTracker.java#L854-L872), [update emission](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/core/DataTracker.java#L128-L152), [scale correction](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/RoomDataTracker.java#L153-L168).

### Native IDs distinguish aggro forms, not merely lane versus arena

P explicitly labels normal IDs 8342–8347 as ordinary small/big forms and 8348–8353 as their **aggro** forms. Both families retain size one or two according to the small/big distinction. The recordings corroborate this: pillar-bound bigs can remain IDs8345/8347 after parking at a pillar, while aggros change to the corresponding 8351–8353 family. Therefore the simulator's cache names “incoming” and “fighting” should not be interpreted as an unconditional lane-exit transformation for every nylo. [Permanent ID definitions](https://github.com/blert-io/protos/blob/3c5bc5cfc315ff4266128601829e66615f3ef83d/npc_definitions.json#L245-L377), [recorded ID/state transitions](nylocas-lifecycle-evidence.md).

## Combat parameters that remain unresolved

The reviewed primary evidence establishes that pillar-bound nylos attack their fixed pillar, a collapse damages players and causes surviving attackers to switch to players, and natural expiry has an area damage effect. It does **not** supply an exact numeric pillar HP/damage model, attack cadence, collapse damage formula, or explosion radius/distance metric. The selected replays do not include pillar-HP trajectories. No values for those fields should be promoted into verified fixtures solely from this study. [Blert behavior account](https://github.com/blert-io/blert/blob/e5f44caceebd3a0979c9a89742f964f65841b670/web/app/guides/tob/nylocas/mechanics/page.tsx#L167-L184).

| Unresolved parameter | Evidence needed before claiming accuracy |
| --- | --- |
| Pillar starting HP and team scaling | Native pillar state/health-bar observations or controlled runs with known damage sequence. |
| Nylo-to-pillar attack interval and damage | Animation/HP observations aligned to arrival and freeze status, for both sizes. Stationary contact is not proof of an attack on that tick. |
| Pillar collapse damage and retarget timing | A recording with collapse, player hitsplats, and the same surviving nylo identities before/after collapse. |
| Natural explosion radius, damage, and tick | Player positions and health/hitsplats around small and big natural expiry; distinguish collision distance, anchor distance, and footprint distance. |
| Aggro player choice and retarget rules | Duo examples with unequal/equal distances, movement across ranges, target death, and LOS obstruction. Fixed aggro **spawn identity** does not establish a nearest-player target rule. |
| Ranged/magic aggro attack range and cadence | Position plus actual attack/projectile events, independently for size/style; present simulator range four is not verified by these sources. |

Mc's notes include explosion-radius details for **Verzik's** summoned crabs elsewhere on the same page. Those are a different encounter mechanic and must not be copied into the Nylocas room model. General combat stat infoboxes also do not independently specify the scripted pillar attack schedule.

## Current simulator audit

Links below reference the reviewed local files. These are implementation findings, not inferences about the live game.

| Mechanic | Current behavior | Consequence / required change |
| --- | --- | --- |
| Wave schedule | [NylocasWaves.kt](../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasWaves.kt) defines only three rows, each delay 4. The scheduler reads the **next row's** `ticksAfterPrevious`. | Add all verified rows; import W's delay with an index shift. First-wave delay requires separate phase logic. |
| Room clock | [NylocasEncounter.kt:50](../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasEncounter.kt#L50) schedules four ticks after barrier pass. No pre-room phase is stored. | Start clock on instance-area arrival; barrier entry should select the second applicable cycle, with phase-dependent first-wave latency. |
| Room cap | [Encounter processing](../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasEncounter.kt#L63) never counts NPCs or stalls. | Track spawned-through-despawn lifecycle separately from active behavior; add cap, whole-wave admission, and four-tick retries. |
| Death tracking | [NylocasBehaviour.kt:74](../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt#L74) prunes behavior at HP≤0. Generic [NpcDeath.kt](../upstream/rsmod/api/death/src/main/kotlin/org/rsmod/api/death/NpcDeath.kt) handles stop, arrival delay, cache death animation, removal, and normal drops. | A pruned behavior map is unsuitable for cap counts. Nylo-specific lifecycle, natural expiry, split scheduling, and death effects are absent. Audit generic animation length before reusing it. |
| Big/small occupancy | [pillarTiles/canOccupy](../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt#L128) checks every tile in `npc.size × npc.size`; routefinder also receives the full avatar. | This directly encodes full 2×2 occupancy for bigs and contradicts the [overlap witnesses](nylocas-occupancy-evidence.md). Changing only the final target check would leave routing wrong. |
| Full-pillar movement | [pace](../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt#L195) chooses a cardinal tile farthest from candidate pillar tiles; ties preserve west/east/south/north order; then alternates back. | This is a designed heuristic. Replace or calibrate against recorded coordinate traces, including mixed sizes, obstructions, and vacancy timing. |
| Pillar combat | [pillar branch](../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt#L98) faces an artificial size-3 avatar and returns after approach. | No pillar HP, attack timing, damage, collapse, or retarget behavior exists. |
| Aggro selection | [player branch](../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt#L104) selects nearest valid player, then retains it; ranged/mage use distance 4 and LOS. | Nearest-player and target retention are hypotheses requiring duo traces. Their correctness does not follow from “aggros are fixed.” |
| Style/state | [spawn model](../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasWaves.kt) has one immutable style; [behavior](../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt#L91) transforms every incoming NPC to fighting at lane entry. | The unconditional transformation is contradicted by ordinary-ID pillar occupants and P's aggro labels. No flicker schedule, age-26 stage, or per-player null state is present in this module. Style-matched incoming-damage handling needs an explicit implementation. |
| Lane placement | Six fixed slots share an anchor independent of size. | R expects a special east big x3309 anchor; local east slots x3310 cannot represent it. Verify all lane positions from events. |
| Later lifecycle | Encounter ends scheduling after last configured row, without cleanup/boss/completion. | Implement all phases with distinct timestamps and native NPC transitions. |

The existing [movement integration tests](../upstream/rsmod/content/areas/dungeons/nylocas/src/integration/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasMovementTest.kt) assert big footprints never overlap and demand exactly two-tile alternating pacing for a fifth small. They prove the implementation's chosen rules, not that those rules match OSRS. The default-wave movement test still calls `waves.single()` although the current default table has three rows; do not use that test as evidence of a verified three-wave baseline.

## Source-derived test contracts to implement

These are proposed tests; this research task did not add or run simulator tests.

1. **Four entry phases:** arrive at the pre-room boundary, wait 0/1/2/3 ticks before barrier pass, and assert the first spawn follows the measured room phase. Preserve one phase for a shared duo instance.
2. **Natural delays:** assert all 30 measured next-wave intervals, with separate room cap retries. Assert every successful wave remains on the same modulo-four phase.
3. **Cap lifecycle:** at cap, an HP-zero small must still block admission until its removal. Repeat with moving/stationary bigs and natural expiry. Include a big→two-smalls count transition and whole-wave overshoot.
4. **Death matrix:** fixtures must include spawn tick, last movement tick, lethal impact tick, death animation tick, removal tick, and split ticks. Player attack time and XP drop are not substitutes for impact time.
5. **Wave fidelity:** for all 31 waves assert exact spawn anchor, initial style/size, fixed aggro flag, all observed rotation stages, transform ticks, and fixed destination pillar. Freeze the empirical corrections for wave 7's malformed source tuple, wave 9's shared anchor, wave 30's south magic, and physical index orientation.
6. **Split lifecycle:** two children, parent removal synchronization, observed spawn offsets, independent age timers, no inherited aggro, and allowed styles. Separate confirmed deterministic rules from unknown selection/randomness.
7. **Occupancy separation:** big visual footprint overlap with distinct anchors; same-anchor episodes and their conditions; terrain obstruction of each footprint tile; mixed big/small pillar slots; player tiles; lane NPC passing; and directional approach. Each needs its own recording evidence.
8. **Crowded pillar traces:** replay exact positions each tick for free, full, and newly freed pillars; retain source NPC IDs to reproduce processing-order effects.
9. **Cleanup/boss:** last small removal, any simultaneous big splits, cleanup signal, fifth-cycle process start, drop NPC, combat NPC, first attack, style changes, boss removal, and room end.
10. **Source import integrity:** validate two-item tuples, monotonically increasing waves, rotations of valid styles, natural delays divisible by four, and exact equality with verified raw spawn fixtures. Tests should catch source-data defects rather than silently normalize them.

