package web

import viewmodel.GameViewModel
import viewmodel.TileLabels

/** The Tile labels dialog's flows (GH-139, GH-160), like the terminal's `LabelsDialogController`.
 *  [ask] is [WebGame]'s modal: it shows the dialog and waits for OK's scheme, or `null`;
 *  [cancelled] tracks a Cancel. */
internal class WebLabelsDialog(
    private val viewModel: GameViewModel,
    private val cancelled: (dialog: String) -> Unit,
    private val ask: suspend (WebDialog.Labels) -> TileLabels?,
) {

    /** `[ Labels ]`, or 4 with [openedFrom] `shortcut`: OK hands the marked scheme to the viewModel. */
    suspend fun fromToolbar(openedFrom: String) {
        viewModel.tileLabelsDialogOpened(openedFrom)
        choose()?.let(viewModel::changeTileLabels)
    }

    /** A loaded save's labels don't fit its board (GH-160): the picker with [message] above it. */
    suspend fun chooseFitting(message: String): TileLabels? = choose(message)

    /** [open] with [labels] marked, or `null` when [labels] doesn't fit the board. */
    fun select(open: WebDialog.Labels, labels: TileLabels): WebDialog.Labels? =
        if (labels.fits(viewModel.squareSide())) dialog(labels, open.message) else null

    private suspend fun choose(message: String? = null): TileLabels? {
        val chosen = ask(dialog(viewModel.tileLabels(), message))
        if (chosen == null) cancelled("labels")
        return chosen
    }

    private fun dialog(selected: TileLabels, message: String?): WebDialog.Labels {
        val side = viewModel.squareSide()
        return WebDialog.Labels(
            TileLabels.entries.map {
                if (it.fits(side)) LabelOption(it, it.title, it.sampleRow(side))
                else LabelOption(it, it.title, listOf(it.limitText), available = false)
            },
            selected,
            message,
        )
    }
}
