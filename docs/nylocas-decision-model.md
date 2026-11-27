# Nylocas decision model

The ranger and mage each plan 16 ticks ahead. A role first chooses a task, such as executing a timed attack, finishing an assigned parent, clearing for the next wave, or helping the partner. The task supplies legal attacks and approaches. When there are alternatives, the planner compares up to eight first actions across six sampled futures each. The bot executes the first input and replans from the next observation. [Shared planner](../upstream/rsmod/api/nylocas-agents/src/main/java/org/localscape/nylocas/RoomPlanner.java)

The ranger retains its opening, three-lane chin sequence, transition assignments and wave-30 west-to-south movement. It can spend a shot clearing a small when splitting an unopened parent would block admission. The mage reserves time and position for sequences such as the east-12 scythe into the west-12 Dinh. A confirmed launch anchors the follow-up. An injured parent, a lethal projectile or an overdue partner assignment can change the next choice. [Ranger strategy](../upstream/rsmod/api/nylocas-agents/src/main/java/org/localscape/nylocas/ranger/RangerStrategy.java) · [Mage strategy](../upstream/rsmod/api/nylocas-agents/src/main/java/org/localscape/nylocas/mage/MageStrategy.java)

Each forecast advances cooldowns, running, lane travel, flickers, projectile impacts, deaths, two-child splits and natural expiry. The next wave checks occupied bodies, including dying ones. A failed check delays the whole wave four ticks and shifts its scheduled attacks. Utility rewards wave progress, damage and early cleanup, and charges for remaining living work, stalls and missed attack windows. Each alternative sees the same set of sampled damage rolls and split styles. [Room projection](../upstream/rsmod/api/nylocas-agents/src/main/java/org/localscape/nylocas/RoomProjection.java)

The partner estimate uses the known wave assignments, observed position, cooldown, individual NULL states and confirmed flights. Each player chooses independently. With the live queued-damage feature enabled, the controller supplies damage already rolled on native attacks and the ticks until impact. Future hits and child styles are sampled locally. The forecast approximates damage, area coverage, movement after lane entry and partner choices. Native combat, collision, supplies and survival determine the measured results. [Controller](../upstream/rsmod/content/other/nylocas-bots/src/main/kotlin/org/rsmod/content/other/nylocas/bots/NylocasBotController.kt)

## Paired native evaluation

The final implementation is RSMod `45770f70`. Five configurations each ran the same 200 holdout seeds, 11276201–11276400, from entry until boss spawn. All 1,000 runs completed with zero deaths or timeouts. Each used randomized damage, 99 starting HP, finite food, prepot, utilities, live server boosts and queued-damage observations. The original bots used their original controller at `f80a4c07`. The other four configurations used identical new tasks and independent observations. Disabling lookahead for a role made it select the leading feasible action by its immediate preference and launch timing. [Per-seed results, including the development cohort](research/nylocas/decision-model-2026-11-27.csv)

| Configuration | All-room mean, ticks | Eligible mean, game time | Eligible P5 | Eligible P95 | Wave-19 failures | By 3:16.8 | By 3:22 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Original bots | 325.14 | 3:15.06 | 3:07.2 | 3:24.0 | 1 | 142/200 | 188/200 |
| New tasks, immediate utility | 328.76 | 3:16.94 | 3:09.6 | 3:24.0 | 6 | 111/200 | 166/200 |
| Ranger lookahead only | 326.94 | 3:15.94 | 3:09.6 | 3:24.0 | 5 | 133/200 | 185/200 |
| Mage lookahead only | 326.84 | 3:15.88 | 3:09.6 | 3:24.0 | 7 | 134/200 | 180/200 |
| Both roles with lookahead | 325.90 | 3:15.50 | 3:09.6 | 3:24.0 | 3 | 148/200 | 187/200 |

Eligible times exclude rooms with a wave-19 stall, as specified in the training guidance. Both deadline columns count all 200 trials. One tick is 0.6 game seconds. The 95% intervals below use paired differences in all-room completion ticks. Identical seeds control the initial random state. Subsequent random draws can diverge after different actions. [Training guidance](nylocas-training-guidance.md)

* Enabling both planners saved **2.86 ticks** against the same new tasks using immediate utility (95% interval **1.23 to 4.49 ticks saved**).
* Against the original bots, the revised pair took **0.76 ticks longer** on average (95% interval **0.70 ticks faster to 2.22 ticks slower**). Eligible P95 was **3:24.0** for both. The revised pair reached 3:22 in **187/200** rooms (93.5%). The training targets are a 3:09 eligible mean, a 3:16.8 eligible P95 and at least 95% of all rooms by 3:22.

