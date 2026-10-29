# Duo Nylocas mage: priorities by last-spawned wave

Implementation update: the mage now prioritizes its rotation and ranger ownership over idle reduction, favors newer eligible birth waves, and excludes targets more than four waves old. Wave-29 births are excluded only after wave 31 spawns. Earlier filler and blanket-cutoff descriptions below are historical research proposals; see the [current mage policy](nylocas-mage-plugin.md) and [validation results](nylocas-agent-planning.md) for implemented behavior.

**Wave 1: ayak or shadow the wave 1 south mage small.**

Run south from the entry toward `(3296,4244)` and hit the south east-slot magic small, then move west during cooldown. Every qualifying replay opens with **shadow at `s1+2`** from that tile. Ayak is the user's permitted alternative; its shorter cooldown would change the following timing and is not an observed opening variant in this corpus.

Use this breakdown with the [strategy hierarchy](nylocas-duo-mage-strategy.md). **Headings identify the last wave actually spawned**, while a named target retains its birth wave. `sN` is actual lane-spawn time including stalls. Birth, room entry and first actionable attack are different moments. Timings below are **Blert observations**, not extra ticks to wait after a target becomes actionable. North/south distinguish east/west lane slots; west/east distinguish south-lane slots.

Carry reserved packets across new wave markers. Outside them, prefer young eligible targets, useful AoE and the next assigned task, checking live color, HP, death state and partner claims. Exact tile paths are not recoverable; the coordinates below are observed route anchors. The strict requested ≤29 cutoff starts when 29 spawns and overrides pending old-target work. Recorded exceptions are labeled explicitly.

## Wave 2: travel west for the wave-2 magic small

Use the opening shadow cooldown to run toward `(3292–3293,4248–4249)`. No mage attack is recorded while wave 2 is current in any of the 62 rooms. Do not invent a second first-tick attack four ticks after a five-tick shadow. The west magic target is normally Ayaked at `s2+4`, when wave 3 has just spawned. The ranger handles wave-2 east ranged.

## Wave 3: Ayak wave-2 west magic, then move east

Ayak the west north-slot wave-2 magic small, normally on `s3`. Move east along the central row toward `(3298–3299,4248–4249)` for the east north-slot wave-3 magic small. That attack is commonly at target age three or four; age four puts it under wave 4. Both targets receive mage magic attacks in 62/62 rooms. Avoid taking the ranger's ranged opening targets.

## Wave 4: finish wave-3 east magic, then Ayak the south magic big

Finish the pending east small and move south toward `(3295–3296,4243–4244)`. Ayak the wave-4 south big, commonly at `s4+2…3`; a second hit can carry into wave 5. This big is attacked in 62/62 rooms. The wave-4 east melee small is saved for the nearby melee work in the next interval, rather than a detour that delays the south magic big.

## Wave 5: east magic, east melee, south melee, fresh splits

Finish the south wave-4 big if necessary, then Ayak the wave-5 east north magic small while returning east. Sulphur-blade the wave-4 east south melee small as it reaches the mage, often around its age nine from `(3300,4248)`. Turn south for the wave-5 south east melee small around `(3296,4243)` and clear recent melee/magic children during this long interval. Exact ordering depends on prior damage and cooldown. The ranger is clearing west ranged big/small work, not these melee targets.

## Wave 6: clear recent splits and prepare west magic

Keep clearing recent melee with sulphur and magic with Ayak while moving west. The wave-6 west north magic small is an assignment, but its attack commonly happens after wave 7 has spawned: only 12/62 rooms attack it in the wave-6-current interval, while 60/62 hit it by age 20. Do not duplicate the ranger's south ranged work or its saved east melee big claw-special setup.

## Wave 7: west wave-6 magic, south wave-7 magic, then nearby melee

Finish west magic if pending, then Ayak the wave-7 south east magic small, often around `s7+4` from south-center `(3294–3295,4244–4245)`. Use spare time for fresh splits or the east north melee small if nearby. The ranger owns the south ranged big and wave-6 east claw special. Mage dart assistance on the ranged big is an observed exception, not the main task. End moving west for wave 8.

