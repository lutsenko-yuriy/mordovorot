# Changelog

A record of all versioned releases. For planned work and known issues, see @docs/BACKLOG.md.

---

<!-- This file is maintained by the Product Owner agent.
     New sections are prepended after each merged PR in the format:

## [X.Y.Z] — YYYY-MM-DD (PR #N merged)

### Added / Changed / Fixed
- ...
-->

## [Unreleased]

## [0.5.0] — 2026-09-11 (PR #25 merged)

### Added
- [wip] GH-3 (WU1/5): launch-mode resolution (`--console` opt-in, default mouse TUI once it exists, automatic fallback to console with no interactive terminal) and its `app_launched`/`input_method` analytics plumbing — no user-visible behavior change yet, still runs the console UI either way until WU3.
- [wip] GH-3 (WU2/5): terminal layer (`Terminal`, `AnsiTerminal`, `TerminalEvent`, `TerminalInputParser`, `FakeTerminal`) — decodes raw terminal bytes (SGR-1006 and legacy X10 mouse reports, single-byte keys) into events; not wired into the app yet, still unreachable until WU3 builds the board screen on top of it.
- [wip] GH-3 (WU3/5): board screen (`BoardLayout`, `HitTarget`, `ScreenState`, `ScreenRenderer`, `TuiView`) — a mouse-playable 4x4 board with the 16 shift arrows, now wired as the default `LaunchMode.MOUSE` UI. Save/Load/Exit toolbar buttons render but are inert (no dialogs yet), and there's no Congratulations screen — both land in WU4/WU5.
- [wip] GH-3 (WU4/5): Save/Load/Exit dialogs and the startup restore prompt (`Dialog`, `DialogLayout`, `TuiView`'s modal loops) — the mouse-driven TUI now has full feature parity with the console UI (save, load, exit with save-before-quitting, restoring a save at launch), plus their `dialog_cancelled`/`screen_*_dialog` analytics. Still no Congratulations screen — a solved board disables the arrows with nothing to hand off to yet; that lands in WU5.
- [app] GH-3 (WU5/5): Congratulations screen — once the board is solved, the title switches to `Congratulations ✓`, the shift arrows dim and stop responding to clicks, and the toolbar (Save/Load/Exit) stays fully live, with `screen_congratulations` firing once on the transition. Loading a different, unsolved save from this screen re-enables the arrows and title immediately. This completes GH-3: the mouse-driven TUI is now the default launch experience, with `--console` opting back into the line-based console UI.

## [0.4.0] — 2026-09-10 (PR #15 merged)

### Added
- [app] GH-12: `exit`/`quit` command, with a save-before-quitting prompt (`save_command_used`/`exit_command_used` analytics) that re-prompts on an unusable name and aborts the quit — rather than losing the session — if the save attempt itself fails.

## [0.3.0] — 2026-09-10 (PR #14 merged)

### Added
- [app] GH-6 (WU1/4): line-based console input reader with EOF termination for play mode.
- [app] GH-6 (WU2/4): storage package (SaveRepository/SavedBoard/FileSaveRepository/SaveFileFormatException) + BoardModel.restoreState — persistence layer, not yet wired to the UI.
- [app] GH-6 (WU3/4): `save <file-name>` / `load <file-name>` console commands, wired view → presenter → storage, with `save_command_used`/`load_command_used` analytics events.
- [app] GH-6 (WU4/4): startup restore prompt — on launch, offers to resume an existing save (a yes/no question for exactly one save, a name prompt for two or more, re-prompting on an unrecognized name), with `startup_restore_prompt_shown`/`startup_restore_decision` analytics events.

## [0.2.0] — 2026-09-10 (PR #11 merged)

### Changed
- [app] GH-10: 1-based tile values and row/column indices

## [0.1.1] — 2026-09-10 (PR #5 merged)

### Added
- [test] GH-4: automated test coverage for `board_model` and `presenter` (37 tests), independent of any view/UI implementation.
- [ci] GH-4: GitHub Actions workflow running `./gradlew build` on push/PR.

### Fixed
- [app] GH-4: fix an off-by-one in the board shift bounds check (`shiftLeft`/`shiftRight`/`shiftUp`/`shiftDown`) that let an out-of-range index through and crashed instead of raising a clear error.
- [app] GH-4: fix `./gradlew run` not receiving keyboard input (the `application` plugin doesn't wire stdin by default).

### Changed
- [meta] GH-4: adopt Gradle as the project's build tool; replace the hand-rolled board shuffle with Kotlin's stdlib `IntArray.shuffle()`.

## [0.1.0] — 2026-09-10 (PR #1 merged)

### Added
- [meta] GH-2: adopt the yuriys-agentic-boyz multi-skill AI workflow (`.claude/commands`, `skills/`, `docs/`, `scripts/skill_router`, `styles/`), adapted to this project's Kotlin stack and GitHub Issues, with a calibrated model-tier mapping in `docs/MODEL_TIERS.md`.
