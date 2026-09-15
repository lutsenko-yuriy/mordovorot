package view.tui

/** What focusing item `index` of a [DialogFocus] ring means for [ScreenRenderer] (which control
 *  to highlight) and [TuiInput] (what Enter should do): the dialog's text field, a selectable
 *  list row, or a button ready to activate. */
sealed class DialogFocusTarget {
    object TextField : DialogFocusTarget()
    data class ListRow(val index: Int) : DialogFocusTarget()
    data class Button(val hitTarget: HitTarget.DialogButton) : DialogFocusTarget()
}

/**
 * Pure focus ring for GH-18's keyboard dialog navigation: the focusable controls of a [Dialog],
 * in order - its text field (Save only), then its list rows (Load only), then its buttons -
 * with [next]/[previous] wrapping a 0-based focus index around the ring and [target] mapping
 * that index back to what it means ([DialogFocusTarget]). No terminal or viewModel involved.
 */
class DialogFocus(dialog: Dialog) {

    private val targets: List<DialogFocusTarget> = buildList {
        if (dialog.kind == Dialog.Kind.SAVE) add(DialogFocusTarget.TextField)
        if (dialog.kind == Dialog.Kind.LOAD) dialog.listItems.indices.forEach { add(DialogFocusTarget.ListRow(it)) }
        dialog.buttons.forEach { add(DialogFocusTarget.Button(HitTarget.DialogButton(it.id))) }
    }

    val size: Int get() = targets.size

    fun next(current: Int): Int = if (targets.isEmpty()) 0 else (current + 1).mod(targets.size)

    fun previous(current: Int): Int = if (targets.isEmpty()) 0 else (current - 1).mod(targets.size)

    fun target(index: Int): DialogFocusTarget? = targets.getOrNull(index)
}
