# LocalScape features

Gameplay, sidebar controls, and encounter behavior for LocalScape. See the
[README](README.md) for project setup, architecture, and development commands.

## Local Development sidebar

The custom client includes a **Local Development** plugin. Its terminal icon in
the RuneLite sidebar opens four independently collapsible sections, in order:
**Theatre controls**, **Gear setups**, **Debug tools**, and **Nylocas waves**. Theatre controls starts
expanded; the other sections start collapsed. Section contents and inputs
are preserved when collapsed. On login it also
writes `Custom RuneLite build active.` to game chat, confirming that RSProx
launched your build. Edit the Java sources under `client/` to extend it.

Open the terminal icon and expand **Debug tools** while logged into LocalScape:

- **Restore Skills** restores all skills, including Hitpoints and Prayer, to 99,
  reapplies the permanent combat boosts, and refills run and special attack
  energy to 100%.
- **Clear Inventory** removes all items from your inventory.
- **Spellbook** switches between Standard, Ancient, Arceuus, and Lunar. The dropdown
  follows your current book, including after login. Switching books stops combat
  and clears the current autocast selection.
- **Teleport to Nylocas** enters a fresh, empty instance of the original Nylocas
  room. Its map and scenery already exist in the revision-233 cache.
- **Teleport to Nylocas Bridge** lands on the bridge outside the room, at the
  instance equivalent of `(3295, 4264, 0)`.
- **Teleport to Bloat Exit** lands immediately before the Bloat room's exit
  door, at the instance equivalent of `(3322, 4447, 0)`. Use **Enter** or
  **Quick-Enter** on either half of the passage to reach the start of the Nylocas
  bridge at `(3302, 4275, 0)` within the same instance.
- **Start Raid** starts the raid in the current development instance and opens
  the native ToB party HP orbs in the top left. It includes up to five players
  already in the instance, synchronizes party names and HP, and preserves your
  position. Use it before entering the Bloat door or passing the Nylocas barrier.
- Search for an item by name or ID, select a result, enter a quantity, and click
  **Add Selected Item**. Results show IDs and identify noted items. Search reads
  the local game cache, includes untradeable items, and excludes placeholders.
  Broad searches show the first 100 matches; narrow the query to find more.

The item list loads over several client ticks after login. Controls use RSMod's
`master`, `invclear`, `invadd`, `nylocas`, `nylocasbridge`, `bloatexit`, `starttob`,
and `spellbook` admin commands. Your account needs server admin access; command
results and inventory-limit messages appear in game chat. Restore Skills preserves earned XP.

## Theatre controls and multiplayer lobbies

The **Theatre controls** section in Local Development provides multiplayer lobbies
for ordinary players:

1. The host clicks **Create lobby** to enter a fresh instance at the Bloat door.
2. Other players enter the host's player name and click **Join raid**. Names are
   case insensitive and may contain spaces. Parties support up to five players.
3. The host selects a **Starting wave** from 1–31 (default 1) and raid options,
   then clicks **Start raid** to apply them to the whole party, open the party HUD,
   and unlock the Bloat door.
   Everyone can then use **Enter** or **Quick-Enter** to reach the Nylocas bridge
   and pass the barrier together.

The host can choose **Max hit: all weapons** or **Max hit: 5 ticks or slower**
against correct-style Nylocas. These modes are mutually exclusive; **Normal hits** is
the default. Eligible attacks always pass accuracy and roll their maximum damage,
including each splat of multi-hit weapons. The slow mode uses the selected combat
style's attack speed (including Rapid); spells use their casting speed. Wrong-style
attacks and previously nulled targets keep their usual damage restrictions. Options
apply only to party members against Nylocas in their own raid.

**Replace Piety with Zeal** upgrades Piety for every party member during the raid:
+25% Attack, +28% Strength, and +25% Defence, with Piety’s prayer drain rate.
Zeal uses Piety’s icon, requirements, and quick-prayer selection. Leaving the raid
restores ordinary Piety. This option is off by default.

**Start everyone at 250 HP** sets every party member's current HP to exactly 250
once when the raid starts. Base Hitpoints and XP stay unchanged.

Options lock when the raid starts. The selected wave is the first to spawn, four ticks
after confirmed Nylocas entry. Starting at wave 21 or later immediately uses the
24-nylo cap; earlier starts use 12 until wave 20 has spawned. Create a new lobby to
change options for another raid. The host's selected starting wave appears in every
party member's game chat when the raid starts.

Creating or joining a lobby restores all skill levels, including HP and Prayer, to base,
and fully refills special attack and run energy.
Joining closes when the host starts. A host leaving or logging out closes their
lobby to new joins; creating another lobby starts a fresh instance. Server results
appear in game chat. The commands are `::theatrecreate`, `::theatrejoin <host name>`,
and `::theatrestart [starting wave] [maxhit-all|maxhit-slow] [hp250] [zeal]`, with no
admin access required. Rebuild the client with `./dev build-client`. With RSMod
stopped, run `./gradlew packCache` from `upstream/rsmod`, then restart it to load
these controls and Zeal’s temporary setting.

## Gear setups

The **Gear setups** section in Local Development saves and restores development
loadouts:

