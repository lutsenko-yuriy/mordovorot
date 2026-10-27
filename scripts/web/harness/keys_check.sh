#!/bin/bash
# GH-174: plays GH-163's dialog flows and GH-164's kept focus from the keyboard in headless Chrome
# (report=keys) and diffs the report against tests/keys_expected.txt, so a change to the page's
# keys, focus, inert or announcements (GH-166) shows up.
#
# Usage: keys_check.sh
#   Builds the bundle first, so it's never stale. Exit 0: as expected; 1: differences, shown as a
#   diff; 2: a failed build or a TIMEOUT. Run by the agent, not CI: it needs Chrome.

set -u

here="$(cd "$(dirname "$0")" && pwd)"
project="$(cd "$here/../../.." && pwd)"
expected="$here/tests/keys_expected.txt"
queries=(
  "report=keys&keys=click:Labels,S-Tab,Tab,Tab,Escape"
  "report=keys&keys=focus:new,Enter,Escape"
  "report=keys&keys=click:Save,Escape"
  # GH-164: focus kept across the redraw, or passed on.
  "report=keys&keys=focus:load,Enter,Escape,Tab"
  "report=keys&keys=focus:arrow-up-0,Enter,Enter,R-Enter,Tab"
  # grid=1 (GH-167): no table and no Read board on the solution screen.
  "report=keys&grid=1&mode=solution&keys=focus:replay-next,Enter,focus:replay-prev,Enter"
  "report=keys&mode=solution&keys=focus:replay-next,Enter,Enter,focus:replay-restart,Enter"
  # GH-166: what the live region says for a new game and a shift.
  "report=keys&keys=click:New,click:5x5,click:▶"
  # GH-167: the board as a table and Read board, pressed, after a shift and under a dialog.
  "report=keys&grid=1&keys=focus:read-board,Enter,click:▶,Enter,click:Labels"
)

log="$(cd "$project" && ./gradlew -q :web:jsBrowserDistribution 2>&1)" || {
  echo "$log" >&2
  exit 2
}
# mode=solution plays a random board, so its solution's length and steps vary: those lines keep
# only the shape of what's said, e.g. `Step N of N: LINE N DIR N`. So do the play board's tiles
# on grid lines (GH-167): the numbers said and counted, and which of the first row's cells are in place.
actual="$("$here/measure.sh" "${queries[@]}" | perl -pe '
  s{said "\K[^"]*}{$& =~ s/\d+/N/gr =~ s/\b(?:Row|Column) N (?:left|right|up|down)\b/LINE N DIR/gr}e if /^\S+ solution /;
  if (/\| grid table/) { s{(?:said|row1) "\K[^"]*}{$& =~ s/\d+/N/gr =~ s/, in place//gr =~ s/\b(tile|move)s?\b/$1s/gr}ge; s/in-place \K\d+/N/ }')"
if grep -q '^TIMEOUT' <<<"$actual"; then
  echo "$actual" >&2
  exit 2
fi
diff -u "$expected" - <<<"$actual" || exit 1
echo "OK: $(wc -l <"$expected" | tr -d ' ') lines as expected"
