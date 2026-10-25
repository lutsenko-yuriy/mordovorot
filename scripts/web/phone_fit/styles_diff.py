"""GH-156: the pure parts of styles_diff.sh: seeding Math.random and comparing computed-style dumps.

Usage: styles_diff.py seed <variant dir>       puts SEED_SCRIPT into <variant dir>/index.html
       styles_diff.py compare <results file>  prints the differences; exit 0 with none, 1 with any
"""
import difflib
import json
import re
import sys
from collections import Counter
from pathlib import Path

# The same scramble, and so the same solution screen, in every bundle (GH-153: tiles moved 5-11px between
# two unseeded master runs). An LCG, seed 12345.
SEED_SCRIPT = ("<script>/* GH-156 */(() => { let s = 12345; Math.random = () => "
               "(s = (Math.imul(s, 1103515245) + 12345) >>> 0) / 4294967296; })();</script>")
# Each branch variant against its baseline, never across pointer types.
PAIRS = (("mouse", "master"), ("touch", "master-touch"))
# The game is laid out horizontally, so a logical size only repeats its physical twin.
LOGICAL_SIZES = {"inline-size": "width", "block-size": "height", "min-inline-size": "min-width",
                 "max-inline-size": "max-width", "min-block-size": "min-height", "max-block-size": "max-height"}
SIDES = {"": "", "- ": " (only on the baseline)", "+ ": " (only on the branch)"}


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
    changed = {p: (old.get(p, "(missing)"), new.get(p, "(missing)")) for p in old.keys() | new.keys()
               if old.get(p) != new.get(p)}
    for logical, physical in LOGICAL_SIZES.items():
        if logical in changed and changed[logical] == changed.get(physical):
            del changed[logical]
    return [f"    {p}: {a} -> {b}" for p, (a, b) in sorted(changed.items())]


def _numbered(elements):
    """Each element's place among those sharing its selector, from 1, or None when its selector is unique."""
    totals, seen, numbers = Counter(sel for sel, _ in elements), Counter(), []
    for sel, _ in elements:
        seen[sel] += 1
        numbers.append(seen[sel] if totals[sel] > 1 else None)
    return numbers


def _fold(entries):
    """Report lines, with a run of numbered elements that changed the same way as one entry: DIV.step[1-12]."""
    lines, i = [], 0
    while i < len(entries):
        mark, sel, first, changed = entries[i]
        last, i = first, i + 1
        while (first is not None and i < len(entries) and entries[i][:2] == (mark, sel)
               and entries[i][2] == last + 1 and entries[i][3] == changed):
            last, i = entries[i][2], i + 1
        place = "" if first is None else f"[{first}]" if first == last else f"[{first}-{last}]"
        lines += [f"  {mark}{sel}{place}{SIDES[mark]}"] + changed
    return lines


def diff_case(old, new):
    """(report lines, number of differing elements). Elements are lined up by selector in document order, so
    one added or removed element doesn't shift every element after it; properties compare as maps, since
    Chrome lists them in a different order when the sheets differ (GH-153)."""
    old_numbers, new_numbers = _numbered(old), _numbered(new)
    entries = []  # (mark, selector, number, changed property lines)
    matcher = difflib.SequenceMatcher(None, [s for s, _ in old], [s for s, _ in new], autojunk=False)
    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag == "equal":
            for i, j in zip(range(i1, i2), range(j1, j2)):
                changed = _changed(old[i][1], new[j][1])
                if changed:
                    entries.append(("", new[j][0], new_numbers[j], changed))
            continue
        entries += [("- ", old[i][0], old_numbers[i], []) for i in range(i1, i2)]
        entries += [("+ ", new[j][0], new_numbers[j], []) for j in range(j1, j2)]
    return _fold(entries), len(entries)


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
            changes, n = diff_case(old, new)
            if n:
                lines += [head] + changes
                count += n
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
