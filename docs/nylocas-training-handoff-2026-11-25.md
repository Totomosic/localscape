# Nylocas training handoff — 25 November 2026

## Stop state and reading order

Jordan explicitly asked to wrap up, finish the current experiment and leave this handoff. The final native200 cohort completed, and no further training batch was started. Training is paused at Jordan’s request. Do not automatically resume training from this document; wait for Jordan’s explicit resume.

Read first: [manual guidance](nylocas-training-guidance.md), this handoff, [original goals](../nylo-training.md), [mage loadout/strategy](../mage-strategy.md), [ranger loadout/strategy](../range-strategy.md), and [review questions](nylocas-performance-review-questions.md). The longer [resume history](nylocas-training-resume.md) contains historical checkpoints and stale “running/current” statements: inspect source and use this handoff as the latest stop state.

Preserve all uncommitted work in `/home/jordan/osrs` and its `upstream/rsmod` repository. Do not reset, clean, checkout old full candidates over current code, or remove guidance entries. No commits, pushes or deployment were performed during training; the subsequent local checkpoint is recorded below. User opened `code .` during wrapup; account for possible concurrent user edits by checking hashes before any replacement.

## Subsequent local commit checkpoint

After pausing training, Jordan explicitly requested committing the current work. The server implementation is committed as `21cccf08de8ccaab52e08f1423460393d9f5ca8f` on rsmod branch `nylocas-training-v2`; the parent branch of the same name records that submodule reference together with the training tools, tests, guidance and portable evidence. These are local checkpoint commits; nothing was pushed. Training remains paused and no experimental candidate was promoted.

The ignored `.state/` directory remains preserved locally, including full traces, candidate snapshots and wrap checkpoints. Portable evidence under `docs/research/nylocas/training-v2/` is included in the parent commit; use it when local runtime artifacts are unavailable.

## Canonical source and benchmark

The nine canonical Java files under `upstream/rsmod/api/nylocas-agents/src/main/java` match `.state/simulations/training-v4-retained-timestamp-combined-checkpoint/policy-source`. This was verified again during wrapup. The checkpoint contains `retention.json`, the complete preceding nine-file source, both repository diffs/status, a guidance snapshot and the exact strategy patch. Only MageStrategy and RangerStrategy differ from the preceding provisional-claim canonical checkpoint. No later isolated candidate was promoted.

Retained behavior includes the clarified combined timestamp dim, both-role partner-NULL rescue and literal-last cooperation, normal idle cleanup helpers, Mage West30 parent recovery and South29 opening during actual29. It preserves all earlier retained corrections. Native provisional claims use `provisionalClaimUntil` separately from real `pendingDamage`; do not restore the adapter that overwrote damage with -1 during same-cycle Mage planning. Canonical native controller remains the validated separated-claim version.

Retained validation: identical source, adapter and fixture across fresh independent 200+300 cohorts, 500/500 complete, no deaths/timeouts. Reported average **3:15.252980**, minimum **3:04.8**, maximum **3:28.8**, 453 eligible and 47 entire wave19-failure rooms excluded. **452/500 (90.4%)** of all rooms meet3:22. Raw all-completion average3:16.176, min3:04.8/max3:40.8; rawP95 3:26.4. Goals remain unmet.

## Latest instructions and acceptance rules

- Keep normal combat rules, random damage, finite supplies,99HP and the specified loadouts. No cap, expiry, stats, damage or resource grants.
- Run exactly one Gradle job at a time. Root coordinates native jobs; the requested roles are one Ranger agent and one Mage agent.
- Fresh independently randomized seeds for every new batch: omit `--seed`. Historical reproducible traces may be inspected, but do not use static benchmark seeds. Ignore small mean changes as a strategy rejection criterion; verify concrete behavior, matched fixtures and independent validation.
- Report average **and minimum/maximum** boss time. Any last-spawned wave19 stall is a policy error. Exclude the entire affected room only from the reported average, show its count separately, and keep it in reliability/death/timeout denominators.
- Goals:3:09 reported mean;95% of **all** rooms by3:22; zero wave19 failures; original pre-cap, transition, packet and cleanup consistency. Retained500 has48 late rooms:27 with wave19 failure and21 without. Eligible-only432/453=95.36% is context, never the reliability headline.
- **Dim is a birth-time cutoff**, not a whole-wave label. `29 dim` means born≤actualspawn29. `28+16` means born≤actualspawn28+16. Both use the later timestamp, inclusive, and activate only at actual31. Jordan explicitly confirmed that a child born252after wave29spawn240 remains fresh. Later29 splits participate in selection, NULL rescue and helpers. Literal-last counts every positiveHP/non-dying body, including dimmed bodies. Unknown29 timestamp conservatively falls back to wave≤29.
- Do **not** use the3:02.4 always-max-hit litmus for28+16 or comparisons involving it. Earlier max-hit and whole-wave29-label experiments remain historical; they do not validate the clarified timestamp strategy.
- Focus guidance: unexplained refusal to hit/approach living legal fresh targets, newer nylos, partnerNULL rescue, final-target doubling, required parents, South30 chin and31 rangers before melee. A zero roll is not NULL; proven queued lethal coverage is not an abandoned target.

