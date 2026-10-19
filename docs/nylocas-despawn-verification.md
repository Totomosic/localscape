# Nylocas despawn timing verification

Verified 2026-10-19 against the simulator's game loop and combat handlers.
Overkill requires an actual positive-damage hit clamped to zero after the NPC
has been killed by a player attack. That hit must land on the lethal tick or
during the death animation. A single large hit, a natural zero/miss, and a
future hit that never impacts do not qualify. A thrall's clamped hit can qualify
after a player kill; a thrall delivering the lethal damage cannot. Small deaths
ignore walking; only bigs retain their extra arrival tick.

The [independent source and recording review](nylocas-death-timing-review.md)
distinguishes these simulator measurements from externally verified mechanics.
In particular, stationary big removal at impact+5 with an extra hitsplat is
not corroborated by the pinned recordings or explicitly specified by the guide.

## Death timing

`NylocasDeathsTest` measures impact and removal on the same server clock:

| Size | Moving before death | Ordinary removal after impact | With a qualifying zero-clamped hitsplat |
| --- | --- | ---: | ---: |
| Small | No | 2 ticks | 1 tick |
| Small | Yes | 2 ticks | 1 tick |
| Big | No | 6 ticks | 5 ticks |
| Big | Yes | 7 ticks | 6 ticks |

These rows cover stationary targets and targets already walking continuously.
The NPC queue processor permits early death-queue execution only after a
qualifying zero-clamped impact on the lethal tick. Hits already in flight can
also land during the animation and shorten its remaining delay once, without
restarting it. A merely queued future hit cannot change the death timing. For
bigs beginning or finishing movement around impact, the earlier
queue execution changes which previous-cycle movement flag is sampled: the
measured speedup can be two ticks or zero ticks. See the independent review's
boundary cases. Small Nylocas skip the shared movement arrival delay; animation
durations remain one tick for smalls and five ticks for bigs.
Those animation durations apply to ordinary and lethal-tick-overkill deaths;
overkill arriving during an already running animation shortens its remaining
delay by one tick. Multiple qualifying impacts do not stack speedups.

## Wave 21 regression

`NylocasDamageTest` starts the real encounter at wave 21 and casts Ice Blitz on
its west-north mage at spawn+1. It uses the real five-tick spell cooldown, then
attacks an adjacent small at spawn+6. The test covers both stationary targets
and targets walking north into melee range on the attack tick. It asserts actual
movement before the attack and on the cycle preceding lethal impact. The melee
target represents a survivor from an earlier wave; it has the correct solo HP
of 8. Sulphur blades provide two hits; a scythe's single hit against a small
provides the control.
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
mean despawn at spawn+7. Walking does not change the small's removal timing, so
the overkill target can be moving or stationary and still beat the spawn+8 check.
Temporarily restoring the arrival delay for melee targets makes exactly the two
walking cases fail: removal slips from 8 to 9 with overkill, and from 9 to 10
without overkill. The stationary cases still pass.

Run the focused coverage from `upstream/rsmod`:

```bash
JAVA_HOME=/home/jordan/osrs/.state/tools/jdk-21 \
GRADLE_USER_HOME=/home/jordan/osrs/.state/gradle \
./gradlew :content:areas:dungeons:nylocas:integration \
  :api:combat:combat-scripts:integration --tests '*Nylocas*' \
  :api:game-process:integration --tests '*NpcQueueProcessorTest' \
  --tests '*NpcDeathDropsTest' --offline --console=plain
```

Current validation passes all 622 Nylocas room tests, 55 Nylocas combat tests,
and five shared NPC death/drop queue tests: 682 tests, with no failures or skips.
The 57 dedicated overkill cases cover player/thrall lethal-hit order, genuine
zero-clamping versus natural zeros, future hits, impacts during the animation,
the moving big's pre-animation arrival delay, multiple impacts on the same or
different ticks, and hits on the last animation tick. Animation assertions
ensure that late impacts do not restart the sequence. The real summoned-thrall
regression checks matching-style positive damage clamped after the player kill
against a wrong-style natural zero. The hit-order review adds a lethal final
hit clamped to positive HP, which must not qualify as zero-clamped overkill.

The root cause was queue-list shape implicitly deciding when death could start.
The shared processor now records player-kill eligibility and a qualifying
impact explicitly. Death-animation hits reuse the running death access rather
than launching a replacement coroutine; an HP-zero hit cannot queue a second
death. Formatting checks passed for the changed Kotlin files.

The broader shared game-process suite has 11 player movement/routing failures
across `PlayerMovementMapFlagTest`, `PlayerMovementProcessorTest`,
`PlayerRouteRequestTest`, and `PlayerFaceSquareProcessorTest`. All 11 reproduced
with the seven modified production files temporarily replaced by their unchanged
`HEAD` versions. The overkill sources were then restored byte-for-byte and the
682 targeted tests passed again. These existing failures remain outside this fix.
