package web

import viewmodel.UiRequest

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
            is UiRequest.ConfirmRestore -> request.respond(game.chooseRestore(listOf(request.save)) != null)
            is UiRequest.ChooseSaveToRestore -> request.respond(game.chooseRestore(request.saves))
            is UiRequest.ResolveUnsolvableSave ->
                request.respond(game.resolveUnsolvable(request.save, if (request.atStartup) "startup" else "toolbar"))
            // Only the terminal's Exit raises these.
            is UiRequest.PromptSaveName -> request.respond(null)
            is UiRequest.ConfirmSaveBeforeExit -> request.respond(false)
        }
    }
}
