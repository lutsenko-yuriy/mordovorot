# Glossary

Canonical domain terms for this project. Skills and documentation use these names consistently.
When a new term is introduced during a `brief` session, add it here before creating the ticket.

## Core concepts

| Term | Definition | Code symbol |
|---|---|---|
| Board | The square side × square side grid of tiles holding the puzzle state | `BoardModel`, `BoardImpl` |
| Board size | The valid range of square sides a game can use: 3–5, default 4. Chosen via `--size=N`, the startup size prompt, `size <N>`, or `[ New ]` | `BoardSize` |
| Dialog | A modal box in the TUI (Save, Load, Exit, New-game size picker, Startup restore) that takes over input until confirmed or cancelled | `Dialog`, `DialogLayout` |
| Input mode | Which front-end is driving the game: mouse TUI, keyboard TUI, or console. Switchable mid-session without losing the game | `InputMode` |
| Move | A run of consecutive shifts on the same row or column, in any direction; shifting a different line starts a new move, and so does the first shift after a load. Counted and shown as `Moves: N` | `BoardModel.moveCount` / `ViewModel.moveCount()` |
| Save file | A local `.save` file under `saves/` storing a board's size and tile arrangement, created/updated via the `save` command and restored via `load` or the startup restore prompt | `SavedBoard`, `SaveRepository`, `FileSaveRepository` |
| Save info | A save's name plus its board size, as shown in every save listing (e.g. `foo (5x5)`) | `SaveInfo` |
| Shift | Cyclically rotating one row (left/right) or column (up/down) by one position | `shiftLeft`, `shiftRight`, `shiftUp`, `shiftDown` |
| Solved | The board state where all values are in ascending order. "Correct" is the board-model name for the same check | `isSolved()`, `isCorrect()` |
| Square side | The board's edge length (default 4, giving a 4×4 board) | `squareSide` |
| Startup restore prompt | The launch-time offer to restore an existing save before the first move (console prompt or TUI dialog) | `restoreOnStartup()` |
| Toolbar | The TUI's row of buttons below the board — `[ New ] [ Save ] [ Load ] [ Exit ]` — plus the input-mode switch row beneath it | `HitTarget.Toolbar*` |

## Known aliases to avoid

| Avoid | Use instead |
|---|---|
| grid, matrix | board |
| rotate | shift |
| win, correct state | solved |
