package view.tui

/**
 * Keyboard navigation inside a dialog (GH-18), owning the focus index into its [DialogFocus] ring.
 * [KeyboardInput] uses it for every dialog; [MouseInput] for the Controls dialog (GH-170), which
 * 6 opens from the keyboard.
 *
 * Tab/Down/Right advance the focus ring, Shift-Tab/Up/Left go back, Enter activates whatever's
 * focused (submit, select a row, or click a button), Escape cancels, typing only ever edits the
 * text field. Space is always text inside a dialog, never an activator. A dialog opens focused on
 * its selected row, if any; in Labels (GH-139) Enter on a row submits it, since reaching OK by Tab
 * would walk the selection past it. On an option group (GH-170) the arrows move its mark instead,
 * wrapping, and Enter submits.
 */
internal class DialogKeys {

    /** Reset to `0` by [onOpened] at the start of every modal loop, and by [reset]. */
    private var focusIndex = 0

    /** Set by [onOpened]: the next [decorate] moves focus to the selected row. */
    private var focusSelectedRow = false

    fun onOpened() {
        focusIndex = 0
        focusSelectedRow = true
    }

    fun reset() {
        focusIndex = 0
    }

    fun onEvent(event: TerminalEvent, dialog: Dialog): InputAction {
        val focus = DialogFocus(dialog)
        if (focus.size == 0) {
            return when (event) {
                TerminalEvent.Escape -> InputAction.Cancel
                TerminalEvent.EndOfInput -> InputAction.Quit
                else -> InputAction.None
            }
        }
        val index = focusIndex.mod(focus.size)
        val target = focus.target(index)
        if (target is DialogFocusTarget.Group) groupAction(event, dialog.optionGroups[target.index], target.index)?.let { return it }
        return when (event) {
            TerminalEvent.Escape -> InputAction.Cancel
            TerminalEvent.EndOfInput -> InputAction.Quit
            TerminalEvent.Tab, TerminalEvent.Arrow(Direction.DOWN), TerminalEvent.Arrow(Direction.RIGHT) ->
                moveFocus(focus, focus.next(index))
            TerminalEvent.BackTab, TerminalEvent.Arrow(Direction.UP), TerminalEvent.Arrow(Direction.LEFT) ->
                moveFocus(focus, focus.previous(index))
            TerminalEvent.Enter ->
                if (dialog.kind == Dialog.Kind.LABELS && target is DialogFocusTarget.ListRow) InputAction.Submit
                else activateFocus(target)
            is TerminalEvent.KeyPress ->
                if (target == DialogFocusTarget.TextField) InputAction.TextChar(event.char) else InputAction.None
            TerminalEvent.Backspace ->
                if (target == DialogFocusTarget.TextField) InputAction.EraseChar else InputAction.None
            else -> InputAction.None
        }
    }

    fun decorate(dialog: Dialog): Dialog {
        val focus = DialogFocus(dialog)
        if (focus.size == 0) return dialog
        if (focusSelectedRow) {
            focusSelectedRow = false
            focus.indexOf(DialogFocusTarget.ListRow(dialog.selectedIndex)).takeIf { it >= 0 }?.let { focusIndex = it }
        }
        val index = focusIndex.mod(focus.size)
        focusIndex = index
        return when (val target = focus.target(index)) {
            DialogFocusTarget.TextField -> dialog.copy(textFieldFocused = true)
            is DialogFocusTarget.Button -> dialog.copy(focusedButtonId = target.hitTarget.id)
            is DialogFocusTarget.Group -> dialog.copy(focusedGroup = target.index)
            is DialogFocusTarget.ListRow, null -> dialog
        }
    }

    /** The arrows and Enter on option group [index]; `null` for the keys that act as anywhere else. */
    private fun groupAction(event: TerminalEvent, group: DialogOptionGroup, index: Int): InputAction? {
        val step = when (event) {
            TerminalEvent.Arrow(Direction.LEFT), TerminalEvent.Arrow(Direction.UP) -> -1
            TerminalEvent.Arrow(Direction.RIGHT), TerminalEvent.Arrow(Direction.DOWN) -> 1
            TerminalEvent.Enter -> return InputAction.Submit
            else -> return null
        }
        return InputAction.Activate(HitTarget.DialogOption(index, (group.selected + step).mod(group.options.size)))
    }

    private fun moveFocus(focus: DialogFocus, newIndex: Int): InputAction {
        focusIndex = newIndex
        // A list row's focus doubles as its selection - the same HitTarget.DialogListRow a click
        // would produce. Anything else just moves the highlight; the dialog loop's own
        // per-iteration repaint redraws it, no InputAction.Redraw handling needed there.
        return when (val target = focus.target(newIndex)) {
            is DialogFocusTarget.ListRow -> InputAction.Activate(HitTarget.DialogListRow(target.index))
            else -> InputAction.Redraw
        }
    }

    private fun activateFocus(target: DialogFocusTarget?): InputAction = when (target) {
        DialogFocusTarget.TextField, is DialogFocusTarget.Group -> InputAction.Submit
        is DialogFocusTarget.ListRow -> InputAction.Activate(HitTarget.DialogListRow(target.index))
        is DialogFocusTarget.Button -> InputAction.Activate(target.hitTarget)
        null -> InputAction.None
    }
}
