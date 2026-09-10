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

### `load_command_used`

Fired when the board is restored from a save file, either via the `load` command mid-game or from the startup restore prompt. *(GH-6)*

| Property | Type | Description |
|---|---|---|
| `trigger` | `string` | `command` or `startup_prompt`. |
| `result` | `string` | `success`, `not_found`, `size_mismatch` (save's board size doesn't match the current board), or `error` (corrupted save file or a filesystem failure). |

### `startup_restore_prompt_shown`

Fired when the app boots and finds one or more save files. *(GH-6)*

| Property | Type | Description |
|---|---|---|
| `save_file_count` | `number` | How many save files were found. |

### `startup_restore_decision`

Fired when the user resolves the startup restore prompt. *(GH-6)*

| Property | Type | Description |
|---|---|---|
| `decision` | `string` | `restored` or `new_game`. |
| `save_file_count` | `number` | How many save files were on offer. |

### `exit_command_used`

Fired when the user runs `exit` or `quit` and the session ends. *(GH-12)*

| Property | Type | Description |
|---|---|---|
| `save_choice` | `string` | `saved` or `declined` - whether the user accepted the save-before-quitting prompt. The save outcome itself (success/error/overwrite) is separately reported by `save_command_used`, since this reuses the `save` command's flow. |

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

_(no screen views tracked yet)_
