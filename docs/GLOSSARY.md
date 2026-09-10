# Glossary

Canonical domain terms for this project. Skills and documentation use these names consistently.
When a new term is introduced during a `brief` session, add it here before creating the ticket.

## Core concepts

| Term | Definition | Code symbol |
|---|---|---|
| Board | The `SQUARE_SIDE × SQUARE_SIDE` grid of tiles holding the puzzle state | `BoardModel`, `BoardImpl` |
| Shift | Cyclically rotating one row (left/right) or column (up/down) by one position | `shiftLeft`, `shiftRight`, `shiftUp`, `shiftDown` |
| Square side | The board's edge length (default 4, giving a 4×4 board) | `SQUARE_SIDE` |
| Correct / solved | The board state where all values are in ascending order | `isCorrect()` |

## Known aliases to avoid

| Avoid | Use instead |
|---|---|
| grid, matrix | board |
| rotate, move | shift |
| win, solved state | correct |
