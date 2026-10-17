package view.tui.dialog

import view.tui.dialog.LoadDialogController.LoadOutcome
import viewmodel.SaveInfo

/** The startup restore prompt (GH-78): the Load-shaped dialog over the saves the viewModel
 *  already listed. */
internal class RestoreDialogController(private val load: LoadDialogController) {

    /** The startup restore prompt for exactly one save - the same Load-shaped modal as any
     *  other save count. */
    suspend fun confirmRestore(save: SaveInfo): Boolean =
        load.runLoadDialog("Restore a saved game?", "startup", preloadedSaves = listOf(save)) is LoadOutcome.Confirm

    suspend fun chooseSaveToRestore(saves: List<SaveInfo>): String? =
        (load.runLoadDialog("Restore a saved game?", "startup", preloadedSaves = saves) as? LoadOutcome.Confirm)?.name
}
