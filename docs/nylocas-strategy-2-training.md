# Nylocas strategy 2 training

Strategy 2 uses the ranger’s Ascension crossbows and Zorya’s tome and the mage’s existing loadout. Select it with `::nylobot range 2`, `::nylobot mage 2`, or `::nylobot spectate 2`. The last command runs both bots. Omitting the number or supplying `1` selects the original strategy. [Rotation brief](../strategy-v2-modifications.md) · [Live setup](nylocas-bots.md)

On 500 fresh seeds, the current strategy met the 3:22 reliability goal in **494/500 rooms (98.8%)**. The **488 eligible rooms** averaged **3:11.81** against a 3:09 goal. Their P95 was **3:19.2** against a 3:16.8 goal. All 500 rooms completed.

## Training conditions

Every room used the native server cycle, randomized damage, 99 starting HP, finite food, prepot, utility spells, live server boosts and queued-damage observations. Boss time ends at the spawn. The eligible mean, P5 and P95 exclude completed rooms with a wave-19 stall. Reliability includes every trial. Each game tick is 0.6 seconds. Strategy 2 has no reference recordings, so rotation checks use the requested assignments and confirmed native attacks. [Training criteria](nylocas-training-guidance.md) · [Per-seed results](research/nylocas/strategy-2-2026-11-28.csv)

## Development

The 100-room comparisons used seeds 11280001–11280100. The first traced batch used the first 20 of those seeds. An original-strategy replay matched all gameplay columns on all 100 baseline seeds.

| Configuration | Rooms | Eligible mean | Eligible P95 | Wave-19 failures | By 3:22 |
| --- | ---: | ---: | ---: | ---: | ---: |
| Strategy 1 control | 100 | 3:12.72 | 3:19.2 | 3 | 99/100 |
| First strategy 2 rotation | 20 | 3:12.38 | 3:19.2 | 1 | 20/20 |
| South-7 bow, south-11 handoff and west-5 split tracking | 100 | 3:12.76 | 3:19.2 | 2 | 98/100 |
| Three-mage threshold for the flexible early special | 100 | 3:11.93 | 3:19.2 | 4 | 100/100 |
| Extra priority for charged shots on magic smalls, tested against the handoff version | 100 | 3:12.88 | 3:19.2 | 2 | 97/100 |
| Three-mage threshold with reserved east-6 scythe and protected west-8 follow-up | 100 | 3:12.48 | 3:19.2 | 4 | 98/100 |
| East-11 split priority and immediate crossbow clearance | 100 | 3:12.78 | 3:19.2 | 4 | 99/100 |
| Late south-19 melee recovery added to the preceding configuration | 100 | 3:12.68 | 3:19.2 | 4 | 98/100 |
| Ignore already-covered ranged smalls when choosing an ordinary chin, tested against east-11 split priority | 100 | 3:12.90 | 3:19.2 | 4 | 96/100 |
| Double the cost of projected stalls on waves 10–12 and 21–27, tested against south-19 recovery | 100 | 3:12.94 | 3:19.2 | 3 | 99/100 |
| Reserve west, east and south chins on wave 10, tested against south-19 recovery | 100 | 3:12.91 | 3:19.2 | 0 | 99/100 |
| Adjust the mage’s firing square during cleanup cooldowns, tested against the three-chin rotation | 100 | 3:13.01 | 3:19.2 | 0 | 100/100 |
| Finish a surviving west-20 big after the ranger’s bow, tested against the three-chin rotation | 100 | 3:12.65 | 3:19.2 | 0 | 99/100 |
| Move west on the free tick after lethal damage is confirmed on east-24 | 100 | 3:12.50 | 3:19.2 | 0 | 99/100 |
| Yield unassigned, uncharged tome targets to an available mage below the wave cap | 100 | 3:12.05 | 3:19.2 | 0 | 99/100 |

