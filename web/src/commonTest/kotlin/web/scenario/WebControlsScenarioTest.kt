package web.scenario

import kotlin.test.Test

/** GH-171: the web's Controls dialog (`[ Controls 6 ]` / 6, both modes) picks the input mode and the
 *  keyboard layout, and both are remembered. Stubs until WU2 makes them green. */
class WebControlsScenarioTest {

    @Test
    fun `6 in mouse mode opens Controls, and Keyboard then OK switches mode keeping the game`() {
        // TODO: start on a board with one shift made
        // TODO: press 6
        // TODO: mark Keyboard, OK
        // TODO: the mode is keyboard; tiles and moves are unchanged
    }

    @Test
    fun `Controls opens focused on the marked mode`() {
        // TODO: click `Controls 6` - focus on Mouse
        // TODO: Cancel
        // TODO: press 6 - same focus
    }

    @Test
    fun `arrows move the mark and the key list follows the marked layout`() {
        // TODO: open Controls
        // TODO: mark Left-handed - the list shows the left-handed keys
        // TODO: mark Right-handed - it switches back
    }

    @Test
    fun `Cancel and Escape in Controls change nothing`() {
        // TODO: open, mark Keyboard and Left-handed
        // TODO: Cancel - mode, layout and store unchanged
        // TODO: repeat with Escape
    }

    @Test
    fun `the layout can be picked with Mouse marked`() {
        // TODO: open, mark Left-handed, keep Mouse
        // TODO: OK - still mouse mode, layout stored as left-handed
    }

    @Test
    fun `OK focuses the board on entering keyboard mode, otherwise goes back`() {
        // TODO: Mouse to Keyboard, OK - focus on the board
        // TODO: open from `Controls 6`, change only the layout, OK - focus back on `Controls 6`
    }

    @Test
    fun `mode and layout are remembered, and a first visit is mouse and right-handed`() {
        // TODO: new store - mouse, right-handed
        // TODO: choose Keyboard and Left-handed
        // TODO: new harness on the same store - both kept
    }

    @Test
    fun `in mouse mode only 6 works and only its button shows a digit`() {
        // TODO: press 1 to 5 - no dialog
        // TODO: labels are `New`, `Save`, `Load`, `Labels`, `Controls 6`
    }

    @Test
    fun `on a touch screen Controls is hidden and 6 does nothing`() {
        // TODO: start with touch
        // TODO: controlsShown is false
        // TODO: press 6 - no dialog
    }

    @Test
    fun `a remembered keyboard mode starts as mouse on a touch screen and stays stored`() {
        // TODO: store holds keyboard
        // TODO: start with touch - mouse mode
        // TODO: store still holds keyboard
    }

    /** WU4 deletes this with the flag (D3). */
    @Test
    fun `with the flag off Controls is hidden, 6 is ignored and the mode is mouse`() {
        // TODO: store holds keyboard, flag off
        // TODO: mouse mode, no Controls row
        // TODO: press 6 - no dialog
    }
}
