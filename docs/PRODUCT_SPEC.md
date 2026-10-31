# Product Specification

<!-- Describe what your product does from the user's perspective.
     The Product Owner agent reads this file to understand intended behaviour.
     Be specific about user-facing features, screens, and flows. -->

## Overview

Mordovorot — A terminal version of Loopover, a sliding-row/column puzzle. A browser client,
published at https://lutsenko-yuriy.github.io/mordovorot/, plays the same game with the mouse (Feature 9).

## Features

### Feature 1 — Board
- The game presents a square board of tiles laid out `SQUARE_SIDE × SQUARE_SIDE` (default 4×4), holding a shuffled permutation of the integers `0..SQUARE_SIDE²-1` internally, **displayed to the player as `1..SQUARE_SIDE²`** (GH-10) by default. The player can pick other tile labels (numbers from 0, pairs, letters) in the Tile labels dialog (GH-139, Feature 11); the board, the solved screen and the solution screen all follow the choice, and the step list keeps `Row …`/`Col …`.
- On starting a new game, the board is reshuffled via a Fisher-Yates shuffle.
- Board sizes are 3×3 to 6×6 (6×6 since GH-160).
- Every new game can be solved (GH-74). On a 4×4 or 6×6 board every arrangement can be solved. On 3×3
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
- `--size=N` starts at board side `N` (3-6) and skips the startup restore and size prompts
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
  - A new game clears it (`[ New ]`/1, the startup size prompt, New game from the
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
- **Solution screen saves (GH-126):** a save also stores Mordie's solution once it has found
  one, and whether it was made on the solution screen (Feature 10). Loading a solution-screen
  save, by Load or the startup restore prompt, opens the solution screen again at the start of
  the replay.
- **Tile labels in saves (GH-139):** a save also stores the tile labels it was shown with, and
  loading it (Load, the startup restore, Repair & load) shows them again, ahead of the player's
  preference, until the next new game. A save from before GH-139, or one naming labels this
  build doesn't know, loads with the preference. Older builds still load a new save and ignore
  its labels.

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
  `◀`/`▶`/`▲`/`▼` arrows at the ends of each row/column, the `[ Give Up ]` row (Feature 10)
  below the down arrows, a toolbar (`[ New ] [ Save ] [ Load ] [ Exit ]`) below that, and a second row below that with
  `[ Labels ]` (GH-139) and `[ Controls 6 ]` (GH-170), which opens the **Controls** dialog;
  6 on the keyboard does the same, the one key mouse mode's board answers to, and the only button
  that shows its key.
  Clicking an arrow shifts that row/column; every click repaints the whole screen. `[ New ]`
  (GH-44) opens the same size-picker dialog as the startup prompt and starts a fresh game at
  whichever size is chosen; Cancel leaves the current game untouched.
- The **Controls** dialog (GH-170), in both modes: `Play with ● Mouse ○ Keyboard` with the
  current mode marked, `Keyboard keys ● Right-handed ○ Left-handed` with the current layout
  marked (Feature 7), then the keyboard mode's key list for the marked layout (board, toolbar and
  solution-screen keys; it changes as the mark moves), ending with `Letters: Latin, Ukrainian or
  Russian layout` (GH-200), then `[ OK ] [ Cancel ]`. Clicking an
  option marks it; from the keyboard (it opens focused on `Play with`) the arrows move the mark,
  Tab / Shift+Tab move focus over the groups, OK and Cancel, Enter is OK except on Cancel, and
  Escape cancels. OK applies the marked layout and remembers it in `saves/preferences`
  (`keyboard_layout=left_handed`); with the other mode marked it then switches to it without
  losing the current game, the keyboard selection included (GH-30). Cancel changes nothing. If the switch fails (e.g. no `stty`), the game stays in the current
  mode with `Could not switch to <mode> mode (<reason>) - staying in <current> mode.` (GH-79).
- `[ Labels ]` (GH-139) opens the **Tile labels** dialog: one row per scheme (Numbers, Numbers
  from 0, Pairs, Pairs from 0, Letters, Letter pairs), each with the solved board's first row as
  a sample, the current scheme marked `●` and the rest `○`. Clicking a row marks it; OK relabels
  the board at once and remembers the choice in `saves/preferences`; Cancel (or Escape) changes
  nothing. It works while playing, after a win and on the solution screen.
