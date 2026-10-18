# Nylocas despawn timing verification

Verified 2026-10-18 against the simulator's game loop and combat handlers.
The requested overkill mechanic means an additional hitsplat after lethal
damage, including a hitsplat clamped to zero. A single large hit is not the
trigger. Moving smalls retain their extra arrival tick, as clarified by the user.

## Death timing

`NylocasDeathsTest` measures impact and removal on the same server clock:

| Size | Moving before death | Ordinary removal after impact | With an extra hitsplat |
| --- | --- | ---: | ---: |
| Small | No | 2 ticks | 1 tick |
| Small | Yes | 3 ticks | 2 ticks |
| Big | No | 6 ticks | 5 ticks |
| Big | Yes | 7 ticks | 6 ticks |

The existing NPC queue processor already provides the one-tick speedup. No death
animation duration or arrival delay was changed.

## Wave 21 regression

`NylocasDamageTest` starts the real encounter at wave 21 and casts Ice Blitz on
its west-north mage at spawn+1. It uses the real five-tick spell cooldown, then
attacks an adjacent stationary small at spawn+6. The melee target represents a
survivor from an earlier wave; it has the correct solo HP of 8. Sulphur blades
provide two hits; a scythe's single hit against a small provides the control.
Maximum-hit mode makes the kill deterministic. The fixture includes enough
survivors to fill the 24-NPC cap while the melee target remains at the due check.

| Event | Tick relative to wave 21 |
| --- | ---: |
| Wave 21 spawns | 0 |
| Spell cast | 1 |
| Melee attack becomes available and executes | 6 |
| Melee hits land | 7 |
| Overkill target despawns | 8, before wave admission |
| Wave 22 spawns with overkill | 8 |
| Single-hit control despawns | 9 |
| Wave 22 spawns without overkill, after one cap stall | 12 |

The original implementation checked the cap at `StartCycle`, before NPC death
coroutines resumed. Thus the overkill body still blocked the due check even
though it disappeared later on tick 8. Wave admission now runs at
`GameLifecycle.NpcsProcessed`, after NPC deaths and splits, before player main
processing. Natural expiry still runs at `StartCycle`. The regression observes
the actual wave-spawn event and checks that the melee target is already removed,
rather than comparing only end-of-tick snapshots.

“Before wave 22” here means earlier within the same server tick. It does not
mean despawn at spawn+7. A moving small takes the extra tick shown in the matrix;
this particular spell-to-melee sequence needs a stationary target to beat the
spawn+8 check.

Run the focused coverage from `upstream/rsmod`:

```bash
JAVA_HOME=/home/jordan/osrs/.state/tools/jdk-21 \
GRADLE_USER_HOME=/home/jordan/osrs/.state/gradle \
./gradlew :api:combat:combat-scripts:integration --tests '*NylocasDamageTest.wave 21*' \
  :content:areas:dungeons:nylocas:integration --tests '*NylocasDeathsTest' --offline
```

The complete Nylocas suite (296 tests) and combat-script suite (108 tests) pass.
The broader game-process suite has 11 movement/facing failures out of 88 tests.
Disabling the new event publication reproduces the exact same 11 failures and
messages, establishing that they predate this ordering change.