1. Equip your gear and arrange your inventory, including any empty slots.
2. Enter a setup name and click **Save current setup**. The server prefixes it
   with your player's name, for example `[Jordan] Nylo Duo`. Saving the same
   name again updates your setup.
3. Search by name and click a setup's button to apply its equipment and inventory.
   **Refresh setups** reloads the saved list.
4. Choose a **Default raid setup** from the dropdown, or **None** to keep your
   current equipment and inventory. The choice is saved per character on the
   server and restored on login. Any shared setup can be your default.

Creating or successfully joining a Theatre lobby automatically applies your
selected default to your player. Failed joins leave your gear unchanged. Starting
an existing lobby does not apply it again, so gear changes made before starting
are preserved. If a default setup is missing or invalid, you still enter the lobby
with your current gear and see a message in game chat.

Applying replaces both containers, including quantities and per-item charge data;
it also restores all skill levels, including HP and Prayer, to base, and fully refills
special attack and run energy. It does not withdraw from the bank. Setups are saved
on the server and shared with every player, including players who did not create
them. They persist across client/server restarts in RSMod's
`.data/gear-presets/<creator-character-id>.json`. Each character's default setup
ID is stored in their own file alongside their saved setups; choosing a shared
setup does not change its creator's preference.
Existing character files are included in the shared list automatically; resaving
an older setup adds its creator prefix. Entered names support 1–24 letters,
numbers, spaces and basic punctuation, with up to 100 setups per creator.
Search matches both creator and setup names. All players can save, list and apply
setups; saving cannot overwrite another player's setup. Rebuild the client with
`./dev build-client`, restart RSMod, and launch a fresh client session to load the
panel and server commands; no cache repack is required.

## Nylocas waves report

Expand **Nylocas waves** in Local Development for live party statistics. Recording
begins when the room starts at the entrance barrier. The last report survives room
completion, leaving, logging out, and creating another lobby; entering the next
room replaces it. Closing the client or disabling Local Development discards it.

- **One shot kills**, separately for smalls and bigs: combat kills from one damaging
  hitsplat, divided by combat kills of that size. Misses are ignored; multi-hit
  weapons count each damaging splat. Natural expiries are excluded.
- **Overkilled by Nylo style**: combat kills receiving another hitsplat on the lethal
  tick, divided by kills of that style at death. Zero extra splats count, including
  thralls; multiple extra splats count once per Nylo.
- **Stalls saved**: successful wave admission checks that would have stalled if
  overkilled bodies had remained for one extra tick. The calculation accounts for
  bigs delaying their two split children as well as their own removal. This counts
  immediate cap checks, rather than simulating a whole alternative room timeline.
  **Cap stalls** also shows actual four-tick cap retries.
- **Thrall kills**: Nylos whose lethal hit came from any party member's thrall.
- **Split children by style**: each phase's share of all split births, plus
  melee/ranged/magic counts and percentages within each birth phase: waves 1–20, waves 21–30, and cleanup after wave 31 spawns.
  Children from naturally expired bigs are included. The birth tick determines
  the phase, regardless of the parent's original wave.

The panel shows counts beside percentages and a dash when no denominator exists.
Updates come from authoritative server events over console messages without filling
game chat. Rebuild the client with `./dev build-client`, restart RSMod, and launch
a fresh client session; no cache repack is required.

## In-game keybinds

The in-game settings can assign F1-F12 or Escape to the side panels. Choosing
None removes a binding. Assigning a key to a different panel clears its previous
assignment, and Restore default keybinds restores the starter's F1-F12 layout.
All visible keybind dropdowns refresh when the server confirms a change, so
displaced bindings immediately display None.
Bindings are saved to the account and synchronized again at login. New accounts
receive defaults; saved bindings, including deliberately unbound panels, are
preserved.

The revision-233 server-controlled dropdown only changes its displayed text and
skips the native keybind setter. The custom client's hidden **Local Server Settings**
plugin captures the dropdown click's arguments (with the native setter as a
fallback for client-controlled dropdowns) and submits them through the existing
`ClientCheat` packet (`localscape_keybind` or `localscape_keybind_reset`). RSMod
accepts only the supported setting IDs and keys, updates the persistent
keybinding varps, and sends their values back to the client. Queued binding
changes and Restore defaults still save if Settings closes before delivery.
The native client handles the actual F-key presses.

The **Modern Layout – Side panel can be closed by the hotkeys** checkbox is
also saved to the account. Disabling it keeps the current panel open when its
hotkey is pressed again; enabling it allows the panel to close. The custom
client submits the native toggle's resulting value with
`localscape_sidepanel_hotkeys`, and the server preserves it across keybind
changes, restoring default keybinds, and logins.

The main in-game settings also save **Transparent inventory** and **Transparent
chatbox** independently to the account, including when either option is turned
off. Their native client scripts apply the appearance immediately in supported
resizable layouts. The hidden plugin captures each toggle's resulting value and
sends `localscape_transparency inventory <0|1>` or
`localscape_transparency chatbox <0|1>` to the server. The server updates the
persistent native varbits, preserves other settings packed into the same varp,
and synchronizes the saved values on login. These settings require the client
rebuild and server restart below; no cache repack is needed.

