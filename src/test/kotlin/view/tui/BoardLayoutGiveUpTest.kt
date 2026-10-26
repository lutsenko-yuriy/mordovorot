package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import viewmodel.GiveUpButton
import viewmodel.SolutionState

/** GH-126: the Give Up row, and the solution screen's step list and Play button. */
class BoardLayoutGiveUpTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    private fun solution(atStart: Boolean = true, atEnd: Boolean = false) = SolutionState(listOf("1. Row 1 → ×1"), null, atStart, atEnd, moves = emptyList())

    private fun layout(giveUp: GiveUpButton, solution: SolutionState? = null, side: Int = 4, shortcuts: Boolean = false, size: TerminalSize = terminalSize) =
        BoardLayout(size, side, arrowsEnabled = solution == null, toolbarShortcuts = shortcuts, giveUp = giveUp, solution = solution)

    @Test
    fun `the Give Up row takes the blank row between the down arrows and the toolbar`() {
        val plain = BoardLayout(terminalSize, 4)
        val layout = layout(GiveUpButton.GIVE_UP)

        assertEquals(plain.toolbarRow, layout.toolbarRow)
        assertEquals(layout.downArrowPosition(0).second + 1, layout.giveUpRow)
        assertEquals(layout.toolbarRow - 1, layout.giveUpRow)
    }

    @Test
    fun `a live Give Up or Mordie's solution hit-tests to its target`() {
        val giveUp = layout(GiveUpButton.GIVE_UP)
        val (x, y) = giveUp.giveUpButtonPosition()!!
        assertEquals(HitTarget.GiveUp, giveUp.hitTest(x, y))
        assertEquals("[ Give Up ]", giveUp.giveUpButton()!!.text)

        val solution = layout(GiveUpButton.MORDIES_SOLUTION)
        val (sx, sy) = solution.giveUpButtonPosition()!!
        assertEquals(HitTarget.MordiesSolution, solution.hitTest(sx, sy))
        assertEquals("[ Mordie's solution ]", solution.giveUpButton()!!.text)
    }

    @Test
    fun `a dimmed Give Up is drawn but inert, and a hidden one is absent`() {
        val dimmed = layout(GiveUpButton.DIMMED)
        val (x, y) = dimmed.giveUpButtonPosition()!!
        assertEquals(HitTarget.Nothing, dimmed.hitTest(x, y))

        assertNull(layout(GiveUpButton.HIDDEN).giveUpButton())
    }

    @Test
    fun `keyboard mode labels it with F8`() {
        assertEquals("[Give Up F8]", layout(GiveUpButton.GIVE_UP, shortcuts = true).giveUpButton()!!.text)
        assertEquals("[Mordie's solution F8]", layout(GiveUpButton.MORDIES_SOLUTION, shortcuts = true).giveUpButton()!!.text)
    }

    @Test
    fun `Prev, Next and Restart sit on the down-arrows row, each inert where it has nowhere to go`() {
        val cases = mapOf(
            solution(atStart = true) to listOf(HitTarget.Nothing, HitTarget.ReplayNext, HitTarget.Nothing),
            solution(atStart = false) to listOf(HitTarget.ReplayPrevious, HitTarget.ReplayNext, HitTarget.ReplayRestart),
            solution(atStart = false, atEnd = true) to listOf(HitTarget.ReplayPrevious, HitTarget.Nothing, HitTarget.ReplayRestart),
        )
        for ((solution, targets) in cases) {
            val layout = layout(GiveUpButton.DIMMED, solution)
            val buttons = layout.replayButtons()
            assertEquals(listOf("[ ◀ Prev ]", "[ Next ▶ ]", "[ ↻ Restart ]"), buttons.map { it.text })
            assertEquals(targets, buttons.map { it.target })
            for (button in buttons) {
                assertEquals(layout.downArrowPosition(0).second, button.y)
                assertEquals(button.target, layout.hitTest(button.x, button.y))
            }
        }
        assertTrue(layout(GiveUpButton.GIVE_UP).replayButtons().isEmpty())
    }

    @Test
    fun `the step list runs down the grid's rows, right of the board`() {
        val layout = layout(GiveUpButton.DIMMED, solution())

        assertEquals(layout.gridOrigin().second, layout.stepListTop)
        assertEquals(layout.gridHeight, layout.stepRows)
        assertEquals(layout.rightHintX, layout.stepListX)
    }

    @Test
    fun `on the solution screen the arrows are inert`() {
        val layout = layout(GiveUpButton.DIMMED, solution())
        val (x, y) = layout.leftArrowPosition(0)

        assertEquals(HitTarget.Nothing, layout.hitTest(x, y))
    }

    @Test
    fun `a 5x5 solution screen fits 80x24, the longest step included`() {
        val size = TerminalSize(columns = 80, rows = 24)
        val layout = layout(GiveUpButton.DIMMED, solution(), side = 5, shortcuts = true, size = size)

        assertTrue(layout.toolbarRow + 3 < 24, "the controls hint fits")
        assertTrue(layout.stepListTop + layout.stepRows <= layout.downArrowPosition(0).second)
        assertTrue(layout.stepListX + "147. Col 5 ↑ ×1".length <= 80)
    }
}
