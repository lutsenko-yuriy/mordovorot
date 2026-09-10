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

### Added
- [wip] GH-6 (WU1/4): line-based console input reader with EOF termination for play mode.
- [wip] GH-6 (WU2/4): storage package (SaveRepository/SavedBoard/FileSaveRepository/SaveFileFormatException) + BoardModel.restoreState — persistence layer, not yet wired to the UI.

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