- If switching modes fails (e.g. the terminal can't be set up), the game carries on in the
  current mode with the same game and shows `Could not switch to <mode> mode (<reason>) -
  staying in <current> mode.` on its status line (GH-79).
- **Save/Load/Exit dialogs**, opened from the toolbar:
  - **Save** — a typed name field, prefilled with the current save name (Feature 4, GH-99)
    with the cursor at the end: typing appends, Backspace deletes one character. Latin and Cyrillic
    letters can be typed (Cyrillic since GH-200, in a UTF-8 terminal); other non-ASCII characters
    do nothing. An existing
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
- **Tile selection (GH-170, the keyboard scheme of ADR-0003):** keyboard mode doesn't draw the
  `◀`/`▶`/`▲`/`▼` arrows. One tile is selected: it is drawn in reverse video, the rest of its
  row on a pale yellow background and the rest of its column on light grey, with black text.
  The keys depend on the layout picked in the Controls dialog (right-handed by default):
  - Right-handed: **W/A/S/D** move the selected tile up/left/down/right, wrapping at the edges;
    **H/L** or **←/→** shift its row left/right; **K/J** or **↑/↓** shift its column up/down.
  - Left-handed: **H/J/K/L** or the arrows move the selected tile; **A/D** shift its row,
    **W/S** its column. The toolbar digits and the solution screen's keys are the same in both.
  - The selection stays where it is after a shift. Letters work in either case; Enter and
    Space do nothing on the board.
  - **Ukrainian and Russian keyboards (GH-200):** their letters act as the Latin keys in the same
    position, on the board and the solution screen: `ц` W, `ф` A, `і`/`ы` S, `в` D, `р` H, `о` J,
    `л` K, `д` L, `к` R. Other non-Latin layouts' letters do nothing; the terminal gets only
    characters, so it can't play by key position as the web does (Feature 9).
  - Row 1 / column 1 is selected at launch. A new game, a load or a size change keeps the
    selection, clamped to a smaller board (row 5 / column 2 on 5x5 becomes row 3 / column 2 on
    3x3); switching to mouse mode and back keeps it too.
  - The board stays centred, each hand's keys on its side: right-handed `W/A/S/D select` to its
    left and `←/→ H/L row` / `↑/↓ K/J column` to its right; left-handed `A/D row` / `W/S column`
    to its left and `←↑↓→ H/J/K/L select` to its right.
  - The layout is remembered between sessions in `saves/preferences` and kept across mode
    switches; a storage error just means it isn't remembered.
  - Once solved, the highlight disappears and the selection and shift keys go inert, matching
    mouse mode's dimmed arrows; the hints stay.
- **Toolbar digits (GH-170)**, shown in light blue on the toolbar buttons
  (`[New 1] [Save 2] [Load 3] [Exit Esc]`, then `[Labels 4] [Controls 6]`, mirroring mouse mode's
  `[ Labels ] [ Controls 6 ]` row) and always live regardless of the selection or solved state,
  on the solution screen too: **1** opens the New-game size picker (GH-44), **2** Save, **3**
  Load, **4** Tile labels (GH-139), **5** is the Give Up row's button (`[Give Up 5]` or
  `[Mordie's solution 5]`, GH-126), **6** opens the Controls dialog (Feature 6), and **Escape** opens the Exit
  dialog. Inside a dialog the digits are the dialog's own (they type into the Save name field).
  The function keys do nothing. A controls-hint line below the toolbar spells these out:
  `1 New · 2 Save · 3 Load · 4 Labels · 5 Give Up · 6 Controls · Esc Exit`.
- **Dialog navigation:** Tab/Shift+Tab (or the arrow keys) cycle focus between a dialog's
  controls (text field, list rows, buttons); Enter activates whatever has focus; Escape cancels
  the dialog, same as clicking Cancel. Typing and Backspace work on the focused text field as in
  the mouse mode's Save dialog. The Tile labels dialog (GH-139) opens focused on the marked
  scheme; the arrows move the mark, and Enter on a row confirms it, same as OK.
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
- **Mordie sits 6x6 out (GH-160)**, until a faster search exists (GH-116): no search, and for
  shifts 0-4 its line is `6x6 is too much for me, you're on your own!`, then empty. A loaded 6x6
  save shows the same line (and its Mordie lines, if any, are dropped at the next save). The
  solved title is `Congratulations ✓`, the Give Up row stays empty (no `[ Give Up ]`), and no `[ Mordie's solution ]`
  follows a win. In the terminal the line takes one row on 6x6, so the board fits 80x24.
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
- **Solution screen (GH-126):** Mordie's line reads `Here's how I did it in N moves!` there
  (Feature 10).
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
- **Running it:** the game is published at `https://lutsenko-yuriy.github.io/mordovorot/` (GH-111); for development,
  `./gradlew :web:serve -t` serves it on `http://localhost:8080`. Opening either address starts it. The tab title is `Mordovorot`, and the title appears nowhere else on the page.
  The terminal game is unchanged.
