# Hosting LocalScape on a VPS

The VPS runs RSMod and the HTTP service. Each player runs RSProx and the custom
RuneLite client on their own desktop. The build/setup scripts support Linux,
including WSL. The standalone player JAR runs with Java 21 or newer on Windows
and Linux, without a project checkout or WSL. The VPS needs no desktop or GPU.

## Prepare the VPS

Use a Linux system with Python 3.12 or newer, Git, and outbound internet access
for setup and build dependencies. `./dev setup` installs missing JDK 21, JDK 11,
and Maven in `.state/tools/`. Use a regular account named `localscape` and put the
workspace at `/opt/localscape` if using the supplied systemd units. That account
must own the workspace and have a writable home directory. An administrator can
create the account and directory with:

```bash
sudo useradd --create-home --shell /bin/bash localscape
sudo install -d -o localscape -g localscape /opt/localscape
```

As that account, clone this workspace, including its RSMod submodule:

```bash
git clone --recurse-submodules https://github.com/Totomosic/localscape /opt/localscape
cd /opt/localscape
```

Use a workspace revision containing the hosting commands documented here. A
clone includes published commits only: copy any unpublished workspace/server
changes you intend to host before building. Setup preserves an initialized
RSMod checkout and its local changes.

## Move the existing world

To keep existing accounts, gear setups, keys, and the exact packed cache, stop
the old RSMod process cleanly, then transfer its entire `.data` directory over
SSH. Run this from the existing workspace, using an SSH account that can write
to the destination:

```bash
rsync -a upstream/rsmod/.data/ USER@YOUR_SERVER_IP:/opt/localscape/upstream/rsmod/.data/
```

Keep the old process stopped during the copy so the SQLite database is
consistent. Keep this data outside the HTTP directory; it includes the private
RSA key and player accounts. Do not send `.data` to players.

For a fresh world, skip the copy; setup generates the cache and keys. Moving
`.state` is unnecessary: the VPS can download tools and rebuild the client.

## Configure the public address

Edit these fields in `/opt/localscape/project.json`. Replace `203.0.113.10` with
the VPS's public IPv4 address, or a DNS hostname pointing to it:

```json
{
  "server_host": "203.0.113.10",
  "game_port": 43594,
  "http_bind": "0.0.0.0",
  "http_port": 8080,
  "http_base_url": null
}
```

Merge these fields into the existing file, keeping its other settings.

| Setting | Meaning |
| --- | --- |
| `server_host` | Game server's public IPv4 address or DNS hostname; no scheme or port. Used by the world list and launch configuration. |
| `game_port` | Game/cache TCP port advertised to RSProx. RSMod currently listens on 43594 in `NetworkFactory.kt`; changing this field alone does not change its listener. |
| `http_bind` | HTTP listener address. `0.0.0.0` accepts connections on all IPv4 interfaces. |
| `http_port` | Local HTTP listener port. |
| `http_base_url` | Optional public HTTP(S) base URL. `null` uses `http://<server_host>:<http_port>`. Supports a path prefix for a reverse proxy. |

Install and build on the VPS:

```bash
./dev setup
```

After a later address or port change, run `./dev configure` and restart the HTTP
service. This refreshes the world list, launch configuration, public RSProx
target, existing client artifact URLs, and manifest checksum without rebuilding
the client. Client source changes still require `./dev build-client`.

If you already have an HTTPS reverse proxy, forward its chosen public URL to
`127.0.0.1:8080`, set `http_bind` to `127.0.0.1`, and set `http_base_url` to that
HTTPS URL. The game service still requires direct TCP access to 43594. Setting
an HTTPS URL does not configure TLS on the Python HTTP listener.

## Configure player accounts

The upstream `dev` realm bypasses password verification and grants owner
privileges to newly created accounts. Before allowing outside players,
disable those two behaviors in the server database.

For a fresh world, start `./dev server` once while inbound game access is still
closed, wait for startup to finish, and stop it with Ctrl+C. This creates the
database and realm rows. For a migrated world the database already exists.

With RSMod stopped, run this on the VPS from `/opt/localscape`:

```bash
python3 - <<'PY'
from pathlib import Path
import sqlite3

path = Path("upstream/rsmod/.data/saves/game.db").resolve()
with sqlite3.connect(path.as_uri() + "?mode=rw", uri=True) as database:
    result = database.execute(
        "UPDATE realms SET dev_mode = 0, ignore_passwords = 0 WHERE name = ?",
        ("dev",),
    )
    if result.rowcount != 1:
        raise RuntimeError("Expected the dev realm; check .data/server.toml before continuing")
PY
```

These instructions assume `.data/server.toml` selects `realm = 'dev'`. Adjust
the realm name in the SQL if you use another realm. Registration stays open:
players create an account by entering a new username and password at first
login, then use that same password on later logins. Existing accounts retain
their passwords and moderator levels; disabling dev mode does not demote
accounts created earlier.

## Allow connections and start the services

