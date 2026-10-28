"""GH-168: --text-size sets the throwaway profile's default font size, as a percent of 16px.

Run: python -m unittest discover -s scripts/web/harness/tests -t scripts/web/harness
"""

import json
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

HARNESS = Path(__file__).resolve().parent.parent


class TextSizePrefsTest(unittest.TestCase):

    def setUp(self):
        self.profile = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.profile)

    def prefs(self, percent):
        return subprocess.run(
            ["bash", "-c", 'source "$1"; text_size_prefs "$2" "$3"', "-",
             str(HARNESS / "lib.sh"), percent, str(self.profile)],
            capture_output=True, text=True,
        )

    def font_size(self):
        prefs = json.loads((self.profile / "Default/Preferences").read_text())
        return prefs["webkit"]["webprefs"]["default_font_size"]

    def test_a_percent_sets_the_default_font_size(self):
        self.assertEqual(0, self.prefs("150").returncode)
        self.assertEqual(24, self.font_size())

    def test_200_is_32px(self):
        self.prefs("200")
        self.assertEqual(32, self.font_size())

    def test_smaller_than_standard_is_allowed(self):
        self.prefs("75")
        self.assertEqual(12, self.font_size())

    def test_rounds_to_the_nearest_px(self):
        self.prefs("110")
        self.assertEqual(18, self.font_size())

    def test_100_leaves_the_profile_untouched(self):
        self.assertEqual(0, self.prefs("100").returncode)
        self.assertFalse((self.profile / "Default/Preferences").exists())

    def test_anything_but_a_whole_percent_is_refused(self):
        for bad in ["", "abc", "0", "-50", "1.5", "150%"]:
            with self.subTest(bad=bad):
                self.assertEqual(1, self.prefs(bad).returncode)

    def test_outside_50_to_400_is_refused(self):
        for bad in ["1", "49", "401", "99999999999999999999"]:
            with self.subTest(bad=bad):
                self.assertEqual(1, self.prefs(bad).returncode)


class TextSizeFlagTest(unittest.TestCase):

    def run_script(self, name, *args):
        return subprocess.run(["bash", str(HARNESS / name), *args], capture_output=True, text=True)

    def test_measure_refuses_a_bad_text_size(self):
        result = self.run_script("measure.sh", "--text-size=big", "v=mouse")
        self.assertEqual(2, result.returncode)
        self.assertIn("--text-size", result.stderr)

    def test_styles_diff_refuses_a_bad_text_size(self):
        result = self.run_script("styles_diff.sh", "--text-size=0", "/nowhere")
        self.assertEqual(2, result.returncode)
        self.assertIn("--text-size", result.stderr)


if __name__ == "__main__":
    unittest.main()
