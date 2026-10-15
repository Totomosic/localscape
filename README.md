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

Sulphur blades make two independent melee accuracy and damage rolls against
NPCs and players, with both splats landing on the same tick. Their max hits split
the normal melee max hit, rounding the first down and the second up (45 becomes
22 and 23).

Player melee, ranged, and powered-staff damage rolls, including godswords and
Sulphur blades, use a shared helper that rolls from zero through the max hit and
clamps the result to at least one. A rolled zero and a rolled one both deal one
damage after a successful accuracy check.

Scythe of vitur attacks use one splat per NPC tile size, up to three, with each
splat rolling accuracy separately and landing on the same tick. Each successive
max hit is half the previous one, rounded down (51 becomes 51, 25, 12). This also
applies to the holy, sanguine, and blighted cosmetics, including all uncharged
forms. Player targets receive one standard hit.

The Eye of ayak is a powered staff with a three-tick attack cycle and a base max
hit of `floor(current_magic_level / 3) - 6`, before normal magic damage bonuses.
It uses its native casting and projectile effects. It holds up to 50,000 charges:
either two death runes and one chaos rune per charge, or one demon tear. The two
charge types cannot be mixed. Use charging materials on the staff, or use Charge
on the uncharged item; Check reports the remaining charges, and Uncharge refunds
the original materials. Each cast consumes one charge, including splashes, and
the last charge turns it back into the uncharged item. Charging costs follow the
[OSRS rules](https://osrsindex.com/wiki/eye-of-ayak?site=osrs_wiki).

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

- **Restore Skills** restores all skills, including Hitpoints and Prayer, to 99.
- **Clear Inventory** removes all items from your inventory.
- Search for an item by name or ID, select a result, enter a quantity, and click
  **Add Selected Item**. Results show IDs and identify noted items. Search reads
  the local game cache, includes untradeable items, and excludes placeholders.
  Broad searches show the first 100 matches; narrow the query to find more.

The item list loads over several client ticks after login. Controls use RSMod's
existing `master`, `invclear`, and `invadd` admin commands. Your account needs
server admin access; command results and inventory-limit messages appear in game
chat. Restore Skills preserves earned XP.

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
slot-selection script. Full server Gradle checks and a live GUI session have not
been verified here; Gradle's socket initialization and shell access to GitHub
are blocked in the restricted agent environment.

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
