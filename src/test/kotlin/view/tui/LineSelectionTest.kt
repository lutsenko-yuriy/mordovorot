package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
import view.tui.LineSelection.Axis.COLUMN
import view.tui.LineSelection.Axis.ROW

/** GH-101's keyboard line selection: [LineSelection.select] (WASD), [LineSelection.shift]
 *  (HJKL/arrows) and [LineSelection.clampedTo]. Pure, on a 4x4 board unless noted. */
class LineSelectionTest {

    private val side = 4

    @Test
    fun `starts on row 1 with column 1 remembered`() {
        assertEquals(LineSelection(0, 0, ROW), LineSelection.INITIAL)
    }

    @Test
    fun `a select key on the active axis moves that line, wrapping at both edges`() {
        assertEquals(LineSelection(1, 2, ROW), LineSelection(0, 2, ROW).select(Direction.DOWN, side))
        assertEquals(LineSelection(3, 2, ROW), LineSelection(0, 2, ROW).select(Direction.UP, side))
        assertEquals(LineSelection(0, 2, ROW), LineSelection(3, 2, ROW).select(Direction.DOWN, side))
        assertEquals(LineSelection(1, 3, COLUMN), LineSelection(1, 2, COLUMN).select(Direction.RIGHT, side))
        assertEquals(LineSelection(1, 0, COLUMN), LineSelection(1, 3, COLUMN).select(Direction.RIGHT, side))
        assertEquals(LineSelection(1, 3, COLUMN), LineSelection(1, 0, COLUMN).select(Direction.LEFT, side))
    }

    @Test
    fun `a select key on the other axis switches to the remembered line without moving it`() {
        assertEquals(LineSelection(1, 2, ROW), LineSelection(1, 2, COLUMN).select(Direction.UP, side))
        assertEquals(LineSelection(1, 2, ROW), LineSelection(1, 2, COLUMN).select(Direction.DOWN, side))
        assertEquals(LineSelection(1, 2, COLUMN), LineSelection(1, 2, ROW).select(Direction.LEFT, side))
        assertEquals(LineSelection(1, 2, COLUMN), LineSelection(1, 2, ROW).select(Direction.RIGHT, side))
    }

    @Test
    fun `a shift key on the active axis shifts the selected line`() {
        val row = LineSelection(1, 2, ROW)
        val column = LineSelection(1, 2, COLUMN)

        assertEquals(LineSelection.ShiftOutcome.Shift(HitTarget.ShiftLeft(1)), row.shift(Direction.LEFT))
        assertEquals(LineSelection.ShiftOutcome.Shift(HitTarget.ShiftRight(1)), row.shift(Direction.RIGHT))
        assertEquals(LineSelection.ShiftOutcome.Shift(HitTarget.ShiftUp(2)), column.shift(Direction.UP))
        assertEquals(LineSelection.ShiftOutcome.Shift(HitTarget.ShiftDown(2)), column.shift(Direction.DOWN))
    }

    @Test
    fun `a shift key on the other axis switches without shifting`() {
        val row = LineSelection(1, 2, ROW)
        val column = LineSelection(1, 2, COLUMN)

        assertEquals(LineSelection.ShiftOutcome.Switched(column), row.shift(Direction.UP))
        assertEquals(LineSelection.ShiftOutcome.Switched(column), row.shift(Direction.DOWN))
        assertEquals(LineSelection.ShiftOutcome.Switched(row), column.shift(Direction.LEFT))
        assertEquals(LineSelection.ShiftOutcome.Switched(row), column.shift(Direction.RIGHT))
    }

    @Test
    fun `clampedTo pulls each index inside a smaller board and keeps the axis`() {
        assertEquals(LineSelection(2, 1, COLUMN), LineSelection(3, 1, COLUMN).clampedTo(3))
        assertEquals(LineSelection(2, 2, ROW), LineSelection(4, 4, ROW).clampedTo(3))
        assertEquals(LineSelection(3, 1, ROW), LineSelection(3, 1, ROW).clampedTo(5))
    }
}
