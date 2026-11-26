# Nylocas duo training, 26 November 2026

The overnight session finished all 300 fresh packed-map rooms. Every assigned post-cap big died in combat, west 27 took zero damage, and the ranger followed the south-30 chin with west 30 in all 161 rooms that needed another shot. The eligible mean was **3:15.71** and P95 was **3:24.0**. [Overnight handoff](../../../../nylocas-training-handoff-2026-11-25.md#session-of-2627-november-2026) · [Role priorities and replay evidence](role-priorities.json)

The evening session used the packed map and one GPT-6.2-Sol agent at high reasoning for each role. Across 900 fresh rooms, boss spawn was **3:14.93 mean, 3:07.2 P5 and 3:21.6 P95**. Both assigned east parents died in every room. The scorecard includes the same-seed comparison, split timing and south-30 projectile results. [Evening scorecard](packed-parent-timing.json) · [Evening per-room scores](packed-parent-timing-trials.csv.gz)

## Morning session

Jordan requested a three-hour continuation with one GPT-6.1-Sol agent at high reasoning for each role. The endpoint is the native boss spawn after waves and cleanup. Each seed uses ordinary randomized damage, 99 starting HP, finite supplies, the regular loadouts, prepot, native utilities and server boosts. A tick lasts 0.6 seconds. The cohort ledger identifies each policy, adapter and fixture by hash. [Cohort ledger](results.json) · [Per-room data](trials.csv.gz)

## Retained checkpoints

Each row pools an independent 200-room run and a fresh 300-room run with identical code and fixture. All rooms completed. The mean and nearest-rank P5 and P95 use the individual times of rooms with zero last-spawned wave-19 stalls. The 3:22 column includes all 500 rooms.

| Checkpoint | Eligible | Failed wave 19 | Mean | P5 | P95 | By 3:22 |
|---|---:|---:|---:|---:|---:|---:|
| Control after the native claim correction | 467 | 33 | 3:14.61 | 3:09.6 | 3:21.6 | 459 |
| Wave-1 exclusion and wave-19 small clearance | 494 | 6 | 3:14.37 | 3:07.2 | 3:21.6 | 480 |
| Fresh splits and cleanup support | 488 | 12 | 3:13.71 | 3:07.2 | 3:21.6 | 481 |
| West-6 preparation and early shared lanes | 488 | 12 | 3:13.45 | 3:07.2 | 3:21.6 | 483 |
| Ready mage attacks and ranger cooldown movement | 490 | 10 | 3:13.60 | 3:07.2 | 3:21.6 | 481 |
| Observed split birth on the native death clock | 495 | 5 | 3:13.75 | 3:07.2 | 3:21.6 | 480 |
| Final mage parent recovery and ranger west-18 Eye | 491 | 9 | 3:13.80 | 3:07.2 | 3:21.6 | 484 |

The lookup for each measured policy includes its full nine-file hash map and a patch against the previous day's canonical Java sources. Adapter hashes identify changes to native entry, same-cycle partnership and isolated observations. [Policy patches](policies) · [Original handoff](../../../../nylocas-training-handoff-2026-11-25.md)

The final pair met the all-room 3:22 reliability target at 96.8%. Jordan accepted the 9/500 wave-19 failure rate. The eligible mean was 4.80 seconds above 3:09. Both roles avoided the original melee and stayed within two ready ticks between attacks when waiting for freshness. The mage cast on west 6 at birth 73 times. All 6,368 observed parent removals matched the predicted tick and both child squares. [Final action and timing report](freshness-and-parent-recovery.json)

## Pre-cap decisions

The final sources passed all 349 repository role tests and all 61 native integration tests. The Python suite ran 55 tests with four skips. The ledger contains 113 completed native cohorts with 24,700 distinct randomized seeds. Each constituent run has a source hash, adapter hash, fixture and seed interval. [Native per-room data](trials.csv.gz)

The ranger followed the six-action opening in 499/500 final rooms. In seed `232776541060003`, the mage had lethal damage in flight on west 4 while the ranger finished west 5. West 4 reached zero HP on tick 28. [Opening and packet exceptions](../ranger-handoff-2026-11-26.md)

The control recorded 1,407 mage targeting or approach frames and 23 confirmed primaries on the original wave-1 melee. The ready-attack checkpoint recorded zero selections, primaries, collateral rolls or positive damage on that lifetime. West-6 birth-tick mage casts rose from 14 to 74 in 500 rooms. Each role stayed within two explicit ready-tick waits between native primaries. A fresh split can also win immediately when the competing melee requires a diagonal chase. In the first 200-room isolated test, the mage actually cast on all 66 occasions matching the replacement. [Action counts](guided-fresh-approach.json) · [Mage investigation](../mage-role-evidence-2026-11-26-0156.md)

The server reports the remaining death-animation delay after it becomes visible. Using the following player decision tick as the birth tick matched all 6,340 parent removals and both child squares in the observed-split checkpoint. Its two role policies recorded zero wave-1 melee selections, at most two explicit ready ticks and 81 West-6 birth-tick casts. A 200-room control also compiled and ran the historical Java policy on this controller. [Verified split outcomes](observed-split-birth.json) · [Controller change](observed-split-controller.patch)

The ranger can move west on the final tick of cooldown while still covering its next small. In the first combined 200-room cohort all 128 such moves were followed by a real attack on the next tick. The late ranger checks track first West-24 blowpipe, South-30 chin and whether any wave-31 ranger survives to its permanent melee phase. [Ranger investigation](../ranger-role-2026-11-26.md)

The human reference set contains five vetted pairs, 46 eligible mage views and 42 ranger views with some rooms recorded from both roles. One recording can supply multiple observations of the same action. The normal cohorts use independent seeds, so changes in the aggregate mean can reflect both strategy and sampling. The focused replay checks and native launch records identify which decisions actually changed. [Human reference manifest](../../vetting/summary.json)

## Transition and cleanup comparisons

The `split-transition-dinh-200` and `-300` pair cleared all required wave-18 and wave-19 parents and west/south 20 before wave 21 in 397/500 rooms. The late wave-26 Dinh fired in 484, West 24 received the blowpipe in all 500, and South 30 received the chin in 499. The remaining South 30 was born exactly at the dim cutoff. All 500 finished, 491 entered the reported average of 3:13.37 (P5 3:07.2, P95 3:19.2), and 487 finished by 3:22. Nine rooms failed the wave-19 admission rule. [Matched cohort and packet audits](../ranger-role-2026-11-26/final-source-matched-pair-audits.json)

The `west18-close-eye-200` and `-300` pair added a west-18 Eye attack when the room had enough space for the next lane and a split. The ranger fired this attack at age five in 25 rooms. Two were first attacks on full-health parents and 23 followed earlier ranged attacks. All 25 passed wave 19. Across the full 500, 15 failed wave 19, 477 finished by 3:22, and the reported mean was 3:14.15 (P5 3:07.2, P95 3:21.6). All transition parents were physically dead in 412 rooms and all 500 admitted wave 21 on time. West 24 received the blowpipe in every room. All three omitted South-30 chins were on the inclusive dim boundary. [Ranger trace evidence](../ranger-handoff-2026-11-26.md)

For late mage recovery, seed `101764145237603` exposed an already-hit east-24 parent at 5 HP. At tick 241 a normal Scythe could reach either that parent or the newer 16-HP west-25 parent. The mage chose west 25 and east 24 expired. A focused replay favors the wounded east parent only when both attacks fit the same ready tick and the earlier hit is confirmed. The regression also covers lethal damage already in flight. With this change alone added to the transition-Dinh policy, all required 23-south, 24, 25-west and 26-west parents ended combat-dead in the next 500 rooms. There were 12 wave-19 failures, a 3:14.22 reported mean (P5 3:07.2, P95 3:21.6) and 472/500 rooms by 3:22. These cohorts used fresh independent seeds. [Mage investigation and replay](../mage-final-evidence-2026-11-26.md)
