package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import testing.sessionKeyboardLayout
import viewmodel.GiveUpButton
import viewmodel.ShiftKey
import viewmodel.SolutionState

/**
 * Direct coverage of [KeyboardInput]'s board (GH-170's WASD/HJKL and digits) and dialog (GH-18) translation, in isolation from
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
        KeyboardInput(selection, sessionKeyboardLayout()).also { it.decorateBoard(unsolvedBoard()) }

    @Test
    fun `W A S D move the selected tile and redraw`() {
        val selection = KeyboardSelection()
        val input = input(selection)

        assertEquals(InputAction.Redraw, input.onBoardEvent(key('s'), boardLayout))
        assertEquals(TileSelection(1, 0), selection.current)
        assertEquals(InputAction.Redraw, input.onBoardEvent(key('d'), boardLayout))
        assertEquals(TileSelection(1, 1), selection.current)
        input.onBoardEvent(key('a'), boardLayout)
        input.onBoardEvent(key('a'), boardLayout)
        assertEquals(TileSelection(1, 3), selection.current)
        input.onBoardEvent(key('w'), boardLayout)
        assertEquals(TileSelection(0, 3), selection.current)
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
        val input = input(KeyboardSelection().apply { current = TileSelection(0, 2) })

        assertEquals(InputAction.Shift(HitTarget.ShiftDown(2), ShiftKey.HJKL), input.onBoardEvent(key('j'), boardLayout))
        assertEquals(InputAction.Shift(HitTarget.ShiftUp(2), ShiftKey.HJKL), input.onBoardEvent(key('k'), boardLayout))
        assertEquals(InputAction.Shift(HitTarget.ShiftDown(2), ShiftKey.ARROW), input.onBoardEvent(arrow(Direction.DOWN), boardLayout))
        assertEquals(InputAction.Shift(HitTarget.ShiftUp(2), ShiftKey.ARROW), input.onBoardEvent(arrow(Direction.UP), boardLayout))
    }

    @Test
    fun `a shift leaves the selection where it is`() {
        val selection = KeyboardSelection().apply { current = TileSelection(1, 2) }
        val input = input(selection)

        input.onBoardEvent(key('l'), boardLayout)
        input.onBoardEvent(arrow(Direction.DOWN), boardLayout)
        assertEquals(TileSelection(1, 2), selection.current)
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
    fun `2 and 3 activate Save and Load, Escape activates Exit`() {
        val input = input()

        assertEquals(InputAction.Activate(HitTarget.ToolbarSave, shortcut = true), input.onBoardEvent(TerminalEvent.KeyPress('2'), boardLayout))
        assertEquals(InputAction.Activate(HitTarget.ToolbarLoad, shortcut = true), input.onBoardEvent(TerminalEvent.KeyPress('3'), boardLayout))
        assertEquals(InputAction.Activate(HitTarget.ToolbarExit, shortcut = true), input.onBoardEvent(TerminalEvent.Escape, boardLayout))
    }

    @Test
    fun `6 opens Controls`() {
        val input = input()

        assertEquals(InputAction.Activate(HitTarget.ToolbarControls, shortcut = true), input.onBoardEvent(TerminalEvent.KeyPress('6'), boardLayout))
    }

    @Test
    fun `5 activates whichever Give Up row button is live, and nothing while it is dimmed or hidden (GH-126)`() {
        val expected = mapOf(
            GiveUpButton.GIVE_UP to InputAction.Activate(HitTarget.GiveUp, shortcut = true),
            GiveUpButton.MORDIES_SOLUTION to InputAction.Activate(HitTarget.MordiesSolution, shortcut = true),
            GiveUpButton.DIMMED to InputAction.None,
            GiveUpButton.HIDDEN to InputAction.None,
        )
        for ((button, action) in expected) {
            val input = KeyboardInput(KeyboardSelection(), sessionKeyboardLayout())
            input.decorateBoard(unsolvedBoard().copy(giveUpButton = button))

            assertEquals(action, input.onBoardEvent(TerminalEvent.KeyPress('5'), boardLayout), "$button")
        }
    }

    private fun solutionScreen() = ScreenState.forBoard(
        (0 until 16).toList(), 4, solved = false, giveUpButton = GiveUpButton.DIMMED,
        solution = SolutionState(listOf("1. Row 1 → ×1"), current = null, atStart = true, atEnd = false, moves = emptyList()),
    )

    @Test
    fun `on the solution screen the arrows, A D and H L step, Space and Enter go next, R restarts, W S K J up down scroll`() {
        val selection = KeyboardSelection()
        val input = KeyboardInput(selection, sessionKeyboardLayout())
        input.decorateBoard(solutionScreen())

        val expected = mapOf(
            arrow(Direction.LEFT) to InputAction.Activate(HitTarget.ReplayPrevious),
            key('a') to InputAction.Activate(HitTarget.ReplayPrevious),
            arrow(Direction.RIGHT) to InputAction.Activate(HitTarget.ReplayNext),
            key('D') to InputAction.Activate(HitTarget.ReplayNext),
            key(' ') to InputAction.Activate(HitTarget.ReplayNext),
            TerminalEvent.Enter to InputAction.Activate(HitTarget.ReplayNext),
            key('r') to InputAction.Activate(HitTarget.ReplayRestart),
            key('w') to InputAction.Scroll(-1),
            arrow(Direction.UP) to InputAction.Scroll(-1),
            key('S') to InputAction.Scroll(1),
            arrow(Direction.DOWN) to InputAction.Scroll(1),
            key('h') to InputAction.Activate(HitTarget.ReplayPrevious),
            key('L') to InputAction.Activate(HitTarget.ReplayNext),
            key('k') to InputAction.Scroll(-1),
            key('J') to InputAction.Scroll(1),
            key('x') to InputAction.None,
        )
        for ((event, action) in expected) assertEquals(action, input.onBoardEvent(event, boardLayout), "$event")
        assertEquals(TileSelection.INITIAL, selection.current)
        assertEquals(InputAction.Activate(HitTarget.ToolbarSave, shortcut = true), input.onBoardEvent(TerminalEvent.KeyPress('2'), boardLayout))
    }

    @Test
    fun `off the solution screen Space, Enter and R do nothing (GH-126)`() {
        val input = input()

        assertEquals(InputAction.None, input.onBoardEvent(key(' '), boardLayout))
        assertEquals(InputAction.None, input.onBoardEvent(TerminalEvent.Enter, boardLayout))
        assertEquals(InputAction.None, input.onBoardEvent(key('r'), boardLayout))
    }

    @Test
    fun `the solution screen's hints are step, restart and scroll, with no shift hint (GH-126)`() {
        val decorated = KeyboardInput(KeyboardSelection(), sessionKeyboardLayout()).decorateBoard(solutionScreen())

        assertNull(decorated.selection)
        assertEquals(listOf("←/→ A/D H/L  step", "Space/Enter  next", "R  restart", "↑/↓ W/S K/J  scroll"), decorated.leftHint)
        assertEquals(emptyList(), decorated.rightHint)
    }

    @Test
    fun `1 activates New and 4 Labels`() {
        assertEquals(InputAction.Activate(HitTarget.ToolbarNew, shortcut = true), input().onBoardEvent(TerminalEvent.KeyPress('1'), boardLayout))
        assertEquals(InputAction.Activate(HitTarget.ToolbarLabels, shortcut = true), input().onBoardEvent(TerminalEvent.KeyPress('4'), boardLayout))
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
        val input = KeyboardInput(selection, sessionKeyboardLayout())
        input.decorateBoard(solvedBoard())

        for (event in listOf(key('s'), key('d'), key('h'), key('L'), arrow(Direction.DOWN), arrow(Direction.LEFT))) {
            assertEquals(InputAction.None, input.onBoardEvent(event, boardLayout), "$event")
        }
        assertEquals(TileSelection.INITIAL, selection.current)
        assertEquals(InputAction.Activate(HitTarget.ToolbarSave, shortcut = true), input.onBoardEvent(TerminalEvent.KeyPress('2'), boardLayout))
        assertEquals(InputAction.Activate(HitTarget.ToolbarExit, shortcut = true), input.onBoardEvent(TerminalEvent.Escape, boardLayout))
        assertEquals(InputAction.Activate(HitTarget.ToolbarControls, shortcut = true), input.onBoardEvent(TerminalEvent.KeyPress('6'), boardLayout))
        assertEquals(InputAction.Activate(HitTarget.ToolbarNew, shortcut = true), input.onBoardEvent(TerminalEvent.KeyPress('1'), boardLayout))
    }

    @Test
    fun `decorateBoard shows the selection, hides the arrows and sets the hints and toolbar shortcuts`() {
        val input = KeyboardInput(KeyboardSelection(), sessionKeyboardLayout())

        val unsolved = input.decorateBoard(unsolvedBoard())
        assertEquals(TileSelection.INITIAL, unsolved.selection)
        assertFalse(unsolved.showArrows)
        assertEquals(listOf("W/A/S/D select"), unsolved.leftHint)
        assertEquals(listOf("←/→ H/L row", "↑/↓ K/J column"), unsolved.rightHint)
        assertEquals("1 New · 2 Save · 3 Load · 4 Labels · 5 Give Up · 6 Controls · Esc Exit", unsolved.controlsHint)
        assertEquals(true, unsolved.toolbarShortcuts)

        val solved = input.decorateBoard(solvedBoard())
        assertNull(solved.selection)
        assertEquals(unsolved.leftHint, solved.leftHint)
    }

    @Test
    fun `the controls hint fits inside an 80-column terminal (audit finding on PR #40)`() {
        val hint = KeyboardInput(KeyboardSelection(), sessionKeyboardLayout()).decorateBoard(unsolvedBoard()).controlsHint

        assertTrue((hint?.length ?: 0) <= 80)
    }

    @Test
    fun `a smaller board clamps the selection, and a solved spell doesn't reset it`() {
        val selection = KeyboardSelection().apply { current = TileSelection(4, 1) }
        val input = KeyboardInput(selection, sessionKeyboardLayout())

        input.decorateBoard(solvedBoard(squareSide = 5))
        assertEquals(TileSelection(4, 1), input.decorateBoard(unsolvedBoard(squareSide = 5)).selection)
        assertEquals(TileSelection(2, 1), input.decorateBoard(unsolvedBoard(squareSide = 3)).selection)
        assertEquals(TileSelection(2, 1), selection.current)
    }

    @Test
    fun `two KeyboardInputs over one KeyboardSelection share it - a mode switch keeps it`() {
        val selection = KeyboardSelection()
        input(selection).onBoardEvent(key('s'), boardLayout)

        assertEquals(TileSelection(1, 0), input(selection).decorateBoard(unsolvedBoard()).selection)
    }

    private fun saveDialog(typed: String = "") = Dialog(
        kind = Dialog.Kind.SAVE,
        title = "Save game",
        textFieldValue = typed,
        buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")),
    )

    private fun dialogLayoutFor(dialog: Dialog) = DialogLayout(dialog, terminalSize)

    @Test
    fun `a fresh dialog focuses the text field, so printable keys and digits type`() {
        val input = KeyboardInput(KeyboardSelection(), sessionKeyboardLayout())
        input.decorateBoard(unsolvedBoard().copy(dialog = saveDialog()))
        val dialog = saveDialog()

        assertEquals(InputAction.TextChar('h'), input.onDialogEvent(TerminalEvent.KeyPress('h'), dialog, dialogLayoutFor(dialog)))
        assertEquals(InputAction.TextChar('2'), input.onDialogEvent(TerminalEvent.KeyPress('2'), dialog, dialogLayoutFor(dialog)))
        assertEquals(true, input.decorateDialog(dialog).textFieldFocused)
    }

    @Test
    fun `Tab moves focus from the text field to the Save button, then Enter submits`() {
        val input = KeyboardInput(KeyboardSelection(), sessionKeyboardLayout())
        input.decorateBoard(unsolvedBoard().copy(dialog = saveDialog("hi")))
        val dialog = saveDialog("hi")

        assertEquals(InputAction.Redraw, input.onDialogEvent(TerminalEvent.Tab, dialog, dialogLayoutFor(dialog)))
        assertEquals("save", input.decorateDialog(dialog).focusedButtonId)
        assertEquals(InputAction.Activate(HitTarget.DialogButton("save")), input.onDialogEvent(TerminalEvent.Enter, dialog, dialogLayoutFor(dialog)))
    }

    @Test
    fun `Escape cancels and EndOfInput quits a dialog session regardless of focus`() {
        val input = KeyboardInput(KeyboardSelection(), sessionKeyboardLayout())
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
        val input = KeyboardInput(KeyboardSelection(), sessionKeyboardLayout())
        val dialog = loadDialog(listOf("foo", "bar"), selected = 0)
        input.decorateBoard(unsolvedBoard().copy(dialog = dialog))

        val action = input.onDialogEvent(TerminalEvent.Arrow(Direction.DOWN), dialog, dialogLayoutFor(dialog))

        assertEquals(InputAction.Activate(HitTarget.DialogListRow(1)), action)
    }

    @Test
    fun `dialogFocusIndex resets to the first control once decorateBoard sees no dialog`() {
        val input = KeyboardInput(KeyboardSelection(), sessionKeyboardLayout())
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
        val input = KeyboardInput(KeyboardSelection(), sessionKeyboardLayout())
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
