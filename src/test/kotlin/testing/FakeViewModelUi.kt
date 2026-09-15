package testing

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import viewmodel.UiRequest
import viewmodel.ViewModel

/**
 * Drains a [ViewModel]'s [ViewModel.uiRequests] and answers from scripted lists - the
 * viewModel-test replacement for [FakeView]'s four prompt methods (GH-42 WU2), which moved off
 * [view.View] onto [UiRequest] once [viewmodel.ViewModelImpl] started raising requests instead
 * of calling a `View` directly. Keeps the same recording fields and "ran out of scripted
 * responses" `IllegalStateException` bound as the old `FakeView`, so no test can hang on an
 * under-scripted response list.
 */
class FakeViewModelUi(
    private val confirmRestoreResponses: MutableList<Boolean> = mutableListOf(),
    private val chooseSaveToRestoreResponses: MutableList<String?> = mutableListOf(),
    private val confirmSaveBeforeExitResponses: MutableList<Boolean> = mutableListOf(),
    private val promptSaveNameResponses: MutableList<String?> = mutableListOf(),
) {

    val shownMessages = mutableListOf<String>()

    val confirmRestoreCalls = mutableListOf<String>()

    val chooseSaveToRestoreCalls = mutableListOf<List<String>>()

    var confirmSaveBeforeExitCallCount = 0
        private set

    var promptSaveNameCallCount = 0
        private set

    /** Runs [block] against [viewModel] while concurrently draining its [ViewModel.uiRequests] -
     *  without this, a suspended `ask()` call (e.g. inside `saveGame`/`exitGame`) would hang
     *  forever, since [ViewModel.uiRequests] is a rendezvous channel with no other consumer. Any
     *  exception [block] throws propagates out of [drive] once the handler is cancelled. */
    suspend fun drive(viewModel: ViewModel, block: suspend () -> Unit) = coroutineScope {
        val handler = launch { for (request in viewModel.uiRequests) handle(request) }
        try {
            block()
        } finally {
            handler.cancel()
        }
    }

    private fun handle(request: UiRequest<*>) {
        when (request) {
            is UiRequest.ShowMessage -> {
                shownMessages.add(request.text)
                request.respond(Unit)
            }
            is UiRequest.ConfirmRestore -> {
                confirmRestoreCalls.add(request.saveName)
                request.respond(nextOrThrow(confirmRestoreResponses, "confirmRestore"))
            }
            is UiRequest.ChooseSaveToRestore -> {
                chooseSaveToRestoreCalls.add(request.saveNames)
                request.respond(nextOrThrow(chooseSaveToRestoreResponses, "chooseSaveToRestore"))
            }
            is UiRequest.ConfirmSaveBeforeExit -> {
                confirmSaveBeforeExitCallCount++
                request.respond(nextOrThrow(confirmSaveBeforeExitResponses, "confirmSaveBeforeExit"))
            }
            is UiRequest.PromptSaveName -> {
                promptSaveNameCallCount++
                request.respond(nextOrThrow(promptSaveNameResponses, "promptSaveName"))
            }
        }
    }

    private fun <T> nextOrThrow(responses: MutableList<T>, name: String): T {
        if (responses.isEmpty()) {
            throw IllegalStateException("FakeViewModelUi ran out of scripted $name responses")
        }
        return responses.removeAt(0)
    }
}
