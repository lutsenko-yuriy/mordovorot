package view

import presenter.PresenterImpl
import java.util.*

/**
 * Created by yurich on 08.12.16.
 */
class ViewImpl : View {

    val presenter = PresenterImpl(this)
    var scanner = Scanner(System.`in`)

    override fun displayBoard(boardState: IntArray, squareSide: Int) {
        for (i in 0..squareSide - 1) {
            for (j in 0..squareSide - 1) {
                System.out.print("${boardState[i * squareSide + j]}\t")
            }
            System.out.println()
        }
        System.out.println()
    }

    override fun processCommand() {

        when (scanner.next().toLowerCase()) {
            "left" -> presenter.shiftLeft(scanner.nextInt())
            "right" -> presenter.shiftRight(scanner.nextInt())
            "up" -> presenter.shiftUp(scanner.nextInt())
            "down" -> presenter.shiftDown(scanner.nextInt())

            "reset" -> presenter.resetGame()

            else -> throw IllegalArgumentException("Incorrect input")
        }

        System.out.println()

    }

    override fun play() {
        presenter.play()
    }
}