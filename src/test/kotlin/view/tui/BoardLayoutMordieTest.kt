package view.tui

import mordie.MordieLine
import viewmodel.MordieText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** GH-83's Mordie row(s) under `Moves:`: their wrap width, reserved height and the grid offset. */
class BoardLayoutMordieTest {

    private val ample = TerminalSize(columns = 80, rows = 40)

    /** `[ New ] [ Save ] [ Load ] [ Exit ]` and `[New 1] [Save 2] [Load 3] [Exit Esc]`. */
    private val mouseToolbarWidth = 34
    private val keyboardToolbarWidth = 36

    @Test
    fun `the wrap width follows the widest widget, not the terminal`() {
        for (side in 3..5) {
            assertEquals(mouseToolbarWidth, BoardLayout(ample, side).mordieWrapWidth)
            assertEquals(keyboardToolbarWidth, BoardLayout(ample, side, toolbarShortcuts = true).mordieWrapWidth)
        }
    }

    @Test
    fun `the wrap width is capped two columns short of a narrow terminal`() {
        assertEquals(28, BoardLayout(TerminalSize(columns = 30, rows = 40), 4).mordieWrapWidth)
    }

    @Test
    fun `the reservation is the longest line's wrapped height`() {
        assertEquals(3, BoardLayout(ample, 4).mordieRows)
        assertEquals(3, BoardLayout(ample, 4, toolbarShortcuts = true).mordieRows)
        assertEquals(3, BoardLayout(TerminalSize(columns = 40, rows = 40), 4).mordieRows)
        assertEquals(5, BoardLayout(TerminalSize(columns = 20, rows = 40), 4).mordieRows)
    }

    @Test
    fun `no line Mordie can say at 99 moves needs more than the reservation`() {
        val lines = listOf(
            MordieLine.Opening,
            MordieLine.Resumed(99),
            MordieLine.Progress(99, hard = true),
            MordieLine.Found(99),
        ).map { "Mordie: ${MordieText.line(it)}" }
        for (columns in listOf(80, 40, 20)) {
            val layout = BoardLayout(TerminalSize(columns, rows = 40), 4)
            for (line in lines) assertTrue(wordWrap(line, layout.mordieWrapWidth).size <= layout.mordieRows, "$line at $columns")
        }
    }

    @Test
    fun `the Mordie rows sit under Moves and push the grid down by exactly their height`() {
        for (columns in listOf(80, 20)) {
            val layout = BoardLayout(TerminalSize(columns, rows = 40), 4)
            assertEquals(layout.movesRow + 1, layout.mordieRow)
            // A blank row after the Mordie rows, then the up-arrows, then the grid.
            assertEquals(layout.mordieRow + layout.mordieRows + 1, layout.upArrowPosition(0).second)
            assertEquals(layout.upArrowPosition(0).second + 1, layout.gridOrigin().second)
        }
    }

    @Test
    fun `a 5x5 board still fits the controls hint on a 24-row terminal`() {
        val layout = BoardLayout(TerminalSize(columns = 80, rows = 24), 5, toolbarShortcuts = true)

        // The controls hint is drawn three rows under the toolbar (ScreenRenderer.render).
        assertTrue(layout.toolbarRow + 3 < 24)
    }

    @Test
    fun `on 6x6 one row is reserved for the sit-out line, which runs to its own width (GH-160)`() {
        val sitsOut = "Mordie: ${MordieText.line(MordieLine.SitsOut(6))}"
        for (shortcuts in listOf(false, true)) {
            val layout = BoardLayout(TerminalSize(columns = 80, rows = 24), 6, toolbarShortcuts = shortcuts)
            assertEquals(1, layout.mordieRows)
            assertEquals(listOf(sitsOut), wordWrap(sitsOut, layout.mordieWrapWidth))
        }
        assertEquals(28, BoardLayout(TerminalSize(columns = 30, rows = 40), 6).mordieWrapWidth)
    }

    @Test
    fun `a 6x6 board fits the controls hint on a 24-row terminal (GH-160)`() {
        val layout = BoardLayout(TerminalSize(columns = 80, rows = 24), 6, toolbarShortcuts = true)

        assertTrue(layout.toolbarRow + 3 < 24)
    }
}
