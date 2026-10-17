# Architecture

## Related docs

- `docs/TECH_STACK.md` — languages, frameworks, tooling
- `docs/CODE_STYLE.md` — style rules
- `docs/CONSTRAINTS.md` — standing constraints that shape design trade-offs

## Overview

MVVM (Model-View-ViewModel): board_model + viewmodel + view packages, plus the `mordie`
opponent package (GH-83).

## Directory structure

```
src/main/kotlin/
├── Main.kt              # Entry point — resolves the starting InputMode (or aborts with no
│                          # terminal, GH-79), builds the session-lifetime objects (BoardImpl,
│                          # FileSaveRepository, Mordie) and runs buildGameSession (GH-3, GH-30)
├── AppWiring.kt         # The composition root (GH-78): buildGameSession / defaultView /
│                          # buildViewModel / buildTuiView - the only production code that
│                          # constructs collaborators (MordieHooks, ScreenRenderer,
│                          # ModeSwitcherImpl, TuiInputs). Test builders call the same functions
├── InputMode.kt         # MOUSE / KEYBOARD — parses the starting mode from `--keyboard`/
│                          # `--mouse`; also the type GameSession/ModeSwitcher pass around
│                          # mid-session (GH-3, GH-18, renamed from LaunchMode for GH-30)
├── GameSession.kt       # Session loop extracted out of Main: builds a fresh View per mode
│                          # through its buildView(mode, startupRestoreDone) - wired by
│                          # buildGameSession so every rebuild shares one board, saves and
│                          # CurrentSaveName (GH-99), KeyboardSelection and GameShiftKeys
│                          # (GH-101) -
│                          # runs play(), and re-enters on ModeSwitchRequestedException. A
│                          # rebuild's play() throwing anything else falls back to the previous
│                          # mode (GH-79) instead of crashing and losing the in-progress game -
│                          # a first-session failure, or a failed fallback, still throws (GH-30).
│                          # Also holds the one `Mordie` (GH-83) and stops its search in
│                          # run()'s `finally`
├── analytics/
│   ├── AnalyticsService.kt             # Analytics abstraction — track(event, properties)
│   ├── NoopAnalyticsService.kt         # Default implementation; no SDK wired up yet
│   └── InputMethodAnalyticsService.kt  # Decorator adding `input_method` to every forwarded
│                                         # event, without touching ViewModelImpl's call sites (GH-3)
├── board_model/
│   ├── BoardModel.kt    # Board interface (domain contract)
│   ├── BoardSize.kt     # The valid board side range (MIN 3 / MAX 5 / DEFAULT 4) and its
│   │                      # validation - the one place the range lives (GH-44)
│   ├── Solvability.kt   # Which boards shifts can solve (permutation parity on odd sides)
│   │                      # and the two-tile repair - the one place the rule lives (GH-74)
│   └── BoardImpl.kt     # IntArray-backed board state + shift/reset/newGame/isCorrect logic + move count.
│                          # `squareSide` is a read-only view over a private mutable field:
│                          # `newGame(side)` is the only thing that ever changes it (GH-44),
│                          # `resetGame()` reshuffles at the same size, always to a solvable board (GH-74)
├── mordie/              # GH-83: the computer opponent - pure domain, no project imports
│   ├── MoveMetricHeuristic.kt   # Admissible lower bound in the GH-62 move metric:
│   │                              # ceil(wrong-row tiles/n) + ceil(wrong-column tiles/n)
│   ├── IdaStarSolver.kt         # Steppable IDA* (explicit stack, O(depth) memory): ruledOut,
│   │                              # solutionLength; can resume above a saved N
│   ├── MordieSearch.kt          # MordieSearch / SearchProgress / MordieSearchFactory (+ NONE) -
│   │                              # the seam tests replace with FakeMordieSearchFactory
│   ├── BackgroundMordieSearch.kt # The real search: one throttled coroutine on
│   │                               # Dispatchers.Default, publishing a volatile snapshot
│   ├── Mordie.kt                # Opponent state: shift count, 5-shift checks, opening/resume
│   │                              # kind, found reveal, frozen outcome, save snapshot
│   └── MordieLine.kt            # MordieLine (sealed) / MordieOutcome / MordieResult /
│                                  # MordieSnapshot (what a save stores)
├── viewmodel/
│   ├── GameViewModel.kt      # The shared viewmodel interface (GH-42 WU3; Exit split out for
│   │                           # GH-41) — shift/newGame/save/load plus the read-only query
│   │                           # surface (listSaves/saveExists/currentSaveName/isSolved/
│   │                           # boardState/squareSide/moveCount) and restoreOnStartup
│   ├── GameViewModelImpl.kt  # Its one implementation (GH-42 WU3, collapsing BaseViewModel +
│   │                           # ConsoleViewModelImpl + TuiViewModelImpl). Keeps shifts and the
│   │                           # queries; delegates every other flow to a usecase/ class. All
│   │                           # collaborators are injected, with no defaults (GH-78). Holds no
│   │                           # `View` reference — see the "viewmodel" section below
│   ├── GameViewModelFactory.kt # buildGameViewModel — builds GameViewModelImpl and its use
│   │                             # cases; the only way a platform gets one (GH-41)
│   ├── UnsolvableSaveExit.kt # The platform hook for Exit on an unsolvable save, run after the
│   │                           # shared flow returns (GH-41); NONE offers no Exit
│   ├── ViewModel.kt          # The terminal's viewmodel: GameViewModel + exitGame() (GH-41)
│   ├── ViewModelImpl.kt      # Delegates to GameViewModelImpl, adds ExitGameUseCase; built
│   │                           # only by buildViewModel (GH-41)
│   ├── UiRequest.kt          # Sealed request/response type for the seven viewmodel→UI
│   │                           # interactions (ShowMessage/ConfirmRestore/ChooseSaveToRestore/
│   │                           # ConfirmSaveBeforeExit/PromptSaveName/ChooseBoardSize (GH-44)/
│   │                           # ResolveUnsolvableSave (GH-74) + UnsolvableSaveChoice),
│   │                           # sent on
│   │                           # GameViewModel.uiRequests (GH-42 WU2)
│   ├── UiRequestChannel.kt   # The rendezvous channel behind uiRequests: ask/showMessage. One
│   │                           # per rebuild, shared by GameViewModelImpl, every use case and
│   │                           # the terminal's Exit (GH-78, GH-41)
│   ├── CurrentSaveName.kt    # The session's current save name (`var name: String?`): set on
│   │                           # save/load/repair, cleared on new game, prefills the Save dialog.
│   │                           # One per session, made by buildGameSession, so it survives mode
│   │                           # switches; never persisted (GH-99)
│   ├── GameShiftKeys.kt      # ShiftKey {ARROW, HJKL} + GameShiftKeys: which keys made this
│   │                           # game's shifts, for board_solved.shift_keys. Reset on new game/
│   │                           # load/repair; one per session, made by buildGameSession (GH-101)
│   ├── SaveCatalog.kt        # Read-only SaveInfo views of the saves: infos (throws) /
│   │                           # listSaves / saveExists (swallow) / availableSavesMessage (GH-78)
│   ├── SessionControlException.kt # Sealed marker base for control-flow exceptions that unwind
│   │                                 # play() intentionally — lets a catch-all rethrow via this
│   │                                 # one type instead of naming each subtype (GH-30)
│   ├── CatchingNonControl.kt # catchingNonControl — GameSession's catch-all: rethrows
│   │                           # SessionControlException/CancellationException, routes any
│   │                           # other Exception to onError (GH-65)
│   ├── CatchingNonCancellation.kt # catchingNonCancellation — the use cases' catch-all: rethrows
│   │                                 # only CancellationException, since no session-control
│   │                                 # exception passes through shared flows (GH-41)
│   ├── ExitRequestedException.kt # Signals play() (or view.tui.TuiView's own loop, GH-3) to stop
│   ├── ModeSwitcher.kt        # ModeSwitcher / ModeSwitcherImpl — tracks
│   │                             # input_mode_switched and throws ModeSwitchRequestedException;
│   │                             # same-mode no-op (GH-30)
│   ├── ModeSwitchRequestedException.kt # Carries the requested InputMode; unwinds a running
│   │                                      # session's play() back to GameSession's loop (GH-30)
│   ├── MordieHooks.kt         # GameViewModelImpl's Mordie part: binds it on new game/load/repair/
│   │                             # startup (sits out a loaded board already played on), tracks
│   │                             # its analytics, words its line and ending (GH-83)
│   ├── MordieText.kt          # Every Mordie string (line bodies + the four endings), singular
│   │                             # `1 move` included (GH-83)
│   └── usecase/               # One class per viewmodel flow, delegated to by GameViewModelImpl
│                                 # (GH-78): NewGameUseCase, SaveGameUseCase, ExitGameUseCase
│                                 # (terminal-only, delegated to by ViewModelImpl, GH-41),
│                                 # LoadGameUseCase -> ResolveUnsolvableSaveUseCase (repair /
│                                 # new game / exit, GH-74; both return LoadResult, GH-41), StartupRestoreUseCase (restore or
│                                 # size prompt, then Mordie; takes `startupRestoreDone`, which
│                                 # GameSession seeds true after a mode switch (GH-30), and
│                                 # `sizeChosenAtLaunch`)
├── storage/
│   ├── SaveRepository.kt          # Persistence contract: list/exists/save/load
│   ├── SavedBoard.kt              # Data carrier: square side + tile arrangement (+ optional
│   │                                # SavedMordie, GH-83)
│   ├── SavedMordie.kt             # Original arrangement + Mordie's N + found flag (GH-83)
│   ├── FileSaveRepository.kt      # Plain-text file implementation (the only class
│   │                                touching the filesystem); rejects unsafe file names
│   └── SaveFileFormatException.kt # Thrown on a malformed save file
└── view/
    ├── View.kt          # View interface — play() (GH-79 removed the console-only members)
    └── tui/             # GH-3: the View implementation, the mouse/keyboard TUI
        ├── Terminal.kt / AnsiTerminal.kt   # Raw-mode terminal I/O (stty via ProcessBuilder,
        │                                     # xterm mouse-reporting escapes) - the one seam
        │                                     # touching the real terminal
        ├── TerminalEvent.kt / TerminalInputParser.kt # Byte-stream -> event decoding (SGR-1006
        │                                               # and legacy X10 mouse reports, keys,
        │                                               # arrow/Tab/BackTab/function-key
        │                                               # decoding for keyboard mode, incl.
        │                                               # F7 for the mode-switch shortcut)
        │                                               # (GH-18, GH-30)
        ├── BoardLayout.kt / HitTarget.kt   # Pure board geometry + click hit-testing, incl. the
        │                                     # toolbar's mode-switch buttons
        │                                     # (HitTarget.ToolbarMode) (GH-30) and the reserved
        │                                     # Mordie rows under `Moves:` (GH-83)
        ├── WordWrap.kt      # Shared word wrap for dialogs and the Mordie line (GH-83)
        ├── LineSelection.kt # Pure keyboard line selection (GH-101, replaced GH-18's ArrowRing):
        │                      # LineSelection(row, col, axis) - WASD select with wraparound,
        │                      # HJKL/arrows shift the active line or switch axis, clampedTo(side).
        │                      # Plus KeyboardSelection, its session-lifetime holder (made by
        │                      # buildGameSession, so it survives mode switches)
        ├── Dialog.kt / DialogLayout.kt     # One model + one geometry class for all four
        │                                     # modal dialogs (Save/Load/Exit/Size (GH-44, a
        │                                     # buttons-only kind, so DialogLayout/DialogFocus
        │                                     # need no per-kind branch for it)) and the
        │                                     # Load-shaped startup restore prompt (GH-3 WU4);
        │                                     # Dialog also carries keyboard focus state
        │                                     # (focusedButtonId, textFieldFocused) (GH-18)
        ├── DialogFocus.kt   # Pure focus ring over a dialog's controls (text field/list rows,
        │                      # then buttons) for Tab/arrow-key dialog navigation (GH-18)
        ├── ScreenState.kt / ScreenRenderer.kt # Pure ScreenState -> frame String rendering,
        │                                        # including the dialog overlay, keyboard mode's
        │                                        # selected-line highlight and side hints
        │                                        # (GH-101, arrows hidden), the controls hint, and
        │                                        # toolbar shortcut labels (GH-18)
        ├── TuiInput.kt      # Per-mode input strategy interface (prepare/onBoardEvent/
        │                      # onDialogEvent/decorateBoard/decorateDialog/onDialogOpened) +
        │                      # InputAction sum type, so TuiView is mode-agnostic. onDialogOpened
        │                      # is called at the start of every modal loop so a stateful TuiInput
        │                      # can reset per-session state at an explicit boundary, even when
        │                      # one dialog opens another directly with no board repaint in
        │                      # between (e.g. Exit -> Save) (GH-18)
        ├── MouseInput.kt    # TuiInput impl: today's click-driven behaviour, stateless. Used
        │                      # by both the default mouse mode and the explicit --mouse flag
        │                      # (GH-18)
        ├── KeyboardInput.kt # TuiInput impl for --keyboard mode (GH-18): the WASD/HJKL key map
        │                      # over the shared KeyboardSelection (GH-101) and DialogFocus state
        ├── dialog/          # One controller per dialog flow (GH-78), each over TuiScreen's
        │                      # runModal: SaveDialogController (toolbar Save + the exit flow's
        │                      # name prompt), ExitDialogController (owns the toolbar answer the
        │                      # viewModel's ConfirmSaveBeforeExit then consumes),
        │                      # LoadDialogController (toolbar Load + the Load-shaped dialog),
        │                      # RestoreDialogController (startup restore prompt over it),
        │                      # SizeDialogController (toolbar New/F9 + ChooseBoardSize),
        │                      # UnsolvableDialogController (GH-74's repair/new game/exit)
        ├── UiRequestRouter.kt # Answers each UiRequest with the dialog that owns it - an
        │                      # exhaustive `when`, so a new request needs an answer to compile
        ├── ToolbarRouter.kt # Runs the activated toolbar button: Save/Load/Exit/New and the
        │                      # mode switch (GH-78)
        ├── TuiScreen.kt     # What the TUI draws (GH-78): the last-drawn BoardLayout/DialogLayout,
        │                      # the parked status message, wasSolved, repaint, and runModal -
        │                      # the one blocking dialog loop (inline, calls
        │                      # TuiInput.onDialogOpened first)
        └── TuiView.kt   # Owns its own event loop: runs startup restore, the board loop and
                          # shifts, and hands toolbar targets to ToolbarRouter and UiRequests to
                          # UiRequestRouter; every dialog shares TuiScreen's blocking modal loop
                          # over Terminal (WU4, GH-64). State-driven solved screen (WU5): every
                          # repaint asks ViewModel.isSolved() fresh rather than tracking a phase
                          # flag, so the Congratulations title/dimmed arrows and a load back to an
                          # unsolved board both fall out of the same repaint path with no extra
                          # branching. Delegates input handling to an injected TuiInput
                          # (MouseInput or KeyboardInput, picked per mode in defaultView) so
                          # mouse and keyboard modes share one event loop (GH-18).

src/test/kotlin/
├── InputModeTest.kt       # InputMode resolution + app_launched tracking (GH-3, renamed
│                            # from LaunchModeTest for GH-30)
├── ConsoleRemovalScenarioTest.kt # GH-79 end to end: no-terminal abort, --console notice, no
│                            # console switch in the TUI, failed-switch fallback, old saves
├── MoveCounterScenarioTest.kt / UnsolvableSaveScenarioTest.kt # GH-62/GH-74 end to end
│                            # through the TUI over a real board and temp-dir saves
├── MordieScenarioTest.kt  # GH-83 end to end with a scripted FakeMordieSearchFactory
├── GameSessionTest.kt     # Session loop: startupRestoreDone seeding, multi-switch chains,
│                            # failed-switch fallback, Mordie stopped on exit (GH-30)
├── AppWiringTest.kt      # Composition root: each mode's switch key really switches, a mode
│                            # switch rebuilds around the session's one board and Mordie, which
│                            # analytics each collaborator gets (GH-78)
├── ModeSwitcherTest.kt    # input_mode_switched tracking, trigger pass-through, same-mode
│                            # no-op (GH-30)
├── analytics/            # NoopAnalyticsService, InputMethodAnalyticsService coverage
├── board_model/         # BoardImpl coverage: reset/shuffle, isCorrect, all four shifts, restoreState
├── mordie/                # Heuristic admissibility + solver optimality against a 3x3 BFS oracle,
│                            # background search, Mordie state machine (GH-83)
├── viewmodel/            # One test file per ViewModelImpl concern (GH-42 WU3, collapsing GH-23's
│                           # split): ViewModelDelegationTest / ViewModelSaveLoadTest /
│                           # ViewModelExitTest drive ViewModelImpl directly, no play()
│                           # loop involved; ViewModelStartupRestoreTest covers restoreOnStartup's
│                           # branch matrix plus the idempotence guard; ViewModelQueriesTest covers
│                           # the read-only query surface; ViewModelCancellationTest/
│                           # ViewModelOrderingTest pin the request-channel's CancellationException
│                           # guard and rendezvous ordering (GH-42 WU2); CatchingNonControlTest
│                           # pins the shared catch-all helper (GH-65)
├── storage/               # FileSaveRepository coverage
├── view/tui/              # TUI coverage (GH-3, WU2-WU5)
└── testing/              # testViewModel / testTuiView / testGameSession - the only way tests
                            # build these, calling AppWiring with fake defaults (GH-78) /
                            # NoopModeSwitcher /
                            # FakeBoardModel / FakeTerminal / FakeSaveRepository / RecordingAnalyticsService /
                            # FakeViewModel (recording double, collapsing GH-23's
                            # RecordingViewModel/FakeConsoleViewModel/FakeTuiViewModel split in
                            # GH-42 WU3) / FakeViewModelUi (scripted UiRequest responder, GH-42
                            # WU2) / RecordingModeSwitcher (GH-30) / FakeMordieSearch +
                            # FakeMordieSearchFactory (scripted progress, GH-83)
```

