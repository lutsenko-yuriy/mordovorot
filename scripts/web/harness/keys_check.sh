#!/bin/bash
# GH-174: plays GH-163's dialog flows from the keyboard in headless Chrome (report=keys) and diffs the
# report against tests/keys_expected.txt, so a change to the page's keys, focus or inert shows up.
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
)

log="$(cd "$project" && ./gradlew -q :web:jsBrowserDistribution 2>&1)" || {
  echo "$log" >&2
  exit 2
}
actual="$("$here/measure.sh" "${queries[@]}")"
if grep -q '^TIMEOUT' <<<"$actual"; then
  echo "$actual" >&2
  exit 2
fi
diff -u "$expected" - <<<"$actual" || exit 1
echo "OK: $(wc -l <"$expected" | tr -d ' ') lines as expected"
