package view.tui

/**
 * What a mouse click resolves to. Row/column indices here are 0-based model indices - the
 * `DISPLAY_OFFSET` 1-based dialect ([view.ViewImpl]'s GH-10 note) applies only to typed input
 * and rendered tile values, never to a click coordinate.
 */
sealed class HitTarget {
    data class ShiftLeft(val row: Int) : HitTarget()
    data class ShiftRight(val row: Int) : HitTarget()
    data class ShiftUp(val col: Int) : HitTarget()
    data class ShiftDown(val col: Int) : HitTarget()

    object ToolbarSave : HitTarget()
    object ToolbarLoad : HitTarget()
    object ToolbarExit : HitTarget()

    /** [id] identifies which dialog button was clicked (e.g. `"save"`, `"cancel"`, `"yes"`) -
     *  used by WU4's Save/Load/Exit dialogs. */
    data class DialogButton(val id: String) : HitTarget()

    /** A click on row [index] of a dialog's selectable list (WU4's Load dialog). */
    data class DialogListRow(val index: Int) : HitTarget()

    object Nothing : HitTarget()
}
