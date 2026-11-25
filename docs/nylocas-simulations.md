# Headless duo Nylocas trials

Run the ranger and mage against RSMod's actual game cycle, without RuneLite,
network players, a display, or a 600 ms tick delay:

```bash
# Requires the server/cache/JDKs prepared by ./dev setup.
python3 tools/simulate-nylocas.py --trials 1000 --seed 1 --offline

# Choose a report directory and limit unsuccessful trials.
python3 tools/simulate-nylocas.py --trials 10000 --seed 1001 \
  --max-ticks 1000 --output .state/simulations/comparison
```

The first invocation compiles the server. Each batch loads the cache once into
one JVM, then creates an isolated game scope for each trial. Combat, projectiles,
pathfinding, flickers, cap stalls, splits, deaths, natural expiry, and boss spawn
use the server implementations. Trial seeds control combat/encounter randomness,
the core RNG, and shuffled player processing order. The simulation uses the live
shuffle interval rather than the integration harness's usual fixed RNG.

The endpoint is **boss spawn after wave 31 and cleanup**. Boss combat is outside
these policies. A completed trial
can include NPCs that expire naturally; it does not mean every NPC was killed.
Live companions also have a basic boss phase; the benchmark still stops at spawn. Deaths and tick-limit timeouts are reported separately. Setup errors abort the
batch instead of producing misleading performance results.

## Reports

Each output directory contains:

- `trials.csv`: one row per trial, including its seed and terminal status.
- `summary.json`: machine-readable aggregate statistics, run configuration,
  policy source hashes, and throughput.
- `report.md`: readable statistics table.
- `server.log`: Gradle and server diagnostics.

Completion mean/median/min/max use successful trials only. Times are reported in
ticks and game seconds (0.6 seconds per tick). Throughput includes per-trial
setup but excludes compilation and the initial cache load. The invocation time
in JSON includes those startup costs.

Stall averages, medians, minima and maxima are included in JSON and Markdown,
with per-trial counts in CSV. Each failed population-cap admission check counts
as one stall (four ticks of delay), including repeated failures for the same
incoming wave. The training brief names a stall after the last admitted wave: pre-cap is
last-spawned 1–19 (incoming 2–20, cap 12), post-cap is 20–30 (cap 24).
The historical `wave_29_stalls` CSV column means incoming wave 29, hence a
**wave-28 stall**. `stalls_by_last_spawned_wave` stores all counters for waves
0–30. The consistency metric checks waves 21–27 separately from the brief’s
explicitly allowed wave-28 retries. `pre_29_stalls` retains its legacy incoming
1–28 definition. Averages include every trial, including partial counts
from deaths and timeouts. Old CSVs without these counters show `n/a`; missing
values are excluded from stall statistics.

Idle ticks count ticks without a confirmed primary attack, including cooldown
and movement, from each role's first confirmed attack through the final living
nylo's death. Entrance time, terminal death animations and the final boss wait
are excluded. A temporary gap while a dying big produces children remains in
the window. Ready idle ticks count the subset where policy cooldown had elapsed.
Reports also give the fraction of completed trials below 20 ready-idle ticks
and their 95th percentile; deaths cannot improve that fraction by ending early.
Required packet holds still count. Reports made before this window change
included the entrance and boss wait, so their idle counters are not directly
comparable. New summaries record the window in run metadata.

`overlapping_primary_flights` counts confirmed primaries launched while the other
role's own-flight lease is active. Native claims now follow actual non-thrall hit
completion, including splash/zero and trailing splats, with bounded cancellation
recovery. Missing legacy overlap counters are unknown. Overlaps do not by
themselves establish wasted damage.

Same-tick double ups count both agents launching primary attacks at the same NPC
on the same tick. Shared targets count distinct NPC lifetimes attacked by both
agents at any time. A useful follow-up attack also qualifies as a shared target.
Positive damage rolls include AoE victims and multiple splats before impact;
they are not kill counts or damage actually applied. NPC identities remain
distinct when the server reuses a slot.

