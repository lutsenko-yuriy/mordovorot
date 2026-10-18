# Product Specification

<!-- Describe what your product does from the user's perspective.
     The Product Owner agent reads this file to understand intended behaviour.
     Be specific about user-facing features, screens, and flows. -->

## Overview

Mordovorot — A terminal version of Loopover, a sliding-row/column puzzle. A browser client on
`localhost` plays the same game with the mouse (Feature 9).

## Features

### Feature 1 — Board
- The game presents a square board of tiles laid out `SQUARE_SIDE × SQUARE_SIDE` (default 4×4), holding a shuffled permutation of the integers `0..SQUARE_SIDE²-1` internally, **displayed to the player as `1..SQUARE_SIDE²`** (GH-10).
- On starting a new game, the board is reshuffled via a Fisher-Yates shuffle.
- Every new game can be solved (GH-74). On a 4×4 board every arrangement can be solved. On 3×3
  and 5×5 boards, only arrangements an even number of tile swaps away from the solved order can
  be solved. If the shuffle lands on one that can't, two tiles are swapped to fix it. Every board
  that can be solved stays equally likely.
- The board is considered "correct" (solved) when its values are in ascending order.

### Feature 2 — Shifts and moves
- The user can shift any row left or right, cyclically wrapping the value that falls off one end onto the other end.
- The user can shift any column up or down, with the same cyclic wrap-around behavior.
- **Move counter (GH-62):** a *move* is a run of consecutive shifts on the same row or column, in
  either direction - shifting row 1 left, left, then right is one move; row 1, then column 1,
  then row 1 again is three. Row 1 and column 1 are different lines.
  - Shown as `Moves: N` on the row under the title (still there under the solved title).
  - A new game (`[ New ]`, the startup size prompt) starts at `Moves: 0`.
  - Switching input mode keeps the count.
  - Saves store the count; loading restores it, and the next shift always starts a new move. A
    save made before GH-62 loads as `Moves: 0`.

### Feature 3 — Launch (GH-79)
- The terminal game only runs as a terminal UI (Features 6 and 7); the browser client
  (Feature 9) is launched separately. The line-based console mode was
  removed in GH-79.
- Launching without an interactive terminal (e.g. `./gradlew run`, piped or scripted input), with
  any flags, prints `No interactive terminal found - Mordovorot only runs as a terminal UI.` to
  stderr and exits normally (status 0) without starting a game.
- `--console` in a terminal prints `Console mode was removed - starting the terminal UI.` and
  then starts the TUI as if the flag weren't there (`--console --keyboard` starts keyboard mode).
- `--size=N` starts at board side `N` (3-5) and skips the startup restore and size prompts
  (Feature 4). An unusable value warns and falls back to 4. Unrecognized arguments are reported
  and ignored.

### Feature 4 — Save and load (GH-6)
- The Save dialog stores the board's current tile arrangement to a local file, creating it or
  overwriting an existing file of that name. The Load dialog restores the board from an existing
  save. Both work at any point in a game.
- A save that can't be loaded (deleted or corrupted since it was listed) leaves the board
  untouched and shows a message instead of crashing. Loading a save whose board size differs
  from the current board resizes the board to match and restores its tiles - it is never
  rejected for a size mismatch (GH-44).
- Every place a save's name is listed - the startup restore dialog and the Load dialog - also
  shows that save's board size, e.g. `foo (5x5)`, so a player can tell up front what size
  loading it will resize the board to (GH-44). A save whose size can't be read (corrupted)
  shows its name alone.
- A save that no sequence of shifts can solve (only possible on 3×3/5×5, e.g. a hand-edited or
  pre-GH-74 save) is marked in every listing: `foo (3x3) unsolvable` (GH-74).
- Loading an unsolvable save (mid-game or at startup) doesn't restore it. Instead it opens a
  `Can't be solved` dialog with three choices (GH-74, see Feature 6):
  - **Repair** swaps two tiles, which makes the board solvable, and overwrites the save file with
    the result, keeping its move count, then loads it: `Repaired 'foo' (two tiles swapped) and
    loaded it.` If the file can't be rewritten, the repaired board is still loaded with a warning,
    and the save stays marked.
  - **New game** opens the board-size picker.
  - **Exit** at startup quits right away, since there's no game to save. Mid-game, it runs the
    `Save before quitting?` flow (Feature 5); a failed save keeps the current game going.
- **Current save name (GH-99):** the Save dialog opens with the name of the save the game was
  last loaded from or saved to already typed in, so saving back to it is just Enter.
  - A successful load sets it (Load dialog, startup restore, Repair & load), and so does a
    successful save under any name: load `foo`, save as `bar`, and the next Save dialog shows
    `bar`.
  - A new game clears it (`[ New ]`/F9, the startup size prompt, New game from the
    `Can't be solved` dialog), as does a fresh launch - it isn't stored anywhere.
  - A failed save or load, or a cancelled dialog, leaves it unchanged. Switching input mode
    keeps it.
