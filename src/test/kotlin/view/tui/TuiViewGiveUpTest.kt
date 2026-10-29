package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import testing.FakeTerminal
import testing.FakeViewModel
import testing.RecordingAnalyticsService
import testing.testTuiView
import viewmodel.GiveUpButton
import viewmodel.SolutionState

/** GH-126: the TUI's Give Up row, its confirmation and the solution screen, over a [FakeViewModel]. */
class TuiViewGiveUpTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)
    private val analytics = RecordingAnalyticsService()

    private fun layout(giveUp: GiveUpButton, solution: SolutionState? = null) =
        BoardLayout(terminalSize, 4, arrowsEnabled = solution == null, giveUp = giveUp, solution = solution)

    private val confirm = Dialog(
        Dialog.Kind.GIVE_UP, "Give up and see Mordie's solution?",
        buttons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("cancel", "No")),
    )

    private fun clickButton(id: String): TerminalEvent {
        val dialogLayout = DialogLayout(confirm, terminalSize)
        return TerminalEvent.MouseClick(dialogLayout.buttons().first { it.target == HitTarget.DialogButton(id) }.x, dialogLayout.buttonsRow())
    }

    private fun click(position: Pair<Int, Int>?) = TerminalEvent.MouseClick(position!!.first, position.second)

    private fun play(viewModel: FakeViewModel, vararg events: TerminalEvent, input: TuiInput = MouseInput()): FakeTerminal = runBlocking {
        val terminal = FakeTerminal(events.toMutableList(), terminalSize)
        testTuiView(terminal, analytics, input = input, viewModel = viewModel).play()
        terminal
    }

    /** On the solution screen, one shift in. */
    private fun onScreen(steps: Int = 3) = FakeViewModel().apply {
        giveUpButton = GiveUpButton.DIMMED
        solution = SolutionState(List(steps) { "${it + 1}. Row 1 → ×1" }, current = 0, atStart = false, atEnd = false, moves = emptyList())
    }

    @Test
    fun `Give Up asks first, tracking the dialog with the board size and moves, and Yes gives up`() {
        val viewModel = FakeViewModel().apply { giveUpButton = GiveUpButton.GIVE_UP; moves = 2 }

        val terminal = play(viewModel, click(layout(GiveUpButton.GIVE_UP).giveUpButtonPosition()), clickButton("yes"))

        assertTrue(terminal.frames.any { "Give up and see Mordie's solution?" in it })
        assertEquals(mapOf("board_size" to 4, "move_count" to 2), analytics.events.single { it.name == "screen_give_up_dialog" }.properties)
        assertTrue("giveUp" in viewModel.calls)
    }

    @Test
    fun `No and Escape keep the game and track dialog_cancelled give_up`() {
        val viewModel = FakeViewModel().apply { giveUpButton = GiveUpButton.GIVE_UP }
        val giveUp = click(layout(GiveUpButton.GIVE_UP).giveUpButtonPosition())

        play(viewModel, giveUp, clickButton("cancel"), giveUp, TerminalEvent.Escape)

        assertFalse("giveUp" in viewModel.calls)
        assertEquals(2, analytics.events.count { it.name == "dialog_cancelled" && it.properties["dialog"] == "give_up" })
    }

    @Test
    fun `Mordie's solution opens the screen with no confirmation`() {
        val viewModel = FakeViewModel().apply { giveUpButton = GiveUpButton.MORDIES_SOLUTION; solved = true }

        play(viewModel, click(layout(GiveUpButton.MORDIES_SOLUTION).giveUpButtonPosition()))

        assertTrue("showSolution" in viewModel.calls)
        assertTrue(analytics.events.none { it.name == "screen_give_up_dialog" })
    }

    @Test
    fun `Prev, Next and Restart call the viewModel`() {
        val viewModel = onScreen()
        val buttons = layout(GiveUpButton.DIMMED, viewModel.solution).replayButtons()

        play(viewModel, *buttons.map { TerminalEvent.MouseClick(it.x, it.y) }.toTypedArray())

        assertEquals(listOf("replayPrevious", "replayNext", "replayRestart"), viewModel.calls.filter { it.startsWith("replay") })
    }

    @Test
    fun `an inert Prev at the start does nothing`() {
        val viewModel = onScreen().apply { solution = solution!!.copy(current = null, atStart = true) }
        val prev = layout(GiveUpButton.DIMMED, viewModel.solution).replayButtons().first()

        play(viewModel, TerminalEvent.MouseClick(prev.x, prev.y))

        assertTrue(viewModel.calls.none { it.startsWith("replay") })
    }

    @Test
    fun `the wheel scrolls a long step list`() {
        val viewModel = onScreen(steps = 20)
        val solutionLayout = layout(GiveUpButton.DIMMED, viewModel.solution)

        val terminal = play(viewModel, TerminalEvent.Wheel(up = false), TerminalEvent.Wheel(up = false))

        val rows = terminal.frames.last().replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")
        assertEquals("3. Row 1 → ×1", rows[solutionLayout.stepListTop].substring(solutionLayout.stepListX).trim())
    }

    @Test
    fun `in keyboard mode F8 opens the confirmation and Enter on the solution screen goes next`() {
        val giveUp = FakeViewModel().apply { giveUpButton = GiveUpButton.GIVE_UP }
        play(giveUp, TerminalEvent.KeyPress('5'), TerminalEvent.Escape, input = KeyboardInput(KeyboardSelection()))
        assertEquals(1, analytics.events.count { it.name == "screen_give_up_dialog" })

        val screen = onScreen()
        play(screen, TerminalEvent.Enter, input = KeyboardInput(KeyboardSelection()))
        assertTrue("replayNext" in screen.calls)
    }

    @Test
    fun `another solution's steps start at the top, however far the last list was scrolled`() {
        val viewModel = onScreen(steps = 20).apply { solution = solution!!.copy(current = null, atStart = true) }
        val other = SolutionState(List(15) { "${it + 1}. Col 1 ↑ ×1" }, current = null, atStart = true, atEnd = false, moves = emptyList())
        val events = mutableListOf<TerminalEvent>(TerminalEvent.Wheel(up = false), TerminalEvent.Wheel(up = false), TerminalEvent.MouseClick(0, 0))
        val terminal = FakeTerminal(events, terminalSize, beforeEvent = mapOf(2 to { viewModel.solution = other }))

        runBlocking { testTuiView(terminal, analytics, viewModel = viewModel).play() }

        val solutionLayout = layout(GiveUpButton.DIMMED, other)
        val rows = terminal.frames.last().replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")
        assertEquals("1. Col 1 ↑ ×1", rows[solutionLayout.stepListTop].substring(solutionLayout.stepListX).trim())
    }
}
