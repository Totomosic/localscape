# Mage cleanup floors and service before wave 31

Status: primary-source investigation, 2026-11-25. No canonical edits, native jobs, combat-rule changes or resource grants were made by this agent. Root serializes native runs. Latest guidance and timestamp clarification govern future work.

## Corrected dim semantics

The confirmed predicate activates only at actual31. Ignore a crab whose `born <= spawn29`; with the second cutoff use `born <= max(spawn29, spawn28 + 16)`. Equality is ignored. Late splits born after this timestamp during wave29 remain fresh. Older experiments used the whole birth-wave label `wave <= 29`; their outputs stay frozen and are historical. They cannot establish the intended timestamp policy's effect.

`tools/research/nylocas_cleanup_floor_audit.py --wave29-timestamp` implements the confirmed audit predicate; add `--wave28-offset 16` for the combined cutoff. Without the timestamp flag it explicitly labels historical whole-wave semantics. A floor is conditional on the observed expiry trajectory, not a universal lower bound: earlier service, admission changes or incidental damage can change it.

## Historical observed expiry floors

A positive-HP, non-dying small naturally disappears at age52 in native traces. Given its observed birth, the small's removal plus16 and encounter four-tick phase gives a conditional boss floor. Native `NylocasEncounter.checkCleanup` sets the cleanup cycle to the previous cycle and aligns the boss phase. Parent origins use the simultaneous southwest/northeast child pair at removal, rather than guessed nearest NPCs.

| Historical run | Rooms | Mean observed floor, ticks | Boss minus floor mean / min / max | Boss exactly at floor | Floor >336 ticks |
|---|---:|---:|---:|---:|---:|
| Cooperation combined whole-wave29 +28+16 |200|324.30|4.34 /0 /20|91|13|
| Cooperation whole-wave29 only |200|322.40|4.28 /0 /16|87|8|

The 183 zero-wave19 rooms in the first run have mean floor322.972678 ticks; the188 in the second321.425532. These numbers were a useful diagnosis of the misinterpreted policy, but late wave29 children will be fresh under the correction. Naturally expired23East parents appear among the latest floor lineages in71 and78 rooms respectively (ties allowed). This is observed lineage, not sole causal attribution.

Source outputs are `cleanup-floor-audit.json` in `training-v4-cooperation-combined-dims-random-200` and `training-v4-cooperation-dim29-random-200`. Raw boss results for those runs: reported average196.380328sec, min184.8sec, max206.4sec (183 eligible,17 wave19 exclusions),171/200 within3:22; and195.421277sec, min187.2sec, max211.2sec (188 eligible,12 exclusions),180/200 within3:22. Reliability counts all rooms. Neither meets95%.

## Vetted human23East lifecycle

The46 pinned Mage recordings contain zero Mage primaries on the23East big: neither early magic Eye nor melee Scythe. It naturally removes at age55 in43/46. Those children are born during actual29 in29 rooms and actual28 in14; none are born during actual30 or31. Thus the hypothesis that humans normally admit30 before this parent's expiry is contradicted by this view. The separate42 pinned Ranger recordings are an overlapping view, not42 additional independent rooms.

Seven Ranger Pipe primaries service this parent in five Mage-view rooms, at ages29–50. The early parent removal at age26 in747b0a15 is compatible with an extra Dinh cast at206 targeting24East:23East HP16 becomes0 at207 without a primary on it. This is secondary-hit evidence, not a Mage23East primary. The other two early removals at48/53 are compatible with late Ranger Pipe service.

Native Scythe `sweepTargets` requires a size1 primary and size1 secondaries. A24East size2 Scythe cannot sweep the23East size2 parent under normal native rules; proximity alone supplies no explanation. No engine change is proposed. Human Dinh radius/HP observations remain compatible secondary damage, not exact kill credit when the recording does not attribute it.

The48 Mage Dinh casts during actual28/29/30 in45 pinned rooms mostly target26South bigs. The bounded radius/current-style screen finds41 melee26South observations (36 large HP drops without another primary),3 melee25East observations (2 large drops),3 melee25West and1 melee27East. Literal27East starts magic and follows AMAM, so a claimed27East ranged identity is an older parent's current phase or a split, not this birth27 parent. See `human-late-dinh-review.json` for observations and caveats.

## Potential ranged support during29/30

Both pinned views contain zero Mage normal ranged primaries on birth29 targets during actual29/30. Human ranged cleanup assistance exists later on fresh30/31, including non-singleton situations, but does not establish this earlier packet. A new Mage Pipe gap helper would be a user-authorized native recovery experiment, not a copied human rotation.

