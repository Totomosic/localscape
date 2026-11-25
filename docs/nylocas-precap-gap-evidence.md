# Remaining pre-cap gap: primary replay evidence

Reproduce with `python3 tools/research/nylocas_precap_gap.py`. The [portable output](research/nylocas/training-v2-precap-gap.json) contains individual witnesses, exact counts, and SHA-256 hashes of all input files. No gameplay changes or builds are part of this analysis.

Human denominators are the **46 mage-corpus / 42 ranger-corpus recordings** released by [whole-pair vetting](nylocas-training-v2-evidence.md), with 19 overlapping recordings. They are individually fast selected rooms from qualifying pairs, not random samples or 88/106-room complete-pair strategy samples. Each corpus contains both players' attacks. Ownership comparisons below use both players from the same recording set; the two corpus estimates must not be pooled as independent observations.

Bot comparison is the 30 completed rooms in `.state/simulations/training-v2-utility-instrumented-30`, using that run's immutable policy sources, native utility support, finite resources, and native prepot setup. A pre-cap primary means a confirmed attack while the current wave is below 20. Human events come from first-party Blert replay responses already pinned by the corpus manifests; for example [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas). CSV attack observations establish launches, not damage attribution or complete secondary-victim counts.

## Strongest falsifiable candidate: west wave-19 shared ownership

| Observation | Vetted mage46 corpus | Vetted ranger42 corpus | Bot30 |
|---|---:|---:|---:|
| Ranger primary attacks on west birth-19 magic big | 45 | 42 | 1 |
| Mage primary attacks on that big | 12 | 15 | 46 |
| Rooms with any ranger primary on it | 36/46 | 30/42 | 1/30 |
| Median first ranger launch age, among rooms with a launch | 4 | 4 | — |

In the 46-room set, west-19 ownership is ranger-only in 29 rooms, both roles in seven, mage-only in five, and neither recorded primary in five. Missing primary events do not identify who killed the NPC. In 22/36 ranger-observed rooms, the first ranger launch is exactly age four; all are ages 4–15. The independent ranger42 set has 20/30 at age four, one at three, and the remainder ages 9–15.

For all 36 ranger-observed rooms in the mage46 set, the median difference between first west-19 launch and the ranger's first respective wave-18 launch is:

| Relative launch | Median difference |
|---|---:|
| west-19 minus west-18 | +8 ticks |
| west-19 minus south-18 | −3 ticks |
| west-19 minus east-18 | −5 ticks |

Thus the dominant observed sequence interleaves **west-18 → west-19 around age four → south-18 → east-18**. It does not wait for all three wave-18 bigs to finish. All per-room timings and exceptions are in the JSON. In the witness `0c25e1`, west-19 is tick 148, following west-18 by six ticks and preceding south/east-18 by three/five.

The bot's mage has a `West big 19` assignment, with the same additional 6,000 score applied to all three wave-19 mage bigs. The ranger also has `West magic big 19`, but `RangerPreCapPlanner.prioritize` explicitly removes west-19 choices whenever any alternative remains. Mage-first confirmed/provisional claims then reinforce that ownership. The trace has mage west-19 attacks **43 while wave 19 is current, three during 20**, and just one ranger attack during 20. This is a scheduling mismatch, not a missing weapon or ranger target rule.

The stall boundary is material: bot mean wave19→20 interval is **18.40 ticks**, versus **12.09** in mage46 and **12.00** in ranger42. The native minimum is 12. Bot stalls after last-spawned wave19 average **1.60/room**, accounting for 35% of its total **4.57 pre-cap stalls**. This correlation does not establish that ownership causes every one of those stalls.

**Candidate experiment:** make a narrow, coordinated west-19 handoff around its early magic window when the ranger can actually launch, preserving mage backup when the ranger is null, unavailable, or misses the window. Remove the ranger's unconditional fallback demotion for that window as part of the same isolated change. Measure confirmed ownership and earliest launch, time of all three big deaths/splits, residual small count at each wave20 check, pre-cap stalls, total time, and deaths on identical seeds/resources. A successful mechanism test should move west-19 launches from mage to ranger without increasing missed wave18 service or primary double-ups; a performance win should reduce last-spawned19 stalls. Reject if total timing or survival regresses.

