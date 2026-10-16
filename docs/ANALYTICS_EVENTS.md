# Analytics Events

All events are sent via the analytics service abstraction (a thin wrapper around the underlying analytics SDK).

---

## Events

<!-- Document each event below. Example format: -->

<!--
### `event_name`

Fired when <trigger condition>.

| Property | Type | Description |
|---|---|---|
| `property_name` | `string` | Description |
-->

### `save_command_used`

Fired when the user saves via the Save dialog (toolbar Save / F5) or the exit flow. *(GH-6)* The name dates from the removed console `save` command and is kept for continuity (GH-79).

| Property | Type | Description |
|---|---|---|
| `result` | `string` | `success` or `error` (e.g. an unsafe name, or a filesystem failure such as a full disk). |
| `overwrote_existing` | `boolean` | Whether a file with that name already existed. Only present when `result` is `success` - on `error` it may not be known (e.g. the name check itself failed). |
| `input_method` | `string` | `mouse` or `keyboard` — which UI mode the session was running in. *(GH-3, GH-18)* *(`console` retired — GH-79)* |

### `load_command_used`

Fired when the board is restored from a save file, either via the `load` command mid-game or from the startup restore prompt. *(GH-6)*

| Property | Type | Description |
|---|---|---|
| `trigger` | `string` | `command` (a mid-game load: toolbar Load / F6 - the value predates GH-79 and is kept) or `startup_prompt`. |
| `result` | `string` | `success` (also covers a load that resized the board to the save's size, GH-44 WU5), `not_found`, `error` (corrupted save file or a filesystem failure), or `unsolvable` (GH-74 - the save can't be solved; `unsolvable_save_resolved` carries the outcome). |
| `input_method` | `string` | `mouse` or `keyboard` — which UI mode the session was running in. *(GH-3, GH-18)* *(`console` retired — GH-79)* |

### `startup_restore_prompt_shown`

Fired when the app boots and finds one or more save files. *(GH-6)*

| Property | Type | Description |
|---|---|---|
| `save_file_count` | `number` | How many save files were found. |
| `input_method` | `string` | `mouse` or `keyboard` — which UI mode the session was running in. *(GH-3, GH-18)* *(`console` retired — GH-79)* |

### `startup_restore_decision`

Fired when the user resolves the startup restore prompt. *(GH-6)*

| Property | Type | Description |
|---|---|---|
| `decision` | `string` | `restored`, `new_game`, or `quit` (GH-74 - Exit chosen for an unsolvable save). Repairing an unsolvable save counts as `restored`. |
| `save_file_count` | `number` | How many save files were on offer. |
| `input_method` | `string` | `mouse` or `keyboard` — which UI mode the session was running in. *(GH-3, GH-18)* *(`console` retired — GH-79)* |

### `exit_command_used`

Fired when the user exits via the Exit dialog (toolbar Exit / Escape) **and the session actually ends** - not fired if the
user asked to save but the save attempt failed, since the session keeps running in that case
instead of quitting (see Feature 5 in `docs/PRODUCT_SPEC.md`). *(GH-12)*

| Property | Type | Description |
|---|---|---|
| `save_choice` | `string` | `saved` (the save-before-quitting prompt was accepted and the save succeeded) or `declined` (the user said no, or left the save name blank/EOF - never fired for a failed save attempt, which keeps the session open instead). The save outcome itself (success/error/overwrite) is separately reported by `save_command_used`, since this reuses the `save` command's flow. |
| `input_method` | `string` | `mouse` or `keyboard` — which UI mode the session was running in. *(GH-3, GH-18)* *(`console` retired — GH-79)* |

### `app_launched`

Fired once at startup, after the launch mode is resolved (default mouse TUI or `--keyboard`), when a game actually starts - a launch with no terminal fires `app_launch_aborted` instead (GH-79). *(GH-3, GH-18)*

| Property | Type | Description |
|---|---|---|
| `mode` | `string` | `mouse` or `keyboard`. *(`console` retired — GH-79)* |
| `board_size` | `number` | The resolved board side (3-5) for this launch — from `--size=N` or the default (4). *(GH-44)* |

*GH-79:* new property `console_flag_ignored` (`boolean`, always sent) - `true` when `--console` was passed and the `Console mode was removed` notice was shown before the TUI started.

### `dialog_cancelled`

