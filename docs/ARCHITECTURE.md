# Architecture

<!-- Describe how the code is organised.
     The Tech Lead and Developer agents read this file before planning or implementing work.
     Keep it accurate — update it whenever the structure changes. -->

## Overview

MVVM (Model-View-ViewModel): board_model + viewmodel + view packages

## Directory structure

```
src/main/kotlin/
├── Main.kt              # Entry point — resolves the starting InputMode, then hands off to
│                          # GameSession.run() (GH-3, GH-30)
├── InputMode.kt         # MOUSE / CONSOLE / KEYBOARD — resolves the starting mode from
│                          # `--console`/`--keyboard`/`--mouse` + terminal availability; also
│                          # the type GameSession/ModeSwitcher pass around mid-session
│                          # (GH-3, GH-18, renamed from LaunchMode for GH-30)
├── GameSession.kt       # Session loop extracted out of Main: owns the one BoardImpl and
│                          # one FileSaveRepository that survive every mode switch; builds a
│                          # fresh View + viewmodel + InputMethodAnalyticsService per mode,
│                          # runs play(), and re-enters on ModeSwitchRequestedException. A
│                          # rebuild's play() throwing anything else falls back to console
│                          # (unless console itself just failed) instead of crashing and losing
│                          # the in-progress game - a first-session failure still throws (GH-30)
├── analytics/
│   ├── AnalyticsService.kt             # Analytics abstraction — track(event, properties)
│   ├── NoopAnalyticsService.kt         # Default implementation; no SDK wired up yet
│   └── InputMethodAnalyticsService.kt  # Decorator adding `input_method` to every forwarded
│                                         # event, without touching ViewModelImpl's call sites (GH-3)
├── board_model/
│   ├── BoardModel.kt    # Board interface (domain contract)
│   ├── BoardSize.kt     # The valid board side range (MIN 3 / MAX 5 / DEFAULT 4) and its
│   │                      # validation - the one place the range lives (GH-44)
│   └── BoardImpl.kt     # IntArray-backed board state + shift/reset/newGame/isCorrect logic.
│                          # `SQUARE_SIDE` is a read-only view over a private mutable field:
│                          # `newGame(side)` is the only thing that ever changes it (GH-44),
│                          # `resetGame()` reshuffles at the same size
├── viewmodel/
│   ├── ViewModel.kt          # The one viewmodel interface (GH-42 WU3, collapsing GH-23's
│   │                           # ConsoleViewModel/TuiViewModel split) — shift/reset/save/load/exit
│   │                           # plus the read-only query surface (listSaves/saveExists/isSolved/
│   │                           # boardState/squareSide) and restoreOnStartup
│   ├── ViewModelImpl.kt      # The one implementation (GH-42 WU3, collapsing BaseViewModel +
│   │                           # ConsoleViewModelImpl + TuiViewModelImpl). board/saves/analytics
│   │                           # are constructor params (each defaulted to a real impl) so fakes
│   │                           # can be injected. `startupRestoreDone` is also a constructor param
│   │                           # (default false) so GameSession can seed it true on every session
│   │                           # after the first, skipping a re-shown startup restore prompt after
│   │                           # a mode switch (GH-30). Holds no `View` reference — see the
│   │                           # "viewmodel" section below
│   ├── UiRequest.kt          # Sealed request/response type for the six viewmodel→UI
│   │                           # interactions (ShowMessage/ConfirmRestore/ChooseSaveToRestore/
│   │                           # ConfirmSaveBeforeExit/PromptSaveName/ChooseBoardSize (GH-44)),
│   │                           # sent on
│   │                           # ViewModel.uiRequests (GH-42 WU2)
│   ├── SessionControlException.kt # Sealed marker base for control-flow exceptions that unwind
│   │                                 # play() intentionally — lets a catch-all rethrow via this
│   │                                 # one type instead of naming each subtype (GH-30)
│   ├── ExitRequestedException.kt # Signals play() (or view.tui.TuiView's own loop, GH-3) to stop
│   ├── ModeSwitcher.kt        # ModeSwitcher / ModeSwitcherImpl / NoopModeSwitcher — checks for
│   │                             # an interactive terminal, tracks input_mode_switched, and
│   │                             # either throws ModeSwitchRequestedException (success) or shows
│   │                             # a message and returns (rejected), mirroring
│   │                             # ViewModelImpl.exitGame's contract (GH-30)
│   └── ModeSwitchRequestedException.kt # Carries the requested InputMode; unwinds a running
│                                          # session's play() back to GameSession's loop (GH-30)
├── storage/
│   ├── SaveRepository.kt          # Persistence contract: list/exists/save/load
│   ├── SavedBoard.kt              # Data carrier: square side + tile arrangement
│   ├── FileSaveRepository.kt      # Plain-text file implementation (the only class
│   │                                touching the filesystem); rejects unsafe file names
│   └── SaveFileFormatException.kt # Thrown on a malformed save file
└── view/
    ├── View.kt          # View interface — console display + command loop contract
    ├── ViewImpl.kt      # Console I/O implementation (BufferedReader-based input). `mouse`/
    │                      # `keyboard` commands reach an injected ModeSwitcher, wired via
    │                      # create()'s modeSwitcherFactory param (GH-30)
    └── tui/             # GH-3: a second View implementation for the mouse-driven TUI
        ├── Terminal.kt / AnsiTerminal.kt   # Raw-mode terminal I/O (stty via ProcessBuilder,
        │                                     # xterm mouse-reporting escapes) - the one seam
        │                                     # touching the real terminal
        ├── TerminalEvent.kt / TerminalInputParser.kt # Byte-stream -> event decoding (SGR-1006
        │                                               # and legacy X10 mouse reports, keys,
        │                                               # arrow/Tab/BackTab/function-key
        │                                               # decoding for keyboard mode, incl.
        │                                               # F7/F8 for the mode-switch shortcuts)
        │                                               # (GH-18, GH-30)
        ├── BoardLayout.kt / HitTarget.kt   # Pure board geometry + click hit-testing, incl. the
        │                                     # toolbar's mode-switch buttons
        │                                     # (HitTarget.ToolbarMode) (GH-30)
        ├── ArrowRing.kt     # Pure perimeter-ring cursor logic for keyboard board navigation:
        │                      # ArrowCursor(edge, index) + Edge{LEFT,RIGHT,TOP,BOTTOM}, one
        │                      # move per arrow key, wrapping at corners (GH-18)
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
        │                                        # including the dialog overlay, the keyboard
        │                                        # cursor highlight, the controls hint, and
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
        ├── KeyboardInput.kt # TuiInput impl: owns the board ArrowCursor and DialogFocus state
        │                      # for --keyboard mode (GH-18)
        └── TuiView.kt   # Owns its own event loop (doesn't go through ViewImpl.play()'s
                          # console-only loop - see the ticket's solved-state note). Save/Load/
                          # Exit and the startup restore prompt each run their own blocking modal
                          # loop over Terminal (WU4). State-driven solved screen (WU5): every
                          # repaint asks ViewModel.isSolved() fresh rather than tracking a phase
                          # flag, so the Congratulations title/dimmed arrows and a load back to an
                          # unsolved board both fall out of the same repaint path with no extra
                          # branching. Delegates input handling to an injected TuiInput (default
                          # MouseInput()) so mouse and keyboard modes share one event loop (GH-18).

src/test/kotlin/
├── InputModeTest.kt       # InputMode resolution + app_launched tracking (GH-3, renamed
│                            # from LaunchModeTest for GH-30)
├── GameSessionTest.kt     # Session loop: switch rebuilds around the same BoardModel
│                            # instance, startupRestoreDone seeding, multi-switch chains (GH-30)
├── ModeSwitcherTest.kt    # TTY present/absent x target x trigger matrix; success vs
│                            # rejected_no_tty; same-mode no-op (GH-30)
├── analytics/            # NoopAnalyticsService, InputMethodAnalyticsService coverage
├── board_model/         # BoardImpl coverage: reset/shuffle, isCorrect, all four shifts, restoreState
├── viewmodel/            # One test file per ViewModelImpl concern (GH-42 WU3, collapsing GH-23's
│                           # split): ViewModelDelegationTest / ViewModelSaveLoadTest /
│                           # ViewModelExitTest drive ViewModelImpl directly, no play()
│                           # loop involved; ViewModelStartupRestoreTest covers restoreOnStartup's
│                           # branch matrix plus the idempotence guard; ViewModelQueriesTest covers
│                           # the read-only query surface; ViewModelCancellationTest/
│                           # ViewModelOrderingTest pin the request-channel's CancellationException
│                           # guard and rendezvous ordering (GH-42 WU2)
├── storage/               # FileSaveRepository coverage
├── view/                  # ViewImpl command-parsing coverage (incl. ViewImplPlayTest, the
│                           # console session loop that moved here from ConsoleViewModelImpl.play()
│                           # in GH-42 WU3); view/tui/ coverage (GH-3, WU2-WU5)
└── testing/              # FakeBoardModel / FakeView / FakeSaveRepository / RecordingAnalyticsService /
                            # FakeViewModel (recording double, collapsing GH-23's
                            # RecordingViewModel/FakeConsoleViewModel/FakeTuiViewModel split in
                            # GH-42 WU3) / FakeViewModelUi (scripted UiRequest responder, GH-42
                            # WU2) / RecordingModeSwitcher (GH-30)
```

