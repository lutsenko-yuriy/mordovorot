package view

import presenter.Presenter
import java.io.BufferedReader
import java.io.EOFException
import java.io.InputStreamReader
import java.io.PrintStream

/**
 * Created by yurich on 08.12.16.
 */
class ViewImpl(
    private val input: BufferedReader = BufferedReader(InputStreamReader(System.`in`)),
    private val output: PrintStream = System.out,
) : View {

    lateinit var presenter: Presenter

    override fun displayBoard(boardState: IntArray, squareSide: Int) {
        for (i in 0 until squareSide) {
            for (j in 0 until squareSide) {
                output.print("${boardState[i * squareSide + j]}\t")
            }
            output.println()
        }
        output.println()
    }

    override fun processCommand() {
        // BufferedReader.readLine() returns null cleanly at EOF (unlike Scanner, which
        // throws NoSuchElementException on exhausted input). Translating that into an
        // explicit EOFException lets PresenterImpl.play() tell "no more input" apart
        // from a recoverable per-command error - see GH-4's 651ca64 for the bug this
        // used to cause when the two were conflated.
        val line = input.readLine() ?: throw EOFException()
        val parts = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val command = parts.getOrNull(0)?.lowercase() ?: throw IllegalArgumentException("Incorrect input")

        when (command) {
            "left" -> presenter.shiftLeft(intArg(parts))
            "right" -> presenter.shiftRight(intArg(parts))
            "up" -> presenter.shiftUp(intArg(parts))
            "down" -> presenter.shiftDown(intArg(parts))

            "reset" -> presenter.resetGame()

            else -> throw IllegalArgumentException("Incorrect input")
        }

        output.println()
    }

    private fun intArg(parts: List<String>): Int =
        parts.getOrNull(1)?.toIntOrNull() ?: throw IllegalArgumentException("Incorrect input")

    override fun play() {
        presenter.play()
    }
}
