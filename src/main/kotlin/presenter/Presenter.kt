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

    fun exitGame()
}