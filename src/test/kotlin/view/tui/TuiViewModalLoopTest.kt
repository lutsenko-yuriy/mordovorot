package view.tui

import InputMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import testing.FakeTerminal
import testing.FakeViewModel
import testing.RecordingAnalyticsService

/** Pins the cancel/quit contract all four toolbar dialogs share (GH-64): Escape and the Cancel
 *  button each track `dialog_cancelled` exactly once; end of input closes without tracking. */
class TuiViewModalLoopTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    private class Case(val name: String, val open: (BoardLayout) -> Pair<Int, Int>, val dialog: Dialog)

    private val cancelButton = DialogButtonSpec("cancel", "Cancel")

    private val cases = listOf(
        Case("save", BoardLayout::saveButtonPosition, Dialog(Dialog.Kind.SAVE, "Save game", buttons = listOf(DialogButtonSpec("save", "Save"), cancelButton))),
        Case("load", BoardLayout::loadButtonPosition, Dialog(Dialog.Kind.LOAD, "Load game", listItems = listOf("foo"), buttons = listOf(DialogButtonSpec("load", "Load"), cancelButton))),
        Case("exit", BoardLayout::exitButtonPosition, Dialog(Dialog.Kind.EXIT, "Save before quitting?", buttons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("no", "No"), cancelButton))),
        Case(
            "size", BoardLayout::newButtonPosition,
            Dialog(Dialog.Kind.SIZE, "New game size", buttons = listOf(DialogButtonSpec("3", "3x3"), DialogButtonSpec("4", "4x4"), DialogButtonSpec("5", "5x5"), cancelButton)),
        ),
    )

    private fun cancelCount(case: Case, vararg afterOpen: (Dialog) -> TerminalEvent): Int {
        val viewModel = FakeViewModel().apply { saveNames = listOf("foo") }
        val analytics = RecordingAnalyticsService()
        val (x, y) = case.open(BoardLayout(terminalSize, viewModel.side, arrowsEnabled = true, modeButtons = listOf(InputMode.KEYBOARD)))
        val events = mutableListOf<TerminalEvent>(TerminalEvent.MouseClick(x, y))
        afterOpen.forEach { events += it(case.dialog) }
        runBlocking { TuiView.create(FakeTerminal(events = events, terminalSize = terminalSize), analytics, viewModel = viewModel).play() }
        return analytics.events.count { it.name == "dialog_cancelled" && it.properties["dialog"] == case.name }
    }

    private fun clickCancel(dialog: Dialog): TerminalEvent {
        val layout = DialogLayout(dialog, terminalSize)
        return TerminalEvent.MouseClick(layout.buttons().first { it.target == HitTarget.DialogButton("cancel") }.x, layout.buttonsRow())
    }

    @Test
    fun `Escape tracks dialog_cancelled exactly once for every dialog`() {
        cases.forEach { assertEquals(1, cancelCount(it, { TerminalEvent.Escape }), it.name) }
    }

    @Test
    fun `Cancel button tracks dialog_cancelled exactly once for every dialog`() {
        cases.forEach { assertEquals(1, cancelCount(it, ::clickCancel), it.name) }
    }

    @Test
    fun `end of input closes every dialog without tracking dialog_cancelled`() {
        cases.forEach { assertEquals(0, cancelCount(it), it.name) }
    }
}
