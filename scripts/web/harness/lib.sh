# GH-156: shared by measure.sh and styles_diff.sh, which set here, chrome, port and max_wait_s first.
# make_work sets work, results and server for them; stop_work, the EXIT trap, removes them.
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
