# LocalScape

A Linux development workspace for a custom RuneLite client, an RSMod server,
and RSProx packet inspection. The baseline is OSRS revision 233 and RuneLite
1.11.19.

```text
custom RuneLite → RSProx → RSMod game and cache/update services
local HTTP service → launch configuration, world list, custom client manifest/JAR
```

The custom client includes a **Local Development** plugin. Its terminal icon in
the RuneLite sidebar opens item search and server controls. On login it also
writes `Custom RuneLite build active.` to game chat, confirming that RSProx
launched your build. Edit the Java sources under `client/` to extend it.

A second sidebar panel, **Theatre controls** (red T icon), provides multiplayer
lobbies for ordinary players:

1. The host clicks **Create lobby** to enter a fresh instance at the Bloat door.
2. Other players enter the host's player name and click **Join raid**. Names are
   case insensitive and may contain spaces. Parties support up to five players.
3. The host selects a **Starting wave** from 1–31 (default 1), then clicks **Start raid**
   to apply the options to the whole party, open the party HUD, and unlock the Bloat door.
   Everyone can then use **Enter** or **Quick-Enter** to reach the Nylocas bridge
   and pass the barrier together.

Options lock when the raid starts. The selected wave is the first to spawn, four ticks
after confirmed Nylocas entry. Starting at wave 21 or later immediately uses the
24-nylo cap; earlier starts use 12 until wave 20 has spawned. Create a new lobby to
change options for another raid. The host's selected starting wave appears in every
party member's game chat when the raid starts.

Joining closes when the host starts. A host leaving or logging out closes their
lobby to new joins; creating another lobby starts a fresh instance. Server results
appear in game chat. The commands are `::theatrecreate`, `::theatrejoin <host name>`,
and `::theatrestart [starting wave]`, with no admin access required. Rebuild the client with
`./dev build-client` and restart RSMod to load these controls; no cache repack
is required.

## First setup

Use a Linux desktop session, including WSLg, with Git and Python 3.12 or newer.
Downloads require internet access. Initial setup downloads source, build
dependencies, and RSMod's cache, and may take several minutes.

```bash
./dev setup
```

Setup:

1. Initializes the RSMod submodule from `https://github.com/Totomosic/rsmod` at
   the commit recorded by this workspace, and checks out RuneLite's
   `runelite-parent-1.11.19` tag and the configured RSProx ref under `upstream/`.
2. Records exact RuneLite and RSProx commits in `sources.lock.json`; subsequent
   setups reuse them. RSMod's commit is tracked by Git's submodule pointer.
3. Installs missing Temurin JDK 21 and JDK 11 under `.state/tools/`. JDK 21 runs
   the tools/apps; RSProx requests JDK 11 as its compile toolchain. Maven 3.9.11
   is downloaded there if Maven is unavailable.
4. Downloads a revision-233 launch-config template, rewrites its initial world
   and world-list URL for localhost, and generates a binary world list.
5. Runs RSMod's installer to prepare its cache and generate its RSA key pair.
6. Reads only the public modulus from RSMod's `.data/client.key` into the
   project's RSProx target configuration.
7. Copies `client/src/main/java/` into the RuneLite checkout, builds its thin
   client JAR with Maven, and generates a local manifest referencing that JAR.

RSMod's pinned source targets revision 233. Setup checks the source revision
and stops if that changes. Setup preserves initialized submodule checkouts,
including local edits and commits; it never resets or automatically pulls them.
To initialize all submodules immediately after cloning, use
`git clone --recurse-submodules <workspace-url>`.

To use existing tools, set:

```bash
export JAVA21_HOME=/path/to/jdk-21
export JAVA11_HOME=/path/to/jdk-11
export MVN=/path/to/maven/bin/mvn
```

Java 25 alone is insufficient for these pinned builds. `./dev tools` installs
the requested JDKs without changing your system Java. `./dev doctor` reports
tools, source directories, and the desktop display.

## Run locally

After setup, start a tmux session:

```bash
./tools/tmux.sh
```

This creates three separate windows: `http`, `rsmod`, and `shell`,
with the general shell selected initially. Add `--rsprox` to also create an
`rsprox` window and launch RSProx:

```bash
./tools/tmux.sh --rsprox
```