## Wave 8: Ayak the west magic big

Open the wave-8 west magic big from around `(3290,4248–4249)`, commonly `s8+1…2`, and finish as needed into wave 9. All 62 rooms have a mage magic attack on it. This is a selected early big, not a rule to kill every magic big. Leave the east ranged small to the ranger.

## Wave 9: west-big finish, young splits and south melee

Finish the wave-8 west big if alive and clear its fresh magic/melee children. Sulphur the wave-8 south west melee small when useful as it arrives, often around age nine near the south mouth. Wave-9 east magic is conditional: mage attacks it by age 20 in 19/62; ranger coverage is common. Do not traverse the room solely to duplicate that target. Prepare the wave-9 west melee small and east-side wave-11 setup during cooldowns.

## Wave 10: wave-9 west melee and fresh split clearing

Sulphur the wave-9 west melee small, commonly in this interval, plus fresh magic/melee splits. Move toward east/central tiles as targets permit. The ranger's west→east→south chin packet handles the new ranged lanes. Keep the mage's next east barrage available instead of spending a late slow attack on a marginal ranged survivor.

## Wave 11: east barrage, then south-west magic small

Barrage the east magic pair promptly from the east edge, around `(3301,4248–4249)`. Explicit east-barrage events exist in 26/62 rooms, with imperfect attack recognition; do not claim it is visibly recorded in every replay. Ayak the **south west-slot** magic small, commonly `s11+6…8` from `(3296–3297,4244–4245)`, then the west magic big around its age nine onward. The ranger's recurring off-style task is the **south east-slot** small. Check actual coverage if a member survives the AoE.

## Wave 12: finish west wave-11 magic while staging east; reserve the scythe

Ayak the surviving wave-11 west big or smalls only while preserving the scythe cooldown. Reach `(3301,4249)` for the east melee pair's first actionable tick, usually observed at **`s12+8`**. Prefer the **southern non-fighting primary** as requested; scythe the pair. In the recordings the primary is north in 61/62, south in one at age nine, so the southern preference is an implementation refinement.

The timed east scythe often does **not** occur while 12 is the current wave: in 42/62 rooms it is under wave 13. Save special energy and move west immediately after scything. Do not spend the remaining five-tick cooldown on an intervening attack.

## Wave 13: carry the wave-12 scythe→Dinh's packet through this transition

If the east scythe is due now, perform it before ordinary wave-13 clearing. Exactly five ticks later Dinh's-special the **southern small from the wave-12 west lane**, typically observed at `s12+13` from `(3293,4249)` or `(3292,4249)`. This is not the wave-12 south big. The desired removal is before the first due wave-14 check, `s13+8`; verify actual death/removal after the attack.

The ranger normally covers the new west magic/ranged pair. Mage west-magic assistance is seen in 18/62 by target age 20 but must not displace the special. Keep the **wave-13 east melee big** alive for its later central scythe; do not open it just because it is new. South wave-13 melee is subsequent ordinary work.

## Wave 14: west magic, then recent south melee

Ayak the new west north magic small from the west side, commonly `(3290–3291,4249)`, finishing through misses if useful. This assignment appears in 61/62. Sulphur the wave-13 south west melee small as convenient, commonly around its age 13–17 near `(3292,4245)`. Leave the wave-14 south pair and east ranged big to ranger work. Remain west/central for the next melee arrivals.

## Wave 15: wave-14 west melee, then wave-15 west pair as it arrives

Sulphur the wave-14 west south melee small, often around age nine, while holding west mouth tiles `(3290–3291,4248)`. The new wave-15 west north melee small takes over once reachable, often after wave 16 spawns. Dart its west south ranged companion only when unclaimed and useful. The ranger is east handling its magic/ranged pair, so this nearby dart is legitimate assistance rather than a required ranged rotation.

Leave the **wave-15 south magic big** alive: all 62 copies remove at age 55. Do not confuse the mage role with mandatory ownership of that big.

## Wave 16: finish west melee and prepare the deferred east big