The two-target threshold spent the flexible special on wave 7 in all 100 rooms. Requiring three available mages before the west-11 special spread it across waves 7–15. The ranger spent all three specials in every development room with either threshold. The retained rule also permits the flexible special on two available mages after the west-11 special. The separate charged-shot priority increased the eligible mean and was dropped.

Reserving the east-6 scythe brought its first launch to entry ages 7–8 in all 100 development rooms. Without the reservation, 23 first launches came at ages 21–29. Both policies hit a west-5 child before east-6 in 92 rooms and opened west-8 on spawn in all 100. The reservation left fewer opportunities for a second hit on the west-5 children. Both children died by east-6 entry in 54 rooms, compared with 63 using the three-mage threshold alone. The route was selected for the requested east-6 timing.

The first 200-seed holdout (11280101–11280300) completed 200/200, with 194 eligible rooms averaging 3:12.26 and a P95 of 3:19.2. The ranger spent three specials and attacked west-24 before south-24 in all 200 rooms. Both assignments fell to combat damage in every room. The initial audit counted any two pre-cap energy debits that included west-11. Checking the target of each debit revealed that two rangers repeated west-11, giving 198/200 rooms with distinct assignments. The mage used the east-22 Dinh in 189 rooms and the south-29 Dinh in 32 of 34 insta-29 rooms. The original strategy averaged 3:12.39 over 196 eligible rooms on those seeds.

The mage recognizes nearby melee splits up to six ticks old after the east-11 barrage, including children born just before wave 11. It can spend one spare attack on wave 17 when at most six nylos are alive, earlier priority parents are covered and the next scheduled attack still fits. The ranger takes a ready crossbow clearance over ordinary uncharged magic when the magic target can wait one crossbow cycle. Named assignments, stored attacks and rescue targets retain priority.

In the 100-room development comparison, late south-19 recovery reduced natural expiries of that parent from four to one. Earlier wave-19 failures and the number of transitions completed before wave 21 were identical. This rule applies to the surviving parent after its permanent melee flicker and preserves the south-20 assignment.

Two alternatives stayed experimental. Keeping chins off clusters containing a single uncovered target removed all 37 observed launches of that kind. The ranger nulled 27 fewer targets, the mage nulled 27 more, and four fewer rooms finished by 3:16.8. Charging both planners twice as much for a projected stall also left P95 at 3:19.2.

Removing the optional crossbow shot between the west and east wave-10 chins raised south-10 chin coverage from 41 to 100 rooms. All 100 retained the west-11 tome special and both wave-12 mage attacks. Failed admissions after wave 11 fell from 64 to 33, and wave-19 failure rooms fell from four to zero. The all-room average stayed at 321.52 ticks. The eligible mean increased because the comparison policy had excluded its four wave-19 failures. Full transitions fell from 66 to 57 rooms, with the west-20 big the most frequent survivor.

Giving the ranger a reachable sulphur-blade follow-up on a surviving west-20 big cut the number still alive at wave 21 from 25 to eight. Full transitions rose from 57 to 74, and all 100 rangers kept the east-21 chin. A separate attempt to move the mage closer to fresh melee splits during ordinary Eye cooldowns on wave 31 left the upper boss time at 3:19.2 and increased the mean by 0.10 seconds. It stayed experimental.

The mage first swung its scythe at east-24 at age six in 91 development rooms. Moving west on the free tick after a confirmed lethal scythe brought the first west-25 hit to age six or seven in 83 rooms, up from 69, with identical east-24 timing. All 13 insta-29 rooms kept the south-29 Dinh. The change leaves the native Death Charge and Spellbook Swap schedules in place.

On the next 200 seeds (11280301–11280500), the west-20 recovery version averaged 3:11.82 over 195 eligible rooms, versus 3:11.77 over 195 for strategy 1. Both had an eligible P95 of 3:19.2. Strategy 2 reached the boss by 3:22 in 199 rooms, and 159 met the full wave-21 transition goal. All 200 rangers spent three specials and attacked the two wave-24 assignments in order. Both bigs died in combat in every room. In two rooms, seeds 11280346 and 11280478, a missed first special allowed the flexible special to fire on west-11 as well. The final policy uses an ordinary follow-up on that parent and spends the flexible special on another target.

