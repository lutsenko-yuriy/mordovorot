package presenter

import kotlinx.coroutines.CompletableDeferred

/** One outstanding presenter->UI interaction plus the slot its answer lands in (GH-42 WU2).
 *  [R] makes [respond] type-checked per branch in the View's `when` over a received request.
 *  Travels on [Presenter.uiRequests], a rendezvous channel - see [presenter.BasePresenter.ask]
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
    class ConfirmRestore(val saveName: String) : UiRequest<Boolean>()
    class ChooseSaveToRestore(val saveNames: List<String>) : UiRequest<String?>()
    class ConfirmSaveBeforeExit : UiRequest<Boolean>()
    class PromptSaveName : UiRequest<String?>()
}