Sulphur the wave-15 west north melee small, commonly at its age eight to eleven from `(3290,4249)`. Finish useful west ranged work only if the ranger has not claimed it. The new wave-16 flicker smalls are mainly ranger work once ranged; there is no recurring mage first-tick east-magic attack. Move toward center and watch the approaching **wave-13 east melee big**. A few scythes begin in this interval; do not reset that target's age to 16.

## Wave 17: scythe the deferred wave-13 east big; leave new bigs alone

Scythe the old wave-13 east melee big when it reaches the central mage route, commonly around `(3293–3295,4248–4249)` and target age 29–35. The age-25…40 predicate matches 59/62 rooms. This is a deliberate deferred assignment, not general permission to chase old targets. Clear its fresh melee/magic children and other recent splits.

Leave the new wave-17 bigs alone. Across 186 lane bigs, 185 have no primary attack from either player and remove at age 55. Avoid interpreting “all three start magic” as an instruction to open them before cap.

## Wave 18: clear fresh magic/melee splits; leave ranged bigs to ranger

Work recent children while staying central/north-central, commonly near `(3295–3296,4249)`. Sulphur fresh melee and Ayak fresh magic rather than opening extra old bigs. The ranger handles the wave-18 ranged big workload. Coordinate its big-removal timing with actual cap pressure; do not copy its ranged assignments as mage priorities. Start moving toward the east/south magic work of wave 19.

## Wave 19: east magic big, then south magic big, with split relief

Clear useful fresh splits while the east big approaches, then kill the east wave-19 magic big and south wave-19 magic big. Both have mage magic attacks by age 25 in 62/62. East is commonly Ayaked around age eight; a shadow at age four is also observed. Stand near `(3295–3296,4248–4249)` and turn south as useful. The ranger supplies west-big magic coverage; mage help there is conditional.

South-big completion often extends into wave 20. Preserve the coming south wave-20 initial magic window and west wave-21 barrage; no old big is worth making either reservation late.

## Wave 20: south magic big, south-side magic smalls, west-barrier setup

Ayak the new south big before it changes style, commonly `s20+2…3` near `(3295–3296,4243–4244)`; 58/62 have a matching magic hit by age five. Finish a survivor with its current matching style if useful. The ranger targets the west big's ranged flicker; east is not the default mage assignment.

Use the longer interval to finish recent south wave-19 magic if needed and remove south-side magic smalls. This is the first interval whose wave marker reports cap 24. Drift west to **`(3290,4249)`** in time for 21, reserving the barrage's attack tick. The current ranger controller's pre-20-big exclusion must not silently delete this observed mage south-19 continuation.

## Wave 21: first-tick west barrage, then northern sulphur five ticks later

From directly beside the west barrier, barrage the fresh west mage pair on its first actionable tick, commonly recorded **`s21+1`**. Target the currently matching west small; north is the recurring primary. Move toward a northern melee small during the five-tick cooldown and sulphur it, ideally at **`s21+6`**, seeking an overkill before the first due 22 check at `s21+8`.

Northern targets are usually existing splits near y4252–4254, not a new north lane. If no useful northern target is available, use actual nearby eligible work without inventing a fixed room ID. The ranger chins east. Other free magic attacks should continue reducing the south-side magic population for Dinh's, but not replace either reserved attack when available.

## Wave 22: south magic cleanup, then Dinh's the south pair's western small

Ayak fresh south-side magic smalls while moving toward `(3295,4245–4246)`. Dinh's-special the wave-22 **south west-slot small**, one tick after entering as requested, normally observed at **`s22+10`** from `(3295,4244)` onto `(3295,4243)`. Eleven-tick examples use `(3294,4244)` after the target moves west. All 62 rooms special this target at age ten or eleven. It began magic but is **melee at the special**; select using live color.

