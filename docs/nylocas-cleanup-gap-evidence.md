# Wave-31 cleanup and magic profile evidence

Reproduce offline with `python3 tools/research/nylocas_cleanup_gap.py`. The [portable JSON](research/nylocas/training-v2-cleanup-gap.json) contains per-room timing, every fresh mage attack, comparable alternative-target witnesses, terminal native NULL observations, decoded magic-stat witnesses, and input SHA-256 hashes. No gameplay or fixture changes were made.

Human rotation comparisons use the 46 eligible mage-corpus recordings from the [vetted list](nylocas-training-v2-evidence.md), including both roles. Additional magic-stat checks use the 42 eligible ranger-corpus recordings; these sets overlap and are not independent. The bot comparison is `.state/simulations/training-v2-nine-utilities-fresh-100`, seeds31–130, 100/100 completed, with its immutable policy snapshot. Human primary events and cached player updates are first-party replay data; for example [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas).

## Most of the gap precedes cleanup

| Mean ticks | Human46 | Bot100 | Difference |
|---|---:|---:|---:|
| Wave31 spawn | 264.26 | 280.72 | +16.46 |
| Boss spawn | 319.39 | 340.52 | +21.13 |
| Wave31→boss | 55.13 | 59.80 | +4.67 |
| Wave31→last NPC removal | 37.83 | 42.44* | +4.61 |
| Last removal→boss | 17.30 | 17.36* | +0.06 |

Human removal is raw event9/event122. Bot removal is estimated as one tick after the last appearance in the per-tick NPC snapshot, not a dedicated despawn event. The agreement of the remaining delay gives no indication that the boss-spawn delay itself causes the performance gap.

The user's cutoff remains **current wave31: no primary targets born at ≤29**. Human recordings do not follow that exact restriction: they contain 58 mage and 45 ranger such primaries during cleanup. Those launches are excluded from the fresh-target comparison below. Later-born splits remain legal regardless of their parent's birth wave. `MAX_WAVE_AGE=4` cannot exclude a birth30/31 target at current31; their wave age is at most one.

## Strongest cleanup candidate: earlier ordinary magic-small service

Fresh means birth30/31, current wave31, strictly before boss. These are launch counts and target ages, not attributed damage or kills.

| Mage attack | Human46 per room | Bot100 per room | Human target age mean / median | Bot target age mean / median |
|---|---:|---:|---:|---:|
| Eye | 5.11 | 5.18 | 10.77 / 9 | 16.04 / 13 |
| Sulphur | 2.61 | 4.43 | 19.27 / 19 | 13.84 / 9 |
| Scythe | 0.57 | 0.24 | — | — |

Eye volume is almost identical, but humans service fresh magic earlier and melee later; the bot does the reverse. Its generic score gives melee9,000 and magic6,500, then subtracts age×100, which also makes an aging magic small progressively less competitive against a newly born melee. This observation supports a **cleanup-only** experiment rather than a broad pre-cap priority change. Earlier 15-seed experiments made before the native potion/prayer bridge was corrected cannot settle the present resource-controlled question.

There are **360 of 443** bot cleanup sulphur launches, across **94/100 rooms**, with an alternative fresh magic split that is alive, non-dying, age<49, not observed native Mage-NULL, unclaimed, without an own in-flight hit, and at geometric Eye distance1–8. In 38 rooms, one of these alternative keys subsequently belongs to the last-positive-HP set. There are **57** such launches in **17 rooms** where an alternative is already observed **Ranger-NULL**, so the mage is the available partner. These are reproducible opportunity witnesses; the trace does not include per-target collision/line-of-sight or every planner travel result, so it does not prove all 360 alternatives could launch immediately.

Concrete witness, **seed34**, all at current31:

| Tick | Selected sulphur target | Alternative magic split196 |
|---|---|---|
| 320 | key205, age2, HP8 | age21, HP8, distance3, claim0, own-flight0, Mage-NULL false, Ranger-NULL true |
| 325 | key197, age26 | age26, HP8, distance8, same NULL/claim/flight exclusions |
| 338 | key204, age33 | age39, HP8, distance8, same exclusions |

At tick320 the mage is at `(3299,4246)`, magic196 at `(3299,4243)`. The latter remains in the last-positive-HP set at tick350 with HP8, still Mage-NULL false/Ranger-NULL true. The JSON contains full coordinates and competing candidates for independent native-feasibility verification.

**Falsifiable experiment:** during cleanup only, give a reachable, ready magic split earlier service when it is aging or the ranger is authoritatively NULL to it, preserving all cutoff, flight, claim, style, and native input checks. Verify that its confirmed Eye launches become earlier and that terminal fresh magic lifetimes shrink; compare identical seeds and resources for total time, cleanup time, survival and duplicate primaries. First test the narrower partner-NULL rescue if minimizing scope. Reject a timing regression. Do not grant damage or override native NULL rules.

Scythe positioning is a secondary candidate: all 26 fresh human cleanup scythes have cardinal primary offsets, compatible with the native 1×3 secondary arc. A simple nearby-melee count does not establish a valid arc. Bot trace player coordinates are a planning snapshot, while the confirmed-attack flag is recorded after the cycle; they cannot independently prove exact attack-time sweep geometry. Ranger humans also make 23 fresh melee primaries (16 claws, four scythe, three chally), whereas the bot makes none; weapons outside the requested loadout should not be introduced from that frequency alone.

## Terminal NULL status does not explain most cleanup tails

The bot's last-positive-HP set contains 131 NPC appearances across 100 rooms; tied terminal survivors mean these are not 131 independent encounters. At the room level:

| Terminal set | Neither role has an observed NULL | Ranger-only NULL present | Mage-only NULL present | Both-role NULL present |
|---|---:|---:|---:|---:|
| All births | 84 | 15 | 1 | 0 |
| Allowed fresh births only (69 rooms) | 57 | 11 | 1 | 0 |

There are 41 rooms with a terminal fresh magic small that is NULL to neither player. No terminal mage-NULL melee target needing ranger rescue was found. The JSON lists every terminal lifetime and both authoritative observed role bits. No human NULL status is inferred from a missed or zero attack.

## Sustained human mage magic112 is a remaining profile difference

Every one of the mage46 recordings reports **current Magic112, base99** at the initial attack, wave31, and last pre-boss Eye. The underlying packed value is `7340131 = 112×65536 + 99`. All **14,666** reported pre-boss mage Magic updates in that set, including 46 tick0 updates, equal112; the field remains present through cleanup. The ranger42 set additionally exposes mage Magic112 in 36 recordings, with no different value; six do not expose mage Magic. The two sets overlap.

**Ranger Magic is absent** from all these cached player updates, including ranger-corpus recordings. Its first-attack/31/last-Eye stat witness is explicitly null; do not infer112 for that role.

The verified supported native magic potion starts at103. These bot traces do not log current Magic each tick, so a precise late bot level is not established here. Native Eye base max hit is `floor(Magic/3)−6`: 112 gives31 versus28 at103, before subsequent damage modifiers. This is a real human-versus-benchmark profile difference, separate from policy priority; it does not establish how many ticks would be saved or authorize direct boosts, unsupported items, or combat-rule changes. An authorized native supported supply/profile experiment would need its own controlled run and item-consumption assertions.
