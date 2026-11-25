# Duo Nylocas simulation report

Endpoint: Nylocas boss spawn (waves and cleanup only).

Damage profile: randomized (unless legacy metadata states otherwise).

Trials: 200; completed: 200; deaths: 0; timeouts: 0.

Reported boss spawn game seconds (completed rooms with zero wave-19 stalls): average **195.85**, minimum **187.20**, maximum **211.20**; eligible **181/200**, excluded for wave-19 stalls **19**, unknown wave counters **0**.

Wave-19 policy failures: **19** (target zero; exclusion from the time average does not excuse these failures).

Raw completion ticks (all successful trials): mean **327.98**, median **328.00**, minimum **312.00**, maximum **368.00**.

Average stalls per trial: Pre-cap stalls (last spawned 1–19) **2.63**; Post-cap stalls (last spawned 20–30) **5.84**; Pre-29 stalls (waves 1–28) **3.17**; Wave-28 stalls (admitting wave 29) **2.41**.

Throughput including per-trial setup: **2.54 trials/s**. Cache startup and Gradle build excluded.

| Metric (all trials) | Mean | Median | Min | Max |
| --- | ---: | ---: | ---: | ---: |
| ranger_attacks | 112.83 | 113.00 | 96.00 | 128.00 |
| mage_attacks | 74.81 | 74.00 | 69.00 | 86.00 |
| ranger_idle_ticks | 192.84 | 191.00 | 179.00 | 230.00 |
| mage_idle_ticks | 229.85 | 228.00 | 214.00 | 265.00 |
| ranger_ready_idle_ticks | 22.64 | 22.00 | 8.00 | 47.00 |
| mage_ready_idle_ticks | 30.34 | 29.00 | 10.00 | 55.00 |
| same_tick_double_ups | 0.01 | 0.00 | 0.00 | 1.00 |
| shared_targets | 3.96 | 4.00 | 0.00 | 10.00 |
| positive_damage_rolls | 241.93 | 241.00 | 224.00 | 265.00 |
| Pre-cap stalls (last spawned 1–19) | 2.63 | 2.00 | 0.00 | 13.00 |
| Post-cap stalls (last spawned 20–30) | 5.84 | 6.00 | 3.00 | 10.00 |
| Pre-29 stalls (waves 1–28) | 3.17 | 3.00 | 0.00 | 13.00 |
| Wave-28 stalls (admitting wave 29) | 2.41 | 2.00 | 0.00 | 7.00 |
| overlapping_primary_flights | 1.67 | 2.00 | 0.00 | 6.00 |

Mage completed trials with fewer than 20 ready-idle ticks: **11/200**; 95th percentile **44.00**.

Ranger completed trials with fewer than 20 ready-idle ticks: **75/200**; 95th percentile **35.00**.

Boss-time reliability: **176/200** completed at **3:22** or faster, **88.00%** (target **95%**). This denominator includes wave-19 failure rooms, deaths and timeouts; only the reported average excludes wave-19 rooms.

Raw boss spawn game seconds (all completed rooms): average **196.79**, minimum **187.20**, maximum **220.80**. Reported target average is189 seconds over completed rooms with known zero wave-19 stalls; the wave-19 policy-failure target remains zero.

Completed trials with at most one pre-cap stall: **49/200**. Completed without wave 21–27 stalls: **126/200**. Completed with two or three wave-28 stalls: **107/200**. Wave-counter coverage: 200 completed trials.

Idle ticks count ticks without a confirmed primary attack, including cooldown and movement. Ready idle ticks exclude policy cooldown. Current simulations count from each role's first confirmed attack through the final living nylo's death, excluding entrance and terminal death-animation/boss waits. Older reports used the entire encounter; check the run metadata before comparing.

Stalls count failed population-cap admission checks, each delaying admission by four ticks. Training names stalls by the last wave spawned: pre-cap 1–19, post-cap 20–30, and wave 28 delays admission to 29. Legacy CSV column wave_29_stalls retains its incoming-wave name. The wave 21–27 consistency check excludes the intentional wave-28 retries. Averages include failed and timed-out trials; legacy missing counters are unknown.

Same-tick double ups count both agents launching a primary attack at the same NPC on one tick. Overlapping primary flights count confirmed launches while the partner still awaits a real primary impact; overlaps are not proof of wasted damage. Legacy missing overlap counters are unknown. Shared targets count distinct NPC lifetimes attacked by both agents at any time; this can include useful follow-up hits.

Positive damage rolls include secondary AoE targets and multiple splats; they measure rolled damage before impact, not kills. Completion can include natural NPC expiry.

Fixed equipment and 99 base stats are defined in the server adapter. Reports cover server model behavior; see docs/nylocas-simulations.md for assumptions.