The corrected policy replayed the 133-seed interval containing both double-special rooms. All 133 completed and spent their three specials on the assigned targets. The two affected rangers moved their flexible specials to waves 14 and 12 respectively.

## Paired comparison at the first checkpoint

Both strategies ran on seeds 11280501–11280700. The last selection concerned ordinary, uncharged magic against unassigned smalls. When the room has at least four spare places after imminent splits and the partner can attack the target within three ticks, the ranger yields it to the mage. Across these 200 rooms the handoff reduced uncharged casts from 1,602 to 1,490 and increased the number of rooms reaching 3:22 from 196 to 197. Rooms without a wave 21–27 stall fell from 60 to 52. The table uses all 200 rooms for deadline and stall counts.

| Metric | Strategy 1 | Strategy 2 |
| --- | ---: | ---: |
| Completed | 200/200 | 200/200 |
| Eligible rooms | 194 | 195 |
| Eligible mean, goal 3:09 | 3:11.99 | 3:12.12 |
| Eligible P5 | 3:04.8 | 3:07.2 |
| Eligible P95, goal 3:16.8 | 3:19.2 | 3:19.2 |
| By 3:16.8 | 181/200 | 181/200 |
| By 3:22, goal at least 95% | 199/200 | 197/200 |
| Wave-19 failure rooms | 6 | 5 |
| At most one pre-cap stall | 111/200 | 122/200 |
| Zero stalls on 21–27 | 99/200 | 52/200 |
| Two or three stalls on 28 | 136/200 | 136/200 |

The paired difference across all completed rooms was 0.24 ticks slower with strategy 2. Its approximate 95% interval ranges from 1.11 ticks faster to 1.59 ticks slower. The 3:22 reliability target was met. The eligible mean and P95 were 3.12 and 2.40 seconds above their respective targets. Strategy 2 cleared all transition parents before wave 21 with no admission retry in 153 rooms. Of the 200 rooms, 120 finished between 3:07 and 3:12, and 74 took longer than 3:14.

All 200 rangers hit south-3 with crossbows, covered west-6 and south-7 with the tome and spent two pre-cap specials plus one special on a wave-24 big. Each attacked west-24 before casting on south-24. Both bigs died in combat in all 200 rooms. All 200 third specials launched while wave 24 was current. In the preceding policy, seed 11280517 had used its third special one tick into wave 25 after the ranger ate at 62 HP. The selected ranger made 2,999 pre-cap tome launches. Of those, 400 were specials and 1,109 used stored attacks, together accounting for 50.3%. Each accurate special can add three stored attacks, giving a maximum of eight special or stored launches per room from the two pre-cap specials. The ranger still uses ordinary attacks to finish assigned or urgent targets after the charges run out.

The mage hit a west-5 child before east-6 in 181 rooms, scythed east-6 on entry in all 200 and opened west-8 on its spawn tick in all 200. It barraged east-11 and completed both wave-12 attacks in all 200. Nineteen rooms included a native melee hit on a fresh east-side split between the barrage and the first south-11 cast. East-22 took the delayed Dinh in 183 rooms. The mage used the south-29 Dinh in 30 of 31 insta-29 rooms.

## Independent validation of the first checkpoint

The selected source ran on another 200 seeds (11280701–11280900) after the last policy choice. All 200 completed, with two wave-19 failures. The 198 eligible rooms had P5 **3:04.8**, mean **3:11.68** and P95 **3:19.2**. Of all 200 trials, 185 reached boss spawn by 3:16.8 and 198 by 3:22.