- **Same game:** the rules, board sizes 3-6, solvable new games, `Moves: N` and its
  move rule (Feature 2), Save/Load (Feature 4), Mordie (Feature 8) and Give Up with its solution
  screen (Feature 10) work as in the terminal game. It starts with the mouse; keyboard mode is below. There are no command-line flags and no Exit.
- **Dialogs from the keyboard (GH-163):** every dialog works without the mouse; the toolbar,
  the arrows and the solution screen's buttons are reached with Tab and pressed with Enter or
  Space, in either mode.
  - **Focus on open:** the size picker on the current board's size, Save on its name field, Load
    and `Restore a saved game?` on the first save (Cancel when there are none), `Can't be solved`
    on `Repair & load`, Tile labels on the marked scheme (and on each row picked), Give Up on No.
  - **Tab** and Shift+Tab cycle through the open dialog's controls only; the page behind it, the
    footer included, can't be reached, by Tab or by a screen reader. A screen reader names each
    dialog by its title.
  - **Escape** does what the dialog's Cancel does (Give Up: No; Restore: Cancel, so the size picker
    opens); `Can't be solved` has no Cancel, and Escape does nothing there.
  - **Focus on close** goes back to the button that opened the dialog, however it closed; to the
    next dialog's default when one leads straight to another (Restore → size picker, Load → `Can't
    be solved`); to `Next ▶` after Give Up → Yes (keyboard mode: the board); to the start of the page after a startup dialog.
  - Focus rings show only once a key has been pressed, so a dialog's default control isn't ringed
    for mouse players.
- **Focus after a press (GH-164):** a pressed arrow, toolbar button or solution-screen button keeps
  focus once the page has redrawn, without scrolling the page. When it's gone or disabled, focus
  goes on to the next thing that screen offers:
  - an arrow whose shift solved the board: `Mordie's solution` when shown, else `New`;
  - `Next ▶` at the last step: `◀ Prev`; `◀ Prev` at the first step, and `↻ Restart`: `Next ▶`;
  - `Mordie's solution` after a win: `Next ▶`, as after Give Up → Yes (keyboard mode: the board);
  - anything else: the start of the page.

  A held Enter presses the focused button once, not once per auto-repeat.
