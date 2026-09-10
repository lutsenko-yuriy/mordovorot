package view

import presenter.Presenter
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintStream

/**
 * Created by yurich on 08.12.16.
 */
class ViewImpl internal constructor(
    private val input: BufferedReader = BufferedReader(InputStreamReader(System.`in`)),
    private val output: PrintStream = System.out,
) : View {

    /** Must be assigned before [play] or [processCommand] are called - use [create]. */
    lateinit var presenter: Presenter

    companion object {
        /**
         * Constructs a [ViewImpl] with its [Presenter] wired atomically, avoiding a
         * window where [presenter] is unset. This is the only public way to obtain
         * a working [ViewImpl] - the constructor is internal, so `presenter`'s
         * invariant can't be violated from outside the module.
         */
        fun create(
            input: BufferedReader = BufferedReader(InputStreamReader(System.`in`)),
            output: PrintStream = System.out,
            presenterFactory: (View) -> Presenter,
        ): ViewImpl {
            val view = ViewImpl(input, output)
            view.presenter = presenterFactory(view)
            return view
        }
    }

    override fun displayBoard(boardState: IntArray, squareSide: Int) {
        for (i in 0 until squareSide) {
            for (j in 0 until squareSide) {
                output.print("${boardState[i * squareSide + j]}\t")
            }
            output.println()
        }
        output.println()
    }

    override fun showMessage(message: String) {
        output.println(message)
    }

    override fun processCommand() {
        // BufferedReader.readLine() returns null cleanly at EOF (unlike Scanner, which
        // throws NoSuchElementException on exhausted input). Translating that into a
        // dedicated EndOfInputException - rather than java.io.EOFException, which file
        // APIs also throw on a truncated save file - lets PresenterImpl.play() tell
        // "no more input" apart from a recoverable per-command error without also
        // swallowing an unrelated file-read failure. See GH-4's 651ca64 for the bug
        // this used to cause when EOF and per-command errors were conflated.
        // A dead-but-not-EOF stream (e.g. the controlling terminal disappearing)
        // makes readLine() throw IOException rather than return null - treat that
        // the same as EOF instead of letting it fall into the recoverable-error
        // branch below, which would re-enter this method forever consuming no
        // input (the same "spin on a dead stream" failure this fix set out to close).
        val line = try {
            input.readLine()
        } catch (e: IOException) {
            null
        } ?: throw EndOfInputException()
        val parts = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val command = parts.getOrNull(0)?.lowercase() ?: throw IllegalArgumentException("Incorrect input")

        when (command) {
            "left" -> presenter.shiftLeft(intArg(parts))
            "right" -> presenter.shiftRight(intArg(parts))
            "up" -> presenter.shiftUp(intArg(parts))
            "down" -> presenter.shiftDown(intArg(parts))

            "reset" -> {
                requireArgCount(parts, 1)
                presenter.resetGame()
            }

            else -> throw IllegalArgumentException("Incorrect input")
        }

        output.println()
    }

    private fun intArg(parts: List<String>): Int {
        requireArgCount(parts, 2)
        return parts[1].toIntOrNull() ?: throw IllegalArgumentException("Incorrect input")
    }

    private fun requireArgCount(parts: List<String>, expected: Int) {
        if (parts.size != expected) throw IllegalArgumentException("Incorrect input")
    }

    override fun play() {
        presenter.play()
    }
}
