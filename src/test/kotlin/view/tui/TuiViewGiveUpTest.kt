package view.tui

import InputMode
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
import viewmodel.Playback
import viewmodel.SolutionState

/** GH-126: the TUI's Give Up row, its confirmation and the replay clock, over a [FakeViewModel]. */
class TuiViewGiveUpTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)
    private val analytics = RecordingAnalyticsService()

    private fun layout(giveUp: GiveUpButton, playback: Playback? = null) =
        BoardLayout(terminalSize, 4, arrowsEnabled = playback == null, modeButtons = listOf(InputMode.KEYBOARD), giveUp = giveUp, playback = playback)

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

    private fun playing(steps: Int = 3) = FakeViewModel().apply {
        giveUpButton = GiveUpButton.DIMMED
        solution = SolutionState(List(steps) { "${it + 1}. Row 1 → ×1" }, current = 0, Playback.PLAYING)
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
    fun `Play on the solution screen calls playPause`() {
        val viewModel = playing().apply { solution = solution!!.copy(current = null, playback = Playback.READY) }

        play(viewModel, click(layout(GiveUpButton.DIMMED, Playback.READY).playButtonPosition()))

        assertTrue("playPause" in viewModel.calls)
    }

    @Test
    fun `while playing, each Tick is one shift, 500 ms apart, with another 500 ms after a finished step`() {
        val viewModel = playing().apply { replayShiftResults = mutableListOf(false, true, false) }

        val terminal = play(viewModel, TerminalEvent.Tick, TerminalEvent.Tick, TerminalEvent.Tick)

        assertEquals(3, viewModel.calls.count { it == "replayShift" })
        assertEquals(listOf<Long?>(500, 500, 1000, 500), terminal.timeouts)
    }

    @Test
    fun `when not playing, reads block and a Tick shifts nothing`() {
        val viewModel = playing().apply { solution = solution!!.copy(playback = Playback.PAUSED) }

        val terminal = play(viewModel, TerminalEvent.Tick)

        assertEquals(listOf<Long?>(null, null), terminal.timeouts)
        assertFalse("replayShift" in viewModel.calls)
    }

    @Test
    fun `an open dialog holds the replay - its reads block (D7)`() {
        val viewModel = playing()

        val terminal = play(viewModel, click(layout(GiveUpButton.DIMMED, Playback.PLAYING).saveButtonPosition()), TerminalEvent.Escape)

        assertEquals(listOf<Long?>(500, null, 500), terminal.timeouts)
        assertFalse("replayShift" in viewModel.calls)
    }

    @Test
    fun `the wheel scrolls a long step list`() {
        val viewModel = playing(steps = 20)
        val solutionLayout = layout(GiveUpButton.DIMMED, Playback.PLAYING)

        val terminal = play(viewModel, TerminalEvent.Wheel(up = false), TerminalEvent.Wheel(up = false))

        val rows = terminal.frames.last().replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")
        assertEquals("3. Row 1 → ×1", rows[solutionLayout.stepListTop].substring(solutionLayout.stepListX).trim())
    }

    @Test
    fun `in keyboard mode F8 opens the confirmation and Enter on the solution screen presses Play`() {
        val giveUp = FakeViewModel().apply { giveUpButton = GiveUpButton.GIVE_UP }
        play(giveUp, TerminalEvent.FunctionKey(8), TerminalEvent.Escape, input = KeyboardInput(KeyboardSelection()))
        assertEquals(1, analytics.events.count { it.name == "screen_give_up_dialog" })

        val ready = playing().apply { solution = solution!!.copy(playback = Playback.READY) }
        play(ready, TerminalEvent.Enter, input = KeyboardInput(KeyboardSelection()))
        assertTrue("playPause" in ready.calls)
    }
}
