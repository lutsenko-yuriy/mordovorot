package view

/**
 * Created by yurich on 08.12.16.
 */
interface View {
    fun displayBoard(boardState: IntArray, squareSide: Int)

    fun showMessage(message: String)

    fun processCommand()

    fun play()
}