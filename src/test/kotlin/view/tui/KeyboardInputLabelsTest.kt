package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
import viewmodel.SolutionState

/** GH-139 in [KeyboardInput]: F4 opens Labels in every board state, and the Labels dialog opens on
 *  the marked row, where Enter confirms. */
class KeyboardInputLabelsTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)
    private val boardLayout = BoardLayout(terminalSize, squareSide = 4, arrowsEnabled = true, toolbarShortcuts = true)

    private fun board(solved: Boolean) = ScreenState.forBoard(board = (0 until 16).toList(), squareSide = 4, solved = solved)
    private fun unsolvedBoard() = board(solved = false)
    private fun solvedBoard() = board(solved = true)
    private fun arrow(d: Direction) = TerminalEvent.Arrow(d)
    private fun dialogLayoutFor(dialog: Dialog) = DialogLayout(dialog, terminalSize)

    private fun loadDialog(items: List<String>, selected: Int) = Dialog(
        kind = Dialog.Kind.LOAD,
        title = "Load game",
        listItems = items,
        selectedIndex = selected,
        buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")),
    )

    @Test
    fun `F4 activates Labels while playing, once solved and on the solution screen (GH-139)`() {
        val solution = unsolvedBoard().copy(solution = SolutionState(listOf("1. Row 1 → ×1"), null, atStart = true, atEnd = false))
        for (state in listOf(unsolvedBoard(), solvedBoard(), solution)) {
            val input = KeyboardInput(KeyboardSelection()).also { it.decorateBoard(state) }
            assertEquals(InputAction.Activate(HitTarget.ToolbarLabels), input.onBoardEvent(TerminalEvent.FunctionKey(4), boardLayout))
        }
    }

    private fun labelsDialog(selected: Int) = Dialog(
        kind = Dialog.Kind.LABELS,
        title = "Tile labels",
        listItems = listOf("Numbers", "Numbers from 0", "Pairs", "Pairs from 0", "Letters", "Letter pairs"),
        selectedIndex = selected,
        buttons = listOf(DialogButtonSpec("ok", "OK"), DialogButtonSpec("cancel", "Cancel")),
    )

    @Test
    fun `a fresh Labels dialog focuses the marked row, and Down marks the next (GH-139)`() {
        val input = KeyboardInput(KeyboardSelection())
        input.onDialogOpened()
        val dialog = input.decorateDialog(labelsDialog(selected = 2))

        assertEquals(InputAction.Activate(HitTarget.DialogListRow(3)), input.onDialogEvent(arrow(Direction.DOWN), dialog, dialogLayoutFor(dialog)))
    }

    @Test
    fun `Enter on a Labels row submits it, while Enter on a Load row only selects (GH-139)`() {
        val input = KeyboardInput(KeyboardSelection())
        input.onDialogOpened()
        val labels = input.decorateDialog(labelsDialog(selected = 4))
        assertEquals(InputAction.Submit, input.onDialogEvent(TerminalEvent.Enter, labels, dialogLayoutFor(labels)))

        input.onDialogOpened()
        val load = input.decorateDialog(loadDialog(listOf("foo", "bar"), selected = 0))
        assertEquals(InputAction.Activate(HitTarget.DialogListRow(0)), input.onDialogEvent(TerminalEvent.Enter, load, dialogLayoutFor(load)))
    }

    @Test
    fun `Down and Up skip a disabled Labels row (GH-160)`() {
        val input = KeyboardInput(KeyboardSelection())
        input.onDialogOpened()
        val dialog = input.decorateDialog(labelsDialog(selected = 3).copy(disabledRows = setOf(4)))

        assertEquals(InputAction.Activate(HitTarget.DialogListRow(5)), input.onDialogEvent(arrow(Direction.DOWN), dialog, dialogLayoutFor(dialog)))
        assertEquals(InputAction.Activate(HitTarget.DialogListRow(3)), input.onDialogEvent(arrow(Direction.UP), dialog, dialogLayoutFor(dialog)))
    }
}
