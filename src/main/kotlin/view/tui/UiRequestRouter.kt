package view.tui

import view.tui.dialog.ExitDialogController
import view.tui.dialog.RestoreDialogController
import view.tui.dialog.SaveDialogController
import view.tui.dialog.SizeDialogController
import view.tui.dialog.UnsolvableDialogController
import viewmodel.UiRequest

/** Answers each [UiRequest] the viewModel raises with the dialog that owns it (GH-78). The
 *  `when` is exhaustive, so a new request type doesn't compile until it has an answer. */
internal class UiRequestRouter(
    private val screen: TuiScreen,
    private val saveDialog: SaveDialogController,
    private val exitDialog: ExitDialogController,
    private val restoreDialog: RestoreDialogController,
    private val sizeDialog: SizeDialogController,
    private val unsolvableDialog: UnsolvableDialogController,
) {

    suspend fun handle(request: UiRequest<*>) {
        when (request) {
            is UiRequest.ShowMessage -> {
                screen.showMessage(request.text)
                request.respond(Unit)
            }
            is UiRequest.ConfirmRestore -> request.respond(restoreDialog.confirmRestore(request.save))
            is UiRequest.ChooseSaveToRestore -> request.respond(restoreDialog.chooseSaveToRestore(request.saves))
            is UiRequest.ConfirmSaveBeforeExit -> request.respond(exitDialog.confirmSaveBeforeExit())
            is UiRequest.PromptSaveName -> request.respond(saveDialog.promptSaveName())
            is UiRequest.ChooseBoardSize -> request.respond(sizeDialog.runSizeDialog(request.origin))
            is UiRequest.ResolveUnsolvableSave ->
                request.respond(unsolvableDialog.runUnsolvableDialog(request.save, if (request.atStartup) "startup" else "toolbar"))
        }
    }
}
