package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import viewmodel.GiveUpButton
import viewmodel.SolutionState

/** GH-126: drawing the Give Up row and the solution screen. */
class ScreenRendererGiveUpTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)
    private val renderer = ScreenRenderer()

    private fun state(giveUp: GiveUpButton, solution: SolutionState? = null, moves: Int = 3) =
        ScreenState.forBoard((0..15).toList(), 4, solved = false, moveCount = moves, giveUpButton = giveUp, solution = solution)

    private fun rowsOf(frame: String) = frame.replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")

    private fun highlighted(frame: String) =
        Regex("\u001B\\[7m(.*?)\u001B\\[27m").findAll(frame).map { it.groupValues[1] }.toList()

    private fun steps(n: Int) = List(n) { "${(it + 1).toString().padStart(n.toString().length, '0')}. Row 1 → ×1" }

    @Test
    fun `a live Give Up is drawn plain on its row, a dimmed one dimmed, a hidden one not at all`() {
        val layout = BoardLayout(terminalSize, 4)

        val live = renderer.render(state(GiveUpButton.GIVE_UP), terminalSize)
        assertEquals("[ Give Up ]", rowsOf(live)[layout.giveUpRow].trim())
        assertFalse("\u001B[2m[ Give Up ]" in live)

        val dimmed = renderer.render(state(GiveUpButton.DIMMED), terminalSize)
        assertTrue("\u001B[2m[" in dimmed && "]\u001B[22m" in dimmed)
        assertEquals("[ Give Up ]", rowsOf(dimmed)[layout.giveUpRow].trim())

        assertEquals("", rowsOf(renderer.render(state(GiveUpButton.HIDDEN), terminalSize))[layout.giveUpRow].trim())
    }

    @Test
    fun `the solution screen hides Moves and the arrows, and lists the steps right of the board`() {
        val solution = SolutionState(steps(3), current = null, atStart = true, atEnd = false, moves = emptyList())
        val state = state(GiveUpButton.DIMMED, solution)
        val layout = BoardLayout(terminalSize, 4, giveUp = GiveUpButton.DIMMED, solution = solution)

        val rows = rowsOf(renderer.render(state, terminalSize))

        assertEquals("", rows[layout.movesRow].trim())
        val (lx, ly) = layout.leftArrowPosition(0)
        val (dx, dy) = layout.downArrowPosition(0)
        assertEquals(' ', rows[ly][lx])
        assertFalse("▼" in rows[dy])
        for (i in 0..2) assertEquals("${i + 1}. Row 1 → ×1", rows[layout.stepListTop + i].substring(layout.stepListX).trim())
        assertEquals("[ ◀ Prev ] [ Next ▶ ] [ ↻ Restart ]", rows[layout.downArrowPosition(0).second].trim())
    }

    @Test
    fun `the current step is reverse video`() {
        val frame = renderer.render(state(GiveUpButton.DIMMED, SolutionState(steps(3), current = 1, atStart = false, atEnd = false, moves = emptyList())), terminalSize)

        assertEquals(listOf("2. Row 1 → ×1"), highlighted(frame))
    }

    @Test
    fun `a long list shows the rows from the window's offset`() {
        val state = state(GiveUpButton.DIMMED, SolutionState(steps(20), current = 12, atStart = false, atEnd = false, moves = emptyList())).copy(stepOffset = 5)
        val layout = BoardLayout(terminalSize, 4, giveUp = GiveUpButton.DIMMED, solution = state.solution)

        val rows = rowsOf(renderer.render(state, terminalSize))

        assertEquals("06. Row 1 → ×1", rows[layout.stepListTop].substring(layout.stepListX).trim())
        assertEquals("14. Row 1 → ×1", rows[layout.stepListTop + layout.stepRows - 1].substring(layout.stepListX).trim())
        assertFalse(rows.any { "15." in it }, "nothing past the window is drawn")
    }

    @Test
    fun `off the solution screen the moves row still shows`() {
        val rows = rowsOf(renderer.render(state(GiveUpButton.GIVE_UP), terminalSize))

        assertEquals("Moves: 3", rows[BoardLayout(terminalSize, 4).movesRow].trim())
    }

    @Test
    fun `an inert Prev, Next or Restart is dimmed`() {
        val atStart = renderer.render(state(GiveUpButton.DIMMED, SolutionState(steps(3), null, atStart = true, atEnd = false, moves = emptyList())), terminalSize)
        assertTrue("\u001B[2m[ ◀ Prev ]\u001B[22m" in atStart)
        assertTrue("\u001B[2m[ ↻ Restart ]\u001B[22m" in atStart)
        assertFalse("\u001B[2m[ Next ▶ ]" in atStart)

        val atEnd = renderer.render(state(GiveUpButton.DIMMED, SolutionState(steps(3), 2, atStart = false, atEnd = true, moves = emptyList())), terminalSize)
        assertTrue("\u001B[2m[ Next ▶ ]\u001B[22m" in atEnd)
        assertFalse("\u001B[2m[ ◀ Prev ]" in atEnd)
    }
}
