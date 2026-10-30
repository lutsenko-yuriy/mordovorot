package web

import viewmodel.KeyboardLayout
import viewmodel.TileLabels

/** A control the page focuses once a frame is drawn (GH-163), or keeps focused across one (GH-164);
 *  [key] is its `data-focus`. */
sealed class Focus(val key: String) {
    /** The [arrow] shifting [line], 0-based. */
    data class ArrowButton(val arrow: Arrow, val line: Int) : Focus("arrow-${arrow.name.lowercase()}-$line")
    /** The hidden Read board button (GH-167). */
    data object ReadBoard : Focus("read-board")
    data class SizeChoice(val side: Int) : Focus("size-$side")
    data object SaveName : Focus("save-name")
    data object FirstSave : Focus("first-save")
    /** A dialog's Cancel: Load's, when it has no saves to focus. */
    data object DialogCancel : Focus("dialog-cancel")
    data object Repair : Focus("repair")
    data class Label(val labels: TileLabels) : Focus("label-${labels.name.lowercase()}")
    data object No : Focus("no")
    data object NewButton : Focus("new")
    data object SaveButton : Focus("save")
    data object LoadButton : Focus("load")
    data object LabelsButton : Focus("labels")
    data object ControlsButton : Focus("controls")
    /** The board, one Tab stop in keyboard mode (GH-171). */
    data object Board : Focus("board")
    data class ModeOption(val mode: PlayMode) : Focus("mode-${mode.wireName}")
    data class LayoutOption(val layout: KeyboardLayout) : Focus("layout-${layout.wireName}")
    /** Back on the control a digit was pressed on, whichever it was (GH-171), or what [fallbackFocus] names. */
    data class Restore(val control: String) : Focus(control)
    /** Give Up, or Mordie's solution after a win. */
    data object GiveUpRow : Focus("give-up")
    data object ReplayNext : Focus("replay-next")
    data object ReplayPrev : Focus("replay-prev")
    data object ReplayRestart : Focus("replay-restart")
    /** No control: the start of the page, as on a fresh load. */
    data object PageStart : Focus("page-start")
}

/** The control [dialog] opens focused on. */
fun defaultFocus(dialog: WebDialog): Focus = when (dialog) {
    is WebDialog.Size -> Focus.SizeChoice(dialog.current)
    is WebDialog.Save -> Focus.SaveName
    is WebDialog.Load -> if (dialog.saves.isEmpty()) Focus.DialogCancel else Focus.FirstSave
    is WebDialog.Unsolvable -> Focus.Repair
    is WebDialog.Labels -> Focus.Label(dialog.selected)
    WebDialog.GiveUp -> Focus.No
    is WebDialog.Controls -> Focus.ModeOption(dialog.mode)
}

/** The dialog's title, which also names it for screen readers. */
val WebDialog.title: String get() = when (this) {
    is WebDialog.Size -> "New game size"
    is WebDialog.Save -> "Save game"
    is WebDialog.Load -> if (restore) "Restore a saved game?" else "Load game"
    is WebDialog.Unsolvable -> "Can't be solved"
    is WebDialog.Labels -> "Tile labels"
    WebDialog.GiveUp -> "Give up and see Mordie's solution?"
    is WebDialog.Controls -> "Controls"
}
