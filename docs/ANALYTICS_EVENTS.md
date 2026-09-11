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

Fired when the user runs `save <file-name>`. *(GH-6)*

| Property | Type | Description |
|---|---|---|
| `result` | `string` | `success` or `error` (e.g. an unsafe name, or a filesystem failure such as a full disk). |
| `overwrote_existing` | `boolean` | Whether a file with that name already existed. Only present when `result` is `success` - on `error` it may not be known (e.g. the name check itself failed). |
| `input_method` | `string` | `console` or `mouse` — which UI mode the session was running in. *(GH-3)* |

### `load_command_used`

Fired when the board is restored from a save file, either via the `load` command mid-game or from the startup restore prompt. *(GH-6)*

| Property | Type | Description |
|---|---|---|
| `trigger` | `string` | `command` or `startup_prompt`. |
| `result` | `string` | `success`, `not_found`, `size_mismatch` (save's board size doesn't match the current board), or `error` (corrupted save file or a filesystem failure). |
| `input_method` | `string` | `console` or `mouse` — which UI mode the session was running in. *(GH-3)* |

### `startup_restore_prompt_shown`

Fired when the app boots and finds one or more save files. *(GH-6)*

| Property | Type | Description |
|---|---|---|
| `save_file_count` | `number` | How many save files were found. |
| `input_method` | `string` | `console` or `mouse` — which UI mode the session was running in. *(GH-3)* |

### `startup_restore_decision`

Fired when the user resolves the startup restore prompt. *(GH-6)*

| Property | Type | Description |
|---|---|---|
| `decision` | `string` | `restored` or `new_game`. |
| `save_file_count` | `number` | How many save files were on offer. |
| `input_method` | `string` | `console` or `mouse` — which UI mode the session was running in. *(GH-3)* |

### `exit_command_used`

Fired when the user runs `exit` or `quit` **and the session actually ends** - not fired if the
user asked to save but the save attempt failed, since the session keeps running in that case
instead of quitting (see Feature 5 in `docs/PRODUCT_SPEC.md`). *(GH-12)*

| Property | Type | Description |
|---|---|---|
| `save_choice` | `string` | `saved` (the save-before-quitting prompt was accepted and the save succeeded) or `declined` (the user said no, or left the save name blank/EOF - never fired for a failed save attempt, which keeps the session open instead). The save outcome itself (success/error/overwrite) is separately reported by `save_command_used`, since this reuses the `save` command's flow. |
| `input_method` | `string` | `console` or `mouse` — which UI mode the session was running in. *(GH-3)* |

### `app_launched`

Fired once at startup, after the launch mode is resolved (default mouse TUI, or `--console`). *(GH-3)*

| Property | Type | Description |
|---|---|---|
| `mode` | `string` | `mouse` or `console`. |

### `dialog_cancelled`

Fired when the user clicks Cancel on the Save, Load, or Exit dialog in the mouse-driven TUI without completing the action. *(GH-3)*

| Property | Type | Description |
|---|---|---|
| `dialog` | `string` | `save`, `load`, or `exit`. |

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
| `screen_save_dialog` | Save dialog opens in the mouse-driven TUI (toolbar click, or via the Exit dialog's "Yes"). Property `opened_from`: `toolbar` or `exit_flow`. *(GH-3)* |
| `screen_load_dialog` | Load dialog opens in the mouse-driven TUI (toolbar click, or startup restore when saves exist). Properties `opened_from`: `toolbar` or `startup`; `save_file_count`: number. *(GH-3)* |
| `screen_exit_dialog` | Exit button clicked in the mouse-driven TUI. *(GH-3)* |
| `screen_congratulations` | A shift click solves the board while the mouse-driven TUI is active - fires once on that transition. Does **not** fire when an already-solved save is restored (toolbar Load or startup restore); the Congratulations screen still renders, but nothing was solved *by playing* this session. *(GH-3)* |
