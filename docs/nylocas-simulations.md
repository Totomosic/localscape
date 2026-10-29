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

The endpoint is **boss spawn after wave 31 and cleanup**. Both existing client
agents stop there too. Boss combat is outside these policies. A completed trial
can include NPCs that expire naturally; it does not mean every NPC was killed.
Deaths and tick-limit timeouts are reported separately. Setup errors abort the
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
incoming wave. Pre-cap covers incoming waves 1–20 (cap 12), post-cap covers
21–31 (cap 24), pre-29 covers 1–28, and wave-29 stalls specifically delay
admission to wave 29. Averages include every trial, including partial counts
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

`./dev build-client` copies the shared library into the RuneLite source tree.
The client plugins retain their event subscriptions, UI, instance normalization,
native input, equipment capture, and client acknowledgements. The headless
adapter in `api/combat/combat-scripts/src/integration/kotlin/org/rsmod/api/combat/`
feeds the same policies from authoritative server snapshots. It submits attacks,
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

The server adapter defines fixed melee/ranged/magic gear sets (Torva, Masori,
and Ancestral with their corresponding accessories), the policies' weapons,
99 base combat/prayer stats, and the server's existing 250 starting HP option.
It provisions charged weapons, dragon darts/arrows, barrage runes, and four
four-dose prayer potions per player. It uses normal randomized damage, not the
server's maximum-hit option. Prayer selection and potion thresholds use the
shared policy; activation, drain, and consumption run through server scripts.
It does not add food or restore HP between ticks.

The initial fixture is deliberately independent of saved client equipment.
Change the explicit fixture in `NylocasServerAgents.kt` and trial setup when
comparing gear/supply conditions. Keep seeds, source hashes, and conditions with
the results. The server currently has provisional natural-expiry explosion
damage/radius rules and does not model pillar damage/collapse. Measurements
reflect those existing server limits. Live RuneLite trials
also include input and observation delays that an in-process server run avoids.

Use separate report directories for comparisons. To reproduce trial N, run one
trial with its CSV seed and the same policy/server sources and fixture. Compare
gameplay columns; wall-clock timings naturally vary. Throughput depends on the
machine and JVM warmup; the report measures it rather than assuming a target.
