"""GH-156: the pure parts of styles_diff.sh: seeding Math.random and comparing computed-style dumps.

Usage: styles_diff.py seed <variant dir>       puts SEED_SCRIPT into <variant dir>/index.html
       styles_diff.py compare <results file>  prints the differences; exit 0 with none, 1 with any
"""
import difflib
import json
import re
import sys
from pathlib import Path

# The same scramble, and so the same solution screen, in every bundle (GH-153: tiles moved 5-11px between
# two unseeded master runs). An LCG, seed 12345.
SEED_SCRIPT = ("<script>/* GH-156 */(() => { let s = 12345; Math.random = () => "
               "(s = (Math.imul(s, 1103515245) + 12345) >>> 0) / 4294967296; })();</script>")
# Each branch variant against its baseline, never across pointer types.
PAIRS = (("mouse", "master"), ("touch", "master-touch"))


def inject_seed(html):
    """Puts SEED_SCRIPT before the first <link>, or the first <script> without one, so it runs first."""
    first = re.search(r"<link\b", html) or re.search(r"<script\b", html)
    if not first:
        raise ValueError("no <link> or <script> to put the seed before")
    return html[:first.start()] + SEED_SCRIPT + html[first.start():]


def load_cases(lines):
    """{(variant, case): [(selector, {property: value})]} from harness.html's report=styles lines.

    LINK elements are dropped: a branch may add or remove stylesheets."""
    cases = {}
    for text in lines:
        if not text.strip() or text.startswith("#"):
            continue
        dump = json.loads(text)
        cases[(dump["v"], dump["case"])] = [(sel, props) for sel, props in dump["elements"]
                                           if sel.split(".")[0] != "LINK"]
    return cases


def _changed(old, new):
    return [f"    {p}: {old.get(p, '(missing)')} -> {new.get(p, '(missing)')}"
            for p in sorted(old.keys() | new.keys()) if old.get(p) != new.get(p)]


def diff_case(old, new):
    """One group of report lines per differing element. Elements are lined up by selector in document order,
    so one added or removed element doesn't shift every element after it; properties compare as maps, since
    Chrome lists them in a different order when the sheets differ (GH-153)."""
    groups = []
    matcher = difflib.SequenceMatcher(None, [s for s, _ in old], [s for s, _ in new], autojunk=False)
    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag == "equal":
            for (sel, a), (_, b) in zip(old[i1:i2], new[j1:j2]):
                changed = _changed(a, b)
                if changed:
                    groups.append([f"  {sel}"] + changed)
            continue
        groups += [[f"  - {sel} (only on the baseline)"] for sel, _ in old[i1:i2]]
        groups += [[f"  + {sel} (only on the branch)"] for sel, _ in new[j1:j2]]
    return groups


def _plural(n, word):
    return f"{n} {word}" if n == 1 else f"{n} {word}s"


def compare(cases):
    """(report lines, number of differences): each branch case against its baseline case, then a total."""
    lines, count, differing, total = [], 0, 0, 0
    for branch, baseline in PAIRS:
        names = list(dict.fromkeys(c for v, c in cases if v in (branch, baseline)))
        for name in names:
            total += 1
            head = f"@@ {branch} vs {baseline}: {name}"
            old, new = cases.get((baseline, name)), cases.get((branch, name))
            if old is None or new is None:
                lines.append(f"{head}: only on the {'branch' if old is None else 'baseline'}")
                count += 1
                differing += 1
                continue
            groups = diff_case(old, new)
            if groups:
                lines.append(head)
                for group in groups:
                    lines += group
                count += len(groups)
                differing += 1
    if count:
        lines.append(f"{_plural(count, 'difference')} in {differing} of {_plural(total, 'case')}")
    else:
        lines.append(f"No differences in {_plural(total, 'case')}")
    return lines, count


def main():
    command, path = sys.argv[1], Path(sys.argv[2])
    if command == "seed":
        page = path / "index.html"
        page.write_text(inject_seed(page.read_text(encoding="utf-8")), encoding="utf-8")
    elif command == "compare":
        lines, count = compare(load_cases(path.read_text(encoding="utf-8").splitlines()))
        print("\n".join(lines))
        sys.exit(1 if count else 0)
    else:
        sys.exit(f"Unknown command {command}; see {__file__}")


if __name__ == "__main__":
    main()