Gradle's standard source-set convention (`src/main/kotlin`, `src/test/kotlin`) is used —
see "Dependencies" below.

## Layers

Classic MVVM. `board_model` and `view` are each an interface + one implementation.
`viewmodel` (GH-42) is a single `ViewModel` interface + one `ViewModelImpl`, holding
no reference to any `View` — it communicates with whichever `View` is driving it
through a request channel instead of a constructor-injected dependency. Callers
should always depend on the interface, not `ViewModelImpl`, to keep layers swappable.

### board_model (Domain)
Core game state and rules: board array, shifting, reset, and the win check
(`isCorrect`). No dependency on `viewmodel` or `view`.

**Board size (GH-44):** the board stays square, with a side of 3–5 (default 4).
`BoardSize` holds that range and its validation — the single place it lives, per
this layer's "range validation lives in `board_model`, the view translates but
does not validate" rule (see the 1-based console dialect note under `view`).
`BoardImpl.newGame(side)` is the only operation that ever changes the side: it
validates first, so an out-of-range value throws with the current board left
untouched, then reallocates and shuffles. `resetGame()` keeps its older meaning —
reshuffle at the same size. Nothing resizes an *in-progress* board: every
size-setting surface starts a fresh game instead. The board instance itself is
still the one `GameSession` holds across mode switches (GH-30) — the side mutates
in place rather than the instance being swapped, so that invariant is unaffected.

