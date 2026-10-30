# Analytics Events

All events are sent via the analytics service abstraction (a thin wrapper around the underlying analytics SDK).

*GH-110:* every event and screen view also carries `platform` (`string`): `terminal` or `web`
(`ios` and `android` are reserved for future clients). A decorator adds it, the same way
`InputMethodAnalyticsService` adds `input_method`. On `web`, `input_method` is `rich_ui`, since the
browser client has a single pointer-driven UI with no keyboard mode. The web client also uses
`NoopAnalyticsService`; no SDK is wired up there either.

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

Fired when the user saves via the Save dialog (toolbar Save / 2) or the exit flow. *(GH-6)* The name dates from the removed console `save` command and is kept for continuity (GH-79).

| Property | Type | Description |
|---|---|---|
| `result` | `string` | `success` or `error` (e.g. an unsafe name, or a filesystem failure such as a full disk). |
| `overwrote_existing` | `boolean` | Whether a file with that name already existed. Only present when `result` is `success` - on `error` it may not be known (e.g. the name check itself failed). |
| `input_method` | `string` | `mouse` or `keyboard` — which UI mode the session was running in. *(GH-3, GH-18)* *(`console` retired — GH-79)* |

*GH-99:* new property `name_source` (`string`, sent on `success` and `error`): `prefill` - saved under the prefilled current save name unchanged, `edited` - the field opened prefilled and the name was changed, or `typed` - the field opened empty. The name itself is never sent.

### `load_command_used`

Fired when the board is restored from a save file, either via the `load` command mid-game or from the startup restore prompt. *(GH-6)*

| Property | Type | Description |
|---|---|---|
| `trigger` | `string` | `command` (a mid-game load: toolbar Load / 3 - the value predates GH-79 and is kept) or `startup_prompt`. |
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

*GH-110:* on `web`, it fires on page load. `mode` is `rich_ui`, and `board_size` and `console_flag_ignored` are absent: there are no CLI flags, and `new_game_size_selected` reports the size picked later. New property `layout` (`string`, web only): `wide` (window wider than tall) or `tall` (taller than wide, or square), as of page load.

*GH-111:* two new properties on `web` only: `app_version` (`string`) - the version stamped into
the build, e.g. `0.18.0`, the same one the footer shows; and `host` (`string`) - `public_site`
(`lutsenko-yuriy.github.io`), `localhost` or `other`. Only these three bucketed values are sent,
never the raw hostname, URL, query string or referrer. The footer's links aren't tracked.

### `dialog_cancelled`

Fired when the user cancels the Save, Load, Exit, or Size dialog in the mouse-driven or keyboard-driven TUI (mouse click, or Escape/Cancel-control in keyboard mode) without completing the action. *(GH-3, GH-18, GH-44)*

| Property | Type | Description |
|---|---|---|
| `dialog` | `string` | `save`, `load`, `exit`, or `size`. |
| `input_method` | `string` | `mouse` or `keyboard` — which TUI mode the session was running in. *(GH-18)* |
| `via` | `string` | `web` only: `button` (Cancel / No) or `escape`. Absent on `terminal`. *(GH-163)* |

### `input_mode_switched`

Fired when the user requests a switch to a *different* input mode mid-session — OK in the Controls dialog with the other mode marked (`controls_dialog`, GH-170); before GH-170 a toolbar button click (`toolbar`) or a key (`shortcut`). Requesting the mode that's already active is a no-op instead: no event, no message, no exception (audit finding on PR #36). *(GH-30)*

| Property | Type | Description |
|---|---|---|
| `from_mode` | `string` | `mouse` or `keyboard` — mode active before the switch. *(`console` retired — GH-79)* |
| `to_mode` | `string` | `mouse` or `keyboard` — requested target mode. *(`console` retired — GH-79)* |
| `trigger` | `string` | `controls_dialog` (GH-170). *(`command` retired — GH-79; `toolbar` and `shortcut` retired on `terminal` — GH-170)* |
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
| `trigger` | `string` | `command` (mid-game load: toolbar Load / 3) or `startup_prompt`. |
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

