# Normal-mode Nylocas lane waves

The server now replaces its three placeholder waves with all 31 normal-mode
batches: 120 lane spawns, including 43 bigs, 85 pillar assignments, and 35
player aggros. The Kotlin table is in
[NylocasWaves.kt](../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasWaves.kt).

Spawn positions, sizes, initial styles, and destinations come from
[lane-spawn-targets.csv](research/nylocas/lane-spawn-targets.csv). Style stages
come from the positive-HP observations in
[lifecycle-waves.csv](research/nylocas/lifecycle-waves.csv), at zero-based ages
0, 5, 7, and 26. The frozen guide reference supplies the natural delay **after**
each wave; the server stores that delay **before the next** wave. With no cap
stalls, wave 31 is 232 ticks after wave 1.

`Start Raid` establishes a four-tick phase for this development instance.
Passing the barrier starts a minimum four-tick countdown, rounded forward to
that phase, giving a delay of four to seven ticks. Starting the raid again or
passing the barrier again preserves the phase and the encounter. The research
establishes that the phase precedes entry; using `Start Raid` as its origin is
the development server's convention, since the recordings do not identify
Jagex's exact initialization event.

The table preserves physical coordinates rather than treating guide display
indices as north/south positions. Wave 9's west small and big share an anchor;
east bigs normally use x3309, but wave 30's east big uses x3310. Wave 30's south
west-slot small starts as magic. Wave 31's west south-slot small flickers from
ranged to melee at age 5, overriding the guide's magic entry.

Style transformations preserve current/max HP, size, spawn age, assigned
pillar, and player targeting state. Player attacks use the current style for
range, accuracy, max hit, animation, and hit type. Pillar-directed NPCs retain
ordinary IDs 8342–8347. Player aggros switch to IDs 8348–8353 after their lane
approach and preserve that form through later style changes. Pillar contact
uses the recorded 2×2 pillar coordinates and the NPC's actual size. The straight
lane approach ends on the first arena tile: west x3290, east x3301, and south
z4243. The next movement step uses target pathfinding immediately, so Nylocas
can turn toward their pillar as soon as they enter instead of walking two
extra tiles toward the centre. Big east spawns and shared-anchor overrides
retain their physical lane row through this handoff.

Three flickering spawns lack a positive-HP age-26 observation: wave 18 west big,
wave 19 east big, and wave 20 south big. Their `styleAt26` is explicitly null;
they retain the age-7 style. This is an evidence gap, so those three late styles
are not claimed as verified.

The room cap is 12 until wave 20 has spawned, then 24. The admission check for
wave 20 still uses 12; wave 21 and later use 24. On a due tick, an existing
population at or above the current cap stalls the entire pending wave. The
encounter retries four ticks later without advancing the wave or HUD progress.
Natural delays for following waves begin at the actual successful spawn time.
A population below the cap admits the whole batch, even if that batch takes
the resulting population above the cap.

Counts include every regular Nylocas NPC in the same instance, small or big,
ordinary or player-aggro, including any split forms. Each counts once regardless
of HP, and dying/expiring bodies count until removal. The boss and unrelated NPCs
are excluded. Counting the NPC registry rather than only the behaviour map
keeps HP-zero bodies in the cap after they leave normal AI processing. Natural
expiry/removal runs before the wave check at the start of the cycle; ordinary
deaths removed later in that cycle release capacity for a subsequent check.

Natural expiry uses the recorded lifetime, independent of AI timer delays and
wave progress. Ages below are zero-based (`current tick - spawn tick`):

| Size | Animation starts | Removal and blast |
| --- | --- | --- |
| Small | 51 | 52 |
| Big | 52 | 55 |

