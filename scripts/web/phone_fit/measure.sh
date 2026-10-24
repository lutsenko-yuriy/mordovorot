#!/bin/bash
# GH-147: measures the built web game at phone sizes in headless Chrome and prints one line per case.
#
# Usage: measure.sh [--baseline <dist dir>] <query>...
#   Run `./gradlew :web:jsBrowserDistribution` first. Each <query> is harness.html's query string
#   (see README.md), e.g. "v=touch&mode=solution&boards=3". Variants: mouse, touch (the
#   `pointer: coarse` rules always on), and with --baseline, master and master-touch from that bundle.
#
# Real time, never --virtual-time-budget: Mordie's search keeps virtual time from ever running out.
# Chrome runs headless with a throwaway profile, never the user's own (docs/CONSTRAINTS.md).

set -u

here="$(cd "$(dirname "$0")" && pwd)"
project="$(cd "$here/../../.." && pwd)"
dist="$project/web/build/dist/js/productionExecutable"
chrome="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"
port="${PHONE_FIT_PORT:-8765}"
# One query's cases, at up to ~30s each on the solution screen.
max_wait_s=600

baseline=""
if [[ "${1:-}" == "--baseline" ]]; then
  baseline="$2"
  shift 2
fi
if [[ $# -eq 0 || ! -f "$dist/index.html" ]]; then
  echo "Usage: $0 [--baseline <dist dir>] <query>... (after ./gradlew :web:jsBrowserDistribution)" >&2
  exit 2
fi

work="$(mktemp -d)"
trap 'kill "$server" 2>/dev/null; wait "$server" 2>/dev/null; rm -rf "$work"' EXIT

# A variant: a copy of a bundle, with the touch-screen rules forced on when asked.
add_variant() {
  cp -R "$1" "$work/site/$2"
  if [[ "$3" == "touch" ]]; then
    sed -i '' 's/@media (pointer: coarse)/@media all/' "$work/site/$2"/*.css
  fi
}

mkdir -p "$work/site"
cp "$here/harness.html" "$work/site/"
add_variant "$dist" mouse mouse
add_variant "$dist" touch touch
if [[ -n "$baseline" ]]; then
  add_variant "$baseline" master mouse
  add_variant "$baseline" master-touch touch
fi
results="$work/results.txt"
: >"$results"

python3 "$here/serve.py" "$work/site" "$results" "$port" &
server=$!
sleep 1

for query in "$@"; do
  before=$(grep -c '^#done' "$results")
  "$chrome" --headless=new --user-data-dir="$work/profile" "http://127.0.0.1:$port/harness.html?$query" \
    >/dev/null 2>&1 &
  browser=$!
  for ((i = 0; i < max_wait_s * 2; i++)); do
    [[ $(grep -c '^#done' "$results") -gt "$before" ]] && break
    sleep 0.5
  done
  kill "$browser" 2>/dev/null
  wait "$browser" 2>/dev/null
  [[ $(grep -c '^#done' "$results") -gt "$before" ]] || echo "TIMEOUT $query"
done

grep -v '^#' "$results"
