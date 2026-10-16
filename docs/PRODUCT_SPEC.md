# Product Specification

<!-- Describe what your product does from the user's perspective.
     The Product Owner agent reads this file to understand intended behaviour.
     Be specific about user-facing features, screens, and flows. -->

## Overview

Mordovorot — A terminal version of Loopover, a sliding-row/column puzzle

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
  - Shown as `Moves: N` on the row under the title (still there under `Congratulations ✓`).
  - A new game (`[ New ]`, the startup size prompt) starts at `Moves: 0`.
  - Switching input mode keeps the count.
  - Saves store the count; loading restores it, and the next shift always starts a new move. A
    save made before GH-62 loads as `Moves: 0`.

### Feature 3 — Launch (GH-79)
- The game only runs as a terminal UI (Features 6 and 7). The line-based console mode was
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
  - **Yes** opens the Save dialog and saves under the typed name, then quits. An empty name
    quits without saving.
  - A name that isn't usable as a save identifier - containing spaces, a path separator, or
    `..` - is rejected with a message and asked for again, rather than silently discarding the
    save request.
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
  - **Save** — a typed name field; an existing name shows an inline "already exists - it will
    be overwritten" warning before saving over it. A name containing spaces is rejected with an
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
- **Solved state:** once the board is solved, the title switches to `Congratulations ✓`, the
  shift arrows are shown dimmed and stop responding to clicks, and the toolbar (New/Save/Load/Exit)
  stays fully clickable. Loading a different, unsolved save from the Congratulations screen
  brings the arrows and title straight back — the screen doesn't need a fresh launch or a
  separate "keep playing" action.

### Feature 7 — Keyboard-only TUI (GH-18)
- `--keyboard` runs the same TUI screens as Feature 6 (board, toolbar, dialogs, Congratulations
  screen), but driven entirely from the keyboard instead of the mouse. Launch rules are the same
  as mouse mode (Feature 3).
- **Board navigation:** arrow keys move a highlighted cursor around the ring of shift arrows
  (`◀`/`▶`/`▲`/`▼`); the cursored arrow is shown in yellow on a reverse-video cell, so it stays visible without color. Enter or Space activates whichever
  arrow is highlighted, same effect as clicking it. Once solved, the cursor disappears and arrow
  keys/Enter go inert, matching the mouse mode's dimmed-arrows behavior.
- **Toolbar shortcuts**, shown in light blue on the toolbar buttons — including a second row
  below `[ New ] [ Save ] [ Load ] [ Exit ]` for `[Mouse F7]` (GH-30), mirroring the mouse
  mode's `[ Keyboard ]` row — and always live regardless of cursor position or solved state:
  **F5** opens Save, **F6** opens Load, **F9** opens the New-game size picker (GH-44), **F7**
  switches to mouse mode, and **Escape** opens the Exit dialog. F8 does nothing (it switched to
  console mode before GH-79). A controls-hint line below the toolbar spells these out in plain
  text: `Arrows: move · Enter: shift · F5/F6/F9 Save/Load/New · F7 Mouse · Esc`.
- **Dialog navigation:** Tab/Shift+Tab (or the arrow keys) cycle focus between a dialog's
  controls (text field, list rows, buttons); Enter activates whatever has focus; Escape cancels
  the dialog, same as clicking Cancel. Typing and Backspace work on the focused text field as in
  the mouse mode's Save dialog.
- All other dialog and startup-restore behavior (validation, re-prompting) is identical to
  Feature 6 — only the input method differs, which shows up in analytics as an `input_method`
  property of `keyboard` instead of `mouse`.
