"""Local development commands. Python 3.12+, with no Python dependencies."""

import argparse
import functools
import hashlib
import http.server
import json
import os
from pathlib import Path
import platform
import re
import shutil
import struct
import subprocess
import sys
import tarfile
import tempfile
import urllib.error
import urllib.parse
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT / ".state"
WWW = STATE / "www"
UPSTREAM = ROOT / "upstream"
RUNTIME_HOME = STATE / "home"
SOURCE_LOCK = ROOT / "sources.lock.json"
PLUGIN_CLASS = "net/runelite/client/plugins/localdev/LocalDevPlugin.class"


def config():
    return json.loads((ROOT / "project.json").read_text())


def server_host(cfg):
    host = cfg.get("server_host", "127.0.0.1")
    if not isinstance(host, str) or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9.-]*", host):
        raise ValueError("server_host must be an IPv4 address or DNS hostname, without a scheme or port.")
    return host


def http_url(value):
    if not isinstance(value, str):
        raise ValueError("Use an HTTP(S) URL.")
    parsed = urllib.parse.urlsplit(value)
    if (parsed.scheme not in ("http", "https") or not parsed.hostname
            or parsed.username is not None or parsed.password is not None
            or parsed.query or parsed.fragment or any(c.isspace() for c in value)):
        raise ValueError("Use an HTTP(S) URL without credentials, a query, or a fragment.")
    # Accessing port also rejects malformed port numbers.
    parsed.port
    return value.rstrip("/")


def public_http_url(cfg):
    return http_url(cfg.get("http_base_url") or f'http://{server_host(cfg)}:{cfg["http_port"]}')


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def run(args, cwd=ROOT, env=None, capture=False):
    if not capture:
        print("Running:", " ".join(map(str, args)), flush=True)
    result = subprocess.run(
        list(map(str, args)), cwd=cwd, env=env, check=True,
        text=True, stdout=subprocess.PIPE if capture else None,
    )
    return result.stdout.strip() if capture else None


