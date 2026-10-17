package web.scenario

import kotlin.test.Test

/** GH-110 scenarios: dragging a row or column. Same setup as [WebStartupScenarioTest], with drags
 *  given to `WebGame` as press/move/release in pixels. */
class WebDragScenarioTest {

    @Test
    fun `a drag of k tiles is k shifts and one move`() {
        // WU6
        // TODO: 1. On a known 4x4, drag row 1 two tiles right and release.
        // TODO: 2. Row 1 is shifted by 2; Moves went up by 1; Mordie's shift count went up by 2.
    }

    @Test
    fun `releasing at the starting position changes nothing`() {
        // WU6
        // TODO: 1. Drag a column half a tile down and back; release.
        // TODO: 2. The board and Moves are unchanged.
    }

    @Test
    fun `dragging on a solved board does nothing`() {
        // WU6
        // TODO: 1. Solve the board; drag a row one tile.
        // TODO: 2. The board and Moves are unchanged.
    }

    @Test
    fun `shift_input on board_solved - arrows, drag or mixed`() {
        // WU6
        // TODO: 1. Solve with arrows only: shift_input: arrows.
        // TODO: 2. Solve with drags only: shift_input: drag.
        // TODO: 3. Solve with both: shift_input: mixed.
        // TODO: 4. shift_keys never appears.
    }
}
