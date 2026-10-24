package view.tui

import InputMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import testing.FakeTerminal
import testing.FakeViewModel
import testing.RecordingAnalyticsService
import testing.testTuiView
import viewmodel.TileLabels

/** GH-139's Tile labels dialog through [TuiView] over a [FakeViewModel]: the toolbar button and
 *  F4 open it, a row click or arrow marks a scheme, OK hands it to the viewModel, Cancel and
 *  Escape don't. The labels' analytics are the viewModel's, covered by `ViewModelTileLabelsTest`. */
class TuiViewLabelsDialogTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)
    private val boardLayout = BoardLayout(terminalSize, 4, modeButtons = listOf(InputMode.KEYBOARD))
    private val dialogLayout = DialogLayout(
        Dialog(
            Dialog.Kind.LABELS, "Tile labels", listItems = List(6) { "" },
            buttons = listOf(DialogButtonSpec("ok", "OK"), DialogButtonSpec("cancel", "Cancel")),
        ),
        terminalSize,
    )

    private fun clickButton(id: String) = dialogLayout.buttons().first { it.target == HitTarget.DialogButton(id) }
        .let { TerminalEvent.MouseClick(it.x, dialogLayout.buttonsRow()) }

    private val openLabels = boardLayout.labelsButtonPosition().let { (x, y) -> TerminalEvent.MouseClick(x, y) }
    private fun row(index: Int) = TerminalEvent.MouseClick(dialogLayout.left + 2, dialogLayout.listRowPosition(index))

    private class Run(val terminal: FakeTerminal, val viewModel: FakeViewModel, val analytics: RecordingAnalyticsService)

    private fun play(events: List<TerminalEvent>, input: TuiInput = MouseInput(), labels: TileLabels = TileLabels.DEFAULT): Run = runBlocking {
        val terminal = FakeTerminal(events.toMutableList(), terminalSize)
        val viewModel = FakeViewModel().also { it.tileLabels = labels }
        val analytics = RecordingAnalyticsService()
        testTuiView(terminal, analytics, input, viewModel = viewModel).play()
        Run(terminal, viewModel, analytics)
    }

    @Test
    fun `the toolbar button opens the dialog with the current scheme marked, and OK on a clicked row changes it`() {
        val run = play(listOf(openLabels, row(5), clickButton("ok")), labels = TileLabels.PAIRS)

        val dialogFrame = run.terminal.frames.first { "Tile labels" in it }
        assertTrue("● Pairs            11 12 13 14" in dialogFrame, dialogFrame)
        assertTrue("○ Letter pairs     AA AB AC AD" in dialogFrame, dialogFrame)
        assertEquals(listOf("tileLabelsDialogOpened(toolbar)", "changeTileLabels(LETTER_PAIRS)"), run.viewModel.calls.filter { "Labels" in it })
    }

    @Test
    fun `Cancel tracks dialog_cancelled and changes nothing`() {
        val run = play(listOf(openLabels, row(4), clickButton("cancel")))

        assertTrue(run.viewModel.calls.none { it.startsWith("changeTileLabels") })
        assertEquals(listOf<Any?>("labels"), run.analytics.events.filter { it.name == "dialog_cancelled" }.map { it.properties["dialog"] })
    }

    @Test
    fun `F4 opens it from the keyboard, Down marks the next scheme and Enter confirms it`() {
        val run = play(
            listOf(TerminalEvent.FunctionKey(4), TerminalEvent.Arrow(Direction.DOWN), TerminalEvent.Enter),
            KeyboardInput(KeyboardSelection()),
            labels = TileLabels.PAIRS,
        )

        assertEquals(listOf("tileLabelsDialogOpened(shortcut)", "changeTileLabels(PAIRS_FROM_0)"), run.viewModel.calls.filter { "Labels" in it })
    }

    @Test
    fun `Escape cancels from the keyboard`() {
        val run = play(listOf(TerminalEvent.FunctionKey(4), TerminalEvent.Arrow(Direction.DOWN), TerminalEvent.Escape), KeyboardInput(KeyboardSelection()))

        assertTrue(run.viewModel.calls.none { it.startsWith("changeTileLabels") })
        assertEquals(listOf<Any?>("labels"), run.analytics.events.filter { it.name == "dialog_cancelled" }.map { it.properties["dialog"] })
    }
}