def fetch(url):
    request = urllib.request.Request(url, headers={"User-Agent": "LocalScape-dev/1"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read()


def download(url, destination, digest, algorithm="sha256"):
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_suffix(destination.suffix + ".part")
    checksum = hashlib.new(algorithm)
    try:
        request = urllib.request.Request(url, headers={"User-Agent": "LocalScape-dev/1"})
        with urllib.request.urlopen(request, timeout=60) as response, temporary.open("wb") as out:
            while chunk := response.read(1024 * 1024):
                checksum.update(chunk)
                out.write(chunk)
        if checksum.hexdigest() != digest:
            raise ValueError(f"Checksum mismatch for {url}")
        temporary.replace(destination)
    finally:
        temporary.unlink(missing_ok=True)


def unpack(archive, destination):
    # Extraction filters reject paths outside the destination and unsafe links.
    with tempfile.TemporaryDirectory(dir=archive.parent) as temporary:
        with tarfile.open(archive) as source:
            source.extractall(temporary, filter="data")
        entries = list(Path(temporary).iterdir())
        if len(entries) != 1 or not entries[0].is_dir():
            raise ValueError(f"Expected one root directory in {archive}")
        shutil.move(str(entries[0]), destination)


def java_home(major):
    override = os.environ.get(f"JAVA{major}_HOME")
    candidates = [Path(override)] if override else []
    candidates += [STATE / "tools" / f"jdk-{major}"]
    if os.environ.get("JAVA_HOME"):
        candidates.append(Path(os.environ["JAVA_HOME"]))
    candidates += list(Path("/usr/lib/jvm").glob("*"))
    for candidate in candidates:
        release = candidate / "release"
        if release.is_file() and (candidate / "bin/javac").is_file():
            version = re.search(r'JAVA_VERSION="(\d+)', release.read_text())
            if version and int(version[1]) == major:
                return candidate.resolve()
    raise ValueError(f"JDK {major} missing. Run ./dev tools or set JAVA{major}_HOME.")


def java_env():
    home21, home11 = java_home(21), java_home(11)
    env = dict(os.environ)
    env["JAVA_HOME"] = str(home21)
    env["PATH"] = str(home21 / "bin") + os.pathsep + env.get("PATH", "")
    env["GRADLE_USER_HOME"] = str(STATE / "gradle")
    env["LOCALSCAPE_RUNTIME_HOME"] = str(RUNTIME_HOME)
    # Used by Gradle toolchain discovery; also pass paths explicitly below.
    env["JAVA11_HOME"] = str(home11)
    return env


def install_tools():
    if platform.system() != "Linux":
        raise ValueError("Automatic tool installation currently supports Linux.")
    architecture = {"x86_64": "x64", "aarch64": "aarch64"}.get(platform.machine())
    if not architecture:
        raise ValueError("Set JAVA21_HOME and JAVA11_HOME for this architecture.")
    tools = STATE / "tools"
    tools.mkdir(parents=True, exist_ok=True)
    lockfile = tools / "downloads.json"
    locked = json.loads(lockfile.read_text()) if lockfile.exists() else {}
    for major in (21, 11):
        try:
            print(f"Using JDK {major}: {java_home(major)}")
            continue
        except ValueError:
            pass
        key = f"jdk-{major}-{architecture}"
        if key not in locked:
            query = urllib.parse.urlencode({"architecture": architecture, "image_type": "jdk", "os": "linux"})
            assets = json.loads(fetch(f"https://api.adoptium.net/v3/assets/latest/{major}/hotspot?{query}"))
            package = assets[0]["binary"]["package"]
            locked[key] = {"url": package["link"], "sha256": package["checksum"]}
            write_json(lockfile, locked)
        artifact = locked[key]
        archive = tools / f"{key}.tar.gz"
        print(f"Downloading JDK {major}", flush=True)
        download(artifact["url"], archive, artifact["sha256"])
        unpack(archive, tools / f"jdk-{major}")
        archive.unlink()
    if not shutil.which("mvn") and not (tools / "maven/bin/mvn").exists():
        url = "https://archive.apache.org/dist/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.tar.gz"
        checksum = fetch(url + ".sha512").decode().split()[0]
        archive = tools / "maven.tar.gz"
        download(url, archive, checksum, "sha512")
        unpack(archive, tools / "maven")
        archive.unlink()
    print("Build tools ready.")


def initialize_server_submodule():
    relative = (UPSTREAM / "rsmod").relative_to(ROOT).as_posix()
    entry = run(["git", "ls-files", "--stage", "--", relative], cwd=ROOT, capture=True)
    if not entry.startswith("160000 "):
        raise ValueError(
            f"RSMod is not registered as a submodule. Run git submodule add --name rsmod "
            f"https://github.com/Totomosic/rsmod {relative} from the workspace root."
        )
    # Keep an initialized working tree, including local edits and newer commits.
    # Only an absent checkout needs to be restored to the parent's pinned commit.
    if not (UPSTREAM / "rsmod/.git").exists():
        run(["git", "submodule", "update", "--init", "--recursive", "--", relative], cwd=ROOT)


def checkout_sources(names=None):
    cfg = config()
    UPSTREAM.mkdir(parents=True, exist_ok=True)
    if names is None:
        initialize_server_submodule()
    locked = json.loads(SOURCE_LOCK.read_text()) if SOURCE_LOCK.exists() else {}
    # RSMod is pinned by the submodule gitlink, rather than this download lock.
    locked.pop("rsmod", None)
    for name, source in cfg["repositories"].items():
        if names is not None and name not in names:
            continue
        destination = UPSTREAM / name
        entry = locked.get(name)
        if entry and (entry["url"] != source["url"] or entry["ref"] != source["ref"]):
            raise ValueError(f"{name} settings differ from sources.lock.json; use a fresh checkout for a version change.")
        if not destination.exists():
            # A staging directory makes failed downloads retryable. Fetch accepts
            # a branch, tag or full commit, unlike git clone --branch.
            with tempfile.TemporaryDirectory(prefix=f".{name}-", dir=UPSTREAM) as temporary:
                staging = Path(temporary) / name
                run(["git", "init", staging])
                run(["git", "remote", "add", "origin", source["url"]], staging)
                ref = entry["commit"] if entry else source["ref"]
                run(["git", "fetch", "--depth=1", "origin", ref], staging)
                run(["git", "checkout", "--detach", "FETCH_HEAD"], staging)
                shutil.move(str(staging), destination)
        origin = run(["git", "remote", "get-url", "origin"], destination, capture=True)
        if origin != source["url"]:
            raise ValueError(f"{name} origin differs from project.json; existing checkout preserved.")
        commit = run(["git", "rev-parse", "HEAD"], destination, capture=True)
        if entry and commit != entry["commit"]:
            raise ValueError(f"{name} checkout differs from its lock. Existing source changes were preserved.")
        locked[name] = dict(source, commit=commit)
        write_json(SOURCE_LOCK, locked)
    if names is None:
        check_revision()


def check_revision():
    source = UPSTREAM / "rsmod/api/core/src/main/kotlin/org/rsmod/api/core/Build.kt"
    if not source.is_file():
        raise ValueError("RSMod checkout missing. Run ./dev setup.")
    match = re.search(r"MAJOR\s*:\s*Int\s*=\s*(\d+)", source.read_text())
    if not match or int(match[1]) != config()["revision"]:
        raise ValueError("RSMod revision differs from project.json. Choose a revision-233 RSMod commit before building.")


def base_bootstrap():
    cfg = config()
    path = STATE / "base-bootstrap.json"
    expected = cfg["bootstrap_sha256"]
    if not path.exists() or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
        download(cfg["bootstrap_url"], path, expected)
    bootstrap = json.loads(path.read_text())
    if bootstrap["version"] != cfg["runelite_version"]:
        raise ValueError("Pinned bootstrap and RuneLite source version differ.")
    return bootstrap


def configure_jav(text, cfg):
    values = {}
    for line in text.splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            if key == "param":
                number, value = value.split("=", 1)
                key = "param=" + number
            if key != "msg":
                values[key] = value
    if values.get("param=25") != str(cfg["revision"]):
        raise ValueError("jav_config template has the wrong revision.")
    for key in ("initial_class", "initial_jar", "codebase"):
        if not values.get(key):
            raise ValueError(f"jav_config template is missing {key}")
    values.update({
        "codebase": f"http://{server_host(cfg)}/",
        "param=12": str(cfg["world"]),
        "param=17": f'{public_http_url(cfg)}/worldlist.ws',
        "param=3": "true",
    })
    return "".join(f"{key}={value}\n" for key, value in values.items())


def encode_worldlist(cfg):
    payload = struct.pack(">HHI", 1, cfg["world"], 1)
    payload += server_host(cfg).encode("ascii") + b"\0" + cfg["name"].encode("cp1252") + b"\0"
    payload += struct.pack(">Bh", 0, 0)
    return struct.pack(">I", len(payload)) + payload


def configure_http():
    cfg = config()
    WWW.mkdir(parents=True, exist_ok=True)
    template = STATE / "jav-config-template.ws"
    if not template.exists():
        template.write_bytes(fetch(cfg["jav_config_template_url"]))
    (WWW / "jav_config.ws").write_text(configure_jav(template.read_text(), cfg))
    (WWW / "worldlist.ws").write_bytes(encode_worldlist(cfg))


def gradle(name, *tasks):
    env = java_env()
    path = UPSTREAM / name
    if not (path / "gradlew").is_file():
        raise ValueError(f"{name} checkout missing. Run ./dev setup.")
    # Toolchain discovery reads a Gradle project property (-P), not a JVM property (-D).
    return run([
        "bash", "gradlew", "--no-daemon", "--console=plain",
        f'-Porg.gradle.java.installations.paths={env["JAVA_HOME"]},{env["JAVA11_HOME"]}',
        *tasks,
    ], path, env)


def install_server():
    check_revision()
    data = UPSTREAM / "rsmod/.data"
    # Avoid rerunning upstream install, which repacks/deletes caches, on every setup.
    if not all((data / relative).exists() for relative in ("cache/vanilla", "cache/game", "game.key", "client.key")):
        gradle("rsmod", "install")


def rsa_modulus(path):
    if not path.exists():
        raise ValueError("RSMod public key missing. Run ./dev install-server.")
    values = dict(line.split(":", 1) for line in path.read_text().splitlines() if ":" in line)
    if values.get("Exponent", "").strip().lower() != "10001":
        raise ValueError("RSProx expects the standard RSA exponent 10001.")
    modulus = values.get("Modulus", "").strip().lower()
    if not re.fullmatch(r"[0-9a-f]{128,}", modulus):
        raise ValueError("Invalid RSMod public modulus.")
    return modulus


def configure_target():
    cfg = config()
    modulus = rsa_modulus(UPSTREAM / "rsmod/.data/client.key")
    target = {
        "name": cfg["name"],
        "jav_config_url": f'{public_http_url(cfg)}/jav_config.ws',
        "revision": str(cfg["revision"]),
        "modulus": modulus,
        "game_server_port": cfg["game_port"],
        "runelite_bootstrap_url": f'{public_http_url(cfg)}/bootstrap.json',
        "binary_folder": cfg["name"],
    }
    # JSON is valid YAML; this avoids a dependency just for configuration generation.
    write_json(RUNTIME_HOME / ".rsprox/proxy-targets.yaml", {"config": [target]})
    # Only public connection details belong in the directory served to players.
    write_json(WWW / "proxy-targets.json", {"config": [target]})
    return target


def sync_client():
    destination_root = UPSTREAM / "runelite/runelite-client/src"
    if not (destination_root / "main/java").is_dir():
        raise ValueError("RuneLite checkout missing. Run ./dev setup.")
    for kind in ("main", "test"):
        sources = ROOT / f"client/src/{kind}/java"
        for source in sources.rglob("*.java"):
            target = destination_root / kind / "java" / source.relative_to(sources)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, target)


def package_client(jar, bootstrap):
    with zipfile.ZipFile(jar) as archive:
        if PLUGIN_CLASS not in archive.namelist():
            raise ValueError("Client JAR does not contain the Local Development plugin.")
        if "client.class" in archive.namelist():
            raise ValueError("Use the thin client JAR, not the shaded JAR.")
    content = jar.read_bytes()
    checksum = hashlib.sha256(content).hexdigest()
    cfg = config()
    name = f'client-{cfg["runelite_version"]}-local-{checksum[:12]}.jar'
    artifact = WWW / "artifacts" / name
    artifact.parent.mkdir(parents=True, exist_ok=True)
    artifact.write_bytes(content)
    # Clone the manifest so callers/tests keep the original dependency list intact.
    result = json.loads(json.dumps(bootstrap))
    clients = [a for a in result["artifacts"] if a["name"] == f'client-{cfg["runelite_version"]}.jar']
    if len(clients) != 1:
        raise ValueError("Expected exactly one client artifact in the pinned bootstrap.")
    clients[0].update({
        "name": name, "hash": checksum, "size": len(content),
    })
    clients[0].pop("diffs", None)
    # RSProx handles client patching; the official launcher's update is unnecessary.
    result["updates"] = []
    write_bootstrap(result)
    print(f"Custom RuneLite packaged: {name}")


def write_bootstrap(bootstrap):
    cfg = config()
    prefix = f'client-{cfg["runelite_version"]}-local-'
    for artifact in bootstrap["artifacts"]:
        if artifact["name"].startswith(prefix):
            artifact["path"] = f'{public_http_url(cfg)}/artifacts/{artifact["name"]}'
    write_json(WWW / "bootstrap.json", bootstrap)
    # The RSProx launcher requests this endpoint even for custom manifests. Its
    # custom-manifest path checks artifact hashes but does not verify this as RSA.
    (WWW / "bootstrap.json.sha256").write_text(hashlib.sha256((WWW / "bootstrap.json").read_bytes()).hexdigest() + "\n")


def configure():
    configure_http()
    configure_target()
    manifest = WWW / "bootstrap.json"
    if manifest.is_file():
        write_bootstrap(json.loads(manifest.read_text()))


def build_client():
    env = java_env()
    sync_client()
    bootstrap = base_bootstrap()
    maven = os.environ.get("MVN") or shutil.which("mvn")
    if not maven:
        maven = STATE / "tools/maven/bin/mvn"
    if not Path(maven).is_file():
        raise ValueError("Maven missing. Run ./dev tools or set MVN.")
    run([
        maven, "-B", f"-Dmaven.repo.local={STATE / 'm2'}", "-DskipTests",
        "-Dshade.skip=true", "-pl", "runelite-client", "-am", "install",
    ], UPSTREAM / "runelite", env)
    jar = UPSTREAM / f'runelite/runelite-client/target/client-{config()["runelite_version"]}.jar'
    package_client(jar, bootstrap)


def build_launcher(base_url=None):
    cfg = config()
    if base_url is None:
        candidate = public_http_url(cfg)
        host = urllib.parse.urlsplit(candidate).hostname
        # A development build asks for the VPS address on first launch.
        base_url = "" if host in ("localhost", "0.0.0.0") or host.startswith("127.") else candidate
    else:
        base_url = http_url(base_url)
    gradle("rsprox", ":gui:proxy-tool:shadowJar")
    proxy_jar = UPSTREAM / "rsprox/gui/proxy-tool/build/libs/rsprox.jar"
    return package_launcher(proxy_jar, base_url)


def package_launcher(proxy_jar, base_url):
    cfg = config()
    destination = STATE / "dist/LocalScape.jar"
    destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=destination.parent) as temporary:
        temporary = Path(temporary)
        classes = temporary / "classes"
        sources = sorted((ROOT / "launcher/src/main/java").rglob("*.java"))
        run([java_home(21) / "bin/javac", "--release", "11", "-proc:none", "-encoding", "UTF-8",
             "-cp", proxy_jar, "-d", classes, *sources])
        staged = temporary / "LocalScape.jar"
        with zipfile.ZipFile(proxy_jar) as upstream, zipfile.ZipFile(staged, "w", zipfile.ZIP_DEFLATED) as output:
            for entry in upstream.infolist():
                name = entry.filename.upper()
                if name == "META-INF/MANIFEST.MF" or (name.startswith("META-INF/") and name.endswith((".SF", ".RSA", ".DSA"))):
                    continue
                output.writestr(entry, upstream.read(entry))
            output.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\r\nMain-Class: net.localscape.launcher.Launcher\r\nMulti-Release: true\r\n\r\n")
            output.writestr("localscape-launcher.json", json.dumps({"server_url": base_url, "revision": str(cfg["revision"])}))
            output.writestr("META-INF/LICENSE-rsprox.txt", (UPSTREAM / "rsprox/LICENSE").read_bytes())
            for path in sorted(classes.rglob("*.class")):
                output.write(path, path.relative_to(classes).as_posix())
        staged.replace(destination)
    WWW.mkdir(parents=True, exist_ok=True)
    shutil.copy2(destination, WWW / destination.name)
    print(f"Share this file with players: {destination}")
    print("Players need Java 21 or newer. Run: java -jar LocalScape.jar")
    if not base_url:
        print("This build asks for the server URL on first launch; no public address is configured yet.")
    return destination


