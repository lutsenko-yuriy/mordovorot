#!/bin/bash
# GH-147: serves the built web game to the local network, for a check on a real phone before the PR.
# The user runs this (auto mode won't let the agent expose a local service); Ctrl+C stops it.
#
# Usage: serve_lan.sh [port]
# Builds the bundle first unless it was built from HEAD with a clean tree (GH-160: the phone got a
# bundle older than the fix it was meant to show). SERVE_LAN_DRY_RUN=1 stops before serving (tests).

set -u

project="$(cd "$(dirname "$0")/../../.." && pwd)"
dist="$project/web/build/dist/js/productionExecutable"
port="${1:-8766}"

head="$(git -C "$project" rev-parse HEAD)"
# Uncommitted changes never match a stamp, so they always rebuild; Gradle skips what's current.
[[ -z "$(git -C "$project" status --porcelain)" ]] || head="$head-dirty"
if [[ ! -f "$dist/index.html" || "$(cat "$dist/.commit" 2>/dev/null)" != "$head" || "$head" == *-dirty ]]; then
  echo "Building the web bundle for $head..."
  (cd "$project" && ./gradlew -q :web:jsBrowserDistribution) || exit 2
  echo "$head" >"$dist/.commit"
fi
[[ "${SERVE_LAN_DRY_RUN:-}" != "1" ]] || exit 0
address="$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null)"
# Not inside ${...:-...}: macOS's bash 3.2 takes an apostrophe there for an opening quote.
[[ -n "$address" ]] || address="<this Mac's IP>"
url="http://$address:$port/"
echo "On the phone, on the same Wi-Fi: $url"
# A code for the phone's camera, so its browser can't suggest an older address (GH-148).
if command -v qrencode >/dev/null 2>&1; then
  qrencode -t ANSIUTF8 "$url"
else
  echo "(brew install qrencode to get a QR code for the phone's camera here)"
fi
cd "$dist" && exec python3 -m http.server "$port" --bind 0.0.0.0
