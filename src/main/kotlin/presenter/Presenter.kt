package presenter

/** The shared domain-mutation surface both [ConsolePresenter] and [TuiPresenter] use
 *  identically - shift/reset/save/load/exit. UI-specific concerns (the console's [ConsolePresenter.play]
 *  loop, the TUI's read-only query surface) live on the sub-interfaces, not here (GH-23). */
interface Presenter {

    fun shiftLeft(row: Int)
    fun shiftRight(row: Int)

    fun shiftUp(col: Int)
    fun shiftDown(col: Int)

    fun resetGame()

    /** Returns whether the save actually succeeded - see [presenter.BasePresenter.exitGame],
     *  which needs the real outcome rather than assuming success. */
    fun saveGame(name: String): Boolean
    fun loadGame(name: String)

    /** Ends the current session (the `exit`/`quit` command) after an optional save-first
     *  dialogue. Usually ends [ConsolePresenter.play] outright; returns normally instead,
     *  leaving the session running, if the user asked to save but the save attempt failed -
     *  see [presenter.BasePresenter.exitGame]. */
    fun exitGame()
}