def serve():
    required = ["jav_config.ws", "worldlist.ws", "bootstrap.json", "bootstrap.json.sha256", "proxy-targets.json"]
    if not all((WWW / name).is_file() for name in required):
        raise ValueError("Client/configuration missing. Run ./dev setup, or ./dev configure for an existing build.")
    handler = functools.partial(http.server.SimpleHTTPRequestHandler, directory=str(WWW))
    cfg = config()
    with http.server.ThreadingHTTPServer((cfg.get("http_bind", "127.0.0.1"), cfg["http_port"]), handler) as server:
        print(f"Serving configuration and client at {public_http_url(cfg)} (listening on {server.server_address})", flush=True)
        server.serve_forever()


def doctor():
    issues = []
    for tool in ("git", "python3"):
        found = shutil.which(tool)
        print(f"{tool}: {found or 'MISSING'}")
        if not found:
            issues.append(tool)
    for major in (21, 11):
        try:
            print(f"JDK {major}: {java_home(major)}")
        except ValueError as error:
            print(error)
            issues.append(f"JDK {major}")
    maven = os.environ.get("MVN") or shutil.which("mvn") or STATE / "tools/maven/bin/mvn"
    print(f"Maven: {maven if Path(maven).is_file() else 'MISSING'}")
    if not Path(maven).is_file():
        issues.append("Maven")
    for name in config()["repositories"]:
        exists = (UPSTREAM / name / ".git").exists()
        print(f"{name} source: {'ready' if exists else 'run ./dev setup'}")
    print(f"Runtime home: {RUNTIME_HOME}")
    print(f"Display: {os.environ.get('DISPLAY', 'not set; RSProx/RuneLite need a desktop session')}")
    if issues:
        raise ValueError("Missing tools: " + ", ".join(issues))


