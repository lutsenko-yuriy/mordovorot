package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers GH-3's board geometry: given a terminal size and the board's square side, computing
 * where the 16 shift arrows and 3 toolbar buttons land, and hit-testing a click coordinate
 * against them. Pure geometry, no terminal or presenter involved. Coordinates are read back
 * from the layout's own accessors rather than hardcoded, so the test stays valid across layout
 * tweaks - only the hit-test contract (a click at that coordinate resolves to that target) is
 * asserted.
 */
class BoardLayoutTest {

    private val ampleTerminal = TerminalSize(columns = 80, rows = 40)

    @Test
    fun `hit-tests each of the 16 shift arrows to its correct 0-based row or column`() {
        val layout = BoardLayout(ampleTerminal, squareSide = 4)

        for (row in 0 until 4) {
            val (x, y) = layout.leftArrowPosition(row)
            assertEquals(HitTarget.ShiftLeft(row), layout.hitTest(x, y))

            val (rx, ry) = layout.rightArrowPosition(row)
            assertEquals(HitTarget.ShiftRight(row), layout.hitTest(rx, ry))
        }

        for (col in 0 until 4) {
            val (x, y) = layout.upArrowPosition(col)
            assertEquals(HitTarget.ShiftUp(col), layout.hitTest(x, y))

            val (dx, dy) = layout.downArrowPosition(col)
            assertEquals(HitTarget.ShiftDown(col), layout.hitTest(dx, dy))
        }
    }

    @Test
    fun `hit-tests each toolbar button`() {
        val layout = BoardLayout(ampleTerminal, squareSide = 4)

        val (saveX, saveY) = layout.saveButtonPosition()
        assertEquals(HitTarget.ToolbarSave, layout.hitTest(saveX, saveY))

        val (loadX, loadY) = layout.loadButtonPosition()
        assertEquals(HitTarget.ToolbarLoad, layout.hitTest(loadX, loadY))

        val (exitX, exitY) = layout.exitButtonPosition()
        assertEquals(HitTarget.ToolbarExit, layout.hitTest(exitX, exitY))
    }

    @Test
    fun `a click inside a tile cell returns Nothing`() {
        val layout = BoardLayout(ampleTerminal, squareSide = 4)

        // One row below the row-0 left arrow, one column right of it: inside the grid body,
        // not on any border or arrow.
        val (arrowX, arrowY) = layout.leftArrowPosition(0)

        assertEquals(HitTarget.Nothing, layout.hitTest(arrowX + 3, arrowY + 1))
    }

    @Test
    fun `off-board coordinates return Nothing`() {
        val layout = BoardLayout(ampleTerminal, squareSide = 4)

        assertEquals(HitTarget.Nothing, layout.hitTest(-1, -1))
        assertEquals(HitTarget.Nothing, layout.hitTest(999, 999))
    }

    @Test
    fun `arrow regions return Nothing when arrows are disabled`() {
        val layout = BoardLayout(ampleTerminal, squareSide = 4, arrowsEnabled = false)

        val (x, y) = layout.leftArrowPosition(0)

        assertEquals(HitTarget.Nothing, layout.hitTest(x, y))
    }

    @Test
    fun `a terminal too small to fit the board still produces a layout without throwing`() {
        val layout = BoardLayout(TerminalSize(columns = 5, rows = 5), squareSide = 4)

        layout.leftArrowPosition(0)
        layout.saveButtonPosition()
        layout.hitTest(0, 0)
        layout.hitTest(1000, 1000)
    }
}