- Saves are stored as `.save` files in a `saves/` directory relative to wherever the app is
  launched from (the process's current working directory) — running from a different directory
  will not see saves made from another one. The save file format is unchanged by GH-79, so older
  saves still load.
- **Startup restore prompt:** on launch, before the first move, the app checks for existing
  saves:
  - No saves — skips straight to the board-size prompt (GH-44).
  - One or more saves — opens the Load-shaped "Restore a saved game?" dialog (Feature 6).
    Cancelling it falls through to the board-size prompt.
  - This entire prompt - restore included - is skipped outright when `--size=N` was given at
    launch (GH-44): the flag is the answer, no matter what saves exist.

### Feature 5 — Exit (GH-12)
- The toolbar Exit (Escape in keyboard mode) ends the current session on demand after asking
  `Save before quitting?`:
  - **Yes** opens the Save dialog (prefilled with the current save name, GH-99) and saves under
    the typed name, then quits. An empty name quits without saving.
  - A name that isn't usable as a save identifier - containing spaces, a path separator, or
    `..` - is rejected with a message and asked for again, rather than silently discarding the
    save request. The message stays above any overwrite warning while the name is retyped
    (GH-99).
  - If the save itself fails (e.g. a read-only `saves/` directory), the session **does not
    quit** — the error is shown and play continues, so an explicit save request never costs the
    player their game.
  - **No** quits without saving; **Cancel** closes the dialog and keeps playing.

### Feature 6 — Mouse-driven TUI (GH-3)
- On launch, the app runs a mouse-clickable terminal UI by default: a centered board with
  `◀`/`▶`/`▲`/`▼` arrows at the ends of each row/column, a toolbar
  (`[ New ] [ Save ] [ Load ] [ Exit ]`) below it, and a second row below that with
  `[ Keyboard ]` for switching to keyboard mode without losing the current game (GH-30).
  Clicking an arrow shifts that row/column; every click repaints the whole screen. `[ New ]`
  (GH-44) opens the same size-picker dialog as the startup prompt and starts a fresh game at
  whichever size is chosen; Cancel leaves the current game untouched.
- If switching modes fails (e.g. the terminal can't be set up), the game carries on in the
  current mode with the same game and shows `Could not switch to <mode> mode (<reason>) -
  staying in <current> mode.` on its status line (GH-79).
- **Save/Load/Exit dialogs**, opened from the toolbar:
  - **Save** — a typed name field, prefilled with the current save name (Feature 4, GH-99)
    with the cursor at the end: typing appends, Backspace deletes one character. An existing
    name shows an inline "already exists - it will be overwritten" warning before saving over
    it - straight away when the field is prefilled. A name containing spaces is rejected with an
    on-screen message instead of being saved. An empty name (Enter, or clicking Save with
    nothing typed) cancels without saving.
  - **Load** — a clickable list of save names and sizes (e.g. `foo (5x5)`, GH-44 - `No saves
    found.` when there are none, with the Load button inert); selecting one and confirming
    restores it.
  - **Exit** — `[ Yes ] [ No ] [ Cancel ]`, behaving as Feature 5 describes.
  - Any dialog can also be dismissed with Escape, same effect as its Cancel button - except the
    two below.
- **Unsolvable save** (GH-74): loading one (Load dialog or startup restore) opens a `Can't be
  solved` dialog with `[ Repair & load ] [ New game ] [ Exit ]` (Feature 4). It has no Cancel,
  and Escape does nothing. New game opens the size picker. Exit at startup quits; mid-game it
  opens a `Save before quitting?` dialog with Yes/No only (no Cancel, Escape does nothing),
  which then behaves like the Exit dialog's Yes/No.
- **Startup restore**, when saves exist, is the same Load-shaped dialog (titled "Restore a saved
  game?") regardless of how many saves there are; Cancel starts a new game.
- **Solved state:** once the board is solved, the title switches to Mordie's ending (Feature 8),
  or to `Congratulations ✓` when Mordie sat the game out, the
  shift arrows are shown dimmed and stop responding to clicks, and the toolbar (New/Save/Load/Exit)
  stays fully clickable. Loading a different, unsolved save from the Congratulations screen
  brings the arrows and title straight back — the screen doesn't need a fresh launch or a
  separate "keep playing" action.

### Feature 7 — Keyboard-only TUI (GH-18)
- `--keyboard` runs the same TUI screens as Feature 6 (board, toolbar, dialogs, Congratulations
  screen), but driven entirely from the keyboard instead of the mouse. Launch rules are the same
  as mouse mode (Feature 3).
- **Line selection (GH-101):** keyboard mode doesn't draw the `◀`/`▶`/`▲`/`▼` arrows. It
  remembers a selected row and a selected column, one of them active, and shows the active
  line's tiles in reverse video.
  - **W/S** move the row selection up/down; with a column active they switch to the remembered
    row without moving it. **A/D** do the same for columns. Movement wraps at the edges.
  - **H/L** or **←/→** shift the selected row left/right; **J/K** or **↓/↑** shift the selected
    column down/up. A shift key on the other axis switches the selection to the remembered line
    on that axis instead of shifting. Letters work in either case; Enter and Space do nothing
    on the board.
  - Row 1 is active at launch, with column 1 remembered. A new game, a load or a size change
    keeps the selection, each number clamped to a smaller board (row 4 / column 2 on 5x5 becomes
    row 3 / column 2 on 3x3); switching to mouse mode and back keeps it too.
  - The board stays centred: `W/S row` / `A/D column` sit to its left, `←/→ H/L shift row` /
    `↑/↓ K/J shift column` to its right.
  - Once solved, the highlight disappears and the selection and shift keys go inert, matching
    mouse mode's dimmed arrows; the hints stay.
- **Toolbar shortcuts**, shown in light blue on the toolbar buttons — including a second row
  below `[ New ] [ Save ] [ Load ] [ Exit ]` for `[Mouse F7]` (GH-30), mirroring the mouse
  mode's `[ Keyboard ]` row — and always live regardless of the selection or solved state:
  **F5** opens Save, **F6** opens Load, **F9** opens the New-game size picker (GH-44), **F7**
  switches to mouse mode, and **Escape** opens the Exit dialog. F8 does nothing (it switched to
  console mode before GH-79). A controls-hint line below the toolbar spells these out in plain
  text: `F5/F6/F9 Save/Load/New · F7 Mouse · Esc` (the board keys have their own side hints,
  GH-101).
- **Dialog navigation:** Tab/Shift+Tab (or the arrow keys) cycle focus between a dialog's
  controls (text field, list rows, buttons); Enter activates whatever has focus; Escape cancels
  the dialog, same as clicking Cancel. Typing and Backspace work on the focused text field as in
  the mouse mode's Save dialog.
- All other dialog and startup-restore behavior (validation, re-prompting) is identical to
  Feature 6 — only the input method differs, which shows up in analytics as an `input_method`
  property of `keyboard` instead of `mouse`.

### Feature 8 — Mordie, the computer opponent (GH-83)
- **Search:** when a game starts (`[ New ]`, the startup size prompt, `--size=N`), Mordie starts
  searching in the background for the shortest solution of the game's original arrangement,
  counted in moves (Feature 2). The player's moves don't affect it, and the board stays fully
  playable. Mordie's N is the longest solution length it has ruled out so far; once it has found
  a solution, N is that solution's length, the true minimum. On 5x5 the search may never finish
  within a game; Mordie then just keeps reporting progress.
- **Mordie's line** sits under `Moves: N`: `Mordie:` in magenta, then the text, word-wrapped to
  about the width of the board and toolbar, each row centered. Its rows are always reserved, so
  the board doesn't move as the line comes and goes. Mouse and keyboard mode show the same text,
  and switching mode keeps all of Mordie's state.
- The line follows the player's **shifts** (not moves, no timer). The count starts at 0 when a
  game starts or a save is loaded, and isn't saved. Progress is checked every 5 shifts:

  | Shifts | Mordie's line |
  |---|---|
  | 0-4 | `Alright! Let's sort this one out!` (after a load, the load line below) |
  | 5-19 | empty |
  | 20-24 | `I have already made N moves and have not solved it yet!` |
  | 25-39 | empty |
  | ...5 shown, 15 empty, repeating... | |

  - A progress window is skipped (the line stays empty) when N is the same as the last N Mordie
    told the player: its last progress line, 0 for a new game, or the saved N after a load.
  - From the 60th shift on, the progress line starts with `This one is hard! `.
  - When a check finds that Mordie has its solution, the line switches at once, even in an empty
    stretch, to `Found it! I had to make N moves to find the solution! Can you beat it?`, and
    stays until the board is solved.
- **Endings:** when the player solves the board in M moves, the title shows one of these instead
  of `Congratulations ✓`, and the line disappears:
  - Mordie hasn't found its solution: `You beat me! I was still at N moves.`
  - M = N: `Cool, you matched me! N moves each!`
  - M > N: `Sorry, but I win: N moves vs your M.`
  - M < N (only possible with a hand-edited save):
    `I made N moves and you made M moves! Are you a machine?`
  - A count of 1 reads `1 move`.
- **Saves** also store the original arrangement, Mordie's N and whether it had found its
  solution (two extra lines; older saves still load).
  - Loading a save where Mordie was still searching resumes the search past the saved N, and the
    first line (shifts 0-4) is `Okay, so we stopped when I was at N moves! What was I doing?..`
  - Loading a save where Mordie had found its solution shows the `Found it!` line at once.
  - Loading a solved save shows its ending, or `Congratulations ✓` if it has no Mordie data.
  - A save with no Mordie data (made before GH-83), or one fixed by Repair (GH-74): if no moves
    have been made on it, Mordie starts fresh from the loaded board. If moves have been made,
    Mordie sits the game out: no line, the solved title is `Congratulations ✓`, and the move
    count is kept. Its N would only cover the moves from the load on, so the race wouldn't be
    fair.

### Feature 9 — Browser client (GH-110)
- **Running it:** `./gradlew :web:serve` serves the game on `http://localhost:8080`; opening that
  address starts it. The tab title is `Mordovorot`, and the title appears nowhere else on the page.
  The terminal game is unchanged.
- **Same game, mouse only:** the rules, board sizes 3-5, solvable new games, `Moves: N` and its
  move rule (Feature 2), Save/Load (Feature 4) and Mordie (Feature 8) work as in the terminal
  game. There is no keyboard mode, no command-line flags and no Exit.
- **Toolbar:** `[ New ] [ Save ] [ Load ]`; the player leaves by closing the tab.
  - **New** opens the size picker (3x3, 4x4, 5x5); Cancel leaves the current game untouched.
  - **Save** prefills the current save name and warns before overwriting. A blank name counts as
    Cancel. A rejected name (spaces, a path separator, `..`) saves nothing, and the message
    shows on the status line.
  - **Load** lists each save as `foo (5x5)`, marking unsolvable ones; `No saves found.` when
    there are none.
  - The `Can't be solved` dialog offers `[ Repair & load ] [ New game ]`, with no Exit.
- **Status line** under the toolbar: messages such as `Saved as 'x'.` and load errors. The next
  action clears it.
- **Saves** are kept in the browser's `localStorage` for `localhost:8080`, one entry per save in
  the terminal's save format. They survive reloads and browser restarts, and are separate from
  the terminal game's `saves/`.
- **Startup:** with saves, the page opens with `Restore a saved game?` (Cancel opens the size
  picker); without, it opens the size picker, whose Cancel keeps the board already shown.
- **Leave-site prompt:** with unsaved progress (at least one shift since the last New game,
  successful save or successful load), closing or reloading the tab shows the browser's own
  "Leave site?" prompt. Safari shows it on reload but not when the tab is closed; that is
  Safari's choice, and the page can't change it.
- **Arrows:** `▲` above and `▼` below each column, `◀`/`▶` at both ends of each row. A click
  shifts the line by one tile, sliding it with wrap-around.
- **Dragging:** press a tile and drag. Once the pointer has moved a few pixels, the larger
  direction picks the tile's row or column. The line follows the pointer with wrap-around, and
  on release it slides to the nearest whole tile.
  - Each tile of distance is one shift (Mordie counts them all), and the whole drag is one move.
  - A drag of a full loop or more counts modulo the side, keeping its direction: a full loop
    changes nothing.
  - Only where the drag is released counts: passing through the solved arrangement mid-drag
    doesn't solve the board.
  - Releasing at the starting position changes nothing. The toolbar and arrows wait until the
    drag is released.
- **Solved:** the arrows fade out, and neither clicks nor drags shift anything. Mordie's ending,
  or `Congratulations ✓` when Mordie sat the game out, replaces Mordie's text in the same spot.
- **Mordie's lines come and go like chat messages:** the old line rises towards `Moves: N` and
  fades out; only once it has gone does the new one rise into place and fade in, from under the
  toolbar in the wide layout, from just below its spot in the tall one. A change while one is
  moving waits for it, then the latest line follows. `Mordie:` is bold and coloured.
- **Look:** rounded coloured tiles (each row its own hue, darker along the row, so a tile's home
  shows at a glance), real buttons and modal dialogs, in light or dark to match the system.
- **Layout:**
  - **Wider than tall:** the board with its arrows on the left; on the right, top to bottom,
    `Moves: N`, Mordie's text, the toolbar and the status line.
  - **Taller than wide, or square:** top to bottom, `Moves: N`, Mordie's text, the board, the
    toolbar and the status line.
  - The board scales to fit the window.
- **Analytics:** every event carries `platform: web` and `input_method: rich_ui`;
  see `docs/ANALYTICS_EVENTS.md`.
