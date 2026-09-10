# Architecture

<!-- Describe how the code is organised.
     The Tech Lead and Developer agents read this file before planning or implementing work.
     Keep it accurate — update it whenever the structure changes. -->

## Overview

MVP (Model-View-Presenter): board_model + presenter + view packages

## Directory structure

```
src/
├── Main.kt              # Entry point — constructs ViewImpl and calls play()
├── board_model/
│   ├── BoardModel.kt    # Board interface (domain contract)
│   ├── BoardImpl.kt     # IntArray-backed board state + shift/reset/isCorrect logic
│   └── Extensions.kt    # IntArray.shuffle() (Fisher-Yates)
├── presenter/
│   ├── Presenter.kt     # Presenter interface — mediates view <-> model
│   └── PresenterImpl.kt # Presenter implementation
└── view/
    ├── View.kt          # View interface — console display + command loop contract
    └── ViewImpl.kt       # Console I/O implementation
```

No `test/` directory exists yet.

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

None — pure Kotlin standard library (`java.util.Random` via `Extensions.kt`).
No build tool (Gradle/Maven) is checked into the repo yet; files are compiled
directly (e.g. via `kotlinc` or an IDE run configuration).
