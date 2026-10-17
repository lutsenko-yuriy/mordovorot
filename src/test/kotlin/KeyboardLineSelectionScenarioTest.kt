import kotlin.test.Test

/** GH-101: keyboard mode's line selection - WASD selects a row/column (wrapping, switching
 *  back to the remembered line), HJKL/arrows shift it, no arrows drawn, the active line's
 *  tiles highlighted. Real BoardImpl + ViewModelImpl driven through FakeTerminal in keyboard
 *  mode. Mouse mode is unchanged (its own tests run unmodified). */
class KeyboardLineSelectionScenarioTest {

    @Test
    fun `keyboard mode shows the selected row highlighted and no arrows`() {
        // TODO: launch with --size=4
        // TODO: the first frame has no arrow glyphs; row 1's tiles are reverse video and no others
        // TODO: both side hints are shown (W/S, A/D left; H/L, K/J right); the controls line no longer mentions arrows/Enter
    }

    @Test
    fun `W and S move the row selection with wraparound`() {
        // TODO: S -> row 2 highlighted
        // TODO: W, W -> row 4 highlighted (wrapped from row 1)
    }

    @Test
    fun `A and D switch to the column, then move it with wraparound`() {
        // TODO: with row 1 active, D -> column 1 highlighted (switched, not moved)
        // TODO: A -> column 4 highlighted (wrapped)
    }

    @Test
    fun `switching back returns to the remembered line`() {
        // TODO: S -> row 2; D -> column 1; D -> column 2
        // TODO: W -> row 2 highlighted again (not row 1, and not moved)
    }

    @Test
    fun `H L and the left right arrows shift the selected row`() {
        // TODO: S -> row 2
        // TODO: L -> row 2 rotates right by one; Left arrow -> back
        // TODO: H -> row 2 rotates left
        // TODO: Moves: 1 (all shifts on the same line)
    }

    @Test
    fun `J K and the up down arrows shift the selected column`() {
        // TODO: D -> column 1
        // TODO: J -> column 1 rotates down; Up arrow -> back; K -> rotates up
    }

    @Test
    fun `a shift key on the other axis switches without shifting`() {
        // TODO: with row 1 active, Down arrow -> column 1 highlighted, board unchanged
        // TODO: L -> row 1 highlighted, board unchanged
        // TODO: Moves: 0
    }

    @Test
    fun `upper case letters work too`() {
        // TODO: S then L (upper case) -> row 2 highlighted and shifted right
    }

    @Test
    fun `Enter and Space no longer shift`() {
        // TODO: Enter, Space -> board unchanged, Moves: 0
    }

    @Test
    fun `letters typed in the Save dialog go into the name field`() {
        // TODO: F5, type wasd, Esc
        // TODO: the Save dialog showed `Name: wasd_`; board and selection unchanged
    }

    @Test
    fun `the selection survives a switch to mouse mode and back`() {
        // TODO: S, S -> row 3
        // TODO: F7 -> mouse mode; its frame still shows all the arrows
        // TODO: click [ Keyboard ] -> row 3 highlighted
    }

    @Test
    fun `a smaller new game clamps the selection`() {
        // TODO: launch with --size=5; D, A -> column 5; W, W -> row 4
        // TODO: F9 -> 3x3; D -> column 3 highlighted (5 clamped to 3)
        // TODO: W -> row 3 highlighted (4 clamped to 3)
    }

    @Test
    fun `a solved board hides the highlight and ignores selection and shift keys`() {
        // TODO: start from a board one shift from solved
        // TODO: shift it solved -> the ending title shows, no tiles reverse video
        // TODO: S, L -> board unchanged
        // TODO: F5 still opens the Save dialog
        // TODO: board_solved.shift_keys == hjkl
    }

    @Test
    fun `shift_keys reports mixed when both kinds of key were used`() {
        // TODO: on a board two shifts from solved, solve it with one arrow shift and one HJKL shift
        // TODO: board_solved.shift_keys == mixed
    }
}