## Completed comparisons

Times below use only completed rooms with known zero wave19 stalls. Every row completed without deaths/timeouts. All-time reliability includes excluded rooms.

| Policy | Completed | Eligible /19 excluded | Average | Min | Max | All rooms≤3:22 |
|---|---:|---:|---:|---:|---:|---:|
| RETAINED timestamp combined | 500/500 | 453/47 | 3:15.25 | 3:04.80 | 3:28.80 | 452/500 |
| Timestamp29-only control | 200/200 | 182/18 | 3:15.35 | 3:07.20 | 3:26.40 | 179/200 |
| Mage ready Magic before chase; NOT retained | 500/500 | 461/39 | 3:14.96 | 3:04.80 | 3:31.20 | 451/500 |
| Late24East recovery; NOT retained | 200/200 | 186/14 | 3:15.15 | 3:04.80 | 3:28.80 | 182/200 |
| Optional23East PIPE; NOT retained | 200/200 | 182/18 | 3:15.52 | 3:04.80 | 3:26.40 | 179/200 |
| Ranger actual30 NULL rescue; NOT retained | 200/200 | 179/21 | 3:15.71 | 3:04.80 | 3:33.60 | 171/200 |

Raw sources/reports are under `.state/simulations/` with the names below; pooled summaries include constituent run names and seed ranges. Portable completed metrics/audits and checksums are in `docs/research/nylocas/training-v2/cleanup-session-2026-11-25/manifest.json`.

## Final experiment: mixed-style cleanup cleave safety

Source: `.state/simulations/training-v4-candidate-mage-cleanup-mixed-cleave-safety/policy-source`; based on retained350, **without** the ready-Magic V1 or other pending deltas. Only Mage’s ordinary actual31 small-cluster weapon choice changes. Normal Sulphur replaces Scythe when a useful fresh nonmelee small could walk into next-cycle cleave. Exact native arc source and bounded sibling-NULL witnesses justify this experiment; a conservative current-distance≤3 screen accounts for primary/secondary walking plus the arc. OwnNULL, known-lethal, dimmed/dying/zeroHP/expired targets do not veto. Named12, all bigs and mandatory packets remain unchanged.363 pure checks pass; unchanged baseline has five focused red checks.

Final normal cohort: `training-v4-mage-cleanup-mixed-cleave-safety-random-200`, seeds151137184917001–151137184917200. **200/200 complete, no deaths/timeouts; mean3:15.645405, minimum3:04.8, maximum3:31.2;185eligible/15wave19 exclusions;178/200 (89%)all-room reliability.** This does not meet the goals. Small mean differences do not by themselves invalidate the idea. The candidate remains **unretained**, needs behavioral review and fresh validation before any promotion. Final audit: fresh MageNULL5lifetimes, four before31outside the candidate; the only actual31 case already had8HP covered by22queued damage and intentionally did not veto cleave. No unprotected useful fresh post31NULL was observed. Small-Scythe launches fall19/200 versus102/500 retained; Sulphur876/200 versus2130/500. Fresh-small expiries rise82/200 versus161/500. This is a possible safety/clearance tradeoff under independent seeds, requiring fresh validation rather than rejection on the small mean difference. Both12packets,21barrage and22Dinh occur200/200; all required23–25parents die,26West198dead/2MageNULL before31. Timestamp dim and ownNULL-primary violations: zero. See per-run `mage-cleanup-chase-audit.json`, `mage-fresh-own-null-classification.json`, `cleanup-dim-audit.json` and guidance/training audits.

