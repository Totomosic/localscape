# Duo Nylocas Ranger client plugin

The **Duo Nylocas Ranger** plugin plays the [researched ranger hierarchy](nylocas-duo-ranger-strategy.md) and [wave rotation](nylocas-duo-ranger-waves.md) on the LocalScape client. It controls equipment, attack stances, offensive prayers, specials, NPC attacks and movement. Its scope is the wave room through boss spawn; it stops when the boss appears.

## Use

1. Rebuild with `./dev build-client`, close the existing game client and launch a new session. Enable **Duo Nylocas Ranger** in the RuneLite plugin list. Its sidebar has Start/Stop and equipment capture buttons.
2. Carry the standard charged/usable weapons: twisted bow **20997**, toxic blowpipe **12926**, black chinchompas **11959**, Eye of Ayak **31113**, and dragon claws **13652**, plus prayer potions. Provide arrows, loaded darts/scales and Eye charges/runes as required by the server. The controller selects these exact weapon IDs.
3. Stop the ranger while configuring. Wear your ranged setup and click **Capture range equipment**; repeat with magic and melee. Captures save armor, jewelry, ammo and the magic offhand. They exclude the weapon because the rotation chooses it; ranged/melee captures exclude shields for the two-handed weapons. The three sets can also be edited as item ID lists in plugin configuration. Empty sets switch only weapons.
4. Carry all captured items, keep the minimap visible, and stand north of the Nylocas entrance barrier with a complementary mage partner. Use the existing Theatre controls to create/join/start the raid. Press **Start ranger** when ready.
5. Once per Start, before the first barrier click, the ranger sets fixed and resizable camera zoom to the normal outer limit (**128**) and raises the camera pitch to at least **383**. This also corrects the previous extreme zoom preset. It does not repeat camera setup on subsequent ticks, so manual camera adjustments remain available while waiting and during the room.
6. The ranger clicks the barrier **once every game tick** while waiting and does not enter automatically. When both players are ready, press **Start room** on either the ranger or mage sidebar. That agent selects **Enter** in the Nylocas confirmation; the server also brings the waiting partner through. The ranger then takes its opening position and starts the rotation. **Start ranger** arms the controller; **Start room** separately authorizes entry. Stop or re-arming clears a pending room-start request.

Enabling the plugin arms it initially. It only acts near Nylocas; missing equipment stops it with an item ID in the sidebar. After a stop, logout, disconnect, death, room exit or boss spawn, use Start again at the entrance. Starting halfway through active waves is refused because the original birth ages would be unavailable. Starting with the server's training-wave setting is supported when the plugin was already tracking before that wave spawned.

Every Start rechecks current Hitpoints. A respawned player can restart even if the actor's cached death flag or health bar still shows the previous death; zero current Hitpoints keeps the ranger stopped.

Offensive-prayer switching is configurable and defaults on: Rigour for ranged, Augury for magic, Piety/LocalScape's Zeal for melee. Prayers stay active through cooldowns, movement and idle ticks. Preparing the next weapon does not change the offensive prayer; an actual attack in another style does. The controller activates a replacement prayer without explicitly toggling the old one off. Weapons use rapid ranged, longrange Eye, and aggressive claws. Before entry it prepares ranged equipment without activating prayers. Native controls enable run and disable auto-retaliate.

Protection management defaults on. It considers fighting nylocas targeting the local player, checks reach and line of sight, then prioritizes big attackers over small attackers. More bigs of one style win first; small counts break ties, followed by keeping the current overhead. Dying or inactive late-melee nylocas, pillar attackers, and enemies targeting the partner do not select protection. The last overhead stays active when no attacker needs a change.

Prayer-potion restoration defaults on at **20 points or below**, with a configurable threshold and three ticks between doses. All four standard dose variants are supported, partly used bottles are consumed first, and a dose is only requested when its restoration can fit. Potion use does not consume a ranger attack decision. Missing supplies produce a sidebar notice rather than stopping combat. Stopping the controller leaves the current prayers for the player to manage.

## Behavior

The live HUD supplies the last-spawned wave. NPC births supply each crab's own age and birth wave, including new splits; transformations update style while preserving identity. The tracker keeps dying bodies until despawn but excludes them from attacks. NPC indices are paired with distinct tracked lifetimes so slot reuse cannot inherit an old target's priority or claim.

Before wave 20, a planner compares up to three attacks over twelve ticks, keeping six candidate sequences. It considers current HP, cooldown, travel, lane movement, color changes, clusters and projectile claims. It attacks at the earliest useful opportunity, favours damaged ranged finishes over comparable full-HP smalls, and approaches the next planned target during cooldown. With no useful live target, it stages for the next wave's assigned lane. Most early replay offsets are flexible; only wave 10's chin packet and the coming wave-20 west flicker reserve cooldown. Dying bodies count toward population pressure, and opening bigs is discounted near the small cap because their children add work. Assigned bigs have completion goals that override ordinary small clearing before they become overdue. Early assigned bigs retain finish priority through misses and nonlethal hits only while pre-cap. As soon as wave 20 spawns, every surviving pre-cap big is excluded from targeting and unacknowledged pre-cap work is discarded. The wave-14 east big becomes eligible at age 8 during wave-15 east-side work and has an age-16 finish goal. Cooldown movement prioritizes approaching a big that needs service; when ready, an available attack still takes precedence over travel. Wave-10 chins and the wave-20 flicker retain their timing protection. The sidebar shows the proposed sequence; every tick can replace it as observations change. Projected damage is a ranking heuristic, never an observed kill or a mutation of tracked HP.