The ranger spent all three specials on their intended targets and waves in every room. Specials and stored attacks accounted for 1,479 of 2,892 pre-cap tome casts (51.1%). Both wave-24 bigs died in combat in every room. The mage scythed east-6 on entry and hit west-8 on spawn in all 200. It attacked a west-5 child before east-6 in 185 rooms, used the delayed east-22 Dinh in 190, and hit south-29 with Dinh in 35 of the 36 insta-29 rooms.

There were 133 rooms with at most one pre-cap stall, 161 full transitions, 61 rooms with zero stalls on waves 21–27, and 126 with two or three stalls on wave 28. In 43 rooms the last living targets were old nylos already expiring. Another 11 finished fresh work on the same boundary. Across the paired and independent runs combined, this policy completed 400/400 rooms and reached the boss by 3:22 in 395/400.

## Resumed training

The first checkpoint met the 3:22 reliability goal. It missed the mean and P95 targets. On the two earlier 200-room cohorts, 393 rooms qualified for the timing sample. The 22 rooms with no stalls through wave 27 averaged 3:07.1. The 91 with one stall averaged 3:09.0, and the 118 with two averaged 3:11.1. These are observed groups within the same policy.

The new comparisons used the same native loadouts, finite supplies, damage rules and boss-spawn endpoint. On the 100 development seeds, opening the wave-18 parents early improved the number of stall-free wave 21–27 rooms from 24 to 70, but 61 rooms stalled on wave 19 and only 85 met 3:22. Checking headroom before each parent restored zero wave-19 failures, with an eligible mean of 3:12.12. Both remained experimental.

Extending the cleanup projection from 16 to 32 ticks gave means of 3:11.90 on the development set and 3:12.01 on the 200-room paired set. P95 was 3:19.2 on both. Giving the mage a safe two-target early Dinh sweep increased its use from 34 to 50 development rooms. Every mage kept the wave-11 barrage and both wave-12 attacks, but the eligible mean rose from 3:12.05 to 3:12.43.

Both bots also tried moving toward the planned firing square during cooldown whenever they could still reach it by the attack tick. On the 100- and 200-room comparison sets this gave a P95 of 3:16.8. A fresh 300-room comparison, starting at seed 156337459231236, reversed that improvement. The preceding movement rule was retained.

| Policy on the same 300 new seeds | Eligible mean | Eligible P95 | By 3:22 | Wave-19 failures | Zero stalls on waves 21–27 |
| --- | ---: | ---: | ---: | ---: | ---: |
| First checkpoint | 3:11.74 | 3:19.2 | 297/300 | 7 | 88/300 |
| Move toward the planned square during cooldown | 3:12.26 | 3:19.2 | 295/300 | 10 | 96/300 |

Pooling the two disjoint validation sets for the first checkpoint gives **500/500 completed**, **495/500 by 3:22**, and **491 eligible rooms**. Their mean was **3:11.72**, P5 **3:04.8**, and P95 **3:19.2**. Nine rooms had a wave-19 stall. Only 149/500 avoided stalls on waves 21–27.

## Ranger special timing and mage south-20 follow-up

Two native replays exposed different ways to miss an assigned tome special. On seed `156337459231270`, the cooldown-movement experiment still had a stored tome attack in a crowd of magic nylos on wave 18. The ranger now spends its flexible special in that situation when the observed native regeneration timer guarantees another 50% energy by the earliest possible wave 24. Replaying the same movement and seed confirmed specials on waves 11, 18 and 24. On seed `156337459231499`, the ranger ate at 59 HP on the tick it could launch the south-24 special. It now defers food one tick when the named shot is legal, prayer is active and incoming damage through the following tick leaves at least 55 HP. The native replay confirmed the special on wave 24 at tick 214.

