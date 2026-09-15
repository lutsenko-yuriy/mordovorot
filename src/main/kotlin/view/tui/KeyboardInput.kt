package view.tui

import InputMode

/** Bottom-left controls reminder (GH-18/GH-30) - F5/F6/F7/F8/Esc stay live even when the
 *  arrows/cursor don't. Kept at 78 chars (audit finding on PR #40): the F7/F8 addition pushed
 *  the old wording to 88, clipping silently past the default/fallback 80-column terminal width
 *  ([ScreenRenderer.Canvas.put] has no wrap or ellipsis, unlike dialog content). */
private const val CONTROLS_HINT =
    "Arrows: move · Enter: shift · F5/F6 Save/Load · F7/F8 Mouse/Console · Esc Exit"

/**
 * GH-18's keyboard-driven [TuiInput] - the only stateful one, owning the board [ArrowCursor] and
 * the dialog's focus index into its [DialogFocus] ring.
 *
 * Board: arrows move the cursor around the perimeter ring; Enter/Space activates the highlighted
 * arrow; F5/F6 open Save/Load, F7/F8 (GH-30) request a switch to mouse/console mode, and Escape
 * opens Exit - all regardless of cursor position, staying live even when solved (arrows/Enter go
 * inert instead, cursor stops rendering). Escape replaces a fifth function key for Exit - board
 * and dialog events are mutually exclusive, so it can't collide with Escape's dialog-level
 * Cancel.
 *
 * Dialog: Tab/Down/Right advance the focus ring, Shift-Tab/Up/Left go back, Enter activates
 * whatever's focused (submit, select a row, or click a button), Escape cancels, typing only ever
 * edits the text field. Space is always text inside a dialog, never an activator.
 */
class KeyboardInput : TuiInput {

    override val switchTrigger = "shortcut"

    private var cursor = ArrowCursor(Edge.LEFT, 0)
    private var squareSide = DEFAULT_SQUARE_SIDE
    private var arrowsEnabled = true

    /** Reset to `0` by [onDialogOpened] at the start of every modal loop, and defensively
     *  whenever [decorateBoard] sees no dialog. */
    private var dialogFocusIndex = 0

    override fun prepare(terminal: Terminal) {
        // Keyboard mode never enables mouse reporting.
    }

    override fun onBoardEvent(event: TerminalEvent, layout: BoardLayout): InputAction = when {
        event is TerminalEvent.Arrow && arrowsEnabled -> {
            cursor = ArrowRing(squareSide).move(cursor, event.direction)
            InputAction.Redraw
        }
        event is TerminalEvent.Arrow -> InputAction.None // solved: nothing to land on
        isActivateKey(event) ->
            if (arrowsEnabled) InputAction.Activate(ArrowRing(squareSide).toHitTarget(cursor)) else InputAction.None
        // F5/F6/Escape stay live regardless of arrowsEnabled - the toolbar never goes dead.
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
        // Resets the cursor only on the disabled -> enabled transition (e.g. loading an unsolved
        // save from the Congratulations screen), never on an ordinary mid-game repaint.
        if (arrowsEnabled && !wasEnabled) cursor = ArrowCursor(Edge.LEFT, 0)
        if (state.dialog == null) dialogFocusIndex = 0
        return state.copy(
            cursor = if (arrowsEnabled) cursor else null,
            controlsHint = CONTROLS_HINT,
            toolbarShortcuts = true,
            modeButtons = listOf(InputMode.MOUSE, InputMode.CONSOLE),
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

    private fun isActivateKey(event: TerminalEvent): Boolean =
        event == TerminalEvent.Enter || event == TerminalEvent.KeyPress(' ')

    private fun functionKeyAction(n: Int): InputAction = when (n) {
        5 -> InputAction.Activate(HitTarget.ToolbarSave)
        6 -> InputAction.Activate(HitTarget.ToolbarLoad)
        7 -> InputAction.Activate(HitTarget.ToolbarMode(InputMode.MOUSE))
        8 -> InputAction.Activate(HitTarget.ToolbarMode(InputMode.CONSOLE))
        else -> InputAction.None
    }

    private companion object {
        const val DEFAULT_SQUARE_SIDE = 4
    }
}