If still busy clearing magic and the small timing is missed, the user-specified fallback is the wave-22 **east-spawned fighting big once it is melee**, when it reaches the mage. It starts magic; the current fighting-melee native ID is **8351**. The west-spawned melee big is pillar-bound and is a different target. In [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), the east big becomes 8351 at 189, approaches south-center and reaches `(3295,4246)` at 195, where the mage sulphurs it. No qualifying recording selects it as a Dinh's primary, so the special's fallback timing/outcome is unvalidated here. Do not mislabel the normal south-west small as a west-lane small. An interior-anchor boundary y≥4243 first sees the ordinary target inside on the attack tick; it is not an independent definition of first actionable entry.

## Wave 23: recover the southern melee cluster and prepare south wave 24

After Dinh's, use sulphur/scythe on useful nearby melee survivors—often wave-22 east big after its melee change, west big, or recent splits—around `(3295,4244)`. Choose by current style and HP rather than demanding one specific survivor. The ranger owns the wave-23 south bow and west ranged flickers. A surviving south big can be helped once melee, commonly at `s23+8`, but do not duplicate an already lethal bow.

Leave the east wave-23 magic big as the default: 58/62 copies remove at age 55. Position for the new south magic big next wave rather than travelling east solely because it is magic.

## Wave 24: south magic big, then east melee big as it reaches you

Ayak the wave-24 south magic big around `(3295–3296,4243)` and finish if needed. All 62 have a mage magic hit by age 15. Turn toward the east melee big and scythe it on arrival, often target age ten or eleven, usually **after wave 25 spawns**. This overlap is expected, not a missed wave-24 task. The ranger handles the west ranged big and its flickers.

## Wave 25: finish wave-24 east melee, then approach wave-25 west melee

Finish the pending east-big scythe around `(3299,4248)` when appropriate, then cross west during cooldown for the wave-25 west melee big. Kill it on arrival with scythe; Dinh's is an observed alternative if energy permits while still funding 26-Dinh's. Common attacks are age seven onward near `(3290–3291,4248)`, with many landing in wave 26. Do not add a new east-magic-big assignment: that big usually expires. The ranger handles south ranged.

## Wave 26: finish west wave-25 melee; preserve the new south big

Complete useful west wave-25 melee work, then Ayak the new **west wave-26 magic big** when ready. This is a short four-tick interval, so the west magic hit commonly occurs with 27 current. Stay west around `(3291–3292,4248)`.

**Do not perform “26-Dinh's” now.** Save the south wave-26 melee big on the southwest pillar for the wave-28 geometry. The new east magic big is not a mandatory kill; 56/62 remove at age 55.

## Wave 27: west wave-26 magic, then west wave-27 magic

Ayak the west wave-26 big, then the west wave-27 magic big, using `(3290–3292,4248)` and finishing into wave 28 as needed. Both assignments have 62/62 support by target age 25. Clear fresh local magic children without sacrificing the coming special. East wave-27 big assistance is conditional, not a compulsory traversal. The ranger handles the south big's ranged window, often as wave 28 spawns.

## Wave 28: finish west magic and time the saved south-big Dinh's

Finish useful west magic work and stage southwest near `(3293,4245–4246)`. Reserve the five-tick special's cooldown. Dinh's the **wave-26 south melee big at the southwest pillar** as the west wave-28 smalls enter the intended splash geometry while south ones remain excluded. Common recorded offset is **`s28+9`**; use actual geometry and the retry cycle rather than a timer alone.

The desired result is removal of useful smalls just before the wave-29 retry. Two or three wave-28 retries are acceptable. The ranger handles the south pair's ranged flickers. Avoid hitting the saved big earlier or consuming special energy on unrelated work. There is no complete secondary-victim log, so west coverage and south exclusion remain targeting requirements rather than proven per-room splash results. If 29 has already spawned, the requested cutoff cancels this old-target packet.

## Wave 29: stop attacking birth-wave ≤29 targets

Apply the user's **immediate cutoff**, including wave-29's own lane spawns, old splits and any late south wave-26 special. Stage central/east for the next new magic big; continue protection/movement around ignored old attackers. A fresh child born while 29 is current is also birth-wave 29 and is excluded. Children born in wave 30+ remain eligible regardless of parent age.

