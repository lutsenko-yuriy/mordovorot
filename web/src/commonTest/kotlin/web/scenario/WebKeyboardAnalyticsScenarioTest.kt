package web.scenario

import kotlin.test.Test

/** GH-171: analytics in keyboard play (`docs/ANALYTICS_EVENTS.md`). Stubs until WU2 (dialogs and
 *  `input_method`) and WU3 (shifts) make them green. */
class WebKeyboardAnalyticsScenarioTest {

    @Test
    fun `input_method follows the press, and the launch and Controls events are sent`() {
        // TODO: start in keyboard mode - app_launched.mode is keyboard
        // TODO: press 6 - screen_controls_dialog with shortcut and keyboard
        // TODO: click `Controls 6` - toolbar and rich_ui
        // TODO: OK - input_mode_switched with controls_dialog
        // TODO: press 4 - screen_labels_dialog with shortcut
        // TODO: Escape in Controls - dialog_cancelled with controls
    }

    @Test
    fun `board_solved reports keys or mixed, with shift_keys for right-handed shifts only`() {
        // TODO: solve with H - keys, with shift_keys
        // TODO: arrow then key - mixed
        // TODO: left-handed solve - keys, no shift_keys
    }

    @Test
    fun `selection moves, 0 and mouse-mode keys send nothing`() {
        // TODO: clear events
        // TODO: press W, then 0
        // TODO: in mouse mode press 1
        // TODO: no events
    }
}
