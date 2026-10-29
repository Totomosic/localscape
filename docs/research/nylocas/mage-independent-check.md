# Drummie mage: independent replay cross-check

Research date: 2026-10-29. This independently joins raw raid NPC metadata and stage-12 events from the **19 date-eligible Drummie rooms already preserved in the ranger corpus**. All have start time ≥1788192000000, normal mode, duo scale, boss split ≤326 ticks and zero reported lost ticks. This overlap is a deliberately limited cross-check of the [full mage corpus](mage/README.md), not an additional 19 independent recordings or the entire requested search. It contains 1,384 Drummie attack observations. The primary study's full-corpus counts supersede this subset's frequencies.

The observations below were calculated separately from the mage analysis script. Source inputs are the [ranger manifest](ranger/manifest.json) and its checksum-pinned `recordings/<uuid>.raid.json.gz` / `.events.json.gz` files. Each room in the witness table links directly to its replay. `sN` means the actual recorded spawn tick of wave N, including stalls; attack age means attack tick minus the target's own birth tick. Last-spawned wave is determined by the greatest wave-spawn tick no later than the attack, regardless of array order.

## Event interpretation

The [event schema](https://github.com/blert-io/protos/blob/3c5bc5cfc315ff4266128601829e66615f3ef83d/event.proto) distinguishes player updates (4), observed player attacks (5), NPC spawn/update/removal (7/8/9), wave spawn (120), inferred stall (121), cleanup end (122), and boss spawn (123). The interpretation follows the [existing primary-source audit](ranger-event-interpretation.md), including its inspection of [attack recognition](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/core/DataTracker.java#L631-L689). An attack event is inferred from animation and interaction, so absence of such an event is not proof that no attack occurred. Its coordinates describe the player; the target's position and current native ID come from NPC events.

The locally preserved [attack definitions](ranger/attack-definitions.json), pinned to [Blert protos revision 3c5bc5c](https://github.com/blert-io/protos/blob/3c5bc5cfc315ff4266128601829e66615f3ef83d/attack_definitions.json), identify Dinh's special as type 24 with five-tick cooldown, scythe as type 38 with five-tick cooldown, sulphur blades as type 101 with four-tick cooldown, and Eye of Ayak as type 104 with three-tick cooldown. Barrage includes both identified staff variants and `UNKNOWN_BARRAGE` type 1; filtering only by equipped staff would undercount it. These are recorder definitions, not a guarantee of successful damage or every AoE victim.

## Wave 12: the first actionable east hit, then west

All **19/19** rooms record a scythe attack on the wave-12 east **north-slot** small, born at `(3310,4249)`. Its age is eight ticks in **18/19**, nine in one. The player's attack tile is `(3301,4249)` in 18 and `(3300,4249)` in one; the target's recorded tile is `(3302,4249)` in the 18 age-eight observations. The attack can therefore be actionable before the target's anchor meets an arbitrary `x≤3301` interior filter. An analyzer's interior-anchor crossing must not be treated as the universal first attackable tick.

The scythe happens with wave 13 already current in **16/19**, including 15 exactly on `s13`; it remains in the wave-12-current interval in three rooms. The instruction to scythe wave 12 on entry belongs to the target's timetable and must survive the wave-13 transition. The user's preferred southern non-fighting primary target is a proposed refinement: this subset consistently records the northern primary target. These primary events alone do not tell us which secondary scythe victims were hit.

All **19/19** then Dinh's-special the wave-12 west **south-slot** small, born at `(3281,4248)` and currently an aggro melee native ID at attack time. That special is exactly five ticks after the scythe in **18/19**; one room has a ten-tick gap. The Dinh's target age is 13 in 17, 14 in one and 18 in one. Its removal is strictly before `s14` in **15/19** and on `s14` in four. Removal on the same recorded tick does not establish server cap-check ordering.

The fastest [0c25e1 replay](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas) shows east scythe at 92 from `(3301,4249)`, west special at 97 from `(3293,4249)`, west target removal at 99 and wave 14 at 104. The [747b0a replay](https://blert.io/raids/tob/747b0a15-bdf7-4f83-aa41-6d2d4b214734/nylocas) repeats attacks at 92/97 but has wave 13 at 92 and wave 14 at 100. Thus the same target-age packet can occupy different last-wave headings.

## Wave 21: west barrage and northern melee

A west barrage is explicitly recognized in **13/19**, always at `s21+1`, always from `(3290,4249)`, targeting the north-slot small born at `(3281,4249)`. Twelve are `SCEPTRE_BARRAGE`, one `UNKNOWN_BARRAGE`. The other six are missing that recognized event; they are not evidence that the first-tick barrage should be optional. This observation supports the user's near-barrier positioning and first actionable barrage, while the recorder offset should not be inserted as an extra agent delay.

A sulphur-blade attack at `s21+6` occurs in **13/19**. In all 13, its target is a small melee north of the player, at y=4252–4254. These targets are existing splits with different birth waves and ages; this is not a fictitious wave-21 north lane spawn. Ten targets are removed on `s22`, two at `s22+1`, and one eight ticks before the delayed `s22`. The desired before-check overkill depends on damage and removal timing, not merely choosing sulphur blades.

In [747b0a](https://blert.io/raids/tob/747b0a15-bdf7-4f83-aa41-6d2d4b214734/nylocas), wave 21 spawns at 168, barrage occurs at 169 and sulphur at 174 from `(3291,4251)` onto target 42385 at `(3291,4252)`; target removal and wave 22 are both recorded at 176. In [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), the barrage animation is not recognized, but sulphur occurs at 178 from `(3292,4252)` onto target 46229 at `(3292,4253)`; both removal and wave 22 are at 180. These are useful timing witnesses without claiming that the animation or same-tick removal proves a successful pre-check kill.

## Wave 22: the western member of the south pair

All **19/19** observed wave-22 Dinh's specials target the **south-lane west-slot small**, born at `(3295,4233)`. Its initial style is magic, but its current ID at the melee special is 8342, so calling this a wrong-style attack from birth metadata would be incorrect. It is not a small from the west lane.

Sixteen specials occur at target age 10 from `(3295,4244)` onto `(3295,4243)`, three at age 11 from `(3294,4244)` onto `(3294,4243)`. An interior-anchor filter with south boundary y=4243 first sees the target inside on the attack tick in the usual case; that filter does not independently prove or disprove the user's one-tick-after-entry instruction. The exact tile/age observations are the reproducible evidence. This subset has no wave-22 big selected as a Dinh's primary, so the user's alternative big target remains a fallback. Its identity is the **east-spawned fighting big after changing from magic to melee**, not the pillar-bound west melee spawn. In `0c25e1`, east big 46649 changes to native ID 8351 at 189 and reaches `(3295,4246)` at 195, where it receives sulphur; west big 46650 remains ordinary melee ID 8345 while parking near `(3290,4245)`.

Before the special, the mage repeatedly uses Ayak on recent magic splits near the south side; in [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), targets 46637 and 46639 receive Ayak at 182/185, followed by Dinh's at 190. The southern pair's melee special and these magic-clearing actions support the requested preparation. The events cannot establish that every remaining south-side nylo is melee in every room.

## The wave-26 Dinh's occurs on the wave-28 timetable

The named primary target is the **wave-26 south melee big**, parked near the southwest pillar. It receives Dinh's in **16/19** rooms: 14 with wave 28 current, two after wave 29 already spawned. Offsets relative to actual `s28` are +7 (2), +8 (3), +9 (8), +10 (2), +11 (1). Player tiles cluster at `(3292–3293,4245–4246)`; target southwest anchors cluster at `(3292–3293,4243–4244)`.

The [0c25e1 replay](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas) is a concrete cap-relief witness. Wave 28 is at 228; special at 237 (`s28+9`) from `(3293,4245)` targets big 47701 at `(3293,4243)`. The big's estimated HP changes 16→0 at 238; its removal/split is later, at 245. Wave-28 west north small 48006 changes estimated HP 8→0 at 238 and is removed at 240, the same tick as wave 29. This is consistent with the special helping admit wave 29 through a small removal, while the big itself still occupies a slot.

Do not turn that witness into a claim that every west small took damage or that no south small was hit. West south small 48007 is magic at the special, and its estimated HP remains eight on tick 238. South west small 48008 drops 8→4 at 238 while the ranger has a recent ranged projectile; south east small 48009 was already at zero before the special. The event stream has no complete AoE victim list or damage-source attribution for those updates. The user's west-in/south-out geometry remains the requested targeting objective; primary-target frequency alone does not validate the exact AoE victim set.

Nor is `s28+9` a universal absolute deadline: the big can arrive differently, the 28→29 interval can be four ticks or much longer, and these observations contain late/missing specials. The user's desired special just before the useful wave-29 retry is an implementable reservation, not a claim that every successful human recording obeys it.

## Complementary roles and the late cutoff

The opening in [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas) demonstrates the division: ranger bows west ranged at 5 and east ranged at 11; mage shadows south magic at 6, Ayaks wave-2 west magic at 12 and wave-3 east magic at 16, then the wave-4 south magic big at 19. During wave 5, mage Ayaks east magic at 22 and sulphurs wave-4 east melee at 25 and wave-5 south melee at 29, while ranger works on west ranged targets. Mage work includes melee and magic; copying the ranger's ranged lane packet would duplicate effort.

The same room records mage scythe on the wave-13 east melee big at age 31 (tick 127, last wave 17), and mage Ayak on wave-19 east/south magic bigs at ticks 152/155 before south wave-20 big at 158. These are specific rotation tasks, not evidence that every old target deserves service. The [18b55b replay](https://blert.io/raids/tob/18b55b89-579b-4e7a-bb84-0d340fcceb2d/nylocas), outside this overlap but present in the full mage corpus, additionally shows that mage south-wave-19 work can continue after wave 20; do not assume the ranger implementation's pre-20-big exclusion applies to the mage.

All **19/19** overlap rooms contain at least one mage attack after wave 29 spawns on a birth-wave ≤29 target; each has 2–8 such observations. In [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), wave 29 is at 240, yet mage sulphurs old splits at 242/246 and scythes the wave-29 south big at 250, after wave 30 at 248; later old split attacks also occur. These contradict a literal observed invariant of never attacking ≤29 after 29.

The current user instruction explicitly requests that cutoff as the agent baseline. Preserve it as a requested policy and identify these human actions as deviations. The earlier ranger study's permission to finish a fresh lane packet before applying its cutoff belongs to that study; it does not silently authorize the same exception for this mage research. Newly born splits keep their own birth wave, per [the recorder's tick handler](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/challenges/tob/rooms/nylocas/NylocasDataTracker.java#L104-L136), so an old parent alone does not make a wave-30+ child ineligible.

## Room-level timing witnesses

All values below are room ticks. `—` means the specified observed primary attack was absent; it does not prove no AoE or unrecognized animation occurred. The wave-21 sulphur column is deliberately restricted to `s21+6`. `26 Dinh` requires the named south big, not any nearby special.

| Replay | Boss tick | s12 / s13 / s14 | East scythe / west Dinh | s21 / s22 | West barrage / north sulphur / 22 Dinh | s28 / s29 | 26 Dinh |
| --- | --- | --- | --- | --- | --- | --- | --- |
| [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas) | 308 | 84 / 96 / 104 | 92 / 97 | 172 / 180 | — / 178 / 190 | 228 / 240 | 237 |
| [747b0a](https://blert.io/raids/tob/747b0a15-bdf7-4f83-aa41-6d2d4b214734/nylocas) | 312 | 84 / 92 / 100 | 92 / 97 | 168 / 176 | 169 / 174 / 186 | 224 / 240 | 234 |
| [934cff](https://blert.io/raids/tob/934cff40-3524-4711-b02e-86e029798465/nylocas) | 312 | 84 / 96 / 104 | 92 / 102 | 172 / 180 | 173 / 178 / 190 | 232 / 244 | 239 |
| [fc3ae6](https://blert.io/raids/tob/fc3ae6ed-cc38-4901-898e-8facd01cb9fc/nylocas) | 316 | 84 / 96 / 104 | 92 / 97 | 172 / 180 | — / — / 190 | 232 / 248 | — |
| [0783d8](https://blert.io/raids/tob/0783d870-5e9e-4227-98bf-3a2d495cd66e/nylocas) | 316 | 84 / 92 / 100 | 92 / 97 | 168 / 176 | — / — / 187 | 228 / 248 | 237 |
| [bf4ebc](https://blert.io/raids/tob/bf4ebc82-56cb-4cb5-9df7-9a8342cf2e14/nylocas) | 316 | 84 / 92 / 100 | 92 / 97 | 168 / 184 | 169 / 174 / 194 | 232 / 244 | 241 |
| [efa275](https://blert.io/raids/tob/efa27528-eecf-48ba-8253-4815ef976eb7/nylocas) | 316 | 84 / 92 / 100 | 92 / 97 | 168 / 176 | 169 / 174 / 187 | 232 / 248 | 241 |
| [c112f9](https://blert.io/raids/tob/c112f942-5558-4ed3-af57-2118458aea8a/nylocas) | 316 | 84 / 92 / 100 | 92 / 97 | 172 / 180 | 173 / 178 / 190 | 228 / 240 | 236 |
| [419584](https://blert.io/raids/tob/4195844f-b13e-451f-916a-9b8ffd17b009/nylocas) | 316 | 84 / 92 / 100 | 93 / 98 | 168 / 176 | 169 / 174 / 187 | 236 / 240 | 247 |
| [f5aab6](https://blert.io/raids/tob/f5aab638-bf14-4511-8a83-026ea46cc37f/nylocas) | 320 | 88 / 96 / 104 | 96 / 101 | 172 / 188 | — / — / 198 | 236 / 248 | 245 |
| [437438](https://blert.io/raids/tob/4374381d-f598-4d96-90b6-e5204385bc46/nylocas) | 320 | 84 / 92 / 104 | 92 / 97 | 176 / 184 | 177 / 182 / 194 | 232 / 236 | 241 |
| [ff4098](https://blert.io/raids/tob/ff40985c-663c-4119-8070-9434283cb671/nylocas) | 320 | 84 / 92 / 100 | 92 / 97 | 172 / 180 | 173 / 178 / 190 | 232 / 244 | — |
| [6c9cb3](https://blert.io/raids/tob/6c9cb37e-45cd-4c41-92fd-8f4dd21e6f92/nylocas) | 320 | 84 / 92 / 100 | 92 / 97 | 168 / 176 | 169 / 174 / 186 | 224 / 240 | 231 |
| [73687c](https://blert.io/raids/tob/73687cf8-5599-4f4d-a92c-d663a3e9e78a/nylocas) | 320 | 84 / 92 / 100 | 92 / 97 | 172 / 180 | 173 / 178 / 190 | 232 / 244 | 241 |
| [3650ae](https://blert.io/raids/tob/3650ae9e-8234-4d76-b1a7-4bb879d7aa70/nylocas) | 324 | 84 / 92 / 100 | 92 / 97 | 176 / 188 | — / — / 198 | 236 / 252 | — |
| [c052bf](https://blert.io/raids/tob/c052bfe0-61a9-45a6-8544-435ef6592bcd/nylocas) | 324 | 84 / 92 / 100 | 92 / 97 | 172 / 180 | 173 / 178 / 190 | 232 / 252 | 240 |
| [36e9d7](https://blert.io/raids/tob/36e9d7cf-3953-4beb-b06d-7d04ec4fad58/nylocas) | 324 | 84 / 92 / 100 | 92 / 97 | 172 / 184 | 173 / — / 194 | 232 / 244 | 242 |
| [977784](https://blert.io/raids/tob/97778429-1912-47db-a6b0-45c0be484ca1/nylocas) | 324 | 84 / 92 / 100 | 92 / 97 | 168 / 176 | 169 / 174 / 186 | 228 / 240 | 237 |
| [16732a](https://blert.io/raids/tob/16732a46-4067-47f9-8c18-c4d60056726e/nylocas) | 324 | 84 / 92 / 100 | 92 / 97 | 172 / 184 | — / — / 194 | 232 / 248 | 240 |

## Limits

This is a successful-fast-room, one-mage, overlapping-partner subset. It supports timing and ownership anchors, not a proof of an optimal hierarchy or pairwise preference whenever two targets were simultaneously available. Damage rolls, estimated HP, merged observers and missing animation recognition limit causal interpretation. NPC removal, HP zero, attack launch and cap-slot release must remain separate events; same-tick wave admission and removal do not establish their server order. Exact click paths and every AoE victim cannot be reconstructed from primary-target events.
