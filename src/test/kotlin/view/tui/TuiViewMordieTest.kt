package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import testing.FakeTerminal
import testing.FakeViewModel
import testing.testTuiView

/** GH-83: the viewModel's Mordie line and ending reach the frame, identically in both input modes. */
class TuiViewMordieTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)
    private val inputs = listOf<() -> TuiInput>(::MouseInput, ::KeyboardInput)

    private fun lastFrame(input: TuiInput, viewModel: FakeViewModel): List<String> = runBlocking {
        val terminal = FakeTerminal(terminalSize = terminalSize)
        testTuiView(terminal, input = input, viewModel = viewModel).play()
        terminal.frames.last().replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")
    }

    @Test
    fun `the Mordie line is drawn under Moves`() {
        for (input in inputs) {
            val rows = lastFrame(input(), FakeViewModel().apply { mordieLine = "Alright! Let's sort this one out!" })
            val layout = BoardLayout(terminalSize, 4)

            val shown = rows.subList(layout.mordieRow, layout.mordieRow + layout.mordieRows).joinToString(" ") { it.trim() }.trim()

            assertEquals("Mordie: Alright! Let's sort this one out!", shown)
        }
    }

    @Test
    fun `a solved board's title is Mordie's ending, or Congratulations without one`() {
        for (input in inputs) {
            val ending = "Cool, you matched me! 3 moves each!"
            val layout = BoardLayout(terminalSize, 4)

            assertEquals(ending, lastFrame(input(), FakeViewModel().apply { solved = true; mordieEnding = ending })[layout.titleRow].trim())
            assertEquals(TITLE_SOLVED, lastFrame(input(), FakeViewModel().apply { solved = true })[layout.titleRow].trim())
        }
    }
}
