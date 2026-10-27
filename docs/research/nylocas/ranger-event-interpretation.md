# Ranger recording interpretation and independent cross-check

Research date: 2026-10-27. This supports the ranger strategy study. It independently checks the first eight rooms in the boss-spawn-sorted [ranger manifest](ranger/manifest.json), using the raw raid metadata and stage-12 event snapshots. These eight are a deliberate fastest-room check, not eight independent players or a random sample. Conclusions about intent remain inferences from recorded actions.

## What the events mean

The pinned [Blert event schema](https://github.com/blert-io/protos/blob/3c5bc5cfc315ff4266128601829e66615f3ef83d/event.proto) defines event 4 as player update, 5 as player attack, 7/8/9 as NPC spawn/update/removal, 120 as wave spawn, 121 as wave stall, 122 as cleanup end, and 123 as boss spawn. Event 123 is the relevant boss-spawn split, rather than the first player attack on the boss.

An event-5 attack is inferred by the recorder from animation, weapon, attack cooldown, and interaction target. Continuous animations have special treatment. It is evidence of an observed attack, not a click log, individual projectile hit, successful damage, or lethal hit. Its `(xCoord,yCoord)` is the player's tile. `distanceToTarget` uses actor world-area distance and therefore includes the target's footprint. Join `attack.target.roomId` to the NPC's spawn/update records or the raid's room NPC metadata; the attack target alone does not carry complete nylo properties. See [DataTracker.checkForPlayerAttack](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/core/DataTracker.java#L631-L689).

The [pinned attack definitions](https://github.com/blert-io/protos/blob/3c5bc5cfc315ff4266128601829e66615f3ef83d/attack_definitions.json), fetched and byte-compared during this cross-check, identify the important attacks:

| Event attack type | Weapon | Definition cooldown | Interpretation |
| --- | --- | --- | --- |
| 12 | Toxic blowpipe, ID 12926 | 2 ticks | Ranged attack. |
| 68 | Toxic blowpipe, ID 12926 | 2 ticks | Blowpipe special attack. |
| 16 | Black chinchompa, ID 11959 | 3 ticks | Ranged; a primary target does not describe every splash victim. |
| 54 | Twisted bow, ID 20997 | 5 ticks | Ranged. |
| 19 | Dragon claws, ID 13652 | 4 ticks | Ordinary claw scratch. |
| 20 | Dragon claws, ID 13652 | 4 ticks | Claw special, defined cost 50. |
| 104 | Eye of Ayak, ID 31113 | 3 ticks | Magic; the ranger has an off-style magic weapon. |

These are recorder definitions, not a measurement of every attack interval or combat style selection in every historical client. In particular, scheduling two first-tick twisted-bow shots on waves four ticks apart is impossible under the five-tick definition; the actual opening is west on tick 5 and east on tick 11 in all eight checked rooms.

Use the wave whose spawn tick is greatest and no later than the attack tick. The order of events in the returned array can put a player's attack before the same-tick wave marker. NPC update payloads can omit unchanged nylo fields; preserve prior fields or join metadata instead of replacing the complete model with an update payload.

The recorder sets every NPC born on a tick to the wave current on that tick, including new splits. A split's `nylo.wave` is **its birth wave**, not its parent's lane wave. For example, room `0c25e1…` has child 42433 born at tick 28, wave 5, from parent 42154 born at tick 16, wave 4. See [the room tick handler](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/NylocasDataTracker.java#L104-L136). `parentRoomId` is inferred by same-tick removal/position matching and can be absent or ambiguous; see [parent assignment](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/NylocasDataTracker.java#L326-L350).

Consequently, a fresh split born after wave 29 should not be classified as old solely because its parent came from an earlier wave. For an age-based policy, use the child's own `spawnTick` and distinguish this from optional lineage-based ownership.

## Complementary assignments checked against eight rooms

The following patterns recur in the eight rooms linked below. These are evidence of complementary responsibilities; they do not prove a rule forbidding all teammate assistance.

| Wave | Ranger observation | Mage observation |
| --- | --- | --- |
| 1 | West ranged small, twisted bow at tick 5 in all eight. | South magic small. |
| 2 | East ranged small, twisted bow at tick 11 in all eight. | West magic small, sometimes attacked after wave 3 has spawned. |
| 4–5 | Ranger switches to west wave-5 ranged big. | Mage works on wave-4 south magic big and wave-5 east magic small. |
| 7 | South ranged big is the ranger's main lane assignment. | Mage normally handles south magic small and/or east melee small; cross-help on the ranged big occurs. |
| 14 | Ranger works on the south ranged/magic pair. | Mage works on west magic small. |
| 15 | Ranger works on the east ranged/magic pair. | Mage works on west pair or other cleanup. |
| 19 | Ranger can Eye the west magic big. | Mage works on east and sometimes south bigs. |
| 20 | Ranger twisted-bows the **west** big during its ranged flicker. | Mage works on south big. |
| 23 | Ranger twisted-bows south big in all eight. | Mage's other work is situational. |

An opening target can survive into a later wave, so a table grouped only by attacks strictly inside each wave's interval can omit a real assignment. Likewise, the target's initial style need not equal its style at the attack: wave-20 west big starts melee and is ranged for the ranger's observed shot. See raw updates before calling that a wrong-style attack.

## Wave-29 transition and the agreed cleanup cutoff

All eight checked recordings contain ranger attacks at or after the wave-29 spawn against NPCs whose recorded birth wave is 29 or earlier. The counts below include attacks on the exact wave-29 tick and exclude boss attacks. They are attack-event counts, not kills. The lane column excludes splits.

| Recording | Boss spawn | Ranger attacks on birth-wave ≤29 after wave-29 spawn | Of those, lane targets |
| --- | --- | --- | --- |
| [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas) | 308 ticks / 3:04.8 | 7 | 3 |
| [747b0a](https://blert.io/raids/tob/747b0a15-bdf7-4f83-aa41-6d2d4b214734/nylocas) | 312 ticks / 3:07.2 | 5 | 2 |
| [934cff](https://blert.io/raids/tob/934cff40-3524-4711-b02e-86e029798465/nylocas) | 312 ticks / 3:07.2 | 6 | 1 |
| [a1b6a6](https://blert.io/raids/tob/a1b6a6fe-5048-4229-998c-cd75f2b95648/nylocas) | 312 ticks / 3:07.2 | 6 | 3 |
| [dae05a](https://blert.io/raids/tob/dae05a58-6d36-40fe-8463-1d0a66f78a5c/nylocas) | 312 ticks / 3:07.2 | 8 | 2 |
| [fc3ae6](https://blert.io/raids/tob/fc3ae6ed-cc38-4901-898e-8facd01cb9fc/nylocas) | 316 ticks / 3:09.6 | 7 | 5 |
| [5eaf15](https://blert.io/raids/tob/5eaf15b9-e299-4e18-8bfb-3fe1f1535880/nylocas) | 316 ticks / 3:09.6 | 7 | 2 |
| [0783d8](https://blert.io/raids/tob/0783d870-5e9e-4227-98bf-3a2d495cd66e/nylocas) | 316 ticks / 3:09.6 | 4 | 3 |

For a concrete witness, in `0c25e1…` wave 29 spawns at tick 240. The ranger blowpipes wave-28 south lane target 48008 at tick 244, then wave-29 west lane targets 48455 and 48456 at ticks 246 and 248. A wave-29-born split, 48664, receives an Eye attack at tick 287. Thus “never attack a birth-wave ≤29 target once 29 spawns” is not an observed invariant, even in the fastest recording. The user clarified during this study: preserve fresh lane attacks and apply the birth-wave ≤29 cutoff during final cleanup. The strategy therefore completes the wave-29/30/31 lane rotation before entering that cleanup policy. Some recordings also hit old targets late in cleanup; those remain observed exceptions, rather than mandatory agent actions. The recordings alone cannot establish whether each exception was necessary or optimal.

The [mechanics audit](../../nylocas-mechanics-source-audit.md) documents other boundaries needed for the strategy: cap counts include dying NPCs through removal; killing a big creates two fresh smalls; `nylosAlive` in a wave event is the count after admission; recorded stall events are client inference; old-small expiry and big removal/split timing differ. A claim that an attack freed a cap slot on that tick requires removal evidence, not merely an attack animation or zero HP estimate.
