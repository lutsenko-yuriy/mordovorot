package viewmodel

import kotlinx.coroutines.channels.ReceiveChannel

/** The viewModel surface `view.tui.TuiView` depends on (GH-42 WU3) - shift/new game/save/load/
 *  exit plus the read-only query surface (`listSaves`/`saveExists`/`isSolved`/`boardState`/
 *  `squareSide`) the TUI needs to render/re-render. */
interface ViewModel {

    /** The UI interactions this viewModel is waiting on, in emission order (GH-42 WU2). Exactly
     *  one consumer - the View that owns this viewModel's session, draining it in a coroutine
     *  alongside its own event loop. See [viewmodel.ViewModelImpl.ask]. */
    val uiRequests: ReceiveChannel<UiRequest<*>>

    fun shiftLeft(row: Int)
    fun shiftRight(row: Int)

    fun shiftUp(col: Int)
    fun shiftDown(col: Int)

    /** Starts a fresh shuffled game at [size] (GH-44) - throws (matching `shiftLeft`'s
     *  "Incorrect row" contract) and tracks nothing if [size] is out of range, otherwise tracks
     *  `new_game_size_selected {size, trigger}`. [trigger] is `"startup_prompt"`, `"toolbar"`
     *  (`[ New ]` -> size-picker dialog), or `"unsolvable_save"` - see docs/ANALYTICS_EVENTS.md. */
    fun newGame(size: Int, trigger: String)

    /** Returns whether the save actually succeeded - see [viewmodel.ViewModelImpl.exitGame],
     *  which needs the real outcome rather than assuming success. `suspend` (GH-42): asks the
     *  View for confirmation/messages instead of returning synchronously. */
    suspend fun saveGame(name: String): Boolean
    /** Throws [ExitRequestedException] when Exit is chosen for an unsolvable save (GH-74) and
     *  the exit flow ends the session. */
    suspend fun loadGame(name: String)

    /** Ends the current session (the `exit`/`quit` command) after an optional save-first
     *  dialogue. Usually ends the owning View's play loop outright; returns normally instead,
     *  leaving the session running, if the user asked to save but the save attempt failed -
     *  see [viewmodel.ViewModelImpl.exitGame]. */
    suspend fun exitGame()

    /** Saves with their board size (GH-44 WU4), sorted by name; empty when nothing has been
     *  saved yet, or on a storage error - lets the mouse-driven Load dialog (GH-3) show what's
     *  available without exposing storage errors to the UI layer. A save whose size couldn't be
     *  read has a `null` [SaveInfo.squareSide] rather than being dropped from the list. */
    fun listSaves(): List<SaveInfo>

    /** Whether a save named [name] already exists. Backs the mouse-driven Save dialog's
     *  overwrite warning (GH-3); degrades to `false` on a storage error, same non-throwing
     *  discipline as [listSaves]. */
    fun saveExists(name: String): Boolean

    /** Whether the board is currently solved - lets the mouse-driven TUI (GH-3) show the
     *  Congratulations screen and disable the shift arrows. */
    fun isSolved(): Boolean

    /** Offers to restore a previous save at startup (0/1/2+ saves) before play begins. Public so
     *  each View can run it ahead of its own event loop (`view.tui.TuiView`, GH-3). Throws [ExitRequestedException] when Exit is chosen for
     *  an unsolvable save (GH-74) - the caller must end the session. */
    suspend fun restoreOnStartup()

    /** Current board tile values (0-based, GH-10 dialect) - lets a View render/re-render after
     *  every command or click. */
    fun boardState(): IntArray

    /** The board's square side (rows == columns), paired with [boardState] for layout. */
    fun squareSide(): Int

    /** Moves made this game (GH-62) - see [board_model.BoardModel.moveCount]. */
    fun moveCount(): Int
}
