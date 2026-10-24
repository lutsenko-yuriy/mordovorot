# Glossary

Canonical domain terms for this project. Skills and documentation use these names consistently.
When a new term is introduced during a `brief` session, add it here before creating the ticket.

## Core concepts

| Term | Definition | Code symbol |
|---|---|---|
| Board | The square side × square side grid of tiles holding the puzzle state | `BoardModel`, `BoardImpl` |
| Board size | The valid range of square sides a game can use: 3–5, default 4. Chosen via `--size=N`, the startup size prompt, or `[ New ]` | `BoardSize` |
| Current save name | The name of the save the current game was most recently loaded from or saved to; prefills the Save dialog. Cleared by a new game, not kept across launches (GH-99) | `CurrentSaveName`, `ViewModel.currentSaveName()` |
| Dialog | A modal box in the TUI (Save, Load, Exit, New-game size picker, Startup restore) that takes over input until confirmed or cancelled | `Dialog`, `DialogLayout` |
| Drag | Web client only: pressing on a tile and dragging slides its row (horizontal drag) or column (vertical drag) under the pointer, snapping to whole tiles on release. Each tile of distance is one shift; the whole drag is one move (GH-110) | — |
| Footer | Web client only: one muted line at the bottom of the page with the app version and the Loopover credit (Cary Huang, links to the original and loopover.xyz) (GH-111) | — |
| Give up | Ending a game by accepting Mordie's solution: `[ Give Up ]` (own row above the toolbar, enabled once Mordie has found its solution) asks for confirmation, then opens the solution screen. The player's moves are discarded (GH-126) | — |
| Input mode | Which front-end is driving the game: mouse TUI or keyboard TUI. Switchable mid-session without losing the game | `InputMode` |
| Move | A run of consecutive shifts on the same row or column, in any direction; shifting a different line starts a new move, and so does the first shift after a load. Counted and shown as `Moves: N` | `BoardModel.moveCount` / `ViewModel.moveCount()` |
| Line selection | Keyboard mode's selected row and selected column, one of them active; WASD selects, HJKL/arrows shift the active line. Its tiles are highlighted (GH-101) | — |
| Mordie | The computer opponent: searches in the background for the shortest solution of the game's original arrangement (in moves), reports its progress under `Moves:`, and its result against the player's move count becomes the solved title | `Mordie`, `MordieHooks`, `MordieText` |
| Public site | The web client published on GitHub Pages at `https://lutsenko-yuriy.github.io/mordovorot/`, updated on every merge to `master` whose build and tests pass (GH-111) | — |
| Save file | A local `.save` file under `saves/` storing a board's size and tile arrangement, created/updated via the Save dialog or the exit flow and restored via the Load dialog or the startup restore prompt | `SavedBoard`, `SaveRepository`, `FileSaveRepository` |
| Save info | A save's name plus its board size, as shown in every save listing (e.g. `foo (5x5)`) | `SaveInfo` |
| Shared module | One of the five Kotlin Multiplatform modules future Web/Android/iOS clients build on: Core (board), Analytics, Mordie, Storage (save contract only), ViewModel (game flows minus Exit and session control). Everything else stays in the terminal app (GH-41) | — |
| Shift | Cyclically rotating one row (left/right) or column (up/down) by one position | `shiftLeft`, `shiftRight`, `shiftUp`, `shiftDown` |
| Solution screen | The screen after giving up, or after a win via `[ Mordie's solution ]`: the original arrangement, Mordie's solution as a numbered step list (one step per move, e.g. `03. Col 3 ↑ ×1`), and Prev / Next / Restart to step through it on the board one shift at a time. Saved and loaded as such (GH-126) | — |
| Solved | The board state where all values are in ascending order. "Correct" is the board-model name for the same check | `isSolved()`, `isCorrect()` |
| Square side | The board's edge length (default 4, giving a 4×4 board) | `squareSide` |
| Startup restore prompt | The launch-time offer to restore an existing save before the first move (a TUI dialog) | `restoreOnStartup()` |
| Tile labels | How tiles are labelled, one of six schemes: Numbers (default, `1..side²`), Numbers from 0, Pairs (`23`), Pairs from 0 (`12`), Letters (`G`), Letter pairs (`BC`). Chosen in the Tile labels dialog; remembered as a preference and stored in saves, a save's labels winning for that game (GH-139) | — |
| Toolbar | The TUI's row of buttons below the board — `[ New ] [ Save ] [ Load ] [ Exit ]` — plus the input-mode switch row beneath it | `HitTarget.Toolbar*` |
| Unsaved progress | At least one shift since the last New game, successful save or successful load. The web client asks before the tab is closed or reloaded with it (GH-110) | — |
| Web client | The browser version of the game, mouse only and built on the shared modules, with its saves kept in the browser (GH-110) | — |

## Known aliases to avoid

| Avoid | Use instead |
|---|---|
| grid, matrix | board |
| rotate | shift |
| win, correct state | solved |
