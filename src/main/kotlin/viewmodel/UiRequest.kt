package viewmodel

import kotlinx.coroutines.CompletableDeferred

/** One outstanding viewModel->UI interaction plus the slot its answer lands in (GH-42 WU2).
 *  [R] makes [respond] type-checked per branch in the View's `when` over a received request.
 *  Travels on [ViewModel.uiRequests], a rendezvous channel - see [viewmodel.ViewModelImpl.ask]
 *  for why that ordering is what keeps message/prompt sequencing identical to the old direct
 *  blocking calls into [view.View]. */
sealed class UiRequest<R> {
    private val response = CompletableDeferred<R>()

    /** Called by the View once it has an answer. A second call is ignored - [CompletableDeferred]
     *  already has that contract, no extra guard needed here. */
    fun respond(value: R) {
        response.complete(value)
    }

    internal suspend fun awaitResponse(): R = response.await()

    class ShowMessage(val text: String) : UiRequest<Unit>()
    /** [save] carries its size (GH-44 WU4) so the View can show it alongside the name. */
    class ConfirmRestore(val save: SaveInfo) : UiRequest<Boolean>()
    class ChooseSaveToRestore(val saves: List<SaveInfo>) : UiRequest<String?>()
    class ConfirmSaveBeforeExit : UiRequest<Boolean>()
    class PromptSaveName : UiRequest<String?>()

    /** "Which size for the fresh game?" (GH-44) - raised by [ViewModelImpl.restoreOnStartup]
     *  once it's established restore didn't happen. Answered with a chosen side, or `null` to
     *  keep [current]. */
    class ChooseBoardSize(val current: Int) : UiRequest<Int?>()
}
