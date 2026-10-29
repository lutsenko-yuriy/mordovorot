package view.tui

import InputMode

/**
 * What a mouse click resolves to. Row/column indices here are 0-based model indices - tile
 * labels ([viewmodel.TileLabels], GH-139) apply only to rendered tile values, never to a click
 * coordinate.
 */
sealed class HitTarget {
    data class ShiftLeft(val row: Int) : HitTarget()
    data class ShiftRight(val row: Int) : HitTarget()
    data class ShiftUp(val col: Int) : HitTarget()
    data class ShiftDown(val col: Int) : HitTarget()

    object ToolbarSave : HitTarget()
    object ToolbarLoad : HitTarget()
    object ToolbarExit : HitTarget()

    /** The `[ New ]` toolbar button (GH-44 WU3) - opens the same size-picker dialog as the
     *  startup prompt ([viewmodel.ViewModel.newGame]'s `"toolbar"` trigger). */
    object ToolbarNew : HitTarget()

    /** The `[ Labels ]` button / 4 (GH-139) - opens the Tile labels dialog. */
    object ToolbarLabels : HitTarget()

    /** A toolbar button requesting a switch to [mode] (GH-30) - one per entry in
     *  [ScreenState.modeButtons], never the mode the view is already in. */
    data class ToolbarMode(val mode: InputMode) : HitTarget()

    /** [id] identifies which dialog button was clicked (e.g. `"save"`, `"cancel"`, `"yes"`) -
     *  used by WU4's Save/Load/Exit dialogs. */
    data class DialogButton(val id: String) : HitTarget()

    /** A click on row [index] of a dialog's selectable list (WU4's Load dialog). */
    data class DialogListRow(val index: Int) : HitTarget()

    /** The live `[ Give Up ]` button / 5 (GH-126) - opens the give-up confirmation. */
    object GiveUp : HitTarget()

    /** `[ Mordie's solution ]` after a win / 5 (GH-126) - opens the solution screen. */
    object MordiesSolution : HitTarget()

    /** The solution screen's Prev / Next / Restart (GH-126): one shift back, one forward, or
     *  back to the original arrangement. */
    object ReplayPrevious : HitTarget()
    object ReplayNext : HitTarget()
    object ReplayRestart : HitTarget()

    object Nothing : HitTarget()
}