The main settings' **Chat box scrollbar position** dropdown switches between
**Left** and **Right** in resizable mode. The native client places the scrollbar;
the hidden plugin sends the selection with `localscape_scrollbar_position <0|1>`
(Right is 0, Left is 1). RSMod saves the preference to the account and restores it
on login without changing chatbox transparency or neighboring settings. Queued
selections still save if Settings closes before delivery. This uses the same
client rebuild and server restart below; no cache repack is needed.

Changes to this feature require both the custom client and the server:

```bash
./dev build-client
# Stop and restart RSMod with ./dev server, then launch a new RSProx session.
```

The settings close-button fix is already committed on the local `localscape`
branch as `2c9a471`. It handles `settings:close` and sends the interface close
packet while preserving the settings sidebar. Server integration tests also
cover closing and reopening the panel.

## World, combat, and equipment

Lumbridge is multi-combat throughout its defined map boundary on all four height
levels. Changes to map-area definitions require running `./gradlew packCache`
from `upstream/rsmod` with the server stopped, then restarting `./dev server`.

All seven ordinary cow and calf variants have 50 hitpoints. This is an NPC stat
override in `api/config/src/main/kotlin/org/rsmod/api/config/editors/NpcEdits.kt`;
NPC stat overrides also require repacking the cache with the server stopped.

Sulphur blades make two independent melee accuracy and damage rolls against
NPCs and players, with both splats landing on the same tick. Their max hits split
the normal melee max hit, rounding the first down and the second up (45 becomes
22 and 23).