Fired when the user cancels the Save, Load, Exit, or Size dialog in the mouse-driven or keyboard-driven TUI (mouse click, or Escape/Cancel-control in keyboard mode) without completing the action. *(GH-3, GH-18, GH-44)*

| Property | Type | Description |
|---|---|---|
| `dialog` | `string` | `save`, `load`, `exit`, or `size`. |
| `input_method` | `string` | `mouse` or `keyboard` — which TUI mode the session was running in. *(GH-18)* |

### `input_mode_switched`

Fired when the user requests a switch to a *different* input mode mid-session — toolbar button (mouse mode) or keyboard shortcut (keyboard mode). Requesting the mode that's already active is a no-op instead: no event, no message, no exception (audit finding on PR #36). *(GH-30)*

| Property | Type | Description |
|---|---|---|
| `from_mode` | `string` | `mouse` or `keyboard` — mode active before the switch. *(`console` retired — GH-79)* |
| `to_mode` | `string` | `mouse` or `keyboard` — requested target mode. *(`console` retired — GH-79)* |
| `trigger` | `string` | `toolbar` or `shortcut`. *(`command` retired — GH-79)* |
| `result` | `string` | Always `success`. *(`rejected_no_tty` retired — GH-79: a running TUI always has a terminal)* |

### `new_game_size_selected`

Fired when a fresh game starts at a chosen board size via one of the post-launch surfaces (the
`--size` CLI flag is covered by `app_launched.board_size` instead). *(GH-44)*

| Property | Type | Description |
|---|---|---|
| `size` | `number` | The board side chosen (3-5). |
| `trigger` | `string` | `startup_prompt` or `toolbar` (`[ New ]` → size-picker dialog). *(`command` retired — GH-79)* |
| `input_method` | `string` | `mouse` or `keyboard` — which UI mode the session was running in. *(`console` retired — GH-79)* |

*GH-66:* `trigger` also took `play_again` - the console size prompt after accepting `Start a new game?` on a solved board. *(Retired — GH-79)*

*GH-74:* `trigger` also takes `unsolvable_save` - the size prompt after choosing New game for an unsolvable save mid-game. At startup that choice falls through to the regular startup size prompt (`startup_prompt`).

### `unsolvable_save_resolved`

Fired when the player picks what to do with an unsolvable save, in any input mode. Save names are
never sent. *(GH-74)*

| Property | Type | Description |
|---|---|---|
| `choice` | `string` | `repair`, `new_game`, or `exit`. |
| `trigger` | `string` | `command` (mid-game load: toolbar Load / F6) or `startup_prompt`. |
| `board_size` | `number` | The save's board side (3 or 5). |
| `repair_saved` | `boolean` | Only on `repair`: whether the repaired board was written back to the save file (`false` if the overwrite failed; the board is loaded either way). |
| `input_method` | `string` | `mouse` or `keyboard`. *(`console` retired — GH-79)* |

### `board_solved`

Fired once when a shift solves the board during play, in either input mode. Like
`screen_congratulations`, it does **not** fire when an already-solved save is restored. *(GH-62)*

| Property | Type | Description |
|---|---|---|
| `move_count` | `number` | The final move count shown to the player (a move is a run of consecutive shifts on one row/column). |
| `board_size` | `number` | The board side (3-5). |
| `input_method` | `string` | `mouse` or `keyboard` — which UI mode the session was running in. *(`console` retired — GH-79)* |