Allow inbound TCP on **43594** for game/cache traffic and **8080** for the HTTP
service in both the VPS provider's firewall and the operating system's firewall.
Keep SSH access available. If using an HTTPS reverse proxy instead, expose its
HTTPS port and keep the backend HTTP port private. A VPS with a public IP does
not need home-router port forwarding.

To start manually, use separate terminals for `./dev serve` and `./dev server`.
To keep them running after logout and reboot, use the supplied units. They
assume the `localscape` account and `/opt/localscape` path described above:

```bash
sudo install -m 644 deploy/systemd/localscape-http.service /etc/systemd/system/
sudo install -m 644 deploy/systemd/localscape-server.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now localscape-http localscape-server
```

Inspect startup and errors with:

```bash
sudo journalctl -u localscape-server -u localscape-http -f
```

Stop the server before copying saves or repacking the cache:

```bash
sudo systemctl stop localscape-server
```

The supplied server service rebuilds through Gradle on startup, matching
`./dev server`. Complete the initial setup/build interactively so dependency or
compilation failures are resolved before enabling the service.

## Give players a JAR

After configuring the VPS address and completing `./dev setup`, rebuild the
hosted custom client and standalone launcher with:

```bash
./tools/build-client.sh
```

This script works from any directory. It rebuilds RuneLite with the custom
plugins, refreshes the hosted configuration/manifest, and packages the pinned
RSProx runtime into a single file: `.state/dist/LocalScape.jar`. A copy is also
available in `.state/www/LocalScape.jar`, so players can download it from
`http://YOUR_SERVER_IP:8080/LocalScape.jar` when the HTTP service is running.

Give friends that JAR. On Windows they install Java 21 or newer, then double-click
the file. If their file association does not run it, open a terminal in its
folder and use:

```text
java -jar LocalScape.jar
```

They do not need Git, Python, build tools, the project, or server data. On first
game launch, the bundled RSProx downloads the host's custom RuneLite client and
its dependencies. In its window select **LocalScape → RuneLite → Default →
Launch Session**. Enter a new username and password in the game to register.

The launcher normally embeds the public HTTP address from `project.json`. If
only a loopback address is configured when building, it asks for the server URL
on first launch and remembers the answer. To embed a specific address instead:

```bash
./tools/build-client.sh http://YOUR_SERVER_IP:8080
```

That argument selects the launcher's initial address; configure the actual
public game/download addresses in `project.json` as described above. For a
launcher-only rebuild, use `./dev build-launcher [url]`. After changing only
client plugins, existing launchers download the newly hosted client on a new
game session. They do not need a new project copy.

Player settings, caches, recordings, and logs live under `%USERPROFILE%\.localscape`
on Windows or `~/.localscape` on Linux. Startup errors show a dialog; RSProx's
output is written to `launcher.log` in that directory. The JAR requires Java
and internet access; it does not bundle a Java runtime or the game cache. macOS
still needs RSProx's additional loopback configuration and has not been verified
with this launcher.

For troubleshooting or an automated connection check:

```text
java -jar LocalScape.jar --server http://YOUR_SERVER_IP:8080 --check
```

This checks and saves public connection details without opening the GUI. It does
not verify game login or multiplayer connectivity.

## Join from a source checkout

Players need Git, Python 3.12+, an internet connection, and a Linux/WSLg desktop.
They clone the workspace revision containing `join`, then run:

```bash
git clone https://github.com/Totomosic/localscape
cd localscape
./dev join http://YOUR_SERVER_IP:8080
```

If using an HTTPS base URL, give them that URL instead. The command downloads
the server's public target, installs missing tools, checks out the pinned
RSProx source, and launches its GUI. It does not install RSMod, generate server
keys, or build a separate RuneLite client. The launcher downloads the host's
custom client and its matching dependencies.

In RSProx, choose **LocalScape** (or the configured server name), **RuneLite**,
and **Default**, then **Launch Session**. Use a new username and password on the
game login screen. Everyone uses the same hosted world; the Theatre controls
panel provides **Create lobby** and **Join raid** for playing together.

Players rerun `./dev join <url>` on subsequent launches to refresh connection
details, including the public key if it changes. `./dev proxy` is the local
development command and regenerates its target from the local server key.
The standalone JAR above is the distribution intended for friends; this source
workflow is available for development.

## Check access from outside the VPS

From a different machine, open `http://YOUR_SERVER_IP:8080/proxy-targets.json`.
It should contain the configured address, port, revision, and public modulus.
Check that `/jav_config.ws`, `/worldlist.ws`, `/bootstrap.json`, and
`/bootstrap.json.sha256` also load, and that the custom client artifact URL
inside the manifest is reachable. None should advertise localhost for a direct
public deployment.

Then use the player join command and log in. Confirm that two separate accounts
can create and join the same Theatre lobby. If HTTP loads but the game does not
connect, check port 43594 in both firewalls and the server log. If the launcher
tries localhost, run `./dev configure` on the VPS and rerun the player command.

This guide prepares the deployment; external connectivity and multiplayer login
must still be verified against the actual VPS.