The read-only screen on `training-v4-west30-parent-lifecycle-random-200` finds48 ready/no-launch review frames in200 rooms with a positive-HP, stable ranged birth29 split within5, not ownNULL or known-lethal-covered.27 already select future own work;21 do not. Many apparently idle frames reserve the late Dinh. These are potential cases, not48 proven free attacks. Exact selected attack time, native action delay, movement/return budget, real special energy and packet protection must all be established before a candidate is justified. The screen and bounded frames are `mage29-gap-screen.json`; its source is `mage29_gap_screen.py` in the guidance-checks directory.

## Healthy30West lifecycle candidate

The isolated `training-v4-candidate-mage-west30-parent-lifecycle/policy-source` copied the root early29/separated-claims combined-dim control and changed only Mage. It combines permanent-melee age26 takeover with cooldown approach when own work is clear. Actual native witnesses showed ordinary small work taking a now-permanently-melee30West parent slot and ready-only helper selection delaying approach until its last request was unreachable. Essential own30/31East work, NULL rescue, dim, ownNULL, unknown leases and known lethal coverage remain protected. This is native recovery evidence: no Mage30West primary was found in the vetted views.

Root334 checks plus10 takeover and5 cooldown probes pass349. Candidate-local expectation updates reflect the intended lifecycle tier and preserve the archived probes. The exact Mage-only patch, base hashes, runner, logs and test copies remain inside that candidate directory. Root's same-source200+300 native validation completed500/500: reported453 eligible rooms mean195.427815sec, minimum184.8sec, maximum208.8sec;47 wave19 failures excluded from this average but counted in447/500=89.4% reliability. Raw mean327.128ticks, minimum308, maximum360. Those historical whole-wave results are promising behavior evidence, not timestamp-dim acceptance. Root is rebuilding the corrected timestamp controls on this base.

## Corrected timestamp29 control: first200 rooms

The root's corrected timestamp29-only control completes200/200.182eligible rooms average195.349451seconds (3:15.349451), minimum187.2seconds (3:07.2), maximum206.4seconds (3:26.4).18wave19failures are excluded only from that average;179/200=89.5% of all rooms meet3:22. Raw mean326.98ticks, minimum312, maximum360.

The timestamp-aware observed small-expiry floor averages317.72ticks, with actualboss-minus-floor mean9.26ticks, minimum0, maximum28. Only20/200bosses are exactly at the observed floor. The182zero19rooms have mean floor316.483516ticks and residual9.098901ticks; only one has floor>336. These observed floors still depend on the trajectory, but they show why the historical whole-wave floor conclusion cannot be carried into intended timestamp behavior.

Mage guidance checks observe11ordering200/200;19east-first195with5missing;all94physical26/27overlaps have known lethal26coverage. Required23South,24South/East/West and26West all200 enter combat death. One25West remains positiveHP to expiry with Mage available and needs classification. Root's dim audit sees zero dimmed primary attacks and235legitimate attacks on birth<=29during31; those late births are fresh under the user-confirmed predicate.

Artifacts are `mage-timestamp-floor-audit.json`, `mage-guidance-audit.json` and root's `cleanup-dim-audit.json` in `training-v4-timestamp-dim29-control-random-200`. No acceptance claim is made from this single200cohort.

## Isolated required24East recovery

Historical same-source fresh300seed68515107335293 has untouched24East136, born204,16HP, no MageNULL and no pending coverage. At233–235 Mage approaches25West141 while24East is close; native food blocks those action ticks. At243 both parents are adjacent but generic25West gets Scythe; at24825West14HP gets the next Scythe while24East reaches age44. The first parent receives no primary before normal age49eligibility ends, then expires. This is actual target competition, not inferred from a slow aggregate mean.

The separate `training-v4-candidate-mage-late24east-recovery/policy-source` copies corrected combined timestamp350and changes onlyMage. After24East's named20-age window, its surviving melee lifecycle gets priority ahead ordinary25West retries/smalls, while uncovered legal26Westmagic suppresses the extra tier. Mandatory packets still execute before selection and ordinary cooldown/return budgets still constrain candidates; no deadline is extended.361checks pass.11focused checks on the unchanged corrected control produce3reds (243/248selection and recovery when26is already lethally covered). The candidate is unmeasured and remains separate from the timestamp control/candidate batches.

## Ready Eye versus a moving split approach

