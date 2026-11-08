#!/bin/bash
# UserPromptSubmit hook: a self-imposed working-hours guard, ported from Habit
# Loop (GH-237 debrief): late-night sessions lower attentiveness. Two curfews:
#   hard  blocks any new prompt from CURFEW_START to CURFEW_END (default
#         23:00-07:59 local time); no per-message override.
#   soft  (GH-248) warns, without blocking, in the hour before CURFEW_START:
#         four marks at :00, :15, :30 and :45, each shown once per day on the
#         first prompt at or after it, firmer each time. The user sees it
#         (systemMessage) and so does Claude (additionalContext).
#
# Configuration (environment variables):
#   CURFEW_START  first blocked hour, 0-23 (default 23)
#   CURFEW_END    first allowed hour again, 0-23 (default 8)
#                 (either one not an hour switches both curfews off)
#   CURFEW_TZ     time zone for the clock, e.g. Europe/Berlin (default: the
#                 machine's; cloud containers run in UTC, so set this there)
#
# One switch for both curfews: CURFEW=off in .claude/curfew.env (gitignored,
# per machine; created with CURFEW=on when missing). Soft marks already shown are kept in .claude/curfew.state.
# Test hooks, not settings: CURFEW_ENV_FILE, CURFEW_STATE_FILE and
# CURFEW_NOW_OVERRIDE="YYYY-MM-DD HH:MM".
#
# Exit 2 = block the prompt; stderr is shown to the user. Exit 0 = allow; stdout
# JSON carries the soft warning.

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-.}"
TOGGLE_FILE="${CURFEW_ENV_FILE:-$PROJECT_DIR/.claude/curfew.env}"
STATE_FILE="${CURFEW_STATE_FILE:-$PROJECT_DIR/.claude/curfew.state}"

# Drain the hook input JSON; leaving it unread causes broken-pipe noise.
cat >/dev/null

CURFEW=on
# A missing toggle file is created, so the switch the block message names exists
# to edit; a file that can't be written only means the curfew stays on.
if [ -f "$TOGGLE_FILE" ]; then
  source "$TOGGLE_FILE"
else
  echo "CURFEW=on" 2>/dev/null >"$TOGGLE_FILE"
fi
[ "$CURFEW" = "on" ] || exit 0

# A start or end that is not an hour (a CRLF in the env file, a typo) switches
# both curfews off: failing closed would refuse every prompt until fixed by hand.
start="${CURFEW_START:-23}"
end="${CURFEW_END:-8}"
for hour_setting in "$start" "$end"; do
  [[ "$hour_setting" =~ ^[0-9]{1,2}$ ]] && [ $((10#$hour_setting)) -le 23 ] || exit 0
done
start=$((10#$start))
end=$((10#$end))

# Read the clock once, so the date, hour and minute come from the same minute.
# An empty TZ would mean UTC, so TZ is set only when CURFEW_TZ is.
if [ -n "${CURFEW_NOW_OVERRIDE:-}" ]; then
  now="$CURFEW_NOW_OVERRIDE"
elif [ -n "${CURFEW_TZ:-}" ]; then
  now=$(TZ="$CURFEW_TZ" date '+%F %H:%M')
else
  now=$(date '+%F %H:%M')
fi
today="${now%% *}"
hour=$((10#${now:11:2}))
minute=$((10#${now:14:2}))

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

# Soft curfew: no hard window (start == end) means nothing to warn about.
[ "$start" -ne "$end" ] || exit 0
[ "$hour" -eq $(( (start + 23) % 24 )) ] || exit 0

mark=$(( minute / 15 ))

# State is "<date> <mark>"; anything else counts as nothing shown yet.
shown_date=""
shown_mark=""
read -r shown_date shown_mark extra 2>/dev/null <"$STATE_FILE"
if [ "$shown_date" = "$today" ] && [ -z "$extra" ] && [[ "$shown_mark" =~ ^[0-3]$ ]] \
    && [ "$shown_mark" -ge "$mark" ]; then
  exit 0
fi

case "$mark" in
  0) user="Soft curfew: the hard curfew starts at ${start}:00, in 1 hour. Plan where to stop."
     claude="Soft curfew (1 of 4): new prompts are blocked from ${start}:00. Mention this in one short line in your reply, then carry on with the request." ;;
  1) user="Soft curfew: 45 minutes to the ${start}:00 curfew. Don't start anything big now."
     claude="Soft curfew (2 of 4): 45 minutes until new prompts are blocked at ${start}:00. Mention this in one short line, then carry on." ;;
  2) user="Soft curfew: 30 minutes to the ${start}:00 curfew. Start wrapping up: finish the current step."
     claude="Soft curfew (3 of 4): 30 minutes until new prompts are blocked at ${start}:00. Mention this in one short line, then carry on." ;;
  3) user="Soft curfew: 15 minutes left. At ${start}:00 this session stops taking new prompts until ${end}:00. Wrap up now: commit, push, and write down where you stopped."
     claude="Soft curfew (4 of 4): new prompts are blocked at ${start}:00 until ${end}:00, in 15 minutes. Steer the user toward wrapping up: bring the current step to a safe stopping point, commit and push work in progress, say what is left, and don't start new large work." ;;
esac

# The texts are fixed and hold no quotes or backslashes, so printf needs no escaping.
printf '{"systemMessage": "%s", "hookSpecificOutput": {"hookEventName": "UserPromptSubmit", "additionalContext": "%s"}}\n' "$user" "$claude"

# A state file that can't be written only means the warning may repeat.
echo "$today $mark" 2>/dev/null >"$STATE_FILE"
exit 0
