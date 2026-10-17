package view.tui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import testing.FakeTerminal
import testing.FakeViewModel
import testing.testTuiView
import viewmodel.ViewModel

/**
 * Covers GH-18's keyboard-driven solved-state view: the cursor disappearing and arrow
 * keys/Enter/Space becoming inert once solved, the toolbar (and its F5/F6/Esc shortcuts) staying
 * live, and the transition back to a navigable board when a load restores an unsolved game.
 * Mirrors `TuiViewSolvedStateTest` (GH-3's mouse equivalent).
 */
class TuiViewKeyboardSolvedStateTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    private fun view(terminal: FakeTerminal, viewModel: ViewModel): TuiView =
        testTuiView(terminal, input = KeyboardInput(KeyboardSelection()), viewModel = viewModel)

    @Test
    fun `once solved, no selection is highlighted and the selection and shift keys are inert`(): Unit = runBlocking {
        val viewModel = FakeViewModel().apply { solved = true }
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.KeyPress('s'), TerminalEvent.KeyPress('l'), TerminalEvent.Arrow(Direction.DOWN)),
            terminalSize = terminalSize,
        )

        view(terminal, viewModel).play()

        assertTrue(viewModel.calls.none { it.startsWith("shift") })
        assertTrue(terminal.frames.last().contains("Congratulations ✓"))
        assertTrue(terminal.frames.none { it.contains("\u001B[7m") })
    }

    @Test
    fun `F5, F6, and Escape still open their dialogs after solve`(): Unit = runBlocking {
        // Any frame, not just the last - see TuiViewKeyboardBoardTest's equivalent test.
        suspend fun framesFor(event: TerminalEvent): List<String> {
            val viewModel = FakeViewModel().apply { solved = true }
            val terminal = FakeTerminal(events = mutableListOf(event), terminalSize = terminalSize)
            view(terminal, viewModel).play()
            return terminal.frames
        }

        assertTrue(framesFor(TerminalEvent.FunctionKey(5)).any { it.contains("Save game") })
        assertTrue(framesFor(TerminalEvent.FunctionKey(6)).any { it.contains("Load game") })
        assertTrue(framesFor(TerminalEvent.Escape).any { it.contains("Save before quitting?") })
    }

    @Test
    fun `loading an unsolved save from the Congratulations screen brings back the selection and the shift keys`(): Unit = runBlocking {
        val delegate = FakeViewModel().apply { solved = true; saveNames = listOf("save1") }
        val viewModel = object : ViewModel by delegate {
            override suspend fun loadGame(name: String) {
                delegate.loadGame(name)
                delegate.solved = false
            }
        }
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.FunctionKey(6), // open the Load dialog - focus starts on its only row
                TerminalEvent.Tab, // move focus from the row to the Load button
                TerminalEvent.Enter, // confirm - loads "save1", flipping solved to false
                TerminalEvent.KeyPress('h'), // row 1 is selected again - shift it
            ),
            terminalSize = terminalSize,
        )

        view(terminal, viewModel).play()

        assertTrue(delegate.calls.contains("loadGame(save1)"))
        assertTrue(delegate.calls.contains("shiftLeft(0, HJKL)"))
        val boardFrame = terminal.frames.last()
        assertTrue(boardFrame.contains("Mordovorot"))
        assertFalse(boardFrame.contains("Congratulations"))
    }
}
