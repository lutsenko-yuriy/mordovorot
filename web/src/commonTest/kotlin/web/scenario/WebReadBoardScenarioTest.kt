package web.scenario

import kotlin.test.Test

/** GH-167 scenarios: the board a screen reader reads during play - Read board's words and which
 *  tiles are home. The press itself and the grid's markup are checked by the harness
 *  (`keys_check.sh`, `grid=1`). */
class WebReadBoardScenarioTest {

    @Test
    fun `the board read-out says each row, then how many tiles are in place`() = webTest {
        // TODO: Start on ONE_SHIFT_FROM_SOLVED.
        // TODO: The read-out is "Row 1: 4, 1, 2, 3. Row 2: 5, 6, 7, 8. Row 3: 9, 10, 11, 12. Row 4: 13, 14, 15, 16. 12 tiles in place."
        // TODO: Rows 2-4 are marked home, row 1 isn't.
    }

    @Test
    fun `the board read-out follows a shift`() = webTest {
        // TODO: Start on ONE_SHIFT_FROM_SOLVED.
        // TODO: Click row 2's right arrow.
        // TODO: The read-out's row 2 is "8, 5, 6, 7" and it ends "8 tiles in place."
        // TODO: None of row 2's tiles are marked home.
    }

    @Test
    fun `mid-drag the board read-out stays on the settled board`() = webTest {
        // TODO: Start on ONE_SHIFT_FROM_SOLVED.
        // TODO: Press a tile on row 2 and drag it 1.5 tiles right, without releasing.
        // TODO: The read-out and the home marks are the same as before the drag.
        // TODO: Release: the read-out now reflects the drag's shifts.
    }

    @Test
    fun `the board read-out uses the tile labels shown, previewed ones too`() = webTest {
        // TODO: Start on ONE_SHIFT_FROM_SOLVED.
        // TODO: Open Labels and mark Letters: with the dialog open, the read-out's row 1 is "D, A, B, C".
        // TODO: Cancel: row 1 is back to "4, 1, 2, 3".
        // TODO: Open Labels again, mark Letters, press OK: row 1 is "D, A, B, C".
    }

    @Test
    fun `a solved board has no read-out`() = webTest {
        // TODO: Start on ONE_SHIFT_FROM_SOLVED.
        // TODO: Click row 1's left arrow, which solves the board.
        // TODO: The frame has no board read-out and no home marks.
    }

    @Test
    fun `the solution screen has no read-out`() = webTest {
        // TODO: Start on a board with Mordie's solution found, as WebGiveUpScenarioTest does.
        // TODO: Press Give Up, then Yes: on the solution screen the frame has no board read-out.
        // TODO: Press Next: still no board read-out.
    }
}
