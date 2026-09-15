package viewmodel

import kotlinx.coroutines.channels.ReceiveChannel

/** The one viewModel surface both `view.ViewImpl` and `view.tui.TuiView` depend on (GH-42 WU3) -
 *  shift/reset/save/load/exit plus the read-only query surface (`listSaves`/`saveExists`/
 *  `isSolved`/`boardState`/`squareSide`) the TUI needs to render/re-render outside any console
 *  loop. Superseded GH-23's `ConsoleViewModel`/`TuiViewModel` split: once `ConsoleViewModelImpl.play()`'s
 *  console loop moved into `ViewImpl` (hard problem 1 on the plan comment on GH-42), both UIs
 *  needed the identical surface, so the split no longer described anything real. */
interface ViewModel {

    /** The UI interactions this viewModel is waiting on, in emission order (GH-42 WU2). Exactly
     *  one consumer - the View that owns this viewModel's session, draining it in a coroutine
     *  alongside its own event loop. See [viewmodel.ViewModelImpl.ask]. */
    val uiRequests: ReceiveChannel<UiRequest<*>>

    fun shiftLeft(row: Int)
    fun shiftRight(row: Int)

    fun shiftUp(col: Int)
    fun shiftDown(col: Int)

    fun resetGame()

    /** Returns whether the save actually succeeded - see [viewmodel.ViewModelImpl.exitGame],
     *  which needs the real outcome rather than assuming success. `suspend` (GH-42): asks the
     *  View for confirmation/messages instead of returning synchronously. */
    suspend fun saveGame(name: String): Boolean
    suspend fun loadGame(name: String)

    /** Ends the current session (the `exit`/`quit` command) after an optional save-first
     *  dialogue. Usually ends the owning View's play loop outright; returns normally instead,
     *  leaving the session running, if the user asked to save but the save attempt failed -
     *  see [viewmodel.ViewModelImpl.exitGame]. */
    suspend fun exitGame()

    /** Save names, sorted; empty when nothing has been saved yet, or on a storage error -
     *  lets the mouse-driven Load dialog (GH-3) show what's available without exposing
     *  storage errors to the UI layer. */
    fun listSaves(): List<String>

    /** Whether a save named [name] already exists. Backs the mouse-driven Save dialog's
     *  overwrite warning (GH-3); degrades to `false` on a storage error, same non-throwing
     *  discipline as [listSaves]. */
    fun saveExists(name: String): Boolean

    /** Whether the board is currently solved - lets the mouse-driven TUI (GH-3) show the
     *  Congratulations screen and disable the shift arrows. */
    fun isSolved(): Boolean

    /** Offers to restore a previous save at startup (0/1/2+ saves) before play begins. Public so
     *  each View can run it ahead of its own event loop (`view.tui.TuiView`, GH-3;
     *  `view.ViewImpl.play`, GH-42 WU3). */
    suspend fun restoreOnStartup()

    /** Current board tile values (0-based, GH-10 dialect) - lets a View render/re-render after
     *  every command or click. */
    fun boardState(): IntArray

    /** The board's square side (rows == columns), paired with [boardState] for layout. */
    fun squareSide(): Int
}
