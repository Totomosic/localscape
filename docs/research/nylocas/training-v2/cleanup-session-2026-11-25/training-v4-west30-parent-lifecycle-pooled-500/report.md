# Duo Nylocas simulation report

Endpoint: Nylocas boss spawn (waves and cleanup only).

Damage profile: randomized (unless legacy metadata states otherwise).

Trials: 500; completed: 500; deaths: 0; timeouts: 0.

Reported boss spawn game seconds (completed rooms with zero wave-19 stalls): average **195.43**, minimum **184.80**, maximum **208.80**; eligible **453/500**, excluded for wave-19 stalls **47**, unknown wave counters **0**.

Wave-19 policy failures: **47** (target zero; exclusion from the time average does not excuse these failures).

Raw completion ticks (all successful trials): mean **327.13**, median **324.00**, minimum **308.00**, maximum **360.00**.

Average stalls per trial: Pre-cap stalls (last spawned 1–19) **2.51**; Post-cap stalls (last spawned 20–30) **5.84**; Pre-29 stalls (waves 1–28) **3.06**; Wave-28 stalls (admitting wave 29) **2.16**.

Throughput including per-trial setup: **2.54 trials/s**. Cache startup and Gradle build excluded.

| Metric (all trials) | Mean | Median | Min | Max |
| --- | ---: | ---: | ---: | ---: |
| ranger_attacks | 112.72 | 112.50 | 90.00 | 127.00 |
| mage_attacks | 74.75 | 75.00 | 68.00 | 84.00 |
| ranger_idle_ticks | 192.16 | 191.00 | 177.00 | 217.00 |
| mage_idle_ticks | 229.14 | 228.00 | 214.00 | 254.00 |
| ranger_ready_idle_ticks | 22.70 | 22.00 | 8.00 | 60.00 |
| mage_ready_idle_ticks | 30.20 | 30.00 | 10.00 | 55.00 |
| same_tick_double_ups | 0.02 | 0.00 | 0.00 | 1.00 |
| shared_targets | 3.99 | 4.00 | 0.00 | 9.00 |
| positive_damage_rolls | 241.21 | 241.00 | 201.00 | 264.00 |
| Pre-cap stalls (last spawned 1–19) | 2.51 | 2.00 | 0.00 | 11.00 |
| Post-cap stalls (last spawned 20–30) | 5.84 | 6.00 | 0.00 | 11.00 |
| Pre-29 stalls (waves 1–28) | 3.06 | 3.00 | 0.00 | 11.00 |
| Wave-28 stalls (admitting wave 29) | 2.16 | 2.00 | 0.00 | 6.00 |
| overlapping_primary_flights | 1.75 | 2.00 | 0.00 | 6.00 |

Mage completed trials with fewer than 20 ready-idle ticks: **28/500**; 95th percentile **43.00**.

Ranger completed trials with fewer than 20 ready-idle ticks: **177/500**; 95th percentile **36.00**.

Boss-time reliability: **447/500** completed at **3:22** or faster, **89.40%** (target **95%**). This denominator includes wave-19 failure rooms, deaths and timeouts; only the reported average excludes wave-19 rooms.

Raw boss spawn game seconds (all completed rooms): average **196.28**, minimum **184.80**, maximum **216.00**. Reported target average is189 seconds over completed rooms with known zero wave-19 stalls; the wave-19 policy-failure target remains zero.

Completed trials with at most one pre-cap stall: **137/500**. Completed without wave 21–27 stalls: **299/500**. Completed with two or three wave-28 stalls: **303/500**. Wave-counter coverage: 500 completed trials.

Idle ticks count ticks without a confirmed primary attack, including cooldown and movement. Ready idle ticks exclude policy cooldown. Current simulations count from each role's first confirmed attack through the final living nylo's death, excluding entrance and terminal death-animation/boss waits. Older reports used the entire encounter; check the run metadata before comparing.

Stalls count failed population-cap admission checks, each delaying admission by four ticks. Training names stalls by the last wave spawned: pre-cap 1–19, post-cap 20–30, and wave 28 delays admission to 29. Legacy CSV column wave_29_stalls retains its incoming-wave name. The wave 21–27 consistency check excludes the intentional wave-28 retries. Averages include failed and timed-out trials; legacy missing counters are unknown.

Same-tick double ups count both agents launching a primary attack at the same NPC on one tick. Overlapping primary flights count confirmed launches while the partner still awaits a real primary impact; overlaps are not proof of wasted damage. Legacy missing overlap counters are unknown. Shared targets count distinct NPC lifetimes attacked by both agents at any time; this can include useful follow-up hits.

Positive damage rolls include secondary AoE targets and multiple splats; they measure rolled damage before impact, not kills. Completion can include natural NPC expiry.

Fixed equipment and 99 base stats are defined in the server adapter. Reports cover server model behavior; see docs/nylocas-simulations.md for assumptions.