Gradle's standard source-set convention (`src/main/kotlin`, `src/test/kotlin`) is used —
see "Dependencies" below.

## Layers

Classic MVVM. `board_model` and `view` are each an interface + one implementation
(`view`'s is the TUI, `view.tui.TuiView`).
`viewmodel` (GH-42) is the shared `GameViewModel` interface + one `GameViewModelImpl`,
holding no reference to any `View` — it communicates with whichever `View` is driving it
through a request channel instead of a constructor-injected dependency. The terminal's
`ViewModel` adds Exit on top (GH-41), implemented by `ViewModelImpl`, which delegates the
rest to `GameViewModelImpl`. Callers should always depend on an interface, not an impl,
to keep layers swappable.

### board_model (Domain)
Core game state and rules: board array, shifting, reset, and the win check
(`isCorrect`). No dependency on `viewmodel` or `view`.

**Board size (GH-44):** the board stays square, with a side of 3–5 (default 4).
`BoardSize` holds that range and its validation — the single place it lives, per
this layer's "range validation lives in `board_model`, the view translates but
does not validate" rule (see the 1-based display dialect note under `view`).
`BoardImpl.newGame(side)` is the only operation that ever changes the side: it
validates first, so an out-of-range value throws with the current board left
untouched, then reallocates and shuffles. `resetGame()` keeps its older meaning —
reshuffle at the same size. Every shuffle is solvable (GH-74): `Solvability`
holds the parity rule, and `resetGame()` swaps tiles 0/1 when the shuffle lands
on an unsolvable board. Nothing resizes an *in-progress* board: every
size-setting surface starts a fresh game instead. The board instance itself is
still the one every rebuild shares across mode switches (GH-30) — the side mutates
in place rather than the instance being swapped, so that invariant is unaffected.

**Move count (GH-62):** `BoardImpl` owns the move rule — a move is a run of
consecutive shifts on one row or column. Each shift checks bounds first, then
counts a move only if its line differs from the last shift's. `resetGame`/`newGame`
zero the count; `restoreState(state, moveCount)` sets it and clears the last line,
so the first shift after a load always starts a new move. Because the count lives
on the board, it survives mode switches with no extra wiring.

### mordie (GH-83: the computer opponent)
Pure domain: plain `IntArray` arrangements, no dependency on `board_model`,
`storage`, `viewmodel` or `view` (`viewmodel` → `mordie`). `IdaStarSolver` searches the
game's original arrangement for its shortest solution in the GH-62 move metric, with
`MoveMetricHeuristic` as the admissible bound. It runs in slices, so
`BackgroundMordieSearch` can throttle it (about half of one core; the constants sit in
one place for the GH-41 mobile port), and memory stays O(depth). A search started from a
saved N begins at N + 1, so it never re-reports below it. `Mordie` turns the player's
shifts into the line state: a check every 5 shifts, a reveal of "found", and the outcome
frozen on solve, using Mordie's live state at that shift. Its `MordieSnapshot` mirrors
`storage.SavedMordie` on purpose, so `mordie` never imports `storage`; `MordieHooks` maps
between the two. `Main` builds one `Mordie` per session, so it survives mode switches;
`GameSession` stops it. `GameViewModelImpl` reaches it through `MordieHooks`, which
`buildGameViewModel` builds per rebuild so it tracks through that mode's decorated analytics.
Test builders default to an inert `Mordie(MordieSearchFactory.NONE)`, so unit tests never
leave real searches running.

### viewmodel (GH-42: ViewModel-style, no View reference)
**Status: landed (WU4/4 of the GH-42 redesign — final WU).** Everything described below is
real and in use; the `presenter` package/naming this superseded is gone.

Mediates between `view`, `board_model`, and `storage`, but never calls into `view`
directly. `GameViewModel` is one interface (shift/newGame/save/load plus the
query surface a View needs — `isSolved`/`boardState`/`squareSide`/`moveCount`/`listSaves`/
`saveExists`), and `GameViewModelImpl` is its one implementation. It keeps the shifts and
queries and delegates each other flow to a use case in `viewmodel.usecase` (GH-78), all
sharing one `UiRequestChannel`. Every collaborator is a constructor param with no
default: `buildGameViewModel` wires them. The terminal's `ViewModel` adds `exitGame()`
(GH-41): `buildViewModel` builds `ExitGameUseCase` on the same `UiRequestChannel` and wraps
the `GameViewModelImpl` in `ViewModelImpl`; tests build it through `testViewModel`'s fakes.

Five interactions that used to be blocking calls into `View` (`showMessage`,
`confirmRestore`, `chooseSaveToRestore`, `confirmSaveBeforeExit`, `promptSaveName`)
are now `viewmodel.UiRequest<R>` values sent on `ViewModel.uiRequests`, a
`Channel.RENDEZVOUS` the owning `View` drains in a sibling coroutine and answers via
`UiRequest.respond`. `TuiView` hands each to `UiRequestRouter`, which routes it to the
dialog controller that answers it (`view.tui.dialog`, GH-78); `ShowMessage` parks the text on
`TuiScreen`. Rendezvous delivery
(the channel has zero buffer, and `UiRequestChannel.ask()` doesn't return until the View has actually
handled the request) is what keeps message/prompt ordering identical to the old
blocking-call behavior. The one invariant that keeps this deadlock-free: a View's
request handler must never call a request-raising `ViewModel` method
(`saveGame`/`loadGame`/`exitGame`/`restoreOnStartup`) from inside
itself — only the plain query methods are safe there. All five are `suspend`; a cancelled coroutine resumes them with `CancellationException`, which
each use case's catch-all (`catchingNonCancellation`) rethrows rather than swallows (ordinary
structured-concurrency hygiene). No session-control exception passes through a use case
(GH-41), so that catch-all doesn't need to know about one.

