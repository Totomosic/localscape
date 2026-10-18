# Implementation and recording verification

Implemented 2026-10-18 on `nylo-pathing-corrections` in both the workspace and its `upstream/rsmod` submodule. Comparison baselines: workspace `07387c2114c287766f2e5a1ab9fa1289a7ff4c54`, rsmod `bbb4b2a654fcf3813bf127c14682f571cdfb4dd0`. The workspace integrates into `master`; the `rsmod` submodule integrates into `localscape`. No server deployment or cache repack was required for these runtime behavior changes.

## What changed

The implementation follows the common mechanisms identified by the [research](README.md), without wave/UUID-specific production rules:

1. **Ordinary big removal clears dynamic NPC collision across its native 2×2 footprint.** Movement and reservation addition still use the southwest anchor. This permits a living stationary neighbor to become nonblocking, with later arrivals stacking on it. Cleanup runs through the encounter's NPC-delete event, before ordinary deletion finishes and before split births add their reservations. This is the leading recorded-history hypothesis implemented at deletion; earlier collision-release sub-phases remain unresolved. Fighting bigs, bosses and unrelated NPC cleanup retain existing behavior.
2. **Pillar movement approaches an outer-aligned geometric goal and checks nearby contacts first.** A free inner contact can fill without a global search for another vacant face. Blocked arrivals choose a legal neighboring step, allowing straight and diagonal retries. The random selector is explicitly provisional; the recordings establish retry shapes, not that the server uses this distribution.
3. **Dynamic NPC collision is tested at the chosen destination, while terrain corners remain checked.** The implementation follow-up found [624 screened diagonal transition/side witnesses](verification/corner-evidence.json). Ordinary entered pillar attackers use `PassThru` in shared movement so it does not reapply NPC corner blocking after the script validates its single step. Their script rejects occupied destinations and uses ordinary terrain validation. Incoming lanes retain `BlockedNormal`; fighting NPCs retain `Normal` and their existing footprint.
4. **Static terrain detours retain all route waypoints.** They do not search around dynamic NPC congestion. Exact routes require `success && !alternative`, so a closest-approach result no longer masquerades as contact success at this call site. A multi-bend terrain regression exposed and corrected an initial waypoint-retention error before completion.

The production change is confined to [NylocasBehaviour.kt](../../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt). Generic movement and route-result semantics were not globally changed.

## Real-engine comparison against recordings

[nylocas_pathing_fixtures.py](../../tools/research/nylocas_pathing_fixtures.py) verifies the pinned raw API hashes and classification provenance, then exports [31 contexts](../../upstream/rsmod/content/areas/dungeons/nylocas/src/integration/resources/nylocas-pathing.tsv): both east bigs from waves 25/27 spawns in all 14 recordings, plus the small-NE, big-SE and big-SW congestion fixtures. They contain 945 consecutive focal transitions and surrounding observed NPC footprints.

[NylocasReplayTest](../../upstream/rsmod/content/areas/dungeons/nylocas/src/integration/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasReplayTest.kt) exercises the actual AI, native cache terrain and shared movement execution, rather than a Python copy of the algorithm. Each transition is checked under two surrounding-blocker snapshots: before the step and after the step. Those are two timing assumptions, **not bounds on every possible actor interleaving**. Thus there are 1,890 checks of 945 source transitions, not 1,890 independent observations.

The baseline was rerun with the original production `NylocasBehaviour.kt` loaded from pinned commit `bbb4b2a`, followed by automatic restoration of the revised file. The rest of the harness and fixture inputs were identical. [Baseline output](verification/baseline.tsv), [revised output](verification/revised.tsv), [per-case comparison](verification/comparison.json).

| Conditional next-step comparison | Original policy | Revised policy |
| --- | --- | --- |
| All checks matching recorded destination | 1,703/1,890 (90.1%) | 1,812/1,890 (95.9%) |
| Checks where the recording moves | 131/318 (41.2%) | 240/318 (75.5%) |
| Checks where the recording stays still | 1,572/1,572 | 1,572/1,572 |

The revised policy adds 109 matching checks, all on moving transitions. Stationary holds dominate the full percentage, so the moving-only row is essential. All disagreements, including exact alternative predicted/recorded tiles, remain in the TSV outputs. The deterministic test seed is the testing framework's `123456`; the aggregate gate requires at least 94% agreement. This is a regression floor, not a statement of complete game accuracy or a statistical confidence interval.

