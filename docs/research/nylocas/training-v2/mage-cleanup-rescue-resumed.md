> Timestamp correction, 2026-11-25: the user confirmed that the intended dim is birth timestamp, not birth-wave label. At actual31 only, ignore born <= actual spawn29; the combined experiment uses born <= max(actual spawn29, actual spawn28 + 16). Later splits born during wave29 remain fresh. All whole-wave dim measurements below are preserved historical experiments, not evaluations of that intended rule. Candidate freshness conditions copied from those experiments must be generalized before new validation.

# Mage cleanup rescue after the explicit resume

The current source remains retainedSouth20. This investigation preserves the user's appended guidance and every existing uncommitted change. No Gradle jobs or canonical policy edits were made by the mage agent. Root serializes native experiments and owns retention.

The new reliability denominator is **all trials**, requiring at least95% boss spawns by202seconds (336ticks qualifies,337does not). The3:09average excludes entire rooms with any wave19stall, with those policy failures reported separately. Neither exclusion nor completion changes the reliability denominator.

## Previously unreviewed idle ranged helper

`training-v3-mage-idle-ranged-cleanup-random-200` completes200/200. Raw average328.76ticks (3:17.256), minimum312 (3:07.2), maximum364 (3:38.4). There are181average-eligible rooms and19excluded wave19failures. Eligible average327.005525ticks (3:16.203315), minimum312, maximum356 (3:33.6). Reliability is170/200=85% within3:22;166/181within the eligible subset is context only. The95% goal remains unmet.

The helper makes26 normal blowpipe launches in15rooms on23distinct targets:24launches on birth31 and2on birth30, all smalls. Every launch has a known nonlethal pending forecast; no ownNULL or actual31old-birth violation is observed. Both12packets,21barrage and22Dinh occur200/200. All required23–26parents enter combat death;11ordering200/200,19east-first198 with2missing, all101physical26/27overlaps covered by lethal pending26damage. These independent cohort means do not isolate a causal speed change.

Native behavior is saved in `mage-idle-helper-behavior.json`, `mage-movement-audit.json`, `mage-guidance-audit.json` and `mage-partner-null-screen.json` in that run directory.

## Reproduced NULL rescue and final-target gaps

Native seed22080991948215 supplies a priority witness. Ranger NULL on south30small185 becomes visible at282. It remains5HP and uncovered. At292 mage3300,4249 can use Eye on both185at3299,4244 and fresh31split203at3301,4251, but selects203. Mage only launches on185at298. Its earlier melee-to-magic flicker and cooldown mean the entire16ticks sinceNULL are not automatically avoidable; the minimal292frame proves an equally actionable rescue loses to ordinary work. A second check makes the NULL become visible during an existing approach, reproducing the stale-intent version.

Current canonical source releases some ordinary ownership for partnerNULL, but still excludes unassigned bigs and ranged bigs from generic service. It also keeps an unknown partner lease when the target is the final living crab. These are policy filters, separate from native damage or movement restrictions. A zero damage roll is never treated as NULL.

The immutable V2 candidate is `.state/simulations/training-v3-candidate-mage-cleanup-rescue-final-target-v2/policy-source`. It is based on the inspected current canonical source, with base hashes saved alongside it. It adds:

- Priority rescue for positiveHP, non-dying fresh birth30+targets when the partner is NULL, during actual30/31cleanup, covering smalls/bigs and current legal styles.
- Release of an unserviceable partner's ordinary ownership, stale unknown partner lease and partner packet reservation. Mandatory mage packets still preempt and normal return budgets still apply.
- Separate final-living-target cooperation in actual31, allowing unknown/nonlethal partner leases and ordinary ownership to yield. Every positiveHP, non-dying birth counts, including ignored old targets.

OwnNULL, known lethal queued damage, unknown own-flight protection, normal cooldown, upcoming-style checks, native expiry and the actual31cutoff for birth29or earlier remain enforced. This candidate contains no South29gate, dim, movement or ordinary idle-helper changes.

Nineteen cooperation checks plus110existing mage checks pass129/129. Original canonical fails10new checks. One legacy test expects an ordinary named31lane to beat a partnerNULL rescue and a partnerNULL target to retain its ranger reservation; that expectation conflicts with the user's latest clarification. Its revised copy is candidate-local under `role-tests/`, while the original uncommitted test is preserved. V1is also preserved; V2adds explicit positiveHP/non-dying rescue eligibility and a transient-zeroHP check.

A supplemental matrix outside the immutable candidate checks all six small/big × current-style combinations, each with an unknown stale partner lease and competing ordinary work. It passes using the specified normal Eye/Pipe/Sulphur/Scythe selection. No native result is claimed for V2 yet.

## Foundation behavior and remaining healthy-parent failure

