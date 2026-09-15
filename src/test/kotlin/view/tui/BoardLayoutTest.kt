package view.tui

import InputMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Covers GH-3's board geometry: given a terminal size and the board's square side, computing
 * where the 16 shift arrows and 3 toolbar buttons land, and hit-testing a click coordinate
 * against them. Pure geometry, no terminal or viewModel involved. Coordinates are read back
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

    @Test
    fun `toolbarShortcuts hit-tests the wider F-key labels at the coordinates the renderer draws them`() {
        val layout = BoardLayout(ampleTerminal, squareSide = 4, toolbarShortcuts = true)

        val (saveX, saveY) = layout.saveButtonPosition()
        assertEquals(HitTarget.ToolbarSave, layout.hitTest(saveX, saveY))

        val (loadX, loadY) = layout.loadButtonPosition()
        assertEquals(HitTarget.ToolbarLoad, layout.hitTest(loadX, loadY))

        val (exitX, exitY) = layout.exitButtonPosition()
        assertEquals(HitTarget.ToolbarExit, layout.hitTest(exitX, exitY))

        // The wider "[Save F5]" label pushes the next button further right than "[ Save ]" does -
        // asserting the coordinates actually differ, not just that hit-testing works at whatever
        // they happen to be, is what would catch toolbarShortcuts being ignored by hitTest.
        val plainLayout = BoardLayout(ampleTerminal, squareSide = 4, toolbarShortcuts = false)
        assertNotEquals(plainLayout.loadButtonPosition(), layout.loadButtonPosition())
    }

    @Test
    fun `mode buttons sit on their own row below the toolbar and hit-test to ToolbarMode`() {
        val layout = BoardLayout(ampleTerminal, squareSide = 4, modeButtons = listOf(InputMode.KEYBOARD, InputMode.CONSOLE))

        val buttons = layout.toolbarButtons()
        assertEquals(
            listOf(HitTarget.ToolbarSave, HitTarget.ToolbarLoad, HitTarget.ToolbarExit, HitTarget.ToolbarMode(InputMode.KEYBOARD), HitTarget.ToolbarMode(InputMode.CONSOLE)),
            buttons.map { it.target },
        )
        val (save, load, exit, keyboard, console) = buttons
        assertEquals(layout.toolbarRow, save.y)
        assertEquals(layout.toolbarRow, load.y)
        assertEquals(layout.toolbarRow, exit.y)
        assertEquals(layout.modeRow, keyboard.y)
        assertEquals(layout.modeRow, console.y)
        for (button in buttons) assertEquals(button.target, layout.hitTest(button.range.first, button.y))
    }

    @Test
    fun `a terminal too narrow for a single five-button row still fits Save Load Exit split across two rows`() {
        // Regression test (audit finding on PR #38): a single row of all five buttons needed 51
        // columns; splitting Save/Load/Exit onto their own row (unchanged from before this
        // ticket) keeps it fitting at the same width the three-button toolbar always has.
        val narrow = TerminalSize(columns = 40, rows = 24)
        val layout = BoardLayout(narrow, squareSide = 4, modeButtons = listOf(InputMode.KEYBOARD, InputMode.CONSOLE))

        val (exitX, exitY) = layout.exitButtonPosition()
        assertEquals(HitTarget.ToolbarExit, layout.hitTest(exitX, exitY))
        val (keyboardX, keyboardY) = layout.toolbarButtons().first { it.target == HitTarget.ToolbarMode(InputMode.KEYBOARD) }.let { it.x to it.y }
        assertEquals(HitTarget.ToolbarMode(InputMode.KEYBOARD), layout.hitTest(keyboardX, keyboardY))
    }

    @Test
    fun `Save, Load, and Exit positions stay correct once mode buttons lengthen the toolbar`() {
        val layout = BoardLayout(ampleTerminal, squareSide = 4, modeButtons = listOf(InputMode.KEYBOARD, InputMode.CONSOLE))

        val (saveX, saveY) = layout.saveButtonPosition()
        assertEquals(HitTarget.ToolbarSave, layout.hitTest(saveX, saveY))

        val (loadX, loadY) = layout.loadButtonPosition()
        assertEquals(HitTarget.ToolbarLoad, layout.hitTest(loadX, loadY))

        val (exitX, exitY) = layout.exitButtonPosition()
        assertEquals(HitTarget.ToolbarExit, layout.hitTest(exitX, exitY))
    }

    @Test
    fun `mode button labels switch to F-key shortcuts under toolbarShortcuts`() {
        val plain = BoardLayout(ampleTerminal, squareSide = 4, modeButtons = listOf(InputMode.MOUSE, InputMode.CONSOLE))
        val shortcuts = BoardLayout(ampleTerminal, squareSide = 4, toolbarShortcuts = true, modeButtons = listOf(InputMode.MOUSE, InputMode.CONSOLE))

        assertEquals(listOf("[ Mouse ]", "[ Console ]"), plain.toolbarButtons().map { it.text }.filter { it.contains("Mouse") || it.contains("Console") })
        assertEquals(listOf("[Mouse F7]", "[Console F8]"), shortcuts.toolbarButtons().map { it.text }.filter { it.contains("Mouse") || it.contains("Console") })
    }
}
