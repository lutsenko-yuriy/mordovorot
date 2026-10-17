#!/bin/bash
# Notification/Stop hook (GH-107, ported from Habit Loop's HAB-162): alerts when a session waits on the user —
# Notification (permission prompt, or idle > 60s) or Stop (a turn ended). bash 3.2-safe.
#
# Two channels, a macOS notification (osascript) and speech (say), both on by default. Toggles live in the gitignored
# <project>/.claude/alert.env, sourced on every alert, so a change applies to the next one without a restart:
#   YAB_ALERT_NOTIFY=on|off   YAB_ALERT_SPEAK=on|off   YAB_ALERT_SUPPRESS_FOCUSED=on|off
# SUPPRESS_FOCUSED skips both channels while any terminal app is frontmost (assumed to be this session).
# A channel whose tool is missing (non-macOS) is skipped. Always exits 0 with no output, so Stop is never blocked.
#
# Test seams: YAB_ALERT_DRY_RUN=1 prints "NOTIFY: ..." / "SPEAK: ..." instead of firing; YAB_ALERT_ENV overrides the
# toggle file; YAB_ALERT_FRONTMOST fakes the frontmost app (set-but-empty counts as set).

project="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "$0")/../.." && pwd)}"
alert_env="${YAB_ALERT_ENV:-$project/.claude/alert.env}"
terminal_apps="Terminal iTerm2 iTerm Ghostty Alacritty kitty WezTerm Hyper Warp"

input=$(cat)

# Gate 1: only Notification and Stop, checked before the slower frontmost-app query.
event=$(printf '%s' "$input" | jq -r '.hook_event_name // ""' 2>/dev/null)
case "$event" in
  Notification | Stop) ;;
  *) exit 0 ;;
esac

YAB_ALERT_NOTIFY=on
YAB_ALERT_SPEAK=on
YAB_ALERT_SUPPRESS_FOCUSED=on
# shellcheck source=/dev/null
[ -f "$alert_env" ] && . "$alert_env"
# Opted out: skip the osascript query below (and its Automation consent prompt).
[ "$YAB_ALERT_NOTIFY" = "on" ] || [ "$YAB_ALERT_SPEAK" = "on" ] || exit 0

# Gate 2: a frontmost terminal means the user is already watching.
if [ "$YAB_ALERT_SUPPRESS_FOCUSED" = "on" ]; then
  if [ -n "${YAB_ALERT_FRONTMOST+set}" ]; then
    frontmost="$YAB_ALERT_FRONTMOST"
  else
    frontmost=$(osascript -e 'tell application "System Events" to name of first application process whose frontmost is true' 2>/dev/null)
  fi
  for app in $terminal_apps; do
    [ "$frontmost" = "$app" ] && exit 0
  done
fi

cwd=$(printf '%s' "$input" | jq -r '.cwd // ""' 2>/dev/null)
repo=$(basename "${cwd:-$PWD}")
title="Claude Code — $repo"
if [ "$event" = "Notification" ]; then
  body=$(printf '%s' "$input" | jq -r '.message // ""' 2>/dev/null)
  [ -n "$body" ] || body="Claude needs your input"
else
  body="Session in $repo stopped and is waiting"
fi

if [ "${YAB_ALERT_DRY_RUN:-0}" = "1" ]; then
  [ "$YAB_ALERT_NOTIFY" = "on" ] && echo "NOTIFY: $title - $body"
  [ "$YAB_ALERT_SPEAK" = "on" ] && echo "SPEAK: $body"
  exit 0
fi

if [ "$YAB_ALERT_NOTIFY" = "on" ] && command -v osascript >/dev/null 2>&1; then
  osascript -e 'on run argv
    display notification (item 2 of argv) with title (item 1 of argv)
  end run' "$title" "$body" >/dev/null 2>&1
fi
if [ "$YAB_ALERT_SPEAK" = "on" ] && command -v say >/dev/null 2>&1; then
  say "$body" >/dev/null 2>&1
fi
exit 0
