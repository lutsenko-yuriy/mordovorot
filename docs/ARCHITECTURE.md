# Architecture

<!-- Describe how the code is organised.
     The Tech Lead and Developer agents read this file before planning or implementing work.
     Keep it accurate — update it whenever the structure changes. -->

## Overview

MVP (Model-View-Presenter): board_model + presenter + view packages

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
│                          # fresh View + presenter + InputMethodAnalyticsService per mode,
│                          # runs play(), and re-enters on ModeSwitchRequestedException (GH-30)
├── analytics/
│   ├── AnalyticsService.kt             # Analytics abstraction — track(event, properties)
│   ├── NoopAnalyticsService.kt         # Default implementation; no SDK wired up yet
│   └── InputMethodAnalyticsService.kt  # Decorator adding `input_method` to every forwarded
│                                         # event, without touching BasePresenter's call sites (GH-3)
├── board_model/
│   ├── BoardModel.kt    # Board interface (domain contract)
│   └── BoardImpl.kt     # IntArray-backed board state + shift/reset/isCorrect logic
├── presenter/
│   ├── Presenter.kt          # Shared core interface — shift/reset/save/load/exit (GH-23)
│   ├── ConsolePresenter.kt   # Console-only interface — adds play() (GH-23)
│   ├── TuiPresenter.kt       # TUI-only interface — adds the read-only query surface
│   │                           # (listSaves/saveExists/isSolved/boardState/squareSide) and
│   │                           # restoreOnStartup (GH-23)
│   ├── BasePresenter.kt      # Abstract base implementing the shared core; board/saves/analytics
│   │                           # are constructor params (each defaulted to a real impl) so fakes
│   │                           # can be injected (GH-23). `startupRestoreDone` is also a
│   │                           # constructor param (default false) so GameSession can seed it
│   │                           # true on every session after the first, skipping a re-shown
│   │                           # startup restore prompt after a mode switch (GH-30)
│   ├── ConsolePresenterImpl.kt # BasePresenter + ConsolePresenter — owns the line-based play() loop
│   ├── TuiPresenterImpl.kt     # BasePresenter + TuiPresenter — owns the query surface, exposes
│   │                             # restoreOnStartup() publicly for view.tui.TuiView (GH-23)
│   ├── ExitRequestedException.kt # Signals play() (or view.tui.TuiView's own loop, GH-3) to stop
│   ├── ModeSwitcher.kt        # ModeSwitcher / ModeSwitcherImpl / NoopModeSwitcher — checks for
│   │                             # an interactive terminal, tracks input_mode_switched, and
│   │                             # either throws ModeSwitchRequestedException (success) or shows
│   │                             # a message and returns (rejected), mirroring
│   │                             # BasePresenter.exitGame's contract. Same package as
│   │                             # ExitRequestedException, not root — both are thrown from and
│   │                             # caught inside the presenter layer (GH-30)
│   └── ModeSwitchRequestedException.kt # Carries the requested InputMode; unwinds a running
│                                          # session's play() back to GameSession's loop, the same
│                                          # way ExitRequestedException unwinds it to quit (GH-30)
├── storage/
│   ├── SaveRepository.kt          # Persistence contract: list/exists/save/load
│   ├── SavedBoard.kt              # Data carrier: square side + tile arrangement
│   ├── FileSaveRepository.kt      # Plain-text file implementation (the only class
│   │                                touching the filesystem); rejects unsafe file names
│   └── SaveFileFormatException.kt # Thrown on a malformed save file
└── view/
    ├── View.kt          # View interface — console display + command loop contract
    ├── ViewImpl.kt      # Console I/O implementation (BufferedReader-based input)
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
        ├── Dialog.kt / DialogLayout.kt     # One model + one geometry class for all three
        │                                     # modal dialogs (Save/Load/Exit) and the
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
        └── TuiView.kt   # Owns its own event loop (doesn't call ConsolePresenterImpl.play() - see
                          # the ticket's solved-state note). Save/Load/Exit and the startup
                          # restore prompt each run their own blocking modal loop over
                          # Terminal (WU4). State-driven solved screen (WU5): every repaint asks
                          # TuiPresenter.isSolved() fresh rather than tracking a phase flag, so the
                          # Congratulations title/dimmed arrows and a load back to an unsolved
                          # board both fall out of the same repaint path with no extra branching.
                          # Delegates input handling to an injected TuiInput (default
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
├── presenter/            # One test file per production class (GH-3, split GH-23):
│                           # BasePresenterDelegationTest / BasePresenterSaveLoadTest /
│                           # BasePresenterExitTest drive testing.TestPresenter directly, no
│                           # play() loop; ConsolePresenterPlayTest drives ConsolePresenterImpl.play()
│                           # (incl. the loop-continuation half of the exit-save-failure case);
│                           # ConsolePresenterStartupRestoreTest covers the startup-restore branch
│                           # matrix through play(); TuiPresenterStartupRestoreTest covers the
│                           # idempotence guard plus one cross-check against the TUI entry point;
│                           # TuiPresenterQueriesTest covers TuiPresenterImpl's query surface
├── storage/               # FileSaveRepository coverage
├── view/                  # ViewImpl command-parsing coverage; view/tui/ coverage (GH-3, WU2-WU5)
└── testing/              # FakeBoardModel / FakeView / FakeSaveRepository / RecordingAnalyticsService /
                            # RecordingPresenter + FakeConsolePresenter/FakeTuiPresenter (recording
                            # doubles, split GH-23) / TestPresenter (minimal concrete BasePresenter) /
                            # RecordingModeSwitcher (GH-30)
