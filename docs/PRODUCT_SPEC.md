# Product Specification

<!-- Describe what your product does from the user's perspective.
     The Product Owner agent reads this file to understand intended behaviour.
     Be specific about user-facing features, screens, and flows. -->

## Overview

Mordovorot — A console prototype of a sliding-row/column puzzle board game

## Features

### Feature 1 — Board
- The game presents a square board of tiles laid out `SQUARE_SIDE × SQUARE_SIDE` (default 4×4), holding a shuffled permutation of the integers `0..SQUARE_SIDE²-1`.
- On starting a new game, the board is reshuffled via a Fisher-Yates shuffle.
- The board is considered "correct" (solved) when its values are in ascending order.

### Feature 2 — Moves
- The user can shift any row left or right, cyclically wrapping the value that falls off one end onto the other end.
- The user can shift any column up or down, with the same cyclic wrap-around behavior.
- Moves are addressed by row/column index.

### Feature 3 — Console interaction
- The user issues commands through a console (stdin) loop.
- After every command, the board is redisplayed and re-evaluated for the solved state.

## Known gaps

- No documented command syntax, win-condition message, or exit command yet — see `src/main/kotlin/view/ViewImpl.kt` for current behavior and treat it as the source of truth until this section is expanded.
- No automated tests, build tool (Gradle/Maven), or CI exist yet.
