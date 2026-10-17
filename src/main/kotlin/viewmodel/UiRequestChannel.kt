package viewmodel

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

/** The viewmodel side of [GameViewModel.uiRequests] (GH-78): one per rebuild, shared by
 *  [GameViewModelImpl], every use case and any platform flow (the terminal's Exit, GH-41) so
 *  their requests reach the one View draining it. */
class UiRequestChannel {

    /** Rendezvous - `ask` doesn't return until the View has actually finished handling the
     *  request, which is what keeps message/prompt ordering identical to the old direct blocking
     *  calls into `View` (GH-42 WU2, see docs/ARCHITECTURE.md's viewModel section). */
    private val channel = Channel<UiRequest<*>>(Channel.RENDEZVOUS)

    val requests: ReceiveChannel<UiRequest<*>> = channel

    /** Raises [request] on [requests] and suspends until the View responds. The View must
     *  never call back into a request-raising viewModel method (`saveGame`/`loadGame`/
     *  `restoreOnStartup`, or the terminal's `exitGame`) from inside its own request handler - doing so deadlocks, since the
     *  handler is `ask`'s only consumer and would be busy with the request that triggered the
     *  callback. */
    suspend fun <R> ask(request: UiRequest<R>): R {
        channel.send(request)
        return request.awaitResponse()
    }

    /** The one place [UiRequest.ShowMessage] is raised - every message goes through this, not a
     *  bare `ask` call, so a future new message site can't forget it. */
    suspend fun showMessage(text: String) {
        ask(UiRequest.ShowMessage(text))
    }
}
