package view

import InputMode
import analytics.AnalyticsService
import analytics.NoopAnalyticsService
import board_model.BoardSize
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintStream
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import viewmodel.ExitRequestedException
import viewmodel.ModeSwitcher
import viewmodel.NoopModeSwitcher
import viewmodel.SaveInfo
import viewmodel.UNSOLVABLE_MARK
import viewmodel.catchingNonControl
import viewmodel.UiRequest
import viewmodel.UnsolvableSaveChoice
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

    /** Receives `help_command_used`/`console_command_rejected` (GH-67). */
    var analytics: AnalyticsService = NoopAnalyticsService()
        internal set

    /** Set after `help` or a rejected command, whose output takes the board's place until the
     *  next command (GH-67). */
    private var skipNextBoard = false

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
            analytics: AnalyticsService = NoopAnalyticsService(),
        ): ViewImpl {
            val view = ViewImpl(input, output)
            view.modeSwitcher = modeSwitcherFactory(view)
            view.viewModel = viewModel
            view.analytics = analytics
            return view
        }
    }

    override fun displayBoard(boardState: IntArray, squareSide: Int, moveCount: Int) {
        for (i in 0 until squareSide) {
            for (j in 0 until squareSide) {
                output.print("${boardState[i * squareSide + j] + DISPLAY_OFFSET}\t")
            }
            output.println()
        }
        output.println("Moves: $moveCount")
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
        val verb = parts.getOrNull(0)?.lowercase() ?: throw ConsoleCommandException("Empty command", "empty")
        val command = ConsoleCommands.find(verb)
            ?: throw ConsoleCommandException("Unknown command: $verb", "unknown_command")
        val typed = Typed(verb, command, parts)

        when (command.name) {
            "left" -> typed.shift { viewModel.shiftLeft(it) }
            "right" -> typed.shift { viewModel.shiftRight(it) }
            "up" -> typed.shift { viewModel.shiftUp(it) }
            "down" -> typed.shift { viewModel.shiftDown(it) }

            "reset" -> {
                typed.requireArgCount(1)
                viewModel.resetGame()
            }

            // Not subject to DISPLAY_OFFSET - a size isn't a row/column index (GH-44 WU3).
            "size" -> {
                val size = typed.intArg()
                typed.rangeChecked(size) { viewModel.newGame(size, trigger = "command") }
            }

            "save" -> viewModel.saveGame(typed.nameArg())
            "load" -> viewModel.loadGame(typed.loadNameArg())

            "exit" -> {
                typed.requireArgCount(1)
                viewModel.exitGame()
            }

            "mouse" -> {
                typed.requireArgCount(1)
                modeSwitcher.switchTo(InputMode.MOUSE, trigger = "command")
            }
            "keyboard" -> {
                typed.requireArgCount(1)
                modeSwitcher.switchTo(InputMode.KEYBOARD, trigger = "command")
            }

            "help" -> help(typed)

            else -> error("No handler for console command '${command.name}'")
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

    internal suspend fun confirmPlayAgain(): Boolean {
        output.print("Start a new game? [y/N] ")
        val line = readLineOrNull() ?: return false
        return line.trim().lowercase() in setOf("y", "yes")
    }

    internal suspend fun resolveUnsolvableSave(): UnsolvableSaveChoice = UnsolvableSaveChoice.EXIT

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

    /** `help` and `help <topic>` (GH-67): an unknown topic falls back to the full list. */
    private fun help(typed: Typed) {
        if (typed.parts.size > 2) throw typed.reject("Too many arguments for ${typed.verb}", "too_many_arguments")
        val side = viewModel.squareSide()
        val asked = typed.parts.getOrNull(1)
        val topic = asked?.let { ConsoleCommands.find(it) }
        output.println(topic?.let { ConsoleCommands.helpLine(it, side) } ?: ConsoleCommands.helpText(side))
        skipNextBoard = true
        // The unknown word itself is never sent - free text may carry PII.
        analytics.track(
            "help_command_used",
            when {
                asked == null -> mapOf("scope" to "all")
                topic == null -> mapOf("scope" to "unknown_topic")
                else -> mapOf("scope" to "command", "command" to topic.name)
            },
        )
    }

    /** One parsed console line: [verb] lowercased as typed, [command] its table entry. Every
     *  rejection is a [ConsoleCommandException] naming the problem plus the usage line (GH-67). */
    private inner class Typed(val verb: String, val command: ConsoleCommand, val parts: List<String>) {

        fun reject(problem: String, reason: String) = ConsoleCommandException(
            "$problem. Usage: ${ConsoleCommands.usage(verb, viewModel.squareSide())}",
            reason,
            command.name,
        )

        fun requireArgCount(expected: Int) {
            if (parts.size < expected) throw reject("Missing ${command.arg.missing} for $verb", "missing_argument")
            if (parts.size > expected) throw reject("Too many arguments for $verb", "too_many_arguments")
        }

        fun intArg(): Int {
            requireArgCount(2)
            return parts[1].toIntOrNull() ?: throw reject("${command.arg.label} must be a number for $verb", "not_a_number")
        }

        /** The range check stays in board_model (GH-10) - its rejection is only reworded here. */
        inline fun rangeChecked(value: Int, action: () -> Unit) {
            try {
                action()
            } catch (e: IllegalArgumentException) {
                throw reject("${command.arg.label} $value is out of range for $verb", "out_of_range")
            }
        }

        fun shift(action: (Int) -> Unit) {
            val value = intArg()
            rangeChecked(value) { action(value - DISPLAY_OFFSET) }
        }

        /** Save/load file names are free-form text, not a 1-based index - no [DISPLAY_OFFSET]
         *  translation applies here. */
        fun nameArg(): String {
            requireArgCount(2)
            return parts[1]
        }

        /** [nameArg], but also strips a trailing `(NxN)` token - what `viewmodel.SaveInfo.display`
         *  (GH-44 WU4) appends when a save's size is shown in the `load`-not-found message, so
         *  typing back exactly what's shown (`load foo (5x5)`) loads `foo` (round 2 audit finding
         *  on PR #54), and the same with a further ` unsolvable` mark (GH-74). A real save name
         *  with a trailing `(NxN)`-shaped token is unreachable: every save path rejects whitespace
         *  in a name ([viewmodel.ViewModelImpl.promptForValidSaveName],
         *  `view.tui.TuiView.handleToolbarSave`). */
        fun loadNameArg(): String {
            val sized = parts.size == 3 || (parts.size == 4 && parts[3] == UNSOLVABLE_MARK)
            if (sized && Regex("""\(\d+x\d+\)""").matches(parts[2])) return parts[1]
            return nameArg()
        }
    }

    /** The console session: offers the startup restore prompt, prints the help hint (on every
     *  console start, a switch back from the TUI included - GH-67), then loops `displayBoard` plus
     *  `processCommand` - or, once solved, `offerPlayAgain` (GH-66) - until exit or EOF (GH-42 WU3 - moved here from
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
            showMessage(ConsoleCommands.HELP_HINT)
            while (true) {
                // Other control-flow exceptions (e.g. mode switch) propagate to GameSession.
                try {
                    catchingNonControl(
                        onError = { e ->
                            if (e is EndOfInputException) throw e
                            showMessage(e.message ?: "Error") // not System.err - keeps order with board output
                            if (e is ConsoleCommandException) reportRejection(e)
                        },
                    ) {
                        if (skipNextBoard) {
                            skipNextBoard = false
                        } else {
                            displayBoard(viewModel.boardState(), viewModel.squareSide(), viewModel.moveCount())
                        }
                        if (viewModel.isSolved()) viewModel.offerPlayAgain() else processCommand()
                    }
                } catch (e: EndOfInputException) {
                    break
                } catch (e: ExitRequestedException) {
                    break
                }
            }
        } finally {
            ui.cancel()
        }
    }

    private suspend fun reportRejection(e: ConsoleCommandException) {
        showMessage(ConsoleCommands.HELP_HINT)
        skipNextBoard = true
        analytics.track(
            "console_command_rejected",
            buildMap {
                put("reason", e.reason)
                e.command?.let { put("command", it) }
            },
        )
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
            is UiRequest.ConfirmPlayAgain -> request.respond(confirmPlayAgain())
            is UiRequest.ResolveUnsolvableSave -> request.respond(resolveUnsolvableSave())
        }
    }
}