```

Gradle's standard source-set convention (`src/main/kotlin`, `src/test/kotlin`) is used —
see "Dependencies" below.

## Layers

Classic MVP. `board_model` and `view` are each an interface + one implementation.
`presenter` is an interface hierarchy (`Presenter` core + `ConsolePresenter`/`TuiPresenter`)
with one implementation per UI (`ConsolePresenterImpl`/`TuiPresenterImpl`), split for GH-23
so each `View` depends only on the presenter surface it actually needs. Callers should always
depend on the interface, not the `*Impl` class, to keep layers swappable.

### board_model (Domain)
Core game state and rules: board array, shifting, reset, and the win check
(`isCorrect`). No dependency on `presenter` or `view`.

### presenter
Mediates between `view`, `board_model`, and `storage`. `Presenter` is the
shared core (shift/reset/save/load/exit) both UIs use identically; `ConsolePresenter`
and `TuiPresenter` extend it with their own UI-specific surface (`play()` for the
console; the read-only query methods and `restoreOnStartup` for the TUI) so each
`View` implementation depends only on the presenter surface it actually needs
(GH-23). `BasePresenter` implements the shared core and takes `board`/`saves`/
`analytics` as constructor params (each defaulted to a real implementation), so
tests can inject fakes without touching the filesystem or a real analytics SDK.
`ConsolePresenterImpl` and `TuiPresenterImpl` extend it with their respective
UI-specific methods.

### view
Console I/O only: reads commands from stdin, renders the board, and calls
into the `ConsolePresenter`. Should not manipulate `board_model` directly.

**1-based console dialect (GH-10):** the console is 1-based (displayed tile
values, `left`/`right`/`up`/`down` row/column input); `board_model` and
`storage` stay 0-based. `ViewImpl`'s `DISPLAY_OFFSET` constant is the single
translation point — `+1` when rendering, `-1` when parsing a shift argument.
Range validation still lives in `board_model` (`BoardImpl`'s bounds check),
not `view` — the view translates, it does not validate.

`BoardImpl.restoreState` and `FileSaveRepository`'s validation errors avoid
stating an explicit numeric range (e.g. "contain each of N tile values
exactly once" rather than "permutation of 0..N-1") specifically so they read
correctly once surfaced through `ConsolePresenterImpl.play`'s `showMessage(e.message
...)` to the 1-based console — GH-6's `save`/`load` commands are the first
thing that makes these messages reachable.

### storage
File persistence for save games. `SaveRepository` is the contract `presenter`
depends on; `FileSaveRepository` is the only class in the codebase that touches
the filesystem. No dependency on `board_model`, `presenter`, or `view` — it
deals in plain data (`SavedBoard`), not domain objects.

### analytics
Cross-cutting: `AnalyticsService` is injected into `presenter` (and any layer
that needs to track an event), currently backed by `NoopAnalyticsService`.
`InputMethodAnalyticsService` (GH-3) decorates another `AnalyticsService`,
adding an `input_method` (`console`/`mouse`/`keyboard`, GH-18) property to
every forwarded event — the decorator pattern lets `Main` distinguish events
by launch mode without `BasePresenter` itself knowing which UI mode is
running.

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
the `View`/presenter/analytics stack for the new mode around the *same*
`BoardImpl`/`FileSaveRepository` instances, and re-enters `play()` — so
switching preserves board state and never reshuffles. `app_launched` still
fires exactly once, from the initial `InputMode` resolution in `Main`; each
subsequent switch fires `input_mode_switched` instead (`docs/ANALYTICS_EVENTS.md`).

## Dependencies

- **Build tool:** Gradle (Kotlin DSL), via the wrapper (`./gradlew`) — pinned to 8.7.
  `kotlin("jvm")` + `application` plugins; JVM toolchain 17.
- **Test framework:** `kotlin("test")` on the JUnit 5 platform (`useJUnitPlatform()`).
  No mocking library — hand-written fakes in `src/test/kotlin/testing/`.
- Production code has no dependencies beyond the Kotlin standard library
  (board shuffling uses `kotlin.collections.shuffle()`, not a custom implementation).
- **JLine considered and rejected (GH-6):** save/load filename tab-autocompletion
  would require raw/cbreak terminal input, which only a library like JLine 3 provides.
  Rejected as too heavyweight for this project's zero-third-party-dependency stance,
  and JLine degrades to a dumb terminal anyway when stdin isn't a real TTY (exactly how
  `./gradlew run` and the test suite invoke the app). Substitute: save-selection prompts
  list the available save names so the user can always see and copy an exact name.
