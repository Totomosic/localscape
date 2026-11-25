# Nylocas performance review questions

Jordan requested this record during the paused training review on 25 November 2026. Add each subsequent performance question as it is asked, preserving its intent and wording. Use the accumulated questions when reviewing future ranger and mage runs.

For each question, record the answer separately from the reusable question, identify the measured policy and cohort, link the supporting evidence, and distinguish observations from explanations that still need testing. Record unanswered questions and evidence limitations explicitly.

## 1. Wave-19 stalls in slow rooms

Jordan asked: “in runs where the boss spawn is slow (>3:19) how many wave 19 stalls are there”

Reusable check: For rooms above an explicit boss-spawn threshold, report the distribution, total and mean of wave-19 stalls, including the proportion with none. Identify the policy/cohort; do not mix candidate policies silently. A wave-19 stall retries admission to wave20 after19 has successfully spawned.

Answer for the current retainedSouth20 500-room randomized benchmark: **173/500** rooms have boss spawn strictly above199seconds/3:19 (first qualifying observed time332ticks/3:19.2).

| Wave-19 stalls per slow room | Rooms |
|---|---:|
| 0 | 144 |
| 3 | 3 |
| 4 | 14 |
| 5 | 11 |
| 7 | 1 |

That is **127 stalls total**, **0.734 per slow room**, with **29/173 (16.8%)** slow rooms having any. **144/173 (83.2%)** slow rooms have zero wave-19 stalls. No slow room has exactly1,2or6. This establishes that slow rooms commonly occur without wave-19 stalls; it does not identify the cause of their delay.

Evidence: [review calculation and per-room seeds](research/nylocas/training-v2/performance-review/slow-rooms-wave19-stalls.json), [retained cohort CSV](research/nylocas/training-v2/guidance-session-2026-11-25/training-v3-south20-coherent-pooled-500/trials.csv). Source `.state/simulations/training-v3-south20-coherent-pooled-500/trials.csv`; both constituent200/300 cohorts have matching policy/adapter hashes and disjoint freshly randomized seed ranges. No new simulations were run for this answer.

## Follow-up instruction: wave-19 stalls are policy failures

Jordan: “any wave 19 stall is a mistake, prevent that from happening but do not count them towards the average”. Clarification: **exclude affected rooms from the average**, rather than subtracting their stall delay.

Apply this rule to future performance reports: completed rooms with any last-spawned-wave19 stall are excluded entirely from the reported boss-time sample. Report the excluded count and treat every such room as a policy failure, with a zero-failure target. Keep raw all-room timings, CSV rows, stalls, deaths and completion statistics intact for diagnosis. Missing wave counters cannot establish eligibility. Exclusion does not mean the room passed the training goals.

Current retainedSouth20 benchmark under this rule: **470 eligible rooms**, **30 excluded policy-failure rooms**, reported average **326.953191ticks /3:16.171915**, eligible minimum **308 /3:04.80**, eligible maximum **352 /3:31.20**. Raw500-room average remains327.84ticks/3:16.704. The reported mean gap to3:09 is7.171915seconds. [Recomputed report](research/nylocas/training-v2/performance-review/retained-south20-wave19-filter/report.md).

Preventing these stalls is a required strategy correction, not an encounter-rule change. A targeted investigation of existing traces is authorized; the broader training loop remains paused. No prevention fix has yet been implemented or validated. The reporting code now exposes `wave19_zero_stall_goal_met`: excluding failed rooms cannot make the zero-stall requirement pass, and unknown wave-counter coverage cannot establish that it passed.

Initial trace investigation covers all30affected retained rooms. Every first-admission snapshot includes all three ignored wave17 melee parents;22include a still-living18East parent,25a dying18South parent, and19an old15South parent. Late removal can add children while20 remains unadmitted. These are occupancy observations, not causal proof or permission to change native caps. [Saved snapshots and launches](research/nylocas/training-v2/performance-review/wave19-failure-snapshots.json). Priority for resumed training: reproduce the first failed19→20admission, identify a legal earlier clearance or population-timing correction, and validate zero wave19failures on fresh randomized rooms while preserving transition packets.

