# Nylocas training handoff — 25 November 2026

## Session of 26–27 November 2026

All **300/300** fresh packed-map rooms reached boss spawn. Every assigned post-cap big died in combat in every room. Both roles left the optional west-27 big untouched. One GPT-6.1-Sol agent at high reasoning worked on each role. [Role guidance](nylocas-training-guidance.md#pre-cap-movement-and-assigned-bigs-2627-november) · [Trace counts, replays and source hashes](research/nylocas/training-v2/session-2026-11-26/role-priorities.json)

**Pre-cap.** When a fresh split or lane spawn is one or two ticks away, the mage can hold a ready attack and step into position while an old, healthy small waits. The ranger generally takes a clear blowpipe shot if its two-tick cooldown will finish in time for the new target. The mage spent 252 ready ticks waiting for fresh targets across the 300 rooms, the ranger 25. Neither spent more than two between attacks. The mage cast Eye on west 8 at birth in 299 rooms and one tick later in the remaining room. It cast again on time in all 111 rooms where the parent still needed a hit three ticks after birth.

**Pathing.** Both roles pick a firing square and use ground clicks around the pillars during cooldown. They only click the nylo when they have line of sight and it is within one launch cycle's closing distance. The packed-map traces recorded 20,636 pre-cap cooldown moves with an actual mage step and 13,966 with a ranger step. Across all waves, zero of 56,644 NPC clicks had blocked sight or exceeded that distance.

**Post-cap mage.** Finish south 20, south 23 if it survives the ranger, west/south/east 24, west 25, west 26, south 29 and east 30. Keep attacking after a low roll. Protect a wounded parent from a Dinh that would make further mage hits impossible. All nine originals died in combat in every room. South 20 reached zero HP at a median age of six ticks, east 24 at eleven and west 26 at eight. The earlier 900-room policy let south 20 expire 66 times. The mage also has the scythe follow-up on a reachable south 20 after the wave-21 barrage. That branch passed its role regression and was used zero times in this fresh sample.

Two same-seed replays recovered bigs that had expired at 16 HP during training. In seed `264763169323360`, the mage hit south 20 with Eye at tick 158 and it reached zero HP at 162. In seed `135959225567683`, the mage scythed east 24 at tick 253 and it reached zero HP at 255.

**West 27 is optional.** Leave it to expire unless the team is very far ahead and every other nylo is already dead. A lethal hit in flight on another nylo still blocks it. Only the mage may take it while it is still in the west lane or just entering the room. The policy checks its position on every attempt and closes the window after age nine. The prior policy killed it in 697/900 rooms. The new policy recorded zero selections, damaging hits or combat kills in 300.

**Post-cap ranger.** Both original south-30 smalls received the same black chin three ticks after the first-green throw in **300/300**. The ranger moves west during that cooldown and gives the original west-30 big every attack it still needs, even after wave 31 begins. In **161/161** rooms where it still needed a shot after the chin, west 30 received the first shot and no other target intervened before lethal coverage. The previous policy went straight to the parent in 233/410 such rooms. West 30 died in combat in all 300.

To preserve the first-green throw at 55–65 HP, the ranger can defer a manta by one tick if it has prayer and queued damage through the next tick leaves at least 55 HP.

| Packed-map run | Wave-19 failures | Eligible mean | P5 | P95 | Eligible min–max | All rooms by 3:16.8 | All rooms by 3:22 |
|---|---:|---:|---:|---:|---:|---:|---:|
| Earlier 400 + 500 | 25/900 | 3:14.93 | 3:07.2 | 3:21.6 | 3:04.8–3:33.6 | 672/900 | 847/900 |
| Final 150 + 150 | 9/300 | 3:15.71 | 3:09.6 | 3:24.0 | 3:04.8–3:31.2 | 209/300 | 279/300 |

The final cohorts used independent seeds, random damage, 99 starting HP and finite supplies. There were zero player deaths or timeouts. The 291 rooms without a wave-19 stall supply the timing distribution. P95 was 7.2 seconds above the 3:16.8 goal, and 93% of all rooms finished by 3:22. All 507 role tests and 73 native bot integration tests passed. Python discovery ran 59 tests with four skipped. The frozen sources and full traces are in `.state/simulations/role-priorities-20261126/final-a-150` and `final-b-150` in the same directory.

## Session of 26 November 2026

### Packed-map P95 training, evening of 26 November

Jordan resumed training at 19:41 AWST. The independent P95 was **3:21.6**, 4.8 seconds above the **3:16.8** goal. The two independent batches finished 900/900 with 0 deaths and 0 timeouts. The earlier 3:21.6 reference used the lighter map fixture. The packed-map starting control is the direct comparison.

| Packed-map policy | Rooms | Eligible | Mean | P5 | P95 | Wave-19 failures | Boss by 3:16.8 | Pre-cap stalls per room |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Starting policy | 250 | 244 | 3:15.78 | 3:09.6 | 3:24.0 | 6 | 166/250 | 2.54 |
| Retained policy, same 250 seeds | 250 | 245 | 3:15.51 | 3:07.2 | 3:24.0 | 5 | 170/250 | 2.42 |
| Retained policy, fresh 400 + 500 | 900 | 875 | 3:14.93 | 3:07.2 | 3:21.6 | 25 | 672/900 | 2.35 |

P5 and P95 are nearest-rank percentiles among completed rooms with zero wave-19 stalls. The deadline and wave-19 columns count every room. The fresh batches separately measured 3:24.0 and 3:21.6 P95. On the 250 matched seeds, 105 rooms ended faster, 102 ended slower and 43 tied. All nine Java policy files and all 16 server-adapter snapshots in the fresh batches match the working tree. [Packed-room evidence](research/nylocas/training-v2/session-2026-11-26/packed-parent-timing.json) · [Per-room scores](research/nylocas/training-v2/session-2026-11-26/packed-parent-timing-trials.csv.gz)

The mage cast Eye on the original wave-8 west big at spawn in **900/900** rooms. It cast again on the next ready tick in **389/389** rooms where that big still needed damage. Travel time and normal damage rolls put its first zero-HP observation at a mean age of 5.44 ticks. The west-1 to east-2 ranger bows were exactly five ticks apart in 900/900. Neither role selected or hit the original wave-1 melee, and both spent at most two ready ticks waiting for a fresh spawn between attacks. On the matched seeds, the mage attacked the wave-6 west small within two ticks of spawn in 33/250 rooms, up from 17/250.

The mage's east-13 big died in combat in **900/900** and the ranger's east-14 big in **900/900**. Both roles track the two original east-15 smalls. Once north is covered and the ranger is close enough to pipe south, the mage can move into cardinal scythe reach during its attack cooldown. The ranger also moves east during cooldown. If clearing the smalls takes too long, each role gives itself time to finish its own parent. A small counts as covered at zero HP, on its death queue, or once a lethal hit is in flight. [Mage wave 13 notes](AdvancedNylos/MageInfo/13-melee-big.md) · [Ranger waves 13–15 notes](AdvancedNylos/RangeInfo/13-15-and-14-range-big.md)

| First attack on assigned parent | After both east-15 smalls covered | Within two ticks of coverage |
|---|---:|---:|
| Mage, east 13 | 846/900 | 681/900 |
| Ranger, east 14 | 864/900 | 849/900 |

The mage's cooldown movement increased east-13 openers on the first policy-ready tick after both smalls were covered from 135 to 152 in 200 matched rooms. We also tested sending the ranger to the southern east-15 small at the first possible blowpipe angle. That got the south shot out 1.56 ticks earlier when the mage opened north, but wave-16 admission retries rose from 76 to 93 and P95 went from 3:21.6 to 3:24.0. The retained ranger keeps its earlier eastward staging.

On the 250 matched seeds, the east-13 and east-14 splits landed within two ticks of each other in 109 rooms, up from 7. The number of rooms with at least 12 bodies on the east-14 split tick fell from 232 to 195. For the east-13 split it rose from 177 to 223. On the fresh seeds, **452/900** rooms had the two parents split within two ticks of each other. Both newborn squares were verified for every parent. These counts include ticks between wave-admission checks.

The ranger fired the scheduled south-30 black chin on the first green tick in **900/900** rooms. Both originals received the same attack three ticks later in **900/900**. Both took positive damage in 889 rooms and both died on impact in 464. The wave counter already showed 31 for 348 throws, including 5 on a pair inside the dim cutoff.

In seed `164046106452635`, the ranger reached the correct tile and equipped black chins, then ate a manta at 65 HP on the first green tick. Both originals reached melee untouched. On the corrected replay it threw at tick 265, ate at 266, and dealt 2 and 8 damage to the original pair at 268. Its boss time and final food count were identical. The seeded replay exercised the one-tick food deferral. Its fresh-room count was zero. The ranger keeps its normal food timing if health or queued damage make waiting unsafe. Both companion commands exercised the projectile and shared-impact regression.

Pre-cap delays still distinguish the slower rooms. Among eligible finishes, rooms past 3:16.8 averaged 3.35 pre-cap stalls, versus 1.91 in rooms at or under the deadline. The respective average times from wave 31 to boss spawn were 58.24 and 53.21 ticks. 269/900 rooms had at most one pre-cap stall.

Across the fresh rooms, the late original parents that died in combat were west 24 **899/900**, south 24 **900/900**, east 24 **899/900**, west 25 **900/900**, west 26 **895/900**, and south 29 **880/900**. The evidence lists each positive-HP expiry with its seed and actual primary ticks.

The retained ranger can blowpipe a single east-21 survivor after the chin and use the fifth blowpipe tile before wave 11 when time permits. The mage also prepares for west 26 and south 29. The final checks passed 442 role tests and all 66 native bot integration tests. Python discovery ran 58 tests with four skipped.

An earlier policy let the east-13 parent expire with 9 HP after two scythes in seed `144042924228274`. The corrected mage killed it in the same-seed replay. The intermediate `share-finish-240` checkpoint killed both assigned parents in 240/240 fresh rooms, with four wave-19 failures and a 3:21.6 P95. The final native traces and frozen sources are in `.state/simulations/p95-20261126/retained-matched-250`, `retained-fresh-400` and `retained-fresh-500` under the same directory.

### Live spectator correction for the south-30 chin

Jordan's 18:48 AWST recording of `::nylobot spectate` shows both original south crabs turning green on tick 306 and melee on 308. The ranger equipped black chins on 306. Its first throw went to a different crab on 309, when wave 31 began. Neither south crab took a hit from that throw.

An isolated server with the full plugin set and packed map reproduced the wait. From (3293, 4248), both south crabs are out of sight on the first green tick. The previous simulator and command tests instantiated the room without its static map objects. They now copy those objects from the same packed map used by the world server. The 100-room result in the earlier section used the old fixture.

The ranger now reaches (3291, 4248) before firing the west blowpipe and runs to (3293, 4249) during cooldown. From there it can aim at the east small and splash the west one. If the west pipe is still on cooldown when its ranged window closes, the ranger moves for the chin immediately. The companion tests check the spectator's projectile update and associate both hit events with the scheduled throw's impact cycle.

On packed-map seeds `202611261919..202611262018`, the ranger landed the west pipe on time and launched the south chin on the first green tick in **100/100** rooms. Three ticks later, both original crabs received the same attack in all 100. Both took positive damage in 98, and both died in 58. The counter already showed wave 31 at launch in 31 rooms. Boss spawn was **3:15.65 mean, 3:09.6 P5 and 3:24.0 P95**, with 100 completions and zero wave-19 failures. The local trace and policy snapshot are in `.state/simulations/ranger-south30-packed-staged-100-20261126`.

A second packed-map batch, seeds `152274597635335..152274597635359`, also threw on the first green tick and hit both originals in **25/25** rooms. In **14/14** independent spectator rooms using the full server plugin set, the ranger threw on that tick and both originals received the same black-chin attack. In one of those rooms the west pipe missed its opening and the south throw still went out on time. Both companion commands passed the projectile regression, along with all 61 native bot tests and 354 role tests.

### Focused ranger corrections after training

The ranger now keeps the Twisted bow equipped after the west-1 shot and runs east during its cooldown. Previously the pre-cap planner could choose the south-1 magic small before east 2 existed. It selected the Eye during that crossing in 446/500 checkpoint rooms and fired the east bow on its first ready tick in 394/500. With the corrections, the ranger crossed east with the bow and fired five ticks after west in **100/100 fresh rooms**. Both live-command regressions also check the worn weapon, movement and attack interval. [Trace counts and policy hashes](research/nylocas/training-v2/session-2026-11-26/ranger-opening-south30.json)

Jordan confirmed that the scheduled **south-30 black chin takes priority over the dim cutoff**. The ranger reserves its movement and cooldown for that throw, and can aim at either original south small. In recorded seed `152274597635335`, wave 30 spawned at tick 256, exactly 16 ticks after wave 28. Wave 31 activated the dim at 260. The old ranger waited for west 31 and both south crabs lived to expiry at full HP. Replaying the same seed with only the chin correction, it blowpiped west at 259, stepped toward the middle at 260 and threw on green at 261. One chin killed both crabs at 264. The boss spawned at tick 336 in both versions. [Matched replay](research/nylocas/training-v2/session-2026-11-26/ranger-opening-south30.json)

In the 100 fresh rooms, every south chin launched on the first green tick. Both hitsplats appeared three ticks later, after the pair had turned melee. **99/100** throws damaged both, and **52/100** killed both. The wave counter already showed 31 at launch in **31/100**. The cutoff caught the pair in one room, and the ranger landed the chin. The ranger releases its reservation when both targets already have lethal hits in flight. Tests through `::nylobot range` and `::nylobot spectate` verified that the observer received the projectile update and both crabs received the same black-chin attack. The final checks passed 352 role tests, 61 native bot integration tests and 20 black-chinchompa tests. Python discovery ran 57 tests with four skipped. [Ranger guidance](nylocas-training-guidance.md#ranger-opening-and-south-30-chin--26-november-2026)

The focused cohort completed 100/100 with zero deaths or timeouts. Boss spawn across its 99 eligible rooms was **3:14.30 mean, 3:07.2 P5 and 3:21.6 P95**. One room failed wave 19 and 97/100 reached the boss by 3:22. The 500-room benchmark below belongs to the earlier policy. Regular training remains paused. [Focused cohort evidence](research/nylocas/training-v2/session-2026-11-26/ranger-opening-south30.json)

### Final checkpoint at the three-hour limit

The mage and ranger policies measured at the three-hour limit are preserved in `.state/simulations/training-v5-20261126-retained-freshness-and-parent-recovery`. Two independent randomized native cohorts, `final-east24-west18-200` and `final-east24-west18-300`, finished **500/500 rooms with zero deaths or timeouts**. The reported boss time was **3:13.80 mean, 3:07.2 P5 and 3:21.6 P95** across 491 eligible rooms. Jordan accepted the wave-19 failure rate of **9/500 (1.8%)**. **484/500 (96.8%)** reached the boss by 3:22. The eligible mean is 4.80 seconds above the 3:09 target. [Final timing, fixture and policy hashes](research/nylocas/training-v2/session-2026-11-26/freshness-and-parent-recovery.json)

Both roles avoided the original wave-1 melee in every room, including approaches, primaries, collateral rolls and positive player or thrall damage. Each spent at most two explicit ready ticks waiting between confirmed attacks. The mage cast on the original wave-6 west magic small on its birth tick in **73/500**, up from **14/500** in the control. For all **6,368** forecast dying parents, the removal tick and both newborn squares were correct. The mage can also immediately select a reachable newborn instead of chasing an unhit older melee. [Pre-cap guidance](nylocas-training-guidance.md) · [Mage evidence](research/nylocas/training-v2/mage-final-evidence-2026-11-26.md)

The final policy retains the ranger's low-population west-18 Eye follow-up, its movement toward reachable splits during cooldown, and the mage's recovery on a previously hit east-24 parent. All required 23-south, 24, 25-west and 26-west parents died in combat in every room. The three wave-10 chins, wave-11 barrage, both wave-12 attacks, wave-21 barrage, wave-22 Dinh and wave-30 south chin all fired in 500/500. The wave-21 north follow-up fired in 461, the wave-26 Dinh on 28 in 486, and the wave-24 west blowpipe in 499. [Ranger packet evidence](research/nylocas/training-v2/ranger-handoff-2026-11-26.md)

All 500 admitted wave 21 on time, while **384/500** had all required 18/19 and west/south 20 parents physically dead beforehand. West 20 was the most frequent uncovered survivor, in 76 rooms. Only **189/500** had at most one pre-cap stall, **296/500** had no stalls across 21–27, **295/500** had two or three on 28, and **226/500** finished within 3:07–3:12. These are the remaining consistency targets for a future session. [Session comparisons and per-room evidence](research/nylocas/training-v2/session-2026-11-26/README.md)

The one omitted west-24 blowpipe followed a real food action in seed `232776541059990`. Food still blocked the age-three shot, and the ranger launched Eye at age five. All nine west-18 age-five Eye rooms passed wave 19. No wave-31 ranger remained alive when it permanently changed to melee. The checkpoint passed 349 role tests and all 61 native integration tests. Python discovery ran 55 tests, with four skipped. The session ledger covers 113 native cohorts and 24,700 distinct seeds. Training stopped within the agreed window. [Cohort ledger](research/nylocas/training-v2/session-2026-11-26/results.json)

### Checkpoint history

Jordan resumed training at 08:16 AWST with a three-hour limit. One GPT-6.1-Sol agent with high reasoning works on each role. The native fixture now casts the initial thralls inside the arena while movement continues. With the retained Java policy this entry fixture completed 500/500 rooms. Its reported mean was 3:14.60, with P5 of 3:07.2 and P95 of 3:21.6. There were 47 wave-19 failures and 465/500 rooms reached the boss by 3:22.

A native cooperation regression reproduced a ready ranger standing next to an uncovered melee split while the mage chased it from five tiles away. The controller now reserves only targets that could enter the mage's weapon range in that cycle. An independent 200+300 control on this controller completed 500/500, with 467 eligible, mean 3:14.61, P5 3:09.6, P95 3:21.6, 33 wave-19 failures and 459/500 within 3:22. These controls have separate adapter fingerprints.

Jordan added pre-cap guidance during the session. Never hit the original wave-1 melee. Accept a one- or two-tick wait for a new lane crab or split when the alternative is four waves old. The example is the wave-6 west magic small. The current-controller 500-room control selected or approached the wave-1 melee on 1,407 mage decision ticks in 456 rooms, with 23 confirmed primaries in 23 rooms. Six were within two ticks of West 6. Both roles have a focused regression. See [manual guidance](nylocas-training-guidance.md) and the [per-policy cohort ledger](research/nylocas/training-v2/session-2026-11-26/results.json).

Both roles now leave the original wave-1 melee alone. The first 500-room validation recorded zero approaches, primaries, secondary rolls and positive thrall or player hits on that lifetime. The mage launched its first cast on West 6 on the spawn tick in 68/500 rooms, up from 14/500 with the same controller and prior policy. The reported mean for the wave-1 change alone was 3:14.84 with P5 of 3:09.6 and P95 of 3:24.0, 455 eligible rooms, 45 wave-19 failures and 451/500 by 3:22.

The next retained change lets the ranger clear an uncovered small during actual wave 19, ticks +5 through +8. It requires crowded conditions and enough remaining time to service the unopened wave-18 parents. A confirmed parent survivor keeps its follow-up. The two independent cohorts completed 500/500 with 494 eligible, a reported mean of **3:14.37**, **P5 3:07.2 and P95 3:21.6**, six wave-19 failures, **480/500 (96.0%)** by 3:22 and zero deaths or timeouts. The local checkpoint is `.state/simulations/training-v5-20261126-retained-wave1-cap19`.

The next retained checkpoint extends the one- or two-tick freshness preference to both roles. The allowance follows the player across target and wave changes and resets on an actual attack. The policy uses an upcoming assigned lane spawn or an observed dying parent, and can move during cooldown. In cleanup it can service a younger reachable magic small while an older ordinary small nears expiry. It also recovers several parent and packet follow-ups. The two fresh cohorts, `newer-cleanup-200` and `newer-cleanup-300`, completed **500/500** with **488 eligible, 12 wave-19 failures, a 3:13.71 reported mean, P5 of 3:07.2, P95 of 3:21.6 and 481/500 (96.2%) by 3:22**. No room ended in death or timeout. The local checkpoint is `.state/simulations/training-v5-20261126-retained-dual-fresh-cleanup`.

The trace recorded zero wave-1 selections, approaches, primaries or damaging impacts across these 500 rooms. Both roles stayed within two explicit ready-tick waits between confirmed attacks. All three wave-10 chins, the wave-11 barrage, both wave-12 attacks, the wave-21 barrage and the wave-22 Dinh fired in every room. All specified 18/19 parents and West/South 20 were physically clear before 21 in 359/500. The most frequent surviving parent was West 20, uncovered in 96 rooms. There were 193/500 rooms with at most one pre-cap stall, 301/500 with clear admissions on 21–27 and 285/500 with two or three stalls on 28. See the [mage evidence](research/nylocas/training-v2/mage-role-evidence-2026-11-26-0156.md) and [ranger evidence](research/nylocas/training-v2/ranger-role-2026-11-26.md).

The next checkpoint adds mage movement toward West 6 during the final cooldown, with follow-through on a reachable spawn. The ranger releases an unhit approach for an equally early fresh split, keeps its West 11 rescue available after wave 12 and leaves enough cooldown after West 23 for the West 24 blowpipe. The mage can cover two early shared magic lanes and help on a local small during a failed wave-10 admission. In `fixed24-early-help-200` and `fixed24-early-help-300`, all **500/500** finished, with **488 eligible, 12 wave-19 failures, a 3:13.45 reported mean, P5 of 3:07.2, P95 of 3:21.6 and 483/500 (96.6%) by 3:22**. The mage cast on West 6 at birth in 70 rooms. Both roles recorded zero original wave-1 melee selections and stayed within the two-ready-tick allowance. All 500 fired the required wave-10 through wave-12 packets, the wave-21 barrage, wave-22 Dinh and West 24 pipe. West/South 20 and all 18/19 parents were physically clear at 21 in 355/500. The local checkpoint is `.state/simulations/training-v5-20261126-retained-west6-early-help`.

The next retained checkpoint lets the mage launch a ready cast on a fresh split when chasing an unhit melee would lose the attack. It also tracks the moving northern melee after the wave-21 barrage and lets the ranger move toward West 9 during cooldown while preserving its next shot. In two independent cohorts, **500/500 rooms finished, 490 were eligible, ten failed wave 19, and 481/500 (96.2%) finished by 3:22**. The reported average was **3:13.60**, with P5 of **3:07.2** and P95 of **3:21.6**. Both roles left the original wave-1 melee untouched and spent at most two explicit ready ticks waiting between attacks. The mage launched on West 6 at birth in 74 rooms and landed the wave-21 northern melee follow-up in 459. All required transition parents were physically clear at 21 in 357 rooms. One room stalled while admitting wave 21. The checkpoint is `.state/simulations/training-v5-20261126-retained-guided-fresh-approach`. [Retained action and timing counts](research/nylocas/training-v2/session-2026-11-26/guided-fresh-approach.json)

The next retained checkpoint reads a dying parent's visible death animation before spending a ready tick waiting for a split. In 500 fresh rooms, all 6,340 forecasts matched the tick the parent disappeared and the squares where its two children appeared. Every role stayed within two ready ticks and left the wave-1 melee untouched. The mage cast on the wave-6 west small at birth in 81 rooms. All 500 finished, with **495 eligible, five wave-19 failures, a 3:13.75 reported mean, P5 of 3:07.2, P95 of 3:21.6 and 480/500 (96.0%) by 3:22**. Throughout this session, 3:22 reliability includes every trial. The checkpoint is `.state/simulations/training-v5-20261126-retained-observed-split-birth`. [Split forecasts and action counts](research/nylocas/training-v2/session-2026-11-26/observed-split-birth.json)

The following stop state and experiments describe 25 November. The manual guidance and its timestamp cutoffs still apply.

## Stop state and reading order

Jordan explicitly asked to wrap up, finish the current experiment and leave this handoff. The final native200 cohort completed, and no further training batch was started. Training is paused at Jordan’s request. Do not automatically resume training from this document; wait for Jordan’s explicit resume.

Read first: [manual guidance](nylocas-training-guidance.md), this handoff, [original goals](../nylo-training.md), [mage loadout/strategy](../mage-strategy.md), [ranger loadout/strategy](../range-strategy.md), and [review questions](nylocas-performance-review-questions.md). The longer [resume history](nylocas-training-resume.md) contains historical checkpoints and stale “running/current” statements: inspect source and use this handoff as the latest stop state.

Preserve all uncommitted work in `/home/jordan/osrs` and its `upstream/rsmod` repository. Do not reset, clean, checkout old full candidates over current code, or remove guidance entries. No commits, pushes or deployment were performed during training; the subsequent local checkpoint is recorded below. User opened `code .` during wrapup; account for possible concurrent user edits by checking hashes before any replacement.

## Subsequent local commit checkpoint

After pausing training, Jordan explicitly requested committing the current work. The server implementation is committed as `21cccf08de8ccaab52e08f1423460393d9f5ca8f` on rsmod branch `nylocas-training-v2`; the parent branch of the same name records that submodule reference together with the training tools, tests, guidance and portable evidence. These are local checkpoint commits; nothing was pushed. Training remains paused and no experimental candidate was promoted.

The ignored `.state/` directory remains preserved locally, including full traces, candidate snapshots and wrap checkpoints. Portable evidence under `docs/research/nylocas/training-v2/` is included in the parent commit; use it when local runtime artifacts are unavailable.

## Canonical source and benchmark

The nine canonical Java files under `upstream/rsmod/api/nylocas-agents/src/main/java` match `.state/simulations/training-v4-retained-timestamp-combined-checkpoint/policy-source`. This was verified again during wrapup. The checkpoint contains `retention.json`, the complete preceding nine-file source, both repository diffs/status, a guidance snapshot and the exact strategy patch. Only MageStrategy and RangerStrategy differ from the preceding provisional-claim canonical checkpoint. No later isolated candidate was promoted.

Retained behavior includes the clarified combined timestamp dim, both-role partner-NULL rescue and literal-last cooperation, normal idle cleanup helpers, Mage West30 parent recovery and South29 opening during actual29. It preserves all earlier retained corrections. Native provisional claims use `provisionalClaimUntil` separately from real `pendingDamage`; do not restore the adapter that overwrote damage with -1 during same-cycle Mage planning. Canonical native controller remains the validated separated-claim version.

Retained validation: identical source, adapter and fixture across fresh independent 200+300 cohorts, 500/500 complete, no deaths/timeouts. Reported average **3:15.252980**, minimum **3:04.8**, maximum **3:28.8**, 453 eligible and 47 entire wave19-failure rooms excluded. **452/500 (90.4%)** of all rooms meet3:22. Raw all-completion average3:16.176, min3:04.8/max3:40.8; rawP95 3:26.4. Goals remain unmet.

## Instructions and acceptance rules on 25 November

- Keep normal combat rules, random damage, finite supplies,99HP and the specified loadouts. No cap, expiry, stats, damage or resource grants.
- Run exactly one Gradle job at a time. Root coordinates native jobs; the requested roles are one Ranger agent and one Mage agent.
- Fresh independently randomized seeds for every new batch: omit `--seed`. Historical reproducible traces may be inspected, but do not use static benchmark seeds. Ignore small mean changes as a strategy rejection criterion; verify concrete behavior, matched fixtures and independent validation.
- Report average **and minimum/maximum** boss time. Any last-spawned wave19 stall is a policy error. Exclude the entire affected room only from the reported average, show its count separately, and keep it in reliability/death/timeout denominators.
- Goals:3:09 reported mean;95% of **all** rooms by3:22; zero wave19 failures; original pre-cap, transition, packet and cleanup consistency. Retained500 has48 late rooms:27 with wave19 failure and21 without. Eligible-only432/453=95.36% is context, never the reliability headline.
- **Dim is a birth-time cutoff**, not a whole-wave label. `29 dim` means born≤actualspawn29. `28+16` means born≤actualspawn28+16. Both use the later timestamp, inclusive, and activate only at actual31. Jordan explicitly confirmed that a child born252after wave29spawn240 remains fresh. Later29 splits participate in selection, NULL rescue and helpers. Literal-last counts every positiveHP/non-dying body, including dimmed bodies. Unknown29 timestamp conservatively falls back to wave≤29.
- Do **not** use the 3:02.4 always-max-hit litmus for 28+16 or comparisons involving it. Earlier max-hit and whole-wave29-label experiments remain historical; they do not validate the clarified timestamp strategy.
- Focus guidance: unexplained refusal to hit/approach living legal fresh targets, newer nylos, partnerNULL rescue, final-target doubling, required parents, South30 chin and31 rangers before melee. A zero roll is not NULL; proven queued lethal coverage is not an abandoned target.

## Completed comparisons

Times below use only completed rooms with known zero wave19 stalls. Every row completed without deaths/timeouts. All-time reliability includes excluded rooms.

| Policy | Completed | Eligible /19 excluded | Average | Min | Max | All rooms≤3:22 |
|---|---:|---:|---:|---:|---:|---:|
| RETAINED timestamp combined | 500/500 | 453/47 | 3:15.25 | 3:04.80 | 3:28.80 | 452/500 |
| Timestamp29-only control | 200/200 | 182/18 | 3:15.35 | 3:07.20 | 3:26.40 | 179/200 |
| Mage ready Magic before chase; NOT retained | 500/500 | 461/39 | 3:14.96 | 3:04.80 | 3:31.20 | 451/500 |
| Late24East recovery; NOT retained | 200/200 | 186/14 | 3:15.15 | 3:04.80 | 3:28.80 | 182/200 |
| Optional23East PIPE; NOT retained | 200/200 | 182/18 | 3:15.52 | 3:04.80 | 3:26.40 | 179/200 |
| Ranger actual30 NULL rescue; NOT retained | 200/200 | 179/21 | 3:15.71 | 3:04.80 | 3:33.60 | 171/200 |

Raw sources/reports are under `.state/simulations/` with the names below; pooled summaries include constituent run names and seed ranges. Portable completed metrics/audits and checksums are in `docs/research/nylocas/training-v2/cleanup-session-2026-11-25/manifest.json`.

## Final experiment: mixed-style cleanup cleave safety

Source: `.state/simulations/training-v4-candidate-mage-cleanup-mixed-cleave-safety/policy-source`; based on retained350, **without** the ready-Magic V1 or other pending deltas. Only Mage’s ordinary actual31 small-cluster weapon choice changes. Normal Sulphur replaces Scythe when a useful fresh nonmelee small could walk into next-cycle cleave. Exact native arc source and bounded sibling-NULL witnesses justify this experiment; a conservative current-distance≤3 screen accounts for primary/secondary walking plus the arc. OwnNULL, known-lethal, dimmed/dying/zeroHP/expired targets do not veto. Named12, all bigs and mandatory packets remain unchanged.363 pure checks pass; unchanged baseline has five focused red checks.

Final normal cohort: `training-v4-mage-cleanup-mixed-cleave-safety-random-200`, seeds151137184917001–151137184917200. **200/200 complete, no deaths/timeouts; mean3:15.645405, minimum3:04.8, maximum3:31.2;185eligible/15wave19 exclusions;178/200 (89%)all-room reliability.** This does not meet the goals. Small mean differences do not by themselves invalidate the idea. The candidate remains **unretained**, needs behavioral review and fresh validation before any promotion. Final audit: fresh MageNULL5lifetimes, four before31outside the candidate; the only actual31 case already had8HP covered by22queued damage and intentionally did not veto cleave. No unprotected useful fresh post31NULL was observed. Small-Scythe launches fall19/200 versus102/500 retained; Sulphur876/200 versus2130/500. Fresh-small expiries rise82/200 versus161/500. This is a possible safety/clearance tradeoff under independent seeds, requiring fresh validation rather than rejection on the small mean difference. Both12packets,21barrage and22Dinh occur200/200; all required23–25parents die,26West198dead/2MageNULL before31. Timestamp dim and ownNULL-primary violations: zero. See per-run `mage-cleanup-chase-audit.json`, `mage-fresh-own-null-classification.json`, `cleanup-dim-audit.json` and guidance/training audits.

## Candidates and concrete findings for resumption

All source paths in this list are under `.state/simulations/`. Do not copy full candidate trees over canonical: they have distinct bases and deliberately isolated changes.

1. **Mage ready Magic before moving-melee chase:** `training-v4-candidate-mage-ready-magic-before-melee-chase/policy-source`. Based on canonical timestamp350, only Mage differs. Generic actual31 nonadjacent Sulphur split chase yields to an already-ready, in-range, unobstructed ordinary Eye. Named packets, required parents, rescue, adjacent melee and ScytheAoE keep priority.364 pure checks pass; full native49/49 passes, including five exact-map ray rows. Normal200+fresh300 validate500 rooms (`training-v4-mage-ready-magic-pooled-500`). It remains **unretained**, awaiting final behavior review/possible composition. A separate unmeasured margin variant `training-v4-candidate-mage-ready-magic-before-melee-chase-margin` uses Eye range minus one walking step;366 checks. Do not assume it is the measured V1.

   In V1’s first200, Eye attacks rise5.670/room versus4.716 in retained500, no-launch split chase screens fall.750 versus1.044, fresh-small expiries.230 versus.322. These are independent-cohort behavioral observations, not paired causal estimates. All14 fresh MageNULL lifetimes in V1 follow Scythe on another primary one tick earlier, with none following Eye replacement; mixed cleave motivated the final experiment.

2. **Required late24East recovery:** `training-v4-candidate-mage-late24east-recovery/policy-source`; canonical350 base, Mage-only,361 checks. Exact historical failure68515107335293 at243/248 selects25West retry while adjacent unhit24East16HP ages39/44. The candidate gives required24East melee recovery after named age20 window priority over generic fillers, retaining required26Magic and mandatory work. Native200 has44late launches in39 rooms; all required23–26parents die200/200. One missing22Dinh still needs classification; pre31 source changes exist here, so review instead of assuming noise. No fresh validation yet.

3. **Optional23East spare PIPE:** `training-v4-candidate-timestamp-combined-optional-east23-pipe/policy-source`; canonical350 base, RangerStrategy-only fallback when ordinary candidate set empty during27/28, no PreCapPlanner/Mage delta. Seven focused checks pass. Native200:68launches in67rooms,38combat removals of23East versus1/500 retained,216/400 fresh children versus660/1000 retained. No proved abandoned mandatory target: missing24PIPE follows finite food; missing30chins are dimmed; two undimmed31rangers atage26 have queued lethal damage, a third is dimmed. Physical deadline misses remain visible. Needs fresh validation. Evidence: `ranger-east23-spare-pipe.md`, source-pinned human recordings and per-run lineage/packet JSON.

4. **Ranger actual30 fresh NULL rescue:** `training-v4-ranger-timestamp-combined-null-rescue-at30/policy-source`; canonical350 base, Ranger-only. Enable fresh-cutoff rescue during30, preserve packet budgets and release stale ordinary intent. Native200 shows one actual30 rescue plus eight during31; all eight ready/nonlethal-screen lifetimes selected, two skipped lifetimes already queued lethal. Zero unsafe primaries; six simultaneous double-ups all literal-last. Opening/24PIPE200; no31physical-melee survivor; one30chin intentionally dimmed. Needs fresh validation. Use `RangerPre31NullRescueCombinedTest`; the other29-only witness class has different cutoff assumptions and is not a permanent expectation for combined dims.

5. **Pre-cap fresh unhit intent:** `training-v4-ranger-precap-fresh-intent-timestamp/policy-source`. Frozen at wrapup; read [Ranger handoff](research/nylocas/training-v2/ranger-timestamp-wrapup.md) and candidate metadata. Focused4 baseline1red→candidate4green; guidance/replay18pass; broad127has only three other previously pending failures. The beam currently filters to a committed unhit older small even when a newer same-style split has an equally early attack. Candidate releases only that approach; confirmed damaged-survivor followups, previously attacked targets, bigs and named priorities remain protected. No native trial has been run. Do not mistake a snapshot/probe for full native replay.

6. **EarlyWest18Eye5:** historical `training-v4-candidate-parent-lifecycle-early18-eye` used the old whole-wave post31 dim; only PreCapPlanner eligibility changed. Ten focused checks plus349base pass359. Native200:45WestEye age5 shots versus0/500 lifecycle control, no proved remaining age5 idle refusal;19wave19failures remain. West parent combat-death age10.73 versus11.118 historical500, not a causal speed claim. Rebase only the recorded eligibility delta onto the inspected current strategy and run fresh normal trials; never restore that old full source. Other pre-cap failures remain.

7. **Potential pre-cap Mage chase issue:** read-only screen has85 actionable no-launch split-Sulphur frames with nearby healthy own-Magic splits in retained combined200, especially17/10/9/19. These require exactLOS, policy eligibility and budget reproduction; no pre31Mage candidate exists. Example191996532081087/t68 actual10. Script/data: `precap_split_chase_screen.py` in `training-v3-mage-guidance-checks`, and `mage-precap-split-chase-screen.json` in the retained combined200 run. Treat as leads, not85 proven missed attacks.

## Audits, tests and commands

Vetted primary evidence: five pairs,46 eligible Mage rooms and42 Ranger rooms (overlapping views, not88 independent rooms). `python3 tools/research/nylocas_duo_vetting.py` verifies pinned source hashes and eligible set. Human recording notes distinguish observed attacks from native recovery extrapolations.

Normal native cohort command (needs the existing approved local Gradle socket escalation in the managed sandbox):

```sh
python3 tools/simulate-nylocas.py --trials 200 --offline --trace   --starting-hitpoints 99 --prepot --utilities --server-boosts   --policy-source .state/simulations/CANDIDATE/policy-source   --output .state/simulations/NEW-RUN
```

Serialize Gradle. Use fresh random seeds by omitting `--seed`. Runner records nine policy hashes, native adapter hashes, fixture, seed range, finite resource profile and timing summaries. Compare these before pooling; require same source/adapter/fixture and disjoint seeds. Do not pool across the old adapter that overwrote pending damage.

Timestamp-aware cleanup audit:

```sh
python3 tools/research/nylocas_cleanup_dim_audit.py .state/simulations/RUN   --timestamp-wave29 --wave28-offset 16
python3 tools/research/nylocas_cleanup_floor_audit.py .state/simulations/RUN   --wave29-timestamp --wave28-offset 16
python3 tools/analyze-nylocas-training.py .state/simulations/RUN
python3 tools/research/nylocas_mage_guidance_audit.py .state/simulations/RUN
python3 tools/research/nylocas_mage_cleanup_chase_audit.py --help
python3 tools/research/nylocas_ranger_cleanup_rescue_audit.py --help
```

Omit only the28offset flag for timestamp29-only. Defaults without timestamp flags deliberately preserve historical whole-wave audits. `old_cleanup_attacks` in the legacy overall audit is merely birth-label≤29 and legitimately nonzero; use the timestamp audit for dim violations. Additional-time-dim counters now compare the actual baseline predicate, including late29 births between anchors.

Current role/native checks: retained350 filtered checks; MageV1 364 and native49; mixed-cleave363. The filtered base excludes four separately pending behaviors, not failures silently waived as goals: savedWest12through17, West11continuation, late18permanentMagic and pre-cap fresh intent (the new pending candidate resolves the latter). Current native real-map checks live in `NylocasMageCleanupRayTest.kt`; five rows pass. Final wrap Python suite:55tests, zero failures, four skipped (`.state/simulations/training-v4-wrap-python-checks.log`). Candidate-local suites preserve historical assumptions; do not mix test/source trees arbitrarily.

See the [Ranger stop-state notes](research/nylocas/training-v2/ranger-timestamp-wrapup.md) and [Mage stop-state research](research/nylocas/training-v2/mage-cleanup-floors-and-pre31-service.md) for exact candidate hashes, classified exceptions and source-pinned evidence. Final preservation snapshot: `.state/simulations/training-v4-wrap-2026-11-25/state.json`, both repository diffs/status, canonical9source and guidance/handoff copies. Both role agents stopped; no native or audit job remains.

No fresh cohort, test or handoff update means the training goals are achieved. Resume only on explicit instruction, then continue investigation and native validation until goals or another explicit stop.
