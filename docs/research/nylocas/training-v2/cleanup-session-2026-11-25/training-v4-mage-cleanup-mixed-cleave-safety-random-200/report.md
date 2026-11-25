# Duo Nylocas simulation report

Endpoint: Nylocas boss spawn (waves and cleanup only).

Damage profile: randomized (unless legacy metadata states otherwise).

Trials: 200; completed: 200; deaths: 0; timeouts: 0.

Reported boss spawn game seconds (completed rooms with zero wave-19 stalls): average **195.65**, minimum **184.80**, maximum **211.20**; eligible **185/200**, excluded for wave-19 stalls **15**, unknown wave counters **0**.

Wave-19 policy failures: **15** (target zero; exclusion from the time average does not excuse these failures).

Raw completion ticks (all successful trials): mean **327.18**, median **328.00**, minimum **308.00**, maximum **356.00**.

Average stalls per trial: Pre-cap stalls (last spawned 1–19) **2.51**; Post-cap stalls (last spawned 20–30) **6.02**; Pre-29 stalls (waves 1–28) **3.07**; Wave-28 stalls (admitting wave 29) **2.32**.

Throughput including per-trial setup: **3.64 trials/s**. Cache startup and Gradle build excluded.

| Metric (all trials) | Mean | Median | Min | Max |
| --- | ---: | ---: | ---: | ---: |
| ranger_attacks | 113.25 | 113.00 | 101.00 | 126.00 |
| mage_attacks | 75.20 | 75.00 | 68.00 | 83.00 |
| ranger_idle_ticks | 191.60 | 191.00 | 176.00 | 216.00 |
| mage_idle_ticks | 228.65 | 228.00 | 212.00 | 251.00 |
| ranger_ready_idle_ticks | 21.09 | 20.00 | 9.00 | 42.00 |
| mage_ready_idle_ticks | 28.63 | 28.00 | 12.00 | 52.00 |
| same_tick_double_ups | 0.04 | 0.00 | 0.00 | 1.00 |
| shared_targets | 4.22 | 4.00 | 0.00 | 11.00 |
| positive_damage_rolls | 242.91 | 242.00 | 229.00 | 265.00 |
| Pre-cap stalls (last spawned 1–19) | 2.51 | 2.00 | 0.00 | 8.00 |
| Post-cap stalls (last spawned 20–30) | 6.02 | 6.00 | 3.00 | 11.00 |
| Pre-29 stalls (waves 1–28) | 3.07 | 3.00 | 0.00 | 9.00 |
| Wave-28 stalls (admitting wave 29) | 2.32 | 2.00 | 0.00 | 6.00 |
| overlapping_primary_flights | 1.82 | 2.00 | 0.00 | 7.00 |

Mage completed trials with fewer than 20 ready-idle ticks: **20/200**; 95th percentile **40.00**.

Ranger completed trials with fewer than 20 ready-idle ticks: **90/200**; 95th percentile **34.00**.

Boss-time reliability: **178/200** completed at **3:22** or faster, **89.00%** (target **95%**). This denominator includes wave-19 failure rooms, deaths and timeouts; only the reported average excludes wave-19 rooms.

Raw boss spawn game seconds (all completed rooms): average **196.31**, minimum **184.80**, maximum **213.60**. Reported target average is189 seconds over completed rooms with known zero wave-19 stalls; the wave-19 policy-failure target remains zero.

Completed trials with at most one pre-cap stall: **52/200**. Completed without wave 21–27 stalls: **121/200**. Completed with two or three wave-28 stalls: **122/200**. Wave-counter coverage: 200 completed trials.

Idle ticks count ticks without a confirmed primary attack, including cooldown and movement. Ready idle ticks exclude policy cooldown. Current simulations count from each role's first confirmed attack through the final living nylo's death, excluding entrance and terminal death-animation/boss waits. Older reports used the entire encounter; check the run metadata before comparing.

Stalls count failed population-cap admission checks, each delaying admission by four ticks. Training names stalls by the last wave spawned: pre-cap 1–19, post-cap 20–30, and wave 28 delays admission to 29. Legacy CSV column wave_29_stalls retains its incoming-wave name. The wave 21–27 consistency check excludes the intentional wave-28 retries. Averages include failed and timed-out trials; legacy missing counters are unknown.

Same-tick double ups count both agents launching a primary attack at the same NPC on one tick. Overlapping primary flights count confirmed launches while the partner still awaits a real primary impact; overlaps are not proof of wasted damage. Legacy missing overlap counters are unknown. Shared targets count distinct NPC lifetimes attacked by both agents at any time; this can include useful follow-up hits.

Positive damage rolls include secondary AoE targets and multiple splats; they measure rolled damage before impact, not kills. Completion can include natural NPC expiry.

Fixed equipment and 99 base stats are defined in the server adapter. Reports cover server model behavior; see docs/nylocas-simulations.md for assumptions.