The user's mage prose assigns all three wave-19 bigs to mage, so selective ranger assistance must retain that responsibility as backup. Attack CSVs do **not** expose authoritative per-player NULL state: do not infer that the seven dual-role rooms prove a particular NULL-recovery rule. Native observed NULL fields should gate a bot experiment.

## Attack counts and multi-target windows

| Confirmed pre-cap primaries / room | Vetted humans | Bot30 |
|---|---:|---:|
| Mage, all weapons | 37.04 (46 rooms) | 41.20 |
| Mage sulphur | 12.39 | 14.70 |
| Mage Eye | 18.57 | 20.50 |
| Ranger, all weapons | 53.45 (42 rooms) | 58.87 |
| Ranger pipe | 31.21 | 32.30 |
| Ranger Eye | 12.19 | 14.90 |
| Ranger bow | 2.83 | 5.77 |
| Ranger chin | 4.57 | 3.90 |

More bot launches may be a consequence of longer waves, damage variance, or target choices. It is not by itself proof of weaker damage rolls. Generic mage scythe overuse was checked and **not supported**: bot65 pre-cap scythes comprise 30 scheduled east-12, 32 deferred east-13 big, and only three generic small attacks. Human46 has 46 east-12, 45 east-13 big, and five generic split scythes. The native scythe's secondary arc requires cardinal adjacency and a 1×3 row; merely counting nearby melee NPCs would overstate possible secondary hits.

The optional ranger chin windows differ: human south-3 has 40 primary throws/42 rooms, south-6 has 22/42, and east-8 has 24/42. Bot30 has respectively **0, 0, and 1**. Its three wave-10 lane packets already total 94 throws/30 rooms. This suggests another bounded experiment, but requires checking actual future victim geometry and damage; a bow can be a better single-target choice, so copying human throw counts alone is insufficient. This finding was sent to the ranger agent; wave18 ordering and fourth-Dinh experiments are owned separately.

## Thrall cast timing: initial summons versus renewal

The pinned [spell definitions](research/nylocas/training-v2/training-v2-spell-definitions.json) identify spell15 as Greater Ghost and spell16 as Greater Skeleton. Raw event11 casts were counted strictly before event123 boss spawn, with per-player ordinals. This avoids confusing a delayed first summon with renewal.

**Mage46: exactly 46 Ghost casts, all initial.** Thirty-nine are tick3 and seven tick4. There are **zero pre-boss Ghost renewals**. Relative to actual wave21 spawn, initial casts range −177..−164 ticks (median −169); relative31 −277..−252 (median −261); relative boss −321..−305 (median −317). Humans accept expiration during waves rather than automatically renewing mage Ghost.

**Ranger46 from those same recordings: 53 Skeleton casts.** The 46 initial casts are tick3×28, tick4×16, tick7×1, tick16×1. Seven second casts occur at ticks149,174,177,187,198,200,219, during waves19,21,21,22,23,24,25 respectively. The tick16 cast is a first summon, not a renewal. Their relative-wave21 offsets are −19,+6,+5,+15,+26,+28,+43; all are well before31/boss. The JSON preserves each UUID and exact relative timings.

In the instrumented bot30 log, initial thrall requests precede room timing by three ticks. There are **29 mage renewal requests**, at local ticks207–236 during observed waves23/24, and **30 ranger renewals**, 27 during31 and one each during22/23/25. These are request trace events, not a substitute for the native effect tests. The extra mage renewal is unlike all 46 human recordings, but it occurs after pre-cap in this run, so it cannot directly explain its pre-cap stalls. An initial-only versus renewal control can test the post-cap cost; do not attribute a later utility/order bundle's entire regression to this observation without isolating it.