## Candidates and concrete findings for resumption

All source paths in this list are under `.state/simulations/`. Do not copy full candidate trees over canonical: they have distinct bases and deliberately isolated changes.

1. **Mage ready Magic before moving-melee chase:** `training-v4-candidate-mage-ready-magic-before-melee-chase/policy-source`. Based on canonical timestamp350, only Mage differs. Generic actual31 nonadjacent Sulphur split chase yields to an already-ready, in-range, unobstructed ordinary Eye. Named packets, required parents, rescue, adjacent melee and ScytheAoE keep priority.364 pure checks pass; full native49/49 passes, including five exact-map ray rows. Normal200+fresh300 validate500 rooms (`training-v4-mage-ready-magic-pooled-500`). It remains **unretained**, awaiting final behavior review/possible composition. A separate unmeasured margin variant `training-v4-candidate-mage-ready-magic-before-melee-chase-margin` uses Eye range minus one walking step;366 checks. Do not assume it is the measured V1.

   In V1’s first200, Eye attacks rise5.670/room versus4.716 in retained500, no-launch split chase screens fall.750 versus1.044, fresh-small expiries.230 versus.322. These are independent-cohort behavioral observations, not paired causal estimates. All14 fresh MageNULL lifetimes in V1 follow Scythe on another primary one tick earlier, with none following Eye replacement; mixed cleave motivated the final experiment.

2. **Required late24East recovery:** `training-v4-candidate-mage-late24east-recovery/policy-source`; canonical350 base, Mage-only,361 checks. Exact historical failure68515107335293 at243/248 selects25West retry while adjacent unhit24East16HP ages39/44. The candidate gives required24East melee recovery after named age20 window priority over generic fillers, retaining required26Magic and mandatory work. Native200 has44late launches in39 rooms; all required23–26parents die200/200. One missing22Dinh still needs classification; pre31 source changes exist here, so review instead of assuming noise. No fresh validation yet.

3. **Optional23East spare PIPE:** `training-v4-candidate-timestamp-combined-optional-east23-pipe/policy-source`; canonical350 base, RangerStrategy-only fallback when ordinary candidate set empty during27/28, no PreCapPlanner/Mage delta. Seven focused checks pass. Native200:68launches in67rooms,38combat removals of23East versus1/500 retained,216/400 fresh children versus660/1000 retained. No proved abandoned mandatory target: missing24PIPE follows finite food; missing30chins are dimmed; two undimmed31rangers atage26 have queued lethal damage, a third is dimmed. Physical deadline misses remain visible. Needs fresh validation. Evidence: `ranger-east23-spare-pipe.md`, source-pinned human recordings and per-run lineage/packet JSON.

4. **Ranger actual30 fresh NULL rescue:** `training-v4-ranger-timestamp-combined-null-rescue-at30/policy-source`; canonical350 base, Ranger-only. Enable fresh-cutoff rescue during30, preserve packet budgets and release stale ordinary intent. Native200 shows one actual30 rescue plus eight during31; all eight ready/nonlethal-screen lifetimes selected, two skipped lifetimes already queued lethal. Zero unsafe primaries; six simultaneous double-ups all literal-last. Opening/24PIPE200; no31physical-melee survivor; one30chin intentionally dimmed. Needs fresh validation. Use `RangerPre31NullRescueCombinedTest`; the other29-only witness class has different cutoff assumptions and is not a permanent expectation for combined dims.

5. **Pre-cap fresh unhit intent:** `training-v4-ranger-precap-fresh-intent-timestamp/policy-source`. Frozen at wrapup; read [Ranger handoff](research/nylocas/training-v2/ranger-timestamp-wrapup.md) and candidate metadata. Focused4 baseline1red→candidate4green; guidance/replay18pass; broad127has only three other previously pending failures. The beam currently filters to a committed unhit older small even when a newer same-style split has an equally early attack. Candidate releases only that approach; confirmed damaged-survivor followups, previously attacked targets, bigs and named priorities remain protected. No native trial has been run. Do not mistake a snapshot/probe for full native replay.

