package presenter

import kotlinx.coroutines.channels.ReceiveChannel

/** The shared domain-mutation surface both [ConsolePresenter] and [TuiPresenter] use
 *  identically - shift/reset/save/load/exit. UI-specific concerns (the console's [ConsolePresenter.play]
 *  loop, the TUI's read-only query surface) live on the sub-interfaces, not here (GH-23). */
interface Presenter {

    /** The UI interactions this presenter is waiting on, in emission order (GH-42 WU2). Exactly
     *  one consumer - the View that owns this presenter's session, draining it in a coroutine
     *  alongside its own event loop. See [presenter.BasePresenter.ask]. */
    val uiRequests: ReceiveChannel<UiRequest<*>>

    fun shiftLeft(row: Int)
    fun shiftRight(row: Int)

    fun shiftUp(col: Int)
    fun shiftDown(col: Int)

    fun resetGame()

    /** Returns whether the save actually succeeded - see [presenter.BasePresenter.exitGame],
     *  which needs the real outcome rather than assuming success. `suspend` (GH-42): asks the
     *  View for confirmation/messages instead of returning synchronously. */
    suspend fun saveGame(name: String): Boolean
    suspend fun loadGame(name: String)

    /** Ends the current session (the `exit`/`quit` command) after an optional save-first
     *  dialogue. Usually ends [ConsolePresenter.play] outright; returns normally instead,
     *  leaving the session running, if the user asked to save but the save attempt failed -
     *  see [presenter.BasePresenter.exitGame]. */
    suspend fun exitGame()
}