Native map-only tests use the real `NylocasRoom.template` instance and `RayCastValidator`. Both control rays from3295,4251to3292,4253and3299,4244 pass. Three combined-cutoff witness rays have since been added to the same parameterized fixture. Root reports the full native suite, including all five rays, passing49checks. No runtime instrumentation or engine behavior changed.

The decisive source detail is that `NylocasBehaviour.split` assigns children a random pillar. These are pillar walkers, with `fighting=false`, not fighting aggros. They still move before interaction. `AttackTiming.position` returns the current position unchanged for Lane.SPLIT, so a modeled immediate Sulphur request can resolve as native chase.

Combined-cutoff seed191996532081100at272 is exact: Mage3296,4247 selects30melee split191at3297,4244, but native only moves to3297,4245 without attacking. Fresh29Magic183born254is at3292,4244, within4and uncovered, after cutoff248. The ordinary ready Eye option is real own work, not an invented older-age bonus.

`training-v4-candidate-mage-ready-magic-before-melee-chase` is isolated on corrected combined350. Atactual31only, an ordinary nonadjacent Sulphur split approach may yield to an already-ready, unobstructed ordinary Eye small. It leaves named packets, required parents, Scythe AoE, partnerNULL rescue, adjacent melee and attack cooldown unchanged; existing same-wave approach intent cannot overwrite that selected cast.364checks pass,350existing+14new. The unchanged control fails two focused checks: native272selection and same-wave stale melee intent.

Its first independent200normal rooms complete200/200:186average-eligible,14wave19excluded, reported195.212903seconds, minimum184.8seconds, maximum211.2seconds. These small mean differences do not decide policy quality. Behavior against retained timestamp500 is more specific: cleanup Eye launches5.670vs4.716perroom; a bounded screen of Eye launches with a nearby newer melee split rises0.920vs0.120perroom; actionable no-launch nonadjacent Sulphur chase falls0.750vs1.044perroom. Late29Magic expiry falls21/200(.105perroom)vs87/500(.174);Melee12/200(.060)vs42/500(.084). Allfreshsmall expiry46/200vs161/500. Independent seeds can contribute to each difference; fresh validation remains root-owned.

The audit is `tools/research/nylocas_mage_cleanup_chase_audit.py`; outputs in each run distinguish observed launches from the bounded replacement screen. The screen is not exact counterfactual attribution. A separate immutable margin variant limits replacement casts to current range<=7ratherthan8to leave one native walking step;366checks pass. V1is frozen and measured separately.

## Avoidable mixed Scythe secondary NULLs

The V1cohort's14fresh ownNULL lifetimes all first appear one tick after MageScythe on a different primary; none follows an Eye replacement.11occuractual31and3before31. Thus increased raw ownNULL counts are compatible with mixed Scythe secondary hits, rather than a direct Eye wrong-style regression. Bounded evidence is `mage-fresh-own-null-classification.json` and `mage-mixed-scythe-witnesses.json`.

Exact seed122457299293389at279: Mage3301,4251Scythes newborn31melee202at3301,4252, with another melee198stacked there. New31Magic201is at3300,4251, steps into the native arc and becomes ownNULL280. Other witnesses are367at289and399at301. Native `ScytheOfViturWeapons.sweepTargets` selects size1secondaries on the primary tile/left/right, up to two, with no style filter. Both smalls can move one normal walking step before native interaction; source collision and target ordering still control the actual hit.

`training-v4-candidate-mage-cleanup-mixed-cleave-safety` copies current retained timestamp baseline, without V1or late24changes. Onlyactual31generic small-cluster Scythe selection changes: use normalSulphur if a useful undimmed nonmelee small could enter the next-cycle arc. A conservative current Chebyshev gap<=3fromprimary covers primary1walk+secondary1walk+arc1. AlreadyownNULL, known-lethal-covered, dimmed, dying, zeroHP, expired, size2or imminent melee-flicker bodies do not veto. All named Scythes, parents and mandatory packets remain unchanged.363checks pass,350+13; unchanged retained control has five focused reds. This isolated candidate is not yet measured and intentionally trades possible AoE for preserving a legally serviceable future lifetime; native testing must assess that tradeoff.

## Wrapup handoff at the user's request

The user requested finishing the current mixed-cleave experiment and recording a handoff for another agent. Do not resume new batches or candidates from this note without explicit user resumption. Root owns the global handoff and final retention choice. Canonical remained corrected timestampcombined350when this was recorded; this Mage agent made no canonical policy edits.

