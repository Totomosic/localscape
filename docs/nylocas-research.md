# Nylocas room and RuneLite compatibility

Researched 2026-10-15 against the public source revisions selected by RuneLite's Plugin Hub. The implementation provides room access, explicit raid startup with the native party HUD, and one configured wave triggered by passing the entrance barrier. Nylos follow their lanes, then path to assigned pillars or attack players. Further waves, pillar damage, full raid completion, and end-to-end plugin verification are future encounter work.

## Source revisions

| Plugin | Plugin Hub revision examined |
| --- | --- |
| ToB QoL | `3715d6ae24547fa87dc7ed82665e37b632407ba7` |
| Nyloer | `9b400ec733fb7d83c261affee4b8edf4feb3fb5f` |
| Nylo Death Indicators | `1b29a76e1799cf813299bfdc75e12251028643ae` |

Repository names and revisions come from the official Plugin Hub manifests: [ToB QoL](https://github.com/runelite/plugin-hub/blob/master/plugins/tobqol), [Nyloer](https://github.com/runelite/plugin-hub/blob/master/plugins/nyloer), [Nylo Death Indicators](https://github.com/runelite/plugin-hub/blob/master/plugins/nylo-death-indicators). Links below pin plugin source so later upstream changes do not change these findings.

The workspace also has older installed plugin JARs under
`.state/home/.rlcustom/plugins/`. Inspecting their bytecode with `javap -p -c`
confirmed the same location requirements: installed ToB QoL uses
`Client.isInInstancedRegion()` and `WorldPoint.fromLocalInstance`, Nyloer checks
map region `13122`, and Nylo Death Indicators checks `13122` and party varbits
`6442..6446`. The public source snapshots above describe further encounter
requirements; they do not imply that the installed JARs are those exact revisions.

## Location and instancing

Use source region **13122**, plane **0**. ToB QoL identifies Nylocas with this region and source chunk origin `(408, 528)`. Its room-entry timing line is `(3304, 4274)` through `(3304, 4277)`. Crucially, `Region.getCurrentRegionID` returns `-1` unless the top-level world view is an **instance**, then obtains source coordinates with `WorldPoint.fromLocalInstance`. A static teleport to region 13122 alone therefore does not meet this plugin's room-detection requirements. [ToB QoL Region.java](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/api/game/Region.java)

Region math gives region origin `(3264, 4224)` and inclusive bounds `x=3264..3327`, `y=4224..4287`. These bounds are derived from region ID 13122, rather than a claim that every tile is walkable. For future instances, preserve the original source chunk coordinates in the instance map packet; moving geometry to unrelated source coordinates loses the plugin's coordinate checks.

Nyloer checks whether `client.getMapRegions()` contains 13122. It resets its wave state on pillar NPC spawn and counts a new wave when a non-split NPC spawns on a different client tick. Pillar IDs are `8358` (normal), `10790` (story), `10811` (hard). [NyloerPlugin.java](https://github.com/EIKOOT/nyloer/blob/9b400ec733fb7d83c261affee4b8edf4feb3fb5f/src/main/java/com/nyloer/NyloerPlugin.java)

Nyloer's source-coordinate spawn classification is:

| Region-local tile | World tile | Classification |
| --- | --- | --- |
| `(17,25)` / `(17,24)` | `(3281,4249)` / `(3281,4248)` | West north / south |
| `(31,9)` / `(32,9)` | `(3295,4233)` / `(3296,4233)` | South west / east |
| `(46,24)` / `(46,25)` | `(3310,4248)` / `(3310,4249)` | East south / north; second is big when NPC size exceeds 1 |
| `(18,25)` | `(3282,4249)` | West big |
| `(32,10)` | `(3296,4234)` | South big |
| `(47,25)` | `(3311,4249)` | East big wave 30 |

Other coordinates classify as splits. World tiles above are calculated by adding the region origin. [NyloerNpc.findSpawn](https://github.com/EIKOOT/nyloer/blob/9b400ec733fb7d83c261affee4b8edf4feb3fb5f/src/main/java/com/nyloer/npc/NyloerNpc.java)

## Signals the future encounter must preserve

ToB QoL consumes raid status, room status, boss HP, party usernames, and orb states. Its event manager reads usernames from varc strings `330..334`; orb value `1` means dead, while other nonzero values mean a living member. [EventManager.java](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/EventManager.java) `isInRaid()` requires party status `2` or `3`; status `0/1` is outside. [InstanceService.java](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/InstanceService.java)

| Client state | Identifier |
| --- | --- |
| Raid party status | Varbit `6440` (`THEATRE_OF_BLOOD`) |
| Party slot/orb states | Varbits `6442..6446` (`TOB_CLIENT_P0..P4`) |
| Room status | Varbit `6447` |
| Boss HP | Varbit `6448` |
| Precise timer preference | Varbit `11866` |

Identifiers are defined by [RuneLite Varbits](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/Varbits.java), [RuneLite VarbitID](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/gameval/VarbitID.java), and [ToB QoL RaidConstants](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/api/game/RaidConstants.java). Values must reflect real encounter state; a teleport should not fabricate an active raid.

Use official NPC IDs and composition sizes. Normal incoming small melee/range/mage IDs are `8342/8343/8344`, incoming big IDs `8345/8346/8347`; fighting variants are `8348..8353`. Nyloer also recognizes story/hard equivalents. [NyloType.java](https://github.com/EIKOOT/nyloer/blob/9b400ec733fb7d83c261affee4b8edf4feb3fb5f/src/main/java/com/nyloer/npc/NyloType.java)

ToB QoL's boss IDs are `8354` dropping, `8355` melee, `8356` magic, `8357` ranged; attack animations are `8004`, `7989`, `7999`, respectively. Pillar object IDs are `32862` intact and `32864` collapsed. Spectator webs are `32939/32865/32937`, walls `32876/32899`. Its normal wave count is 31, and completion chat matches `Wave 'The Nylocas' (...) complete!`. Its valid incoming spawn set is slightly broader than Nyloer's labels: west `x=17/18,y=24/25`; south `x=31/32,y=9/10`; east `x=46/47,y=24/25`. [NylocasConstants.java](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/rooms/nylocas/commons/NylocasConstants.java)

Nylo Death Indicators also checks map region 13122, but additionally counts nonzero party-slot varbits on `LOGGED_IN`; it ignores NPC spawns when party size is outside `1..5`. It predicts lethal damage from XP drops, weapon IDs, attack style, player animation, and interaction target, then reconciles hitsplats and NPC health. Multi-target damage uses neighboring NPC positions. Consequently boosted/custom XP or mismatched combat packets can break prediction even in the correct room. [NyloDeathIndicatorsPlugin.java](https://github.com/InfernoStats/Nylo-Death-Indicators/blob/1b29a76e1799cf813299bfdc75e12251028643ae/src/main/java/com/infernostats/NyloDeathIndicatorsPlugin.java)

Its normal/hard small and big HP assumptions by party size are `(8,16)` for 1–3 players, `(9,19)` for 4, `(11,22)` for 5. Story mode uses `(2,3)`, `(3,5)`, `(6,9)`, `(8,12)`, `(10,15)`. [NylocasHealth.java](https://github.com/InfernoStats/Nylo-Death-Indicators/blob/1b29a76e1799cf813299bfdc75e12251028643ae/src/main/java/com/infernostats/NylocasHealth.java) The plugin maps mode/style/size from NPC IDs rather than server-provided max HP. [NylocasType.java](https://github.com/InfernoStats/Nylo-Death-Indicators/blob/1b29a76e1799cf813299bfdc75e12251028643ae/src/main/java/com/infernostats/NylocasType.java)

## Verification boundary

### Local cache and implementation

The installed revision-233 game cache already has terrain archive `m51_66` and
encrypted scenery archive `l51_66`, and `xteas.json` contains its key. Decoding
both with RuneLite's cache library succeeded and found **601 scenery placements**,
including the four intact pillar objects `32862`. The central arena has the
original 12-tile-wide floor with clipped corners. RSMod loads maps listed in the
XTEA file, including this one, during startup. The **map exists** independently of
the new encounter scripts; creating a replacement 12×12 map is unnecessary.
These are observations from this workspace's installed cache and
source, rather than claims about an uninspected download.

The `nylocas` admin command copies the complete map square on all four levels
using `RegionTemplate.copyAllLevels(408, 528)` with an 8×8 chunk block. The block
starts at instance chunk `(4,4)`, keeping the arena near the instance centre.
`nylocas` teleports to the instance equivalent of source `(3295,4248,0)`;
`nylocasbridge` lands outside on the northern approach bridge at `(3295,4264,0)`.
The bridge tile has a plane-1 floor with the link-below flag, placing its visible
surface on player plane 0. Its destination and access restriction are covered by
the command integration checks. All scenery and clipping come from the existing
map. Teleporting does not start the encounter. Each request creates a fresh instance, and the existing region
allocator reclaims rooms after players leave. The Local Development sidebar
submits this command through its existing login-checked client-thread bridge.

Relevant local sources: [map loading](../upstream/rsmod/api/cache/src/main/kotlin/org/rsmod/api/cache/map/GameMapDecoder.kt),
[instance creation](../upstream/rsmod/content/other/commands/src/main/kotlin/org/rsmod/content/other/commands/NylocasDevelopmentCommand.kt),
[shared room template](../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasRoom.kt),
[instance rebuild packets](../upstream/rsmod/api/net/src/main/kotlin/org/rsmod/api/net/rsprot/RspCycle.kt),
and [region allocation and reclamation](../upstream/rsmod/api/repo/src/main/kotlin/org/rsmod/api/repo/region/RegionRepository.kt).

### Bloat exit approach

The `bloatexit` admin command and **Teleport to Bloat Exit** sidebar button land
at source `(3322,4447,0)`, immediately inside the eastern exit of Bloat's region
`13125` (`51_69`). Local cache inspection found the exit scenery and stairs,
with walkable linked floor ending at `x=3322`; no interactive passage exists at
that exit in the cache. The server adds two native Formidable passage clickboxes
(`33113`) there, at `y=4447/4448`, with wall shape 0 facing east. Both **Enter**
and **Quick-Enter** lead to source `(3295,4258,0)`, one tile west of the combat
dummy just outside the Nylocas barrier.

This development route copies both complete source map squares on all four
levels into one large instance. Bloat occupies instance chunks `(20..27,4..11)`
and Nylocas `(4..11,4..11)`. The spacing keeps Nylocas's source region outside the
client's scene while at the Bloat exit. Door traversal retains the same instance,
so multiple players share the destination and its first-wave state. The handler
checks the passage's source coordinates to avoid changing other Theatre doors.
Arrival does not start the wave; passing the Nylocas barrier remains the trigger.
There is no Bloat combat or completion requirement in this development route.
See [BloatExit.kt](../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/BloatExit.kt).

### Raid startup and native party HUD

**Start Raid** submits the admin command `starttob`. It starts the existing Bloat
or Nylocas development instance without teleporting or spawning NPCs, and includes
up to five players already present. The Nylocas barrier requires membership in
this started raid before allowing entry. Repeating Start Raid does not reset the
encounter. Starting a new raid requires a fresh development instance.

The server opens native interface `28` (`tob_hud`) on the gameframe's
`overlay_hud` target. Inspection of the revision-233 cache confirms its on-load
script `2087` already has the 24 component arguments needed to initialize the HP
orbs. No custom RuneLite overlay is needed. Script `2301` takes five names and
writes varc strings `330..334`; the server supplies blank strings for empty slots.
Party status `6440=2` enables the native orb display. Party slot `6441` is
one-based (`1..5`), allowing the client to label and update the viewer's own orb.

The inspected HUD draw script `2297` requires an instance coordinate with
`x>=6400`, through script `1935`, and status `2`. Both development templates use
the server's instance allocation area and satisfy this check. Script `2298`
hides empty orb slots (`0`) and maps values `1..27` to `0..26` HP-fill pixels.
The server sends `1 + ceil(26 * clamped_current_hp / base_hp)` for each member,
updating changed values each server tick. The viewer's own orb additionally uses
native stat-transmit script `2299`/`2300` for precise HP updates.

Before the barrier, room-progress type `6447` is `0`. Passing sets it to `1`
during the four-tick countdown, which ToB QoL observes as room start. The first
wave then sets type `2` and progress `1/31`. This is one implemented wave within
the normal-mode wave count, not a completed raid. Names, slots, HP, progress, and
the HUD are cleared on departure or logout; login also clears stale state.
The default route is to start at Bloat's exit before its door causes the scene
transition to Nylocas, so region- and login-state-based plugins can observe the
party values on room entry. Live plugin behavior remains unverified.

These HUD details were decoded from this workspace's revision-233 cache with
RuneLite's `ScriptLoader`, `Disassembler`, and `InterfaceLoader`.
See [TheatreRaid.kt](../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/TheatreRaid.kt).

### First wave

Both entrance barriers are object `32755` (`tob_arena_barrier`), with **Pass** as
their first option. The cache places them at `(3295,4255,1)` and `(3296,4255,1)`;
the bridge link-below flag resolves them onto player plane 0. The handler checks
their original coordinates, since other Theatre rooms share this object type.
Entering crosses to `y=4254`; passing out crosses to `y=4256`.

The first entry schedules a wave exactly four ticks after the Pass action.
All three spawns occur in the same server tick, using official incoming NPC IDs
and source lane coordinates:

| Lane | NPC | ID | Source tile |
| --- | --- | --- | --- |
| South | Small Nylocas hagios (mage) | 8344 | `(3295,4233,0)` |
| West | Small Nylocas toxobolos (ranger) | 8343 | `(3281,4249,0)` |
| East | Small Nylocas ischyros (melee) | 8342 | `(3310,4248,0)` |

Small nylos spawn with both current and maximum HP set to **8** for 1–3 players,
**9** for four players, or **11** for five players. Big nylos use **16**, **19**,
or **22** HP respectively. Scaling uses the team size recorded by Start Raid
and stays fixed if members leave or disconnect.

The countdown belongs to the instance and runs on server ticks, independently of
the player's actions or session. Subsequent Pass actions do not reset or repeat
it. Deleted instances lose their pending wave, and NPCs are reclaimed with their
instance. Nylos do not respawn. Basic party state and room-progress varbits are
supplied by explicit raid startup.
See [NylocasEncounter.kt](../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasEncounter.kt).

### Wave table and targeting

[NylocasWaves.kt](../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasWaves.kt)
defines six nullable slots per row: west north/south, south west/east, and east
north/south. An occupied slot specifies small/big, melee/ranged/magic, and an
optional target pillar. Empty slots spawn nothing. Only the original first wave
is configured: west north targets the northwest pillar, south west targets the
southwest pillar, and east south targets the northeast pillar. These are explicit
development targets, not a complete reconstruction of the official wave table.

The first row's `ticksAfterPrevious` counts from barrier entry; subsequent rows
count from the preceding wave's spawn. All occupied slots in a row spawn together.
The scheduler consumes further configured rows and updates the native wave counter.

Each nylo walks one tile per tick to its slot's entry point before acquiring a
target. The source cache marks incoming lanes as blocked terrain, so only this
straight approach uses the per-NPC blocked/normal movement override. Arena movement
switches to normal terrain collision with NPC stacking and uses the existing route
finder to navigate around scenery. On lane completion, the nylo changes to its
matching official fighting NPC ID without resetting HP.

Pillar targets use the cache's 3×3 support bounds and stop at an adjacent tile.
Pillar HP, damage, and collapse are not implemented. A null pillar target selects
the nearest living player inside the same instance's arena, retains that target
while valid, and selects another if they leave or die. Players on the bridge or
in other instances are excluded. Melee requires adjacency; ranged/magic currently
use four-tile range and line of sight. Attacks use the existing accuracy/max-hit
formulas, attack-rate parameters, typed queued hits, and protection prayers.
Full ToB attack stats, projectiles, and visual timing still need encounter tuning.

NPC visibility expands from the normal 15-tile radius to 32 tiles on plane 0 of
the Nylocas source map, including instanced copies and the bridge. The incoming
east and west lanes are 29 tiles apart, so the normal radius dropped the opposite
lane when walking across the arena. The radius updates after movement and before
NPC info encoding, independently of raid startup. RSProt selects the native large
NPC info packet for the expanded range. Leaving the room restores the normal
radius. This changes server tracking; client camera and draw-distance settings
still control what is drawn on screen.

### Remaining encounter work

The room, native HUD, wave scheduler, and movement/targeting establish the encounter foundation. Full compatibility still requires the remaining wave data, pillar health/damage/collapse, splitting and expiry, full combat tuning, later encounter lifecycle events, and live checks with the installed plugins. These findings are source inspection and server integration tests, not a completed encounter compatibility test.

### Checks performed

- `./dev build-client`: built and packaged the client with teleport and Start Raid buttons.
- Existing `LocalDevPanelTest` and `LocalDevPluginTest`: six tests passed with
  headless AWT and the workspace Java home.
- RSMod `:content:other:commands:integration`: four tests passed through the
  `ClientCheat` handler, covering admin-only access, walkable arrival terrain,
  source-coordinate preservation for the incoming lanes and entry line, all
  four source levels, reclamation after repeated entry and departure, and the
  Start Raid command's HUD setup and idempotence.
- RSMod `:content:other:commands:spotlessCheck`: passed.
- RSMod `:api:net:test`: the real NPC info protocol regression test confirms
  normal-range culling, retention of all three lanes while moving between the
  west and east sides at 32 tiles, the large packet format, and return to the
  normal packet and range afterwards. Removing the range update makes it fail.
- RSMod `:content:areas:dungeons:nylocas:integration`: thirty-five tests passed through
  real loc interaction packets, covering exact four-tick timing, NPC IDs and
  lane coordinates, both barrier halves sharing a countdown, starter logout,
  repeated entry, instance isolation, cancellation on reclamation, and passing
  out without starting the encounter. Bloat exit checks also cover both passage
  options and halves, shared instance identity, loaded cache collision at both
  destinations, cleanup, unrelated passages, and walking from the bridge start
  to the barrier before the first wave's countdown. Raid tests cover native HUD
  open/name/varp packets, solo and five-player slots, live HP synchronization,
  departure and login cleanup, same-tick teleport/start ordering, invalid
  locations, oversized parties, and refusing barrier entry before raid start.
  Visibility tests cover source-map normalization across instances, bridge/Bloat
  transitions, independent players, plane changes, and applying the radius before
  the client update. HP tests cover all five team sizes, current and maximum HP
  for all three styles, persistence over 40 ticks, and a five-player raid retaining
  its scaling when four members depart during the countdown.
  Movement tests cover lane-first travel on actual cache collision, reaching pillar
  bounds, all six spawn slots and all size/style combinations, later-row delays
  and HUD progress, big-nylo routing around obstacles, queued player damage,
  retargeting on departure, and excluding the bridge and other instances.
- RSMod `:content:areas:dungeons:nylocas:spotlessCheck`: passed.
- RSMod `:engine:game:test`: 128 tests passed; the existing
  `NpcMovementProcessorTest` integration regression also passed. The changed NPC
  file passed its targeted formatting check.
- `./dev test`: all 12 workspace tests passed.

The client tests ran with `-Djava.awt.headless=true` and a workspace `user.home`.
Gradle ran offline outside the restricted sandbox because its lock service needs
local networking. A live graphical session and plugin overlays have not been
tested.

## ToB QoL Waves and Cleanup splits (2026-10-17)

The installed ToB QoL plugin counts the final wave from native lane-spawn NPC
events and records cleanup when its tracked nylocas disappear. Both markers
were already detected in the captured run, but its live overlay hid them: the
server omitted the four pillar NPCs (`8358`, `tob_nylocas_support`). Without a
pillar spawn, room status 1 creates an uncertain starting tick, and the overlay
renders only `Room`. See the pinned
[Nylocas handler](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/rooms/nylocas/NylocasHandler.java)
and [room timing renderer](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/timetracking/RoomDataHandler.java).

Room entry now publishes a start event and spawns the four native support NPCs
at the pillar anchors during that same countdown tick, before client updates.
Repeated party entry does not duplicate them; reclaiming an instance removes
its supports. Their damage/collapse mechanics and the boss remain separate work.

A replay of the captured 31 wave batches and NPC removals against the installed
plugin produced `Waves = 352` and `Cleanup = 401` ticks in both cases. Without
the support spawn, visible rows were `[Room]`; with the support spawn before
the first countdown `GameTick`, they were `[Waves, Cleanup, Room]`. This checks
the real plugin handler and renderer rather than a replacement split detector.
Server integration regressions cover all four support spawns before the first
countdown client update, a second party member entering, separate instances,
and support removal on reclamation.

Validation: all 195 Nylocas integration tests passed, along with the module's
`spotlessCheck`. The two new regressions failed with missing supports before
the fix and passed afterward. A live client check after restarting RSMod remains
to be performed.
