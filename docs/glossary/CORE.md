# Glossary — Core

Core game: the board, its moves, Mordie and saves — what every client shares. Part of the [glossary](../GLOSSARY.md).

| Term | Definition | Code symbol |
|---|---|---|
| Board | The square side × square side grid of tiles holding the puzzle state | `BoardModel`, `BoardImpl` |
| Board size | The valid range of square sides a game can use: 3–6, default 4 (6x6 since GH-160; Mordie sits 6x6 out until GH-116). Chosen via `--size=N`, the startup size prompt, or `[ New ]` | `BoardSize` |
| Current save name | The name of the save the current game was most recently loaded from or saved to; prefills the Save dialog. Cleared by a new game, not kept across launches (GH-99) | `CurrentSaveName`, `ViewModel.currentSaveName()` |
| Give up | Ending a game by accepting Mordie's solution: `[ Give Up ]` (own row above the toolbar, enabled once Mordie has found its solution) asks for confirmation, then opens the solution screen. The player's moves are discarded (GH-126) | — |
| Mordie | The computer opponent: searches in the background for the shortest solution of the game's original arrangement (in moves), reports its progress under `Moves:`, and its result against the player's move count becomes the solved title | `Mordie`, `MordieHooks`, `MordieText` |
| Move | A run of consecutive shifts on the same row or column, in any direction; shifting a different line starts a new move, and so does the first shift after a load. Counted and shown as `Moves: N` | `BoardModel.moveCount` / `ViewModel.moveCount()` |
| Save file | A local `.save` file under `saves/` storing a board's size and tile arrangement, created/updated via the Save dialog or the exit flow and restored via the Load dialog or the startup restore prompt | `SavedBoard`, `SaveRepository`, `FileSaveRepository` |
| Save info | A save's name plus its board size, as shown in every save listing (e.g. `foo (5x5)`) | `SaveInfo` |
| Shared module | One of the five Kotlin Multiplatform modules future Web/Android/iOS clients build on: Core (board), Analytics, Mordie, Storage (save contract only), ViewModel (game flows minus Exit and session control). Everything else stays in the terminal app (GH-41) | — |
| Shift | Cyclically rotating one row (left/right) or column (up/down) by one position | `shiftLeft`, `shiftRight`, `shiftUp`, `shiftDown` |
| Solution screen | The screen after giving up, or after a win via `[ Mordie's solution ]`: the original arrangement, Mordie's solution as a numbered step list (one step per move, e.g. `03. Col 3 ↑ ×1`), and Prev / Next / Restart to step through it on the board one shift at a time. Saved and loaded as such (GH-126) | — |
| Solved | The board state where all values are in ascending order. "Correct" is the board-model name for the same check | `isSolved()`, `isCorrect()` |
| Square side | The board's edge length (default 4, giving a 4×4 board) | `squareSide` |
