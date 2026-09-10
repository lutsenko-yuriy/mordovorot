# Product Specification

<!-- Describe what your product does from the user's perspective.
     The Product Owner agent reads this file to understand intended behaviour.
     Be specific about user-facing features, screens, and flows. -->

## Overview

Mordovorot — A console prototype of a sliding-row/column puzzle board game

## Features

### Feature 1 — Board
- The game presents a square board of tiles laid out `SQUARE_SIDE × SQUARE_SIDE` (default 4×4), holding a shuffled permutation of the integers `0..SQUARE_SIDE²-1` internally, **displayed to the player as `1..SQUARE_SIDE²`** (GH-10).
- On starting a new game, the board is reshuffled via a Fisher-Yates shuffle.
- The board is considered "correct" (solved) when its values are in ascending order.

### Feature 2 — Moves
- The user can shift any row left or right, cyclically wrapping the value that falls off one end onto the other end.
- The user can shift any column up or down, with the same cyclic wrap-around behavior.
- Moves are addressed by a **1-based** row/column index (the first row/column is `1`, the last is `SQUARE_SIDE`); an index of `0` or greater than `SQUARE_SIDE` is rejected (GH-10).

### Feature 3 — Console interaction
- The user issues commands through a console (stdin) loop.
- After every command, the board is redisplayed and re-evaluated for the solved state.

### Feature 4 — Save and load (GH-6)
- `save <file-name>` stores the board's current tile arrangement to a local file, creating it
  or overwriting an existing file of that name. Works both at the start of a session and mid-game.
- `load <file-name>` restores the board's tile arrangement from an existing save file. Works
  mid-game, not just at startup.
- Loading an unknown file name, or a save whose board size doesn't match the current board,
  leaves the board untouched and shows a message (listing available saves, where relevant)
  instead of crashing.
- Saves are stored as `.save` files in a `saves/` directory relative to wherever the app is
  launched from (the process's current working directory) — running from a different directory
  will not see saves made from another one.
- **Startup restore prompt:** on launch, before the first move, the app checks for existing
  saves:
  - No saves — starts a new game immediately, no prompt.
  - Exactly one save — asks a yes/no question offering to restore it; declining (or EOF) starts
    a new game.
  - Two or more saves — lists all save names and asks the user to type one to restore, or press
    Enter for a new game. An unrecognized name re-prompts rather than silently starting a new
    game; EOF is treated the same as pressing Enter.
  - When saves exist, this prompt consumes stdin lines before the normal command loop starts —
    at least one (the yes/no answer, or the typed name) and, with two or more saves, one per
    unrecognized name typed, since each miss re-prompts on the next line rather than giving up.
    A script feeding commands via a pipe (e.g. `printf "left 1\n" | app`) must account for
    this, or its lines will be consumed as restore-prompt answers instead of reaching the
    command loop.

## Known gaps

- No documented win-condition message or exit command yet — see `src/main/kotlin/view/ViewImpl.kt`
  for current behavior and treat it as the source of truth until this section is expanded.
