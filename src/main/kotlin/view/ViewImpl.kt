package view

import InputMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import presenter.ExitRequestedException
import presenter.ModeSwitcher
import presenter.NoopModeSwitcher
import presenter.Presenter
import presenter.SessionControlException
import presenter.UiRequest
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

    /** Backs the `mouse`/`keyboard` commands (GH-30) - defaults to a no-op so existing call
     *  sites/tests that don't care about mode switching need no change. */
    var modeSwitcher: ModeSwitcher = NoopModeSwitcher()
        internal set

    companion object {
        // The console is 1-based (row/column input, displayed tile values); board_model
        // and storage stay 0-based. This is the only translation point (GH-10).
        private const val DISPLAY_OFFSET = 1

        /** The only public way to obtain a [ViewImpl] - wires [presenter]/[modeSwitcher] atomically.
         *  [presenter] no longer needs a `View` to already exist (GH-42 WU3 - it holds no `View`
         *  reference at all), unlike [modeSwitcherFactory], which still does. */
        fun create(
            input: BufferedReader = BufferedReader(InputStreamReader(System.`in`)),
            output: PrintStream = System.out,
            modeSwitcherFactory: (View) -> ModeSwitcher = { NoopModeSwitcher() },
            presenter: Presenter,
        ): ViewImpl {
            val view = ViewImpl(input, output)
            view.modeSwitcher = modeSwitcherFactory(view)
            view.presenter = presenter
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

    override suspend fun showMessage(message: String) {
        output.println(message)
    }

    override suspend fun processCommand() {
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

            "mouse" -> {
                requireArgCount(parts, 1)
                modeSwitcher.switchTo(InputMode.MOUSE, trigger = "command")
            }
            "keyboard" -> {
                requireArgCount(parts, 1)
                modeSwitcher.switchTo(InputMode.KEYBOARD, trigger = "command")
            }

            else -> throw IllegalArgumentException("Incorrect input")
        }

        output.println()
    }

    /** No longer a [View] override (GH-42 WU2) - called only from [handle], this view's own
     *  [uiRequests] handler. Stays `internal`, not `private`, so `ViewImplCommandTest` can keep
     *  driving it directly. */
    internal suspend fun confirmRestore(saveName: String): Boolean {
        output.print("Restore save '$saveName'? [y/N] ")
        val line = readLineOrNull() ?: return false
        return line.trim().lowercase() in setOf("y", "yes")
    }

    internal suspend fun chooseSaveToRestore(saveNames: List<String>): String? {
        output.print(
            "Multiple saves found: ${saveNames.joinToString(", ")}. " +
                "Type a name to restore, or press Enter to start a new game: "
        )
        val line = readLineOrNull() ?: return null
        return line.trim().ifEmpty { null }
    }

    internal suspend fun confirmSaveBeforeExit(): Boolean {
        output.print("Save before quitting? [y/N] ")
        val line = readLineOrNull() ?: return false
        return line.trim().lowercase() in setOf("y", "yes")
    }

    internal suspend fun promptSaveName(): String? {
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

    /** The console session: offers the startup restore prompt, then loops
     *  `displayBoard`/`processCommand` until the board is solved (GH-42 WU3 - moved here from
     *  the old `ConsolePresenterImpl.play()` (GH-23), since a presenter-owned loop that called back into
     *  a `View` it also raised requests on would deadlock - see hard problem 1 on the plan
     *  comment on GH-42). Alongside it, drains [presenter]'s [presenter.Presenter.uiRequests]
     *  (GH-42 WU2) - the handler coroutine is what lets `saveGame`/`loadGame`/`exitGame`/
     *  `restoreOnStartup` suspend on [presenter.PresenterImpl.ask] instead of calling back into
     *  this view directly. Cancelled once `play()` returns either way. */
    override suspend fun play() = coroutineScope {
        val ui = launch { for (request in presenter.uiRequests) handle(request) }
        try {
            presenter.restoreOnStartup()
            while (!presenter.isSolved()) {
                try {
                    displayBoard(presenter.boardState(), presenter.squareSide())
                    processCommand()
                } catch (e: EndOfInputException) {
                    break
                } catch (e: ExitRequestedException) {
                    break
                } catch (e: SessionControlException) {
                    throw e // e.g. ModeSwitchRequestedException - must reach GameSession, not the catch-all below
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    showMessage(e.message ?: "Error") // not System.err - stays in sync with the board output
                }
            }
        } finally {
            ui.cancel()
        }
    }

    private suspend fun handle(request: UiRequest<*>) {
        when (request) {
            is UiRequest.ShowMessage -> {
                showMessage(request.text)
                request.respond(Unit)
            }
            is UiRequest.ConfirmRestore -> request.respond(confirmRestore(request.saveName))
            is UiRequest.ChooseSaveToRestore -> request.respond(chooseSaveToRestore(request.saveNames))
            is UiRequest.ConfirmSaveBeforeExit -> request.respond(confirmSaveBeforeExit())
            is UiRequest.PromptSaveName -> request.respond(promptSaveName())
        }
    }
}
