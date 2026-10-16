package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** GH-83's Mordie line and solved title in the rendered frame. */
class ScreenRendererMordieTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)
    private val renderer = ScreenRenderer()
    private val layout = BoardLayout(terminalSize, 4)
    private val found = "Found it! I had to make 11 moves to find the solution! Can you beat it?"

    private fun rows(frame: String) = frame.replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")

    private fun board(solved: Boolean = false, solvedTitle: String = TITLE_SOLVED, mordieLine: String? = null) =
        ScreenState.forBoard((0..15).toList(), 4, solved, solvedTitle = solvedTitle, mordieLine = mordieLine)

    @Test
    fun `the line is wrapped to the widget width and each row centered`() {
        val rows = rows(renderer.render(board(mordieLine = found), terminalSize))

        val wrapped = wordWrap("Mordie: $found", layout.mordieWrapWidth)
        assertTrue(wrapped.size > 1)
        wrapped.forEachIndexed { i, line ->
            assertEquals(" ".repeat((80 - line.length) / 2) + line, rows[layout.mordieRow + i].trimEnd())
        }
    }

    @Test
    fun `only the Mordie prefix is magenta`() {
        val frame = renderer.render(board(mordieLine = found), terminalSize)

        assertTrue(frame.contains("\u001B[35mMordie:\u001B[39m Found"))
    }

    @Test
    fun `no line leaves the reserved rows blank`() {
        val rows = rows(renderer.render(board(), terminalSize))

        for (i in 0 until layout.mordieRows) assertEquals("", rows[layout.mordieRow + i].trim())
    }

    @Test
    fun `the solved title is the given ending, and Congratulations by default`() {
        val ending = "You beat me! I was still at 0 moves."

        assertEquals(ending, rows(renderer.render(board(solved = true, solvedTitle = ending), terminalSize))[layout.titleRow].trim())
        assertEquals(TITLE_SOLVED, rows(renderer.render(board(solved = true), terminalSize))[layout.titleRow].trim())
        assertEquals(TITLE_UNSOLVED, rows(renderer.render(board(solvedTitle = ending), terminalSize))[layout.titleRow].trim())
    }
}