- **Keyboard mode (GH-171, the scheme of ADR-0003):** chosen in the Controls dialog and remembered
  with the layout in `localStorage`; a first visit is mouse mode, right-handed. Where the main input
  is touch, `[ Controls 6 ]` is hidden like the arrows and 6 does nothing, so the mode can't be
  reached, whatever is stored.
  - **Controls dialog** (`[ Controls 6 ]`, on its own row under the toolbar in both modes, or 6):
    `Play with ○ Mouse ○ Keyboard`, `Keyboard keys ○ Right-handed ○ Left-handed`, the marked layout's
    key list, `[ OK ] [ Cancel ]`. It opens on `Play with`'s marked option; Tab moves between the two
    groups, OK and Cancel, and the arrows move a group's mark. OK applies both; Cancel or Escape
    changes nothing.
  - **Toolbar:** every button shows its digit: `[ New 1 ][ Save 2 ][ Load 3 ][ Labels 4 ]`,
    `[ Give Up 5 ]` or `[ Mordie's solution 5 ]`, `[ Controls 6 ]`. The digits work wherever focus
    is, outside dialogs. In mouse mode only 6 works, and only its button shows its digit.
  - **Board:** one Tab stop, focused at page load in keyboard mode, on entering the mode and when a
    dialog opened from it closes. While it has focus, right-handed: W/A/S/D move the selected tile,
    wrapping; H/← L/→ shift its row, K/↑ J/↓ its column. Left-handed: H/J/K/L or the arrows move it;
    A/D shift its row, W/S its column. Letters work in either case, and held keys repeat.
  - **Non-Latin keyboards (GH-200):** on a non-Latin system keyboard layout, a letter key on the board
    or the solution screen does what the Latin key in its position does: the key typing `ц` is W.
    Hints and the key list keep naming the Latin letters. Latin layouts still act by the letter they
    type, so AZERTY and Dvorak work as ever. A known gap: a key that types ASCII punctuation on a
    non-Latin layout (Hebrew's W types `'`) can't be told from a Latin layout's and does nothing.
  - **Selection:** a rounded rectangle around the selected row and one around its column, crossing at
    the selected tile, which has a thicker ring; the arrows are hidden, their room kept. It stays put
    after a shift, starts on row 1, column 1, survives a new game, a load, a size change (clamped) and
    a mode switch, and goes once solved, when the select and shift keys go inert. A shift that solves
    the board passes focus on as an arrow's would.
  - **Mouse in keyboard mode:** drags and clicks work as usual. A press on a tile selects it and gives
    the board focus, so after a drag the selection sits on the dragged line.
  - **Key hints:** the keys above what they do, under the board, ordered by hand:
    `W A S D` select, `← → H L` row, `↑ ↓ K J` column; left-handed `A D` row, `W S` column,
    `←↑↓→ H J K L` select. The solution screen shows its own: `← → A D H L` prev/next, `Space Enter`
    next, `R` restart, `↑ ↓ W S K J` scroll.
  - **Solution screen:** Give Up → Yes and `Mordie's solution` put focus on the board, a Tab stop here
    too; the keys of Feature 10 work while it has focus.
  - **0** reads the board, as Read board does, during play, wherever focus is outside dialogs. It has
    no button or hint; the key list names it. Without a screen reader it does nothing you can see or
    hear.
  - **What a screen reader hears:** the reader says the selected cell itself, in VoiceOver
    `delta, column 4, row 2`, on a selection move and when the board takes focus; the page adds
    nothing, so it isn't said twice. A shift, by key or drag, says what it says in mouse mode, then
    the tile now under the selection: `Row 2 left, 5 moves. Now charlie`. The solution screen's board
    is named after the current step, `Board, step 3 of 12: Column 3 up 2`, `Board, start, step 0 of
    12`. Tested only by one sighted developer in VoiceOver, and frozen until volunteers try it (`docs/CONSTRAINTS.md`); NVDA and JAWS are untested, and `NVDA+Space` frees stuck keys.
- **Screen reader announcements (GH-166):** a screen reader is told what just happened, politely
  (after what it's reading) and without moving focus. Always on; nothing shows on the page.
  - **A shift:** `Row 2 left`, `Column 4 up`; with the move count when it starts a new move
    (`Row 2 left, 5 moves`, `1 move` for one). A drag is said once, on release, with how far
    (`Row 2 left 2`); nothing mid-drag, and nothing for a release back where it started.
  - **The win:** only `Solved in M moves`; after it, nothing more from the board.
  - **The status line:** every message, as it appears; the same message twice is said twice.
  - **A new game:** `New 4x4 game`, from New, at startup with no restore (the size picked or
    cancelled), and after `Can't be solved` → New game. Cancel on New says nothing.
  - **A load:** `Loaded 'foo', 5x5, 12 moves`, `Loaded 'foo', 5x5, solved in 14 moves`, or for a
    save on the solution screen `Loaded 'foo', 5x5, solution, 12 moves` (its steps), from Load and
    the startup restore; the status line still shows `Loaded 'foo'.`
  - **The solution screen:** `Solution, 12 moves` on entry; Next and Prev say the step now current
    in words, `Step 3 of 12: Column 3 up 2` (a two-shift step is said again on its second Next);
    Restart, and Prev back to the start, say `Start, step 0 of 12`.
  - Mordie's lines are never said, and the board only when asked (GH-167, below).
- **Board for screen readers (GH-167):** during play, a screen reader can read the board. Nothing
  changes on screen.
  - **A table:** the tiles are a table named `Board`, a row per board row, each tile a cell
    saying its label, plus `in place` when it is home (`5, in place`). Letters are said in the
    phonetic alphabet, so `B` and `D` don't sound alike: `D` is `delta`, `BC` `bravo charlie`. VoiceOver's table keys
    (Ctrl+Option+arrows) go cell by cell. While a line slides or is dragged, the table stays on
    the board as it was before, so a tile pushed past one end is read once.
  - **Read board:** a button, hidden from sight, first in Tab order, that says the board row by
    row through the announcements above, then how many tiles are home: `Row 1: 5, 2, 9, 1. Row
    2: … 3 tiles in place.` (`1 tile in place`, `0 tiles in place`). It uses the labels shown, letters
    phonetically (`Row 1: delta, alpha, …`),
    the Tile labels dialog's preview too. Pressing it changes nothing on the page, and focus
    stays on it. A shift that solves the board while it has focus passes focus on as an arrow's
    would (GH-164).
  - While a dialog is open, neither can be reached. The solved board and the solution screen
    have neither, and read as before.
- **Toolbar:** `[ New ] [ Save ] [ Load ] [ Labels ]`; the player leaves by closing the tab.
  - **New** opens the size picker (3x3, 4x4, 5x5, 6x6); Cancel leaves the current game untouched.
  - **Save** prefills the current save name and warns before overwriting. A blank name counts as
    Cancel. A rejected name (spaces, a path separator, `..`) saves nothing, and the message
    shows on the status line.
  - **Load** lists each save as `foo (5x5)`, marking unsolvable ones; `No saves found.` when
    there are none.
  - The `Can't be solved` dialog offers `[ Repair & load ] [ New game ]`, with no Exit.
  - **Labels** opens the Tile labels dialog (GH-139, Feature 11).
- **Status line** under the toolbar: messages such as `Saved as 'x'.` and load errors. The next
  action clears it.
- **Saves** are kept in the browser's `localStorage`, one entry per save in the terminal's save
  format. Each address has its own: saves on the public site and on `localhost:8080` don't see
  each other (GH-111). They survive reloads and browser restarts, and are separate from
  the terminal game's `saves/`.
- **Startup:** with saves, the page opens with `Restore a saved game?` (Cancel opens the size
  picker); without, it opens the size picker, whose Cancel keeps the board already shown.
- **Leave-site prompt:** with unsaved progress (at least one shift since the last New game,
  successful save or successful load), closing or reloading the tab shows the browser's own
  "Leave site?" prompt. Safari shows it on reload but not when the tab is closed; that is
  Safari's choice, and the page can't change it. The solution screen never prompts: entering it
  discards the player's moves (GH-126).
- **Arrows:** `▲` above and `▼` below each column, `◀`/`▶` at both ends of each row. A click
  shifts the line by one tile, sliding it with wrap-around.
  - **Touch screens (GH-111):** where the main input is touch (phones, tablets), the arrows
    aren't shown and the board grows into their room; dragging is the only way to shift (screen
    readers still find the arrow buttons).
    Touchscreen laptops keep the arrows.
  - **Touch gestures (GH-145):** a double tap doesn't zoom the page, and a long press or a drag
    doesn't select the game's text or offer to copy it. The save name field and the footer stay
    selectable.
  - **No zoom on touch screens (GH-149):** a pinch doesn't zoom the page either, and focusing the
    save name field doesn't zoom into it. Desktop zoom (keys, wheel, trackpad) still works. Players
    who need bigger text get it from their system's text size instead (GH-168).
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
- **Contrast (GH-165):** text, tile labels, buttons and the Save field's edge meet WCAG AA
  contrast (4.5:1 for text, 3:1 for edges) in light and dark. The tiles are the same pale colours
  in both, with near-black labels. The colours only hint at a tile's home: to colour-blind
  players some rows look alike, so the tile labels carry it.
- **Reduced motion (GH-162):** when the system asks for reduced motion, nothing animates: shifts
  and Mordie's lines appear at once, and dialogs, buttons and the win show without fading or
  glowing. A change of the setting applies with the tab open.
- **Layout:**
  - **Wider than tall:** the board with its arrows on the left; on the right, top to bottom,
    `Moves: N`, Mordie's text, the toolbar and the status line.
  - **Taller than wide, or square:** top to bottom, `Moves: N`, Mordie's text, the board, the
    toolbar and the status line.
  - The `[ Give Up ]` row sits directly above the toolbar in both (GH-126).
  - **Solution screen (GH-126):** wide puts the step list between the board and the panel, as
    tall as the tiles; tall puts it under the board, a few steps high. Either way it scrolls when
    the steps don't fit, and keeps the current step in view. `[ ◀ Prev ] [ Next ▶ ] [ ↻ Restart ]`
    sit above the toolbar. The board keeps its place, with its arrows hidden, wherever the window's
    height limits the board; where its width does (narrow wide windows), the steps' column takes
    room and the board is smaller there (GH-148).
  - **Wide fits the window (GH-148):** wider than tall, the page is exactly the window's height
    and never scrolls or bounces, as when tall: the tiles shrink to fit beside the panel and above
    the footer, down to 16px, in every browser. Landscape phones get tighter spacing: under 500px
    tall smaller gaps; under 340px tall the panel starts level with the board's top, Mordie's text
    is smaller and keeps two lines, and `Moves` is smaller; under 760px wide the panel, the gap
    and the steps' column are narrower. A dialog taller than the window scrolls inside itself.
  - **Tall fits the window (GH-147):** taller than wide, the page is exactly the window's height
    (the part the browser's own toolbars leave) and never scrolls: the board takes the room the
    rest leaves and its tiles shrink to fit, down to 16px. Pulling the page past its edge neither
    bounces nor refreshes it.
    Browsers without CSS size container queries (before Safari 16, Firefox 110, Chrome 105) keep
    the older estimate, which can scroll on short phones.
  - The toolbar's buttons stay on one row, tighter on screens under 380px wide (GH-147) and on
    wide ones under 760px wide (GH-148).
  - **Larger text (GH-168):** the page's text follows the text size the player set outside the
    game: the browser's font size, Android's text scaling (Chrome for Android 156 and later) or
    iOS Dynamic Type, accessibility sizes included. Tile labels grow with it until they fill
    their tile. A smaller setting leaves the text at the standard size, and a change shows from
    the next page load. Where the larger text no longer fits, the board shrinks down to 16px tiles,
    then the page scrolls vertically; rows of buttons wrap, and the page never scrolls sideways.
    At the standard size the page is as above.
  - The footer sits at the very bottom in both (GH-111).
- **Footer (GH-111):** one small muted line,
  `v<version> · Based on Loopover by Cary Huang (carykh) · original · loopover.xyz`. The version is
  the app's current one, stamped in when the page is built. `original` links to the game on
  OpenProcessing and `loopover.xyz` to the community site, both in a new tab so the game in this
  one is kept. Dialogs cover it like the rest of the page. Taller than wide and under 480px wide
  it is one short line without the links, `v<version> · Based on Loopover by carykh` (GH-147).
  Wider than tall it is one line of fixed height, cut off with `…` if a window is ever too narrow
  for it (GH-148).
- **Public site / Updates (GH-111):** every merge to `master` whose build and tests pass
  publishes the new version; a failed build leaves the last good one up. GitHub may serve the
  previous version for up to ~10 minutes after that. An open tab keeps the version it loaded
  until it's reloaded; the footer's version shows which one is running.
- **Analytics:** every event carries `platform: web` and `input_method`, following the actual input
  in either mode: `keyboard` for key presses, `rich_ui` for mouse and touch; see `docs/ANALYTICS_EVENTS.md`.

### Feature 10 — Give Up and Mordie's solution (GH-126)
Applies to terminal mouse mode, terminal keyboard mode and the web client.
- **Give Up:** `[ Give Up ]` sits on its own row, directly above the toolbar. It is dimmed and
  inert until Mordie has found its solution (the `Found it!` moment, Feature 8), so on boards
  where Mordie never finishes (often 5x5) it stays dimmed. On 6x6, which Mordie sits out, it isn't
  shown at all (GH-160). Keyboard mode: 5. Pressing it asks
  `Give up and see Mordie's solution?` `[ Yes ] [ No ]`: No (or Escape) keeps playing; Yes
  discards the player's moves and opens the solution screen.
- **After a win:** the row shows `[ Mordie's solution ]` instead (keyboard mode: 5) if Mordie
  had found its solution by then. It opens the solution screen with no confirmation. After the
  `You beat me!` ending the row is empty.
- **Solution screen:**
  - The board goes back to the game's original arrangement and isn't playable: no arrows, no
    drag, no shift keys. `Moves: N` is hidden, Mordie's line reads
    `Here's how I did it in N moves!` (`1 move` for one), and the Give Up row is empty.
  - **Step list:** one numbered step per Mordie move, `<n>. Row <i> <→|←> ×<k>` or
    `<n>. Col <i> <↑|↓> ×<k>`, each the shortest way round its line (a tie goes → or ↓). Numbers
    are zero-padded to the total's width (`01.` to `12.`). It sits right of the board (the
    terminal, the wide web) or below it (the tall web), scrolls when it doesn't fit (the mouse
    wheel in mouse mode and on the web; ↑/↓, W/S or K/J in keyboard mode), and keeps the current step
    in view.
  - **Stepping, by hand:** `[ ◀ Prev ] [ Next ▶ ] [ ↻ Restart ]`. Next applies one shift of the
    current step, Prev undoes one, Restart goes back to the original arrangement. Prev and
    Restart are dimmed at the start, Next on the solved board. The step of the last shift made
    is highlighted: reverse video in the terminal, a coloured background on the web. On the web
    each shift slides one tile, as a player's shift does.
  - **Keyboard mode:** ←/→, A/D or H/L are Prev/Next, Space and Enter are Next too, R is
    Restart, and ↑/↓, W/S or K/J scroll the steps, in both layouts; side hints (terminal, GH-170) or
    hints under the board (web, while the board has focus, GH-171) spell these out.
  - **Toolbar:** New, Save, Load (and Exit in the terminal) work as before. New and Load leave
    the screen. Saving there saves the solution screen (Feature 4). Switching input mode in the
    terminal keeps the screen and the replay's place.
  - The replay never counts moves, feeds Mordie, solves the game or sets unsaved progress.
