# Changelog

A record of all versioned releases. For planned work and known issues, see @docs/BACKLOG.md.

---

<!-- This file is maintained by the Product Owner agent.
     New sections are prepended after each merged PR in the format:

## [X.Y.Z] — YYYY-MM-DD (PR #N merged)

### Added / Changed / Fixed
- ...
-->

## [0.14.1] — 2026-10-17 (PR #91 merged)

### Changed
- [wip] (PR #91) GH-78 (WU1/8): tests now build `ViewModelImpl`, `TuiView` and `GameSession` only through `testViewModel`/`testTuiView`/`testGameSession` builders with fake defaults, and `FakeViewModelUi.drive` times out after 10s instead of hanging. Test-only - no production change; WU2 moves all wiring into one composition root.

## [0.14.0] — 2026-10-17 (PR #89 merged)

### Added
- [user] (PR #89) GH-83 (WU4/4): Mordie, a computer opponent, races you to the shortest solution of the board you started with. A magenta `Mordie:` line under `Moves:` greets you, reports how far its search has got every 20 shifts (skipped when nothing has changed), and announces when it has found the shortest solution. The solved title then says how you did: you beat it, matched it, lost to it, or (hand-edited saves only) "Are you a machine?". Saves keep Mordie's progress; older saves still load. This completes 0.13.1–0.13.4.

## [0.13.4] — 2026-10-17 (PR #88 merged)

### Added
- [wip] (PR #88) GH-83 (WU3/4): Mordie wired into the game - one search per session that survives mode switches and stops on exit; it starts on new game, Repair and startup, resumes from or restores a save, and sits out a loaded board that already has moves. Saves store its state; new `mordie_solution_found` event and `board_solved` gains `mordie_result`/`mordie_moves`. Nothing on screen yet - the TUI lands in WU4.

## [0.13.3] — 2026-10-16 (PR #87 merged)

### Added
- [wip] (PR #87) GH-83 (WU2/4): Mordie's opponent state machine (shift-driven line: opening, progress every 20 shifts, "hard" from 60, found revealed at the next 5-shift check; live-state outcome incl. the "Are you a machine?" case), its text with singular "1 move", and the save format - lines 4-5 store the original arrangement and `<N> found|searching`; older saves still load. Not wired into the game yet - that lands in WU3.

## [0.13.2] — 2026-10-16 (PR #86 merged)

### Added
- [wip] (PR #86) GH-83 (WU1/4): Mordie's search engine - an admissible move-metric heuristic, a steppable IDA* solver (optimal against a full 3x3 BFS; O(depth) memory, resumable above a saved N) and a throttled background search (~half a core, cancellable). Not wired into the game yet - Mordie's state, text and save format land in WU2.

## [0.13.1] — 2026-10-16 (PR #84 merged)

### Changed
- [meta] (PR #84) GH-82: credited Loopover and its creator Cary Huang in README; described Mordovorot as "a terminal version of Loopover" in README, AGENTS.md, PRODUCT_SPEC.md, and skill_router.toml.

## [0.13.0] — 2026-10-15 (PR #80 merged)

### Removed
- [user] (PR #80) GH-79: console mode removed - the game only runs as a terminal UI. A launch with no terminal prints "No interactive terminal found - Mordovorot only runs as a terminal UI." and exits; --console prints a notice and starts the TUI; the TUI's Console button and F8 are gone; a failed mode switch stays in the current mode. Saves are unaffected.

## [0.12.0] — 2026-10-15 (PR #77 merged)

### Added
- [user] (PR #77) GH-74: loading a save that can't be solved (3x3/5x5) now offers repair (two tiles swapped, save rewritten), a new game, or exit - console prompt and a TUI "Can't be solved" dialog; new games are always solvable (since 0.11.1).

## [0.11.1] — 2026-10-15 (PR #76 merged)

### Fixed
- [wip] (PR #76) GH-74 (WU1/2): new games on 3x3/5x5 boards are always solvable (parity-fixed shuffle); saves that can't be solved are marked `unsolvable` in every save listing, and `load` accepts the marked form. Loading such a save still restores it as-is - the repair / new game / exit choice lands in WU2.

## [0.11.0] — 2026-10-15

### Added
- [user] (PR #73) GH-67: console `help` command lists all commands or shows one command's usage; `help` hint printed at console start; specific error messages with usage lines for rejected commands.

## [0.10.0] — 2026-10-15

### Added
- [user] (PR #72) GH-66: console shows solved-state message and final board; congratulations prompt and play-again option follow. Board state is reset between games.

## [0.9.2] — 2026-10-14

### Changed
- [app] (PR #71) GH-64: extract shared runModal dialog-loop helper in TuiView; Save/Load/Exit/Size dialogs route Escape/Cancel/end-of-input through it so dialog_cancelled is tracked exactly once. No user-visible behaviour change.

## [0.9.1] — 2026-10-14

### Changed
- [app] (PR #70) GH-65: add catchingNonControl exception-catch helper; route five non-control catch sites through it for consistency. No user-visible behaviour change.

## [0.9.0] — 2026-10-14

### Added
- [user] (PR #69) GH-62: move counter — a move is a run of consecutive shifts on one row/column; shown as `Moves: N` (console: after the board; TUI: under the title), kept across mode switches, saved as the save file's third line (older saves load as 0), and a `board_solved` analytics event.
- [meta] (PR #61) GH-60: add `bookmarks:` frontmatter to GH notes; align README test comments; run notes index check in CI
- [wip] HAB-278 WU22: port YAB onboarding remote/README/CHANGELOG handling and skill_router fixes; session start reads [providers].pm (PR #59)
- [meta] (PR #58) HAB-278 WU11: README refresh — installDist launcher for the TUIs, --mouse/--keyboard/--console/--size flags, onboarding test command, /onboard prerequisites (python3 vs 3.11+), launch-from-root and config-push hint.
- [meta] (PR #57) HAB-278 WU10: ran /onboard — filled skill_router.toml [project]/pm=github, TECH_STACK/CODE_STYLE/CONSTRAINTS/MODEL_TIERS docs, removed unused .mcp.json, synced build.gradle.kts version to 0.8.1.

## [0.8.1] — 2026-10-05 (PR #56 merged)

### Added
- [wip] HAB-278 (WU9, Habit Loop ticket): port the `/onboard` skill and onboarding gate from yuriys-agentic-boyz (`scripts/onboard/`, `skills/configure/onboard/`, committed `.claude/settings.json` hooks, empty `[project]` config shape) and delete `setup.sh`. Until `/onboard` runs (WU10) the gate keeps the clone in onboarding mode. CI runs the onboard test suite via `tests.yml` (template-state tests skip once the sentinel is gone).

## [0.8.0] — 2026-09-16 (PR #55 merged)

### Added
- [user] GH-44 (WU5/5): load auto-resizes to match a save's size instead of rejecting a mismatch — `BoardModel.restoreState` now derives `squareSide` from the restored state's own length, so loading a differently-sized save resizes the board and restores it, both at startup and via the mid-game `load`/Load dialog. This completes GH-44: the board is adjustable 3x3-5x5 (default 4x4) via `--size=N`, the startup prompt, `size <N>`/`[ New ]` mid-session; save-listing surfaces show each save's size; and a mismatched load resizes rather than rejects.

## [0.7.8] — 2026-09-16 (PR #54 merged)

### Added
- [wip] GH-44 (WU4/5): save size visibility — every save-listing surface (startup restore prompts, the console `load` not-found message, the TUI Load dialog) now shows each save's board size alongside its name, e.g. `foo (5x5)`, so a mismatch is visible before picking a save. `ViewModel.listSaves()` returns `SaveInfo` (name + nullable size) instead of bare names. A size-mismatched load still gets rejected rather than auto-resized - that lands in WU5, the final WU.

## [0.7.7] — 2026-09-16 (PR #53 merged)

### Added
- [wip] GH-44 (WU3/5): `size <N>` console command and the TUI `[ New ]` toolbar button/F9 shortcut - both start a fresh game at a chosen size (3-5) mid-session, opening the same size picker the startup prompt uses. Keyboard-mode arrow cursor now resets when the board's side changes mid-session. Still no save-size visibility in the restore/Load prompts, and a size-mismatched load still gets rejected rather than auto-resized - those land in WU4/WU5.

## [0.7.6] — 2026-09-15 (PR #52 merged)

### Added
- [wip] GH-44 (WU2/4): startup size prompt — `ViewModelImpl.restoreOnStartup` raises `UiRequest.ChooseBoardSize` once it's established nothing was restored (no saves, declined, blank/EOF, failed load); console answers with a text prompt, the TUI with a new `Dialog.Kind.SIZE` picker (buttons-only, like `EXIT`). `--size=N` at launch now bypasses the restore prompt entirely too, not just the size prompt - the flag is the answer no matter what saves exist. Still no `size <N>`/`[ New ]` mid-session entry points - those land in WU3.

## [0.7.5] — 2026-09-15 (PR #51 merged)

### Added
- [wip] GH-44 (WU1/3): board-size domain layer — `board_model.BoardSize` (valid range 3-5, default 4), `BoardModel.newGame(side)` (the only way the board's side changes; `resetGame()` keeps reshuffling at the current size), and a `--size=N` launch flag (`Main.kt`'s `resolveBoardSize`) that's tracked on `app_launched.board_size`. `BoardModel.SQUARE_SIDE` renamed to `squareSide` since it's no longer constructor-fixed. `ViewModel.newGame(size, trigger)` delegates to the board and tracks `new_game_size_selected`. No player-visible entry point yet — the startup size prompt (WU2) and `size <N>`/`[ New ]` surfaces (WU3) land next.

## [0.7.4] — 2026-09-15 (PR #47 merged)

### Changed
- [app] GH-42 (WU4/4): mechanical rename closing the Presenter→ViewModel redesign: `presenter.Presenter`/`presenter.PresenterImpl` → `viewmodel.ViewModel`/`viewmodel.ViewModelImpl`, package rename `presenter` → `viewmodel`, and all references (classes, variables, KDoc, test doubles, test files, docs: AGENTS.md, README.md, docs/ARCHITECTURE.md) to match. Also replaced `MVP (Model-View-Presenter)` with `MVVM (Model-View-ViewModel)` in documentation. No user-visible behaviour change.

## [0.7.3] — 2026-09-15 (PR #46 merged)

### Changed
- [wip] GH-42 (WU3/4): collapse `Presenter`/`ConsolePresenter`/`TuiPresenter` into one `Presenter`, and `BasePresenter`/`ConsolePresenterImpl`/`TuiPresenterImpl` into one `PresenterImpl` — it holds no `View` reference at all now. The console session loop (`displayBoard`/`processCommand`/the solved check) moved from the old `ConsolePresenterImpl.play()` into `ViewImpl.play()`, alongside its own `UiRequest`-draining handler coroutine, matching the shape `TuiView.play()` already had. No user-visible behaviour change.

## [0.7.2] — 2026-09-15 (PR #45 merged)

### Changed
- [wip] GH-42 (WU2/4): `UiRequest`/`ask()` request-response channel — `Presenter` no longer calls `showMessage`/`confirmRestore`/`chooseSaveToRestore`/`confirmSaveBeforeExit`/`promptSaveName` directly on its `View`; it raises a `UiRequest<R>` on a `Channel.RENDEZVOUS` (`Presenter.uiRequests`) and suspends until the View answers. `ViewImpl`/`TuiView` each gain a request-handler coroutine draining that channel alongside their own event loop. `saveGame`/`loadGame`/`exitGame`/`offerStartupRestore` gain a `CancellationException`-before-catch-all guard so a cancelled coroutine isn't misread as a failed save/load. No user-visible behaviour change.

## [0.7.1] — 2026-09-15 (PR #43 merged)

### Changed
- [wip] GH-42 (WU1/4): coroutines dependency + suspend propagation, no design change — adds `kotlinx-coroutines-core` (the project's first third-party runtime dependency) and `kotlinx-coroutines-test`; `main()` wraps in `runBlocking`; `GameSession.run()`, `View`'s prompt methods, and `Presenter.saveGame()`/`loadGame()`/`exitGame()` become `suspend`, ahead of WU2's request channel. `BasePresenter` still holds its `View` reference at this point. No user-visible behaviour change.

## [0.7.0] — 2026-09-15 (PR #40 merged)

### Added
- [app] GH-30 (WU4/4): keyboard mode's F7/F8 function keys request a runtime switch to mouse/console mode, mirroring the mouse toolbar's `[Mouse F7]`/`[Console F8]` row (added in WU3) and reaching the same injected `ModeSwitcher`. Both stay live even when solved, matching F5/F6/Escape. `TerminalInputParser` decodes `ESC[19~` as the new `FunctionKey(8)`. This completes GH-30: from console, mouse, or keyboard mode, the player can switch to any other mode mid-game — via console `mouse`/`keyboard` commands (WU2), the mouse toolbar's mode row (WU3), or keyboard F7/F8 (WU4) — without losing the board.

## [0.6.3] — 2026-09-15 (PR #38 merged)

### Changed
- [wip] GH-30 (WU3/4): TUI toolbar mode buttons — mouse mode's toolbar gains `[ Keyboard ]`/`[ Console ]` buttons on a row below `[ Save ] [ Load ] [ Exit ]`, switching mode without losing the current game (the TUI counterpart of WU2's console `mouse`/`keyboard` commands). Reached via an injected `ModeSwitcher`, mirroring `ViewImpl`'s WU2 wiring. `BoardLayout`'s Save/Load/Exit button positions now look themselves up by `HitTarget` instead of a fixed index.

## [0.6.2] — 2026-09-15 (PR #37 merged)

### Changed
- [wip] GH-30 (WU2/4): console `mouse`/`keyboard` commands — lets the user switch from the line-based console UI to the mouse-driven or keyboard-driven TUI mid-game without losing the board, via `ViewImpl`'s new `mouse`/`keyboard` commands reaching an injected `ModeSwitcher`. If there's no interactive terminal to switch into, the command is rejected with an on-screen message and play continues in console mode. Also hardens `GameSession`'s mode-switch loop: a rebuild that fails outright (e.g. a missing `stty`) now falls back to console instead of crashing and losing the in-progress game.

## [0.6.1] — 2026-09-14 (PR #36 merged)

### Changed
- [wip] GH-30 (WU1/4): session loop + mode-switch plumbing — extracts `GameSession` out of `main()` so a running session can rebuild its View/presenter/analytics stack around a different `InputMode` mid-session instead of the process just ending; adds `ModeSwitchRequestedException`/`ModeSwitcher`/`ModeSwitcherImpl` (the single place that checks for an interactive terminal and tracks `input_mode_switched`), a shared `SessionControlException` marker base, and renames `LaunchMode` to `InputMode`. Not yet reachable from any UI — no `mouse`/`keyboard` console command (WU2), no TUI toolbar buttons (WU3), no F7/F8 shortcuts (WU4) call `ModeSwitcher.switchTo` yet.

## [0.6.0] — 2026-09-14 (PR #34 merged)

### Added
- [wip] GH-18 (WU1/5): decode arrow/Tab/BackTab/function keys in the TUI input parser — extends `TerminalInputParser` to recognize ESC sequences for arrow keys (`→`/`←`/`↑`/`↓`), Tab/BackTab, and function keys (F1–F12), converting them to `TerminalEvent.KeyPress` — not yet wired to the UI, still unreachable until WU2/3 routes these events through the board screen.
- [wip] GH-18 (WU2/5): cursor ring + dialog focus geometry and rendering in the TUI view layer
- [wip] GH-18 (WU3/5): TuiInput strategy extraction in the TUI view layer (no behaviour change)
- [wip] GH-18 (WU4/5): `--keyboard` launch mode + board navigation — `KeyboardInput` driver wires arrow keys to `ArrowCursor`, Enter/Space activates shifts, F5/F6/Esc open dialogs; `LaunchMode` gains `KEYBOARD` with `--keyboard`/`--mouse` flags, fixes missing `input_method` property on view events, dialog focus driven by Tab/arrow keys
- [app] GH-18 (WU5/5): keyboard dialog navigation test coverage, docs, and audit fixes — completes the keyboard-only TUI: Tab/arrow-key focus cycling and typing work across Save/Load/Exit dialogs and the startup restore prompt, with the yellow cursor and light-blue toolbar shortcuts (F5 Save, F6 Load, Esc Exit) documented in `docs/PRODUCT_SPEC.md` as Feature 7. This completes GH-18: `--keyboard` is now a fully playable alternative to the default mouse-driven TUI, with `--console` and a non-interactive terminal both still falling back to the line-based console UI.

## [0.5.1] — 2026-09-11 (PR #27 merged)

### Changed
- [app] GH-23: splits `Presenter` into a shared core plus `ConsolePresenter`/`TuiPresenter` sub-interfaces, with `BasePresenter` (shared domain-mutation flows) and `ConsolePresenterImpl`/`TuiPresenterImpl` replacing the single `PresenterImpl` — `ViewImpl` now depends only on `ConsolePresenter`, `TuiView` only on `TuiPresenter`. Test doubles and presenter test files restructured to match (`FakePresenter` split into `FakeConsolePresenter`/`FakeTuiPresenter`; `PresenterImpl*Test.kt` renamed/split per production class). Pure internal refactor - no user-visible behavior change.

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
