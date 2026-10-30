package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals

/** GH-170: which line a key shift works on, the terminal's part of the shared [TileSelection]. */
class TileSelectionShiftTargetTest {

    @Test
    fun `left and right shift the row, up and down the column`() {
        val selection = TileSelection(1, 2)
        assertEquals(HitTarget.ShiftLeft(1), selection.shiftTarget(Direction.LEFT))
        assertEquals(HitTarget.ShiftRight(1), selection.shiftTarget(Direction.RIGHT))
        assertEquals(HitTarget.ShiftUp(2), selection.shiftTarget(Direction.UP))
        assertEquals(HitTarget.ShiftDown(2), selection.shiftTarget(Direction.DOWN))
    }
}