Wave 18 uses a separate policy: attack its bigs only once their observed anchors are inside the room, normally after the actual wave-19 age-five melee flicker, and aim to finish them before their age-26 mage change. The west big alone can be killed earlier when no smalls remain, including dying bodies. The delayed kills aim to shift split births past wave-20 admission. The shared wave-19 west mage is strict fallback and never acquires finish priority. The team goal is all three wave-18 and all three wave-19 bigs, plus west/south of wave 20, with zero wave-20 retries; the partner handles complementary mage work. Wave-20 admission still uses the old cap, so retained bodies matter.

From wave 20, the policy uses the named late lane packets and reserves cooldown and repositioning time before hard packets. Request offsets precede Blert's observed attack offsets by one tick: for example, wave-20 west bow is requested during age five's ranged flicker and is expected to launch at age six. Live style and predicted next-tick style must both match. This prevents a shot requested at the end of a window from arriving after the next color change.

From wave 20, between packets it prefers young ranged smalls and matching clusters, the phase's assigned bigs, Eye coverage, then nearby melee scratches. Pre-cap planning retains the complementary big/magic assignments and local melee fallback. It can finish an opened big, recover from a missed hit or failed action request, and use an ordinary claw attack when special energy is insufficient. A packet is completed only after an observed attack, not merely after sending its request. Teammate attack animations temporarily claim their primary target while a hit can be in flight.

If an attack request has no matching launch by the next game tick, the controller immediately retries or selects another eligible target. It does not wait three ticks or send a stop/movement command while waiting for acknowledgement. Confirmed attacks retain their normal weapon cooldowns and packet reservations.

Fresh wave-29 lane work is retained, including attacks carried into wave 30. Once the final wave-30/31 lane packets are completed, gone or expired, the cleanup filter ignores crabs born in waves ≤29. A new split from an older big remains eligible if the child was born in wave 30 or later.

Movement projects a reachable arena tile onto the native minimap and dispatches a canvas press/release. It does not rely on a client-only destination variable or a WALK opcode that merely opens scene selection. Reachability uses the client's collision flags; approaching a target also checks attack range and line of sight. It repositions during cooldown, cancelling automatic repeat attacks so they cannot consume a reserved shot or hit a changed color. Queued movement is invalidated when superseded by an attack or stopped.

## Implementation and verification

- [Plugin and room tracking](../client/src/main/java/net/runelite/client/plugins/nyloranger/NylocasRangerPlugin.java)
- [Strategy and named packets](../client/src/main/java/net/runelite/client/plugins/nyloranger/RangerStrategy.java)
- [Pre-cap attack planning](../client/src/main/java/net/runelite/client/plugins/nyloranger/RangerPreCapPlanner.java)
- [Lane flicker predictions](../client/src/main/java/net/runelite/client/plugins/nyloranger/RangerFlickers.java)
- [Native client actions](../client/src/main/java/net/runelite/client/plugins/nyloranger/RangerActions.java)
- [Sustained prayers, protections and potions](../client/src/main/java/net/runelite/client/plugins/nyloranger/RangerPrayers.java)
- [Regression tests](../client/src/test/java/net/runelite/client/plugins/nyloranger)

The targeted suite checks service of every assigned pre-cap big under competing smalls, wave-14 eligibility/finish continuity, dropping old bigs at wave 20, wave-18 room-entry/timing gates and early-west exception, shared wave-19 fallback and retained hard windows, damaged-target finishes, immediate attack availability, cooldown approaches, multi-attack color-window planning, flexible early timings, retained wave-10 chins, cooldown reservations, packet carry-over, age priorities, teammate claims, the cleanup cutoff, claw specials, gear/action ordering, sustained prayers, big-first protections, potion thresholds/dose cooldown/native drinking, actual canvas movement, blocked tiles, input cancellation, repeated barrier clicks without automatic confirmation, explicit Start room confirmation and cancellation, and the first live-event-driven attack. The replay test checks checksummed snapshots from all 93 qualifying rooms: living NPC color histories must agree with the prediction table, and every named packet must have a matching recorded target/weapon/timing example. Dying-body color freezes are deliberately excluded from living-flicker validation.

Run the tests after syncing the overlay with `from tools.dev import sync_client; sync_client()`, then run Maven from `upstream/runelite` with the workspace's JDK 21 and `.state/m2` repository:

```bash
mvn -o -B -Dmaven.repo.local=/home/jordan/osrs/.state/m2 \
  -Djava.awt.headless=true -Duser.home=/tmp/nyloranger-test-home \
  -Dshade.skip=true -pl runelite-client -am \
  '-Dtest=Ranger*Test,NylocasRangerPluginTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

These tests validate the policy and client interactions offline. They do not establish a live completion time or prove successful room completion with every partner and split outcome. A live duo session remains the check for server timing, supplies and actual resulting room performance.