## 2. Average pre-cap stalls in slow rooms

Jordan asked: “what is the average pre-cap stalls on slow boss spawns”

Reusable check: Among rooms above the specified boss-spawn threshold, apply the current room-exclusion rule, then report mean pre-cap stalls, sample size, distribution and largest wave contributors. Pre-cap means last-spawned waves1–19. Preserve raw-cohort context separately.

For retainedSouth20, slow means strictly above3:19/199seconds. After excluding entire rooms with any wave19stall, **144 eligible slow rooms average2.902778 pre-cap stalls**, minimum0, maximum6 (418stalls total).

| Pre-cap stalls per eligible slow room | Rooms |
|---|---:|
| 0 | 1 |
| 1 | 13 |
| 2 | 42 |
| 3 | 44 |
| 4 | 32 |
| 5 | 11 |
| 6 | 1 |

Largest mean contributions per eligible slow room: wave12 **0.875**, wave16 **0.548611**, wave10 **0.381944**, wave11 **0.291667**. These identify where stalls accumulate, not why they occur. Before the exclusion rule, all173slow rooms average3.566474pre-cap stalls; that raw number includes their wave19failures and is not the requested filtered average.

Evidence: [calculation, per-wave means and eligible seeds](research/nylocas/training-v2/performance-review/slow-rooms-precap-stalls.json). Source is the same retained500-room CSV and checksum used for question1. No new simulations were run.

## 3. Stalls before wave28 and making expiry in slow rooms

Jordan asked: “ok seems like pre-cap is not the problem when it comes to these slow bosses. How many stalls pre-28 and was expiry made?”

Reusable check: On the same slow-room sample and exclusion rules, separate stalls after waves21–27 from the allowed wave28stalls; also report total stalls before28if the phrase could mean all waves1–27. Determine making expiry from terminal NPC births and native lifetime/death state, not boss time alone. Split results by wave28stall count and keep same-tick boundary cases separate.

RetainedSouth20, **144 eligible slow rooms (>3:19, no wave19stalls)**:

| Stall window | Mean per room | Minimum | Maximum |
|---|---:|---:|---:|
| After waves21–27 | 0.791667 | 0 | 5 |
| All stalls before28 (waves1–27) | 3.694444 | 1 | 7 |
| After wave28 | 2.854167 | 0 | 6 |

After waves21–27:70rooms have0stalls,44have1,22have2,7have3,1has5. Wave20stalls are zero in this sample. These phase windows use last-successfully-spawned naming.

**Making expiry:**24/144(16.7%) clearly make it: the last positiveHP, non-dying observed targets are wave29births already in their native natural-expiry animation, with fresh30+work finished.117/144(81.3%) do not: the last living snapshot still includes fresh30+births.3/144(2.1%) have an old29expiry and fresh30+target together in the final living snapshot; within-cycle ordering is not asserted. Additionally,40/144rooms show at least one fresh30+ lifetime reaching its native expiry animation with positiveHP. That is distinct from successfully making old29expiry.

| Wave28stalls | Slow rooms | Clearly made expiry | Same-tick boundary |
|---|---:|---:|---:|
| 0 | 12 | 1 | 1 |
| 1 | 6 | 3 | 0 |
| 2 | 59 | 8 | 1 |
| 3 | 17 | 0 | 0 |
| 4 | 18 | 4 | 0 |
| 5 | 28 | 6 | 1 |
| 6 | 4 | 2 | 0 |

This establishes a cleanup gap in the selected slow rooms: even17rooms with3wave28stalls all miss expiry. It does not establish that pre-cap contributes no delay, or that the same expiry percentages apply to all470eligible rooms.

Evidence: [per-room terminal snapshots, stalls, classifications and source hashes](research/nylocas/training-v2/performance-review/slow-rooms-pre28-expiry.json). Native `NylocasSize` expiry animation/removal ages are51/52for smalls and52/55for bigs; natural expiry preserves positiveHP and differs from the combat death queue. Read-only review used the existing two retainedSouth20trace logs. No simulations or policy changes were made.