The second comparison **continues the focal actor without resetting its position each tick**, while supplying observed surroundings. For the 27 wave 25/27 spawns with sustained recorded contact, both timing assumptions require eventual contact and reject contact on any face except SE's north side. All 27 cases pass in both phases; fixture cardinality is asserted. The original policy fails `1073635f-27` in the before-snapshot phase by contacting another face. The 28th spawn is censored by the recording before sustained contact and is included in conditional comparisons, but not required to complete an arrival.

## Regression tests and wider verification

The collision witness was first reproduced as a failing test: a big at `(3298,4243)` is removed while a positive-HP small remains at `(3299,4243)`; another small later enters that anchor. It now passes without killing or moving the incumbent. Existing cleanup expectations were corrected to account for broader clears and subsequent split-birth reservations.

Focused tests additionally verify:

- Both sizes can step diagonally past an NPC on a side tile; occupied destinations and blocked terrain corners still reject the step.
- A crowded north approach produces both straight and diagonal local motion without seeking an available west face, then takes a nearby vacancy.
- Settled actors remain still; empty arrivals retain native-size contact and the established outer endpoint/diagonal-first approaches.
- Natural expiry frees a waiting approach; fighting bigs retain full reservation geometry; overlapping ordinary bigs remain supported.
- Static obstacles, including a multi-bend retreat, retain valid detours.

Some older tests assumed that successive arrivals must fill every contact and always bounce between exactly two cardinal tiles. Their arrangements now explicitly fill the pillar before testing congestion, and assert legal local steps and vacancy admission. This preserves the occupancy/release checks without encoding the superseded pacing policy.

**345 integration tests passed:** 288 in the complete Nylocas encounter module,48 Nylocas combat integration cases, and 9 development-command cases. Kotlin formatting was applied to the changed module; final format/diff checks are recorded with the completed change. No temporary production debug logging remains.

Run through the repository's configured JDKs:

```bash
python - <<'PY'
from tools.dev import gradle
gradle('rsmod',
    ':content:areas:dungeons:nylocas:integration',
    ':api:combat:combat-scripts:integration', '--tests', '*Nylocas*',
    ':content:other:commands:integration', '--tests', '*NylocasDevelopmentCommandTest')
PY
python tools/research/nylocas_pathing_comparison.py
```

Regenerate source-derived fixtures/evidence separately:

```bash
python tools/research/nylocas_pathing_fixtures.py
python tools/research/nylocas_corners.py
```

## Review

The [code-review skill](/home/jordan/.agents/skills/code-review/SKILL.md) was used for independent standards and spec reviews of the uncommitted changes against the pinned baseline. Its actual installed source is `/home/jordan/.agents/skills/code-review/SKILL.md`; the workspace does not vendor that skill.

### Standards

No documented hard standards violations were found. Two judgement findings were resolved: the replay harness originally lacked a failure gate, and its retry-distance filter was mathematically ineffective/redundant. The final review found no material defect in the encounter-scoped destination validation or retained terrain route. Its focused diagonal-test recommendation was implemented.

### Spec

One finding was resolved: printed replay counts alone could not catch regression, and initial coverage omitted wave 25 in most recordings. The final harness has a 94% floor, all 14 wave 25/27 inputs, 27 asserted uncensored arrival cases, and continued-trajectory north-contact checks. No remaining spec defect was identified. The retry selector remains a documented approximation, as the research requires.

## What this does not establish

This is a substantive correction with improved recorded-step agreement, not exact raid reconstruction. Surrounding NPCs are supplied from snapshots rather than simulated from their true server slot order; HP/death phases can lag; blocker flags are rebuilt in the replay harness, so persistent collision holes are tested separately through actual cleanup regressions. The continued focal tests still depend on those observed surroundings.

The remaining 78 conditional mismatches concentrate on movement. Exact retry ordering/randomness and early collision release relative to visual despawn are unresolved. Broader big cleanup at deletion is a supported hypothesis, not a recovered server instruction. Full-room pacing frequencies also require matched spawn, player-kill, split and lifecycle histories before comparing them fairly. The coordinate-level boundary-overhang lead remains unconfirmed and no room-boundary expansion was added.
