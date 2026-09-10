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
├── board_model/
│   ├── BoardModel.kt    # Board interface (domain contract)
│   └── BoardImpl.kt     # IntArray-backed board state + shift/reset/isCorrect logic
├── presenter/
│   ├── Presenter.kt     # Presenter interface — mediates view <-> model
│   └── PresenterImpl.kt # Presenter implementation; board is a constructor param
│                         # (defaulted to BoardImpl()) so it can be swapped for a fake
└── view/
    ├── View.kt          # View interface — console display + command loop contract
    └── ViewImpl.kt      # Console I/O implementation

src/test/kotlin/
├── board_model/         # BoardImpl coverage: reset/shuffle, isCorrect, all four shifts
├── presenter/            # PresenterImpl coverage: delegation + play() loop behavior
└── testing/              # FakeBoardModel / FakeView test doubles shared across tests
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

## Dependencies

- **Build tool:** Gradle (Kotlin DSL), via the wrapper (`./gradlew`) — pinned to 8.7.
  `kotlin("jvm")` + `application` plugins; JVM toolchain 17.
- **Test framework:** `kotlin("test")` on the JUnit 5 platform (`useJUnitPlatform()`).
  No mocking library — hand-written fakes in `src/test/kotlin/testing/`.
- Production code has no dependencies beyond the Kotlin standard library
  (board shuffling uses `kotlin.collections.shuffle()`, not a custom implementation).
