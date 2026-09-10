# Changelog

A record of all versioned releases. For planned work and known issues, see @docs/BACKLOG.md.

---

<!-- This file is maintained by the Product Owner agent.
     New sections are prepended after each merged PR in the format:

## [X.Y.Z] — YYYY-MM-DD (PR #N merged)

### Added / Changed / Fixed
- ...
-->

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
