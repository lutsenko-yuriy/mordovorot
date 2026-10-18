package web

import viewmodel.UiRequest
import viewmodel.UnsolvableSaveChoice

/** Answers each [UiRequest] the viewModel raises (GH-110). The `when` is exhaustive, like the
 *  terminal's `UiRequestRouter`. */
internal class WebRequestRouter(private val game: WebGame) {

    suspend fun handle(request: UiRequest<*>) {
        when (request) {
            is UiRequest.ShowMessage -> {
                game.showStatus(request.text)
                request.respond(Unit)
            }
            is UiRequest.ChooseBoardSize -> request.respond(game.chooseSize(request.origin))
            // The page keeps no saves until WU5, which gives these their dialogs.
            is UiRequest.ConfirmRestore -> request.respond(false)
            is UiRequest.ChooseSaveToRestore -> request.respond(null)
            is UiRequest.PromptSaveName -> request.respond(null)
            is UiRequest.ResolveUnsolvableSave -> request.respond(UnsolvableSaveChoice.NEW_GAME)
            // Only the terminal's Exit raises this.
            is UiRequest.ConfirmSaveBeforeExit -> request.respond(false)
        }
    }
}
