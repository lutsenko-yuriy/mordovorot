# Architecture

<!-- Describe how the code is organised.
     The Tech Lead and Developer agents read this file before planning or implementing work.
     Keep it accurate — update it whenever the structure changes. -->

## Overview

MVP (Model-View-Presenter): board_model + presenter + view packages

## Directory structure

```
src/main/kotlin/
├── Main.kt              # Entry point — resolves LaunchMode, then builds and plays either
│                          # view.tui.TuiView (MOUSE) or ViewImpl (CONSOLE) (GH-3)
├── LaunchMode.kt        # MOUSE / CONSOLE — resolves from `--console` + terminal availability (GH-3)
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
│   │                           # can be injected (GH-23)
│   ├── ConsolePresenterImpl.kt # BasePresenter + ConsolePresenter — owns the line-based play() loop
│   ├── TuiPresenterImpl.kt     # BasePresenter + TuiPresenter — owns the query surface, exposes
│   │                             # restoreOnStartup() publicly for view.tui.TuiView (GH-23)
│   └── ExitRequestedException.kt # Signals play() (or view.tui.TuiView's own loop, GH-3) to stop
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
        │                                               # and legacy X10 mouse reports, keys)
        ├── BoardLayout.kt / HitTarget.kt   # Pure board geometry + click hit-testing
        ├── Dialog.kt / DialogLayout.kt     # One model + one geometry class for all three
        │                                     # modal dialogs (Save/Load/Exit) and the
        │                                     # Load-shaped startup restore prompt (GH-3 WU4)
        ├── ScreenState.kt / ScreenRenderer.kt # Pure ScreenState -> frame String rendering,
        │                                        # including the dialog overlay
        └── TuiView.kt   # Owns its own event loop (doesn't call ConsolePresenterImpl.play() - see
                          # the ticket's solved-state note). Save/Load/Exit and the startup
                          # restore prompt each run their own blocking modal loop over
                          # Terminal (WU4). State-driven solved screen (WU5): every repaint asks
                          # TuiPresenter.isSolved() fresh rather than tracking a phase flag, so the
                          # Congratulations title/dimmed arrows and a load back to an unsolved
                          # board both fall out of the same repaint path with no extra branching.

src/test/kotlin/
├── LaunchModeTest.kt      # LaunchMode resolution + app_launched tracking (GH-3)
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
                            # doubles, split GH-23) / TestPresenter (minimal concrete BasePresenter)
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
adding an `input_method` (`console`/`mouse`) property to every forwarded
event — the decorator pattern lets `Main` distinguish events by launch mode
without `BasePresenter` itself knowing which UI mode is running.

### Launch mode (GH-3)
`LaunchMode` (root package) resolves which `View` `Main` builds: `--console`
always selects the console `ViewImpl`; otherwise the mouse-driven TUI
(`view.tui`, in progress) is the default, unless no interactive terminal is
available (`System.console() == null`, e.g. a piped/scripted run), in which
case it falls back to console mode automatically. Resolution is pure and
injectable (`hasInteractiveTerminal` is a constructor-style parameter), so
it's unit-tested without a real terminal.

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
