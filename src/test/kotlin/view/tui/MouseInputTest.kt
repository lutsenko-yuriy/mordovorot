package view.tui

import InputMode
import testing.FakeTerminal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Direct coverage of GH-18's extracted [MouseInput] strategy - the translation [TuiViewBoardTest]/
 * [TuiViewDialogTest] already cover indirectly through a full [TuiView.play], now pinned on its
 * own so the WU3 refactor doesn't rely solely on those indirect assertions.
 */
class MouseInputTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)
    private val boardLayout = BoardLayout(terminalSize, squareSide = 4)
    private val input = MouseInput()

    @Test
    fun `prepare enables mouse reporting`() {
        val terminal = FakeTerminal(terminalSize = terminalSize)

        input.prepare(terminal)

        assertEquals(true, terminal.mouseReportingEnabled)
    }

    @Test
    fun `a board click activates whatever the layout hit-tests it to, including Nothing`() {
        val (lx, ly) = boardLayout.leftArrowPosition(1)
        assertEquals(InputAction.Activate(HitTarget.ShiftLeft(1)), input.onBoardEvent(TerminalEvent.MouseClick(lx, ly), boardLayout))

        val deadSpace = TerminalEvent.MouseClick(lx + 5, ly + 1)
        assertEquals(InputAction.Activate(HitTarget.Nothing), input.onBoardEvent(deadSpace, boardLayout))
    }

    @Test
    fun `EndOfInput on the board quits, everything else is None`() {
        assertEquals(InputAction.Quit, input.onBoardEvent(TerminalEvent.EndOfInput, boardLayout))
        assertEquals(InputAction.None, input.onBoardEvent(TerminalEvent.Enter, boardLayout))
        assertEquals(InputAction.None, input.onBoardEvent(TerminalEvent.Escape, boardLayout))
        assertEquals(InputAction.None, input.onBoardEvent(TerminalEvent.Arrow(Direction.UP), boardLayout))
    }

    @Test
    fun `dialog keys map one-for-one onto text editing and submit-cancel actions`() {
        val dialog = Dialog(Dialog.Kind.SAVE, "Save game", buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")))
        val layout = DialogLayout(dialog, terminalSize)

        assertEquals(InputAction.TextChar('h'), input.onDialogEvent(TerminalEvent.KeyPress('h'), dialog, layout))
        assertEquals(InputAction.EraseChar, input.onDialogEvent(TerminalEvent.Backspace, dialog, layout))
        assertEquals(InputAction.Submit, input.onDialogEvent(TerminalEvent.Enter, dialog, layout))
        assertEquals(InputAction.Cancel, input.onDialogEvent(TerminalEvent.Escape, dialog, layout))
        assertEquals(InputAction.Quit, input.onDialogEvent(TerminalEvent.EndOfInput, dialog, layout))
        assertEquals(InputAction.None, input.onDialogEvent(TerminalEvent.Arrow(Direction.UP), dialog, layout))
    }

    @Test
    fun `a dialog click activates whatever the dialog layout hit-tests it to`() {
        val dialog = Dialog(Dialog.Kind.SAVE, "Save game", buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")))
        val layout = DialogLayout(dialog, terminalSize)
        val saveButton = layout.buttons().first { it.target == HitTarget.DialogButton("save") }

        val action = input.onDialogEvent(TerminalEvent.MouseClick(saveButton.x, layout.buttonsRow()), dialog, layout)

        assertEquals(InputAction.Activate(HitTarget.DialogButton("save")), action)
    }

    @Test
    fun `decorateBoard offers Keyboard and Console on the toolbar (GH-30), decorateDialog stays the identity`() {
        val state = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false)
        assertEquals(state.copy(modeButtons = listOf(InputMode.KEYBOARD, InputMode.CONSOLE)), input.decorateBoard(state))

        val dialog = Dialog(Dialog.Kind.EXIT, "Save before quitting?", buttons = listOf(DialogButtonSpec("yes", "Yes")))
        assertEquals(dialog, input.decorateDialog(dialog))
    }
}