When enabled, RSProx waits for the HTTP service to start. Running the script again reattaches
to the existing `localscape` session without starting duplicate services. Inside
tmux, it switches to that session. Use `Ctrl+b`, then `n` or `p` to change windows,
or `Ctrl+b`, then `d` to detach while services keep running.

Use `./tools/tmux.sh --detach` to create the session without attaching, or pass a
different session name. Failed commands leave their window's shell available so
you can inspect the error and rerun the command.

Existing sessions keep their layout. If you already started the earlier tiled
session, use `./tools/tmux.sh localscape-windows` to create a fresh session after
stopping its service commands.

Alternatively, start the service commands manually in three terminals:

```bash
# Terminal 1: configuration and client artifacts on 127.0.0.1:8080
./dev serve

# Terminal 2: RSMod game server (default game port 43594)
./dev server

# Terminal 3: RSProx GUI
./dev proxy
```

Wait for RSMod startup, then in RSProx choose:

- **Proxy Target:** LocalScape
- **Client Type:** RuneLite
- **Account:** Default

Click **Launch Session** and use a local test account on the game login screen.
Successful login should show the custom-build message in chat and packets in
RSProx. Leave the HTTP service and server running during client startup.
Stop each foreground command with Ctrl+C; close the game client separately.

RSProx and its child launcher/client use `.state/home/` as Java's `user.home`.
Settings, caches, sockets, and recordings stay in the workspace. The generated
target file is `.state/home/.rsprox/proxy-targets.yaml`. Plugin Hub plugins can
load, and the GPU plugin is available to enable in RuneLite's plugin settings.

The HTTP service binds to loopback. RSMod uses its upstream network binding
defaults; this scaffold does not change its listener configuration. `game_port`
in `project.json` describes the target port, so changing it also requires changing
RSMod's listener configuration.

## Change the server

If server ticks remain above 600 ms on WSL, check for operating-system clock
skew before changing the game loop. Competing Ubuntu and Hyper-V time
synchronization made a nominal 600 ms tick take roughly 640 ms in real time in
this workspace. See the [diagnosis and clock fix](docs/server-tick-timing.md).

The server at `upstream/rsmod` is a submodule of your RSMod fork. Its `origin`
points to `https://github.com/Totomosic/rsmod`. In the existing development
checkout, the original `rsmod/rsmod` remote is retained as `upstream`.

Submodule initialization normally checks out a detached commit. Before editing
on a fresh clone, create a development branch:

```bash
git -C upstream/rsmod switch -c localscape
```

Edit the server's Kotlin sources, then stop and restart `./dev server` to rebuild
and run the changes. Commit server changes in `upstream/rsmod` and publish them
to your fork before recording the new submodule pointer in the workspace:

```bash
git -C upstream/rsmod add <changed-server-files>
git -C upstream/rsmod commit -m "Describe the server change"
git -C upstream/rsmod push -u origin localscape
git add upstream/rsmod
git commit -m "Update RSMod server"
```

The settings close-button fix is already committed on the local `localscape`
branch as `2c9a471`. It handles `settings:close` and sends the interface close
packet while preserving the settings sidebar. Server integration tests also
cover closing and reopening the panel.

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

