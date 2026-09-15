package view

import InputMode
import board_model.BoardSize
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import viewmodel.ExitRequestedException
import viewmodel.ModeSwitcher
import viewmodel.NoopModeSwitcher
import viewmodel.SaveInfo
import viewmodel.SessionControlException
import viewmodel.UiRequest
import viewmodel.ViewModel
import viewmodel.display

/**
 * Created by yurich on 08.12.16.
 */
class ViewImpl internal constructor(
    private val input: BufferedReader = BufferedReader(InputStreamReader(System.`in`)),
    private val output: PrintStream = System.out,
) : View {

    /** Must be assigned before [play] or [processCommand] are called - use [create]. */
    lateinit var viewModel: ViewModel
        internal set

    /** Backs the `mouse`/`keyboard` commands (GH-30) - defaults to a no-op so existing call
     *  sites/tests that don't care about mode switching need no change. */
    var modeSwitcher: ModeSwitcher = NoopModeSwitcher()
        internal set

    companion object {
        // The console is 1-based (row/column input, displayed tile values); board_model
        // and storage stay 0-based. This is the only translation point (GH-10).
        private const val DISPLAY_OFFSET = 1

        /** The only public way to obtain a [ViewImpl] - wires [viewModel]/[modeSwitcher] atomically.
         *  [viewModel] no longer needs a `View` to already exist (GH-42 WU3 - it holds no `View`
         *  reference at all), unlike [modeSwitcherFactory], which still does. */
        fun create(
            input: BufferedReader = BufferedReader(InputStreamReader(System.`in`)),
            output: PrintStream = System.out,
            modeSwitcherFactory: (View) -> ModeSwitcher = { NoopModeSwitcher() },
            viewModel: ViewModel,
        ): ViewImpl {
            val view = ViewImpl(input, output)
            view.modeSwitcher = modeSwitcherFactory(view)
            view.viewModel = viewModel
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
            "left" -> viewModel.shiftLeft(oneBasedIndexArg(parts))
            "right" -> viewModel.shiftRight(oneBasedIndexArg(parts))
            "up" -> viewModel.shiftUp(oneBasedIndexArg(parts))
            "down" -> viewModel.shiftDown(oneBasedIndexArg(parts))

            "reset" -> {
                requireArgCount(parts, 1)
                viewModel.resetGame()
            }

            // Not subject to DISPLAY_OFFSET - a size isn't a row/column index (GH-44 WU3).
            // Out-of-range/non-numeric values throw, same as shift's "Incorrect row"/"Incorrect
            // column" - caught by play()'s generic Exception handler and shown as a message.
            "size" -> {
                requireArgCount(parts, 2)
                val size = parts[1].toIntOrNull() ?: throw IllegalArgumentException("Incorrect input")
                viewModel.newGame(size, trigger = "command")
            }

            "save" -> viewModel.saveGame(nameArg(parts))
            "load" -> viewModel.loadGame(loadNameArg(parts))

            "exit", "quit" -> {
                requireArgCount(parts, 1)
                viewModel.exitGame()
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
    internal suspend fun confirmRestore(save: SaveInfo): Boolean {
        output.print("Restore save '${save.display()}'? [y/N] ")
        val line = readLineOrNull() ?: return false
        return line.trim().lowercase() in setOf("y", "yes")
    }

    internal suspend fun chooseSaveToRestore(saves: List<SaveInfo>): String? {
        output.print(
            "Multiple saves found: ${saves.joinToString(", ") { it.display() }}. " +
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

    /** Re-prompts on an out-of-range or non-numeric answer instead of falling back to
     *  [current] - unlike blank/EOF, which *is* "keep the current size" (`null`). Safe to loop
     *  here: this calls no request-raising viewModel method, so the WU2 deadlock invariant
     *  (see [ask][viewmodel.ViewModelImpl.ask]'s KDoc) holds (GH-44). */
    internal suspend fun chooseBoardSize(current: Int): Int? {
        while (true) {
            output.print("Board size? [${BoardSize.MIN}-${BoardSize.MAX}, Enter for $current]: ")
            val line = readLineOrNull() ?: return null
            val typed = line.trim()
            if (typed.isEmpty()) return null
            val size = typed.toIntOrNull()
            if (size != null && BoardSize.isValid(size)) return size
            output.println("Please enter a number between ${BoardSize.MIN} and ${BoardSize.MAX}, or press Enter to keep $current.")
        }
    }

    /** null on a clean EOF or a dead stream (IOException) - both mean "no more input". Unlike
     *  [processCommand], the startup-restore and exit-before-quitting prompts treat that as
     *  "decline"/"blank" rather than throwing - see [viewmodel.ViewModelImpl]'s
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

    /** [nameArg], but also strips a trailing `(NxN)` token - what `viewmodel.SaveInfo.display`
     *  (GH-44 WU4) now appends when a save's size is shown in the `load`-not-found message, so
     *  typing back exactly what's shown (`load foo (5x5)`) loads `foo` instead of failing with
     *  "Incorrect input" (round 2 audit finding on PR #54, same class of issue
     *  [viewmodel.ViewModelImpl.restoreOnStartup]'s own match already handles). A real save name
     *  containing a literal trailing `(NxN)`-shaped token is not reachable through any in-app
     *  save path - every one rejects whitespace in a name
     *  ([viewmodel.ViewModelImpl.promptForValidSaveName], `view.tui.TuiView.handleToolbarSave`). */
    private fun loadNameArg(parts: List<String>): String {
        if (parts.size == 3 && Regex("""\(\d+x\d+\)""").matches(parts[2])) return parts[1]
        return nameArg(parts)
    }

    private fun requireArgCount(parts: List<String>, expected: Int) {
        if (parts.size != expected) throw IllegalArgumentException("Incorrect input")
    }

    /** The console session: offers the startup restore prompt, then loops
     *  `displayBoard`/`processCommand` until the board is solved (GH-42 WU3 - moved here from
     *  the old `ConsoleViewModelImpl.play()` (GH-23), since a viewModel-owned loop that called back into
     *  a `View` it also raised requests on would deadlock - see hard problem 1 on the plan
     *  comment on GH-42). Alongside it, drains [viewModel]'s [viewmodel.ViewModel.uiRequests]
     *  (GH-42 WU2) - the handler coroutine is what lets `saveGame`/`loadGame`/`exitGame`/
     *  `restoreOnStartup` suspend on [viewmodel.ViewModelImpl.ask] instead of calling back into
     *  this view directly. Cancelled once `play()` returns either way. */
    override suspend fun play() = coroutineScope {
        val ui = launch { for (request in viewModel.uiRequests) handle(request) }
        try {
            viewModel.restoreOnStartup()
            while (!viewModel.isSolved()) {
                try {
                    displayBoard(viewModel.boardState(), viewModel.squareSide())
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
            is UiRequest.ConfirmRestore -> request.respond(confirmRestore(request.save))
            is UiRequest.ChooseSaveToRestore -> request.respond(chooseSaveToRestore(request.saves))
            is UiRequest.ConfirmSaveBeforeExit -> request.respond(confirmSaveBeforeExit())
            is UiRequest.PromptSaveName -> request.respond(promptSaveName())
            is UiRequest.ChooseBoardSize -> request.respond(chooseBoardSize(request.current))
        }
    }
}
