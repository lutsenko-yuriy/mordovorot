#!/bin/bash
# GH-156: compares every element's computed styles in the built web game against a baseline bundle, case by
# case, so a CSS refactor can show the game looks the same.
#
# Usage: styles_diff.sh [--text-size=<percent>] [--build-baseline[=<ref>]] <baseline dist dir> [<query>...]
#   Builds the branch's bundle first, so it's never stale. --build-baseline builds <ref> (default
#   origin/master) into <baseline dist dir>, and later runs reuse it until <ref> moves. Each <query> is
#   harness.html's query string without v and report (see README.md); by default the play, dialog and
#   solution screens on a 4x4 board at 390x844, 844x390 and 1200x800. --text-size sets the browser's
#   font size for both bundles (GH-168).
#   Exit 0: no differences; 1: differences, or a case on one side only; 2: usage, build or TIMEOUT.

set -u

here="$(cd "$(dirname "$0")" && pwd)"
project="$(cd "$here/../../.." && pwd)"
dist="$project/web/build/dist/js/productionExecutable"
chrome="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"
# Not 8765 (measure.sh) or 8766 (serve_lan.sh), so they can all run side by side.
port="${STYLES_DIFF_PORT:-8767}"
max_wait_s=600
source "$here/lib.sh"

text_size=100
take_text_size "${1:-}" && shift
ref=""
case "${1:-}" in
  --build-baseline) ref=origin/master; shift ;;
  --build-baseline=*) ref="${1#*=}"; shift ;;
esac
if [[ $# -eq 0 ]]; then
  echo "Usage: $0 [--text-size=<percent>] [--build-baseline[=<ref>]] <baseline dist dir> [<query>...]" >&2
  exit 2
fi
baseline="$1"
shift
queries=("$@")
if [[ ${#queries[@]} -eq 0 ]]; then
  for mode in play dialog solution; do
    queries+=("mode=$mode&boards=4&sizes=390x844,844x390,1200x800")
  done
fi

make_work
trap 'stop_work; git -C "$project" worktree prune' EXIT

# Builds the web bundle in a checkout, showing Gradle's output only when the build fails.
build_web() {
  (cd "$1" && ./gradlew -q :web:jsBrowserDistribution) >"$work/build.log" 2>&1 || {
    cat "$work/build.log" >&2
    return 1
  }
}

baseline="${baseline%/}"
if [[ -z "$baseline" ]]; then
  echo "The baseline can't be /" >&2
  exit 2
fi
if [[ -n "$ref" ]]; then
  # A built bundle is stamped with its commit, so a moved ref is rebuilt rather than silently reused.
  commit="$(git -C "$project" rev-parse --verify -q "$ref^{commit}")" || {
    echo "Unknown ref $ref" >&2
    exit 2
  }
  if [[ "$(cat "$baseline/.commit" 2>/dev/null)" != "$commit" ]]; then
    if [[ -e "$baseline" && ! -f "$baseline/.commit" && -n "$(ls -A "$baseline")" ]]; then
      echo "$baseline isn't empty and wasn't built by $0; pass an empty or new directory" >&2
      exit 2
    fi
    tree="$work/baseline-src"
    # Copied beside the target and moved in whole, so an interrupted copy is never taken for a bundle.
    partial="$baseline.partial"
    mkdir -p "$(dirname "$baseline")" || exit 2
    rm -rf "$partial"
    git -C "$project" worktree add -q --detach "$tree" "$commit" \
      && build_web "$tree" \
      && cp -R "$tree/web/build/dist/js/productionExecutable" "$partial" \
      && echo "$commit" >"$partial/.commit" \
      && rm -rf "$baseline" && mv "$partial" "$baseline"
    built=$?
    git -C "$project" worktree remove --force "$tree" 2>/dev/null
    if [[ $built -ne 0 ]]; then
      rm -rf "$partial"
      echo "Couldn't build $ref into $baseline" >&2
      exit 2
    fi
  fi
elif [[ ! -f "$baseline/index.html" ]]; then
  echo "No bundle in $baseline; pass --build-baseline to build one" >&2
  exit 2
fi

if ! build_web "$project"; then
  echo "Couldn't build the branch's bundle" >&2
  exit 2
fi

add_variant "$dist" mouse mouse
add_variant "$dist" touch touch
add_variant "$baseline" master mouse
add_variant "$baseline" master-touch touch
all=()
for variant in mouse touch master master-touch; do
  python3 "$here/styles_diff.py" seed "$work/site/$variant" || exit 2
  for query in "${queries[@]}"; do
    all+=("v=$variant&report=styles&$query")
  done
done

start_server
run_queries "${all[@]}"
timed_out=$?
python3 "$here/styles_diff.py" compare "$results"
differs=$?
[[ $timed_out -eq 0 ]] || exit 2
exit "$differs"