The mage now uses its blowpipe when a wounded south-20 parent flickers ranged at age five. The two-tick weapon cooldown leaves its melee follow-up available at age seven if the parent survives. On the 200 paired seeds, this change improved the eligible mean from 3:12.12 to 3:11.99 and increased rooms with zero wave 21–27 stalls from 52 to 55. Following the wave-21 barrage with a two-target scythe produced the same stall-free count and a slower mean on both comparison sets. That variant stayed experimental.

On the same 300 seeds used to compare movement, the selected ranger and mage changes improved the eligible mean from 3:11.74 to 3:11.38 and P95 from 3:19.2 to 3:16.8. Seven wave-19 rooms were excluded from each timing sample. All 300 rangers spent the three specials as assigned and attacked west-24 before south-24. Both parents died in combat in every room. The repaired rotation reached 3:22 in 297 rooms and cleared waves 21–27 without a stall in 99. These seeds had already informed the repairs.

## Independent validation of the ranger and mage changes

The selected changes ran on 500 fresh native rooms starting at seed `233594316842202`. All 500 completed. Twelve rooms stalled on wave 19 and remained in both deadline counts. The 488 eligible rooms had a P5 of 3:04.8.

| Measure | Target | Observed | Result |
| --- | --- | ---: | --- |
| Eligible mean | 3:09 or faster | 3:11.81 | 2.81 s over |
| Eligible P95 | 3:16.8 or faster | 3:19.2 | 2.4 s over |
| All trials by 3:22 | At least 95% | 494/500 (98.8%) | Met |

Of all 500 rooms, 465 reached the boss by 3:16.8, 316 had at most one pre-cap stall, 417 cleared the transition parents by wave 21, 175 had zero stalls on waves 21–27, and 325 had two or three stalls on wave 28. Among eligible rooms, the 35 with zero stalls through wave 27 averaged 3:07.61. The 154 with two averaged 3:11.38, and the 101 with three averaged 3:13.54. These are groups of observed rooms. Damage rolls and cleanup times also vary between them.

All 500 rangers spent two specials before cap and the third on a wave-24 parent. Each hit south-3 with the crossbows, cast on west-6 and south-7, and attacked west-24 before south-24. Both wave-24 parents died in combat in every room. Specials and stored attacks accounted for 3,772 of 7,467 pre-cap tome casts (50.5%). The mage hit a west-5 child before east-6 in 460 rooms, scythed east-6 at age seven or eight in 499, and hit west-8 on spawn in all 500. It used the delayed east-22 Dinh in 480 and the south-29 Dinh in 63 of the 64 insta-29 rooms.

## Paused checkpoint

The last comparisons used the retained pair as their control. Scoring the remaining cleanup work by style after wave 29 increased the eligible mean from 3:11.99 to 3:12.23 on the 200 paired seeds. A ranger variant used its flexible special on wave 8 when two magic nylos were available and a third was due on wave 9. That increased the eligible mean by 0.12 seconds on the 100 development seeds and 0.11 seconds on the 200 paired seeds. P95 stayed at 3:19.2 on both sets. Both variants stayed experimental.

Training paused at Jordan’s request. The working policy and all 16 adapter source files match the snapshots in `.state/simulations/strategy2-goal-repairs-independent500/`. The final verification passed 1,092 Java policy tests and 174 native integration tests. The Python suite ran 62 tests with four skipped. Strategy 1 matched its recorded gameplay on 100/100 seeds. [Continuation handoff](nylocas-training-resume.md)

## Reproduce the comparison

The command below reproduces the latest independent strategy 2 validation from the current source. For the earlier paired comparison, use seeds 11280501–11280700 and the corresponding archived policy snapshots. Give each run its own output directory.

```bash
mkdir -p .state/simulations
run_dir=$(mktemp -d .state/simulations/strategy2-replay-XXXXXX)
python3 tools/simulate-nylocas.py --strategy 2 --trials 500 --seed 233594316842202 \
  --offline --trace --starting-hitpoints 99 --prepot --utilities --server-boosts --output "$run_dir"
python3 tools/analyze-nylocas-training.py "$run_dir"
```
