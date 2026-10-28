# GH-156: shared by measure.sh and styles_diff.sh, which set here, chrome, port and max_wait_s first,
# and text_size (GH-168) through take_text_size. make_work sets work, results and server for them;
# stop_work, the EXIT trap, removes them.
#
# Real time, never --virtual-time-budget: Mordie's search keeps virtual time from ever running out.
# Chrome runs headless with a throwaway profile, never the user's own (docs/CONSTRAINTS.md).

# A scratch dir with the harness in its site, removed with the server on exit.
make_work() {
  work="$(mktemp -d)"
  server=""
  trap stop_work EXIT
  mkdir -p "$work/site"
  cp "$here/harness.html" "$here/keys.js" "$work/site/"
  results="$work/results.txt"
  : >"$results"
  text_size_prefs "$text_size" "$work/profile"
}

# GH-168: the browser's font size as a whole percent of the standard 16px, in the throwaway profile's
# Preferences, as a player's browser setting would set it, rounded to the nearest px; 100 leaves the
# profile as it is.
text_size_prefs() {
  valid_text_size "$1" || return 1
  [[ "$1" -eq 100 ]] && return 0
  mkdir -p "$2/Default"
  printf '{"webkit":{"webprefs":{"default_font_size":%d}}}\n' $((($1 * 16 + 50) / 100)) >"$2/Default/Preferences"
}

# A whole percent from 50 to 400 (8px to 64px).
valid_text_size() {
  [[ "$1" =~ ^[1-9][0-9]{0,2}$ ]] && (($1 >= 50 && $1 <= 400))
}

# Sets text_size from a leading --text-size=<percent> and returns 0 when it took one, 1 when $1 is
# something else; exits 2 on a bad value.
take_text_size() {
  [[ "${1:-}" == --text-size=* ]] || return 1
  text_size="${1#*=}"
  if ! valid_text_size "$text_size"; then
    echo "--text-size takes a whole percent of the standard 16px from 50 to 400, e.g. --text-size=150" >&2
    exit 2
  fi
}

stop_work() {
  if [[ -n "$server" ]]; then
    kill "$server" 2>/dev/null
    wait "$server" 2>/dev/null
  fi
  rm -rf "$work"
}

# A variant: a copy of a bundle, with the touch-screen rules forced on when asked.
add_variant() {
  cp -R "$1" "$work/site/$2"
  if [[ "$3" == "touch" ]]; then
    sed -i '' 's/@media (pointer: coarse)/@media all/' "$work/site/$2"/*.css
  fi
}

start_server() {
  python3 "$here/serve.py" "$work/site" "$results" "$port" &
  server=$!
  sleep 1
}

# Loads harness.html with each query in turn; prints "TIMEOUT <query>" and returns 1 if any never reports.
run_queries() {
  local query before browser i timed_out=0
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
    if [[ $(grep -c '^#done' "$results") -le "$before" ]]; then
      echo "TIMEOUT $query"
      timed_out=1
    fi
  done
  return "$timed_out"
}