| Per room, holdout | Original bots | Both roles with lookahead |
| --- | ---: | ---: |
| Pre-cap stalls | 2.04 | 2.27 |
| Post-cap stalls | 6.50 | 6.45 |
| Wave-28 stalls | 2.08 | 2.04 |
| Ranger ready-idle ticks | 24.18 | 22.59 |
| Mage ready-idle ticks | 36.97 | 38.73 |
| Same-tick double-ups | 0.04 | 1.09 |
| Overlapping primary flights | 1.88 | 2.77 |
| Untraced headless rooms per wall-clock second | 5.51 | 3.45 |

The revised pair had fewer ranger ready-idle ticks and fewer post-cap stalls. Pre-cap stalls and duplicate primaries increased. Of 54 same-tick double-ups in the first 50 traced holdout rooms, 24 occurred before cap, six during waves 20–30 and 24 after wave 31 spawned. Four hit the last living nylo. No target already had lethal damage in flight before the two simultaneous launches. Simulation throughput includes per-room setup and excludes Gradle and cache startup.

## Native behavior checks

The first 50 holdout seeds were replayed with tracing. Every gameplay column reproduced exactly. The timings below are confirmed native launches or observed combat deaths.

| Check | Holdout trace result |
| --- | --- |
| Ranger wave 10 | 50/50 west chins on spawn, followed in order by east and south chins |
| Mage wave 8 | 50/50 west openers on spawn, 15/15 first-available follow-ups on uncovered survivors |
| Mage wave 12 | 50/50 east scythes into west Dinh. All requested Dinh at scythe +5. Movement delayed two launches to +6 |
| Wave 21 | 50/50 east chin first for ranger and 50/50 west barrage for mage |
| Transition clearance at wave 21 | 36/50 cleared every original 18/19 parent and south/west 20 in time |
| Assigned parents on waves 23–26 | 50/50 combat kills for each of south 23, west/south/east 24, west 25 and west 26 |
| Late Dinh on the south-26 parent | 46/50 launches. The primary stayed ranged in two rooms and moved out of the southwest doorway in two |
| Ranger wave 30 | 50/50 chins hit both south smalls on first green, 27/27 returned to an uncovered west parent on the next shot |

No confirmed primary targeted a nylo already known to be personally NULL. Neither bot issued an obstructed NPC click or one beyond its one-cycle reach. Native integration fixtures exercise each bot with a human-controlled partner. The room-time measurements cover bot duos. The final implementation passed 524 Java tests and 74 native integration tests. The Python tooling suite passed 55 tests and skipped four.

## Development cohort and recovery

The first five-way experiment used seeds 11276001–11276200. Before changing mage parent order, both planners averaged 325.94 ticks, compared with 327.96 for immediate utility and 325.20 for the original pair. The 200 fresh holdout seeds above were reserved for the revised implementation. Both cohorts are labeled in the CSV.

Tracing seed `11276015` exposed a scheduling error. The mage opened west 25 while east 24 was untouched. Two low scythe rolls on west 25 left time for only one swing on east 24 before the south-29 window. The mage now approaches east 24 if it can swing by the next ready tick and west 24 is covered. A wounded west-25 parent retains its follow-up. On the same seed, east 24 died at age 22 and the later required parents also died in combat.

## Reproduction

Run from the workspace root with a fresh output directory.

```bash
python3 tools/simulate-nylocas.py --offline --trials 200 --seed 11276201 \
  --starting-hitpoints 99 --prepot --utilities --server-boosts \
  --output .state/simulations/planner-replay
```

The runner saves the policy sources, controller sources, hashes, trial CSV and summary in each output directory. Local runs are under `.state/simulations/decision-model-20261127/`. To rerun a control, pass the saved `controls-final/roles-only`, `controls-final/ranger-lookahead` or `controls-final/mage-lookahead` directory to `--policy-source`.

To recreate a control elsewhere, copy the canonical Java source tree at `45770f70`. In `RoomPlanner.choose()`, return `choices.get(0).action` for each disabled role immediately after sorting candidates. Leave the entire native controller and both role strategies in place. Use the original RSMod checkout at `f80a4c07` for the original-pair comparison. To reproduce the traces, use `--trials 50 --trace` and run the training and role audits on that output directory. [Training audit](../tools/analyze-nylocas-training.py) · [Role audit](../tools/research/nylocas_role_priorities_audit.py)
