# LocalScape

A Linux development workspace for a custom RuneLite client, an RSMod server,
and RSProx packet inspection. The baseline is OSRS revision 233 and RuneLite
1.11.19.

```text
custom RuneLite → RSProx → RSMod game and cache/update services
local HTTP service → launch configuration, world list, custom client manifest/JAR
```

See [features.md](features.md) for gameplay features, sidebar controls, and
Nylocas encounter details.

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

The HTTP service binds to loopback by default. `server_host`, `http_bind`, and
`http_base_url` in `project.json` configure hosting for other players. See the
[VPS hosting guide](docs/hosting.md) for moving server data, opening ports,
running services after reboot, and distributing a standalone player JAR.

Build the hosted custom client and shareable launcher again with:

```bash
./tools/build-client.sh
```

Give friends `.state/dist/LocalScape.jar`. They need Java 21 or newer and can
double-click it, or run `java -jar LocalScape.jar`; no project checkout, Python,
or WSL is needed for the JAR on Windows. It downloads your hosted custom client.
The launcher uses the public address configured in `project.json`; a development
build with only a loopback address asks for the server URL on first launch.
You can also embed an explicit address with
`./tools/build-client.sh http://YOUR_SERVER_IP:8080`.

Players using a source checkout can still run:

```bash
./dev join http://YOUR_SERVER_IP:8080
```

RSMod uses its upstream network binding defaults; this scaffold does not change
its listener configuration. `game_port` describes the target port, so changing it
also requires changing RSMod's listener configuration.

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

Changes to map-area definitions, NPC or item stat overrides, and cache-backed
animations or projectiles require repacking the cache with the server stopped:

```bash
cd upstream/rsmod
./gradlew packCache
```

Then restart `./dev server` from the workspace root. NPC stat overrides are in
`api/config/src/main/kotlin/org/rsmod/api/config/editors/NpcEdits.kt` inside the
server checkout. Feature-specific rebuild requirements are documented in
[features.md](features.md).

For an existing workspace converting from the earlier downloaded checkout,
register the prepared submodule from a terminal with write access to `.git`:

```bash
git submodule add --name rsmod https://github.com/Totomosic/rsmod upstream/rsmod
```

This reuses the existing server checkout, including its game data and local fix.
Publish the fix commit to your fork before others clone the workspace.

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
| `features.md` | Gameplay features, sidebar controls, and encounter details |
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
./dev configure       # Refresh connection config and existing manifest URLs
./dev join <url>      # Install/launch RSProx against a hosted server (desktop)
./dev build-launcher [url] # Package the standalone player JAR
./tools/build-client.sh [url] # Rebuild the hosted client and player launcher
./dev test            # Offline scaffold tests; no game or Java dependencies
```

After changing the server address or HTTP settings, run `./dev configure` and
restart `./dev serve`. Existing packaged client URLs and the manifest checksum
are refreshed without recompiling Java. Changes to client sources still require
`./dev build-client`. Players rerun `./dev join <url>` to refresh their target.

## Validation status

Client regression tests live under `client/src/test/java/` and are copied into
the RuneLite checkout alongside the production overlay. Server regression tests
live in RSMod's settings module under `src/integration/kotlin/`.

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
