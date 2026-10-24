#!/bin/bash
# GH-147: serves the built web game to the local network, for a check on a real phone before the PR.
# The user runs this (auto mode won't let the agent expose a local service); Ctrl+C stops it.
#
# Usage: serve_lan.sh [port]   (after ./gradlew :web:jsBrowserDistribution)

set -u

project="$(cd "$(dirname "$0")/../../.." && pwd)"
dist="$project/web/build/dist/js/productionExecutable"
port="${1:-8766}"

if [[ ! -f "$dist/index.html" ]]; then
  echo "No bundle: run ./gradlew :web:jsBrowserDistribution first." >&2
  exit 2
fi
address="$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null)"
echo "On the phone, on the same Wi-Fi: http://${address:-<this Mac's IP>}:$port/"
cd "$dist" && exec python3 -m http.server "$port" --bind 0.0.0.0
