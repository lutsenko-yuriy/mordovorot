package web

import analytics.AnalyticsService
import viewmodel.GameViewModel
import viewmodel.TileLabels

/** The Tile labels dialog's flows (GH-139, GH-160), like the terminal's `LabelsDialogController`.
 *  [ask] is [WebGame]'s modal: it shows the dialog and waits for OK's scheme, or `null`. */
internal class WebLabelsDialog(
    private val viewModel: GameViewModel,
    private val analytics: AnalyticsService,
    private val ask: suspend (WebDialog.Labels) -> TileLabels?,
) {

    /** `[ Labels ]`: OK hands the marked scheme to the viewModel. */
    suspend fun fromToolbar() {
        viewModel.tileLabelsDialogOpened("toolbar")
        choose()?.let(viewModel::changeTileLabels)
    }

    /** A loaded save's labels don't fit its board (GH-160): the picker with [message] above it. */
    suspend fun chooseFitting(message: String): TileLabels? = choose(message)

    /** [open] with [labels] marked, or `null` when [labels] doesn't fit the board. */
    fun select(open: WebDialog.Labels, labels: TileLabels): WebDialog.Labels? =
        if (labels.fits(viewModel.squareSide())) dialog(labels, open.message) else null

    private suspend fun choose(message: String? = null): TileLabels? {
        val chosen = ask(dialog(viewModel.tileLabels(), message))
        if (chosen == null) analytics.track("dialog_cancelled", mapOf("dialog" to "labels"))
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
