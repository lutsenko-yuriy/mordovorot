#!/bin/bash
# GH-147: serves the built web game to the local network, for a check on a real phone before the PR.
# The user runs this (auto mode won't let the agent expose a local service); Ctrl+C stops it.
#
# Usage: serve_lan.sh [port]
# Builds the bundle first unless it was built from HEAD with a clean tree (GH-160: the phone got a
# bundle older than the fix it was meant to show). Then it follows the commits (GH-283): every
# SERVE_LAN_POLL_S seconds (15) it looks at HEAD, and a new one is built and served in place of the
# old. A build that fails keeps the old bundle served. Uncommitted edits are built at start-up, and ride
# along (stamped -dirty) in a build a commit starts; saving a file starts none. The address and its QR
# code are printed again after each new build (GH-311).
# Each build is copied to web/build/serve-lan/ and served from there, so the phone never gets a half-built
# bundle, nor one a harness run left in dist/. SERVE_LAN_DRY_RUN=1 stops before serving (tests);
# SERVE_LAN_BIND sets the address to serve on (0.0.0.0).

set -u

project="$(cd "$(dirname "$0")/../../.." && pwd)"
dist="$project/web/build/dist/js/productionExecutable"
serve="$project/web/build/serve-lan"
port="${1:-8766}"
poll="${SERVE_LAN_POLL_S:-15}"

# The commit a bundle built now would show; uncommitted changes never match a stamp (-dirty).
stamp() {
  local head
  head="$(git -C "$project" rev-parse HEAD)"
  [[ -z "$(git -C "$project" status --porcelain)" ]] || head="$head-dirty"
  echo "$head"
}

# Builds the bundle and serves it from then on; fails, leaving what is served, if the build does.
build() {
  local label="$1" copy
  echo "Building the web bundle for $label..."
  (cd "$project" && ./gradlew -q :web:jsBrowserDistribution) || return 1
  echo "$label" >"$dist/.commit"
  mkdir -p "$serve" && copy="$(basename "$(mktemp -d "$serve/copy.XXXXXX")")" && cp -R "$dist/." "$serve/$copy" || return 1
  # A rename replaces the link in one step; a request never finds it missing. A link an interrupted
  # swap left is removed first: ln would follow it into its directory.
  rm -f "$serve/current.new"
  ln -s "$copy" "$serve/current.new" && python3 -c 'import os, sys; os.replace(sys.argv[1], sys.argv[2])' "$serve/current.new" "$serve/current" || return 1
  find "$serve" -mindepth 1 -maxdepth 1 ! -name current ! -name "$copy" -exec rm -rf {} +
  echo "Serving $label"
}

now="$(stamp)"
if [[ ! -f "$serve/current/index.html" || "$(cat "$serve/current/.commit" 2>/dev/null)" != "$now" || "$now" == *-dirty ]]; then
  build "$now" || exit 2
fi
[[ "${SERVE_LAN_DRY_RUN:-}" != "1" ]] || exit 0
address="$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null)"
# Not inside ${...:-...}: macOS's bash 3.2 takes an apostrophe there for an opening quote.
[[ -n "$address" ]] || address="<this Mac's IP>"
url="http://$address:$port/"
# The address and a code for the phone's camera, so its browser can't suggest an older address (GH-148).
# Printed again after every new build (GH-311), so the phone is one scan from the latest commit.
show_url() {
  echo "On the phone, on the same Wi-Fi: $url"
  if command -v qrencode >/dev/null 2>&1; then
    qrencode -t ANSIUTF8 "$url"
  else
    echo "(brew install qrencode to get a QR code for the phone's camera here)"
  fi
}
show_url

# No-cache, so a reload on the phone asks again instead of reusing the old bundle's files.
python3 - "$port" "${SERVE_LAN_BIND:-0.0.0.0}" "$serve/current" <<'PY' &
import functools, http.server, sys

class NoCache(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Cache-Control", "no-cache")
        super().end_headers()

port, bind, root = int(sys.argv[1]), sys.argv[2], sys.argv[3]
handler = functools.partial(NoCache, directory=root)
http.server.ThreadingHTTPServer((bind, port), handler).serve_forever()
PY
server=$!
trap 'kill "$server" 2>/dev/null' EXIT
trap 'exit 130' INT TERM

last="$(git -C "$project" rev-parse HEAD)"
# A moment for the server to fail to start (the port is taken) before the first look at it.
sleep 1
# `wait` rather than a plain sleep, so Ctrl+C is not held up until the sleep ends.
while kill -0 "$server" 2>/dev/null; do
  sleep "$poll" & wait $!
  head="$(git -C "$project" rev-parse HEAD)"
  [[ "$head" != "$last" ]] || continue
  last="$head"
  if build "$(stamp)"; then show_url; else echo "The build failed, still serving $(cat "$serve/current/.commit")."; fi
done
# The server ended on its own (the port is taken): its status is ours.
wait "$server"
