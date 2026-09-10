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
- Not yet implemented: the startup restore prompt (offering to resume an existing save when the
  app launches) — tracked as GH-6's remaining work unit.
- Saves are stored as `.save` files in a `saves/` directory relative to wherever the app is
  launched from (the process's current working directory) — running from a different directory
  will not see saves made from another one.

## Known gaps

- No documented win-condition message or exit command yet — see `src/main/kotlin/view/ViewImpl.kt`
  for current behavior and treat it as the source of truth until this section is expanded.
- No startup save/restore prompt yet (GH-6, in progress).