`training-v4-cleanup-foundation-random-200` completes200/200, raw average328.08ticks (3:16.848), minimum312 (3:07.2), maximum356 (3:33.6). Reported184eligible rooms average326.782609ticks (3:16.069565);16wave19failures remain excluded from that average. Reliability171/200=85.5% is below the95% goal.

Both12packets,21barrage and22Dinh occur200/200; South29first mage attack198/200, always during actual30. All required23–26parents enter combat death,11ordering200/200,19east-first194 with6missing, all100physical26/27overlaps covered by lethal26forecasts. The final living snapshot in three of four rooms over350ticks contains fresh targets already covered by lethal damage; the fourth is an old29expiry floor. Their origins require investigation before blaming the final protected flight.

Exact SW/NE newborn-pair attribution finds10terminal fresh-child lifetimes in idle200and7in foundation200whose parent is a **fresh30West big naturally expiring**. These counts are lifetimes, not independent rooms. Several preceding mage-ready Clear observations show the parent melee, mage-notNULL and otherwise unserviced. A healthy unassigned30West melee parent is still rejected by `finishBig`; partnerNULL/final-target exceptions do not activate while ignored old29bodies remain. This is a separate idle-melee-parent hypothesis, not part of V2. Attribution and bounded witnesses are in each run's `cleanup-terminal-parent-origins.json`.

Foundation seed268077164744000 at312 is a precise example. Parent185 is birth30, age40,7HP, melee, pending0, neither roleNULL. Mage3297,4249 is ready and Clear; ranger3293,4248 stages. Another physical31range body197 remains3HP but is covered by pending5, so the literal final-target predicate correctly remains false. Parent185 expires327, creates the final31child and boss spawns356. The isolated `training-v3-candidate-mage-idle-melee-parent-cleanup` permits normalScythe movement/attacks on fresh melee bigs only when ordinary mage candidates and packets are empty, actual31 and ready. Ten guards plus110existing checks pass120; canonical fails two. It is separate from V2, idleRange, dim and South29timing and has not been measured natively.

The older South29moving-target return-margin investigation was incomplete at the pause. Its three movement200fillers are birth28meleecases during actual29; one is a late split born after28spawn+16. Root clarified that the dim experiment activates **only at actual31**, so it cannot affect these earlier29filler decisions. An initial overlap inference based on earlier boundary wording was corrected.

Resumed probes reproduce all three258frames just before their259north-filler launches. The first two are west28pillar melees with `fighting=false`; the third is a pillar split. Native chase ends farther north than the short geometric return prediction, and the original return budget spends the last usable cooldown. The isolated `training-v4-candidate-mage-south29-return-margin`, based on the measured cleanup foundation with29staging already present, adds one return-travel tick only for melee fillers before `29 south stage`/`South big 29`. It leaves the own named packet exempt and changes no attack deadline. All three controls are red; the candidate passes116checks, including a nearby filler still permitted, no actual29opening and the actual31cutoff. This before31movement candidate remains separate from dim, rescueV2 and the pending original29gate experiment and has no native randomized result yet.

## Distinguishing deliberate dim from refused legal work

Root's isolated dim experiment activates only during actual31 and ignores a target if **birth wave≤29 OR born≤wave28spawn+16**. It changes no policy before31. The rescue/final-target exceptions must not bypass this hard experimental gate; dimmed positiveHP bodies still count for literal final-target detection.

The experiment's fresh300 completes300/300, with282average-eligible rooms and18excluded19failures. Reported average196.263830seconds (3:16.263830), minimum184.8 (3:04.8), maximum213.6 (3:33.6);261/300=87% meet3:22. Zero primary attacks violate the measured dim. Fifteen rooms newly dim birth30/31targets at31, including13big lifetimes. Eleven newly dimmed bigs naturally expire across7rooms; that is intentional experimental behavior, not a refusal on an undimmed target. Independent control500 comparison is root-owned; these numbers do not establish an effect.

Separately,38undimmed lifetimes reach expiry:8healthy30West bigs and30birth30smalls. All8bigs and29/30smalls have neither roleNULL; the remaining small has RangerNULL only. Five bigs have ready Mage Clear observations, while none of the smalls does before age49. Thus the reproduced idle-parent filter and competition for Mage time are distinct gaps; comprehensive NULL rescue cannot by itself address all these expiries.

Exact undimmed witness32146393791809: wave28spawn232 gives cutoff248;30West183 is born260, so it remains eligible under the experimental dim. At304 it is melee,8HP,pending0, neither roleNULL. Mage3297,4249 is Clear and ranger3293,4248 stages; native action delays−3/0 confirm both can act. Parent expires later and the boss spawns340. This supports the separate idle-Scythe-parent candidate, not an exception to dim. Per-lifetime NULL classifications and bounded native witnesses are saved under the fresh300run directory and copied alongside this note.
