# Duo Nylocas Mage client plugin

The **Duo Nylocas Mage** plugin executes the [mage hierarchy](nylocas-duo-mage-strategy.md) and [wave guide](nylocas-duo-mage-waves.md) on LocalScape. It is a separate plugin from the ranger; enable the role appropriate to each player's client.

## Setup and use

1. Build the custom client with `./dev build-client`, then restart the client through the existing launcher/RSProx workflow.
2. Enable **Duo Nylocas Mage**. Its sidebar contains Start mage, Start room, Stop and equipment-capture buttons. Enabling initially arms it; Start re-arms after a stop.
3. Switch to **Ancient Magicks**, carry supplies for Ice Barrage (the local server supports its rune-pouch behavior), and carry Eye of Ayak, Shadow, sulphur blades, charged scythe, Dinh's bulwark, dragon darts, armor and prayer potions. Shadow is optional if **Shadow opener** is disabled. Configure the scythe, Dinh's and barrage-staff item IDs to match carried variants; the default barrage staff is Ancient sceptre, ID 27624.
4. Equip each armor set and use **Capture magic/melee/range equipment**. Capture excludes the weapon because the controller chooses it. Captured magic equipment can include an offhand for Ayak/staff switches; account for two-handed Shadow/scythe/bulwark switches in the inventory. Set variant options before enabling or press Start again after changes.
5. Create/join/start a duo using the existing Theatre controls. Press **Start mage before entering the room**, so it can record original NPC births. It repeats barrier clicks while waiting and does not enter automatically. When both players are ready, press **Start room** on either sidebar. That agent confirms **Enter** specifically in the Nylocas dialogue, and the server brings the waiting partner through. **Start mage** arms the controller; **Start room** separately authorizes entry. Stop or re-arming clears a pending room-start request. The plugin then stages south for its opening attack.

The controller validates the spellbook, weapons and captured equipment before entering. Missing items show an item ID in the sidebar and stop the attempt. It refuses a new Start halfway through active waves because original birth ages would be unknown. Training-wave starts work when already tracking before the configured wave spawns. Stop, death, logout/disconnect, leaving the room and boss spawn stop the wave controller; Start again for another attempt. Boss combat is outside its scope.

The plugin enables run and disables auto-retaliate through native controls. At the entrance it sets the same room camera as the ranger once per Start, leaving subsequent manual camera changes available. Movement uses collision-aware native minimap clicks and attack approaches use line of sight. Keep the minimap visible. Queued movement is cancelled when superseded by an attack or Stop.

## Policy implemented

Each NPC keeps its own birth tick, birth wave, lane, slot and size through transformations. Live style, fighting form, estimated HP, death state, partner claims and reachability guide decisions. Dying bodies remain tracked until despawn. A new split receives its own birth wave, including a wave-30+ child of an ignored older big. HUD wave changes drive admission timing; predicted retries only reserve cooldown and never manufacture a new wave through a stall.

The mage reserves attack cooldown and staging travel for these packets. Offsets are **request ticks**, normally one before the recorded attack animation:

| Packet | Request / behavior |
| --- | --- |
| Wave 12 | Scythe the east pair around `s12+7`, preferring the southern primary; five request ticks later Dinh's the west-lane southern small. Carry the packet through wave 13. |
| Wave 21 | Barrage west at `s21` from beside the barrier; sulphur an available northern melee small five request ticks later. |
| Wave 22 | Clear south magic between reserved actions; Dinh's south west-slot small around `s22+9…10`, checking its current melee color. If that window is missed, continue useful magic clearing and special the east-spawned big once it is fighting melee and reaches the mage. |
| 26-Dinh's during wave 28 | Preserve the south wave-26 big beside the southwest pillar. Around `s28+8`, require west entrants to be at the doorway while south entrants remain in their lane, and require melee reach of the primary. Cancel if 29 has spawned. |

A packet completes only after an observed matching weapon animation/target, not after issuing an action. Continuous Ayak animations have a game-tick acknowledgement path. A failed or unavailable request remains retryable within its window. Scythe/barrage follow-up timing adjusts to an observed late first attack. Dinh's requires at least 50% special energy; insufficient energy does not become an ordinary bulwark hit or block all other clearing. This basic policy does not spend optional specials on wave 25, preserving energy for the named packets. Equipment and special controls precede the native attack; barrage uses native Ice Barrage selection followed by spell-on-NPC, rather than a normal staff attack or a client-only autocast setting.

