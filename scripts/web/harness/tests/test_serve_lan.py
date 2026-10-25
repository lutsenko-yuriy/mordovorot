"""serve_lan.sh (GH-160): rebuilds the bundle when it wasn't built from the current commit.

Run: python -m unittest discover -s scripts/web/harness/tests -t scripts/web/harness
"""

import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parent.parent / "serve_lan.sh"

# Stands in for gradlew: logs each call and writes a bundle.
FAKE_GRADLEW = """#!/bin/bash
echo "$@" >>"$(dirname "$0")/gradlew.log"
mkdir -p web/build/dist/js/productionExecutable
echo page >web/build/dist/js/productionExecutable/index.html
"""


class ServeLanTest(unittest.TestCase):

    def setUp(self):
        self.repo = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.repo)
        harness = self.repo / "scripts/web/harness"
        harness.mkdir(parents=True)
        shutil.copy(SCRIPT, harness / "serve_lan.sh")
        gradlew = self.repo / "gradlew"
        gradlew.write_text(FAKE_GRADLEW)
        gradlew.chmod(0o755)
        (self.repo / ".gitignore").write_text("web/build/\ngradlew.log\n")
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

    def serve(self):
        result = subprocess.run(
            ["bash", str(self.repo / "scripts/web/harness/serve_lan.sh")],
            env={**os.environ, "SERVE_LAN_DRY_RUN": "1"}, capture_output=True, text=True,
        )
        self.assertEqual(0, result.returncode, result.stderr)

    def builds(self):
        log = self.repo / "gradlew.log"
        return len(log.read_text().splitlines()) if log.exists() else 0

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


if __name__ == "__main__":
    unittest.main()
