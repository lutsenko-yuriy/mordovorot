package view.tui

import InputMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import viewmodel.GiveUpButton
import viewmodel.Playback
import viewmodel.ShiftKey
import viewmodel.SolutionState

/**
 * Direct coverage of [KeyboardInput]'s board (GH-101's WASD/HJKL) and dialog (GH-18) translation, in isolation from
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

    private fun key(c: Char) = TerminalEvent.KeyPress(c)

    private fun arrow(d: Direction) = TerminalEvent.Arrow(d)

    private fun input(selection: KeyboardSelection = KeyboardSelection()) =
        KeyboardInput(selection).also { it.decorateBoard(unsolvedBoard()) }

    @Test
    fun `W A S D move or switch the selection and redraw`() {
        val selection = KeyboardSelection()
        val input = input(selection)

        assertEquals(InputAction.Redraw, input.onBoardEvent(key('s'), boardLayout))
        assertEquals(LineSelection(1, 0, LineSelection.Axis.ROW), selection.current)
        assertEquals(InputAction.Redraw, input.onBoardEvent(key('d'), boardLayout))
        assertEquals(LineSelection(1, 0, LineSelection.Axis.COLUMN), selection.current)
        input.onBoardEvent(key('a'), boardLayout)
        assertEquals(LineSelection(1, 3, LineSelection.Axis.COLUMN), selection.current)
        input.onBoardEvent(key('w'), boardLayout)
        assertEquals(LineSelection(1, 3, LineSelection.Axis.ROW), selection.current)
    }

    @Test
    fun `H L and the left right arrows shift the selected row, tagged with the key that did it`() {
        val input = input()

        assertEquals(InputAction.Shift(HitTarget.ShiftLeft(0), ShiftKey.HJKL), input.onBoardEvent(key('h'), boardLayout))
        assertEquals(InputAction.Shift(HitTarget.ShiftRight(0), ShiftKey.HJKL), input.onBoardEvent(key('l'), boardLayout))
        assertEquals(InputAction.Shift(HitTarget.ShiftLeft(0), ShiftKey.ARROW), input.onBoardEvent(arrow(Direction.LEFT), boardLayout))
        assertEquals(InputAction.Shift(HitTarget.ShiftRight(0), ShiftKey.ARROW), input.onBoardEvent(arrow(Direction.RIGHT), boardLayout))
    }

    @Test
    fun `J K and the down up arrows shift the selected column`() {
        val input = input(KeyboardSelection().apply { current = LineSelection(0, 2, LineSelection.Axis.COLUMN) })

        assertEquals(InputAction.Shift(HitTarget.ShiftDown(2), ShiftKey.HJKL), input.onBoardEvent(key('j'), boardLayout))
        assertEquals(InputAction.Shift(HitTarget.ShiftUp(2), ShiftKey.HJKL), input.onBoardEvent(key('k'), boardLayout))
        assertEquals(InputAction.Shift(HitTarget.ShiftDown(2), ShiftKey.ARROW), input.onBoardEvent(arrow(Direction.DOWN), boardLayout))
        assertEquals(InputAction.Shift(HitTarget.ShiftUp(2), ShiftKey.ARROW), input.onBoardEvent(arrow(Direction.UP), boardLayout))
    }

    @Test
    fun `a shift key on the other axis switches the selection instead of shifting`() {
        val selection = KeyboardSelection()
        val input = input(selection)

        assertEquals(InputAction.Redraw, input.onBoardEvent(key('j'), boardLayout))
        assertEquals(LineSelection(0, 0, LineSelection.Axis.COLUMN), selection.current)
        assertEquals(InputAction.Redraw, input.onBoardEvent(arrow(Direction.RIGHT), boardLayout))
        assertEquals(LineSelection(0, 0, LineSelection.Axis.ROW), selection.current)
    }

    @Test
    fun `upper case letters work the same`() {
        val selection = KeyboardSelection()
        val input = input(selection)

        input.onBoardEvent(key('S'), boardLayout)
        assertEquals(InputAction.Shift(HitTarget.ShiftRight(1), ShiftKey.HJKL), input.onBoardEvent(key('L'), boardLayout))
    }

    @Test
    fun `Enter and Space no longer do anything on the board`() {
        val input = input()

        assertEquals(InputAction.None, input.onBoardEvent(TerminalEvent.Enter, boardLayout))
        assertEquals(InputAction.None, input.onBoardEvent(key(' '), boardLayout))
    }

    @Test
    fun `F5 and F6 activate Save and Load, Escape activates Exit`() {
        val input = input()

        assertEquals(InputAction.Activate(HitTarget.ToolbarSave), input.onBoardEvent(TerminalEvent.FunctionKey(5), boardLayout))
        assertEquals(InputAction.Activate(HitTarget.ToolbarLoad), input.onBoardEvent(TerminalEvent.FunctionKey(6), boardLayout))
        assertEquals(InputAction.Activate(HitTarget.ToolbarExit), input.onBoardEvent(TerminalEvent.Escape, boardLayout))
    }

    @Test
    fun `F7 requests a switch to mouse mode`() {
        val input = input()

        assertEquals(InputAction.Activate(HitTarget.ToolbarMode(InputMode.MOUSE)), input.onBoardEvent(TerminalEvent.FunctionKey(7), boardLayout))
    }

    @Test
    fun `F8 activates whichever Give Up row button is live, and nothing while it is dimmed or hidden (GH-126)`() {
        val expected = mapOf(
            GiveUpButton.GIVE_UP to InputAction.Activate(HitTarget.GiveUp),
            GiveUpButton.MORDIES_SOLUTION to InputAction.Activate(HitTarget.MordiesSolution),
            GiveUpButton.DIMMED to InputAction.None,
            GiveUpButton.HIDDEN to InputAction.None,
        )
        for ((button, action) in expected) {
            val input = KeyboardInput(KeyboardSelection())
            input.decorateBoard(unsolvedBoard().copy(giveUpButton = button))

            assertEquals(action, input.onBoardEvent(TerminalEvent.FunctionKey(8), boardLayout), "$button")
        }
    }

    private fun solutionScreen() = ScreenState.forBoard(
        (0 until 16).toList(), 4, solved = false, giveUpButton = GiveUpButton.DIMMED,
        solution = SolutionState(listOf("1. Row 1 → ×1"), current = null, Playback.READY),
    )

    @Test
    fun `on the solution screen Space and Enter press Play, W S and the up down arrows scroll (GH-126)`() {
        val selection = KeyboardSelection()
        val input = KeyboardInput(selection)
        input.decorateBoard(solutionScreen())

        assertEquals(InputAction.Activate(HitTarget.Play), input.onBoardEvent(key(' '), boardLayout))
        assertEquals(InputAction.Activate(HitTarget.Play), input.onBoardEvent(TerminalEvent.Enter, boardLayout))
        assertEquals(InputAction.Scroll(-1), input.onBoardEvent(key('w'), boardLayout))
        assertEquals(InputAction.Scroll(-1), input.onBoardEvent(arrow(Direction.UP), boardLayout))
        assertEquals(InputAction.Scroll(1), input.onBoardEvent(key('S'), boardLayout))
        assertEquals(InputAction.Scroll(1), input.onBoardEvent(arrow(Direction.DOWN), boardLayout))
        for (event in listOf(key('a'), key('d'), key('h'), key('j'), arrow(Direction.LEFT))) {
            assertEquals(InputAction.None, input.onBoardEvent(event, boardLayout), "$event")
        }
        assertEquals(LineSelection.INITIAL, selection.current)
        assertEquals(InputAction.Activate(HitTarget.ToolbarSave), input.onBoardEvent(TerminalEvent.FunctionKey(5), boardLayout))
    }

    @Test
    fun `off the solution screen Space and Enter do nothing (GH-126)`() {
        val input = input()

        assertEquals(InputAction.None, input.onBoardEvent(key(' '), boardLayout))
        assertEquals(InputAction.None, input.onBoardEvent(TerminalEvent.Enter, boardLayout))
    }

    @Test
    fun `the solution screen's hints are play and scroll, with no shift hint (GH-126)`() {
        val decorated = KeyboardInput(KeyboardSelection()).decorateBoard(solutionScreen())

        assertNull(decorated.selection)
        assertEquals(listOf("Space  play/pause", "W/S  scroll"), decorated.selectionHint)
        assertEquals(emptyList(), decorated.shiftHint)
    }

    @Test
    fun `F9 activates the New toolbar button (GH-44 WU3)`() {
        assertEquals(InputAction.Activate(HitTarget.ToolbarNew), input().onBoardEvent(TerminalEvent.FunctionKey(9), boardLayout))
    }

    @Test
    fun `EndOfInput quits, an unrelated key is None`() {
        val input = input()

        assertEquals(InputAction.Quit, input.onBoardEvent(TerminalEvent.EndOfInput, boardLayout))
        assertEquals(InputAction.None, input.onBoardEvent(TerminalEvent.Tab, boardLayout))
        assertEquals(InputAction.None, input.onBoardEvent(key('x'), boardLayout))
    }

    @Test
    fun `once solved, selection and shift keys are None but the toolbar shortcuts stay live`() {
        val selection = KeyboardSelection()
        val input = KeyboardInput(selection)
        input.decorateBoard(solvedBoard())

        for (event in listOf(key('s'), key('d'), key('h'), key('L'), arrow(Direction.DOWN), arrow(Direction.LEFT))) {
            assertEquals(InputAction.None, input.onBoardEvent(event, boardLayout), "$event")
        }
        assertEquals(LineSelection.INITIAL, selection.current)
        assertEquals(InputAction.Activate(HitTarget.ToolbarSave), input.onBoardEvent(TerminalEvent.FunctionKey(5), boardLayout))
        assertEquals(InputAction.Activate(HitTarget.ToolbarExit), input.onBoardEvent(TerminalEvent.Escape, boardLayout))
        assertEquals(InputAction.Activate(HitTarget.ToolbarMode(InputMode.MOUSE)), input.onBoardEvent(TerminalEvent.FunctionKey(7), boardLayout))
        assertEquals(InputAction.Activate(HitTarget.ToolbarNew), input.onBoardEvent(TerminalEvent.FunctionKey(9), boardLayout))
    }

    @Test
    fun `decorateBoard shows the selection, hides the arrows and sets the hints, toolbar shortcuts and mode buttons`() {
        val input = KeyboardInput(KeyboardSelection())

        val unsolved = input.decorateBoard(unsolvedBoard())
        assertEquals(LineSelection.INITIAL, unsolved.selection)
        assertFalse(unsolved.showArrows)
        assertEquals(listOf("W/S  row", "A/D  column"), unsolved.selectionHint)
        assertEquals(listOf("←/→ H/L  shift row", "↑/↓ K/J  shift column"), unsolved.shiftHint)
        assertEquals("F5/F6/F9 Save/Load/New · F8 Give Up · F7 Mouse · Esc", unsolved.controlsHint)
        assertEquals(true, unsolved.toolbarShortcuts)
        assertEquals(listOf(InputMode.MOUSE), unsolved.modeButtons)

        val solved = input.decorateBoard(solvedBoard())
        assertNull(solved.selection)
        assertEquals(unsolved.selectionHint, solved.selectionHint)
    }

    @Test
    fun `the controls hint fits inside an 80-column terminal (audit finding on PR #40)`() {
        val hint = KeyboardInput(KeyboardSelection()).decorateBoard(unsolvedBoard()).controlsHint

        assertTrue((hint?.length ?: 0) <= 80)
    }

    @Test
    fun `a smaller board clamps the selection, and a solved spell doesn't reset it`() {
        val selection = KeyboardSelection().apply { current = LineSelection(4, 1, LineSelection.Axis.COLUMN) }
        val input = KeyboardInput(selection)

        input.decorateBoard(solvedBoard(squareSide = 5))
        assertEquals(LineSelection(4, 1, LineSelection.Axis.COLUMN), input.decorateBoard(unsolvedBoard(squareSide = 5)).selection)
        assertEquals(LineSelection(2, 1, LineSelection.Axis.COLUMN), input.decorateBoard(unsolvedBoard(squareSide = 3)).selection)
        assertEquals(LineSelection(2, 1, LineSelection.Axis.COLUMN), selection.current)
    }

    @Test
    fun `two KeyboardInputs over one KeyboardSelection share it - a mode switch keeps it`() {
        val selection = KeyboardSelection()
        input(selection).onBoardEvent(key('s'), boardLayout)

        assertEquals(LineSelection(1, 0, LineSelection.Axis.ROW), input(selection).decorateBoard(unsolvedBoard()).selection)
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
        val input = KeyboardInput(KeyboardSelection())
        input.decorateBoard(unsolvedBoard().copy(dialog = saveDialog()))
        val dialog = saveDialog()

        assertEquals(InputAction.TextChar('h'), input.onDialogEvent(TerminalEvent.KeyPress('h'), dialog, dialogLayoutFor(dialog)))
        assertEquals(InputAction.None, input.onDialogEvent(TerminalEvent.FunctionKey(5), dialog, dialogLayoutFor(dialog)))
        assertEquals(true, input.decorateDialog(dialog).textFieldFocused)
    }

    @Test
    fun `Tab moves focus from the text field to the Save button, then Enter submits`() {
        val input = KeyboardInput(KeyboardSelection())
        input.decorateBoard(unsolvedBoard().copy(dialog = saveDialog("hi")))
        val dialog = saveDialog("hi")

        assertEquals(InputAction.Redraw, input.onDialogEvent(TerminalEvent.Tab, dialog, dialogLayoutFor(dialog)))
        assertEquals("save", input.decorateDialog(dialog).focusedButtonId)
        assertEquals(InputAction.Activate(HitTarget.DialogButton("save")), input.onDialogEvent(TerminalEvent.Enter, dialog, dialogLayoutFor(dialog)))
    }

    @Test
    fun `Escape cancels and EndOfInput quits a dialog session regardless of focus`() {
        val input = KeyboardInput(KeyboardSelection())
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
        val input = KeyboardInput(KeyboardSelection())
        val dialog = loadDialog(listOf("foo", "bar"), selected = 0)
        input.decorateBoard(unsolvedBoard().copy(dialog = dialog))

        val action = input.onDialogEvent(TerminalEvent.Arrow(Direction.DOWN), dialog, dialogLayoutFor(dialog))

        assertEquals(InputAction.Activate(HitTarget.DialogListRow(1)), action)
    }

    @Test
    fun `dialogFocusIndex resets to the first control once decorateBoard sees no dialog`() {
        val input = KeyboardInput(KeyboardSelection())
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
        // Regression: the exit flow's Yes -> Save handoff chains straight into a new dialog
        // session with no decorateBoard(dialog = null) repaint in between.
        val input = KeyboardInput(KeyboardSelection())
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
