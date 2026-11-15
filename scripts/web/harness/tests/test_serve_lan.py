"""serve_lan.sh (GH-160, GH-283): rebuilds the bundle when it wasn't built from the current commit, and
while serving follows each new commit.

Run: python -m unittest discover -s scripts/web/harness/tests -t scripts/web/harness
"""

import os
import shutil
import signal
import socket
import subprocess
import tempfile
import time
import unittest
import urllib.request
from pathlib import Path

SCRIPT = Path(__file__).resolve().parent.parent / "serve_lan.sh"

# Stands in for gradlew: logs each call and writes a bundle that names its commit; a FAIL file fails the build.
FAKE_GRADLEW = """#!/bin/bash
echo "$@" >>"$(dirname "$0")/gradlew.log"
[[ ! -f "$(dirname "$0")/FAIL" ]] || exit 1
mkdir -p web/build/dist/js/productionExecutable
git rev-parse HEAD >web/build/dist/js/productionExecutable/index.html
"""


class ServeLanCase(unittest.TestCase):
    """A throwaway repo with the script, a fake gradlew and one commit."""

    def setUp(self):
        self.repo = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.repo)
        harness = self.repo / "scripts/web/harness"
        harness.mkdir(parents=True)
        shutil.copy(SCRIPT, harness / "serve_lan.sh")
        gradlew = self.repo / "gradlew"
        gradlew.write_text(FAKE_GRADLEW)
        gradlew.chmod(0o755)
        (self.repo / ".gitignore").write_text("web/build/\ngradlew.log\nFAIL\n")
        self.git("init", "-q")
        self.commit("first")

    def git(self, *args):
        subprocess.run(
            ["git", "-c", "user.name=t", "-c", "user.email=t@t", *args],
            cwd=self.repo, check=True, capture_output=True,
        )

    def commit(self, message):
        self.git("add", "-A")
        self.git("commit", "-q", "--allow-empty", "-m", message)

    def builds(self):
        log = self.repo / "gradlew.log"
        return len(log.read_text().splitlines()) if log.exists() else 0

    def head(self):
        return subprocess.run(["git", "rev-parse", "HEAD"], cwd=self.repo, capture_output=True, text=True).stdout.strip()


class ServeLanTest(ServeLanCase):

    def serve(self):
        result = subprocess.run(
            ["bash", str(self.repo / "scripts/web/harness/serve_lan.sh")],
            env={**os.environ, "SERVE_LAN_DRY_RUN": "1"}, capture_output=True, text=True,
        )
        self.assertEqual(0, result.returncode, result.stderr)

    def test_no_bundle_builds_one(self):
        self.serve()
        self.assertEqual(1, self.builds())

    def test_a_bundle_from_this_commit_is_served_as_is(self):
        self.serve()
        self.serve()
        self.assertEqual(1, self.builds())

    def test_a_new_commit_rebuilds(self):
        self.serve()
        self.commit("second")
        self.serve()
        self.assertEqual(2, self.builds())

    def test_uncommitted_changes_always_rebuild(self):
        self.serve()
        (self.repo / "change.txt").write_text("edit")
        self.serve()
        self.serve()
        self.assertEqual(3, self.builds())


    def test_the_served_copy_is_this_commits_and_older_ones_go(self):
        self.serve()
        self.commit("second")
        self.serve()
        serve = self.repo / "web/build/serve-lan"
        self.assertEqual(self.head(), (serve / "current/index.html").read_text().strip())
        self.assertEqual(2, len(list(serve.iterdir())))  # current and its copy

    def test_a_failed_first_build_exits_2(self):
        (self.repo / "FAIL").write_text("")
        result = subprocess.run(
            ["bash", str(self.repo / "scripts/web/harness/serve_lan.sh")],
            env={**os.environ, "SERVE_LAN_DRY_RUN": "1"}, capture_output=True, text=True,
        )
        self.assertEqual(2, result.returncode)


class ServeLanFollowTest(ServeLanCase):
    """The server running: each new commit is built and served without a restart (GH-283)."""

    def setUp(self):
        super().setUp()
        with socket.socket() as probe:
            probe.bind(("127.0.0.1", 0))
            self.port = probe.getsockname()[1]
        self.log = open(self.repo / "serve.log", "w")
        self.addCleanup(self.log.close)
        self.process = subprocess.Popen(
            ["bash", str(self.repo / "scripts/web/harness/serve_lan.sh"), str(self.port)],
            env={**os.environ, "SERVE_LAN_BIND": "127.0.0.1", "SERVE_LAN_POLL_S": "0.2"},
            stdout=self.log, stderr=subprocess.STDOUT,
        )
        self.addCleanup(self.stop)
        self.wait_for(self.head())

    def stop(self):
        if self.process.poll() is None:
            self.process.send_signal(signal.SIGINT)
            self.process.wait(timeout=10)

    def page(self):
        try:
            with urllib.request.urlopen(f"http://127.0.0.1:{self.port}/", timeout=2) as response:
                return response.read().decode().strip()
        except OSError:
            return None

    def wait_for(self, expected, seconds=15):
        deadline = time.time() + seconds
        while time.time() < deadline:
            if self.page() == expected:
                return
            time.sleep(0.1)
        self.fail(f"served {self.page()!r}, expected {expected!r}\n{(self.repo / 'serve.log').read_text()}")

    def test_a_new_commit_is_served_without_a_restart(self):
        self.commit("second")
        self.wait_for(self.head())
        self.assertEqual(2, self.builds())

    def test_a_failed_build_keeps_the_old_page_and_the_next_commit_recovers(self):
        old = self.head()
        (self.repo / "FAIL").write_text("")
        self.commit("broken")
        time.sleep(1.5)
        self.assertEqual(old, self.page())
        self.assertEqual(2, self.builds())  # tried once, not on every poll
        self.assertIn(f"still serving {old}", (self.repo / "serve.log").read_text())
        (self.repo / "FAIL").unlink()
        self.commit("fixed")
        self.wait_for(self.head())

    def test_pages_are_not_cached(self):
        with urllib.request.urlopen(f"http://127.0.0.1:{self.port}/", timeout=2) as response:
            self.assertEqual("no-cache", response.headers["Cache-Control"])

    def test_ctrl_c_stops_the_server(self):
        self.process.send_signal(signal.SIGINT)
        self.process.wait(timeout=10)
        self.assertIsNone(self.page())


if __name__ == "__main__":
    unittest.main()
