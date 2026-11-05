"""curfew.sh (GH-237, GH-248): working-hours guard on UserPromptSubmit.

Runs the script with a fixed clock (CURFEW_NOW_OVERRIDE="YYYY-MM-DD HH:MM"), a scratch toggle file
(CURFEW_ENV_FILE) and a scratch state file (CURFEW_STATE_FILE), so results never depend on the machine.

Behaviour table (start=23, end=8 unless stated; k = minute // 15 in the hour before start):
    CURFEW | time                  | state file                    | expect
    off    | any                   | any                           | exit 0, silent, state untouched
    on     | 23:00-07:59           | any                           | exit 2, BLOCKED on stderr
    on     | elsewhere             | any                           | exit 0, silent, state untouched
    on     | 22:mm                 | missing/empty/malformed/stale | warning k, state = "<date> k"
    on     | 22:mm                 | today, mark < k               | warning k only (skipped marks never shown)
    on     | 22:mm                 | today, mark >= k              | silent
    on     | 22:mm                 | unwritable                    | warning k, exit 0
    on     | start=0: 23:mm        | as above                      | same rules, soft hour is 23
    on     | start=end             | any                           | never blocks, no warning
    on     | CURFEW_TZ set         | clock read in that zone       | TZ reaches the one date call

Wiring: UserPromptSubmit runs "${CLAUDE_PROJECT_DIR}/scripts/curfew/curfew.sh" exactly once; curfew.sh is
tracked executable; .claude/curfew.env and .claude/curfew.state are ignored.
"""

import json
import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
CURFEW_SH = ROOT / "scripts" / "curfew" / "curfew.sh"
BASH = shutil.which("bash")