Regenerate reports from an existing batch:

```bash
python3 tools/simulate-nylocas.py --report-only .state/simulations/comparison/trials.csv \
  --output .state/simulations/reports
```

## Shared policies and adapters

The canonical policy sources live in the server submodule at
`api/nylocas-agents/src/main/java/org/localscape/nylocas/`. This Java library has
no RuneLite or RSMod dependencies. It contains the two strategies, ranger's
pre-cap planner, flicker predictions, source-room tiles, and prayer/supply
decisions. Strategy changes are made here once.

Each role exposes `decide(room, tick)`, `attacked(decision, ...)`, and `reset()`.
The room is a mutable observation snapshot with persistent crab lifetime
objects. Adapters update position, live style, observed HP, wave births,
cooldown, obstruction, and partner claims before deciding. Coordinates are
normalized to the source room, with north/south represented by `Tile.y`.
An NPC's stable key identifies its lifetime; its slot/index alone is insufficient.

Decisions name a target, weapon, special-attack flag, staging tile, optional
approach target, and rule/reason. Only a confirmed attack calls `attacked` and
completes a rotation rule. A rejected request or movement does not complete it.
Ticks follow the existing policies' request/next-observation convention.
Create fresh strategies and rooms, or reset both, between encounters.

The custom client has retired its mage and ranger plugins. The build copies the
canonical library into test sources to retain policy and replay coverage; it is
excluded from the packaged client. The production controller in
`content/other/nylocas-bots/src/main/kotlin/org/rsmod/content/other/nylocas/bots/`
feeds the policies from authoritative server snapshots. The thin headless input
adapter remains in `api/combat/combat-scripts/src/integration/kotlin/org/rsmod/api/combat/`.
Live players can add a companion with `::nylobot range` or `::nylobot mage`;
see [bot setup and lifecycle](nylocas-bots.md). It submits attacks,
movement, prayers, and supplies through the server's real input handlers,
acknowledges server attack events,
and walks during staging without accidentally attacking an approach target.
The harness supplies no human input and starts both players through the barrier.

The dedicated Gradle entry point is:

```bash
cd upstream/rsmod
./gradlew :api:combat:combat-scripts:simulateNylocas \
  -Pnylo.trials=100 -Pnylo.seed=1 -Pnylo.maxTicks=2000 \
  -Pnylo.output=/absolute/path/trials.csv
```

Ordinary integration runs skip the batch job. It always executes when explicitly
requested, even if its build inputs are unchanged.

## Baseline conditions

The controller provisions the role loadouts in [mage-strategy.md](../mage-strategy.md)
and [range-strategy.md](../range-strategy.md): ranger uses elite ranged Void,
Dizana’s quiver, anguish, avernic treads (max) and Lightbearer; mage uses
Torva/Oathplate/blood fury for melee/range and Ancestral/occult/confliction for
magic. Equipment switches use ordinary finite inventory transactions and
preserve charge-bearing items. Each role receives weapons, ammunition, runes
and four four-dose prayer potions once. NPC attack, movement, protection prayers,
drain, special regeneration and potions use normal server handlers/scripts.

Base/current combat levels start at 99. The historical benchmark defaults to the
server’s 250 starting-HP raid option; `--starting-hitpoints 99` measures normal HP.
`--prepot` consumes supported super attack/strength/defence, ranging and magic
potions through held-item handlers before the timed room; it is a distinct
fixture condition, recorded in JSON. The potion-only control reaches 103 Magic.
`--server-boosts` instead loads the live world's existing `LoginScript` and
`StatRegenScript`: both players receive the same maintained 118 melee/112
ranged/112 Magic and passive regeneration as other live players. This behavior
predates the bots. Keep this profile separate from earlier potion-only controls,
which omit both scripts. New CSVs record final Magic levels.