The Eye-before-chase V1same-source200+300finishes500/500:461reported-average eligible,39wave19failures excluded only from that average;reported194.957050seconds (3:14.957050), minimum184.8seconds (3:04.8), maximum211.2seconds (3:31.2);451/500=90.2% all-room timely. Root reports49native regression checks green. V1is validated but **not promoted** at wrapup. Goals remain unmet; pooled averages alone are not a retention decision.

Pending pre-cap research is a screen only. The already-started read-only screen of retained combined200finds85actionable no-launch ordinary nonadjacent Sulphur split frames with a nearby healthy ownMagic split within7, among428ready no-launch nonadjacent split chases. Actual-wave counts:17=25,10=15,9=11,19=10,5=8,18=7,12=5,16/6/8/13=1each. Exact191996532081087at68during10: Mage3296,4246chases27while9Magic36/37are at3290,4248and3291,4249. No nativeLOS or exact Java packet-budget reproduction has been performed for these earlier frames. No before31extension candidate exists. Bounded previous snapshots and confirmed attack history are preserved in `mage-precap-split-chase-screen.json`; script `precap_split_chase_screen.py` is in the guidance-checks directory.

Current Mage source SHA256identities:

| Artifact | SHA256 | Status |
|---|---|---|
| Canonical retained timestampcombined |`1af7387a948c48945e95637eb0c11e0d902683d168977c6f75016b2ddd901f8a`|350baseline; current at handoff|
| ReadyMagic-before-chase V1 |`e508ad8d1c62b860abf1433f05993b03dedb89da3003189909343aa3b568899f`|364checks;500native; not promoted|
| Separate seven-tile margin variant |`d24e8d3a18bbf68ef76eea04eb6d7d0a888a62148b1bc5959c21de76c541815b`|366checks; unmeasured|
| Separate late24East recovery |`656895cb82bd5cda788fb6d7ab89d3764d52a6dfaf6bbd8ab6501b48b51bf91e`|361checks;200native; not promoted|
| Separate mixed-cleave safety |`0af803689444937d3248f97fd3d7baf5ee5b5a5a93cfd915f28378d59dbf6cbd`|363checks; current200experiment; not promoted|

All named candidates are under `.state/simulations/` with the full directory names given above; each contains its exact policy source, Mage-only delta and role-check log. The late24candidate200has44late24East requests in39rooms and allrequired23–26parents combat-dead200/200. Its single22Dinh omission and18lateDinh omissions were not yet individually classified; they are not established regressions. No new tests, policies or batches should be inferred as authorized after wrapup.

### Final mixed-cleave200 evidence

The requested final experiment completes200/200with no deaths.185average-eligible rooms average195.645405seconds (3:15.645405), minimum184.8seconds (3:04.8), maximum211.2seconds (3:31.2);15wave19failures are excluded only from that average.178/200=89%all-room timely. This is an isolated cleave-safety candidate on retained timestampbaseline, not a V1combination, and remains unpromoted.

Five fresh ownNULL lifetimes are observed. Four first appear before actual31, outside this candidate's scope. The sole actual31NULL is seed151137184917110/key198at293: it was8HPwith22already-queued damage at291/292, so it correctly does not veto useful Scythe AoE. No unprotected useful fresh post31NULL is observed in this200. All five follow Scythe on another primary. This is narrower and more informative than claiming all NULLs should disappear.

Actual31small Scythe launches19/200(.095perroom)versus retained102/500(.204); smallSulphur876/200(4.38)versus2130/500(4.26). Freshsmall expiries82/200(.410)versus161/500(.322). These independent cohorts show the expected weapon/NULL behavior but do not establish an overall performance gain; less cleave can also cost useful damage. No fresh validation beyond200was started after the wrapup request.

Both12packets,21barrage and22Dinh occur200/200;lateDinh183/200. All required23South/24South/East/West/25West parents enter combat death200/200;26West198combat-dead with2MageNULL survivors, generated before this actual31scope.11ordering200/200,19east-first194with6missing;all110physical26/27overlaps have known lethal26coverage. The last slow tails include a29Magic natural expiry and freshMagic already covered by lethal flights, rather than an observed new refusal. The older movement audit's whole-wave final-target labels must not be interpreted as timestamp freshness.

Final output files are `mage-cleanup-chase-audit.json`, `mage-final-guidance-audit.json`, `mage-final-movement-audit.json`, `mage-cleanup-weapon-counts.json` and `mage-fresh-own-null-classification.json` in `training-v4-mage-cleanup-mixed-cleave-safety-random-200`. They are copied with hashes to the portable evidence folder. Root's handoff is `docs/nylocas-training-handoff-2026-11-25.md`. Training stops here as explicitly requested.
