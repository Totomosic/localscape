from contextlib import contextmanager
import http.server
import json
from pathlib import Path
import subprocess
import tempfile
import threading
import unittest
import zipfile

from tools import dev


class PlayerLauncherTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        try:
            cls.java21 = dev.java_home(21)
            cls.java11 = dev.java_home(11)
        except ValueError as error:
            raise unittest.SkipTest(str(error))
        dependencies = dev.UPSTREAM / "rsprox/gui/proxy-tool/build/libs/rsprox.jar"
        if not dependencies.is_file():
            raise unittest.SkipTest("Run ./dev build-launcher once to cache the player dependencies.")
        cls.temporary = tempfile.TemporaryDirectory()
        cls.addClassCleanup(cls.temporary.cleanup)
        root = Path(cls.temporary.name)
        classes = root / "classes"
        subprocess.run([
            str(cls.java21 / "bin/javac"), "--release", "11", "-proc:none", "-encoding", "UTF-8",
            "-cp", str(dependencies), "-d", str(classes),
            str(dev.ROOT / "launcher/src/main/java/net/localscape/launcher/Launcher.java"),
        ], check=True, capture_output=True)
        cls.jar = root / "LocalScape.jar"
        with zipfile.ZipFile(cls.jar, "w") as jar:
            jar.writestr("META-INF/MANIFEST.MF", (
                "Manifest-Version: 1.0\r\nMain-Class: net.localscape.launcher.Launcher\r\n"
                f"Class-Path: {dependencies.as_uri()}\r\n\r\n"
            ))
            jar.writestr("localscape-launcher.json", json.dumps({"server_url": "", "revision": "233"}))
            for path in classes.rglob("*.class"):
                jar.write(path, path.relative_to(classes).as_posix())

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.home = Path(self.temporary.name) / "Player home with spaces"

    def launch(self, *args, java=None):
        return subprocess.run([
            str((java or self.java21) / "bin/java"), "-Djava.awt.headless=true", "-jar", str(self.jar),
            "--check", "--data-dir", str(self.home), *args,
        ], capture_output=True, text=True, timeout=20)

    @contextmanager
    def server(self, revision="233", bootstrap_status=200):
        responses = {}

        class Handler(http.server.BaseHTTPRequestHandler):
            def do_GET(self):
                status, body = responses.get(self.path, (404, b"missing"))
                self.send_response(status)
                self.end_headers()
                self.wfile.write(body)

            def log_message(self, *args):
                pass

        try:
            server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        except PermissionError:
            self.skipTest("Loopback sockets are unavailable in this sandbox.")
        base = f"http://127.0.0.1:{server.server_port}"
        target = {
            "name": "Hosted LocalScape", "revision": revision, "modulus": "ab" * 128,
            "game_server_port": 43594, "jav_config_url": base + "/jav_config.ws",
            "runelite_bootstrap_url": base + "/bootstrap.json", "binary_folder": "../../outside",
        }
        responses.update({
            "/proxy-targets.json": (200, json.dumps({"config": [target]}).encode()),
            "/jav_config.ws": (200, b"param=25=233\n"),
            "/bootstrap.json": (bootstrap_status, b'{"artifacts": []}'),
        })
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            yield base
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=5)

    def test_jar_downloads_public_target_and_remembers_server_without_project(self):
        with self.server() as base:
            result = self.launch("--server", base + "/")
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn("Ready: Hosted LocalScape", result.stdout)
            target = json.loads((self.home / ".rsprox/proxy-targets.yaml").read_text())["config"][0]
            self.assertEqual(target["jav_config_url"], base + "/jav_config.ws")
            self.assertEqual(target["binary_folder"], "LocalScape")
            self.assertEqual(target["game_server_port"], 43594)
            again = self.launch()
            self.assertEqual(again.returncode, 0, again.stderr)

    def test_incompatible_server_preserves_previous_working_target(self):
        target = self.home / ".rsprox/proxy-targets.yaml"
        target.parent.mkdir(parents=True)
        target.write_text("previous working target")
        with self.server(revision="234") as base:
            result = self.launch("--server", base)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("needs server revision 233", result.stderr)
        self.assertEqual(target.read_text(), "previous working target")

    def test_unavailable_client_downloads_preserve_previous_working_target(self):
        target = self.home / ".rsprox/proxy-targets.yaml"
        target.parent.mkdir(parents=True)
        target.write_text("previous working target")
        with self.server(bootstrap_status=404) as base:
            result = self.launch("--server", base)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("HTTP 404", result.stderr)
        self.assertEqual(target.read_text(), "previous working target")

    def test_java11_reports_required_runtime_without_class_version_error(self):
        result = self.launch(java=self.java11)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("needs Java 21 or newer", result.stderr)
        self.assertNotIn("UnsupportedClassVersionError", result.stderr)

    def test_missing_server_value_has_actionable_error(self):
        result = self.launch("--server")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("--server needs a server URL", result.stderr)
