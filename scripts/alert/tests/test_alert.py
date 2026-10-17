"""alert.sh (GH-107): desktop + spoken alert on Notification/Stop, ported from Habit Loop's HAB-162 hook.

Runs the script in dry-run mode (YAB_ALERT_DRY_RUN=1 prints NOTIFY:/SPEAK: lines) with a scratch toggle file
(YAB_ALERT_ENV) and a faked frontmost app (YAB_ALERT_FRONTMOST), so results never depend on the machine.

Behaviour table:
    event          | toggles               | frontmost | expect
    Notification   | none                  | other     | NOTIFY + SPEAK with the hook's message (fallback text if absent)
    Stop           | none                  | other     | "Session in <repo> stopped and is waiting"
    Notification   | NOTIFY/SPEAK/both off | other     | only the channels left on
    other / junk   | any                   | other     | silent, exit 0
    Notification   | none                  | terminal  | silent; fires when SUPPRESS_FOCUSED=off
    toggle file    | from $CLAUDE_PROJECT_DIR, else two levels above the script
    real run       | no osascript/say on PATH -> exit 0, no output

Wiring: Notification and Stop each run "${CLAUDE_PROJECT_DIR}/scripts/alert/alert.sh" exactly once, unfiltered;
alert.sh is tracked executable; .claude/alert.env is ignored.
"""

import json
import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
ALERT_SH = ROOT / "scripts" / "alert" / "alert.sh"
BASH = shutil.which("bash")
MESSAGE = "Claude needs permission to run rm"


def notification(message=MESSAGE, cwd="/tmp/some-repo"):
    d = {"hook_event_name": "Notification", "cwd": cwd}
    if message is not None:
        d["message"] = message
    return json.dumps(d)