## 4. Living nylos that neither bot attacks

Jordan observed: “if there is a nylo alive sometimes the bot will just not hit it”.

Reusable review question: **When a nylo remains alive, why is neither bot attacking it or moving to attack it? Is the restriction real, or is a policy filter unnecessarily suppressing useful work?** Record both players' readiness, native action/food delay, target HP/style/birth/age, NULL, line of sight and movement, confirmed queued damage, reservations and the exact selection/rejection reason. Distinguish ignored old births and known-lethal flights from fresh uncovered targets.

The observation is consistent with saved native witnesses of idle partner help: retainedSouth20 seed279799561480259 tick274 has a ready Mage selectingClear while fresh31RANGE targets195/196/198 haveHP8,pending0within5tiles and no uncovered own work. The isolated normalPIPE helper reproduces that frame red→green; its200-room native cohort is measured but not behavior-audited or retained. Separately,40/144eligible slow rooms in question3 have at least one fresh30+lifetime reach natural expiry, which is a candidate set for refusal/late-service review; expiry alone does not prove an earlier legal attack was available.

The user's exact observation and the future review checks were appended to [the guidance](nylocas-training-guidance.md), preserving every prior byte. [Prior guidance snapshot](research/nylocas/training-v2/performance-review/guidance-before-idle-observation.md). No strategy fix or simulation was performed for this observation; training remains paused.

## 5. Partner NULL rescue and acceptable final-target double-ups

Jordan: “Each bot needs to track if the other player NULLs on nylo and focus on that. It is fine to double up if it is the last nylo alive”.

Reusable review questions:

- **When either player NULLs on a nylo, does the other bot recognize that specific target and prioritize a legal rescue?** Check both roles, big/small targets, ordinary ownership exceptions, human partners, persistent lifetime identity, actual movement/attack availability and delay from the first observed NULL. Do not infer NULL from zero damage.
- **When only one living nylo remains, do anti-double-up rules unnecessarily prevent either player from helping?** Count every living birth, inspect unknown/nonlethal flights separately from proven queued lethal damage, and retain each player's own NULL/native restrictions and the established cutoff.

Current-source finding: the native controller already reads `isNulled(npc, partner)` into each model's `partnerNulled` on every observation. The canonical policies use that state in some ownership/rescue checks, but coverage/priority is incomplete: Mage's generic ranged-big rejection is not released solely by partnerNULL, and Ranger's rescue gates cover selected magic parents rather than every serviceable target. Tracking is present; comprehensive rescue priority and the last-target claim exception are not yet implemented/validated. Previously measured singleton/idle-helper candidates remain experimental.

Exact clarification appended to [the guidance](nylocas-training-guidance.md), preserving all earlier entries; [pre-append snapshot](research/nylocas/training-v2/performance-review/guidance-before-partner-null-clarification.md). Source inspection only; no native simulation or strategy change was performed during this paused review.

## 6. Cleanup reliability:95% of rooms by3:22

Jordan: “I think the biggest difference is going to be cleaning up these slow boss spawns which seems like a cleanup issue. Cleanup is a vital part of the room that saves or loses a lot of time. Work towards 95% of rooms being a boss spawn of 3:22 or less”.

Reusable review question: **Do at least95% of normal randomized rooms spawn the boss within202seconds, and what cleanup failures keep the remaining rooms slow?** Report all-trial numerator/denominator, P95, failures and excluded-average rooms separately. Correlate the tail with making expiry, fresh30/31survivors, partnerNULL, legal attacks declined and wave28stall counts; do not infer a cause from boss time alone.

Current retainedSouth20 baseline: **440/500(88%)** within3:22, **60/500(12%)** over; P95 **344ticks/3:26.40**. Need at least475/500within target (35more than baseline; no more than25late). Of60late rooms,20have a wave19stall and40do not. For context only, the average-eligible zero-wave19subset has430/470(91.49%)within3:22; the reliability headline uses all500rooms. This complements the3:09average and zero-wave19failure goals.

