#!/bin/bash
# UserPromptSubmit hook: hard-blocks any new prompt during the curfew window
# (default 23:00-07:59 local time). A self-imposed working-hours guard, ported
# from Habit Loop (GH-237 debrief): late-night sessions lower attentiveness.
#
# Configuration (environment variables):
#   CURFEW_START  first blocked hour, 0-23 (default 23)
#   CURFEW_END    first allowed hour again, 0-23 (default 8)
#   CURFEW_TZ     time zone for the clock, e.g. Europe/Berlin (default: the
#                 machine's; cloud containers run in UTC, so set this there)
#
# Deliberately no per-message override. To switch the guard off, set
# CURFEW=off in .claude/curfew.env (gitignored, per machine).
#
# Exit 2 = block the prompt; stderr is shown to the user. Exit 0 = allow.

TOGGLE_FILE="${CURFEW_ENV_FILE:-${CLAUDE_PROJECT_DIR:-.}/.claude/curfew.env}"

# Drain the hook input JSON; leaving it unread causes broken-pipe noise.
cat >/dev/null

CURFEW=on
[ -f "$TOGGLE_FILE" ] && source "$TOGGLE_FILE"
[ "$CURFEW" = "on" ] || exit 0

start=$((10#${CURFEW_START:-23}))
end=$((10#${CURFEW_END:-8}))
if [ -n "${CURFEW_HOUR_OVERRIDE:-}" ]; then
  hour=$((10#$CURFEW_HOUR_OVERRIDE))
elif [ -n "${CURFEW_TZ:-}" ]; then
  hour=$((10#$(TZ="$CURFEW_TZ" date +%H)))
else
  hour=$((10#$(date +%H)))
fi

if [ "$start" -le "$end" ]; then
  blocked=$(( hour >= start && hour < end ))
else
  # The window crosses midnight.
  blocked=$(( hour >= start || hour < end ))
fi

if [ "$blocked" -eq 1 ]; then
  echo "BLOCKED: it's outside your working hours (curfew ${start}:00-${end}:00). This session won't act on new requests until ${end}:00. To disable the guard, set CURFEW=off in .claude/curfew.env." >&2
  exit 2
fi

exit 0
