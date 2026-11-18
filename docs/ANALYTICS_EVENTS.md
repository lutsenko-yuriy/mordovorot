# Analytics Events

All events are sent via the analytics service abstraction (a thin wrapper around the underlying analytics SDK).

*GH-110:* every event and screen view also carries `platform` (`string`): `web` now (`terminal` retired — GH-228;
`ios` and `android` are reserved for future clients). A decorator adds it, the same way
`InputMethodAnalyticsService` adds `input_method`. On `web`, `input_method` follows the press that
fired the event: `keyboard` for a key, `rich_ui` for mouse or touch (GH-171). The web client also
uses `NoopAnalyticsService`; no SDK is wired up there either.

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

Fired when the user saves via the Save dialog (toolbar Save / 2). *(GH-6)* The name dates from the removed console `save` command and is kept for continuity (GH-79).

| Property | Type | Description |
|---|---|---|
| `result` | `string` | `success` or `error` (e.g. an unsafe name, or a filesystem failure such as a full disk). |
| `overwrote_existing` | `boolean` | Whether a file with that name already existed. Only present when `result` is `success` - on `error` it may not be known (e.g. the name check itself failed). |
| `input_method` | `string` | `keyboard` or `rich_ui` - the press that fired the event (GH-171). *(`mouse` retired — GH-228; `console` retired — GH-79)* |

*GH-99:* new property `name_source` (`string`, sent on `success` and `error`): `prefill` - saved under the prefilled current save name unchanged, `edited` - the field opened prefilled and the name was changed, or `typed` - the field opened empty. The name itself is never sent.

### `load_command_used`

Fired when the board is restored from a save file, either via the `load` command mid-game or from the startup restore prompt. *(GH-6)*

| Property | Type | Description |
|---|---|---|
| `trigger` | `string` | `command` (a mid-game load: toolbar Load / 3 - the value predates GH-79 and is kept) or `startup_prompt`. |
| `result` | `string` | `success` (also covers a load that resized the board to the save's size, GH-44 WU5), `not_found`, `error` (corrupted save file or a filesystem failure), or `unsolvable` (GH-74 - the save can't be solved; `unsolvable_save_resolved` carries the outcome). |
| `input_method` | `string` | `keyboard` or `rich_ui` - the press that fired the event (GH-171). *(`mouse` retired — GH-228; `console` retired — GH-79)* |

### `startup_restore_prompt_shown`

Fired when the app boots and finds one or more save files. *(GH-6)*

| Property | Type | Description |
|---|---|---|
| `save_file_count` | `number` | How many save files were found. |
| `input_method` | `string` | `keyboard` or `rich_ui` - the press that fired the event (GH-171). *(`mouse` retired — GH-228; `console` retired — GH-79)* |

### `startup_restore_decision`

Fired when the user resolves the startup restore prompt. *(GH-6)*

| Property | Type | Description |
|---|---|---|
| `decision` | `string` | `restored` or `new_game`. *(`quit` retired — GH-228)* Repairing an unsolvable save counts as `restored`. |
| `save_file_count` | `number` | How many save files were on offer. |
| `input_method` | `string` | `keyboard` or `rich_ui` - the press that fired the event (GH-171). *(`mouse` retired — GH-228; `console` retired — GH-79)* |

### `exit_command_used`

**Retired — GH-228:** the terminal's Exit is gone, so this event is no longer fired.

Fired when the user exits via the Exit dialog (toolbar Exit / Escape) **and the session actually ends** - not fired if the
user asked to save but the save attempt failed, since the session keeps running in that case
instead of quitting (see Feature 5 in `docs/PRODUCT_SPEC.md`). *(GH-12)*

| Property | Type | Description |
|---|---|---|
| `save_choice` | `string` | `saved` (the save-before-quitting prompt was accepted and the save succeeded) or `declined` (the user said no, or left the save name blank/EOF - never fired for a failed save attempt, which keeps the session open instead). The save outcome itself (success/error/overwrite) is separately reported by `save_command_used`, since this reuses the `save` command's flow. |
| `input_method` | `string` | `mouse` or `keyboard` — which UI mode the session was running in. *(GH-3, GH-18)* *(`console` retired — GH-79)* |

### `app_launched`

Fired once at startup, when the page loads. *(GH-3, GH-18)*

| Property | Type | Description |
|---|---|---|
| `mode` | `string` | `mouse` or `keyboard`. *(`console` retired — GH-79)* |
| `board_size` | `number` | **Retired — GH-228** (the `--size=N` flag). It was the resolved board side for this launch. *(GH-44)* |

*GH-79:* property `console_flag_ignored` (`boolean`). **Retired — GH-228** with the terminal launch flags.

*GH-110:* `mode` is `rich_ui`, and `board_size` and `console_flag_ignored` are absent: there are no CLI flags, and `new_game_size_selected` reports the size picked later. New property `layout` (`string`, web only): `wide` (window wider than tall) or `tall` (taller than wide, or square), as of page load.

*GH-111:* two new properties on `web` only: `app_version` (`string`) - the version stamped into
the build, e.g. `0.18.0`, the same one the footer shows; and `host` (`string`) - `public_site`
(`lutsenko-yuriy.github.io`), `localhost` or `other`. Only these three bucketed values are sent,
never the raw hostname, URL, query string or referrer. The footer's links aren't tracked.

### `dialog_cancelled`

Fired when the user cancels the Save, Load, Size or another dialog (a click, or Escape or the Cancel control in keyboard mode) without completing the action. *(GH-3, GH-18, GH-44)*

| Property | Type | Description |
|---|---|---|
| `dialog` | `string` | `save`, `load`, `size`, and later values (`controls`, ...). *(`exit` retired — GH-228)* |
| `input_method` | `string` | `keyboard` or `rich_ui` - the press that fired the event (GH-171). *(`mouse` retired — GH-228)* |
| `via` | `string` | `button` (Cancel / No) or `escape`; always sent since the terminal is gone. *(GH-163)* |

