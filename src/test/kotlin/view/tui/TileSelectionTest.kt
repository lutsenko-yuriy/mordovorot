package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals

/** GH-170's selected tile: [TileSelection.move], [TileSelection.shiftTarget] and
 *  [TileSelection.clampedTo]. Pure, on a 4x4 board. */
class TileSelectionTest {

    private val side = 4

    @Test
    fun `starts on row 1, column 1`() {
        assertEquals(TileSelection(0, 0), TileSelection.INITIAL)
    }

    @Test
    fun `move steps one tile, wrapping at every edge`() {
        assertEquals(TileSelection(2, 2), TileSelection(1, 2).move(Direction.DOWN, side))
        assertEquals(TileSelection(3, 2), TileSelection(0, 2).move(Direction.UP, side))
        assertEquals(TileSelection(0, 2), TileSelection(3, 2).move(Direction.DOWN, side))
        assertEquals(TileSelection(1, 0), TileSelection(1, 3).move(Direction.RIGHT, side))
        assertEquals(TileSelection(1, 3), TileSelection(1, 0).move(Direction.LEFT, side))
    }

    @Test
    fun `left and right shift the row, up and down the column`() {
        val selection = TileSelection(1, 2)
        assertEquals(HitTarget.ShiftLeft(1), selection.shiftTarget(Direction.LEFT))
        assertEquals(HitTarget.ShiftRight(1), selection.shiftTarget(Direction.RIGHT))
        assertEquals(HitTarget.ShiftUp(2), selection.shiftTarget(Direction.UP))
        assertEquals(HitTarget.ShiftDown(2), selection.shiftTarget(Direction.DOWN))
    }

    @Test
    fun `clampedTo pulls the tile inside a smaller board and keeps it otherwise`() {
        assertEquals(TileSelection(2, 1), TileSelection(4, 1).clampedTo(3))
        assertEquals(TileSelection(2, 2), TileSelection(4, 4).clampedTo(3))
        assertEquals(TileSelection(1, 2), TileSelection(1, 2).clampedTo(side))
    }
}