Normal-HP bots receive five finite manta rays after initial equipment and eat
through ordinary handlers at 65 HP or below. They consume items and incur the
native bite/attack delay. The 250-HP profile receives none. Live companions start
with normal 99 HP unless their host selects the raid’s 250-HP option.

Both roles use longrange on powered magic weapons (Ayak and Shadow), rapid on
ranged weapons, and aggressive/strength on scythe, sulphur blades and claws.
Dinh's stays on Pummel because it has no strength attack style; Block prevents
attacks. Ice Barrage uses the spell's fixed ten-tile range, independent of the
Ancient sceptre's melee stance. Server regression tests resolve every applicable
weapon against its actual weapon category.

The initial fixture is deliberately independent of saved client equipment.
Change the explicit fixture in `NylocasBotController.kt` and trial setup when
comparing gear/supply conditions. Keep seeds, source hashes, and conditions with
the results. The server currently has provisional natural-expiry explosion
damage/radius rules and does not model pillar damage/collapse. Measurements
reflect those existing server limits. Live RuneLite trials
also include input and observation delays that an in-process server run avoids.

Use separate report directories for comparisons. To reproduce trial N, run one
trial with its CSV seed and the same policy/server sources and fixture. Compare
gameplay columns; wall-clock timings naturally vary. Throughput depends on the
machine and JVM warmup; the report measures it rather than assuming a target.

## Training v2

See [training measurements and reproduction](nylocas-training-v2.md) and the
[vetted Blert evidence](nylocas-training-v2-evidence.md). Each invocation snapshots
Java policies into its output directory before compiling, preventing concurrent
policy edits from changing the experiment or its source hashes. Output directories
must be fresh; `--policy-source <java-source-root>` selects an alternate compatible
policy tree. Use `python3 tools/analyze-nylocas-training.py <traced-run-directory>`
to audit wave milestones, transition survivors and the actual-wave-31 cutoff.

Recent traces include native `NYLO_PRIMARY` launches (actual target, weapon,
pending decision, target HP, levels and equipment), `NYLO_ROLL` damage bounds and
`NYLO_IMPACT` applied damage. These are read-only observations. Run
`python3 tools/research/nylocas_primary_attribution_audit.py <traced-run-directory>`
to compare attack-tagged planner rows with native launches. Missing telemetry in
legacy logs is unknown, not zero mismatches.

When reconstructing decision feasibility, ordinary `TRACE` HP/position/NULL are
captured before the decision, while its lease fields are printed after the native
cycle and applied-hit release. Use the preceding snapshot's leases for such
audits; a current-row zero can represent a flight that landed during that cycle.

The harness registers multi-combat areas by normalized source coordinates. A
regression casts real barrage against two adjacent targets in an instance; older
runs with instance-coordinate registration suppressed secondary damage and must
not be treated as valid multi-combat training results.

## Reported boss-time sample after the wave-19 review

Jordan's review rule excludes entire completed rooms with any last-spawned-wave19 stall from the reported boss-time average. `boss_time_benchmark` records eligible completion ticks/seconds, excluded completed rooms, unknown wave-counter coverage, and wave19 policy-failure counts. Missing counters cannot establish a clean sample. The Markdown headline uses this eligible sample for average/minimum/maximum; every excluded room remains a policy failure with a zero-failure target.

`completion_ticks`, `completion_game_seconds`, original CSV rows, completion rate, deaths and all-room consistency/stall metrics retain their raw definitions. Reports show raw timings separately. This reporting rule does not alter native encounter caps, damage, resources or simulation behavior. Historical immutable reports retain the definitions used when generated.

The cleanup reliability objective is at least95% of **all trials** completing boss spawn by202seconds/3:22. `training.completed_boss_at_most_3m22`, `boss_at_most_3m22_rate_all_trials` and `boss_95pct_at_most_3m22_goal_met` report it. Deaths/timeouts do not qualify. Wave19stall rooms stay in this timing denominator and remain separate policy failures, even though they are excluded from the reported boss-time average.
