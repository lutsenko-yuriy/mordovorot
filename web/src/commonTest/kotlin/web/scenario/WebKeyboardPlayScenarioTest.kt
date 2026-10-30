package web.scenario

import kotlin.test.Test

/** GH-171: playing from the keyboard - the digit toolbar, the selected tile, its shifts in both
 *  layouts, focus and the key hints. The grid's markup is checked by the harness (`keys_check.sh`).
 *  Stubs until WU2 (digits) and WU3 (the board) make them green. */
class WebKeyboardPlayScenarioTest {

    @Test
    fun `keyboard mode shows the digit toolbar and the Controls row`() {
        // TODO: keyboard mode
        // TODO: labels are `New 1`, `Save 2`, `Load 3`, `Labels 4`, `Give Up 5`, `Controls 6`
    }

    @Test
    fun `digits open the toolbar's dialogs wherever focus is`() {
        // TODO: press each of 1 to 6 with focus off the board
        // TODO: each opens its dialog (5 once Give Up is enabled)
    }

    @Test
    fun `a dialog opened by a digit returns focus where it was`() {
        // TODO: press 2 from the board, Cancel - focus on the board
        // TODO: press 2 from `Load`, Cancel - focus on `Load`
    }

    @Test
    fun `digits typed in the Save dialog go into the name field`() {
        // TODO: open Save
        // TODO: press 3 - no Load dialog, the game didn't take the key
    }

    @Test
    fun `W A S D move the selected tile with wraparound`() {
        // TODO: starts at row 1, column 1
        // TODO: D, S move it
        // TODO: A, W return it
        // TODO: W from row 1 wraps to the last row; A likewise
    }

    @Test
    fun `H L and the left and right arrows shift the selected row and the selection stays put`() {
        // TODO: press each key
        // TODO: the row's tiles shift; the selection and other rows don't
    }

    @Test
    fun `K J and the up and down arrows shift the selected column`() {
        // TODO: press each key
        // TODO: the column's tiles shift; the selection and other columns don't
    }

    @Test
    fun `the left-handed layout selects with HJKL and the arrows and shifts with WASD`() {
        // TODO: left-handed
        // TODO: H J K L and the arrows move the selection
        // TODO: A D shift the row; W S shift the column
    }

    @Test
    fun `upper-case letters work too`() {
        // TODO: D, S and H as capitals - same effect as lower case
    }

    @Test
    fun `board keys work only while the board has focus`() {
        // TODO: focus on `New` - W and H do nothing
        // TODO: digit 2 still opens Save
    }

    @Test
    fun `keys with Ctrl, Alt or Meta, and Escape with no dialog, do nothing`() {
        // TODO: Ctrl+S, Meta+1 - not taken
        // TODO: Escape - nothing changes
    }

    @Test
    fun `the board takes focus after the startup dialog in keyboard mode`() {
        // TODO: store holds keyboard, with a save
        // TODO: answer the startup dialog
        // TODO: focus on the board
    }

    @Test
    fun `pressing a tile selects it and gives the board focus`() {
        // TODO: press a tile at row 3, column 2
        // TODO: the selection is there and focus is on the board
    }

    @Test
    fun `a drag leaves the selection on the dragged line and the mouse still works`() {
        // TODO: drag row 2
        // TODO: the row shifted and the selection is on row 2
        // TODO: click `Save 2` - it opens
    }

    @Test
    fun `a smaller board clamps the selection and a new game or load keeps it`() {
        // TODO: select 4,4 on a 4x4
        // TODO: new 4x4 - kept
        // TODO: load a 4x4 - kept
        // TODO: new 3x3 - 3,3
    }

    @Test
    fun `switching mode keeps the selection`() {
        // TODO: select 2,3
        // TODO: to mouse and back
        // TODO: still 2,3
    }

    @Test
    fun `a solved board hides the selection and ignores the select and shift keys`() {
        // TODO: solve with a key
        // TODO: selection is null
        // TODO: W and H change nothing; digits still work
    }

    @Test
    fun `keyboard mode shows the key hints for its layout and hides the arrows`() {
        // TODO: right-handed hints
        // TODO: left-handed hints
        // TODO: arrows hidden
        // TODO: mouse mode - no hints
    }
}