[Baseline calculation](research/nylocas/training-v2/performance-review/cleanup-95pct-3m22-baseline.json). The runner now reports this all-trial timing fraction and a95%-goal flag, independently of its filtered-average calculation. Normal tick boundary:336ticks/201.6seconds qualifies,337ticks/202.2does not. Guidance was appended, preserving all prior bytes. Training remains paused.

## 7. Dim terminology and the combined28+16/29experiment

Jordan defined **dim** as the wave and optional tick offset defining the birth cutoff on or before which spawned nylos are ignored. Examples:29dim and28+16dim; three wave28stalls put29's spawn at28+16. This term is in [the domain glossary](../GLOSSARY.md) and [the training brief glossary](../nylo-training.md#glossary).

Jordan requested: “Try using a 28+16 dim instead of 29 and see the effect it has on average times”. Asked about later split births, Jordan clarified: “I would recommend applying both a 28+16 and 29 dim. The 28+16 dim will help if there are less than 3 w28 stalls”.

Experiment: preserve current29dim and add actualbirthtick≤actual28spawn+16; either condition dims the NPC. Activation stays actual31. Unknown28spawn retains29-only fallback. The ranger also excludes dimmed NPCs from chin cluster counts, active reservations and final-packet completion checks, so ignored packets do not continue reserving time. All other source, combat, resources, loadouts and forecasting stay matched to retainedSouth20. Eight both-role boundary checks pass, including exact≤boundary, later births, wave29split union, activation, three-stall packets and reservation release.

Fresh independent randomized control200 at `.state/simulations/training-v4-dim29-control-random-200`:200/200completed,181averageeligible/19wave19failures, reportedaverage3:16.097238, eligible min3:07.20/max3:31.20;174/200(87%)within3:22, rawP95 3:26.40. Combined candidate200 now running at `.state/simulations/training-v4-dim29-and-28plus16-random-200`; canonical remains retainedSouth20. Do not infer an effect until results and phase-stratified behavior/expiry are audited. Training has explicitly resumed; the old pause line below is historical.

## 8. Metric correction for28+16dim

Jordan: “oh it will be very hard to get 3:02 with that 28+16 dim, don't use that metric”.

The prior3:02.4always-max-hit litmus is not an acceptance/rejection criterion for the28+16dim experiment or its comparisons. Use normal randomized reported average/minimum/maximum, all-trial95%by3:22and dim-aware cleanup behavior. Existing max-hit reports are preserved as historical measurements only. Do not label intentional dimmed-parent expiry a missed eligible attack. Both role agents were informed; no additional max-hit runs are planned. Training continues with the normal cooperation cohort.

Jordan explicitly resumed training after this review. These questions and clarifications remain the checklist for active iterations; earlier pause notes describe the review period.


## 9. Does dim use the actual birth timestamp?

Jordan confirmed **“Yes — later splits remain fresh”** for a split born252after wave29spawn240. Reusable review question: **Does every selection, rescue and helper path preserve targets born after the dim cutoff, including splits born while wave29is still current?** Test exact inclusive boundaries, later29births, both28/29anchor orders, unknown timestamps and literal-last counting of ignored bodies. Historical whole-wave-label dim results above are not results of this corrected timestamp definition. New29-only and29plus28+16controls use the same349parent-lifecycle base with no combat changes.


## 10. Does a selected attack actually launch?

Follow up Jordan’s living-nylo observation by comparing the policy’s predicted request with the confirmed native primary. A target selection that causes movement can still lose an attack cycle. **When the mage selects melee and only chases, was a fresh magic target already in range, with native line of sight and no NULL, claim or lethal coverage?** Preserve newer-target priorities when both attacks have comparable availability, and protect named packets, required parents and rescue work.

Timestamp control seed215613141308293/tick270 is a bounded example: the mage chooses a nonadjacent wave31 melee split and moves without a hit; two healthy late29 magic splits are within Eye range. Both native-map ray checks pass. Split children walk toward assigned pillars, while the current short timing model leaves their split-lane coordinates stationary. The isolated approach-aware candidate is being measured; this observation does not establish an outcome before native validation.
