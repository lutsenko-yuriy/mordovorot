"""styles_diff.py (GH-156): seeding Math.random and comparing computed-style dumps.

Run: python -m unittest discover -s scripts/web/phone_fit/tests -t scripts/web/phone_fit
"""

import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

import styles_diff

SCRIPT = Path(styles_diff.__file__)
PAGE = """<!doctype html>
<head>
    <title>Mordovorot</title>
    <link rel="stylesheet" href="styles.css">
    <link rel="stylesheet" href="fit.css">
</head>
<body><script src="web.js"></script></body>
"""


def line(variant, case, elements):
    return json.dumps({"v": variant, "case": case, "elements": elements})


class InjectSeedTest(unittest.TestCase):
    def test_goes_before_the_first_link(self):
        html = styles_diff.inject_seed(PAGE)
        seed = html.index("Math.random")
        self.assertLess(html.index("<title>"), seed)
        self.assertLess(seed, html.index('<link rel="stylesheet" href="styles.css">'))
        self.assertEqual(html.count("Math.random"), 1)

    def test_leaves_the_rest_of_the_page_alone(self):
        html = styles_diff.inject_seed(PAGE)
        self.assertEqual(html.replace(styles_diff.SEED_SCRIPT, ""), PAGE)

    def test_without_a_link_goes_before_the_first_script(self):
        page = '<head></head><body><script src="a.js"></script><script src="b.js"></script></body>'
        html = styles_diff.inject_seed(page)
        self.assertEqual(html, f'<head></head><body>{styles_diff.SEED_SCRIPT}<script src="a.js"></script>'
                               '<script src="b.js"></script></body>')

    def test_fails_with_neither(self):
        with self.assertRaises(ValueError):
            styles_diff.inject_seed("<body></body>")


class LoadCasesTest(unittest.TestCase):
    def test_drops_links_and_comment_lines(self):
        cases = styles_diff.load_cases([
            "# ?v=mouse&report=styles",
            line("mouse", "play 390x844 4x4", [["HTML", {"color": "red"}], ["LINK", {"color": "red"}],
                                               ["DIV.app", {"color": "blue"}]]),
            "#done",
            "",
        ])
        self.assertEqual(cases, {("mouse", "play 390x844 4x4"): [("HTML", {"color": "red"}),
                                                                ("DIV.app", {"color": "blue"})]})

    def test_keeps_variants_apart(self):
        cases = styles_diff.load_cases([line("mouse", "play 390x844 4x4", []),
                                        line("master", "play 390x844 4x4", [["HTML", {}]])])
        self.assertEqual(cases[("mouse", "play 390x844 4x4")], [])
        self.assertEqual(cases[("master", "play 390x844 4x4")], [("HTML", {})])


class DiffCaseTest(unittest.TestCase):
    def test_property_order_does_not_count(self):
        old = [("DIV.app", {"color": "red", "margin-top": "0px"})]
        new = [("DIV.app", {"margin-top": "0px", "color": "red"})]
        self.assertEqual(styles_diff.diff_case(old, new), ([], 0))

    def test_reports_a_changed_value_on_its_element(self):
        old = [("HTML", {"color": "red"}), ("DIV.dialog", {"color": "red", "opacity": "1"})]
        new = [("HTML", {"color": "red"}), ("DIV.dialog", {"color": "blue", "opacity": "1"})]
        self.assertEqual(styles_diff.diff_case(old, new), (["  DIV.dialog", "    color: red -> blue"], 1))

    def test_reports_a_property_only_one_side_has(self):
        old = [("DIV.app", {"color": "red"})]
        new = [("DIV.app", {"color": "red", "--gap": "4px"})]
        self.assertEqual(styles_diff.diff_case(old, new), (["  DIV.app", "    --gap: (missing) -> 4px"], 1))

    def test_reports_added_and_removed_elements(self):
        old = [("HTML", {}), ("SPAN.old", {}), ("DIV.app", {"color": "red"})]
        new = [("HTML", {}), ("DIV.wrap", {}), ("DIV.app", {"color": "red"}), ("P", {})]
        self.assertEqual(styles_diff.diff_case(old, new),
                         (["  - SPAN.old (only on the baseline)", "  + DIV.wrap (only on the branch)",
                           "  + P (only on the branch)"], 3))

    def test_still_compares_the_elements_after_a_structural_change(self):
        old = [("HTML", {}), ("DIV.app", {"color": "red"})]
        new = [("HTML", {}), ("DIV.wrap", {}), ("DIV.app", {"color": "blue"})]
        self.assertEqual(styles_diff.diff_case(old, new),
                         (["  + DIV.wrap (only on the branch)", "  DIV.app", "    color: red -> blue"], 2))

    def test_a_logical_size_repeating_its_physical_twin_is_left_out(self):
        old = [("DIV.app", {"width": "216px", "inline-size": "216px", "block-size": "30px", "height": "30px"})]
        new = [("DIV.app", {"width": "201px", "inline-size": "201px", "block-size": "40px", "height": "30px"})]
        self.assertEqual(styles_diff.diff_case(old, new),
                         (["  DIV.app", "    block-size: 30px -> 40px", "    width: 216px -> 201px"], 1))

    def test_numbers_elements_that_share_a_selector(self):
        old = [("DIV.board", {}), ("DIV.step", {"color": "red"}), ("DIV.step", {"color": "red"})]
        new = [("DIV.board", {"color": "blue"}), ("DIV.step", {"color": "red"}), ("DIV.step", {"color": "blue"})]
        self.assertEqual(styles_diff.diff_case(old, new),
                         (["  DIV.board", "    color: (missing) -> blue", "  DIV.step[2]", "    color: red -> blue"],
                          2))

    def test_folds_a_run_of_the_same_change_into_one_entry(self):
        old = [("DIV.step", {"width": "216px"})] * 3 + [("DIV.step", {"width": "9px"}), ("P", {})]
        new = [("DIV.step", {"width": "201px"})] * 3 + [("DIV.step", {"width": "8px"}), ("P", {})]
        self.assertEqual(styles_diff.diff_case(old, new),
                         (["  DIV.step[1-3]", "    width: 216px -> 201px", "  DIV.step[4]", "    width: 9px -> 8px"], 4))

    def test_folds_a_run_of_added_elements(self):
        old = [("HTML", {})]
        new = [("HTML", {}), ("DIV.step", {}), ("DIV.step", {})]
        self.assertEqual(styles_diff.diff_case(old, new), (["  + DIV.step[1-2] (only on the branch)"], 2))


