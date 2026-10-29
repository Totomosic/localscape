# Nylocas planning and idle validation

The current mage objective is to follow the rotation, avoid ranger-owned work,
and favor newer eligible birth waves. Ready-idle remains measured, but the
previous sub-20 target is secondary to those constraints. This revision changes
only the mage; existing ranger behavior is retained.

## Measurement window

Each role starts counting at its first confirmed primary attack. Counting ends
at the final living nylo's death, excluding terminal death animations and the
boss-spawn delay. A gap while a dying big produces children remains part of
combat. Ready-idle excludes weapon cooldown; required packet holds still count.
The report includes the under-20 fraction and nearest-rank 95th percentile for
completed trials. Deaths and timeouts are reported separately and cannot improve
that fraction by ending a run early.

Older reports included the entrance and boss wait. Their idle counts are not
directly comparable. New summaries record the window in `run.idle_window`.

## Implemented plan

Both policies retain a target lifetime key and weapon during an approach.
The mage retains a commitment within the same birth wave and rotation step;
newer birth waves and due packets can replace older work. A score-only
improvement cannot replace an equally early committed hit within that step.
Confirmation releases intent without assuming a kill.

Movement overlaps cooldown. Once ready, the policy can queue a useful approach
through native combat, allowing movement and an attack in the same cycle. The
client does not acknowledge an out-of-range approach from a stale continuous
animation. Blocked line of sight uses explicit repositioning instead.

Unresolved own projectiles do not attract another request on their predicted
impact tick. Observed HP changes and own hitsplats, including zero damage,
release that wait. The server adapter uses actual hit-application events.

Mage ordinary assignments retain their first/last request offsets. Preparation
can begin before an opening offset, without queuing an early attack. Newer birth
waves outrank older ordinary work, even when that requires more travel. Within
a wave, rotation assignments precede generic magic/melee cleanup. The explicit
deferred east wave-13 big at age 25 and the named timed packets retain priority.
Every primary target must be at most four waves older than the current wave.

The mage no longer borrows ranger-owned magic lanes, wave-16 lane flickers,
or arbitrary ranged leftovers, and does not open unassigned bigs for idle
reduction. The recorded west wave-15 dart target is an explicit rotation option,
subject to partner claims. Hard packet reservations, incoming-hit waits and
movement during cooldown remain intact.

Wave-29 work remains eligible during waves 29 and 30, but yields to newer
ordinary work. Once wave 31 actually spawns, wave-29 births are excluded,
including already-attacked survivors. Other births still obey the four-wave
limit. Pending native approaches are cancelled at either boundary. Predictions
never fabricate an actual wave through a population-cap retry.

## Attack-style correction

Powered magic weapons now use longrange, including the Shadow opener and its
pre-room setup. Ranged weapons use rapid; scythe, sulphur blades and claws use
aggressive/strength. Dinh's remains on Pummel because Block prevents attacks,
and barrage uses the spell's fixed ten-tile range. The shared client action
adapter now maps all four stance values to their matching combat buttons;
previously stance 0 incorrectly clicked stance 1.

Validation passes **153 client tests** and **30 server style/interaction tests**.
The style tests resolve each weapon against the actual server weapon category.
A ten-room smoke run (seeds 1–10) completed eight rooms with two deaths and no
timeouts; artifacts are in `.state/simulations/attack-styles/`. This small run
does not establish that the previously measured survival regression is fixed.
The rebuilt client is `client-1.11.19-local-683f77f49e49.jar`.

## Rotation validation before attack-style correction

The client build passes **151 strategy, replay, action, plugin and movement
tests**, including the unchanged ranger suite. Mage replay validation checks
all 62 pinned rooms, now including attack type 74 for the explicit west-15 dart
assignment. Ranger policy and plugin source hashes are unchanged by this revision.

```bash
python3 tools/simulate-nylocas.py --trials 100 --seed 1 --offline --trace \
  --output .state/simulations/mage-rotation
```

All **7,748 confirmed mage primary attacks** satisfy the four-wave age limit.
The trace audit found **zero primary attacks on the protected ranger lane
identities** and zero wave-29 mage attacks after wave 31. There were no same-tick
primary double ups. This does not promise that incidental AoE victims or targets
hit at different times are disjoint: the two policies shared 3.25 target
identities per trial on average.

The current batch completed **73/100**, with **27 deaths** and no timeouts.
Completed rooms averaged **374.74 ticks** to boss spawn. Mage ready-idle averaged
**65.64**, median 64 and p95 84 in the requested measurement window. These results
establish the target-selection constraints, but reveal a substantial survival
regression from the earlier idle-focused policy's 98/100 completions on the same
seeds. They do not establish a reliable live-room policy or a sub-20 idle target.
No ranger policy changes or simulation fixture changes were made to obtain them.

Artifacts are retained in `.state/simulations/mage-rotation/`: `summary.json`,
`trials.csv`, `server.log` and `policy-audit.json`. The fixtures include natural
expiry and randomized damage but do not model pillar collapse; see
[fixture assumptions](nylocas-simulations.md). Critical packet unit/replay checks
pass, but the trace does not guarantee every optional packet can find its
required live target and geometry in every room.

## Historical idle-focused results

The earlier idle-focused policy completed **572/600** trials, with 28 deaths and no timeouts.
Mean boss spawn among completed trials was **371.06 ticks**. Both agents had
zero same-tick primary double ups in this batch.

| Completed trials | Mage mean | Mage median | Mage p95 | Mage below 20 | Ranger mean | Ranger p95 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| All 600 seeds | 11.91 | 11 | 21 | 528/572 (92.3%) | 42.19 | 57 |
| Expanded seeds 101–600 | 12.12 | 11 | 21 | 434/474 (91.6%) | 42.01 | 56 |

These are ready-idle ticks in the clarified combat window. The initial 100 seeds
had 94/98 completions below 20, with p95 19. The expanded set provides the more
representative consistency check. Some outliers remain: the all-seed mage maximum
was 40. Ranger idle remains higher; this change does not establish a sub-20
ranger policy.

Across the 87 initial seeds completed by both the clarified-window baseline and
the final policy, mage ready-idle improved from **44.32 to 11.08**, ranger
ready-idle from **64.25 to 43.76**, and boss spawn from **380.64 to 370.85 ticks**.
The baseline completed 89/100 trials; the final policy completed 98/100 on those
same seeds. Paired figures exclude failures from either version.

The absolute wave-31 cutoff is retained at the user's explicit request, including
already-attacked wave-29 survivors. Resulting waits remain in the requested total.
The cap-retry regression came from seed 146: fresh children continued to be born
in wave 29 after its original predicted final-spawn time had passed. They lost
urgency and later became ineligible at wave 31. That idle-focused plan updated its
prediction at retries and kept those children urgent until the real wave marker.
The current rotation policy instead prioritizes birth-wave freshness and retains
the real-marker cutoff without a separate wave-29 urgency override.
The regression tests cover both a long stall and the predicted final-spawn tick.

Validation passed: **145 client strategy/replay/action/plugin/movement tests**,
**21 server interaction/idle-window tests**, and **7 report tests**. The client
was built and packaged locally as `client-1.11.19-local-efa8319802ce.jar`; restart
through the launcher to load it. No running client was restarted automatically.

Detailed distributions, per-trial seeds, fixture conditions, source hashes and
paired comparisons are in `.state/simulations/combat-idle-final/summary.json`,
`trials.csv`, and `evaluation.json`.
