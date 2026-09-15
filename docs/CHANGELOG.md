# Changelog

A record of all versioned releases. For planned work and known issues, see @docs/BACKLOG.md.

---

<!-- This file is maintained by the Product Owner agent.
     New sections are prepended after each merged PR in the format:

## [X.Y.Z] — YYYY-MM-DD (PR #N merged)

### Added / Changed / Fixed
- ...
-->

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