class AlertTests(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp, ignore_errors=True)
        self.toggles = self.tmp / "alert.env"

    def run_alert(self, payload, toggles=None, script=ALERT_SH, **env):
        if toggles is not None:
            self.toggles.write_text(toggles, encoding="utf-8")
        full = {k: v for k, v in os.environ.items() if not k.startswith("YAB_ALERT_") and k != "CLAUDE_PROJECT_DIR"}
        full.update({"YAB_ALERT_DRY_RUN": "1", "YAB_ALERT_ENV": str(self.toggles), "YAB_ALERT_FRONTMOST": "SomeOtherApp"})
        full.update(env)
        for k in [k for k, v in full.items() if v is None]:
            del full[k]
        proc = subprocess.run([BASH, str(script)], input=payload, capture_output=True, text=True, env=full,
                              timeout=10)
        return proc.returncode, proc.stdout

    def assertSilent(self, result):
        self.assertEqual((0, ""), result)

    def test_notification_fires_both_channels_by_default(self):
        rc, out = self.run_alert(notification())
        self.assertEqual(0, rc)
        self.assertIn("NOTIFY: Claude Code — some-repo - %s" % MESSAGE, out)
        self.assertIn("SPEAK: %s" % MESSAGE, out)

    def test_notification_without_message_uses_fallback(self):
        _, out = self.run_alert(notification(message=None))
        self.assertIn("SPEAK: Claude needs your input", out)

    def test_stop_composes_its_own_message(self):
        _, out = self.run_alert(json.dumps({"hook_event_name": "Stop", "cwd": "/tmp/mordovorot"}))
        self.assertIn("NOTIFY: Claude Code — mordovorot - Session in mordovorot stopped and is waiting", out)
        self.assertIn("SPEAK: Session in mordovorot stopped and is waiting", out)

    def test_notify_off_leaves_speak(self):
        _, out = self.run_alert(notification(), "YAB_ALERT_NOTIFY=off\n")
        self.assertIn("SPEAK:", out)
        self.assertNotIn("NOTIFY:", out)

    def test_speak_off_leaves_notify(self):
        _, out = self.run_alert(notification(), "YAB_ALERT_SPEAK=off\n")
        self.assertIn("NOTIFY:", out)
        self.assertNotIn("SPEAK:", out)

    def test_both_off_is_silent(self):
        self.assertSilent(self.run_alert(notification(), "YAB_ALERT_NOTIFY=off\nYAB_ALERT_SPEAK=off\n"))

    def test_other_events_are_silent(self):
        self.assertSilent(self.run_alert(json.dumps({"hook_event_name": "PreToolUse", "cwd": "/tmp/x"})))

    def test_malformed_or_empty_input_is_silent(self):
        self.assertSilent(self.run_alert("not json"))
        self.assertSilent(self.run_alert(""))

    def test_terminal_frontmost_suppresses(self):
        for app in ("Terminal", "Ghostty"):
            self.assertSilent(self.run_alert(notification(), YAB_ALERT_FRONTMOST=app))

    def test_other_app_frontmost_fires(self):
        _, out = self.run_alert(notification(), YAB_ALERT_FRONTMOST="Safari")
        self.assertIn("NOTIFY:", out)
        self.assertIn("SPEAK:", out)

    def test_suppress_focused_off_fires_with_terminal_frontmost(self):
        _, out = self.run_alert(notification(), "YAB_ALERT_SUPPRESS_FOCUSED=off\n", YAB_ALERT_FRONTMOST="Terminal")
        self.assertIn("NOTIFY:", out)
        self.assertIn("SPEAK:", out)

    def test_toggle_file_resolved_from_claude_project_dir(self):
        (self.tmp / ".claude").mkdir()
        (self.tmp / ".claude" / "alert.env").write_text("YAB_ALERT_NOTIFY=off\n", encoding="utf-8")
        _, out = self.run_alert(notification(), YAB_ALERT_ENV=None, CLAUDE_PROJECT_DIR=str(self.tmp))
        self.assertIn("SPEAK:", out)
        self.assertNotIn("NOTIFY:", out)

    def test_toggle_file_falls_back_to_script_location(self):
        copy = self.tmp / "scripts" / "alert" / "alert.sh"
        copy.parent.mkdir(parents=True)
        shutil.copy(ALERT_SH, copy)
        (self.tmp / ".claude").mkdir()
        (self.tmp / ".claude" / "alert.env").write_text("YAB_ALERT_NOTIFY=off\n", encoding="utf-8")
        _, out = self.run_alert(notification(), script=copy, YAB_ALERT_ENV=None)
        self.assertIn("SPEAK:", out)
        self.assertNotIn("NOTIFY:", out)

    def test_real_run_without_macos_tools_exits_quietly(self):
        bin_dir = self.tmp / "bin"
        bin_dir.mkdir()
        for tool in ("jq", "cat", "basename", "dirname"):
            found = shutil.which(tool)
            self.assertIsNotNone(found, tool)
            (bin_dir / tool).symlink_to(found)
        result = self.run_alert(notification(), YAB_ALERT_DRY_RUN=None, PATH=str(bin_dir))
        self.assertSilent(result)


class WiringTests(unittest.TestCase):
    COMMAND = '"${CLAUDE_PROJECT_DIR}/scripts/alert/alert.sh"'

    def test_notification_and_stop_each_wired_once(self):
        hooks = json.loads((ROOT / ".claude" / "settings.json").read_text(encoding="utf-8"))["hooks"]
        rows = [(event, group, hook) for event, groups in hooks.items() for group in groups
                for hook in group["hooks"] if hook.get("command") == self.COMMAND]
        self.assertEqual(["Notification", "Stop"], sorted(e for e, _, _ in rows))
        for event, group, hook in rows:
            self.assertEqual("command", hook["type"])
            self.assertIn(group.get("matcher", ""), ("", "*"), event)
            self.assertGreaterEqual(hook["timeout"], 10, event)

    def test_alert_sh_is_tracked_executable(self):
        out = subprocess.run(["git", "-C", str(ROOT), "ls-files", "-s", "scripts/alert/alert.sh"],
                             capture_output=True, text=True, check=True).stdout
        self.assertTrue(out.startswith("100755"), out)

    def test_toggle_file_is_ignored(self):
        self.assertEqual(0, subprocess.run(["git", "-C", str(ROOT), "check-ignore", "-q", ".claude/alert.env"]).returncode)


if __name__ == "__main__":
    unittest.main()