class CompareTest(unittest.TestCase):
    CASE = "dialog 390x844 4x4"

    def cases(self, **variants):
        return {(v, self.CASE): elements for v, elements in variants.items()}

    def test_pairs_mouse_with_master_and_touch_with_master_touch(self):
        red, blue = [("DIV.app", {"color": "red"})], [("DIV.app", {"color": "blue"})]
        lines, count = styles_diff.compare(self.cases(mouse=red, master=red, touch=blue,
                                                      **{"master-touch": blue}))
        self.assertEqual(count, 0)
        self.assertEqual(lines[-1], "No differences in 2 cases")

    def test_reports_each_case_with_its_differences_and_a_total(self):
        red, blue = [("DIV.app", {"color": "red"})], [("DIV.app", {"color": "blue"})]
        lines, count = styles_diff.compare(self.cases(mouse=blue, master=red, touch=red,
                                                      **{"master-touch": red}))
        self.assertEqual(count, 1)
        self.assertEqual(lines, [f"@@ mouse vs master: {self.CASE}", "  DIV.app", "    color: red -> blue",
                                 "1 difference in 1 of 2 cases"])

    def test_a_case_missing_on_one_side_is_a_difference(self):
        lines, count = styles_diff.compare({("mouse", self.CASE): [], ("master", "play 390x844 4x4"): []})
        self.assertEqual(count, 2)
        self.assertIn(f"@@ mouse vs master: {self.CASE}: only on the branch", lines)
        self.assertIn("@@ mouse vs master: play 390x844 4x4: only on the baseline", lines)


class CliTest(unittest.TestCase):
    def run_compare(self, *lines):
        with tempfile.TemporaryDirectory() as tmp:
            results = Path(tmp) / "results.txt"
            results.write_text("\n".join(lines) + "\n", encoding="utf-8")
            return subprocess.run([sys.executable, str(SCRIPT), "compare", str(results)],
                                  capture_output=True, text=True)

    def test_exit_0_with_no_differences(self):
        done = self.run_compare(line("mouse", "play 390x844 4x4", [["HTML", {"color": "red"}]]),
                                line("master", "play 390x844 4x4", [["HTML", {"color": "red"}]]))
        self.assertEqual(done.returncode, 0, done.stderr)
        self.assertEqual(done.stdout, "No differences in 1 case\n")

    def test_exit_1_with_any(self):
        done = self.run_compare(line("mouse", "play 390x844 4x4", [["HTML", {"color": "red"}]]),
                                line("master", "play 390x844 4x4", [["HTML", {"color": "blue"}]]))
        self.assertEqual(done.returncode, 1, done.stderr)

    def test_seed_rewrites_the_variants_index_html(self):
        with tempfile.TemporaryDirectory() as tmp:
            page = Path(tmp) / "index.html"
            page.write_text(PAGE, encoding="utf-8")
            done = subprocess.run([sys.executable, str(SCRIPT), "seed", tmp], capture_output=True, text=True)
            self.assertEqual(done.returncode, 0, done.stderr)
            self.assertEqual(page.read_text(encoding="utf-8"), styles_diff.inject_seed(PAGE))


if __name__ == "__main__":
    unittest.main()
