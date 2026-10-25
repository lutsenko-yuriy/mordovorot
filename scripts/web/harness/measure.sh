#!/bin/bash
# GH-147: measures the built web game at phone sizes in headless Chrome and prints one line per case.
#
# Usage: measure.sh [--baseline <dist dir>] <query>...
#   Run `./gradlew :web:jsBrowserDistribution` first. Each <query> is harness.html's query string
#   (see README.md), e.g. "v=touch&mode=solution&boards=3". Variants: mouse, touch (the
#   `pointer: coarse` rules always on), and with --baseline, master and master-touch from that bundle.
#
# Chrome runs headless in real time with a throwaway profile; see lib.sh.

set -u

here="$(cd "$(dirname "$0")" && pwd)"
project="$(cd "$here/../../.." && pwd)"
dist="$project/web/build/dist/js/productionExecutable"
chrome="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"
port="${PHONE_FIT_PORT:-8765}"
# One query's cases, at up to ~30s each on the solution screen.
max_wait_s=600
source "$here/lib.sh"

baseline=""
if [[ "${1:-}" == "--baseline" ]]; then
  baseline="$2"
  shift 2
fi
if [[ $# -eq 0 || ! -f "$dist/index.html" ]]; then
  echo "Usage: $0 [--baseline <dist dir>] <query>... (after ./gradlew :web:jsBrowserDistribution)" >&2
  exit 2
fi

make_work
add_variant "$dist" mouse mouse
add_variant "$dist" touch touch
if [[ -n "$baseline" ]]; then
  add_variant "$baseline" master mouse
  add_variant "$baseline" master-touch touch
fi
start_server
run_queries "$@"

grep -v '^#' "$results"