**Mordie wiring (GH-83).** `ViewModel.mordieLine()` (the text after `Mordie:`, or `null`)
and `mordieEnding()` (the solved title, or `null` → `Congratulations ✓`) are plain
queries. `shift()` calls `mordie.onShift()` and then, on solve, `mordie.onSolved()`;
`newGame` starts Mordie fresh; `saveGame` passes `mordie.snapshot()`. A
successful load binds Mordie through one rule: a solved save with Mordie data gets that
outcome, a solved save without it clears Mordie, a found save resumes found, a searching
save with a solvable original resumes at its N, and anything else (and a Repair) starts
fresh from the loaded board if it has no moves yet, or clears Mordie if it has. All of this
lives in `MordieHooks`. In the TUI, `ScreenState` carries `mordieLine` and the solved
title (`mordieEnding() ?: TITLE_SOLVED`); `BoardLayout` reserves `mordieRows` under
`Moves:` from the longest possible line at `mordieWrapWidth`, so the grid stays put. Every `restoreOnStartup` path ends with
`mordie.ensureStarted`, which does nothing once Mordie is bound (e.g. after a mode switch).

### view
UI only: `view.tui.TuiView` drives the mouse/keyboard TUI, renders the board and
calls into `ViewModel`. Should not manipulate `board_model` directly. The line-based
console mode (`ViewImpl` and its command table) was removed in GH-79.