- **Older saves:** a save from before GH-126 that has Mordie's found N but not its solution
  can't show the solution: Give Up stays dimmed, and the after-win row stays empty.

### Feature 11 — Tile labels (GH-139)
Applies to terminal mouse mode, terminal keyboard mode and the web client.
- **Schemes**, shown for the 0-based tile N on a side-S board:

  | Scheme | Label | 4x4 solved first row |
  |---|---|---|
  | Numbers (default) | N + 1 | `1 2 3 4` |
  | Numbers from 0 | N | `0 1 2 3` |
  | Pairs | solved row and column, from 1 | `11 12 13 14` |
  | Pairs from 0 | solved row and column, from 0 | `00 01 02 03` |
  | Letters | `A` + N | `A B C D` |
  | Letter pairs | solved row and column as letters | `AA AB AC AD` |

  Only the text changes: a tile's colour (web) and the step list's `Row …`/`Col …` stay as they are.
- **The dialog:** `Tile labels`, one row per scheme with the current board size's first solved
  row as a sample, the current scheme marked `●` and the rest `○`. Picking a row marks it; OK
  relabels the board at once, Cancel changes nothing. On the web the board already shows the
  marked scheme while the dialog is open, and Cancel puts the old labels back. It opens from `[ Labels ]` in both clients
  (terminal keyboard mode: `[Labels 4]` and 4; details in Features 6, 7 and 9), during play,
  after a win and on the solution screen. Escape cancels it in both clients (on the web since GH-163).
