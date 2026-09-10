package view

/**
 * Created by yurich on 08.12.16.
 */
interface View {
    fun displayBoard(boardState: IntArray, squareSide: Int)

    fun showMessage(message: String)

    fun processCommand()

    /** Prompts the user to restore the single save [saveName] found at startup.
     *  `false` on a "no" answer or EOF. */
    fun confirmRestore(saveName: String): Boolean

    /** Prompts the user to pick one of [saveNames] to restore at startup, or start a new
     *  game. Returns the raw typed name (which may not be one of [saveNames] - the caller
     *  re-prompts on an unknown name), or `null` on a blank answer or EOF. */
    fun chooseSaveToRestore(saveNames: List<String>): String?

    fun play()
}