6. **EarlyWest18Eye5:** historical `training-v4-candidate-parent-lifecycle-early18-eye` used the old whole-wave post31 dim; only PreCapPlanner eligibility changed. Ten focused checks plus349base pass359. Native200:45WestEye age5 shots versus0/500 lifecycle control, no proved remaining age5 idle refusal;19wave19failures remain. West parent combat-death age10.73 versus11.118 historical500, not a causal speed claim. Rebase only the recorded eligibility delta onto the inspected current strategy and run fresh normal trials; never restore that old full source. Other pre-cap failures remain.

7. **Potential pre-cap Mage chase issue:** read-only screen has85 actionable no-launch split-Sulphur frames with nearby healthy own-Magic splits in retained combined200, especially17/10/9/19. These require exactLOS, policy eligibility and budget reproduction; no pre31Mage candidate exists. Example191996532081087/t68 actual10. Script/data: `precap_split_chase_screen.py` in `training-v3-mage-guidance-checks`, and `mage-precap-split-chase-screen.json` in the retained combined200 run. Treat as leads, not85 proven missed attacks.

## Audits, tests and commands

Vetted primary evidence: five pairs,46 eligible Mage rooms and42 Ranger rooms (overlapping views, not88 independent rooms). `python3 tools/research/nylocas_duo_vetting.py` verifies pinned source hashes and eligible set. Human recording notes distinguish observed attacks from native recovery extrapolations.

Normal native cohort command (needs the existing approved local Gradle socket escalation in the managed sandbox):

```sh
python3 tools/simulate-nylocas.py --trials 200 --offline --trace   --starting-hitpoints 99 --prepot --utilities --server-boosts   --policy-source .state/simulations/CANDIDATE/policy-source   --output .state/simulations/NEW-RUN
```

Serialize Gradle. Use fresh random seeds by omitting `--seed`. Runner records nine policy hashes, native adapter hashes, fixture, seed range, finite resource profile and timing summaries. Compare these before pooling; require same source/adapter/fixture and disjoint seeds. Do not pool across the old adapter that overwrote pending damage.

Timestamp-aware cleanup audit:

```sh
python3 tools/research/nylocas_cleanup_dim_audit.py .state/simulations/RUN   --timestamp-wave29 --wave28-offset 16
python3 tools/research/nylocas_cleanup_floor_audit.py .state/simulations/RUN   --wave29-timestamp --wave28-offset 16
python3 tools/analyze-nylocas-training.py .state/simulations/RUN
python3 tools/research/nylocas_mage_guidance_audit.py .state/simulations/RUN
python3 tools/research/nylocas_mage_cleanup_chase_audit.py --help
python3 tools/research/nylocas_ranger_cleanup_rescue_audit.py --help
```

Omit only the28offset flag for timestamp29-only. Defaults without timestamp flags deliberately preserve historical whole-wave audits. `old_cleanup_attacks` in the legacy overall audit is merely birth-label≤29 and legitimately nonzero; use the timestamp audit for dim violations. Additional-time-dim counters now compare the actual baseline predicate, including late29 births between anchors.

Current role/native checks: retained350 filtered checks; MageV1 364 and native49; mixed-cleave363. The filtered base excludes four separately pending behaviors, not failures silently waived as goals: savedWest12through17, West11continuation, late18permanentMagic and pre-cap fresh intent (the new pending candidate resolves the latter). Current native real-map checks live in `NylocasMageCleanupRayTest.kt`; five rows pass. Final wrap Python suite:55tests, zero failures, four skipped (`.state/simulations/training-v4-wrap-python-checks.log`). Candidate-local suites preserve historical assumptions; do not mix test/source trees arbitrarily.

See the [Ranger stop-state notes](research/nylocas/training-v2/ranger-timestamp-wrapup.md) and [Mage stop-state research](research/nylocas/training-v2/mage-cleanup-floors-and-pre31-service.md) for exact candidate hashes, classified exceptions and source-pinned evidence. Final preservation snapshot: `.state/simulations/training-v4-wrap-2026-11-25/state.json`, both repository diffs/status, canonical9source and guidance/handoff copies. Both role agents stopped; no native or audit job remains.

No fresh cohort, test or handoff update means the training goals are achieved. Resume only on explicit instruction, then continue investigation and native validation until goals or another explicit stop.
