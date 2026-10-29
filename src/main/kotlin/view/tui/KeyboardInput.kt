package view.tui

import board_model.BoardSize
import viewmodel.GiveUpButton

/**
 * GH-18's keyboard-driven [TuiInput], working the session's shared [KeyboardSelection]; its
 * dialog keys are [DialogKeys].
 *
 * Board (GH-170, ADR-0003): the keys of [BoardKeys] move the selected tile and shift its row or
 * column. The digits are the toolbar - 1 New, 2 Save, 3 Load, 4 Labels, 5 the Give Up row's
 * button, 6 Controls - and Escape opens Exit; all stay live when solved (the
 * selection and shift keys go inert instead, the selection stops rendering) and on the solution
 * screen, whose own keys are [BoardKeys.solutionAction]. Board and dialog events are mutually
 * exclusive, so Escape can't collide with a dialog's Cancel, nor a digit with the Save name field.
 */
class KeyboardInput(private val selection: KeyboardSelection) : TuiInput {

    private var squareSide = BoardSize.DEFAULT
    private var arrowsEnabled = true
    private var giveUpButton = GiveUpButton.HIDDEN
    private var onSolutionScreen = false

    /** Reset at the start of every modal loop, and defensively whenever [decorateBoard] sees no dialog. */
    private val dialogKeys = DialogKeys()

    override fun prepare(terminal: Terminal) {
        // Keyboard mode never enables mouse reporting.
    }

    override fun onBoardEvent(event: TerminalEvent, layout: BoardLayout): InputAction {
        // The toolbar keys stay live regardless of arrowsEnabled - the toolbar never goes dead.
        digitAction(event)?.let { return it }
        return when {
            event == TerminalEvent.Escape -> InputAction.Activate(HitTarget.ToolbarExit, shortcut = true)
            event == TerminalEvent.EndOfInput -> InputAction.Quit
            onSolutionScreen -> BoardKeys.solutionAction(event)
            !arrowsEnabled -> InputAction.None // solved: nothing to select or shift
            else -> when (val key = BoardKeys.boardKey(event)) {
                is BoardKey.Select -> {
                    selection.current = selection.current.move(key.direction, squareSide)
                    InputAction.Redraw
                }
                is BoardKey.Shift -> InputAction.Shift(selection.current.shiftTarget(key.direction), key.key)
                null -> InputAction.None
            }
        }
    }

    override fun onDialogEvent(event: TerminalEvent, dialog: Dialog, layout: DialogLayout): InputAction =
        dialogKeys.onEvent(event, dialog)

    override fun decorateBoard(state: ScreenState): ScreenState {
        squareSide = state.squareSide
        arrowsEnabled = state.arrowsEnabled
        giveUpButton = state.giveUpButton
        onSolutionScreen = state.solution != null
        // Kept across new games, loads and solved spells (GH-170), only pulled inside a smaller
        // board - every repaint sees the current side, whichever mode changed it.
        selection.current = selection.current.clampedTo(squareSide)
        if (state.dialog == null) dialogKeys.reset()
        return state.copy(
            selection = if (arrowsEnabled) selection.current else null,
            showArrows = false,
            selectionHint = if (onSolutionScreen) BoardKeys.SOLUTION_HINT else BoardKeys.SELECTION_HINT,
            shiftHint = if (onSolutionScreen) emptyList() else BoardKeys.SHIFT_HINT,
            controlsHint = BoardKeys.CONTROLS_HINT,
            toolbarShortcuts = true,
        )
    }

    override fun onDialogOpened() = dialogKeys.onOpened()

    override fun decorateDialog(dialog: Dialog): Dialog = dialogKeys.decorate(dialog)

    /** The toolbar's digits (GH-170); `null` for any other event. 5 is inert while the Give Up
     *  row's button is dimmed or hidden. */
    private fun digitAction(event: TerminalEvent): InputAction? {
        val target = when ((event as? TerminalEvent.KeyPress)?.char) {
            '1' -> HitTarget.ToolbarNew
            '2' -> HitTarget.ToolbarSave
            '3' -> HitTarget.ToolbarLoad
            '4' -> HitTarget.ToolbarLabels
            '5' -> when (giveUpButton) {
                GiveUpButton.GIVE_UP -> HitTarget.GiveUp
                GiveUpButton.MORDIES_SOLUTION -> HitTarget.MordiesSolution
                GiveUpButton.DIMMED, GiveUpButton.HIDDEN -> return InputAction.None
            }
            '6' -> HitTarget.ToolbarControls
            else -> return null
        }
        return InputAction.Activate(target, shortcut = true)
    }
}
