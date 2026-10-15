# Nylocas room and RuneLite compatibility

Researched 2026-10-15 against the public source revisions selected by RuneLite's Plugin Hub. This milestone provides access to the room; NPC waves, combat, raid state, and end-to-end plugin verification are future encounter work.

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
XTEA file, including this one, during startup. There are no Nylocas encounter
scripts in this checkout. This establishes that the **map exists**, while the
**encounter still needs implementation**; creating a replacement 12×12 map is
unnecessary. These are observations from this workspace's installed cache and
source, rather than claims about an uninspected download.

The `nylocas` admin command copies the complete map square on all four levels
using `RegionTemplate.copyAllLevels(408, 528)` with an 8×8 chunk block. The block
starts at instance chunk `(4,4)`, keeping the arena near the instance centre.
`nylocas` teleports to the instance equivalent of source `(3295,4248,0)`;
`nylocasbridge` lands outside on the northern approach bridge at `(3295,4264,0)`.
The bridge tile has a plane-1 floor with the link-below flag, placing its visible
surface on player plane 0. Its destination and access restriction are covered by
the command integration checks. All scenery and clipping come from the existing
map; no new NPCs, waves, or raid varbits are
introduced. Each request creates a fresh instance, and the existing region
allocator reclaims rooms after players leave. The Local Development sidebar
submits this command through its existing login-checked client-thread bridge.

Relevant local sources: [map loading](../upstream/rsmod/api/cache/src/main/kotlin/org/rsmod/api/cache/map/GameMapDecoder.kt),
[instance creation](../upstream/rsmod/content/other/commands/src/main/kotlin/org/rsmod/content/other/commands/NylocasDevelopmentCommand.kt),
[instance rebuild packets](../upstream/rsmod/api/net/src/main/kotlin/org/rsmod/api/net/rsprot/RspCycle.kt),
and [region allocation and reclamation](../upstream/rsmod/api/repo/src/main/kotlin/org/rsmod/api/repo/region/RegionRepository.kt).

### Remaining encounter work

An accessible empty room establishes geometry and location only. Full compatibility requires a source-mapped instance, correct raid/party state, official NPCs, spawn positions, tick grouping, combat XP/hitsplats, and live checks with the installed plugins. These findings are source inspection, not a completed encounter compatibility test.

### Checks performed

- `./dev build-client`: built and packaged the client with the teleport button.
- Existing `LocalDevPanelTest` and `LocalDevPluginTest`: six tests passed with
  headless AWT and the workspace Java home.
- RSMod `:content:other:commands:integration`: three tests passed through the
  `ClientCheat` handler, covering admin-only access, walkable arrival terrain,
  source-coordinate preservation for the incoming lanes and entry line, all
  four source levels, and reclamation after repeated entry and departure.
- RSMod `:content:other:commands:spotlessCheck`: passed.
- `./dev test`: all 12 workspace tests passed.

The client tests ran with `-Djava.awt.headless=true` and a workspace `user.home`.
Gradle ran offline outside the restricted sandbox because its lock service needs
local networking. A live graphical session and plugin overlays have not been
tested.
