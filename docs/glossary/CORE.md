# Glossary — Core

Core game: the board, its moves, Mordie and saves — what every client shares. Part of the [glossary](../GLOSSARY.md).

| Term | Definition | Code symbol |
|---|---|---|
| Board | The square side × square side grid of tiles holding the puzzle state | `BoardModel`, `BoardImpl` |
| Board size | The valid range of square sides a game can use: 3–9, default 4 (6x6 since GH-160; 7–9 since GH-207, see Very large). Chosen via the startup size prompt or `[ New ]` (the terminal's `--size=N` went with GH-228) | `BoardSize` |
| Current save name | The name of the save the current game was most recently loaded from or saved to; prefills the Save dialog. Cleared by a new game, not kept across launches (GH-99) | `CurrentSaveName`, `GameViewModel.currentSaveName()` |
| Exact search | Mordie's search for the shortest solution (iterative-deepening A*): proves each length impossible in turn, so the first solution it finds is the shortest. Too slow to finish on 5x5 and up (GH-116) | `IdaStarSolver` |
| Fast solver | Mordie's quick solver: finds a good solution almost at once, though not necessarily the shortest: places the tiles one at a time with 3-cycles, on every size 3x3 to 9x9. Mordie runs it alongside the exact search (GH-214, GH-222) | `ThreeCycleSolver`, `BestSolutionSolver` |
| Best solution so far | The shortest solution Mordie has found, from the fast solver or the exact search; the exact search replaces the fast answer when it finds a shorter one. Mordie's N is its length (GH-222) | `BestSolutionSolver` |
| Proven shortest | Whether the best solution so far is the true minimum: the exact search has finished, or has ruled out every shorter length (GH-222) | `SearchProgress.provenShortest` |
| Give up | Ending a game by accepting Mordie's solution: `[ Give Up ]` (own row above the toolbar, enabled once Mordie has found its solution) asks for confirmation, then opens the solution screen. The player's moves are discarded (GH-126) | — |
| Mordie | The computer opponent: searches in the background for the shortest solution of the game's original arrangement (in moves), reports its progress under `Moves:`, and its result against the player's move count becomes the solved title | `Mordie`, `MordieHooks`, `MordieText` |
| Move | A run of consecutive shifts on the same row or column, in any direction; shifting a different line starts a new move, and so does the first shift after a load. Counted and shown as `Moves: N` | `BoardModel.moveCount` / `GameViewModel.moveCount()` |
| Save file | A save in the browser's `localStorage` storing a board's size and tile arrangement, created/updated via the Save dialog and restored via the Load dialog or the startup restore prompt | `SavedBoard`, `SaveRepository`, `BrowserSaveRepository` |
| Delete (a save) | Removing a save from the browser's storage, from the trash icon on its row in the Load or startup restore dialog, after a `Delete 'foo'?` confirmation (GH-251); `SaveRepository.delete`, `GameViewModel.deleteSave` | remove, erase |
| Save info | A save's name plus its board size, as shown in every save listing (e.g. `foo (5x5)`) | `SaveInfo` |
| Shared module | One of the five Kotlin Multiplatform modules the web client and future Android/iOS clients build on: Core (board), Analytics, Mordie, Storage (save contract only), ViewModel (the game flows). Everything else is in the web client (GH-41, GH-228) | — |
| Shift | Cyclically rotating one row (left/right) or column (up/down) by one position | `shiftLeft`, `shiftRight`, `shiftUp`, `shiftDown` |
| Solution screen | The screen after giving up, or after a win via `[ Mordie's solution ]`: the original arrangement, Mordie's solution as a numbered step list (one step per move, e.g. `03. Col 3 ↑ ×1`), and Prev / Next / Restart to step through it on the board one shift at a time. Saved and loaded as such (GH-126) | `SolutionScreen`, `SolutionReplay` |
| Solved | The board state where all values are in ascending order. "Correct" is the board-model name for the same check | `isSolved()`, `isCorrect()` |
| Square side | The board's edge length (default 4, giving a 4×4 board) | `squareSide` |
