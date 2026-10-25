package viewmodel

import kotlinx.coroutines.CompletableDeferred

/** One outstanding viewModel->UI interaction plus the slot its answer lands in (GH-42 WU2).
 *  [R] makes [respond] type-checked per branch in the View's `when` over a received request.
 *  Travels on [GameViewModel.uiRequests], a rendezvous channel - see [UiRequestChannel.ask] for
 *  why that ordering is what keeps message/prompt sequencing identical to the old direct
 *  blocking calls into the View. */
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

    /** "Which size for the fresh game?" (GH-44) - raised by [viewmodel.usecase.StartupRestoreUseCase.restoreOnStartup]
     *  once it's established restore didn't happen. Answered with a chosen side, or `null` to
     *  keep [current]. [origin] is `startup` or `unsolvable_save` - what the TUI
     *  reports as `screen_size_dialog.opened_from`. */
    class ChooseBoardSize(val current: Int, val origin: String = "startup") : UiRequest<Int?>()

    /** "Repair, new game or exit?" when a load hits an unsolvable save (GH-74) - raised by
     *  [GameViewModelImpl]'s `loadGame`. [atStartup] is `true` for the startup restore. */
    class ResolveUnsolvableSave(val save: SaveInfo, val atStartup: Boolean) : UiRequest<UnsolvableSaveChoice>()

    /** A loaded save's labels don't fit its board (Letters on 6x6, GH-160): the Tile labels dialog
     *  with [message] above it and the unfit schemes dimmed. The picked scheme, or `null` on Cancel. */
    class ChooseFittingLabels(val message: String) : UiRequest<TileLabels?>()
}

enum class UnsolvableSaveChoice { REPAIR, NEW_GAME, EXIT }
