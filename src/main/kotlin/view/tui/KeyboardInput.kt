package view.tui

import InputMode
import board_model.BoardSize
import viewmodel.GiveUpButton

/**
 * GH-18's keyboard-driven [TuiInput] - the only stateful one, working the session's shared
 * [KeyboardSelection] and owning the dialog's focus index into its [DialogFocus] ring.
 *
 * Board (GH-170, ADR-0003): the keys of [BoardKeys] move the selected tile and shift its row or
 * column. The digits are the toolbar - 1 New, 2 Save, 3 Load, 4 Labels, 5 the Give Up row's
 * button, 6 the switch to mouse mode - and Escape opens Exit; all stay live when solved (the
 * selection and shift keys go inert instead, the selection stops rendering) and on the solution
 * screen, whose own keys are [BoardKeys.solutionAction]. Board and dialog events are mutually
 * exclusive, so Escape can't collide with a dialog's Cancel, nor a digit with the Save name field.
 *
 * Dialog: Tab/Down/Right advance the focus ring, Shift-Tab/Up/Left go back, Enter activates
 * whatever's focused (submit, select a row, or click a button), Escape cancels, typing only ever
 * edits the text field. Space is always text inside a dialog, never an activator. A dialog opens
 * focused on its selected row, if any; in Labels (GH-139) Enter on a row submits it, since
 * reaching OK by Tab would walk the selection past it.
 */
class KeyboardInput(private val selection: KeyboardSelection) : TuiInput {

    private var squareSide = BoardSize.DEFAULT
    private var arrowsEnabled = true
    private var giveUpButton = GiveUpButton.HIDDEN
    private var onSolutionScreen = false

    /** Reset to `0` by [onDialogOpened] at the start of every modal loop, and defensively
     *  whenever [decorateBoard] sees no dialog. */
    private var dialogFocusIndex = 0

    /** Set by [onDialogOpened]: the next [decorateDialog] moves focus to the selected row. */
    private var focusSelectedRow = false

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

    override fun onDialogEvent(event: TerminalEvent, dialog: Dialog, layout: DialogLayout): InputAction {
        val focus = DialogFocus(dialog)
        if (focus.size == 0) {
            return when (event) {
                TerminalEvent.Escape -> InputAction.Cancel
                TerminalEvent.EndOfInput -> InputAction.Quit
                else -> InputAction.None
            }
        }
        val index = dialogFocusIndex.mod(focus.size)
        return when (event) {
            TerminalEvent.Escape -> InputAction.Cancel
            TerminalEvent.EndOfInput -> InputAction.Quit
            TerminalEvent.Tab, TerminalEvent.Arrow(Direction.DOWN), TerminalEvent.Arrow(Direction.RIGHT) ->
                moveFocus(focus, focus.next(index))
            TerminalEvent.BackTab, TerminalEvent.Arrow(Direction.UP), TerminalEvent.Arrow(Direction.LEFT) ->
                moveFocus(focus, focus.previous(index))
            TerminalEvent.Enter ->
                if (dialog.kind == Dialog.Kind.LABELS && focus.target(index) is DialogFocusTarget.ListRow) InputAction.Submit
                else activateFocus(focus, index)
            is TerminalEvent.KeyPress ->
                if (focus.target(index) == DialogFocusTarget.TextField) InputAction.TextChar(event.char) else InputAction.None
            TerminalEvent.Backspace ->
                if (focus.target(index) == DialogFocusTarget.TextField) InputAction.EraseChar else InputAction.None
            else -> InputAction.None
        }
    }

    override fun decorateBoard(state: ScreenState): ScreenState {
        squareSide = state.squareSide
        arrowsEnabled = state.arrowsEnabled
        giveUpButton = state.giveUpButton
        onSolutionScreen = state.solution != null
        // Kept across new games, loads and solved spells (GH-170), only pulled inside a smaller
        // board - every repaint sees the current side, whichever mode changed it.
        selection.current = selection.current.clampedTo(squareSide)
        if (state.dialog == null) dialogFocusIndex = 0
        return state.copy(
            selection = if (arrowsEnabled) selection.current else null,
            showArrows = false,
            selectionHint = if (onSolutionScreen) BoardKeys.SOLUTION_HINT else BoardKeys.SELECTION_HINT,
            shiftHint = if (onSolutionScreen) emptyList() else BoardKeys.SHIFT_HINT,
            controlsHint = BoardKeys.CONTROLS_HINT,
            toolbarShortcuts = true,
            modeButtons = listOf(InputMode.MOUSE),
        )
    }

    override fun onDialogOpened() {
        dialogFocusIndex = 0
        focusSelectedRow = true
    }

    override fun decorateDialog(dialog: Dialog): Dialog {
        val focus = DialogFocus(dialog)
        if (focus.size == 0) return dialog
        if (focusSelectedRow) {
            focusSelectedRow = false
            focus.indexOf(DialogFocusTarget.ListRow(dialog.selectedIndex)).takeIf { it >= 0 }?.let { dialogFocusIndex = it }
        }
        val index = dialogFocusIndex.mod(focus.size)
        dialogFocusIndex = index
        return when (val target = focus.target(index)) {
            DialogFocusTarget.TextField -> dialog.copy(textFieldFocused = true)
            is DialogFocusTarget.Button -> dialog.copy(focusedButtonId = target.hitTarget.id)
            is DialogFocusTarget.ListRow, null -> dialog
        }
    }

    private fun moveFocus(focus: DialogFocus, newIndex: Int): InputAction {
        dialogFocusIndex = newIndex
        // A list row's focus doubles as its selection - the same HitTarget.DialogListRow a click
        // would produce. Anything else just moves the highlight; the dialog loop's own
        // per-iteration repaint redraws it, no InputAction.Redraw handling needed there.
        return when (val target = focus.target(newIndex)) {
            is DialogFocusTarget.ListRow -> InputAction.Activate(HitTarget.DialogListRow(target.index))
            else -> InputAction.Redraw
        }
    }

    private fun activateFocus(focus: DialogFocus, index: Int): InputAction = when (val target = focus.target(index)) {
        DialogFocusTarget.TextField -> InputAction.Submit
        is DialogFocusTarget.ListRow -> InputAction.Activate(HitTarget.DialogListRow(target.index))
        is DialogFocusTarget.Button -> InputAction.Activate(target.hitTarget)
        null -> InputAction.None
    }

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
            '6' -> HitTarget.ToolbarMode(InputMode.MOUSE)
            else -> return null
        }
        return InputAction.Activate(target, shortcut = true)
    }
}