This entry deliberately differs from the recordings: all 62 scythe the south wave-29 big, usually age eight to ten, and every room contains some post-29 attacks on older targets. Those actions are evidence of human play, not included priorities under the requested cutoff. Skipping the wave-29 big is therefore not a replay-validated performance result.

## Wave 30: Ayak the new east magic big and fresh eligible children

Kill the new east magic big with Ayak from central/east tiles, often `(3296–3297,4248–4249)`, finishing after 31 spawns if necessary. All 62 record a matching magic hit by its age 25. Clear young magic/melee children born in wave 30; keep the ≤29 exclusion. Leave west ranged big and south ranged-flicker chin coverage to the ranger unless an unclaimed survivor requires useful help.

## Wave 31: east magic small, pending east big, then young-tail cleanup

Finish the wave-30 east big if needed and Ayak the wave-31 east north magic small from the east/central row, commonly near `(3299–3300,4249)` or `(3295,4248–4249)`. Both named targets have 62/62 support within age 25; their ordering varies. The ranger clears the west ranged packet, south chins and east ranged member.

Then clear eligible young magic/melee children, following them north toward pillars when worthwhile. Choose useful clusters/finishes, favor newer comparable targets, and use darts only for useful unclaimed ranged leftovers. Do not return to ignored ≤29 survivors or open ignored old bigs. Children born after an old big expires are eligible if their own birth wave is 30+, including children born after all lane waves have spawned. Stop wave-room targeting at the boss transition; boss combat is outside this strategy.

## Validation against the recordings

The [corpus/method](research/nylocas/mage/README.md) preserves every qualifying recording. Run `python tools/research/nylocas_mage.py` to verify input checksums and regenerate [all attacks](research/nylocas/mage/attacks.csv), [per-wave actions/tiles](research/nylocas/mage/waves.csv), [full positions](research/nylocas/mage/positions.csv), [lane lifetimes](research/nylocas/mage/lane-npcs.csv), [critical packet/check timings](research/nylocas/mage/critical-packets.json) and [exact validation predicates](research/nylocas/mage/validation.json). Each denominator is all 62 rooms; missing an exact predicate does not establish that the task was never attempted by another weapon/time or unrecognized animation.

The generated table below tests **target age**, while the entries above use **last-spawned wave**. Some predicates additionally constrain the current wave, tile or category; their full definitions are in JSON. A zero-support user preference/fallback is deliberately retained as a validation boundary.

<!-- MAGE_ANCHOR_VALIDATION -->

