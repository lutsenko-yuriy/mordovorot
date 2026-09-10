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
        internal set

    companion object {
        // The console is 1-based (row/column input, displayed tile values); board_model
        // and storage stay 0-based. This is the only translation point (GH-10).
        private const val DISPLAY_OFFSET = 1

        /** The only public way to obtain a [ViewImpl] - wires [presenter] atomically. */
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
                output.print("${boardState[i * squareSide + j] + DISPLAY_OFFSET}\t")
            }
            output.println()
        }
        output.println()
    }

    override fun showMessage(message: String) {
        output.println(message)
    }

    override fun processCommand() {
        // null (clean EOF) or IOException (dead stream) both mean "no more input" -
        // see GH-4's 651ca64 for the infinite-loop bug this closes.
        val line = try {
            input.readLine()
        } catch (e: IOException) {
            throw EndOfInputException(e)
        } ?: throw EndOfInputException()
        val parts = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val command = parts.getOrNull(0)?.lowercase() ?: throw IllegalArgumentException("Incorrect input")

        when (command) {
            "left" -> presenter.shiftLeft(oneBasedIndexArg(parts))
            "right" -> presenter.shiftRight(oneBasedIndexArg(parts))
            "up" -> presenter.shiftUp(oneBasedIndexArg(parts))
            "down" -> presenter.shiftDown(oneBasedIndexArg(parts))

            "reset" -> {
                requireArgCount(parts, 1)
                presenter.resetGame()
            }

            "save" -> presenter.saveGame(nameArg(parts))
            "load" -> presenter.loadGame(nameArg(parts))

            "exit", "quit" -> {
                requireArgCount(parts, 1)
                presenter.exitGame()
            }

            else -> throw IllegalArgumentException("Incorrect input")
        }

        output.println()
    }

    override fun confirmRestore(saveName: String): Boolean {
        output.print("Restore save '$saveName'? [y/N] ")
        val line = readLineOrNull() ?: return false
        return line.trim().lowercase() in setOf("y", "yes")
    }

    override fun chooseSaveToRestore(saveNames: List<String>): String? {
        output.print(
            "Multiple saves found: ${saveNames.joinToString(", ")}. " +
                "Type a name to restore, or press Enter to start a new game: "
        )
        val line = readLineOrNull() ?: return null
        return line.trim().ifEmpty { null }
    }

    override fun confirmSaveBeforeExit(): Boolean {
        output.print("Save before quitting? [y/N] ")
        val line = readLineOrNull() ?: return false
        return line.trim().lowercase() in setOf("y", "yes")
    }

    override fun promptSaveName(): String? {
        output.print("Save name: ")
        val line = readLineOrNull() ?: return null
        return line.trim().ifEmpty { null }
    }

    /** null on a clean EOF or a dead stream (IOException) - both mean "no more input". Unlike
     *  [processCommand], the startup-restore and exit-before-quitting prompts treat that as
     *  "decline"/"blank" rather than throwing - see [presenter.PresenterImpl]'s
     *  restoreOnStartup/exitGame. */
    private fun readLineOrNull(): String? = try {
        input.readLine()
    } catch (e: IOException) {
        null
    }

    private fun oneBasedIndexArg(parts: List<String>): Int {
        requireArgCount(parts, 2)
        val value = parts[1].toIntOrNull() ?: throw IllegalArgumentException("Incorrect input")
        return value - DISPLAY_OFFSET
    }

    /** Save/load file names are free-form text, not a 1-based index - no [DISPLAY_OFFSET]
     *  translation applies here. */
    private fun nameArg(parts: List<String>): String {
        requireArgCount(parts, 2)
        return parts[1]
    }

    private fun requireArgCount(parts: List<String>, expected: Int) {
        if (parts.size != expected) throw IllegalArgumentException("Incorrect input")
    }

    override fun play() {
        presenter.play()
    }
}