*GH-83:* new properties `mordie_result` (`string`: `player_won` - Mordie hadn't found its solution, `matched` - the move count equals Mordie's N, `mordie_won` - the move count exceeds N, or `machine` - the move count is below N, found or not, which only an inconsistent save can produce) and `mordie_moves` (`number`: Mordie's N when the board was solved - the true minimum if found, otherwise its progress). Both are absent when Mordie sat the game out - a save from before GH-83, or a repaired one, loaded with moves already made.

*GH-101:* new property `shift_keys` (`string`, keyboard mode only - absent in mouse mode): which keys made this game's keyboard shifts - `arrows` (only the arrow keys), `hjkl` (only H/J/K/L) or `mixed` (both). Mouse clicks aren't counted, so a game shifted partly by mouse reports only its keys. Sent only when the solving shift is a keyboard one; only solved games report it.

*GH-110:* new property `shift_input` (`string`, `web` only - absent on `terminal`): how this game's shifts were made - `arrows` (only arrow clicks), `drag` (only drags) or `mixed` (both).

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

*Known gap:* a save stores Mordie's live state, so a solution found between two checks and then
saved is stored as found; loading that save reveals it at once, and this event never fires for
that game. Likewise, a board solved before the next check never fires it.

*GH-110 (web):* the web client has no Exit, so `exit_command_used`, `input_mode_switched` and
`app_launch_aborted` never fire there. `startup_restore_decision.decision` never takes `quit`,
`unsolvable_save_resolved.choice` never takes `exit`, and `dialog_cancelled.dialog` never takes
`exit`. Closing the tab and the leave-site prompt are not tracked: a page can't reliably send
anything while it unloads.

### `game_given_up`

Fired when the player confirms **Yes** in the `Give up and see Mordie's solution?` dialog
(`[ Give Up ]` / 5). *(GH-126)*

| Property | Type | Description |
|---|---|---|
| `board_size` | `number` | The board side (3-5). |
| `move_count` | `number` | The player's moves on this board, which giving up discards. |
| `solution_length` | `number` | Mordie's N - its solution's length in moves. |
| `moves_since_solution_found` | `number` | The player's moves since the `Found it!` line appeared. Absent when a save was loaded with Mordie's solution already found. |
| `shifts_since_solution_found` | `number` | The same, in shifts. Absent in the same case. |
| `input_method` | `string` | `mouse`, `keyboard` or `rich_ui`. |

### `solution_replay_started`

Fired when Next steps the replay off the original arrangement on the solution screen: the
first Next, or the first after Restart (or after stepping back to the start). *(GH-126)*

| Property | Type | Description |
|---|---|---|
| `restart` | `boolean` | `true` when an earlier run on this visit to the screen already started. |
| `board_size` | `number` | The board side (3-5). |
| `solution_length` | `number` | Mordie's N. |
| `input_method` | `string` | `mouse`, `keyboard` or `rich_ui`. |

### `solution_replay_finished`

Fired when Next reaches the solved board, once per run (a run begins at
`solution_replay_started`). *(GH-126)*

| Property | Type | Description |
|---|---|---|
| `board_size` | `number` | The board side (3-5). |
| `solution_length` | `number` | Mordie's N. |
| `stepped_back` | `boolean` | Whether Prev was used during this run. |
| `input_method` | `string` | `mouse`, `keyboard` or `rich_ui`. |

*GH-126:* `dialog_cancelled.dialog` takes a new value, `give_up` - **No** or Escape in the
give-up confirmation.

### `tile_labels_changed`

Fired when the player presses OK in the Tile labels dialog with a **different** scheme selected.
OK on the current scheme fires nothing. Scheme values everywhere: `numbers`, `numbers_from_0`,
`pairs`, `pairs_from_0`, `letters`, `letter_pairs`. *(GH-139)*

| Property | Type | Description |
|---|---|---|
| `from` | `string` | The scheme the game showed before. |
| `to` | `string` | The scheme chosen. |
| `from_source` | `string` | Why the game showed `from`: `default` (no preference set), `preference`, or `save` (a loaded save's labels). |
| `screen` | `string` | `play`, `solved` or `solution` - where the player was. |
| `board_size` | `number` | The board side (3-5). |
| `input_method` | `string` | `mouse`, `keyboard` or `rich_ui`. |

*GH-139:* additions to existing events:
- `app_launched` gains `tile_labels` (`string`, always sent): the remembered preference at launch,
  `numbers` when none is set.
- `board_solved` gains `tile_labels` (`string`): the labels shown when the solving shift was made.
- `load_command_used` gains `labels_differ_from_preference` (`boolean`, only on `success`): `true`
  when the save carried labels other than the preference; saves without labels send `false`.
- `dialog_cancelled.dialog` takes a new value, `labels` (Cancel or Escape in the Tile labels dialog).

*GH-160:* board sides now run 3-6, so `app_launched.board_size`, `new_game_size_selected.size`,
`board_solved.board_size` and `tile_labels_changed.board_size` can be `6`. Mordie sits 6x6 out:
`board_solved` leaves out `mordie_result` and `mordie_moves` there, and `mordie_solution_found`,
`game_given_up` and the `solution_replay_*` events never fire. `unsolvable_save_resolved.board_size`
stays 3 or 5, since every 6x6 arrangement can be solved. `tile_labels_changed.from_source` takes a
new value, `letters_unavailable`: the game showed Numbers because Letters doesn't fit a 6x6 board.

*GH-163:* Escape now cancels web dialogs, firing the same events as their Cancel-like button.
`dialog_cancelled` gains `via` (`string`, `web` only - absent on `terminal`, where `input_method`
already tells keyboard from mouse): `button` (Cancel / No clicked or activated) or `escape`.
`startup_restore_decision` is unchanged: Escape on Restore sends `new_game`, as Cancel does.

*GH-167:* nothing tracked. A press of Read board, the button hidden from sight, would mark a
session as "probably uses a screen reader": health and disability information, sensitive even
without a user ID. Whether any accessibility usage signal is acceptable is GH-180's question.

*GH-168:* nothing tracked. The system text size in effect at load (even bucketed), or the page
scrolling because large text didn't fit, would mark a session as "probably low vision". GH-180
settled it: no accessibility usage signal is acceptable, on any terms.

*GH-170:* the Controls dialog (`[ Controls 6 ]` / 6) replaces the terminal's mode switch.
- `input_mode_switched` fires on OK in the Controls dialog with a different mode marked; `trigger`
  takes a new value, `controls_dialog`. On `terminal`, `toolbar` and `shortcut` are retired: the
  dialog is now the only way to switch.
- `dialog_cancelled.dialog` takes a new value, `controls` (Cancel or Escape in the Controls dialog).
- `board_solved.shift_keys` is sent only in the right-handed layout; absent in the left-handed one.
  Left-handed shifts aren't counted at all, so a game that switched layout mid-way reports only its
  right-handed keys: a left-handed key value would reveal the layout (see below).
- The keyboard layout itself is not tracked anywhere (no change event, nothing on `app_launched` or
  `board_solved`): someone with an injured or impaired hand may pick the left-handed layout, which
  makes it the kind of accessibility usage signal GH-180 ruled out.

*GH-171 (web keyboard play):*
- `input_method` on `web` follows the press that fired the event, in either mode: `keyboard` for
  any key (the scheme's keys, and Enter, Space or Escape in dialogs), `rich_ui` for mouse or touch.
- `app_launched.mode` on `web` is the remembered mode, `mouse` or `keyboard`, instead of `rich_ui`.
- `input_mode_switched` now fires on `web` too (`trigger`: `controls_dialog`), and
  `dialog_cancelled.dialog` takes `controls` there (with `via`).
- `board_solved.shift_input` takes a new value, `keys` (only key shifts); `mixed` covers any
  combination of arrows, drag and keys. `board_solved.shift_keys` is sent on `web` too, under
  GH-170's rule (right-handed layout only).
- Not tracked: selection moves (too granular), Read board's key 0 (a screen-reader signal, GH-167,
  GH-180), the keyboard layout (GH-170), and keys that do nothing in mouse mode.

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
| `screen_save_dialog` | Save dialog opens in the TUI (toolbar click/2, or via the Exit dialog's "Yes"). Property `opened_from`: `toolbar` or `exit_flow`. *(GH-3)* |
| `screen_load_dialog` | Load dialog opens in the TUI (toolbar click/3, or startup restore when saves exist). Properties `opened_from`: `toolbar` or `startup`; `save_file_count`: number. *(GH-3)* |
| `screen_exit_dialog` | Exit dialog opens in the TUI (Exit button click, or Escape in keyboard mode). *(GH-3, GH-18)* |
| `screen_congratulations` | A shift (click or keyboard Enter/Space) solves the board while the TUI is active - fires once on that transition. Does **not** fire when an already-solved save is restored (toolbar Load or startup restore); the Congratulations screen still renders, but nothing was solved *by playing* this session. *(GH-3)* |
| `screen_size_dialog` | TUI size-picker dialog opens. Property `opened_from`: `toolbar` (via `[ New ]`), `startup` (no `--size` given, restore prompt declined/empty), or `unsolvable_save` (New game chosen for an unsolvable save mid-game, GH-74). *(GH-44)* |
| `screen_unsolvable_dialog` | TUI `Can't be solved` dialog opens for an unsolvable save. Property `opened_from`: `toolbar` (Load button/3) or `startup` (restore dialog). *(GH-74)* |

*GH-99:* `screen_save_dialog` takes a new property `prefilled` (`boolean`) - whether the name field opened with the current save name in it.

*GH-110:* on `web`, the screen views fire on the same triggers as in the TUI. `screen_exit_dialog` never fires there, and `screen_save_dialog.opened_from` never takes `exit_flow`.

*GH-126:* new screen views:

| Screen name | When tracked |
|---|---|
| `screen_give_up_dialog` | The `Give up and see Mordie's solution?` confirmation opens (`[ Give Up ]` / 5). Properties `board_size`: number; `move_count`: number - the player's moves so far. *(GH-126)* |
| `screen_solution` | The solution screen opens. Properties `opened_from`: `give_up`, `after_win` (`[ Mordie's solution ]`), `load` (toolbar Load) or `startup_prompt` (restore prompt); `board_size`: number; `solution_length`: number - Mordie's N. *(GH-126)* |

*GH-139:* new screen view:

| Screen name | When tracked |
|---|---|
| `screen_labels_dialog` | The Tile labels dialog opens. Properties `opened_from`: `toolbar` (`[ Labels ]` click) or `shortcut` (4 in keyboard mode); `screen`: `play`, `solved` or `solution`; `current_labels`: the scheme shown when it opened. *(GH-139)* |

*GH-160:* `screen_labels_dialog.opened_from` takes a new value, `letters_save`: the dialog opened by
itself after the warning for a 6x6 save with Letters labels. Cancel there is `dialog_cancelled`
with `dialog: labels`, as usual.

*GH-170:* new screen view:

| Screen name | When tracked |
|---|---|
| `screen_controls_dialog` | The Controls dialog opens. Properties `opened_from`: `toolbar` (`[ Controls 6 ]` click) or `shortcut` (6, in either mode); `screen`: `play`, `solved` or `solution`; `input_method`: `mouse` or `keyboard`. *(GH-170)* |

*GH-171:* `screen_controls_dialog` now fires on `web` too, with `input_method` `keyboard` or
`rich_ui`; `screen_labels_dialog.opened_from` takes `shortcut` there (4 in keyboard mode).