def setup():
    checkout_sources()
    install_tools()
    configure_http()
    install_server()
    configure_target()
    build_client()
    print("Ready. Run ./dev serve, ./dev server, and ./dev proxy in three terminals.")


def require_desktop():
    if not os.environ.get("DISPLAY") and not os.environ.get("WAYLAND_DISPLAY"):
        raise ValueError("RSProx needs a desktop session. Run this command from your Linux/WSLg desktop terminal.")


def launch_proxy(target):
    # Fetch configuration before opening the GUI so a missing HTTP server is clear.
    fetch(target["runelite_bootstrap_url"])
    fetch(target["jav_config_url"])
    print(f"Select target {target['name']}, client RuneLite, account Default, then Launch Session.")
    gradle("rsprox", "-I", str(ROOT / "tools/rsprox.init.gradle"), "proxy")


def proxy():
    require_desktop()
    launch_proxy(configure_target())


def join(base_url):
    require_desktop()
    base_url = http_url(base_url)
    document = json.loads(fetch(base_url + "/proxy-targets.json"))
    if not isinstance(document, dict):
        raise ValueError("Expected a server configuration object in proxy-targets.json.")
    targets = document.get("config")
    if not isinstance(targets, list) or len(targets) != 1 or not isinstance(targets[0], dict):
        raise ValueError("Expected one server in proxy-targets.json.")
    target = targets[0]
    if target.get("revision") != str(config()["revision"]):
        raise ValueError("Remote server revision differs from this workspace.")
    for field in ("jav_config_url", "runelite_bootstrap_url"):
        if not isinstance(target.get(field), str):
            raise ValueError(f"Remote server is missing {field}.")
        http_url(target[field])
    if not isinstance(target.get("name"), str) or not target["name"]:
        raise ValueError("Remote server is missing its name.")
    if not isinstance(target.get("modulus"), str) or not re.fullmatch(r"[0-9a-fA-F]{128,}", target["modulus"]):
        raise ValueError("Remote server is missing its public RSA modulus.")
    port = target.get("game_server_port")
    if type(port) is not int or not 1 <= port <= 65535:
        raise ValueError("Remote server has an invalid game port.")
    # Players need RSProx and Java, but no RSMod data, private key, or client build.
    install_tools()
    checkout_sources(names=("rsprox",))
    write_json(RUNTIME_HOME / ".rsprox/proxy-targets.yaml", document)
    launch_proxy(target)


