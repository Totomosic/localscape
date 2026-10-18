# Nylocas Vasilias timing and combat evidence

Researched 2026-10-18 for regular-mode implementation. The direct replay sample is the existing checksum-pinned corpus: six solos and seven duos, including the user-supplied [Drummie/B asher run](https://blert.io/raids/tob/c77441a7-3caf-4d9d-a482-8ffe8d7f293d/nylocas). All report zero lost ticks. Repeated players and selected successful runs limit statistical generalization. Source URLs, retrieval dates, and content hashes are in the [manifest](research/nylocas/manifest.json).

Run `python tools/research/nylocas_boss.py` from the repository root to reproduce the [per-recording results](research/nylocas/boss-summary.json). The script checks every recording's hash before analyzing form updates and attack events. Ticks below are room-recording ticks, and offsets are differences between tick numbers, not inclusive duration labels.

## Drop, combat form, and initial timing

All 13 recordings have the same offsets between the boss's initial forms:

| Event | Tick relative to drop | Observations |
|---|---:|---:|
| Dropping NPC 8354 appears | 0 | 13 |
| Combat melee NPC 8355 appears | 2 | 13 |
| First style change, to magic 8356 or ranged 8357 | 11 | 13 |
| Subsequent style changes | 21, 31, 41, … | 288 complete subsequent intervals, all 10 ticks |

Therefore the first visible combat melee form lasts **9 ticks**, whereas subsequent complete forms last **10 ticks**. Starting a ten-tick timer when the combat form appears would switch one tick late. The general guide's ten-tick description does not capture this startup offset. No consecutive form transitions retain the same style in the observed sequence. These are direct calculations from the [pinned API event snapshots](research/nylocas/manifest.json); their boss starts are also inspectable in the [user run](https://blert.io/raids/tob/c77441a7-3caf-4d9d-a482-8ffe8d7f293d/nylocas), [duo 1073635f](https://blert.io/raids/tob/1073635f-5326-4bac-a147-8fdf6299f761/nylocas), and [solo 65c9f142](https://blert.io/raids/tob/65c9f142-f240-4284-9384-d32fa313067e/nylocas).

The prior [lifecycle study](nylocas-lifecycle-evidence.md) establishes cleanup as the last regular Nylocas's **despawn**, including dying bodies and final splits. All 13 boss drops follow:

```text
phase = firstWaveTick mod 4
bossDropTick = cleanupTick + 16 + ((phase - cleanupTick) mod 4)
combatMeleeTick = bossDropTick + 2
firstStyleChangeTick = bossDropTick + 11
```

The modulo result is 0–3. The cleanup-to-drop gaps are 16 ticks in two runs, 17 in seven, 18 in three, and 19 in one. The guide describes arrival on the fifth cycle after cleanup; the replay formula is the more precise observed scheduling rule. Sources: [pinned snapshots](research/nylocas/manifest.json), [Blert boss account](https://blert.io/guides/tob/nylocas/mechanics#nylocas-vasilias).

Dropping NPC 8354 has anchor **(3294,4247)** in all runs. The first combat snapshot is at this anchor in nine runs and has already moved in four; do not treat those first combat coordinates as four different fixed spawn points. The recorder deliberately preserves the boss's room identity when replacing the dropping NPC with the combat NPC, and suppresses the dropping NPC's intermediate despawn. Thus the contiguous room ID does not prove one native NPC entity was transformed in place. Source: [recorder two-stage boss handling](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/NylocasDataTracker.java#L166-L199).

## Attacks: four-tick cadence is common, fixed phase slots are not established

The first melee attack occurs at drop +3 in eight recordings and drop +6 in five. Relative to combat melee appearance, complete initial-phase attack sequences are `[1,5]` in eight runs, `[4,8]` in three, and `[4]` in two.

For the 288 complete later phases, the most frequent observed attack offsets relative to that phase's form update are `[3,7]` (94 phases), `[2,6]` (65), `[5]` (53), and `[4,8]` (29). Other sequences, including missing attacks and five-tick observed gaps, occur. Every combat style has both `[2,6]` and `[3,7]` samples; this difference is not simply a melee-versus-projectile distinction. See the [full counters and individual phases](research/nylocas/boss-summary.json), calculated from the [pinned recordings](research/nylocas/manifest.json).

These observations support a common four-tick interval when attacking successfully within a form. They do **not** establish universal attacks at switch +3/+7, or explain whether target availability, walking, random targeting, animation observation, or another state causes each delay. A global always-attacks-every-four-ticks timer also fails to reproduce the form transition gaps. Blert's guide describes two attacks per form and random single-player targeting, but exact server scheduling remains unresolved. Source: [Blert attack description](https://blert.io/guides/tob/nylocas/mechanics#attacks).

The recorder recognizes attacks by native animations **8004 melee, 7999 ranged, 7989 magic**. Source: [recorder animation dispatch](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/NylocasDataTracker.java#L245-L268). Transform animations and boss-specific projectile IDs were not established by these recorder events. The local native sequence symbols identify 7994–7996 as human egg throws, so these must not be guessed as boss transforms.

## Damage, health, and observation limits

Matching the boss's current style permits damage; other styles heal it and reflect damage to the player. This agrees with the user's requested behavior and the first-party [Blert style-rotation description](https://blert.io/guides/tob/nylocas/mechanics#style-rotation). The guide does not resolve whether the server checks style when the player launches an attack, when its hit resolves, or at both points. The replay's attack event and changing estimated HP do not expose sufficient attribution to settle this safely; a switch-crossing damage test remains necessary for an exact claim about that boundary.

The pinned recorder source defines regular boss maximum HP as **1875 for scales 1–3, 2187 for scale 4, and 2500 for scale 5**, with footprint size 4. All 13 solo/duo spawn snapshots contain maximum HP 1875. This is replay-confirmed only at scales 1–2; larger-scale values are recorder configuration evidence. Sources: [TobNpc boss definition](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/TobNpc.java#L110), [scale lookup](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/TobNpc.java#L514-L535), [replay counters](research/nylocas/boss-summary.json).

Boss HP differs from generic wave HP: `NyloBoss` extends `HpVarbitTrackedNpc`, which periodically corrects its estimate from a 0–1000 health varbit and tolerates discrepancies of up to five HP. First recorded zero to final despawn varies across this corpus: 4 ticks once, 5 six times, 6 four times, 11 once, and 13 once. Those values must not be promoted to a random server death-delay rule. They demonstrate that the first estimated zero is unsuitable as a precise lethal tick, especially around very low remaining health. Sources: [boss HP model](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/NyloBoss.java), [varbit reconciliation](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/HpVarbitTrackedNpc.java#L60-L76), [replay counters](research/nylocas/boss-summary.json).

The exact server death animation duration, damage roll statistics, hit-reflection timing/amount rounding, transform animations, and projectile parameters remain unverified by this study. The established spawn and form timings should be implemented independently of those gaps.

## Implementation choices and validation

The implementation uses the measured cleanup/drop/form schedule and native IDs,
size, anchor, and party HP scaling. It evaluates incoming styles at impact,
caps healing at maximum HP, and reflects the original damage to the player as
typeless damage. Impact-time checking is a working assumption, not a replay finding.
Style changes cancel the player's attack interaction; this behavior also remains
unverified by the sources above.

Attacks use native combat stats and accuracy/damage formulas, random party targets,
four-tick spacing, and at most two attacks per form. The first opportunity is
drop+3, then switch+2 in later forms, with movement and target availability allowed
to delay it. This reproduces common observed cadences but is not a complete model
of the variable phase-relative offsets. Hits currently land one tick after the
attack, and native attack animations are used without unverified projectile or
transformation effects. Death uses the native style animation with a five-tick
delay; this duration is provisional.

The native HUD uses `progress_type=1` for boss health, `progress_value` as
`currentHP * 1000 / maximumHP`, and `progress_max=1000`.
Blert maps room-state 3 to Verzik P1, so it must not be used for Vasilias.
Sources: [room-state enum](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/RoomState.java),
[health-varbit reader](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/RoomDataTracker.java),
[health normalization](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/HpVarbitTrackedNpc.java).

Validation on 2026-10-18: all 253 Nylocas integration tests and 48 Nylocas
combat integration tests pass. Boss coverage includes all five party scales,
the opening and subsequent form timings, every cleanup phase residue, native
combat and movement with protection prayers, matching/wrong-style hits,
healing caps and full reflection, multiple delayed hits crossing a switch,
and death/completion. The replay analyzer reproduces all 13 startup schedules
and 288 subsequent ten-tick form intervals. A live client visual check has not
been performed.