Scythe of vitur attacks use one splat per NPC tile size, up to three, with each
splat rolling accuracy separately and landing on the same tick. Each successive
max hit is half the previous one, rounded down (51 becomes 51, 25, 12). This also
applies to the holy, sanguine, and blighted cosmetics, including all uncharged
forms. Player targets receive one standard hit.

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
the [Eye of Ayak hit-delay table](https://oldschool.runescape.wiki/w/Hit_delay#Eye_of_Ayak):
one tick at distances 1–2 and two ticks at distances 3–8. NPC processing adds one
tick to those listed delays. Its attack range is six tiles, or eight on Longrange.
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

Dinh's bulwark and Dinh's blazing bulwark have the Shield Bash special attack,
costing 50% energy and using a custom 25% accuracy bonus against crush defence.
In multi-combat, the primary NPC takes two independently rolled, full-strength
hits on the same tick. The sweep hits up to ten NPCs total, including the primary,
within an 11x11 square centred on the player. Other targets must be attackable,
visible, in multi-combat, and in line of sight. Single-combat and player targets
receive one hit. Successful hits drain the highest offensive stats by 5% of their
current levels, once per target per special; melee drains both Attack and Strength,
with ties preferring melee, then Ranged, then Magic. Energy, area restrictions,
and stat drain follow the [Wiki special attack rules](https://osrsindex.com/wiki/special-attacks?site=osrs_wiki)
and [stat drain rules](https://osrsindex.com/wiki/temporary-skill-drain?site=osrs_wiki),
with the requested accuracy bonus and primary-plus-sweep behavior.

Normal toxic blowpipe attacks, including the blazing cosmetic, require stored
Zulrah's scales and unpoisoned darts. Use either material on the blowpipe to load
up to 16,383 of each; unload its darts before changing dart type. Check reports
the contents, and Unload lets you recover just the darts or all darts and scales.
The blowpipe adds +20 Ranged strength to its loaded dart's strength and uses +30
Ranged accuracy, ignoring quiver ammunition. Rapid attacks take two ticks;
Accurate and Longrange take three. Range is five tiles, or seven on Longrange.
The dart sound effect plays at the target when the projectile lands.
Shots consume a dart unless Ava's device saves it, and consume a scale two-thirds
of the time, including on misses. Running out of one resource preserves the other.
These charging and normal attack mechanics follow the
[OSRS Wiki](https://oldschool.runescape.wiki/w/Toxic_blowpipe).

Black chinchompas attack from nine tiles, or ten on Longrange. Medium fuse
(Rapid) takes three ticks; Short and Long fuse take four. In multi-combat,
each throw selects additional targets in the 3x3 square around the primary
target's southwest tile, including NPCs stacked on that tile. Only southwest
tiles count, and splash requires the engine's line of sight from the primary.
Targets are fixed at launch. One accuracy roll against the primary controls
the whole throw: a miss deals zero to everyone; a success rolls each target's
damage independently. Each throw consumes one chinchompa, including misses,
without Ava's recovery or a ground drop. Quiver bonuses do not apply.
One explosion sound plays at the primary target's tile when the projectile lands,
including on misses.
Restart RSMod to load these changes.

For an existing workspace converting from the earlier downloaded checkout,
register the prepared submodule from a terminal with write access to `.git`:

```bash
git submodule add --name rsmod https://github.com/Totomosic/rsmod upstream/rsmod
```

This reuses the existing server checkout, including its game data and local fix.
Publish the fix commit to your fork before others clone the workspace.

## In-game keybinds

The in-game settings can assign F1-F12 or Escape to the side panels. Choosing
None removes a binding. Assigning a key to a different panel clears its previous
assignment, and Restore default keybinds restores the starter's F1-F12 layout.
Bindings are saved to the account and synchronized again at login. New accounts
receive defaults; saved bindings, including deliberately unbound panels, are
preserved.

The revision-233 dropdown only changes its displayed text and does not submit
the selected binding. The custom client's hidden **Local Server Settings** plugin
captures the settings script's arguments and submits them through the existing
`ClientCheat` packet (`localscape_keybind` or `localscape_keybind_reset`). RSMod
accepts only the supported setting IDs and keys while the settings overlay is
open, updates the persistent keybinding varps, and sends their values back to the
client. The native client handles the actual F-key presses.

The **Modern Layout – Side panel can be closed by the hotkeys** checkbox is
also saved to the account. Disabling it keeps the current panel open when its
hotkey is pressed again; enabling it allows the panel to close. The custom
client submits the native toggle's resulting value with
`localscape_sidepanel_hotkeys`, and the server preserves it across keybind
changes, restoring default keybinds, and logins.

Changes to this feature require both the custom client and the server:

```bash
./dev build-client
# Stop and restart RSMod with ./dev server, then launch a new RSProx session.
```

Client regression tests live under `client/src/test/java/` and are copied into
the RuneLite checkout alongside the production overlay. Server regression tests
live in RSMod's settings module under `src/integration/kotlin/`.

## Local Development sidebar

Open the terminal icon in the RuneLite sidebar while logged into LocalScape:

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

Rebuild with `./dev build-client`, restart RSMod, and launch a new client session
to load spellbook controls and rune pouch support. No cache repack is required.

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
the same instance and does not start the Nylocas wave. Bloat combat is not
implemented.

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
The room cap starts at 12 and increases to 24 after wave 20 spawns. A due
wave stalls when the existing count is at or above the cap and retries four
ticks later. Dying bodies count until removal; an admitted wave spawns in full.
Three age-26 flicker styles remain unobserved in the research and retain their last confirmed style.
Big Nylocas use their southwest tile for terrain and NPC occupancy checks, while
retaining their 2×2 footprint for pillar contact and combat reach. Their other
three tiles can overlap smalls or other bigs. Movement and removal release the
southwest reservation without clearing neighbouring Nylocas' flags.
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

## Change the custom client

```bash
# Edit client/src/main/java/... or the upstream RuneLite checkout, then:
./dev build-client
```

Close the existing game client and launch a new RSProx session. The manifest
includes the custom JAR's hash in its filename so the launcher picks up each
rebuilt version. RSProx patches this thin client, its matching injected client,
and the RuneLite API at launch. Other dependencies remain from the pinned release,
including the injected client and runtime API.

This overlay changes client/plugin code only. Changes to RuneLite's API, injected
client, or wire protocol require updating matching artifacts and the server
together; they are outside the starter's packaging command.

`runelite_bootstrap_url` points to the local manifest. RSProx's launcher requests
`bootstrap.json.sha256` for custom targets but skips the official RSA manifest
signature check for them; artifact hashes are still verified. The scaffold
supplies that endpoint with a SHA-256 digest, not an official RuneLite signature.

## Files and commands

| Path | Purpose |
| --- | --- |
| `project.json` | Revision, upstream refs, world and ports |
| `client/` | Custom Java sources copied into RuneLite |
| `tools/dev.py` | Setup, build, configuration, launch commands |
| `tools/rsprox.init.gradle` | RSProx runtime isolation and Java selection |
| `.gitmodules` | RSMod fork URL and submodule path |
| `upstream/rsmod/` | Editable RSMod fork, tracked as a submodule |
| `upstream/runelite/`, `upstream/rsprox/` | Downloaded checkouts, ignored by the workspace |
| `sources.lock.json` | Resolved RuneLite and RSProx commits; generated during setup |
| `.state/` | Tools, downloads, runtime settings and HTTP artifacts |

```bash
./dev install-server  # Prepare RSMod if cache/key files are missing
./dev configure       # Regenerate local config after a world/HTTP-port change
./dev test            # Offline scaffold tests; no game or Java dependencies
```

After changing the HTTP port, rebuild the client manifest as well. `configure`
does not update previously packaged manifest URLs.

## Validation status

Offline tests cover world-list encoding, launch-config rewriting, public-key
parsing, source locking, submodule initialization, preservation of local server
commits and edits, and custom-JAR packaging. The custom client builds with the
cached Maven dependencies, and its keybind bridge tests pass through JUnit.
The server settings scripts were compiled with the cached Kotlin compiler and
verified through the actual packet handlers. Keybind verification also covers
SQLite account save/load, login synchronization, and the cache's native F-key
slot-selection script. All settings integration tests now pass through Gradle,
including the modern-layout hotkey preference, and its client bridge tests pass
through JUnit. The cache's native redraw script was also checked to confirm that
the disabled checkbox installs hotkeys that keep the panel open. Full server
checks and a live GUI session have not been verified here.

## Upstream references

- [RSMod setup](https://github.com/rsmod/rsmod#installation)
- [RSMod revision/cache](https://github.com/rsmod/rsmod/blob/main/api/core/src/main/kotlin/org/rsmod/api/core/Build.kt)
- [RuneLite 1.11.19 Maven build](https://github.com/runelite/runelite/blob/runelite-parent-1.11.19/runelite-client/pom.xml)
- [RuneLite release/revision archive](https://archive.rsprox.net/runelite/index.json)
- [Revision-233 cache](https://archive.openrs2.org/caches/runescape/2293)
- [RSProx custom targets](https://github.com/blurite/rsprox#private-server-usage-osrs)
- [RSProx launcher patching](https://github.com/blurite/rsprox-runelite-launcher/blob/master/src/main/java/net/runelite/launcher/Launcher.java)

Upstream checkouts retain their own licenses. No game assets are bundled;
RSMod's installer downloads the cache specified by its source.