| Observed anchor (exact predicate in JSON) | Target age tested | Rooms | Replay witness |
| --- | --- | --- | --- |
| 1 south magic opener | 0–12 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 6, last wave 1 |
| 2 west magic | 0–15 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 12, last wave 3 |
| 3 east magic | 0–15 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 16, last wave 4 |
| 4 south magic big | 0–30 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 19, last wave 4 |
| 5 east magic | 0–20 | 52/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 22, last wave 5 |
| 4 east melee during 5 | 8–16 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 25, last wave 5 |
| 5 south melee | 8–20 | 60/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 29, last wave 5 |
| 6 west magic | 0–20 | 60/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 41, last wave 7 |
| 7 south magic | 0–20 | 60/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 44, last wave 7 |
| 8 west magic big | 0–35 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 54, last wave 8 |
| 9 east magic | 0–20 | 19/62 | [18b55b](https://blert.io/raids/tob/18b55b89-579b-4e7a-bb84-0d340fcceb2d/nylocas), tick 58, last wave 9 |
| 8 south melee during 9 | 8–20 | 32/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 61, last wave 9 |
| 9 west melee during 10 | 8–25 | 40/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 69, last wave 10 |
| 11 east barrage | 0–12 | 26/62 | [747b0a](https://blert.io/raids/tob/747b0a15-bdf7-4f83-aa41-6d2d4b214734/nylocas), tick 77, last wave 11 |
| 11 south west magic small | 0–20 | 60/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 83, last wave 11 |
| 11 west magic big | 0–30 | 60/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 86, last wave 12 |
| 12 east scythe birth+8 | 8 | 54/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 92, last wave 12 |
| 12 east scythe south target | 8 | 0/62 | No matching observation |
| 12 west south Dinh at +13 | 13 | 52/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 97, last wave 13 |
| 13 west magic assistance | 0–20 | 18/62 | [378cf3](https://blert.io/raids/tob/378cf375-2692-4a32-b48c-8bd12333df81/nylocas), tick 104, last wave 14 |
| 13 south melee | 8–30 | 61/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 111, last wave 14 |
| 14 west magic | 0–20 | 61/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 105, last wave 14 |
| 14 west melee | 8–25 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 115, last wave 15 |
| 15 west melee | 0–25 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 123, last wave 16 |
| 15 west ranged darts | 0–25 | 31/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 119, last wave 15 |
| 13 east melee big deferred | 25–40 | 59/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 127, last wave 17 |
| 19 east magic big | 0–25 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 152, last wave 19 |
| 19 south magic big | 0–25 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 155, last wave 19 |
| 20 south magic big | 0–5 | 58/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 158, last wave 20 |
| 21 west barrage birth+1 | 1 | 42/62 | [18b55b](https://blert.io/raids/tob/18b55b89-579b-4e7a-bb84-0d340fcceb2d/nylocas), tick 169, last wave 21 |
| 21 north sulphur at +6 | 0–51 | 40/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 178, last wave 21 |
| 22 south west Dinh birth+10 or +11 | 10–11 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 190, last wave 22 |
| 22 east fighting melee big Dinh fallback | 0–30 | 0/62 | No matching observation |
| 24 south magic big | 0–15 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 201, last wave 24 |
| 24 east melee big scythe | 7–18 | 59/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 207, last wave 24 |
| 25 west melee big | 7–18 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 215, last wave 25 |
| 26 south big Dinh during 28 | 0–51 | 47/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 237, last wave 28 |
| 26 west magic big | 0–25 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 220, last wave 27 |
| 27 west magic big | 0–25 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 223, last wave 27 |
| 29 south big scythe observed exception | 0–25 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 250, last wave 30 |
| 30 east magic big | 0–25 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 255, last wave 30 |
| 31 east magic small | 0–25 | 62/62 | [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), tick 259, last wave 31 |

<!-- /MAGE_ANCHOR_VALIDATION -->

### Concrete timing witnesses

The fastest [0c25e1 — 3:04.8](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas) has `s12=84`, `s13=96`, `s14=104`; east scythe at 92 and west Dinh's at 97. In [747b0a — 3:07.2](https://blert.io/raids/tob/747b0a15-bdf7-4f83-aa41-6d2d4b214734/nylocas), `s13=92`, so the same attacks at 92/97 both fall under wave 13. These independently demonstrate why the target's wave must not be mistaken for the current-wave heading.

In `747b0a`, west barrage at 169 from `(3290,4249)` is followed by northern sulphur at 174 from `(3291,4251)` onto `(3291,4252)`, with removal and wave 22 both at 176. In `0c25e1`, south magic split hits at 182/185 precede the south-west small Dinh's at 190 from `(3295,4244)`. Its 26-Dinh's at 237 is followed by a west-small removal at 240, matching wave 29, while the primary big remains until 245. The [independent cross-check](research/nylocas/mage-independent-check.md) lists 19 room-level witnesses and the raw HP/removal details.

Across the broader partner set, [18b55b — 3:04.8](https://blert.io/raids/tob/18b55b89-579b-4e7a-bb84-0d340fcceb2d/nylocas) with Lacking S19 repeats scythe/Dinh's at 92/97, then barrage/sulphur at 169/174. Its 26-Dinh's is at 237 although 29 spawned at 236: a concrete human exception cancelled by the requested policy. Its south wave-19 magic hits at 157/160 occur after `s20=152`, independently supporting the mage's continuation rather than copying the ranger's pre-20-big exclusion.

These checks establish the factual target/tile/timing basis of the guide. They do not prove exact splash victim sets, within-tick cap-check ordering, or the relative speed of the requested refinements. The southern wave-12 primary, alternative wave-22 big special and immediate ≤29 cutoff remain explicitly distinguished from recurring observed behavior.