**1-based display dialect (GH-10):** displayed tile values are 1-based;
`board_model` and `storage` stay 0-based. `ScreenRenderer`'s `DISPLAY_OFFSET`
constant is the single translation point (`+1` when rendering). Clicks and key
presses resolve to 0-based `HitTarget`s directly, so nothing parses a typed index.
Range validation lives in `board_model` (`BoardImpl`'s bounds check), not `view`.

`BoardImpl.restoreState` and `FileSaveRepository`'s validation errors avoid
stating an explicit numeric range (e.g. "contain each of N tile values
exactly once" rather than "permutation of 0..N-1") so they read correctly
next to the 1-based board when shown in a Load dialog message.

### storage
File persistence for save games. `SaveRepository` is the contract `viewmodel`
depends on; `FileSaveRepository` is the only class in the codebase that touches
the filesystem. No dependency on `board_model`, `viewmodel`, or `view` — it
deals in plain data (`SavedBoard`), not domain objects.

Save file format: line 1 is the square side, line 2 the space-separated tile
values, line 3 the move count (GH-62). A missing or blank line 3 (a save from
before GH-62) loads as 0; a non-integer or negative one is a
`SaveFileFormatException`. GH-83 adds two optional lines: line 4 is the original
arrangement and line 5 is `<N> found` or `<N> searching`. A missing or blank line 4 (an
older save, or a GH-74 Repair write) means no Mordie data. Once line 4 is present, a bad
line 4 or 5 is a `SaveFileFormatException`.

### analytics
Cross-cutting: `AnalyticsService` is injected into `viewmodel` (and any layer
that needs to track an event), currently backed by `NoopAnalyticsService`.
`InputMethodAnalyticsService` (GH-3) decorates another `AnalyticsService`,
adding an `input_method` (`mouse`/`keyboard`, GH-18) property to
every forwarded event — the decorator pattern lets `Main` distinguish events
by launch mode without `ViewModelImpl` itself knowing which UI mode is
running.

`board_solved` (GH-62) is tracked in `ViewModelImpl`, not a View: every shift goes
through one private `shift` helper that fires it when the shift turns an unsolved
board into a solved one, so it covers both modes. Loads bypass that helper,
so restoring a solved save never fires it. GH-83 adds `mordie_result`/`mordie_moves` to it
and tracks `mordie_solution_found` from the same helper, when a 5-shift check reveals
the solution.

### Composition root (GH-78)
`AppWiring.kt` is the only production code that constructs collaborators. `Main` builds
the session-lifetime objects (`BoardImpl`, `FileSaveRepository`, `NoopAnalyticsService`,
`Mordie(BackgroundMordieSearch.Factory())`) and hands them to `buildGameSession`, whose
`buildView` closure calls `defaultView` per mode: it decorates analytics, then builds the
viewmodel (`buildViewModel`, including `MordieHooks`) and the TUI (`buildTuiView`, including
`ScreenRenderer`, the mode's `TuiInput` and a `ModeSwitcherImpl`). Production classes take
every collaborator as a constructor parameter with no real-implementation default; value
defaults (a board side, a save directory, a flag) are fine. Tests build through
`testing/`'s `testViewModel`/`testTuiView`/`testGameSession`, which call the same
functions with fakes.

### Board-size surfaces (GH-44)
Three entry points, all of which *start a fresh game* rather than resizing the
running one: the `--size=N` launch flag (`Main.resolveBoardSize` returns `Int?`
- `null` for a missing, non-numeric, or out-of-range value, distinct from "asked
for exactly the default" - warning and falling back to 4 on `null`; reported as
`app_launched.board_size`); a startup prompt raised by
`StartupRestoreUseCase.restoreOnStartup` as `UiRequest.ChooseBoardSize`, answered by
the TUI's `Dialog.Kind.SIZE` picker; and the TUI's `[ New ]`
toolbar button (`HitTarget.ToolbarNew`, F9 in keyboard mode), which opens the
same picker. All of them funnel into `ViewModel.newGame(size, trigger)` →
`BoardImpl.newGame`, which is also where `new_game_size_selected` is tracked
(only on success). Save/load auto-resizes instead of rejecting a size
mismatch (WU5) — `BoardModel.restoreState` is the second way (besides
`newGame`) the board's side ever changes: it derives `squareSide` from the
restored state's own length, so loading a save of a different size resizes
the board to match rather than requiring a coincidental match. There is no
`size_mismatch` outcome any more; every successful `saves.load()` restores.

**Save size visibility (WU4).** Every surface that lists a save's name — the
startup restore dialog, the mid-game Load dialog, and the "not found"/"no save
named" messages — also shows that save's board size, so a
player can tell up front what size loading it will resize the board to.
`ViewModel.listSaves()` returns `List<SaveInfo>` (`name` + nullable
`squareSide`, `viewmodel/SaveInfo.kt`) instead of bare names; `squareSide` is
`null` when the save can't be read (corrupted/IO error) rather than dropping
the entry. `UiRequest.ConfirmRestore`/`ChooseSaveToRestore` now carry
`SaveInfo` too. No save-file-format change — `SaveCatalog` derives each
size from `saves.load(name)?.squareSide`, one extra read per save already
being listed.
`SaveInfo.solvable` (GH-74) comes from the same read via `Solvability`;
`display()` appends `UNSOLVABLE_MARK` for a save that can't be solved.
`LoadGameUseCase` never restores an unsolvable save directly: it raises
`UiRequest.ResolveUnsolvableSave` (`atStartup` set from the startup restore) and
acts on the `UnsolvableSaveChoice`. REPAIR writes `Solvability.repaired` back
through `saves.save` and loads it. NEW_GAME raises
`ChooseBoardSize(origin = "unsolvable_save")` mid-game, or returns `false` at
startup so `restoreOnStartup`'s own size prompt runs. EXIT only reports
`LoadResult.EXIT_CHOSEN` (GH-41); once the load has returned, `GameViewModelImpl` hands it to
its `UnsolvableSaveExit` hook. The terminal's hook (built in `buildViewModel`) runs
`exitGame()` mid-game, or throws `ExitRequestedException` at startup, which
`TuiView.play` catches around `restoreOnStartup`.
`ChooseBoardSize.origin` is what the TUI reports as
`screen_size_dialog.opened_from`. The TUI answers with a non-cancellable
`Dialog.Kind.UNSOLVABLE` (`runModal(cancellable = false)`), and
`ExitDialogController.confirmSaveBeforeExit` opens a Yes/No dialog itself when no toolbar
Exit answer is pending.

**`--size=N` bypasses the *entire* startup sequence, not just the size prompt.**
A usable `--size=N` sets `sizeChosenAtLaunch` (captured by `buildGameSession`
and passed through `defaultView` → `StartupRestoreUseCase` on every rebuild), and `restoreOnStartup` returns immediately when
it's set — no restore prompt, no size prompt, no `startup_restore_prompt_shown`/
`startup_restore_decision`, regardless of what saves exist on disk. Without the
flag (or with an unusable one - `sizeChosenAtLaunch` is `false` in that case
too, matching `resolveBoardSize`'s `null`), the restore prompt runs as before
(GH-6), and the size prompt raises only once it's established nothing was
actually restored (no saves, declined, blank/EOF, or a failed load). With 2+
saves, `restoreOnStartup` matches the answer against save names exactly; the TUI
always answers with a listed name, so anything else counts as declined (GH-79).

### Launch mode and runtime mode switching (GH-3, GH-18, GH-30, GH-79)
`Main.resolveInputMode` decides how a launch starts. With no interactive
terminal (`System.console() == null`, e.g. `./gradlew run` or piped input) it
prints `No interactive terminal found - Mordovorot only runs as a terminal UI.`
to stderr, tracks `app_launch_aborted` instead of `app_launched`, and returns
`null`, so `main` returns and the JVM exits 0. Otherwise `InputMode.resolve`
(root package, renamed from `LaunchMode` for GH-30) is pure flag parsing:
`--keyboard` selects the TUI built with `KeyboardInput`, anything else (no
flag, `--mouse`) the TUI built with `MouseInput`. `--console` selects nothing:
in a terminal it prints `Console mode was removed - starting the terminal UI.`
and sets `app_launched.console_flag_ignored`. The terminal check and both
message sinks are injectable parameters, so all of this is unit-tested without
a real terminal.

`InputMode` no longer only decides where a session *begins* — `GameSession`
lets it switch mid-session, driven by `ModeSwitcher`. `TuiView`'s toolbar
`[ Keyboard ]` button (mouse mode) and F7 (keyboard mode), reached via
`HitTarget.ToolbarMode`/`ScreenState.modeButtons`, go through `ToolbarRouter` to `ModeSwitcherImpl`,
which tracks `input_mode_switched` and throws `ModeSwitchRequestedException`.
`GameSession` catches the exception, rebuilds the `View`/viewmodel/analytics
stack for the new mode (through `buildGameSession`'s closure) around the *same*
`BoardImpl`/`FileSaveRepository` instances, and re-enters `play()` — so switching preserves board state and
never reshuffles. If that rebuild fails (e.g. no `stty`), `GameSession`
rebuilds the previous mode and hands it `Could not switch to <target> mode
(<reason>) - staying in <previous> mode.` via `View.play(notice)`, shown as the
first status line (stderr would be hidden behind the full-screen TUI); a failure on the first session, or of that
fallback, still throws. `app_launched` still fires exactly once, from the
initial resolution in `Main`; each subsequent switch fires
`input_mode_switched` instead (`docs/ANALYTICS_EVENTS.md`).

## Dependencies

- **Build tool:** Gradle (Kotlin DSL), via the wrapper (`./gradlew`) — pinned to 8.7.
  `kotlin("jvm")` + `application` plugins; JVM toolchain 17.
- **Test framework:** `kotlin("test")` on the JUnit 5 platform (`useJUnitPlatform()`).
  No mocking library — hand-written fakes in `src/test/kotlin/testing/`.
- **`kotlinx-coroutines-core` (GH-42) — the project's first third-party runtime
  dependency.** Powers `viewmodel`'s View-request channel (see above): `ViewModel`'s
  suspend functions, `Channel`, and `runBlocking` at `Main`'s entry point. Unlike
  JLine (below), this cleared the zero-third-party bar because it's the
  JetBrains-maintained concurrency primitive the eventual KMP UI layer needs anyway,
  not a terminal-handling convenience — and it's the multiplatform artifact, so the
  same coordinate resolves per-target once other platforms are added, with no
  declaration change. Tests run on plain `runBlocking`; `kotlinx-coroutines-test` was
  dropped unused (checkup CHK-2026-10-14-light-2), with `withTimeout` guarding the
  tests where a deadlock would otherwise hang silently. Run on a single `runBlocking` event
  loop with no dispatcher — there's no
  real concurrency to exploit here (every I/O call is blocking, single-consumer), so
  a thread pool (`Dispatchers.Default`/`IO`) or `Dispatchers.Unconfined` would only
  add nondeterminism to the request/response ordering for no benefit.
  **One exception (GH-83):** `BackgroundMordieSearch` runs the Mordie search as one
  throttled coroutine on `Dispatchers.Default`. It never touches the UI request channel;
  the event loop only reads its volatile progress snapshot. That keeps the request/response
  ordering above deterministic.
- Beyond that, production code has no dependencies past the Kotlin standard library
  (board shuffling uses `kotlin.collections.shuffle()`, not a custom implementation).
- **JLine considered and rejected (GH-6):** save/load filename tab-autocompletion
  would require raw/cbreak terminal input, which only a library like JLine 3 provides.
  Rejected as too heavyweight for this project's zero-third-party-dependency stance.
  The TUI later made the question moot: its Load dialog lists the saves to pick from,
  so nothing needs a typed save name.
