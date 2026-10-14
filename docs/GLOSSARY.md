# Glossary

Canonical domain terms for this project. Skills and documentation use these names consistently.
When a new term is introduced during a `brief` session, add it here before creating the ticket.

## Core concepts

| Term | Definition | Code symbol |
|---|---|---|
| Board | The `SQUARE_SIDE × SQUARE_SIDE` grid of tiles holding the puzzle state | `BoardModel`, `BoardImpl` |
| Move | A run of consecutive shifts on the same row or column, in any direction; shifting a different line starts a new move, and so does the first shift after a load. Counted and shown as `Moves: N` | *(none yet — added by GH-62)* |
| Shift | Cyclically rotating one row (left/right) or column (up/down) by one position | `shiftLeft`, `shiftRight`, `shiftUp`, `shiftDown` |
| Square side | The board's edge length (default 4, giving a 4×4 board) | `SQUARE_SIDE` |
| Correct / solved | The board state where all values are in ascending order | `isCorrect()` |
| Save file | A local file storing a board's tile arrangement, created/updated via the `save` command and restored via `load` or the startup prompt | *(none yet — added by GH-6)* |

## Known aliases to avoid

| Avoid | Use instead |
|---|---|
| grid, matrix | board |
| rotate | shift |
| win, solved state | correct |