- **Which labels a game shows:** a loaded save's own labels (Feature 4) win for that game; then
  the remembered preference; then Numbers. A new game goes back to the preference.
- **Letters on 6x6 (GH-160):** Letters has 26 labels, too few for 36 tiles. On a 6x6 board its row
  in the dialog is dimmed, reads `up to 5x5` instead of a sample, and can't be picked or focused.
  A Letters preference shows Numbers on 6x6 and stays Letters, so smaller boards keep letters; OK
  on those Numbers changes nothing. Loading a 6x6 save with Letters (only possible by hand-editing)
  opens the dialog by itself, with `Letters don't fit a 6x6 board (26 letters, 36 tiles) - pick
  other labels.` above the rows and Numbers marked: OK applies the pick (and makes it the
  preference), Cancel keeps Numbers. Either way the next save stores the labels shown. The
  `Loaded '<name>'.` message follows once the dialog closes.
- **The preference:** OK on a different scheme makes it the preference too. OK on the scheme
  already shown changes nothing, so it doesn't adopt a loaded save's labels. The terminal keeps
  it in `saves/preferences` (`tile_labels=pairs`); the browser in `localStorage`, per address
  like its saves. If it can't be written the board still relabels, silently; it just isn't
  remembered. Changing the labels isn't unsaved progress.