### viewmodel (GH-42: ViewModel-style, no View reference)
**Status: landed (WU4/4 of the GH-42 redesign — final WU).** Everything described below is
real and in use; the `presenter` package/naming this superseded is gone.

Mediates between `view`, `board_model`, and `storage`, but never calls into `view`
directly. `ViewModel` is one interface (shift/reset/save/load/exit plus the
query surface both UIs need — `isSolved`/`boardState`/`squareSide`/`listSaves`/
`saveExists`), and `ViewModelImpl` is its one implementation, taking `board`/`saves`/
`analytics` as constructor params (each defaulted to a real implementation) so
tests can inject fakes without touching the filesystem or a real analytics SDK.
Superseded GH-23's split (`ConsoleViewModel`/`TuiViewModel` + `ConsoleViewModelImpl`/
`TuiViewModelImpl`) once `ConsoleViewModelImpl.play()`'s console loop moved into
`ViewImpl` — at that point both UIs needed the identical viewmodel surface, so the
split no longer described anything real.

Five interactions that used to be blocking calls into `View` (`showMessage`,
`confirmRestore`, `chooseSaveToRestore`, `confirmSaveBeforeExit`, `promptSaveName`)
are now `viewmodel.UiRequest<R>` values sent on `ViewModel.uiRequests`, a
`Channel.RENDEZVOUS` the owning `View` drains in a sibling coroutine and answers via
`UiRequest.respond`. `showMessage` is the exception among the five - it stays on the
`View` interface too (`ModeSwitcherImpl` still calls it directly, outside any
`UiRequest`), while the other four became plain `internal` methods on `ViewImpl`/
`TuiView`, reachable only through each view's own request handler. Rendezvous delivery
(the channel has zero buffer, and `ask()` doesn't return until the View has actually
handled the request) is what keeps message/prompt ordering identical to the old
blocking-call behavior. The one invariant that keeps this deadlock-free: a View's
request handler must never call a request-raising `ViewModel` method
(`saveGame`/`loadGame`/`exitGame`/`restoreOnStartup`) from inside itself — only the
plain query methods are safe there. `saveGame`/`loadGame`/`exitGame`/`restoreOnStartup`
are `suspend`; a cancelled coroutine resumes them with `CancellationException`, which
each method's catch-all rethrows rather than swallows (ordinary structured-concurrency
hygiene).