An expiring NPC stops walking and attacking, hides its options, and rejects
further damage while retaining its HP and NPC slot until removal. It uses the
current style's death sequence: smalls show the initial turn, then a detached
detonation map effect on removal; bigs play the native three-tick detonation
sequence. Expiry deletes the NPC directly without drops or respawning. A prior
lethal hit follows normal death handling and does not later trigger expiry.
Player-killed regular Nylocas use a type-specific no-drop death handler for all
12 ordinary/aggro forms, preventing the generic fallback bone drop while
playing their visible style's native death sequence. Smalls retain their body
for one animation tick, then use a detached standard-death map effect;
bigs retain their body for five animation ticks. The generic death handler
still supplies movement-stop/arrival delay and combat cleanup. Death selection
uses the visible form after flickering rather than the original spawn style.
All 12 forms have explicit native attack/death animation overrides. Hit reactions
only play explicitly configured NPC block sequences, so the shared human block
default cannot distort Nylocas or replace an ongoing attack. Other NPCs retain
their configured hit reactions. Previously these Nylo forms had no overrides:
parameter resolution supplied human punch 422, block 424, and death 836. The
animation regression checks cover that failure in addition to removal/drops.
These timings follow the [lifecycle evidence](nylocas-lifecycle-evidence.md) and
[Blert's death appendix](https://blert.io/guides/tob/nylocas/mechanics#death-timing).

Area damage is **provisional**, explicitly selected for this implementation
while the precise live-game blast rules remain unmeasured. Each explosion
hits eligible players once, at removal, within one tile of the NPC's footprint,
including diagonals (3×3 for smalls, 4×4 for bigs). Targets must be alive, inside
the arena, on the same plane and in the same instance. Each gets an independent
uniform integer roll of 0–18 for a small or 0–21 for a big. Damage is typeless,
ignores protection prayers, and has no distance falloff or line-of-sight check.
The size-dependent timing is recording-backed; the radius, damage distribution,
impact tick, and prayer behavior are a tunable gameplay contract, not a claim
of verified OSRS parity. The general area-damage behavior is described by the
[mechanics guide](https://blert.io/guides/tob/nylocas/mechanics#behavior).

The integration resource `nylocas-waves.csv` freezes all 120 research rows with
their incoming-wave delays, physical anchors, initial targets, and style
stages. Tests compare every definition with that fixture and run the complete
31-wave schedule with expiry-induced stalls through the game engine, checking
every birth and every tick's visible style, HP, ordinary/aggro form, and removal. Separate expiry tests
cover native animations/effects for every size/style and both target modes,
partial damage, blocked AI, prior kills/removal, blast boundaries, instance/plane
isolation, protection prayers, and single-hit damage. Entry tests cover all four
clock residues. Stall tests cover thresholds at both caps, whole-wave
overshoot, repeated retries, natural delays after a delayed spawn, wave-20
admission, zero-HP/expiring bodies, simultaneous expiry and admission, and
instance/type isolation. Existing movement and party/entry tests also run.

```bash
cd upstream/rsmod
JAVA_HOME=/home/jordan/osrs/.state/tools/jdk-21 \
GRADLE_USER_HOME=/home/jordan/osrs/.state/gradle \
./gradlew :content:areas:dungeons:nylocas:integration \
  :content:areas:dungeons:nylocas:spotlessCheck --offline
```

This change implements lane-wave composition, natural timing, style schedules,
initial destinations, cap admission/retries, and natural expiry with provisional
area damage. Big splitting, accurate congested routing, pillar damage/collapse,
style-matched incoming
damage/nulling, and boss/room completion remain separate encounter work. The
full-schedule test deliberately avoids player combat and does not certify those
mechanics. With no player kills or big splits, the full-schedule integration
fixture spawns wave 31 at first-wave+384 ticks after 38 four-tick cap stalls.

All nine Theatre source-map regions are multi-combat across all four levels:
12611, 12612, 12613, 12867, 12869, 13122, 13123, 13125, and 13379. The list
matches RuneLite's Theatre region detection and includes all boss rooms,
connecting/reward areas, and the Sotetseg maze. `TheatreMapAreas` packs the
standard `areas.multiway` area across each complete map square. Source-map
normalization makes every instance inherit the same combat rules; the standard
multiway area script drives the native indicator. This applies before raid start.
Integration checks exercise source/instanced coordinates at every map corner
and centre on all levels, plus indicator entry/exit through Bloat and Nylocas.
They load the actual packed area definitions into each isolated test scope.
The integration suite also checks native attack sequences through actual combat,
all 12 animation configurations, hit-reaction suppression, visible-style deaths
after transmogs, detached small death effects, and body-removal timing.
All 137 integration tests and the affected formatting checks pass.

Restart RSMod to load script changes. Changes to `TheatreMapAreas` or
`NylocasNpcEdits` require
`./gradlew packCache` while the server is stopped, then a restart. The local
cache has been repacked for this change. The cache packer now always writes the
final resource pack, so map-only edits are applied even when config identities
already match. No client rebuild is required.
