package view.tui.dialog

import view.tui.Dialog
import view.tui.DialogButtonSpec
import view.tui.HitTarget
import view.tui.InputAction
import view.tui.TuiScreen
import viewmodel.TileLabels
import viewmodel.ViewModel

/** The Tile labels dialog (GH-139): the second row's `[ Labels ]` / 4. One row per scheme with
 *  its sample of the solved board's first row, the current scheme marked; OK (or Enter on a row
 *  in keyboard mode) hands the marked one to the viewModel. */
internal class LabelsDialogController(
    private val screen: TuiScreen,
    private val viewModel: ViewModel,
) {

    /** [openedFrom] is `toolbar` (a click) or `shortcut` (a key). */
    fun onToolbar(openedFrom: String) {
        viewModel.tileLabelsDialogOpened(openedFrom)
        runLabelsDialog()?.let { viewModel.changeTileLabels(it) }
    }

    /** A loaded save's labels don't fit its board (GH-160): the dialog with [message] above the rows. */
    fun chooseFitting(message: String): TileLabels? = runLabelsDialog(message)

    /** The marked scheme on OK, `null` on Cancel / Escape / end of input. */
    private fun runLabelsDialog(message: String? = null): TileLabels? {
        val schemes = TileLabels.entries
        val side = viewModel.squareSide()
        val rows = schemes.map { labels ->
            val sample = if (labels.fits(side)) labels.sampleRow(side).joinToString(" ") { it.padEnd(2) } else labels.limitText
            "${labels.title.padEnd(16)} $sample".trimEnd()
        }
        val disabled = schemes.indices.filterNot { schemes[it].fits(side) }.toSet()
        var selected = schemes.indexOf(viewModel.tileLabels())
        val dialog = {
            Dialog(
                kind = Dialog.Kind.LABELS,
                title = "Tile labels",
                message = message,
                listItems = rows,
                selectedIndex = selected,
                disabledRows = disabled,
                buttons = listOf(DialogButtonSpec("ok", "OK"), DialogButtonSpec("cancel", "Cancel")),
            )
        }
        return screen.runModal<TileLabels?>("labels", cancelled = null, dialog = dialog) { action ->
            if (action == InputAction.Submit) return schemes[selected]
            when (val target = (action as? InputAction.Activate)?.target) {
                is HitTarget.DialogListRow -> selected = target.index
                HitTarget.DialogButton("ok") -> return schemes[selected]
                else -> {}
            }
        }
    }
}