### view
Console I/O only: reads commands from stdin, renders the board, and calls
into `ViewModel`. Should not manipulate `board_model` directly.

**1-based console dialect (GH-10):** the console is 1-based (displayed tile
values, `left`/`right`/`up`/`down` row/column input); `board_model` and
`storage` stay 0-based. `ViewImpl`'s `DISPLAY_OFFSET` constant is the single
translation point — `+1` when rendering, `-1` when parsing a shift argument.
Range validation still lives in `board_model` (`BoardImpl`'s bounds check),
not `view` — the view translates, it does not validate.

`BoardImpl.restoreState` and `FileSaveRepository`'s validation errors avoid
stating an explicit numeric range (e.g. "contain each of N tile values
exactly once" rather than "permutation of 0..N-1") specifically so they read
correctly once surfaced through `ViewImpl.play`'s `showMessage(e.message ...)`
to the 1-based console — GH-6's `save`/`load` commands are the first thing
that makes these messages reachable.

### storage
File persistence for save games. `SaveRepository` is the contract `viewmodel`
depends on; `FileSaveRepository` is the only class in the codebase that touches
the filesystem. No dependency on `board_model`, `viewmodel`, or `view` — it
deals in plain data (`SavedBoard`), not domain objects.

### analytics
Cross-cutting: `AnalyticsService` is injected into `viewmodel` (and any layer
that needs to track an event), currently backed by `NoopAnalyticsService`.
`InputMethodAnalyticsService` (GH-3) decorates another `AnalyticsService`,
adding an `input_method` (`console`/`mouse`/`keyboard`, GH-18) property to
every forwarded event — the decorator pattern lets `Main` distinguish events
by launch mode without `ViewModelImpl` itself knowing which UI mode is
running.

### Board-size surfaces (GH-44)
Four entry points, all of which *start a fresh game* rather than resizing the
running one: the `--size=N` launch flag (`Main.resolveBoardSize`, warning and
falling back to 4 on anything invalid, and reported as `app_launched.board_size`);
a startup prompt raised by `ViewModelImpl.restoreOnStartup` as
`UiRequest.ChooseBoardSize`, shown only when `--size` wasn't given *and* nothing
was restored — console answers it with a text prompt, the TUI with its
`Dialog.Kind.SIZE` picker; the console `size <N>` command (a plain integer, *not*
subject to `ViewImpl`'s 1-based `DISPLAY_OFFSET` — a size is not a row index); and
the TUI's `[ New ]` toolbar button (`HitTarget.ToolbarNew`, F9 in keyboard mode),
which opens the same picker. All of them funnel into
`ViewModel.newGame(size, trigger)` → `BoardImpl.newGame`, which is also where
`new_game_size_selected` is tracked (only on success). Save/load's size-mismatch
rejection is unchanged — a save is never auto-resized onto a differently-sized
board.

### Launch mode and runtime mode switching (GH-3, GH-18, GH-30)
`InputMode` (root package, renamed from `LaunchMode` for GH-30) resolves
which `View` `Main` builds at startup: `--console` always selects the
console `ViewImpl`; `--keyboard` selects the TUI built with `KeyboardInput`;
`--mouse` explicitly selects the TUI built with `MouseInput` (the same mode
that already runs by default when no mode flag is given). Precedence when
multiple flags are passed: `--console` beats `--keyboard` beats
`--mouse`/default. `--keyboard` and `--mouse` fall back to `CONSOLE` when no
interactive terminal is available (`System.console() == null`, e.g. a
piped/scripted run), same as the default does. Resolution is pure and
injectable (`hasInteractiveTerminal` is a constructor-style parameter), so
it's unit-tested without a real terminal.

`InputMode` no longer only decides where a session *begins* — `GameSession`
lets it switch mid-session, driven by `ModeSwitcher`. Each `View`
implementation exposes a way to request a switch (`ViewImpl`'s `mouse`/
`keyboard` commands; `TuiView`'s toolbar buttons/F7/F8 shortcuts, reached
via `HitTarget.ToolbarMode`/`ScreenState.modeButtons`), which
`ModeSwitcherImpl` either turns into a `ModeSwitchRequestedException`
(interactive terminal available for the target) or rejects with an
on-screen message (no TTY — the same constraint `--mouse`/`--keyboard`
already enforce at launch). `GameSession` catches the exception, rebuilds
the `View`/viewmodel/analytics stack for the new mode around the *same*
`BoardImpl`/`FileSaveRepository` instances, and re-enters `play()` — so
switching preserves board state and never reshuffles. `app_launched` still
fires exactly once, from the initial `InputMode` resolution in `Main`; each
subsequent switch fires `input_mode_switched` instead (`docs/ANALYTICS_EVENTS.md`).

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
  declaration change. `kotlinx-coroutines-test` is a test-only addition alongside it,
  added ahead of WU2 but still unused as of WU3 — every test in the suite runs on
  plain `runBlocking`, matching the rest of the codebase, rather than mixing in
  `runTest` ahead of a project-wide decision (see `build.gradle.kts`'s comment on the
  dependency). `runTest` would give a timeout-failure instead of a silent hang on a
  deadlocked coroutine test, the failure mode the request channel actually risks —
  adopting it (or dropping the dependency and relying on Gradle's own test timeout
  instead) is an open decision, not yet settled. Run on a single `runBlocking` event
  loop with no dispatcher — there's no
  real concurrency to exploit here (every I/O call is blocking, single-consumer), so
  a thread pool (`Dispatchers.Default`/`IO`) or `Dispatchers.Unconfined` would only
  add nondeterminism to the request/response ordering for no benefit.
- Beyond that, production code has no dependencies past the Kotlin standard library
  (board shuffling uses `kotlin.collections.shuffle()`, not a custom implementation).
- **JLine considered and rejected (GH-6):** save/load filename tab-autocompletion
  would require raw/cbreak terminal input, which only a library like JLine 3 provides.
  Rejected as too heavyweight for this project's zero-third-party-dependency stance,
  and JLine degrades to a dumb terminal anyway when stdin isn't a real TTY (exactly how
  `./gradlew run` and the test suite invoke the app). Substitute: save-selection prompts
  list the available save names so the user can always see and copy an exact name.