class CurfewTests(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp, ignore_errors=True)
        self.toggles = self.tmp / "curfew.env"
        self.state = self.tmp / "curfew.state"

    def run_curfew(self, now, state=None, toggles=None, path=None, **env):
        if toggles is not None:
            self.toggles.write_text(toggles, encoding="utf-8")
        if state is not None:
            self.state.write_text(state, encoding="utf-8")
        full = {k: v for k, v in os.environ.items() if not k.startswith("CURFEW_") and k != "CLAUDE_PROJECT_DIR"}
        full.update({"CURFEW_ENV_FILE": str(self.toggles), "CURFEW_STATE_FILE": str(self.state)})
        if now is not None:
            full["CURFEW_NOW_OVERRIDE"] = now
        if path is not None:
            full["PATH"] = path
        full.update(env)
        proc = subprocess.run([BASH, str(CURFEW_SH)], input="{}", capture_output=True, text=True, env=full,
                              timeout=10)
        return proc

    def warning(self, proc):
        self.assertEqual(0, proc.returncode, proc.stderr)
        self.assertEqual("", proc.stderr)
        return json.loads(proc.stdout)

    def assertSilent(self, proc):
        self.assertEqual((0, "", ""), (proc.returncode, proc.stdout, proc.stderr))

    def assertBlocked(self, proc):
        self.assertEqual(2, proc.returncode)
        self.assertEqual("", proc.stdout)
        self.assertIn("BLOCKED", proc.stderr)

    # hard curfew: unchanged behaviour

    def test_blocked_inside_the_hard_window(self):
        for now in ("2026-11-05 23:00", "2026-11-05 23:59", "2026-11-06 03:00", "2026-11-06 07:59"):
            with self.subTest(now=now):
                self.assertBlocked(self.run_curfew(now))

    def test_block_message_names_the_window_and_the_switch(self):
        stderr = self.run_curfew("2026-11-05 23:30").stderr
        self.assertIn("curfew 23:00-8:00", stderr)
        self.assertIn("CURFEW=off", stderr)

    def test_allowed_outside_both_windows(self):
        for now in ("2026-11-06 08:00", "2026-11-06 12:00", "2026-11-05 21:59"):
            with self.subTest(now=now):
                self.assertSilent(self.run_curfew(now))
        self.assertFalse(self.state.exists())

    def test_window_without_midnight_crossing(self):
        env = {"CURFEW_START": "1", "CURFEW_END": "5"}
        self.assertBlocked(self.run_curfew("2026-11-05 01:00", **env))
        self.assertBlocked(self.run_curfew("2026-11-05 04:59", **env))
        self.assertSilent(self.run_curfew("2026-11-05 05:00", **env))

    def test_start_equal_to_end_never_blocks_or_warns(self):
        for now in ("2026-11-05 22:30", "2026-11-05 23:30", "2026-11-05 12:00"):
            with self.subTest(now=now):
                self.assertSilent(self.run_curfew(now, CURFEW_START="23", CURFEW_END="23"))
        self.assertFalse(self.state.exists())

    def test_off_allows_everything_and_leaves_state_alone(self):
        self.assertSilent(self.run_curfew("2026-11-05 23:30", toggles="CURFEW=off\n"))
        self.assertSilent(self.run_curfew("2026-11-05 22:30"))
        self.assertFalse(self.state.exists())

    def test_a_bad_start_or_end_allows_instead_of_blocking(self):
        for name, value in (("CURFEW_START", "22\r"), ("CURFEW_START", "10pm"), ("CURFEW_START", "24"),
                            ("CURFEW_END", "8\r"), ("CURFEW_END", "x"), ("CURFEW_END", "99")):
            with self.subTest(name=name, value=value):
                for now in ("2026-11-05 21:30", "2026-11-05 22:30", "2026-11-05 23:30", "2026-11-06 03:00"):
                    self.assertSilent(self.run_curfew(now, **{name: value}))
        self.assertFalse(self.state.exists())

    def test_a_crlf_in_the_env_file_allows_instead_of_blocking(self):
        self.assertSilent(self.run_curfew("2026-11-05 23:30", toggles="CURFEW_START=22\r\n"))

    # soft curfew: marks

    def test_marks_follow_the_quarter_hours(self):
        for hhmm, mark in (("22:00", 0), ("22:14", 0), ("22:15", 1), ("22:30", 2), ("22:44", 2), ("22:45", 3),
                           ("22:59", 3)):
            with self.subTest(hhmm=hhmm):
                self.state.unlink(missing_ok=True)
                out = self.warning(self.run_curfew("2026-11-05 " + hhmm))
                self.assertEqual("UserPromptSubmit", out["hookSpecificOutput"]["hookEventName"])
                self.assertIn("(%d of 4)" % (mark + 1), out["hookSpecificOutput"]["additionalContext"])
                self.assertEqual("2026-11-05 %d" % mark, self.state.read_text().strip())

    def test_each_warning_is_firmer_than_the_last(self):
        texts = []
        for hhmm in ("22:00", "22:15", "22:30", "22:45"):
            self.state.unlink(missing_ok=True)
            texts.append(self.warning(self.run_curfew("2026-11-05 " + hhmm)))
        self.assertEqual(4, len({t["systemMessage"] for t in texts}))
        self.assertIn("Plan where to stop", texts[0]["systemMessage"])
        self.assertIn("Don't start anything big", texts[1]["systemMessage"])
        self.assertIn("Start wrapping up", texts[2]["systemMessage"])
        self.assertIn("Wrap up now", texts[3]["systemMessage"])
        for t in texts:
            self.assertIn("23:00", t["systemMessage"])
            self.assertIn("23:00", t["hookSpecificOutput"]["additionalContext"])

    def test_the_last_warning_says_how_long_the_block_lasts(self):
        out = self.warning(self.run_curfew("2026-11-05 22:45", CURFEW_END="7"))
        self.assertIn("until 7:00", out["systemMessage"])
        self.assertIn("until 7:00", out["hookSpecificOutput"]["additionalContext"])

    def test_claude_only_steers_at_the_last_mark(self):
        contexts = []
        for hhmm in ("22:00", "22:15", "22:30", "22:45"):
            self.state.unlink(missing_ok=True)
            contexts.append(self.warning(self.run_curfew("2026-11-05 " + hhmm))["hookSpecificOutput"]["additionalContext"])
        for c in contexts[:3]:
            self.assertIn("Mention this in one short line", c)
            self.assertNotIn("wrapping up", c)
        self.assertIn("Steer the user toward wrapping up", contexts[3])

    def test_output_is_exactly_one_json_object(self):
        out = self.warning(self.run_curfew("2026-11-05 22:00"))
        self.assertEqual({"systemMessage", "hookSpecificOutput"}, set(out))
        self.assertEqual({"hookEventName", "additionalContext"}, set(out["hookSpecificOutput"]))

    # soft curfew: once per mark

    def test_a_mark_shows_once(self):
        self.warning(self.run_curfew("2026-11-05 22:00"))
        self.assertSilent(self.run_curfew("2026-11-05 22:05"))
        self.assertSilent(self.run_curfew("2026-11-05 22:14"))

    def test_a_later_mark_shows_after_an_earlier_one(self):
        self.warning(self.run_curfew("2026-11-05 22:00"))
        out = self.warning(self.run_curfew("2026-11-05 22:20"))
        self.assertIn("(2 of 4)", out["hookSpecificOutput"]["additionalContext"])
        self.assertEqual("2026-11-05 1", self.state.read_text().strip())

    def test_skipped_marks_are_never_shown(self):
        out = self.warning(self.run_curfew("2026-11-05 22:50"))
        self.assertIn("(4 of 4)", out["hookSpecificOutput"]["additionalContext"])
        self.assertSilent(self.run_curfew("2026-11-05 22:55"))

    def test_an_earlier_mark_does_not_show_after_a_later_one(self):
        self.state.write_text("2026-11-05 2\n")
        self.assertSilent(self.run_curfew("2026-11-05 22:16"))

    def test_the_next_day_shows_again(self):
        self.state.write_text("2026-11-04 3\n")
        self.warning(self.run_curfew("2026-11-05 22:00"))
        self.assertEqual("2026-11-05 0", self.state.read_text().strip())

    def test_a_bad_state_file_counts_as_nothing_shown(self):
        for junk in ("", "\n", "junk", "2026-11-05", "2026-11-05 x", "2026-11-05 7", "2026-11-05 -1",
                     "2026-11-05 1 extra"):
            with self.subTest(junk=junk):
                self.state.write_text(junk, encoding="utf-8")
                self.warning(self.run_curfew("2026-11-05 22:30"))
                self.assertEqual("2026-11-05 2", self.state.read_text().strip())

    def test_an_unwritable_state_file_still_warns_and_never_blocks(self):
        self.state.mkdir()
        out = self.warning(self.run_curfew("2026-11-05 22:30"))
        self.assertIn("(3 of 4)", out["hookSpecificOutput"]["additionalContext"])

    # soft curfew: where the hour sits

    def test_soft_hour_follows_curfew_start(self):
        self.warning(self.run_curfew("2026-11-05 04:00", CURFEW_START="5", CURFEW_END="9"))
        self.state.unlink(missing_ok=True)
        self.assertSilent(self.run_curfew("2026-11-05 22:00", CURFEW_START="5", CURFEW_END="9"))

    def test_start_at_midnight_warns_in_the_hour_before(self):
        out = self.warning(self.run_curfew("2026-11-05 23:15", CURFEW_START="0", CURFEW_END="8"))
        self.assertIn("(2 of 4)", out["hookSpecificOutput"]["additionalContext"])
        self.assertIn("0:00", out["systemMessage"])
        self.assertEqual("2026-11-05 1", self.state.read_text().strip())
        self.assertBlocked(self.run_curfew("2026-11-06 00:00", CURFEW_START="0", CURFEW_END="8"))

    # clock

    def test_curfew_tz_reaches_the_date_call(self):
        bin_dir = self.tmp / "bin"
        bin_dir.mkdir()
        stub = bin_dir / "date"
        stub.write_text('#!/bin/bash\nif [ "$TZ" = "Test/Zone" ]; then echo "2026-11-05 22:30"; '
                        'else echo "2026-11-05 12:00"; fi\n')
        stub.chmod(0o755)
        path = "%s:%s" % (bin_dir, os.environ["PATH"])
        out = self.warning(self.run_curfew(None, path=path, CURFEW_TZ="Test/Zone"))
        self.assertIn("(3 of 4)", out["hookSpecificOutput"]["additionalContext"])
        self.state.unlink(missing_ok=True)
        self.assertSilent(self.run_curfew(None, path=path))


