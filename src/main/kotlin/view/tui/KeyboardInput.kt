package view.tui

/** The bottom-left reminder of keyboard mode's controls (GH-18), fixed regardless of board
 *  state - F5/F6/Esc stay live even when the arrows/cursor don't (see [KeyboardInput.decorateBoard]). */
private const val CONTROLS_HINT = "Arrows: move · Enter/Space: shift · F5 Save · F6 Load · Esc Exit"

/**
 * GH-18's keyboard-driven [TuiInput]: owns the board [ArrowCursor] (moved via [ArrowRing]) and
 * the current dialog's focus index into its [DialogFocus] ring - the only stateful [TuiInput].
 * Board: arrow keys move the cursor around the perimeter ring, Enter or Space activates
 * whichever arrow is highlighted, F5/F6 open the Save/Load dialogs and Escape opens the Exit
 * dialog, all regardless of cursor position and staying live even when the board is solved
 * (arrow keys/Enter go inert instead, and the cursor stops rendering - see [decorateBoard]).
 * Escape was chosen over a fourth function key (F7) for Exit specifically - it's the
 * conventional back/quit key, and reserving it board-level only never collides with its dialog
 * meaning (Cancel), since the two are mutually exclusive input contexts. Dialog: Tab/Down/Right
 * advance the focus ring, Shift-Tab/Up/Left go back, Enter activates the focused control
 * (submitting the text field, selecting a list row, or clicking a button - see
 * [activateFocus]), Escape cancels, and typing only ever edits the text field, never navigates
 * (a Load dialog's focused list row still can't be typed into). Space is always a text
 * character inside a dialog, never an activator, matching the plan's explicit call-out.
 */
class KeyboardInput : TuiInput {

    private var cursor = ArrowCursor(Edge.LEFT, 0)
    private var squareSide = DEFAULT_SQUARE_SIDE
    private var arrowsEnabled = true

    /** The open dialog's focus index into its [DialogFocus] ring - reset to `0` by
     *  [onDialogOpened] at the start of every modal loop (see its KDoc for why that precise
     *  boundary matters over inferring one from [decorateBoard]'s dialog presence) and again
     *  defensively whenever [decorateBoard] sees no dialog at all, so a freshly-opened dialog
     *  always starts focus at its first control. */
    private var dialogFocusIndex = 0

    override fun prepare(terminal: Terminal) {
        // Keyboard mode never turns mouse reporting on - see MouseInput.prepare for the mouse
        // equivalent this replaces.
    }

    override fun onBoardEvent(event: TerminalEvent, layout: BoardLayout): InputAction = when {
        event is TerminalEvent.Arrow && arrowsEnabled -> {
            cursor = ArrowRing(squareSide).move(cursor, event.direction)
            InputAction.Redraw
        }
        // Solved: the ring has nothing to land on (ScreenState's own KDoc) - arrow keys are a
        // no-op rather than silently moving a cursor nobody can see.
        event is TerminalEvent.Arrow -> InputAction.None
        isActivateKey(event) ->
            if (arrowsEnabled) InputAction.Activate(ArrowRing(squareSide).toHitTarget(cursor)) else InputAction.None
        // F5/F6/Escape are board-level only (a dialog's own onDialogEvent never sees this
        // branch) and stay live regardless of arrowsEnabled - the toolbar never goes dead on
        // solve. Escape opening Exit here can't collide with Escape's dialog-level Cancel
        // meaning (onDialogEvent, below) - the board and a dialog are never both reading events
        // at once.
        event is TerminalEvent.FunctionKey -> functionKeyAction(event.n)
        event == TerminalEvent.Escape -> InputAction.Activate(HitTarget.ToolbarExit)
        event == TerminalEvent.EndOfInput -> InputAction.Quit
        else -> InputAction.None
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
            TerminalEvent.Enter -> activateFocus(focus, index)
            is TerminalEvent.KeyPress ->
                if (focus.target(index) == DialogFocusTarget.TextField) InputAction.TextChar(event.char) else InputAction.None
            TerminalEvent.Backspace ->
                if (focus.target(index) == DialogFocusTarget.TextField) InputAction.EraseChar else InputAction.None
            else -> InputAction.None
        }
    }

    override fun decorateBoard(state: ScreenState): ScreenState {
        squareSide = state.squareSide
        val wasEnabled = arrowsEnabled
        arrowsEnabled = state.arrowsEnabled
        // Loading an unsolved save from the Congratulations screen restores the cursor at
        // LEFT[0] (plan's solved-state note) - only on the disabled -> enabled transition, not
        // on every unsolved repaint, so an ordinary shift mid-game never resets it underfoot.
        if (arrowsEnabled && !wasEnabled) cursor = ArrowCursor(Edge.LEFT, 0)
        // Defensive fallback for [onDialogOpened]'s reset - no dialog this repaint means
        // whatever session dialogFocusIndex belonged to has definitely ended.
        if (state.dialog == null) dialogFocusIndex = 0
        return state.copy(
            cursor = if (arrowsEnabled) cursor else null,
            controlsHint = CONTROLS_HINT,
            toolbarShortcuts = true,
        )
    }

    override fun onDialogOpened() {
        dialogFocusIndex = 0
    }

    override fun decorateDialog(dialog: Dialog): Dialog {
        val focus = DialogFocus(dialog)
        if (focus.size == 0) return dialog
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
        // A list row's focus doubles as its selection (the plan's "Down selects a row") - the
        // same HitTarget.DialogListRow a mouse click on that row would produce, so TuiView's
        // existing `selected = target.index` handling picks it up for free. Anything else just
        // moves the highlight; the loop's own per-iteration repaint (see TuiView.repaintWithDialog)
        // is what actually redraws it - no InputAction.Redraw handling needed in the dialog
        // loops themselves (audit finding on GH-18 WU3 PR #32).
        return when (val target = focus.target(newIndex)) {
            is DialogFocusTarget.ListRow -> InputAction.Activate(HitTarget.DialogListRow(target.index))
            else -> InputAction.Redraw
        }
    }

    private fun activateFocus(focus: DialogFocus, index: Int): InputAction = when (val target = focus.target(index)) {
        // Enter on the text field submits the typed name, same as Enter always did in mouse
        // mode - the field is only ever focusable in a Save dialog, where InputAction.Submit is
        // exactly what "confirm this name" means.
        DialogFocusTarget.TextField -> InputAction.Submit
        is DialogFocusTarget.ListRow -> InputAction.Activate(HitTarget.DialogListRow(target.index))
        is DialogFocusTarget.Button -> InputAction.Activate(target.hitTarget)
        null -> InputAction.None
    }

    private fun isActivateKey(event: TerminalEvent): Boolean =
        event == TerminalEvent.Enter || event == TerminalEvent.KeyPress(' ')

    private fun functionKeyAction(n: Int): InputAction = when (n) {
        5 -> InputAction.Activate(HitTarget.ToolbarSave)
        6 -> InputAction.Activate(HitTarget.ToolbarLoad)
        // F7 is decoded by TerminalInputParser but no longer means anything here - Escape is
        // Exit's trigger now (see the class KDoc).
        else -> InputAction.None
    }

    private companion object {
        const val DEFAULT_SQUARE_SIDE = 4
    }
}
