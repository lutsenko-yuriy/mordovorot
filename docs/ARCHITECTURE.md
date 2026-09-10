# Architecture

<!-- Describe how the code is organised.
     The Tech Lead and Developer agents read this file before planning or implementing work.
     Keep it accurate — update it whenever the structure changes. -->

## Overview

MVP (Model-View-Presenter): board_model + presenter + view packages

## Directory structure

```
src/main/kotlin/
├── Main.kt              # Entry point — constructs ViewImpl and calls play()
├── analytics/
│   ├── AnalyticsService.kt      # Analytics abstraction — track(event, properties)
│   └── NoopAnalyticsService.kt  # Default implementation; no SDK wired up yet
├── board_model/
│   ├── BoardModel.kt    # Board interface (domain contract)
│   └── BoardImpl.kt     # IntArray-backed board state + shift/reset/isCorrect logic
├── presenter/
│   ├── Presenter.kt     # Presenter interface — mediates view <-> model
│   └── PresenterImpl.kt # Presenter implementation; board/saves/analytics are constructor
│                         # params (each defaulted to a real impl) so fakes can be injected
├── storage/
│   ├── SaveRepository.kt          # Persistence contract: list/exists/save/load
│   ├── SavedBoard.kt              # Data carrier: square side + tile arrangement
│   ├── FileSaveRepository.kt      # Plain-text file implementation (the only class
│   │                                touching the filesystem); rejects unsafe file names
│   └── SaveFileFormatException.kt # Thrown on a malformed save file
└── view/
    ├── View.kt          # View interface — console display + command loop contract
    └── ViewImpl.kt      # Console I/O implementation (BufferedReader-based input)

src/test/kotlin/
├── analytics/            # NoopAnalyticsService coverage
├── board_model/         # BoardImpl coverage: reset/shuffle, isCorrect, all four shifts, restoreState
├── presenter/            # PresenterImpl coverage: delegation, play() loop, save/load, startup restore
├── storage/               # FileSaveRepository coverage
├── view/                  # ViewImpl command-parsing coverage
└── testing/              # FakeBoardModel / FakeView / FakeSaveRepository /
                            # RecordingAnalyticsService / FakePresenter test doubles
```

Gradle's standard source-set convention (`src/main/kotlin`, `src/test/kotlin`) is used —
see "Dependencies" below.

## Layers

Classic MVP. Each layer is an interface + one implementation; callers should
depend on the interface (`BoardModel`, `Presenter`, `View`), not the `*Impl`
class, to keep layers swappable.

### board_model (Domain)
Core game state and rules: board array, shifting, reset, and the win check
(`isCorrect`). No dependency on `presenter` or `view`.

### presenter
Mediates between `view` and `board_model`. Owns no UI or storage code itself;
translates view commands into model calls.

### view
Console I/O only: reads commands from stdin, renders the board, and calls
into the `Presenter`. Should not manipulate `board_model` directly.

**1-based console dialect (GH-10):** the console is 1-based (displayed tile
values, `left`/`right`/`up`/`down` row/column input); `board_model` and
`storage` stay 0-based. `ViewImpl`'s `DISPLAY_OFFSET` constant is the single
translation point — `+1` when rendering, `-1` when parsing a shift argument.
Range validation still lives in `board_model` (`BoardImpl`'s bounds check),
not `view` — the view translates, it does not validate.

### storage
File persistence for save games. `SaveRepository` is the contract `presenter`
depends on; `FileSaveRepository` is the only class in the codebase that touches
the filesystem. No dependency on `board_model`, `presenter`, or `view` — it
deals in plain data (`SavedBoard`), not domain objects.

### analytics
Cross-cutting: `AnalyticsService` is injected into `presenter` (and any layer
that needs to track an event), currently backed by `NoopAnalyticsService`.

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