class WiringTests(unittest.TestCase):
    COMMAND = '"${CLAUDE_PROJECT_DIR}/scripts/curfew/curfew.sh"'

    def test_user_prompt_submit_runs_it_once(self):
        hooks = json.loads((ROOT / ".claude" / "settings.json").read_text(encoding="utf-8"))["hooks"]
        rows = [(event, group, hook) for event, groups in hooks.items() for group in groups
                for hook in group["hooks"] if hook.get("command") == self.COMMAND]
        self.assertEqual(["UserPromptSubmit"], [e for e, _, _ in rows])
        _, group, hook = rows[0]
        self.assertEqual("command", hook["type"])
        self.assertIn(group.get("matcher", ""), ("", "*"))

    def test_curfew_sh_is_tracked_executable(self):
        out = subprocess.run(["git", "-C", str(ROOT), "ls-files", "-s", "scripts/curfew/curfew.sh"],
                             capture_output=True, text=True, check=True).stdout
        self.assertTrue(out.startswith("100755"), out)

    def test_env_and_state_files_are_ignored(self):
        for name in (".claude/curfew.env", ".claude/curfew.state"):
            with self.subTest(name=name):
                self.assertEqual(0, subprocess.run(["git", "-C", str(ROOT), "check-ignore", "-q", name]).returncode)


if __name__ == "__main__":
    unittest.main()
