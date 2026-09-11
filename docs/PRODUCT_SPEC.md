# Product Specification

<!-- Describe what your product does from the user's perspective.
     The Product Owner agent reads this file to understand intended behaviour.
     Be specific about user-facing features, screens, and flows. -->

## Overview

Mordovorot — A console prototype of a sliding-row/column puzzle board game

## Features

### Feature 1 — Board
- The game presents a square board of tiles laid out `SQUARE_SIDE × SQUARE_SIDE` (default 4×4), holding a shuffled permutation of the integers `0..SQUARE_SIDE²-1` internally, **displayed to the player as `1..SQUARE_SIDE²`** (GH-10).
- On starting a new game, the board is reshuffled via a Fisher-Yates shuffle.
- The board is considered "correct" (solved) when its values are in ascending order.

### Feature 2 — Moves
- The user can shift any row left or right, cyclically wrapping the value that falls off one end onto the other end.
- The user can shift any column up or down, with the same cyclic wrap-around behavior.
- Moves are addressed by a **1-based** row/column index (the first row/column is `1`, the last is `SQUARE_SIDE`); an index of `0` or greater than `SQUARE_SIDE` is rejected (GH-10).

### Feature 3 — Console interaction
- The user issues commands through a console (stdin) loop.
- After every command, the board is redisplayed and re-evaluated for the solved state.

### Feature 4 — Save and load (GH-6)
- `save <file-name>` stores the board's current tile arrangement to a local file, creating it
  or overwriting an existing file of that name. Works both at the start of a session and mid-game.
- `load <file-name>` restores the board's tile arrangement from an existing save file. Works
  mid-game, not just at startup.
- Loading an unknown file name, or a save whose board size doesn't match the current board,
  leaves the board untouched and shows a message (listing available saves, where relevant)
  instead of crashing.
- Saves are stored as `.save` files in a `saves/` directory relative to wherever the app is
  launched from (the process's current working directory) — running from a different directory
  will not see saves made from another one.
- **Startup restore prompt:** on launch, before the first move, the app checks for existing
  saves:
  - No saves — starts a new game immediately, no prompt.
  - Exactly one save — asks a yes/no question offering to restore it; declining (or EOF) starts
    a new game.
  - Two or more saves — lists all save names and asks the user to type one to restore, or press
    Enter for a new game. An unrecognized name re-prompts rather than silently starting a new
    game; EOF is treated the same as pressing Enter.
  - When saves exist, this prompt consumes stdin lines before the normal command loop starts —
    at least one (the yes/no answer, or the typed name) and, with two or more saves, one per
    unrecognized name typed, since each miss re-prompts on the next line rather than giving up.
    A script feeding commands via a pipe (e.g. `printf "left 1\n" | app`) must account for
    this, or its lines will be consumed as restore-prompt answers instead of reaching the
    command loop.

### Feature 5 — Exit command (GH-12)
- `exit` or `quit` (case-insensitive, no arguments) ends the current session on demand, the
  same as today's EOF (Ctrl+D) or board-solved exits.
- Before quitting, it asks `Save before quitting? [y/N]`:
  - `y`/`yes` — prompts for a save name and saves under it (same underlying flow as the `save`
    command), then quits. A blank name or EOF at this second prompt quits without saving.
  - A name that isn't usable as a save identifier - containing spaces (unloadable via the
    `load` command's single-token parsing), a path separator, or `..` - is rejected with a
    message and re-prompted, rather than silently discarding the save request.
  - If the save itself fails (e.g. a read-only `saves/` directory), the session **does not
    quit** — the error is shown (same message as a failed `save` command) and play continues,
    so an explicit save request never costs the player their game.
  - Anything other than `y`/`yes` (including EOF) at the first prompt — quits without saving.
  - Like the startup restore prompt, these consume extra stdin lines before the process exits
    (or, on a failed save, before play resumes) — a scripted run piping commands must account
    for the yes/no answer and, if given, the save name(s).

### Feature 6 — Mouse-driven TUI (GH-3)
- On launch, the app runs a mouse-clickable terminal UI by default: a centered board with
  `◀`/`▶`/`▲`/`▼` arrows at the ends of each row/column, and a toolbar (`[ Save ] [ Load ]
  [ Exit ]`) below it. Clicking an arrow shifts that row/column the same as the console's
  `left`/`right`/`up`/`down` commands; every click repaints the whole screen.
- `--console` forces the line-based console interaction (Features 1-5) instead. Launching
  without an interactive terminal (e.g. piped/scripted input) falls back to console mode
  automatically, since mouse mode has no terminal to click in.
- **Save/Load/Exit dialogs**, opened from the toolbar, replace the console's typed prompts with
  clickable modals:
  - **Save** — a typed name field; an existing name shows an inline "already exists - it will
    be overwritten" warning before saving over it. A name containing spaces is rejected with an
    on-screen message instead of being saved (the console's `load` command couldn't read it
    back). An empty name (Enter, or clicking Save with nothing typed) cancels without saving.
  - **Load** — a clickable list of save names (`No saves found.` when there are none, with the
    Load button inert); selecting one and confirming restores it.
  - **Exit** — `[ Yes ] [ No ] [ Cancel ]`; Cancel closes the dialog and keeps playing, Yes/No
    behave like the console's `exit`/`quit` save-first prompt (Feature 5), including the
    invalid-name re-prompt and the failed-save-keeps-playing behavior.
  - Any dialog can also be dismissed with Escape, same effect as its Cancel button.
- **Startup restore**, when saves exist, is the same Load-shaped dialog (titled "Restore a saved
  game?") regardless of how many saves there are — the console's one-save-yes/no vs.
  two-or-more-saves-list split (Feature 4) doesn't apply here; Cancel starts a new game.
- **Solved state:** once the board is solved, the title switches to `Congratulations ✓`, the
  shift arrows are shown dimmed and stop responding to clicks, and the toolbar (Save/Load/Exit)
  stays fully clickable. Loading a different, unsolved save from the Congratulations screen
  brings the arrows and title straight back — the screen doesn't need a fresh launch or a
  separate "keep playing" action.

## Known gaps

- Console mode (`--console` or a non-interactive launch) has no documented win-condition
  message — see `src/main/kotlin/view/ViewImpl.kt` for current behavior and treat it as the
  source of truth until this section is expanded. The mouse-driven TUI's solved state (Feature
  6) is unaffected — its Congratulations screen is documented above.
