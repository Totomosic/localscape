import hashlib
import json
import os
from pathlib import Path
import shutil
import struct
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import zipfile

from tools import dev


class DevTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.cfg = dev.config()

    def test_worldlist_decodes_as_one_local_member_world(self):
        encoded = dev.encode_worldlist(self.cfg)
        size, count, world, properties = struct.unpack_from(">IHHI", encoded)
        self.assertEqual(size, len(encoded) - 4)
        self.assertEqual((count, world, properties), (1, 301, 1))
        host, activity, tail = encoded[12:].split(b"\0", 2)
        self.assertEqual(host, b"127.0.0.1")
        self.assertEqual(activity, b"LocalScape")
        self.assertEqual(struct.unpack(">Bh", tail), (0, 0))

    def test_configuration_keeps_revision_parameters_and_rewrites_destinations(self):
        template = (
            "codebase=http://oldschool1.runescape.com/\n"
            "initial_class=client.class\ninitial_jar=gamepack.jar\n"
            "param=25=233\nparam=12=302\nparam=17=https://example.com/worldlist.ws\n"
            "param=99=keep=embedded=equals\nmsg=ignore=this\n"
        )
        local = dev.configure_jav(template, self.cfg)
        self.assertIn("codebase=http://127.0.0.1/\n", local)
        self.assertIn("param=12=301\n", local)
        self.assertIn("param=17=http://127.0.0.1:8080/worldlist.ws\n", local)
        self.assertIn("param=99=keep=embedded=equals\n", local)
        self.assertNotIn("msg=", local)
        with self.assertRaisesRegex(ValueError, "wrong revision"):
            dev.configure_jav(template.replace("25=233", "25=234"), self.cfg)

    def test_public_key_parsing_and_target_use_project_runtime_home(self):
        upstream = self.root / "upstream"
        public_key = upstream / "rsmod/.data/client.key"
        public_key.parent.mkdir(parents=True)
        modulus = "ab" * 128
        public_key.write_text(f"Exponent: 10001\nModulus: {modulus}\n")
        with patch.object(dev, "UPSTREAM", upstream), patch.object(dev, "RUNTIME_HOME", self.root / "home"):
            dev.configure_target()
        target = json.loads((self.root / "home/.rsprox/proxy-targets.yaml").read_text())["config"][0]
        self.assertEqual(target["modulus"], modulus)
        self.assertEqual(target["revision"], "233")
        self.assertEqual(target["runelite_bootstrap_url"], "http://127.0.0.1:8080/bootstrap.json")
        public_key.write_text("Exponent: 3\nModulus: " + modulus)
        with self.assertRaisesRegex(ValueError, "exponent"):
            dev.rsa_modulus(public_key)

    def make_client_jar(self, name="client.jar", shaded=False, custom=True):
        jar = self.root / name
        with zipfile.ZipFile(jar, "w") as archive:
            if custom:
                archive.writestr(dev.PLUGIN_CLASS, b"custom class bytes")
            if shaded:
                archive.writestr("client.class", b"game client bytes")
        return jar

    def test_packaging_replaces_only_client_and_hashes_served_bytes(self):
        original = {
            "version": "1.11.19", "updates": [{"url": "unused"}],
            "artifacts": [
                {"name": "client-1.11.19.jar", "path": "upstream", "diffs": [{}]},
                {"name": "injected-client-1.11.19.jar", "hash": "keep-injected"},
                {"name": "runelite-api-1.11.19-runtime.jar", "hash": "keep-api"},
            ],
        }
        jar = self.make_client_jar()
        www = self.root / "www"
        with patch.object(dev, "WWW", www):
            dev.package_client(jar, original)
        packed = json.loads((www / "bootstrap.json").read_text())
        client = packed["artifacts"][0]
        served = (www / "artifacts" / client["name"]).read_bytes()
        self.assertEqual(client["hash"], hashlib.sha256(served).hexdigest())
        self.assertEqual(client["size"], len(served))
        self.assertNotIn("diffs", client)
        self.assertEqual(packed["artifacts"][1:], original["artifacts"][1:])
        self.assertEqual(original["artifacts"][0]["path"], "upstream")
        self.assertEqual(packed["updates"], [])
        self.assertEqual((www / "bootstrap.json.sha256").read_text().strip(), hashlib.sha256((www / "bootstrap.json").read_bytes()).hexdigest())

    def test_packaging_rejects_shaded_or_unmodified_client(self):
        with self.assertRaisesRegex(ValueError, "thin"):
            dev.package_client(self.make_client_jar(shaded=True), {})
        with self.assertRaisesRegex(ValueError, "Local Development"):
            dev.package_client(self.make_client_jar("stock.jar", custom=False), {})

    def test_corrupt_download_preserves_previous_file(self):
        source = self.root / "source"
        source.write_bytes(b"unexpected bytes")
        destination = self.root / "artifact"
        destination.write_bytes(b"previous valid artifact")
        with self.assertRaisesRegex(ValueError, "Checksum mismatch"):
            dev.download(source.as_uri(), destination, "0" * 64)
        self.assertEqual(destination.read_bytes(), b"previous valid artifact")
        self.assertFalse(destination.with_suffix(".part").exists())

    def make_source_workspace(self):
        remote = self.root / "remote"
        subprocess.run(["git", "init", "-b", "main", str(remote)], check=True, capture_output=True)
        source = remote / "api/core/src/main/kotlin/org/rsmod/api/core/Build.kt"
        source.parent.mkdir(parents=True)
        source.write_text("public const val MAJOR: Int = 233\n")
        subprocess.run(["git", "-C", str(remote), "add", "."], check=True)
        subprocess.run([
            "git", "-C", str(remote), "-c", "user.name=Test", "-c", "user.email=test@example.invalid",
            "commit", "-m", "revision 233",
        ], check=True, capture_output=True)
        commit = subprocess.run(["git", "-C", str(remote), "rev-parse", "HEAD"], check=True, capture_output=True, text=True).stdout.strip()
        workspace = self.root / "workspace"
        subprocess.run(["git", "init", str(workspace)], check=True, capture_output=True)
        subprocess.run([
            "git", "-c", "protocol.file.allow=always", "-C", str(workspace),
            "submodule", "add", "--name", "rsmod", remote.as_uri(), "upstream/rsmod",
        ], check=True, capture_output=True)
        return workspace, remote, commit

    def test_source_lock_reuses_commit_and_preserves_custom_edits(self):
        workspace, remote, commit = self.make_source_workspace()
        cfg = dict(self.cfg, repositories={"runelite": {"url": remote.as_uri(), "ref": commit}})
        upstream = workspace / "upstream"
        lockfile = workspace / "sources.lock.json"
        lockfile.write_text(json.dumps({"rsmod": {"commit": commit}}))
        with patch.object(dev, "config", return_value=cfg), patch.multiple(dev, ROOT=workspace, UPSTREAM=upstream, SOURCE_LOCK=lockfile):
            dev.checkout_sources()
            custom = upstream / "rsmod/custom.txt"
            custom.write_text("keep this edit")
            download_edit = upstream / "runelite/custom.txt"
            download_edit.write_text("keep this downloaded checkout edit")
            initial_lock = lockfile.read_text()
            self.assertNotIn("rsmod", json.loads(initial_lock))
            self.assertEqual(json.loads(initial_lock)["runelite"]["commit"], commit)
            dev.checkout_sources()
            self.assertEqual(custom.read_text(), "keep this edit")
            self.assertEqual(download_edit.read_text(), "keep this downloaded checkout edit")
            self.assertEqual(lockfile.read_text(), initial_lock)

    def test_server_submodule_initialization_uses_parent_pin(self):
        workspace, remote, commit = self.make_source_workspace()
        (remote / "new.txt").write_text("new remote commit")
        subprocess.run(["git", "-C", str(remote), "add", "new.txt"], check=True)
        subprocess.run([
            "git", "-C", str(remote), "-c", "user.name=Test", "-c", "user.email=test@example.invalid",
            "commit", "-m", "advance remote branch",
        ], check=True, capture_output=True)
        subprocess.run([
            "git", "-C", str(workspace), "submodule", "deinit", "-f", "--", "upstream/rsmod",
        ], check=True, capture_output=True)
        with patch.multiple(dev, ROOT=workspace, UPSTREAM=workspace / "upstream"), patch.dict(os.environ, {"GIT_ALLOW_PROTOCOL": "file"}):
            dev.initialize_server_submodule()
        actual = subprocess.run([
            "git", "-C", str(workspace / "upstream/rsmod"), "rev-parse", "HEAD",
        ], check=True, capture_output=True, text=True).stdout.strip()
        self.assertEqual(actual, commit)
        self.assertFalse((workspace / "upstream/rsmod/new.txt").exists())

    def test_server_submodule_preserves_local_commits(self):
        workspace, _, initial = self.make_source_workspace()
        server = workspace / "upstream/rsmod"
        (server / "custom.txt").write_text("custom server change")
        subprocess.run(["git", "-C", str(server), "add", "custom.txt"], check=True)
        subprocess.run([
            "git", "-C", str(server), "-c", "user.name=Test", "-c", "user.email=test@example.invalid",
            "commit", "-m", "custom server change",
        ], check=True, capture_output=True)
        expected = subprocess.run([
            "git", "-C", str(server), "rev-parse", "HEAD",
        ], check=True, capture_output=True, text=True).stdout.strip()
        with patch.multiple(dev, ROOT=workspace, UPSTREAM=workspace / "upstream"):
            dev.initialize_server_submodule()
        actual = subprocess.run([
            "git", "-C", str(server), "rev-parse", "HEAD",
        ], check=True, capture_output=True, text=True).stdout.strip()
        self.assertNotEqual(actual, initial)
        self.assertEqual(actual, expected)
        self.assertEqual((server / "custom.txt").read_text(), "custom server change")

    def test_server_submodule_registration_reuses_existing_checkout(self):
        workspace, remote, commit = self.make_source_workspace()
        # Recreate the pre-migration state: an independent repository, a prepared
        # .gitmodules file, and no gitlink in the parent's index.
        subprocess.run([
            "git", "-C", str(workspace), "rm", "--cached", "upstream/rsmod",
        ], check=True, capture_output=True)
        server = workspace / "upstream/rsmod"
        # Move its separate Git directory into the checkout to match our current
        # downloaded checkout; this only touches the temporary test repository.
        gitdir = subprocess.run([
            "git", "-C", str(server), "rev-parse", "--absolute-git-dir",
        ], check=True, capture_output=True, text=True).stdout.strip()
        (server / ".git").unlink()
        shutil.move(gitdir, server / ".git")
        subprocess.run([
            "git", "-C", str(server), "config", "--unset", "core.worktree",
        ], check=True)
        save = server / ".data/saves/game.db"
        save.parent.mkdir(parents=True)
        save.write_bytes(b"preserve local game data")
        subprocess.run([
            "git", "-C", str(workspace), "submodule", "add", "--name", "rsmod",
            remote.as_uri(), "upstream/rsmod",
        ], check=True, capture_output=True)
        entry = subprocess.run([
            "git", "-C", str(workspace), "ls-files", "--stage", "upstream/rsmod",
        ], check=True, capture_output=True, text=True).stdout
        self.assertTrue(entry.startswith(f"160000 {commit} "))
        self.assertEqual(save.read_bytes(), b"preserve local game data")

    def test_server_submodule_requires_registered_gitlink(self):
        workspace = self.root / "workspace"
        subprocess.run(["git", "init", str(workspace)], check=True, capture_output=True)
        with patch.multiple(dev, ROOT=workspace, UPSTREAM=workspace / "upstream"):
            with self.assertRaisesRegex(ValueError, "not registered as a submodule"):
                dev.initialize_server_submodule()


class GradleToolchainTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        try:
            cls.java21 = dev.java_home(21)
            cls.java11 = dev.java_home(11)
        except ValueError as error:
            raise unittest.SkipTest(str(error))
        libraries = list((dev.STATE / "gradle/wrapper/dists/gradle-8.7-bin").glob("*/gradle-8.7/lib/*.jar"))
        if not libraries:
            raise unittest.SkipTest("Download the RSProx Gradle 8.7 wrapper to run this integration test.")
        cls.temporary = tempfile.TemporaryDirectory()
        cls.addClassCleanup(cls.temporary.cleanup)
        cls.classpath = os.pathsep.join([cls.temporary.name, *map(str, libraries)])
        subprocess.run([
            str(cls.java21 / "bin/javac"), "-cp", cls.classpath, "-d", cls.temporary.name,
            str(dev.ROOT / "tests/fixtures/GradleToolchainPaths.java"),
        ], check=True, capture_output=True)

    def test_launcher_paths_are_visible_to_gradle_toolchain_discovery(self):
        # Capture the actual wrapper invocation, then give its property arguments
        # to Gradle's own parser and installation supplier (no socket/daemon).
        with patch.object(dev, "run") as launch:
            dev.gradle("rsprox", "proxy")
        args = launch.call_args.args[0]
        properties = [arg for arg in args if arg.startswith(("-P", "-D"))]
        result = subprocess.run([
            str(self.java21 / "bin/java"), "-cp", self.classpath,
            "GradleToolchainPaths", *properties,
        ], check=True, text=True, capture_output=True)
        self.assertEqual(set(result.stdout.splitlines()), {str(self.java21), str(self.java11)})


if __name__ == "__main__":
    unittest.main()