*GH-83:* new properties `mordie_result` (`string`: `player_won` - Mordie hadn't found its solution, `matched` - the move count equals Mordie's N, `mordie_won` - the move count exceeds N, or `machine` - the move count is below N, found or not, which only an inconsistent save can produce) and `mordie_moves` (`number`: Mordie's N when the board was solved - the true minimum if found, otherwise its progress).

### `play_again_decision`

**Retired — GH-79:** console mode was removed, so this event is no longer fired.

Fired when the player answers the console's `Start a new game? [y/N]` prompt, shown after the
solved board and Congratulations message - including when an already-solved save was loaded. *(GH-66)*

| Property | Type | Description |
|---|---|---|
| `decision` | `string` | `new_game` (`y`/`yes` - the size prompt follows) or `quit` (anything else, Enter, or EOF - the session ends). |
| `input_method` | `string` | Always `console` - the prompt only exists in console mode. |

### `help_command_used`

**Retired — GH-79:** console mode was removed, so this event is no longer fired.

Fired when the player runs `help` in console mode. *(GH-67)*

| Property | Type | Description |
|---|---|---|
| `scope` | `string` | `all` (bare `help`), `command` (`help <known command>`), or `unknown_topic` (`help <unknown>` - falls back to the full list). The unknown word itself is never sent - free text may carry PII. |
| `command` | `string` | The matched command name (e.g. `left`, `exit`). Only present when `scope` is `command`. |
| `input_method` | `string` | Always `console` - `help` only exists in console mode. |

### `console_command_rejected`

**Retired — GH-79:** console mode was removed, so this event is no longer fired.

Fired when a console command is rejected with an error plus the `Enter help to list all the
commands` hint - `size <N>` out of range included. Not fired for `load` of an unknown save,
whose message is unchanged. *(GH-67)*

| Property | Type | Description |
|---|---|---|
| `reason` | `string` | `unknown_command`, `empty`, `missing_argument`, `too_many_arguments`, `not_a_number`, or `out_of_range`. |
| `command` | `string` | The known command's name. Absent for `unknown_command` and `empty` - the raw typed verb is never sent, since free text may carry PII. |
| `input_method` | `string` | Always `console`. |

### `app_launch_aborted`

Fired when a launch finds no interactive terminal, prints the `No interactive terminal found`
message and exits - instead of `app_launched`, which only fires when a game starts. *(GH-79)*

| Property | Type | Description |
|---|---|---|
| `reason` | `string` | Always `no_interactive_terminal` for now. |
| `requested_mode` | `string` | `mouse`, `keyboard`, `console`, or `none` - the mode flag passed at launch (`none` when no flag was given). With several flags, `keyboard` beats `mouse` beats `console`. |

### `mordie_solution_found`

Fired when a 5-shift check first reveals that Mordie has found its solution, so the `Found it!`
line appears during play. Not fired when a save with Mordie's solution already found is
loaded. *(GH-83)*

| Property | Type | Description |
|---|---|---|
| `board_size` | `number` | The board side (3-5). |
| `solution_length` | `number` | Mordie's N - the true minimum solution length. |
| `player_shift_count` | `number` | The player's shift count at the revealing check. |
| `search_duration_ms` | `number` | Wall-clock search time in this session (time from earlier sessions before a save isn't included). |
| `resumed_from_save` | `boolean` | Whether the search resumed from a saved N. |
| `input_method` | `string` | `mouse` or `keyboard`. |

<!-- All events above are sent through `analytics.AnalyticsService`, currently backed by
     `analytics.NoopAnalyticsService` (no real SDK wired up yet). -->

---

## Screen Views

<!-- Document screen views tracked via the analytics service. Example format: -->

<!--
| Screen name | When tracked |
|---|---|
| `screen_name` | When the screen opens |
-->

| Screen name | When tracked |
|---|---|
| `screen_save_dialog` | Save dialog opens in the TUI (toolbar click/F5, or via the Exit dialog's "Yes"). Property `opened_from`: `toolbar` or `exit_flow`. *(GH-3)* |
| `screen_load_dialog` | Load dialog opens in the TUI (toolbar click/F6, or startup restore when saves exist). Properties `opened_from`: `toolbar` or `startup`; `save_file_count`: number. *(GH-3)* |
| `screen_exit_dialog` | Exit dialog opens in the TUI (Exit button click, or Escape in keyboard mode). *(GH-3, GH-18)* |
| `screen_congratulations` | A shift (click or keyboard Enter/Space) solves the board while the TUI is active - fires once on that transition. Does **not** fire when an already-solved save is restored (toolbar Load or startup restore); the Congratulations screen still renders, but nothing was solved *by playing* this session. *(GH-3)* |
| `screen_size_dialog` | TUI size-picker dialog opens. Property `opened_from`: `toolbar` (via `[ New ]`), `startup` (no `--size` given, restore prompt declined/empty), or `unsolvable_save` (New game chosen for an unsolvable save mid-game, GH-74). *(GH-44)* |
| `screen_unsolvable_dialog` | TUI `Can't be solved` dialog opens for an unsolvable save. Property `opened_from`: `toolbar` (Load button/F6) or `startup` (restore dialog). *(GH-74)* |
