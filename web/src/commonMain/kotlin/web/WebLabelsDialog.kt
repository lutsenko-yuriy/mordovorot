package web

import viewmodel.GameViewModel
import viewmodel.TileLabels

/** The Tile labels dialog's flows (GH-139, GH-160), as [WebControlsDialog] does Controls. */
internal class WebLabelsDialog(
    private val game: WebGame,
    private val viewModel: GameViewModel,
    private val modal: WebModal,
) {

    /** `[ Labels ]` (GH-139): the picker, live on play, after a win and on the solution screen;
     *  [openedFrom] is `toolbar`, or `shortcut` for 4. OK hands the marked scheme to the viewModel. */
    fun open(openedFrom: String = "toolbar", opener: Focus = Focus.LabelsButton) = game.runFlow(opener) {
        viewModel.tileLabelsDialogOpened(openedFrom)
        choose()?.let(viewModel::changeTileLabels)
    }

    /** A Labels row pressed: it becomes the marked one, and the board previews it. */
    fun onSelected(labels: TileLabels) {
        val shown = modal.shown as? WebDialog.Labels ?: return
        modal.update(select(shown, labels) ?: return)
        game.render(focus = Focus.Label(labels))
    }

    /** The dialog's answer: OK with the marked scheme, or Cancel. */
    fun onAnswered(ok: Boolean) {
        val shown = modal.shown as? WebDialog.Labels ?: return
        modal.answer<WebDialog.Labels, TileLabels?>(if (ok) shown.selected else null)
    }

    /** A loaded save's labels don't fit its board (GH-160): the picker with [message] above it. */
    suspend fun chooseFitting(message: String): TileLabels? = choose(message)

    /** [open] with [labels] marked, or `null` when [labels] doesn't fit the board. */
    private fun select(open: WebDialog.Labels, labels: TileLabels): WebDialog.Labels? =
        if (labels.fits(viewModel.squareSide())) dialog(labels, open.message) else null

    private suspend fun choose(message: String? = null): TileLabels? {
        val chosen = modal.ask<TileLabels?>(dialog(viewModel.tileLabels(), message))
        if (chosen == null) game.cancelled("labels")
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