Players start with base level 99 skills and permanent combat boosts: **118
Attack, Strength, and Defence; 112 Ranged and Magic**. The boosts use the super
combat formula (`5 + floor(base × 15%)`) and the ranging potion / saturated heart
formula (`4 + floor(base × 10%)`). They apply everywhere, including Nylocas,
and top back up after decay, stat drains, or Restore Skills without supplies or
activation. Higher boosts are preserved; XP and base levels stay unchanged.
The [saturated heart](https://oldschool.runescape.wiki/w/Saturated_heart) and
[Ranged boost table](https://oldschool.runescape.wiki/w/Ranged#Temporary_boosts)
document the shared four-plus-ten-percent boost.

Player melee, ranged, and powered-staff damage rolls, including godswords and
Sulphur blades, use a shared helper that rolls from zero through the max hit and
clamps the result to at least one. A rolled zero and a rolled one both deal one
damage after a successful accuracy check.

The **Necklace of anguish** uses the [Necklace of rupture's combat stats](https://oldschool.runescape.wiki/w/Necklace_of_rupture):
+20 Ranged attack, +8 Ranged strength, and +3 Prayer. **Dragon arrows** use
[Seeking dragon arrow](https://oldschool.runescape.wiki/w/Seeking_dragon_arrow)
mechanics: +20 Ranged attack while using a bow, the existing +60 Ranged strength,
and a minimum successful damage roll of 3. Rolls of 0, 1, and 2 become 3; misses
and damage restrictions still deal 0. Other weapons gain no arrow accuracy bonus.
After changing these item stats, run `./gradlew packCache` in `upstream/rsmod`
with the server stopped, then
restart RSMod.

Wearing **Lightbearer** restores 10% special attack energy every 25 ticks (15
seconds), instead of the normal 50 ticks. Equipping it shortens a longer remaining
wait to 25 ticks and preserves restores already due sooner. Removing it resets
the wait to 50 ticks. Login and death timer resets respect the equipped ring.
These rules follow the [Lightbearer mechanics](https://oldschool.runescape.wiki/w/Lightbearer).

The **Amulet of blood fury** gives each positive player melee hitsplat an
independent 20% chance to heal 30% of that splat, rounded down and capped at base
Hitpoints. Scythe, Sulphur blades, secondary targets, and melee special attacks
all use this shared effect, including against players. Healing uses the modified
damage roll before enemy Hitpoints clamping, matching the other local lifesteal
effects. Ranged, magic, and thrall hits do not trigger it. This implements the
[Blood Fury passive healing](https://oldschool.runescape.wiki/w/Amulet_of_blood_fury);
blood-shard charging and charge depletion are not implemented. Restart RSMod to
load these equipment effects.

Scythe of vitur attacks use one splat per NPC tile size, up to three, with each
splat rolling accuracy separately and landing on the same tick. Each successive
max hit is half the previous one, rounded down (51 becomes 51, 25, 12). This also
applies to the holy, sanguine, and blighted cosmetics, including all uncharged
forms. Player targets receive one standard hit.

Scythe and Sulphur blades award combat XP per splat, with each splat capped
separately to the target's remaining hitpoints at attack time. Two 8s against
an 8-HP target award XP for 16 damage.

Against a 1x1 NPC in multi-combat, the scythe sweeps the primary target's tile
and the two tiles beside it, perpendicular to the attack direction. It hits up
to three eligible 1x1 NPCs overall, including NPCs stacked on the same tile.
The primary receives the full max hit; additional targets receive half and
quarter max hits with independent accuracy rolls. The local ordering checks
other NPCs on the primary tile, then the player's left, then right, skipping
empty tiles and ineligible NPCs. This ordering is an assumption: [Jagex's original description](https://secure.runescape.com/m=news/dev-blog-theatre-of-blood?oldschool=1)
confirms the arc and damage progression but does not assign damage to either
side. See the [research note](docs/research/scythe-aoe.md). Restart RSMod to load
the change; no cache repack is required.

The Eye of ayak is a powered staff with a three-tick attack cycle and a base max
hit of `floor(current_magic_level / 3) - 6`, before normal magic damage bonuses.
It uses its native casting and projectile effects, with the `shadow_cast` sound
(178), rather than Tumeken's Shadow's firing sound (6410). Its projectile follows
the [Eye of Ayak hit-delay table](https://oldschool.runescape.wiki/w/Hit_delay#Eye_of_Ayak).
That table counts waiting ticks before damage processing: actual hits and splashes
resolve two ticks after casting at distances 1–2 and three ticks at distances 3–8,
for both NPC and player targets. Its attack range is six tiles, or eight on Longrange.
It holds up to 50,000 charges:
either two death runes and one chaos rune per charge, or one demon tear. The two
charge types cannot be mixed. Use charging materials on the staff, or use Charge
on the uncharged item; Check reports the remaining charges, and Uncharge refunds
the original materials. Each cast consumes one charge, including splashes, and
the last charge turns it back into the uncharged item. Charging costs follow the
[OSRS rules](https://osrsindex.com/wiki/eye-of-ayak?site=osrs_wiki).

Eye of ayak and all ancient sceptre variants use upright staff animations while
idle, turning, walking, and running. Powered staves, including Eye of ayak and
Tumeken's shadow, award 1.33 Magic XP and 1 Defence XP per damage on Longrange;
Accurate retains 2 Magic XP per damage. This supplies the Defence XP used by
[Nylo Death Indicators](https://github.com/InfernoStats/Nylo-Death-Indicators/blob/1b29a76e1799cf813299bfdc75e12251028643ae/src/main/java/com/infernostats/NyloDeathIndicatorsPlugin.java#L293-L327)
to predict Longrange kills before the projectile lands. Restart RSMod to load
these changes.

Combat weapon and spell cast sounds play around the attacker so nearby players
can hear them. Dragon claw specials play sound phases `1, 2, 3, 3` privately for
the attacker, including the ornamented variant. The sounds start at approximately
0, 0.38, 0.64, and 1.00 seconds, matching the pause after the first sound in the
[Wiki recording](https://oldschool.runescape.wiki/w/Dragon_claws#Special_attack).
Both bulwark variants play their bash sound, and ancient spells play
one cast sound per cast even when they hit multiple targets or splash.

Ranged weapons and specials use their original hit delay: one queue tick plus
the projectile's client cycles divided by 30, rounded down. Close-range ranged
hits therefore land one tick after melee. Powered staves and standard and ancient
spells also include a queue tick after projectile travel, for hits and splashes.
The timing conversion is verified against RuneLite's
[native hit-delay calculation](https://github.com/runelite/runelite/blob/runelite-parent-1.11.19/runelite-client/src/main/java/net/runelite/client/plugins/specialcounter/SpecialWeapon.java).
Thralls retain their own projectile timing.
Two-projectile attacks retain each projectile's individual delay; the blowpipe
special still takes its extra travel tick outside adjacent range.

Dinh's bulwark and Dinh's blazing bulwark have the Shield Bash special attack,
costing 50% energy and using a custom 25% accuracy bonus against crush defence.
In multi-combat, the primary NPC takes two independently rolled, full-strength
hits on the same tick. The sweep hits up to ten NPCs total, including the primary,
within an 11x11 square centred on the player. Other targets must be attackable,
visible, in multi-combat, and in line of sight from the player. The primary NPC
also requires line of sight. Nylocas in the incoming lanes are excluded; only
Nylocas inside the room can be hit. If a large incoming Nylo is already in melee
reach, the special stays armed until its southwest tile enters the room, without
spending energy or starting an attack delay early. Single-combat and player
targets receive one hit. Successful hits drain the highest offensive stats by 5% of their
current levels, once per target per special; melee drains both Attack and Strength,
with ties preferring melee, then Ranged, then Magic. Energy, area restrictions,
and stat drain follow the [Wiki special attack rules](https://osrsindex.com/wiki/special-attacks?site=osrs_wiki)
and [stat drain rules](https://osrsindex.com/wiki/temporary-skill-drain?site=osrs_wiki),
with the requested accuracy bonus and primary-plus-sweep behavior.

Normal toxic blowpipe attacks require stored
Zulrah's scales and unpoisoned darts. Use either material on the blowpipe to load
up to 16,383 of each; unload its darts before changing dart type. Check reports
the contents, and Unload lets you recover just the darts or all darts and scales.
The blowpipe adds +20 Ranged strength to its loaded dart's strength and uses +30
Ranged accuracy, ignoring quiver ammunition. Rapid attacks take two ticks;
Accurate and Longrange take three. Range is five tiles, or seven on Longrange.
The dart sound effect plays at the player's position on the attack tick.
Shots consume a dart unless Ava's device saves it, and consume a scale two-thirds
of the time, including on misses. Running out of one resource preserves the other.
The Toxic Siphon special attack uses the regular attack animation for each
blowpipe cosmetic, costs 50% energy, doubles accuracy, and increases maximum
damage by 50%. It heals half the rolled damage, rounded down, before
damage is capped to the target's remaining hitpoints; healing cannot exceed the
player's maximum hitpoints. Its distinct projectile takes one extra tick to land
unless the player is adjacent to the target. The regular toxic blowpipe supports it.
These charging and normal attack mechanics follow the
[OSRS Wiki](https://oldschool.runescape.wiki/w/Toxic_blowpipe).

**Ascension crossbows** reuse the blazing blowpipe model and animation (loaded item
`28688`, empty item `28687`). They share the blowpipe's scale/dart loading,
unloading, capacity, ammo saving, and consumption mechanics. With dragon darts,
their total bonuses are +34 Ranged accuracy and +58 Ranged strength: +23 from the
weapon and +35 from the darts. Other unpoisoned darts remain loadable and contribute
their own strength. Rapid attacks take two ticks; Accurate and Longrange take three.
Range is six tiles, or eight on Longrange. A charged or blessed Dizana's quiver,
including the max cape variants, adds its conditional +10 accuracy and +1 strength
on top of its normal cape stats while darts are loaded. Quiver-slot ammunition
adds no stats and is never consumed. Uncharged quivers provide only their normal
cape stats. Ascension crossbows have no special attack; their native special bar
and orb are disabled through the transmitted energy-requirement enum.
Repack with `./gradlew packCache` in `upstream/rsmod`, then restart the server and
relaunch the client to pick up the names, stats, range, and special UI changes.
No RuneLite client rebuild is required.

Charged **Tonalztics of Ralos** (item `28922`) make two independent ranged accuracy
and damage rolls per attack, each capped at `floor(normal_max_hit * 0.75)`.
The [Wiki DPS calculator](https://github.com/weirdgloop/osrs-dps-calc/blob/main/src/lib/PlayerVsNPCCalc.ts)
confirms the independent rolls and 75% cap. Both splats land together two
server ticks after firing at every distance. Projectile timing matches a twisted
bow fired from one tile (51 client cycles), with no increase over distance.
Each splat awards XP separately and respects encounter damage restrictions.
The weapon uses its charged attack animation and two native projectiles, stays
equipped, and does not consume quiver ammunition. This implements normal attacks
for the charged item directly, without Sunfire splinter charging or the special
attack. Its existing attack speed remains seven ticks, or six on Rapid.
Run `./gradlew packCache` in `upstream/rsmod` with the server stopped, then restart
RSMod to load the animation and projectile definitions. No client rebuild is needed.

Black chinchompas attack from nine tiles, or ten on Longrange. Medium fuse
(Rapid) takes three ticks; Short and Long fuse take four. In multi-combat,
each throw selects additional targets in the 3x3 square around the primary
target's southwest tile, including NPCs stacked on that tile. Only southwest
tiles count, and splash requires the engine's line of sight from the primary.
Targets are fixed at launch. One accuracy roll against the primary controls
the whole throw: a miss deals zero to everyone; a success rolls each target's
damage independently. Each throw consumes one chinchompa, including misses,
without Ava's recovery or a ground drop. Quiver bonuses do not apply.
Each throw plays the shared throwing-weapon release sound (2706), followed by
one explosion sound (360) when the projectile lands, including on misses.
Both sounds play privately for the thrower; targets and nearby players cannot hear them.
Elite Void preserves its full 12.5% ranged damage bonus: at 112 Ranged with
Rigour, Medium fuse, and +44 ranged strength, black chinchompas have a max hit of 28.
Restart RSMod to load these changes.

## Spellbooks and spells

Carrying a normal, locked, divine, locked divine, or LMS rune pouch supplies
unlimited runes for spells. Every spell icon lights up, and rune tooltips show
unlimited quantities. Removing the last pouch restores normal rune checks.
Pouches and loose runes are not consumed; required staves and other non-rune
ingredients still apply on the server. This establishes spellbook selection and
rune availability; individual spell effects are implemented separately.

**Ice Blitz** is available from the Ancient spellbook against a single NPC or
player. It uses the shared five-tick spell attack cycle, requires level 82 Magic,
and has a base max hit of 26 before magic damage bonuses. Successful casts freeze
the target immediately for 24 ticks (14.4 seconds); damage and impact effects
follow the normal distance-based spell impact delay. Splashes consume runes but
do not freeze. Existing freezes, the five-tick thaw immunity, and NPC freeze
resistance are respected. Without a rune pouch, each cast costs three water,
two blood, and two death runes. Spell stats and the base freeze duration follow
the [OSRS Wiki](https://oldschool.runescape.wiki/w/Ice_Blitz).

**Ice Barrage** uses the same 3x3 target selection as black chinchompas: the square
is centred on the primary target's southwest tile, includes stacked targets, and
checks line of sight from that tile. Targets are selected at cast time. Splash
requires the caster, primary, and additional targets to be in multi-combat;
player splash targets must also pass the usual wilderness level checks.
Each target receives its own accuracy and damage rolls, so a cast can freeze
some targets and splash on others even when the primary splashes. Both Ice Blitz
and Ice Barrage apply freeze only after a successful accuracy roll, immediately
at cast time. Barrage uses a five-tick attack cycle and a 32-tick freeze, with a
base max hit of 30. It costs six water, two blood, and four death runes once per
cast, regardless of target count, or uses the rune pouch's unlimited runes.
The base spell stats follow the [OSRS Wiki](https://oldschool.runescape.wiki/w/Ice_Barrage);
the target selection deliberately matches this server's chinchompa rules.
The server overrides the cache's placeholder max-hit values with 30 for Ice
Barrage and 26 for Ice Blitz before applying prayer and equipment damage bonuses.
These are base values, so boosted max hits can exceed them.

**Blood Barrage** uses the same area targeting, individual accuracy rolls, and
five-tick attack cycle as Ice Barrage. Its base max hit is 29 before prayer and
equipment bonuses. It applies no freeze. At cast time, it heals the caster for
`floor(total successful damage rolls / 4)`, rounding once across all targets.
Healing uses the damage rolled before limiting it to the targets' remaining HP;
splashes contribute zero. Healing stops at the caster's base maximum Hitpoints.
Rune costs and cast XP apply once per cast, regardless of the number of targets.
Restart RSMod to load it; no cache repack or client rebuild is required.

**Greater thralls** are available on the Arceuus spellbook: **Resurrect Greater
Zombie** (melee), **Resurrect Greater Skeleton** (ranged), and **Resurrect Greater
Ghost** (magic). They require level 76 Magic, six Prayer points, and a Book of the
dead in inventory or equipped, plus ten fire, five blood, and one cosmic rune
(or a rune pouch). Casting awards 88 Magic XP and preserves the player's existing
attack cooldown, ongoing attacks, and pending actions, including after Spellbook
Swap. Resurrection spells share a 16-tick cooldown; a successful recast
replaces the existing thrall. Each style plays its own resurrection sound at the
summoning tile, audible to nearby players; rejected casts remain silent.

Each thrall lasts two ticks per current Magic level at casting: **224 ticks / 134.4
seconds** with the server's permanent Magic boost. The doubled master Combat
Achievements duration applies to every player. Idle thralls wander within two tiles of
where they were summoned or last fought, taking occasional random steps rather
than following the player. Thralls attack the owner's last attacked NPC for up to
12 ticks after the player's last attack, then return to wandering until the
player attacks again. They attack independently every four ticks, rolling
**0–3 damage without an accuracy check** and awarding no combat XP. Zombies walk
into melee range; skeletons and ghosts attack within six tiles with line of sight.
Skeleton projectiles release one tick later than ghost projectiles; their hit
timing follows each projectile's impact tick. Thrall damage grants the owner kill
credit and respects NPC immunity and Nylocas styles. Wrong-style attacks on
Nylocas still animate and land as zero hits, which can speed up death through
overkill without nulling the owner. Thralls cannot take damage, attack players, or
retaliate. They disappear on expiry, owner death/logout, or moving out of the
owner's vicinity. The [OSRS Wiki resurrection mechanics](https://oldschool.runescape.wiki/w/Resurrect_Greater_Zombie)
describe the base duration, damage, costs, and cooldown. The projectile definitions
require `./gradlew packCache` from `upstream/rsmod` with the server stopped, then
an RSMod restart. No client rebuild is required.

**Death Charge** is available on the Arceuus spellbook. Casting grants two stacks
for 60 seconds. Each NPC kill credited to the caster consumes one stack and
restores 15% special attack energy, capped at 100%. Both stacks share the original
expiry, and the 60-second cast cooldown continues after both stacks are used.
Casting preserves ongoing attacks and pending actions, including after Spellbook Swap.
Successful casts play the native Death Charge cast sound around the caster;
failed or cooldown-blocked casts remain silent.
The spell uses its normal level and rune requirements, including unlimited runes
from a rune pouch. Restart RSMod to load it; no cache repack or client rebuild is
required.

**Mark of Darkness** is castable on the Arceuus spellbook without a target. It
checks the normal level and rune requirements and awards cast XP, but applies no
buff or combat effect. After swapping from Lunar to Arceuus with Spellbook Swap,
casting it immediately returns you to Lunar. Failed casts preserve the swap.
Casting leaves attack cooldowns, ongoing attacks, and the player's current target intact.
Restart RSMod to load it; no cache repack or client rebuild is required.

**Vengeance** and **Vengeance Other** are available on the Lunar spellbook. They
grant the caster or selected player a status that reflects `floor(damage * 0.75)`
back to the next NPC that damages them, then consumes the status. Reflection uses
the Hitpoints actually lost, including on lethal hits. Zero hits, player attacks,
and environmental damage leave the status active. The two spells share a
30-second cooldown per caster, which continues after reflection; receiving
Vengeance Other does not start the recipient's cast cooldown. Normal level and
rune requirements apply, including unlimited runes from a rune pouch. Successful
casts play the matching native Vengeance or Vengeance Other sound around the
recipient; failed or cooldown-blocked casts remain silent. Restart RSMod to load
them; no cache repack or client rebuild is required.

**Energy Transfer** is available on the Lunar spellbook at level 91 Magic. It
costs 3 Astral, 2 Law, and 1 Nature rune, all of the caster's special attack
energy, and 10 HP; the caster must have more than 10 HP. It restores the
recipient's special attack and run energy to full four ticks after casting,
with the receiving visual effect arriving at the same time. The native Energy
Transfer sound plays around the caster immediately. Costs are paid and 100 Magic
XP is awarded when casting. Delivery continues through subsequent actions and
skips recipients who die or log out before it arrives.
The recipient must enable Accept Aid. Both players must be in multi-combat
when inside the Wilderness. Casting interrupts the caster's current action
without adding an attack delay or resetting an existing attack cooldown.
Restart RSMod to load it; no cache repack is required. Mechanics follow the
[Energy Transfer wiki reference](https://oldschool.runescape.wiki/w/Energy_Transfer).

**Spellbook Swap (SBS)** is available on the Lunar spellbook. Choose Standard,
Ancient, or Arceuus to play the swap animation and change books three ticks later.
The 130 Magic XP is awarded when that three-tick stall finishes.
Red-x targets remain active throughout the stall and the player keeps facing them.
For a target clicked earlier in the casting tick, movement starts after the stall;
for an interaction already active on an earlier tick, an approach in progress continues.
Ranged attacks already within weapon range stay stationary throughout the stall.
Attacking, passing barriers, and other interactions wait until the stall ends.
Ordinary ground movement pauses during the stall and resumes afterwards.
Movement or attack clicks received after SBS starts are ignored during the stall.
The spell's named menu options cast directly; the generic **Cast** option opens
the spellbook selection dialogue.
The next successful spell cast returns you to Lunar immediately, including combat
splashes and Home Teleport; failed casts preserve the swap. The temporary book
expires after 200 ticks (two minutes) without a cast. Returning clears autocast,
and logging out also restores Lunar. Normal level and rune requirements apply,
including unlimited runes from a rune pouch. Restart RSMod to load it; no cache
repack or client rebuild is required.

Rebuild with `./dev build-client`, restart RSMod, and launch a new client session
to load spellbook controls and rune pouch support. No cache repack is required.

## Nylocas encounter

Nylocas access preserves source region **13122**, plane **0**, and lands at the
instance equivalent of `(3295, 4248, 0)`. ToB QoL requires an instance with the
original source chunks. Nyloer and Nylo Death Indicators also depend on official
NPCs, raid state, and combat events when the encounter is added. The compatibility
research and remaining requirements are in [docs/nylocas-research.md](docs/nylocas-research.md).
The recording-backed movement, occupancy, and lifecycle investigation, including
proposed accuracy tests, is in [docs/nylocas-simulation-accuracy-report.md](docs/nylocas-simulation-accuracy-report.md).
Use `::nylocas` or `::nylocasbridge` directly as an admin, or rebuild the custom
client and restart RSMod to use the sidebar buttons. No cache repack is required.
Each click creates a new instance; the normal region allocator reclaims abandoned rooms. To leave,
use the existing `::tele 0 50 50 22 18` command to return to Lumbridge.

`::bloatexit` is the command behind the Bloat exit button. This instance contains
both Bloat and Nylocas source maps, spaced apart so the client loads the correct
room's region. The cache supplies the doorway scenery; the server adds the native
Formidable passage clickboxes at the exit. Traversing the door keeps players in
the same instance and lands at `(3295,4258)`, one tile west of the combat dummy
near the Nylocas barrier. Traversal does not start the Nylocas wave. Bloat combat
is not implemented.

A combat dummy stands on the bridge just outside the Nylocas barrier, at source
tile `(3296,4258)`. It stays still, never retaliates, and restores its health after
each hitsplat. Melee, ranged, spells, powered staves, and special attacks always
land at their maximum damage; weapon multipliers and multi-hit splits still apply.
It is available before starting the raid.

To start the first Nylocas wave, teleport to the Bloat exit or Nylocas bridge,
click **Start Raid** (or use `::starttob`), walk to the barrier, and click **Pass**.
The barrier refuses entry until the raid has started. Before the room begins,
**Pass** opens a chatbox confirmation: press **1** to enter or **2** to cancel.
Walking away also cancels. When one player confirms, everyone still waiting at
either half of that instance's barrier enters on the same tick. Once the room
has started, **Pass** enters immediately without confirmation. Either half walks
you through with a one-tick action delay. The first confirmed entry starts the
room clock at tick 0, and wave 1 spawns exactly four ticks later, regardless of the wait after
**Start Raid**. Further entries share the running room clock. Entry also spawns
the four native pillar NPCs, giving ToB QoL a valid timer start so its **Waves**
and **Cleanup** splits appear during the encounter. The first wave has a small
mage south targeting NE, a small ranger west targeting players,
and a small melee east targeting NW. All **31 normal-mode lane waves** now spawn
at their natural intervals, subject to room-cap stalls, with 120 recorded
spawns, fixed initial targets, and style changes at ages 5, 7, and 26. Repeated entry does not restart the
sequence; use a teleport to create a fresh instance for another attempt.
Passing from inside returns you to the bridge. Surviving nylocas now expire:
smalls disappear at spawn+52 ticks and bigs at spawn+55, after their explosion
animations. Each big produces two smalls on its removal tick, after either a
player kill or natural expiry. They appear at its final southwest anchor and one
tile northeast, choose styles and pillar targets independently, and receive full
scaled HP and a fresh 52-tick lifetime. The research's rare shifted birth positions
still need an established placement rule. The provisional blast hits players within one tile of the NPC
footprint for 0–18 damage (small) or 0–21 (big), ignoring protection prayers.
Damage lands when the expiry death animation starts: spawn+51 ticks for smalls
and spawn+52 for bigs. Player-killed Nylocas do not deal explosion damage.
The room cap starts at 12 and increases to 24 after wave 20 spawns. A due
wave stalls when the existing count is at or above the cap and retries four
ticks later. Dying bodies count until removal; an admitted wave spawns in full.
At age 26, the wave 18 west big switches to magic, the wave 19 east big to
melee, and the wave 20 south big to ranged. These three styles are user-confirmed.
Nylocas accept damage only from their current style: Hagios from magic,
Toxobolos from ranged, and Ischyros from melee. A wrong-style attack deals zero
and permanently nulls that individual Nylocas for that player, even after switching
to the correct style or a flicker. Teammates and other Nylocas are unaffected.
Each victim of chinchompas, barrage, Dinh's special, and scythe sweeps is checked
separately. Nulling is recorded when the attack rolls, before projectile impact;
blocked hits award no damage XP, freeze, stat drain, or Blood Barrage healing.
Every small and big wave form plays the native Nylocas defend sound (4020) for
the attacker on each hit impact, including zero-damage and lethal hits. Each
splat produces its own sound, including multiple hits in the same tick. Repack
the cache with the server stopped, then restart RSMod to load this NPC setting.
Attacking an already-nulled Nylocas plays the native warning/error buzzer
(`errorsound`, 2394), amplified with two synchronized copies for the attacker,
and flashes the native yellow shield orb around it. The attack that first nulls
the Nylocas produces neither cue, including any additional splats that tick.
Subsequent multi-hit attacks produce one cue per target per tick.
Small Nylocas die two ticks after lethal impact, or one tick after an additional
hitsplat, whether walking or stationary. Walking adds an arrival tick only to
big Nylocas deaths.
Regular big Nylocas use their southwest tile for terrain and NPC occupancy
checks, while retaining their 2×2 footprint for pillar contact and combat reach.
Their other three tiles can overlap smalls or other bigs. Player-targeting big
Nylocas switch to their full 2×2 movement and occupancy footprint when they
enter the arena and change to their fighting form. Pillar-bound Nylocas ignore
players while fighting Nylocas remain blocked by them. Ordinary pillar attackers
check NPC collision at the chosen destination, allowing diagonals past occupied
side tiles while still respecting terrain corners. They approach a geometric
pillar goal and retry locally when crowded; the retry choice is a provisional
random policy rather than a recovered server direction algorithm.
Removing an ordinary big clears NPC collision across its native 2×2 footprint,
which can leave a stationary neighbour nonblocking and allow a later arrival to
stack. Split births can reserve those tiles again. Fighting bigs retain full
movement and removal footprints. Recording comparisons and remaining limits are
in [pathing verification](docs/nylo-pathing/verification.md).
After wave 31 and the last wave body or split child disappears, Nylocas Vasilias
drops 16–19 ticks later, aligned to the room's four-tick wave cycle. It becomes
attackable in melee two ticks after dropping. The opening melee form lasts nine
ticks; subsequent forms switch to a different random style every ten ticks.
Matching-style hits damage it. Other styles heal it, capped at maximum HP, and
reflect the full damage to the attacking player without protection-prayer reduction.
These checks currently use the form at impact, including projectiles crossing a
switch; the precise native boundary remains unverified. Boss HP is 1875 for
1–3 players, 2187 for four, and 2500 for five. Its native attacks respect
protection prayers, and its death completes the room and clears the progress bar.
See [boss timing evidence](docs/nylocas-boss-evidence.md) for the replay measurements
and the remaining attack-timing and visual-effect limitations.
See [wave implementation details](docs/nylocas-wave-implementation.md) for
provenance, verification, and the remaining encounter mechanics.
Starting the raid again does not reset its wave or progress. Leaving the instance
or logging out clears your party state and closes the HUD. Each player's own HP
orb follows native stat updates, while party HP values update every server tick.
For the intended approach, use **Bloat Exit → Start Raid → Enter → Pass**.
The entire Theatre of Blood map is multi-combat on all four height levels,
including Nylocas, its lanes/bridge, the other boss rooms, and connecting/reward
areas. Source-map membership applies to every instance and drives both combat
rules and the native multi-combat indicator, before or after Start Raid.

Restart RSMod to load encounter script changes. Changes to
`TheatreMapAreas.kt` or `NylocasNpcEdits.kt` additionally require running `./gradlew packCache` from
`upstream/rsmod` with the server stopped, then restarting it. No client rebuild
is needed. The cache was repacked locally for the current multi-combat change.
