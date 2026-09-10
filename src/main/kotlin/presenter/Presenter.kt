package presenter

interface Presenter {

    fun play()

    fun shiftLeft(row: Int)
    fun shiftRight(row: Int)

    fun shiftUp(col: Int)
    fun shiftDown(col: Int)

    fun resetGame()

    /** Returns whether the save actually succeeded - see [presenter.PresenterImpl.exitGame],
     *  which needs the real outcome rather than assuming success. */
    fun saveGame(name: String): Boolean
    fun loadGame(name: String)

    /** Ends the current session (the `exit`/`quit` command) after an optional save-first
     *  dialogue. Usually ends [play] outright; returns normally instead, leaving the session
     *  running, if the user asked to save but the save attempt failed - see
     *  [presenter.PresenterImpl.exitGame]. */
    fun exitGame()
}