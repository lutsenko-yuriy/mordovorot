package web

import viewmodel.GameViewModel
import viewmodel.TileLabelGroup
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

    /** A Labels row pressed: it becomes the marked one, and the board previews it. One that doesn't fit the board stays unmarked and
     *  shows its limit in a bubble instead (GH-343); either way focus goes to it. */
    fun onSelected(labels: TileLabels) {
        val shown = modal.shown as? WebDialog.Labels ?: return
        modal.update(if (labels.fits(viewModel.squareSide())) shown.copy(selected = labels, tip = null) else shown.copy(tip = labels))
        game.render(focus = Focus.Label(labels))
    }

    /** A group header pressed (GH-323): the group opens and focus goes to its marked scheme, else its first that fits.
     *  The open group again does nothing; the mark stays where it is, and no event is sent. */
    fun onGroupOpened(group: TileLabelGroup) {
        val shown = modal.shown as? WebDialog.Labels ?: return
        if (group == shown.open) return
        val opened = shown.copy(open = group, tip = null)
        modal.update(opened)
        game.render(focus = defaultFocus(opened))
    }

    /** The dialog's answer: OK with the marked scheme, or Cancel. */
    fun onAnswered(ok: Boolean) {
        val shown = modal.shown as? WebDialog.Labels ?: return
        modal.answer<WebDialog.Labels, TileLabels?>(if (ok) shown.selected else null)
    }

    /** A loaded save's labels don't fit its board (GH-160): the picker with [message] above it. */
    suspend fun chooseFitting(message: String): TileLabels? = choose(message)

    private suspend fun choose(message: String? = null): TileLabels? {
        // OK skips the frame without the dialog: it would show the old labels for an instant, which the label wave (GH-342) reads as a Cancel.
        val chosen = modal.ask<TileLabels?>(dialog(viewModel.tileLabels(), message), leadsOn = { it != null })
        if (chosen == null) game.cancelled("labels")
        return chosen
    }

    private fun dialog(selected: TileLabels, message: String?): WebDialog.Labels {
        val side = viewModel.squareSide()
        return WebDialog.Labels(
            TileLabels.entries.map { LabelOption(it, TileLabelGroup.of(it).rowTitle(it), if (it.fits(side)) null else it.limitText) },
            selected,
            message,
        )
    }
}
