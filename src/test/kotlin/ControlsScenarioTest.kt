import kotlin.test.Test

/** GH-170: the Controls dialog (`[ Controls 6 ]` / 6, both modes) picks the input mode and the
 *  keyboard layout; the left-handed layout mirrors the hands and is remembered. Stubs until
 *  WU2 (mode, cancel, failure) and WU3 (layout) make them green. */
class ControlsScenarioTest {

    @Test
    fun `6 in mouse mode opens Controls, and Keyboard then OK switches mode keeping the game`() {
        // TODO: start mouse mode, make one shift
        // TODO: the toolbar's second row is `[ Labels ] [ Controls 6 ]`
        // TODO: 1-5 do nothing
        // TODO: 6 opens Controls with Mouse marked and the key list shown
        // TODO: →, Enter - keyboard mode, same board and moves
        // TODO: screen_controls_dialog fired with opened_from: shortcut, input_method: mouse
        // TODO: input_mode_switched fired with trigger: controls_dialog
    }

    @Test
    fun `clicking Controls opens it with opened_from toolbar`() {
        // TODO: in mouse mode, click `[ Controls 6 ]`
        // TODO: the dialog opens; screen_controls_dialog has opened_from: toolbar
    }

    @Test
    fun `Cancel and Escape in Controls change nothing`() {
        // TODO: 6, →, Escape - still mouse mode, dialog_cancelled with dialog: controls, no input_mode_switched
        // TODO: same again with Tab to Cancel and Enter
    }

    @Test
    fun `the selection survives a switch to mouse mode and back`() {
        // TODO: in keyboard mode select row 2, column 3
        // TODO: 6, Mouse, OK
        // TODO: 6, Keyboard, OK
        // TODO: row 2, column 3 is still selected
    }

    @Test
    fun `a failed switch keeps the game and says why`() {
        // TODO: the terminal factory fails for the target mode
        // TODO: 6, Keyboard, OK - still mouse mode with the same game
        // TODO: the status line reads `Could not switch to keyboard mode (<reason>) - staying in mouse mode.`
    }

    @Test
    fun `the left-handed layout selects with HJKL and the arrows and shifts with WASD`() {
        // TODO: 6, Tab, → - Left-handed marked, the key list shows the mirrored keys
        // TODO: Enter
        // TODO: hints read `A/D row`, `W/S column` on the left and `←↑↓→ H/J/K/L select` on the right
        // TODO: j, l select row 2, column 2; d shifts row 2 right; w shifts column 2 up
    }

    @Test
    fun `the keyboard layout is remembered by the next session`() {
        // TODO: pick Left-handed, OK
        // TODO: a fresh session over the same preference store starts left-handed: h moves the selection and doesn't shift
    }

    @Test
    fun `a left-handed solve sends board_solved without shift_keys`() {
        // TODO: left-handed, start one shift away from solved, solve it with a/d/w/s
        // TODO: board_solved has no shift_keys
    }
}
