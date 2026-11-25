# Duo Nylocas simulation report

Endpoint: Nylocas boss spawn (waves and cleanup only).

Damage profile: randomized (unless legacy metadata states otherwise).

Trials: 500; completed: 500; deaths: 0; timeouts: 0.

Reported boss spawn game seconds (completed rooms with zero wave-19 stalls): average **195.25**, minimum **184.80**, maximum **208.80**; eligible **453/500**, excluded for wave-19 stalls **47**, unknown wave counters **0**.

Wave-19 policy failures: **47** (target zero; exclusion from the time average does not excuse these failures).

Raw completion ticks (all successful trials): mean **326.96**, median **328.00**, minimum **308.00**, maximum **368.00**.

Average stalls per trial: Pre-cap stalls (last spawned 1–19) **2.62**; Post-cap stalls (last spawned 20–30) **5.81**; Pre-29 stalls (waves 1–28) **3.20**; Wave-28 stalls (admitting wave 29) **2.23**.

Throughput including per-trial setup: **2.45 trials/s**. Cache startup and Gradle build excluded.

| Metric (all trials) | Mean | Median | Min | Max |
| --- | ---: | ---: | ---: | ---: |
| ranger_attacks | 113.14 | 113.00 | 102.00 | 128.00 |
| mage_attacks | 75.03 | 75.00 | 69.00 | 85.00 |
| ranger_idle_ticks | 191.39 | 190.00 | 176.00 | 221.00 |
| mage_idle_ticks | 228.50 | 227.50 | 213.00 | 263.00 |
| ranger_ready_idle_ticks | 21.10 | 20.00 | 8.00 | 53.00 |
| mage_ready_idle_ticks | 28.89 | 28.00 | 10.00 | 59.00 |
| same_tick_double_ups | 0.01 | 0.00 | 0.00 | 1.00 |
| shared_targets | 4.03 | 4.00 | 0.00 | 11.00 |
| positive_damage_rolls | 242.56 | 242.00 | 227.00 | 263.00 |
| Pre-cap stalls (last spawned 1–19) | 2.62 | 2.00 | 0.00 | 11.00 |
| Post-cap stalls (last spawned 20–30) | 5.81 | 6.00 | 3.00 | 11.00 |
| Pre-29 stalls (waves 1–28) | 3.20 | 3.00 | 0.00 | 11.00 |
| Wave-28 stalls (admitting wave 29) | 2.23 | 2.00 | 0.00 | 7.00 |
| overlapping_primary_flights | 1.72 | 2.00 | 0.00 | 8.00 |

Mage completed trials with fewer than 20 ready-idle ticks: **41/500**; 95th percentile **42.00**.

Ranger completed trials with fewer than 20 ready-idle ticks: **230/500**; 95th percentile **34.00**.

Boss-time reliability: **452/500** completed at **3:22** or faster, **90.40%** (target **95%**). This denominator includes wave-19 failure rooms, deaths and timeouts; only the reported average excludes wave-19 rooms.

Raw boss spawn game seconds (all completed rooms): average **196.18**, minimum **184.80**, maximum **220.80**. Reported target average is189 seconds over completed rooms with known zero wave-19 stalls; the wave-19 policy-failure target remains zero.

Completed trials with at most one pre-cap stall: **125/500**. Completed without wave 21–27 stalls: **289/500**. Completed with two or three wave-28 stalls: **288/500**. Wave-counter coverage: 500 completed trials.

Idle ticks count ticks without a confirmed primary attack, including cooldown and movement. Ready idle ticks exclude policy cooldown. Current simulations count from each role's first confirmed attack through the final living nylo's death, excluding entrance and terminal death-animation/boss waits. Older reports used the entire encounter; check the run metadata before comparing.

Stalls count failed population-cap admission checks, each delaying admission by four ticks. Training names stalls by the last wave spawned: pre-cap 1–19, post-cap 20–30, and wave 28 delays admission to 29. Legacy CSV column wave_29_stalls retains its incoming-wave name. The wave 21–27 consistency check excludes the intentional wave-28 retries. Averages include failed and timed-out trials; legacy missing counters are unknown.

Same-tick double ups count both agents launching a primary attack at the same NPC on one tick. Overlapping primary flights count confirmed launches while the partner still awaits a real primary impact; overlaps are not proof of wasted damage. Legacy missing overlap counters are unknown. Shared targets count distinct NPC lifetimes attacked by both agents at any time; this can include useful follow-up hits.

Positive damage rolls include secondary AoE targets and multiple splats; they measure rolled damage before impact, not kills. Completion can include natural NPC expiry.

Fixed equipment and 99 base stats are defined in the server adapter. Reports cover server model behavior; see docs/nylocas-simulations.md for assumptions.