If an attack request has no matching launch by the next game tick, the controller immediately retries or selects another eligible target. It does not wait three ticks or send a stop/movement command while waiting for acknowledgement. Confirmed attacks retain their normal weapon cooldowns and packet reservations.

Outside reservations, assigned lane/big work precedes ordinary clearing. The controller favors fresh magic smalls, fresh melee and useful nearby scythe clusters, then unclaimed nearby ranged leftovers with darts. South magic smalls receive additional priority during waves 20–22. It avoids several ranger-owned magic lanes and temporarily claims the partner's observed primary targets. It uses selected big assignments rather than opening every magic big: south 4, west 8/11, deferred east melee 13, east/south 19, south 20/24, east melee 24, west melee 25, west magic 26/27, east magic 30. The deferred wave-13 big becomes eligible at age 25. Wave-15 south and wave-17 bigs remain ignored. The mage can continue south wave-19 work after wave 20, while preserving the south-20 opener and wave-21 reservation.

**When wave 29 actually spawns, the mage immediately ignores every NPC born in waves ≤29**, including the wave-29 south big and an unfinished late 26-Dinh's. It handles fresh wave-30/31 assignments and their young children. This is the requested policy; it deliberately differs from the recorded late human attacks and the existing ranger's later cleanup cutoff. The ranger's targeting policy remains unchanged.

Offensive prayers default on and persist through cooldowns: Augury initially, then the attack's appropriate Augury/Piety (local Zeal)/Rigour. Equipment preparation alone does not change prayer style. Protection prayers track fighting NPCs attacking the local player and prioritize big threats, including ignored old attackers. Prayer potion restoration defaults to 20 points, consumes partly used bottles first, and runs independently of attack selection. These behaviors share the tested native-control/prayer infrastructure with the ranger.

## Implementation and verification

- [Plugin lifecycle and live tracking](../client/src/main/java/net/runelite/client/plugins/nylomage/NylocasMagePlugin.java)
- [Mage decisions, assignments and reservations](../client/src/main/java/net/runelite/client/plugins/nylomage/MageStrategy.java)
- [Weapons and manual barrage actions](../client/src/main/java/net/runelite/client/plugins/nylomage/MageActions.java)
- [Shared movement, gear, prayers and color predictions](../client/src/main/java/net/runelite/client/plugins/nylocommon)
- [Mage tests](../client/src/test/java/net/runelite/client/plugins/nylomage)

The mage suite tests packet carry-over, southern scythe preference, follow-up timing, cooldown/travel reservations, south magic preparation, the correct wave-22 fallback identity, low special energy, saved big/doorway geometry, deferred and ignored bigs, freshness/claims/expiry, post-29 children, native barrage/special actions, equipment variants, spellbook validation, barrier waiting, explicit room-start confirmation/cancellation, lifecycle behavior and cancelling pending old attacks at 29. Replay tests verify all 62 rooms' raw checksums, living lane color histories and evidence for every ordinary assignment, then drive all critical packets from the fastest room's recorded NPC/wave frames with an available mage at the strategy's staging tiles. They do not reproduce unrecorded input clicks or all damage outcomes.

After syncing the overlay with `from tools.dev import sync_client; sync_client()`, run from `upstream/runelite` using the workspace JDK 21:

```bash
mvn -o -B -Dmaven.repo.local=/home/jordan/osrs/.state/m2 \
  -Djava.awt.headless=true -Duser.home=/tmp/nylomage-test-home \
  -Dshade.skip=true -pl runelite-client -am \
  '-Dtest=Mage*Test,NylocasMagePluginTest,Ranger*Test,NylocasRangerPluginTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Offline tests establish policy decisions and client interactions, not a completed live duo room or a boss-spawn time. In particular, doorway checks implement the requested late timing geometry; the recording data do not prove a complete Dinh's victim set or southern exclusion, and local server AoE behavior still determines actual hits. The southern wave-12 primary, wave-22 alternative special and strict cutoff are intentional refinements whose live performance remains to be checked.
