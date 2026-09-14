package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Direct coverage of GH-18's [KeyboardInput] board and dialog translation, in isolation from
 * [TuiView] - [TuiViewKeyboardBoardTest]/[TuiViewKeyboardSolvedStateTest] cover the same ground
 * end-to-end, this file pins the translation itself.
 */
class KeyboardInputTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)
    private val boardLayout = BoardLayout(terminalSize, squareSide = 4, arrowsEnabled = true, toolbarShortcuts = true)

    private fun unsolvedBoard(squareSide: Int = 4) =
        ScreenState.forBoard(board = (0 until squareSide * squareSide).toList(), squareSide = squareSide, solved = false)

    private fun solvedBoard(squareSide: Int = 4) =
        ScreenState.forBoard(board = (0 until squareSide * squareSide).toList(), squareSide = squareSide, solved = true)

    @Test
    fun `an arrow key moves the cursor per the ArrowRing table`() {
        val input = KeyboardInput()
        input.decorateBoard(unsolvedBoard())

        assertEquals(InputAction.Redraw, input.onBoardEvent(TerminalEvent.Arrow(Direction.DOWN), boardLayout))
        // LEFT[0] + Down -> LEFT[1] (ArrowRing's table) - confirmed via the next decoration.
        assertEquals(ArrowCursor(Edge.LEFT, 1), input.decorateBoard(unsolvedBoard()).cursor)
    }

    @Test
    fun `Enter and Space both activate whatever arrow the cursor is currently on`() {
        val input = KeyboardInput()
        input.decorateBoard(unsolvedBoard())

        assertEquals(InputAction.Activate(HitTarget.ShiftLeft(0)), input.onBoardEvent(TerminalEvent.Enter, boardLayout))
        assertEquals(InputAction.Activate(HitTarget.ShiftLeft(0)), input.onBoardEvent(TerminalEvent.KeyPress(' '), boardLayout))
    }

    @Test
    fun `F5 and F6 activate Save and Load, Escape activates Exit`() {
        val input = KeyboardInput()
        input.decorateBoard(unsolvedBoard())

        assertEquals(InputAction.Activate(HitTarget.ToolbarSave), input.onBoardEvent(TerminalEvent.FunctionKey(5), boardLayout))
        assertEquals(InputAction.Activate(HitTarget.ToolbarLoad), input.onBoardEvent(TerminalEvent.FunctionKey(6), boardLayout))
        assertEquals(InputAction.Activate(HitTarget.ToolbarExit), input.onBoardEvent(TerminalEvent.Escape, boardLayout))
    }

    @Test
    fun `F7 is decoded but means nothing on the board - Escape is Exit's trigger now`() {
        val input = KeyboardInput()
        input.decorateBoard(unsolvedBoard())

        assertEquals(InputAction.None, input.onBoardEvent(TerminalEvent.FunctionKey(7), boardLayout))
    }

    @Test
    fun `EndOfInput quits, an unrelated key is None`() {
        val input = KeyboardInput()
        input.decorateBoard(unsolvedBoard())

        assertEquals(InputAction.Quit, input.onBoardEvent(TerminalEvent.EndOfInput, boardLayout))
        assertEquals(InputAction.None, input.onBoardEvent(TerminalEvent.Tab, boardLayout))
    }

    @Test
    fun `once solved, arrow keys and Enter are None but the toolbar shortcuts stay live`() {
        val input = KeyboardInput()
        input.decorateBoard(solvedBoard())

        assertEquals(InputAction.None, input.onBoardEvent(TerminalEvent.Arrow(Direction.DOWN), boardLayout))
        assertEquals(InputAction.None, input.onBoardEvent(TerminalEvent.Enter, boardLayout))
        assertEquals(InputAction.Activate(HitTarget.ToolbarSave), input.onBoardEvent(TerminalEvent.FunctionKey(5), boardLayout))
        assertEquals(InputAction.Activate(HitTarget.ToolbarExit), input.onBoardEvent(TerminalEvent.Escape, boardLayout))
    }

    @Test
    fun `decorateBoard drops the cursor once solved and always sets the controls hint and toolbar shortcuts`() {
        val input = KeyboardInput()

        val unsolved = input.decorateBoard(unsolvedBoard())
        assertEquals(ArrowCursor(Edge.LEFT, 0), unsolved.cursor)

        val solved = input.decorateBoard(solvedBoard())
        assertNull(solved.cursor)
        assertEquals(true, solved.toolbarShortcuts)
        assertEquals("Arrows: move · Enter/Space: shift · F5 Save · F6 Load · Esc Exit", solved.controlsHint)
    }

    @Test
    fun `the cursor resets to LEFT-0 when the board goes from solved back to unsolved`() {
        val input = KeyboardInput()
        input.decorateBoard(unsolvedBoard())
        input.onBoardEvent(TerminalEvent.Arrow(Direction.DOWN), boardLayout) // moves off LEFT[0]
        input.decorateBoard(unsolvedBoard())
        input.decorateBoard(solvedBoard())

        assertEquals(ArrowCursor(Edge.LEFT, 0), input.decorateBoard(unsolvedBoard()).cursor)
    }

    private fun saveDialog(typed: String = "") = Dialog(
        kind = Dialog.Kind.SAVE,
        title = "Save game",
        textFieldValue = typed,
        buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")),
    )

    private fun dialogLayoutFor(dialog: Dialog) = DialogLayout(dialog, terminalSize)

    @Test
    fun `a fresh dialog focuses the text field, so printable keys type and F-keys are ignored`() {
        val input = KeyboardInput()
        input.decorateBoard(unsolvedBoard().copy(dialog = saveDialog()))
        val dialog = saveDialog()

        assertEquals(InputAction.TextChar('h'), input.onDialogEvent(TerminalEvent.KeyPress('h'), dialog, dialogLayoutFor(dialog)))
        assertEquals(InputAction.None, input.onDialogEvent(TerminalEvent.FunctionKey(5), dialog, dialogLayoutFor(dialog)))
        assertEquals(true, input.decorateDialog(dialog).textFieldFocused)
    }

    @Test
    fun `Tab moves focus from the text field to the Save button, then Enter submits`() {
        val input = KeyboardInput()
        input.decorateBoard(unsolvedBoard().copy(dialog = saveDialog("hi")))
        val dialog = saveDialog("hi")

        assertEquals(InputAction.Redraw, input.onDialogEvent(TerminalEvent.Tab, dialog, dialogLayoutFor(dialog)))
        assertEquals("save", input.decorateDialog(dialog).focusedButtonId)
        assertEquals(InputAction.Activate(HitTarget.DialogButton("save")), input.onDialogEvent(TerminalEvent.Enter, dialog, dialogLayoutFor(dialog)))
    }

    @Test
    fun `Escape cancels and EndOfInput quits a dialog session regardless of focus`() {
        val input = KeyboardInput()
        val dialog = saveDialog()
        input.decorateBoard(unsolvedBoard().copy(dialog = dialog))

        assertEquals(InputAction.Cancel, input.onDialogEvent(TerminalEvent.Escape, dialog, dialogLayoutFor(dialog)))
        assertEquals(InputAction.Quit, input.onDialogEvent(TerminalEvent.EndOfInput, dialog, dialogLayoutFor(dialog)))
    }

    private fun loadDialog(items: List<String>, selected: Int) = Dialog(
        kind = Dialog.Kind.LOAD,
        title = "Load game",
        listItems = items,
        selectedIndex = selected,
        buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")),
    )

    @Test
    fun `Down on a Load dialog selects the next row, same target a click on it would produce`() {
        val input = KeyboardInput()
        val dialog = loadDialog(listOf("foo", "bar"), selected = 0)
        input.decorateBoard(unsolvedBoard().copy(dialog = dialog))

        val action = input.onDialogEvent(TerminalEvent.Arrow(Direction.DOWN), dialog, dialogLayoutFor(dialog))

        assertEquals(InputAction.Activate(HitTarget.DialogListRow(1)), action)
    }

    @Test
    fun `dialogFocusIndex resets to the first control once decorateBoard sees no dialog`() {
        val input = KeyboardInput()
        val dialog = saveDialog()
        input.decorateBoard(unsolvedBoard().copy(dialog = dialog))
        input.onDialogEvent(TerminalEvent.Tab, dialog, dialogLayoutFor(dialog)) // focus -> the Save button
        input.decorateBoard(unsolvedBoard()) // dialog closed

        val reopened = saveDialog()
        input.decorateBoard(unsolvedBoard().copy(dialog = reopened))
        assertEquals(true, input.decorateDialog(reopened).textFieldFocused)
    }

    @Test
    fun `onDialogOpened resets focus to the first control, even with no board repaint in between`() {
        // Audit finding on GH-18 WU4 PR #33: the exit flow's Yes -> Save handoff, and a Save
        // dialog's own invalid-name re-prompt, chain straight into a new dialog session with no
        // decorateBoard(dialog = null) repaint in between - the state.dialog == null reset alone
        // can't see that boundary, which is why onDialogOpened exists as an explicit signal.
        val input = KeyboardInput()
        val exitDialog = Dialog(
            kind = Dialog.Kind.EXIT,
            title = "Save before quitting?",
            buttons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("no", "No"), DialogButtonSpec("cancel", "Cancel")),
        )
        input.decorateBoard(unsolvedBoard().copy(dialog = exitDialog))
        input.onDialogEvent(TerminalEvent.Tab, exitDialog, dialogLayoutFor(exitDialog)) // focus -> "no"
        input.onDialogEvent(TerminalEvent.Tab, exitDialog, dialogLayoutFor(exitDialog)) // focus -> "cancel"

        // The Save dialog opens directly from here, with no intervening decorateBoard(dialog = null).
        input.onDialogOpened()
        val saveDialog = saveDialog()
        input.decorateBoard(unsolvedBoard().copy(dialog = saveDialog))

        assertEquals(true, input.decorateDialog(saveDialog).textFieldFocused)
        assertEquals(InputAction.TextChar('o'), input.onDialogEvent(TerminalEvent.KeyPress('o'), saveDialog, dialogLayoutFor(saveDialog)))
    }
}