### `input_mode_switched`

Fired when the user requests a switch to a *different* input mode mid-session — OK in the Controls dialog with the other mode marked (`controls_dialog`, GH-170); before GH-170 a toolbar button click (`toolbar`) or a key (`shortcut`). Requesting the mode that's already active is a no-op instead: no event, no message, no exception (audit finding on PR #36). *(GH-30)*

| Property | Type | Description |
|---|---|---|
| `from_mode` | `string` | `mouse` or `keyboard` — mode active before the switch. *(`console` retired — GH-79)* |
| `to_mode` | `string` | `mouse` or `keyboard` — requested target mode. *(`console` retired — GH-79)* |
| `trigger` | `string` | `controls_dialog` (GH-170). *(`command` retired — GH-79; `toolbar` and `shortcut` retired on `terminal` — GH-170)* |
| `result` | `string` | Always `success`. *(`rejected_no_tty` retired — GH-79: a running TUI always has a terminal)* |

### `new_game_size_selected`

Fired when a fresh game starts at a chosen board size via one of the post-launch surfaces. *(GH-44;
the terminal's `--size` CLI flag, reported as `app_launched.board_size` instead, is retired — GH-228)*

| Property | Type | Description |
|---|---|---|
| `size` | `number` | The board side chosen (3-5). |
| `trigger` | `string` | `startup_prompt` or `toolbar` (`[ New ]` → size-picker dialog). *(`command` retired — GH-79)* |
| `input_method` | `string` | `keyboard` or `rich_ui` - the press that fired the event (GH-171). *(`mouse` retired — GH-228; `console` retired — GH-79)* |

*GH-66:* `trigger` also took `play_again` - the console size prompt after accepting `Start a new game?` on a solved board. *(Retired — GH-79)*

*GH-74:* `trigger` also takes `unsolvable_save` - the size prompt after choosing New game for an unsolvable save mid-game. At startup that choice falls through to the regular startup size prompt (`startup_prompt`).

### `unsolvable_save_resolved`

Fired when the player picks what to do with an unsolvable save, in any input mode. Save names are
never sent. *(GH-74)*

| Property | Type | Description |
|---|---|---|
| `choice` | `string` | `repair` or `new_game`. *(`exit` retired — GH-228)* |
| `trigger` | `string` | `command` (mid-game load: toolbar Load / 3) or `startup_prompt`. |
| `board_size` | `number` | The save's board side (3 or 5). |
| `repair_saved` | `boolean` | Only on `repair`: whether the repaired board was written back to the save file (`false` if the overwrite failed; the board is loaded either way). |
| `input_method` | `string` | `keyboard` or `rich_ui`. *(`mouse` retired — GH-228; `console` retired — GH-79)* |

### `board_solved`

Fired once when a shift solves the board during play, in either input mode. Like
`screen_congratulations`, it does **not** fire when an already-solved save is restored. *(GH-62)*

| Property | Type | Description |
|---|---|---|
| `move_count` | `number` | The final move count shown to the player (a move is a run of consecutive shifts on one row/column). |
| `board_size` | `number` | The board side (3-5). |
| `input_method` | `string` | `keyboard` or `rich_ui` - the press that fired the event (GH-171). *(`mouse` retired — GH-228; `console` retired — GH-79)* |

*GH-83:* new properties `mordie_result` (`string`: `player_won` - Mordie hadn't found its solution, `matched` - the move count equals Mordie's N, `mordie_won` - the move count exceeds N, or `machine` - the move count is below N, found or not, which only an inconsistent save can produce) and `mordie_moves` (`number`: Mordie's N when the board was solved - the true minimum if found, otherwise its progress). Both are absent when Mordie sat the game out - a save from before GH-83, or a repaired one, loaded with moves already made.

*GH-101:* new property `shift_keys` (`string`, keyboard mode only - absent in mouse mode): which keys made this game's keyboard shifts - `arrows` (only the arrow keys), `hjkl` (only H/J/K/L) or `mixed` (both). Mouse clicks aren't counted, so a game shifted partly by mouse reports only its keys. Sent only when the solving shift is a keyboard one; only solved games report it.

*GH-110:* new property `shift_input` (`string`, always sent since GH-228): how this game's shifts were made - `arrows` (only arrow clicks), `drag` (only drags) or `mixed` (both).

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

**Retired — GH-228:** there is no terminal launch to abort, so this event is no longer fired.

Fired when a launch finds no interactive terminal, prints the `No interactive terminal found`
message and exits - instead of `app_launched`, which only fires when a game starts. *(GH-79)*

| Property | Type | Description |
|---|---|---|
| `reason` | `string` | Always `no_interactive_terminal` for now. |
| `requested_mode` | `string` | `mouse`, `keyboard`, `console`, or `none` - the mode flag passed at launch (`none` when no flag was given). With several flags, `keyboard` beats `mouse` beats `console`. |

### `mordie_solution_found`

Fired when a 5-shift check first reveals that Mordie has found its solution, so the `Got it!`
line appears during play. Not fired when a save with Mordie's solution already found is
loaded. *(GH-83)*

| Property | Type | Description |
|---|---|---|
| `board_size` | `number` | The board side (3-5). |
| `solution_length` | `number` | Mordie's N - the true minimum solution length. |
| `player_shift_count` | `number` | The player's shift count at the revealing check. |
| `search_duration_ms` | `number` | Wall-clock search time in this session (time from earlier sessions before a save isn't included). |
| `resumed_from_save` | `boolean` | Whether the search resumed from a saved N. |
| `input_method` | `string` | `keyboard` or `rich_ui`. *(`mouse` retired — GH-228)* |

*Known gap:* a save stores Mordie's live state, so a solution found between two checks and then
saved is stored as found; loading that save reveals it at once, and this event never fires for
that game. Likewise, a board solved before the next check never fires it.

*GH-110 (web):* the web client has no Exit, so `exit_command_used` and `app_launch_aborted` never
fired there. Closing the tab and the leave-site prompt are not tracked: a page can't reliably send
anything while it unloads.

### `game_given_up`

Fired when the player confirms **Yes** in the `Give up and see Mordie's solution?` dialog
(`[ Give Up ]` / 5). *(GH-126)*

| Property | Type | Description |
|---|---|---|
| `board_size` | `number` | The board side (3-5). |
| `move_count` | `number` | The player's moves on this board, which giving up discards. |
| `solution_length` | `number` | Mordie's N - its solution's length in moves. |
| `moves_since_solution_found` | `number` | The player's moves since the `Got it!` line appeared. Absent when a save was loaded with Mordie's solution already found. |
| `shifts_since_solution_found` | `number` | The same, in shifts. Absent in the same case. |
| `input_method` | `string` | `keyboard` or `rich_ui`. *(`mouse` retired — GH-228)* |

### `solution_replay_started`

Fired when Next steps the replay off the original arrangement on the solution screen: the
first Next, or the first after Restart (or after stepping back to the start). *(GH-126)*

| Property | Type | Description |
|---|---|---|
| `restart` | `boolean` | `true` when an earlier run on this visit to the screen already started. |
| `board_size` | `number` | The board side (3-5). |
| `solution_length` | `number` | Mordie's N. |
| `input_method` | `string` | `keyboard` or `rich_ui`. *(`mouse` retired — GH-228)* |

### `solution_replay_finished`

Fired when Next reaches the solved board, once per run (a run begins at
`solution_replay_started`). *(GH-126)*

| Property | Type | Description |
|---|---|---|
| `board_size` | `number` | The board side (3-5). |
| `solution_length` | `number` | Mordie's N. |
| `stepped_back` | `boolean` | Whether Prev was used during this run. |
| `input_method` | `string` | `keyboard` or `rich_ui`. *(`mouse` retired — GH-228)* |

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
| `input_method` | `string` | `keyboard` or `rich_ui`. *(`mouse` retired — GH-228)* |

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

*GH-207:* nothing new tracked. On `web` board sides now run 3-9 (the very large sizes 7x7 to 9x9),
so `new_game_size_selected.size`, `board_solved.board_size` and `tile_labels_changed.board_size` can
be `7`, `8` or `9`. Mordie sits 6x6 to 9x9 out, as GH-160 describes for 6x6, and
`tile_labels_changed.from_source: letters_unavailable` covers them all.
`unsolvable_save_resolved.board_size` can be 3, 5, 7 or 9 (odd sides). Whether the Very large section was
seen, and abandoned very large games, are not tracked.

*GH-163:* Escape now cancels web dialogs, firing the same events as their Cancel-like button.
`dialog_cancelled` gains `via` (`string`; it was absent on `terminal` until GH-228): `button` (Cancel / No clicked or activated) or `escape`.
`startup_restore_decision` is unchanged: Escape on Restore sends `new_game`, as Cancel does.

*GH-167:* nothing tracked. A press of Read board, the button hidden from sight, would mark a
session as "probably uses a screen reader": health and disability information, sensitive even
without a user ID. Whether any accessibility usage signal is acceptable is GH-180's question.

*GH-168:* nothing tracked. The system text size in effect at load (even bucketed), or the page
scrolling because large text didn't fit, would mark a session as "probably low vision". GH-180
settled it: no accessibility usage signal is acceptable, on any terms.

*GH-170:* the Controls dialog (`[ Controls 6 ]` / 6) replaces the terminal's mode switch.
- `input_mode_switched` fires on OK in the Controls dialog with a different mode marked; `trigger`
  takes a new value, `controls_dialog`. `toolbar` and `shortcut` are retired (they were terminal-only): the
  dialog is the only way to switch.
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
- `app_launched.mode` on `web` is the mode the page starts in, `mouse` or `keyboard`, instead of
  `rich_ui`: the remembered one, except on a touch screen, which always starts in `mouse`.
- `input_mode_switched` now fires on `web` too (`trigger`: `controls_dialog`), and
  `dialog_cancelled.dialog` takes `controls` there (with `via`).
- `board_solved.shift_input` takes a new value, `keys` (only key shifts); `mixed` covers any
  combination of arrows, drag and keys. `board_solved.shift_keys` is sent on `web` too, under
  GH-170's rule (right-handed layout only).
- Not tracked: selection moves (too granular), Read board's key 0 (a screen-reader signal, GH-167,
  GH-180), the keyboard layout (GH-170), and keys that do nothing in mouse mode.

*GH-200:* nothing new tracked. A non-Latin key counts as its Latin twin, so `р`, `о`, `л` and `д`
report as `hjkl` in `board_solved.shift_keys`. The system keyboard layout, whether non-Latin keys
were used, and letters the terminal doesn't know are not tracked: each would point to a player's
language, and possibly their nationality or location.

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
| `screen_save_dialog` | Save dialog opens (toolbar click/2). Property `opened_from`: `toolbar`. *(GH-3; `exit_flow` retired — GH-228)* |
| `screen_load_dialog` | Load dialog opens (toolbar click/3, or startup restore when saves exist). Properties `opened_from`: `toolbar` or `startup`; `save_file_count`: number. *(GH-3)* |
| `screen_exit_dialog` | **Retired — GH-228** (the Exit dialog is gone). *(GH-3, GH-18)* |
| `screen_congratulations` | A shift (click or keyboard Enter/Space) solves the board - fires once on that transition. Does **not** fire when an already-solved save is restored (toolbar Load or startup restore); the Congratulations screen still renders, but nothing was solved *by playing* this session. *(GH-3)* |
| `screen_size_dialog` | The size-picker dialog opens. Property `opened_from`: `toolbar` (via `[ New ]`), `startup` (restore prompt declined/empty), or `unsolvable_save` (New game chosen for an unsolvable save mid-game, GH-74). *(GH-44)* |
| `screen_unsolvable_dialog` | The `Can't be solved` dialog opens for an unsolvable save. Property `opened_from`: `toolbar` (Load button/3) or `startup` (restore dialog). *(GH-74)* |

*GH-99:* `screen_save_dialog` takes a new property `prefilled` (`boolean`) - whether the name field opened with the current save name in it.


*GH-126:* new screen views:

| Screen name | When tracked |
|---|---|
| `screen_give_up_dialog` | The `Give up and see Mordie's solution?` confirmation opens (`[ Give Up ]` / 5). Properties `board_size`: number; `move_count`: number - the player's moves so far. *(GH-126)* |
| `screen_solution` | The solution screen opens. Properties `opened_from`: `give_up`, `after_win` (`[ Mordie's solution ]`), `load` (toolbar Load) or `startup_prompt` (restore prompt); `player_result`: `gave_up`, `solved` or `unknown` (a save from before GH-273); `board_size`: number; `solution_length`: number - Mordie's N. *(GH-126, GH-273)* |

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
| `screen_controls_dialog` | The Controls dialog opens. Properties `opened_from`: `toolbar` (`[ Controls 6 ]` click) or `shortcut` (6, in either mode); `screen`: `play`, `solved` or `solution`; `input_method`: `keyboard` or `rich_ui` (`mouse` retired — GH-228). *(GH-170)* |

*GH-171:* `screen_controls_dialog` now fires on `web` too, with `input_method` `keyboard` or
`rich_ui`; `screen_labels_dialog.opened_from` takes `shortcut` there (4 in keyboard mode).

*GH-222 (Mordie keeps a best solution so far):* Mordie now holds the best solution from the fast
solver and the exact search, so "Mordie's N" is the length of that best solution, not always the
true minimum. Nothing new is tracked on screens.
- `mordie_solution_found` fires when the first solution exists, from either solver, so it can fire
  on any size Mordie plays, 3x3 and 4x4 included. `solution_length` is the best solution's length.
  New properties: `source` (`string`): `fast_solver` or `exact_search`, whichever gave the first
  solution; `proven_shortest` (`boolean`): `true` when the exact search had finished.
- `game_given_up` gains `solution_proven_shortest` (`boolean`): whether
  the exact search had finished when the player gave up.
- `board_solved` gains `mordie_proven_shortest` (`boolean`, absent when `mordie_moves` is).
  Since N may be a longer answer, `mordie_result` values `matched` and `mordie_won` no longer mean
  a tie or loss to the best possible play unless this is `true`.
- `screen_give_up_dialog`, `screen_solution` and `solution_replay_*` are unchanged; their
  `solution_length` means the length of the solution shown.
- `search_duration_ms` is now the time to the first solution, from either solver.
- Not tracked: each time the exact search shortens the answer, and search time per solver.

*GH-223 (Mordie's lines for an answer that may not be the shortest):* no new screens.
- New event `mordie_shorter_found`: fired when a 5-shift check sees that the exact search
  shortened Mordie's best solution, so the `Wait, I found a shorter one…` line shows. Once per
  announcement; not fired when a save loads. Properties: `board_size` (`number`); `solution_length` (`number`): the new N;
  `previous_length` (`number`): the N before it; `proven_shortest` (`boolean`): whether the new
  best is proven shortest.
- `board_solved.mordie_result` gains `beat_mordie`: the player's moves are below an N that is not
  proven shortest (the `You beat me! … Mine may not have been the shortest.` ending). `machine`
  now means only moves below a proven-shortest N, which only an inconsistent save can produce, so
  it marks a likely cheat.

## GH-228 (the terminal client is dropped)

No new events or screen views. The web client is the only client, and mobile apps are planned, so `platform` stays on every event and screen view (always `web` for now; its `terminal` value is retired, and a mobile app would add its own).

Retired — GH-228, each marked where it is catalogued above and kept as history: the events `app_launch_aborted` and `exit_command_used`, the screen view `screen_exit_dialog`, the values `screen_save_dialog.opened_from=exit_flow`, `startup_restore_decision.decision=quit`, `unsolvable_save_resolved.choice=exit`, `dialog_cancelled.dialog=exit` and `platform=terminal`, and the `app_launched` properties `board_size` and `console_flag_ignored`. `dialog_cancelled.via` and `board_solved.shift_input` were absent on `terminal`; they are always sent now. The web still fires `input_mode_switched` and the `opened_from=startup` values, so those stay.

## GH-224 (saves keep Mordie's best solution)

No new events, screen views or properties. Two existing events become more accurate after a load:
- `board_solved.mordie_proven_shortest` and `mordie_result`: a solved save now keeps whether Mordie's answer was proven shortest, so reloading a game won below an unproven N reports `beat_mordie` again instead of `machine`.
- `game_given_up.solution_length` and `solution_proven_shortest`: Give Up right after loading a save reports the solution the save kept, not a fresh search's.

`mordie_solution_found` still never fires for a loaded game, which found its solution in an earlier session. The gap is wider now: the fast solver answers within moments, so a game saved before its first check is stored as an unproven found answer, and loading it shows that answer at once.

## GH-225 (Mordie plays 6x6 to 9x9)

No new events, screen views or properties. Mordie no longer sits out 6x6 to 9x9, so the "sits out" statements in the GH-160 and GH-207 notes above no longer hold, and the events below fire on those sizes as on smaller ones:
- `mordie_solution_found`, `mordie_shorter_found`, `game_given_up` and the `solution_replay_*` events.
- `screen_give_up_dialog` and `screen_solution`.
- `board_solved` carries `mordie_result`, `mordie_moves` and `mordie_proven_shortest`.

Their `board_size` now runs 3-9 (the `3-5` in the tables above predates GH-160 and GH-207).

## GH-237 (saves use a compact bit-packed format)

No new events, screen views or properties. Saving and loading report as before: `save_command_used` with `success` or `error`, and `load_command_used` with `success`, `not_found`, `error` or `unsolvable`. A save that fails to decode in the new format is `load_command_used` with `result=error`, the same as a corrupted text save. The Mordie properties on `board_solved` and `game_given_up` keep their values through a save and a load, whichever format the save was read in.

## GH-243 (share a board with a link)

New screen view:

| Screen name | When tracked |
|---|---|
| `screen_share_dialog` | The Share dialog opens (toolbar Share, or its key in keyboard mode). Properties `board_size`: number; `start_known`: boolean - false when the link shares the current board because the game's start is unknown. *(GH-243)* |

`dialog_cancelled.dialog` takes a new value, `share` (Close or Escape in the Share dialog).

### `board_share_link_copied`

Fired when Copy link in the Share dialog succeeds. A blocked clipboard fires nothing. *(GH-243)*

| Property | Type | Description |
|---|---|---|
| `board_size` | `number` | The shared board's side. |
| `start_known` | `boolean` | False when the link shares the current board because the start is unknown. |
| `via` | `string` | `button` (a pointer click) or `enter_key` (a click with no pointer: Enter, Space or a screen reader activating the button). |

### `shared_board_opened`

Fired when a share link has been checked: when the page loads with one in the address bar, or when one arrives in an open tab (`hashchange`). A link visit at load shows up as `app_launched` followed by this event, with no `startup_restore_prompt_shown` (the prompt is skipped); a link in an open tab fires it alone. A link met while unsaved progress is asked about and cancelled fires nothing. *(GH-243)*

| Property | Type | Description |
|---|---|---|
| `result` | `string` | `started` or `rejected`. |
| `board_size` | `number` | The board's side. Only when `started`. |
| `reason` | `string` | `invalid`, `unsupported_size` or `unsolvable`. Only when `rejected`. |

The link and the board are never sent: the arrangement is the shared content. Which of these a QR scan was can't be told from a typed link.

## GH-251 (delete saves from the Load and restore dialogs)

New screen view:

| Screen name | When tracked |
|---|---|
| `screen_delete_save_dialog` | The `Delete 'foo'?` confirmation opens, from the trash icon in the Load or the startup restore dialog. Properties `opened_from`: `toolbar` (the Load dialog mid-game) or `startup` (the restore dialog), as in `screen_load_dialog`; `save_state`: `ok`, `unsolvable` or `corrupted` - what the list showed for that save; `save_file_count`: number - the saves in the list before the delete. *(GH-251)* |

`dialog_cancelled.dialog` takes a new value, `delete_save` (Cancel or Escape in the confirmation). `via` and `input_method` work as for the other dialogs.

### `save_deleted`

Fired when the player presses Delete in the confirmation, whatever happens next. *(GH-251)*

| Property | Type | Description |
|---|---|---|
| `result` | `string` | `success`, `not_found` (the save was already gone) or `error` (storage refused). |
| `trigger` | `string` | `command` (the Load dialog mid-game) or `startup_prompt` (the restore dialog), the same values as `load_command_used.trigger`. |
| `save_state` | `string` | `ok`, `unsolvable` or `corrupted`, as in `screen_delete_save_dialog`. |
| `was_current_save` | `boolean` | Whether the deleted save was the current save name (GH-99), which deleting clears. Only on `success`. |
| `remaining_save_count` | `number` | The saves left after the delete. Only on `success`. |
| `input_method` | `string` | `keyboard` or `rich_ui` - the press that fired the event (GH-171). |

`startup_restore_decision.decision` takes a new value, `all_deleted`: the player deleted every save from the restore dialog, which then closed and opened the size picker. Its `save_file_count` is the number on offer when the dialog opened.

Already covered: after the last save is deleted at startup the size picker opens and fires `screen_size_dialog` with `opened_from: startup`; after the last save is deleted mid-game the dialog closes and the game carries on, with nothing to track.

Never sent: the save's name (free text, as for every save event) and its contents. Not tracked: the trash icon press itself (the confirmation's screen view covers it), hovering over its tooltip, and any property that tells an Enter or Space press from a pointer click, since that could hint at screen-reader use (GH-167, GH-180).

## GH-260 (no exact search on 5x5 and bigger)

No new events or properties. On 5x5 and bigger the exact search no longer runs, so `mordie_solution_found.source` is always `fast_solver` there, `proven_shortest`, `solution_proven_shortest` and `mordie_proven_shortest` are `false` (except when the fast answer happens to equal the lower bound, or on a solved board), and `mordie_shorter_found` no longer fires. 3x3 and 4x4 are unchanged.

## GH-262 (offline play and a New version available notice)

Planned in `/analyze` and built in GH-275 (WU3, WU4); the web client uses `NoopAnalyticsService` until an SDK is wired up (GH-256).

### `update_available`

Fired once per version when the footer notice `New version available. Update` appears: the new version is fully cached and ready. *(GH-262)*

| Property | Type | Description |
|---|---|---|
| `check_trigger` | `string` | The check that found it: `load` (the page loaded), `tab_return` (the player came back to the tab) or `game_end` (the game was solved or given up). |
| `current_version` | `string` | The running version, the same one the footer and `app_launched.app_version` show. |

### `update_applied`

Fired just before the page reloads to the new version, after a click on `Update` (and, with unsaved progress, a click on Update in the dialog). *(GH-262)*

| Property | Type | Description |
|---|---|---|
| `had_unsaved_progress` | `boolean` | Whether the game had unsaved progress when `Update` was clicked. |
| `confirmed` | `boolean` | `true` when it went through the confirmation dialog, `false` for a direct reload (no unsaved progress). |

`dialog_cancelled.dialog` takes a new value, `update`: Cancel (or Escape) in the update confirmation.

| Screen name | When tracked |
|---|---|
| `screen_update_dialog` | The update confirmation opens (`Update` clicked with unsaved progress). Properties `board_size`: number; `move_count`: number - the shifts at stake. *(GH-262)* |

Not tracked: each version check and a check that finds nothing, a failed or incomplete download, offline launches (they cannot be sent while offline), hovering over the `Update` link, and the new version's number (the page cannot know it before reloading).

## GH-267 (share tile labels in the share link)

Planned in `/analyze`; the web client uses `NoopAnalyticsService` until an SDK is wired up (GH-256). Scheme values are those of `tile_labels_changed` (GH-139).

New screen view:

| Screen name | When tracked |
|---|---|
| `screen_shared_labels_dialog` | The receiver's `Use the sender's labels (<scheme>)?` prompt opens. It doesn't open when the link's labels equal the receiver's, or when the link carries none or unreadable ones. Properties `board_size`: number; `sender_labels`: scheme; `receiver_labels`: the scheme the receiver would otherwise see. *(GH-267)* |

`screen_share_dialog` gains `include_labels` (`boolean`): the toggle's state when the dialog opened (the remembered choice).

`dialog_cancelled.dialog` takes a new value, `shared_labels` (No or Escape in the labels prompt). `via` and `input_method` work as for the other dialogs.

### `share_labels_toggled`

Fired when the sender flips the `Include my tile labels` toggle in the Share dialog. *(GH-267)*

| Property | Type | Description |
|---|---|---|
| `enabled` | `boolean` | The toggle's new state. |
| `labels` | `string` | The scheme shown on the board, the one the link would carry. |
| `board_size` | `number` | The shared board's side. |
| `input_method` | `string` | `keyboard` or `rich_ui`. |

### `shared_labels_accepted`

Fired when the receiver presses Yes in the labels prompt: the sender's labels show for that game only. No fires `dialog_cancelled` instead. *(GH-267)*

| Property | Type | Description |
|---|---|---|
| `sender_labels` | `string` | The scheme the link carried. |
| `receiver_labels` | `string` | The scheme the receiver would otherwise have seen. |
| `board_size` | `number` | The board's side. |
| `input_method` | `string` | `keyboard` or `rich_ui`. |

Additions to existing events:
- `board_share_link_copied` gains `includes_labels` (`boolean`): whether the copied link carries the sender's labels.
- `shared_board_opened` gains `labels` (`string`, only when `result` is `started`): `none` (the link has no labels), `same` (they equal the receiver's, so no prompt), `differs` (the prompt followed) or `unreadable` (the labels were ignored).
- `tile_labels_changed.from_source` takes a new value, `shared_link`: the game showed labels the receiver accepted from a link.

Not tracked: QR scans of a labelled link (indistinguishable from a typed link, as in GH-243), which solved games came from a link (`board_solved.tile_labels` already gives difficulty by labels), the toggle's remembered state at launch (`screen_share_dialog.include_labels` covers it), and hovering over or focusing the toggle. The link and the board are never sent.

## GH-273 (remember how a game ended before the solution screen)

Planned in `/analyze`; the web client uses `NoopAnalyticsService` until an SDK is wired up (GH-256).

No new events or screen views. `screen_solution` gains `player_result` (`string`): `gave_up` (the solution screen was reached through Give Up), `solved` (the player solved the board, then opened Mordie's solution) or `unknown` (a solution-screen save from before this ticket, which keeps the line without the player's result). It lets `opened_from: load` and `startup_prompt` be split by whether the save carried a result.

Not tracked: the player's move count on the solution screen (`game_given_up.move_count` and `board_solved.move_count` already report it when it happened, and a loaded save would only repeat an earlier game), the new Mordie line being shown (it follows from `screen_solution` and `player_result`), and how the result is stored in a save.

## GH-272 (share the sender's progress in the link)

Planned in `/analyze`; the web client uses `NoopAnalyticsService` until an SDK is wired up (GH-256).

New event:

### `share_progress_toggled`

Fired when the sender flips the `Include my progress` toggle in the Share dialog. *(GH-272)*

| Property | Type | Description |
|---|---|---|
| `enabled` | `boolean` | The toggle's new state. |
| `progress` | `string` | What the link would carry: `solving`, `solved` or `gave_up`. |
| `board_size` | `number` | The shared board's side. |
| `input_method` | `string` | `keyboard` or `rich_ui`. |

Additions to existing events:
- `screen_share_dialog` gains `include_progress` (`boolean`): the toggle's state when the dialog opened (the remembered choice); always `false` at 0 moves.
- `board_share_link_copied` gains `includes_progress` (`boolean`): whether the copied link carries the sender's progress.
- `shared_board_opened` gains `progress` (`string`, only when `result` is `started`): `none` (the link has no progress), `solving`, `solved` or `gave_up` (Mordie's opening line shows that status), or `unreadable` (the progress was ignored).

Not tracked: move counts (`board_solved.move_count` and `game_given_up.move_count` report them, and the link's contents are never sent), whether a receiver goes on to solve a game that came from a link (as in GH-267), the `gave_up` row with an unknown count (folded into `gave_up`), the toggle's remembered state at launch (`screen_share_dialog.include_progress` covers it), QR scans, Mordie's opening line being shown (it follows from `shared_board_opened.progress`), and hovering over or focusing the toggle.

## GH-287 (the Mouse/Keyboard mode choice is dropped)

No new events or screen views. Retired, and kept above as history:
- the event `input_mode_switched`: there is no mode to switch, so it never fires again;
- the `app_launched` property `mode`: it would name the same value on every launch.

`board_solved.shift_keys` is no longer "keyboard mode only": every game can report it, and it is still sent only when the solving shift is a keyboard one. `input_method` is unchanged (the press that fired the event), so `screen_controls_dialog.input_method` is the press that opened the dialog, not a mode.

Not tracked: the selection showing or hiding after key or mouse use, how often the digits and key hints are used (`input_method` already shows keyboard use), and the one-time drop of a stored keyboard mode.

## GH-288 (first-launch tutorial)

Planned in `/analyze`; the web client uses `NoopAnalyticsService` until an SDK is wired up (GH-256). No new screen views: the tutorial is a panel, not a screen.

### `tutorial_started`

Fired when the tutorial begins. After a reload halfway it fires again, since the tutorial restarts from step 1. *(GH-288)*

| Property | Type | Description |
|---|---|---|
| `source` | `string` | `first_launch`, or `replay` (GH-289: started from `[ Tutorial ]` or 8). |

### `tutorial_step_completed`

Fired when the player makes the asked-for shift of a guided step. *(GH-288)*

| Property | Type | Description |
|---|---|---|
| `step` | `number` | 1 to 4. |
| `blocked_shifts` | `number` | Other shifts blocked on this step. |

### `tutorial_ended`

Fired when the tutorial's last step ends (the panel hides) or the player skips it. *(GH-288)*

| Property | Type | Description |
|---|---|---|
| `source` | `string` | `first_launch` or `replay`, as `tutorial_started.source` (GH-289). |
| `result` | `string` | `completed` or `skipped`. |
| `last_step` | `number` | 1 to 6: the step the player was on. |
| `blocked_shifts` | `number` | Blocked shifts over the whole tutorial. |

Changes to existing entries:
- `screen_controls_dialog.opened_from` and `screen_labels_dialog.opened_from` take a new value, `tutorial`: opened from the guidance panel's buttons.
- `board_solved` and `mordie_solution_found` gain `tutorial` (`boolean`): true for the tutorial game, so its solve doesn't skew `mordie_result` and move counts.

Not tracked: each shift made during the tutorial (`board_solved.shift_input` and `tutorial_step_completed` cover it), a reload or close halfway (can't be detected; the next launch's `tutorial_started` shows it), the bubble texts and the highlight being shown (they follow from `step`), whether the tutorial game is later saved or loaded (the usual events cover it), the share link met on a first launch (`shared_board_opened` fires as usual after the tutorial), and a player with saves never seeing the tutorial (no `tutorial_started`).

## GH-289 (replay the tutorial)

Planned in `/analyze`; the web client uses `NoopAnalyticsService` until an SDK is wired up (GH-256). The replay is the tutorial of GH-288 started from `[ Tutorial ]` or 8, so `tutorial_started`, `tutorial_step_completed` and `tutorial_ended` fire as there, with `source: replay`.

### `screen_replay_save_dialog`

Fired when `Save your game first?` opens: the player started a replay with unsaved moves. *(GH-289)*

| Property | Type | Description |
|---|---|---|
| `opened_from` | `string` | `button` (`[ Tutorial ]`) or `shortcut` (8). |
| `move_count` | `number` | Moves made in the game the replay would replace. |

### `tutorial_replay_decision`

Fired when the player picks Save or Don't save in that dialog. Cancel fires `dialog_cancelled` instead. *(GH-289)*

| Property | Type | Description |
|---|---|---|
| `decision` | `string` | `save` or `dont_save`. `save` is the choice, not the outcome: the Save dialog that follows can still be cancelled (`dialog_cancelled` with `dialog: save`), and then no replay starts. |

Changes to existing entries:
- `dialog_cancelled.dialog` takes a new value, `replay_save`.
- `screen_save_dialog.opened_from` takes a new value, `tutorial_replay`: opened from the replay save question's Save.
- `tutorial_ended` gains `source` (above); `tutorial_started.source` takes `replay`, and fires once: after Don't save, after a successful save, or at once with no unsaved moves.
- Skip in a replay runs the startup sequence again, in the same session as a launch's. So `startup_restore_prompt_shown`, `startup_restore_decision`, `load_command_used`, `unsolvable_save_resolved` and `new_game_size_selected`, all with `trigger: startup_prompt`, fire again, and a replay-Skip can't be told from a launch by `trigger`. Count real launches with `app_launched`. A distinct trigger value for the replay's startup is left until an SDK is wired up (GH-256) and the numbers show it matters.

Not tracked: a replay that never starts because the question was cancelled (`dialog_cancelled` covers it), which control opened the replay beyond `opened_from`, and how often it is replayed (`tutorial_started` with `source: replay` counts it).

## GH-290 (the tutorial names the keys on a computer)

Planned in `/analyze`; the web client uses `NoopAnalyticsService` until an SDK is wired up (GH-256). No new events or screen views: the bubble's lines, the key line and the Key hints follow from `step`.

Changes to existing entries:
- `tutorial_step_completed` gains `input` (`string`): how the asked-for shift was made: `drag`, `click` (an arrow) or `key`. `key` never occurs on a phone. It doesn't say which keys or which layout: the keyboard layout is not tracked (GH-170).

Not tracked: the keys pressed and the select keys on the way (the layout would show), the hand changed in Controls mid-tutorial (`screen_controls_dialog` with `opened_from: tutorial` covers that Controls was opened), which lines the bubble listed, and the way a blocked shift was made (`blocked_shifts` covers it).

## GH-275 (German and Russian)

Planned in `/analyze`; the web client uses `NoopAnalyticsService` until an SDK is wired up (GH-256). Languages are `en`, `de` and `ru`. The browser's own language (`navigator.language`) is never sent, only one of these three codes: a raw value could point to a player's nationality or location (see GH-200). Every other event, property name and value stays English and language-independent.

No screen views: the picker is a control in the page corner, not a dialog.

### `language_changed`

Fired when the player picks a **different** language in the picker. Picking the current one fires nothing.

| Property | Type | Description |
|---|---|---|
| `from` | `string` | The language shown before: `en`, `de` or `ru`. |
| `to` | `string` | The language chosen. |
| `from_source` | `string` | Why the game showed `from`: `default` (English, nothing detected), `browser` (detected on the first visit) or `preference` (an earlier pick). |
| `screen` | `string` | `play`, `solved` or `solution` - where the player was. |
| `control` | `string` | `dropdown` (phone width) or `toggles` (wider pages). |
| `input_method` | `string` | `keyboard` or `rich_ui`. |

Changes to existing entries:
- `app_launched` gains `language` (`string`, always sent): `en`, `de` or `ru`, the language the page started in.

Not tracked: the browser's raw language list and unsupported languages it asked for, opening the dropdown without choosing (too granular), and a language per save or share link (neither carries one).

## GH-322 (Greek tile labels)

No new events or screen views: the two new schemes only add values to every property that carries a scheme, as GH-313's did.

Changes to existing entries:
- Scheme values take two new names, `TileLabels.wireName`: `greek_letters` and `greek_letter_pairs`. They stay English in every game language.
- `tile_labels_changed.from_source: letters_unavailable` also covers Greek letters on 6x6 and larger (they label up to 5x5).

Not tracked: the words a screen reader says.

## GH-313 (Cyrillic tile labels)

Planned in `/analyze`; the web client uses `NoopAnalyticsService` until an SDK is wired up (GH-256). No new events or screen views: the two new schemes only add values to every property that carries a scheme (`tile_labels_changed.from`/`to`, `app_launched.tile_labels`, `board_solved.tile_labels`, `screen_labels_dialog.current_labels`, `share_labels_toggled.labels`, `shared_labels_accepted.sender_labels`/`receiver_labels`).

Changes to existing entries:
- Scheme values take two new names, the schemes' `TileLabels.wireName`: `cyrillic_letters` and `cyrillic_letter_pairs`. They stay English in every game language.
- `tile_labels_changed.from_source: letters_unavailable` also covers Cyrillic letters on 6x6 and larger (it labels up to 5x5, as Letters does).
- `tile_labels_changed.board_size` is not limited to 3-5: it takes 6 to 9 as well (a correction to the GH-139 entry, which listed 3-5).

Not tracked: the words a screen reader says, the save version written, and a link's scheme an old build can't read (`shared_board_opened.labels: unreadable` already counts it).

## GH-332 (alphabet hint for letter tile labels)

Planned in `/analyze`; the web client uses `NoopAnalyticsService` until an SDK is wired up (GH-256).

| Screen name | Trigger | Properties |
|---|---|---|
| `screen_alphabet_hint_dialog` | The alphabet dialog opens, from `[ Hint 8 ]` or the key 8. | `opened_from`: `button` or `shortcut`; `screen`: `play` or `solved` (the hint is gone on the solution screen); `labels`: the scheme shown, with the values of `tile_labels_changed`; `board_size`: number. *(GH-332)* |

No new events: the dialog changes nothing in the game.

Changes to existing entries:
- `dialog_cancelled.dialog` takes a new value, `alphabet_hint`: the dialog's Escape (and OK) sends it with `via`, as Share's close does (decided in `/plan`).
- Tutorial's key moves from 8 to 9, so `screen_replay_save_dialog.opened_from: shortcut` now means 9. The data is unchanged.

Not tracked: a press on the dimmed button on a number scheme (it does nothing), which letters are greyed or used (both follow from `labels` and `board_size`), time spent open, and which letters the player looked at.

## GH-338 (autosave when leaving the page)

Planned in `/analyze`; the web client uses `NoopAnalyticsService` until an SDK is wired up (GH-256). No screen views: autosave has no UI.

### `autosave_written`

Fired when an autosave was attempted: the page is left (tab hidden, closed or reloaded), or Update, opening a share link or replaying the tutorial tries one first (it saves the game before asking, and asks only if this fails). Needs a game with at least one shift that isn't solved. Not fired for a game with no moves, the solved board or the solution screen. *(GH-338)*

| Property | Type | Description |
|---|---|---|
| `result` | `string` | `success` or `error` (storage full or refused: the case that keeps the browser's "Leave site?" prompt). |
| `board_size` | `number` | The board side (3-9). |
| `tile_labels` | `string` | The scheme's wire name, the same values as everywhere else. |
| `moves` | `number` | The game's move count. |
| `trigger` | `string` | `page_left`, `update`, `shared_link` or `tutorial_replay`: what asked for the autosave. |

Changes to existing entries:
- `load_command_used` gains `autosave` (`boolean`, only on `success`): whether the loaded save was an autosave slot.
- `screen_update_dialog` and `screen_replay_save_dialog` fire only when the autosave before them failed (or the game is solved), since a saved game no longer needs the question.
- `update_applied.had_unsaved_progress` is `false` after a successful autosave: the moves are saved.
- `startup_restore_decision` gains `restored_autosave` (`boolean`, only on `restored`): whether the save picked was an autosave slot.

Not tracked: a skipped autosave (not an action), a separate "page left" event (`autosave_written` covers it, and a hidden tab can't be told from a close), deleting an autosave (the delete event covers it; save names stay out of events), and how the player answered the "Leave site?" prompt (the browser gives no signal).

PII: no save name is sent, only size, scheme, move count and result; `autosave` and `restored_autosave` are booleans derived from the slot name.

Open for GH-256: a real SDK must send this event from a closing page (for example with `sendBeacon`).