def main():
    parser = argparse.ArgumentParser(description="Custom RuneLite + RSMod + RSProx development")
    parser.add_argument("command", choices=[
        "doctor", "tools", "setup", "install-server", "build-client", "configure",
        "serve", "server", "proxy", "join", "build-launcher", "test",
    ])
    parser.add_argument("url", nargs="?", help="Public HTTP(S) base URL for join or build-launcher")
    args = parser.parse_args()
    if args.command == "join" and args.url is None:
        parser.error("Use ./dev join <server-url>.")
    if args.url is not None and args.command not in ("join", "build-launcher"):
        parser.error("Only join and build-launcher take a server URL.")
    commands = {
        "doctor": doctor, "tools": install_tools, "setup": setup,
        "install-server": install_server, "build-client": build_client,
        "configure": configure,
        "serve": serve, "server": lambda: (check_revision(), gradle("rsmod", "run")),
        "proxy": proxy,
        "join": lambda: join(args.url),
        "build-launcher": lambda: build_launcher(args.url),
        "test": lambda: run([sys.executable, "-m", "unittest", "discover", "-s", "tests", "-v"]),
    }
    try:
        commands[args.command]()
    except (ValueError, OSError, subprocess.CalledProcessError, urllib.error.URLError) as error:
        print(f"Error: {error}", file=sys.stderr)
        sys.exit(1)
    except KeyboardInterrupt:
        print("\nStopped.")


if __name__ == "__main__":
    main()
