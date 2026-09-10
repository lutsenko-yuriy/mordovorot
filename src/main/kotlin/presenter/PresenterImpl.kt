package presenter

import analytics.AnalyticsService
import analytics.NoopAnalyticsService
import board_model.BoardImpl
import board_model.BoardModel
import storage.FileSaveRepository
import storage.SaveRepository
import view.EndOfInputException
import view.View

class PresenterImpl(
    var view: View,
    var board: BoardModel = BoardImpl(),
    private val saves: SaveRepository = FileSaveRepository(),
    private val analytics: AnalyticsService = NoopAnalyticsService(),
) : Presenter {

    override fun shiftLeft(row: Int) = board.shiftLeft(row)

    override fun shiftRight(row: Int) = board.shiftRight(row)

    override fun shiftUp(col: Int) = board.shiftUp(col)

    override fun shiftDown(col: Int) = board.shiftDown(col)

    override fun resetGame() = board.resetGame()

    override fun saveGame(name: String): Boolean {
        // saves.exists/save can throw (bad name, IOException) - caught here rather than left to
        // play()'s generic handler, so a failed save is still tracked and gets its own message
        // instead of silently missing from save_command_used (audit finding on PR #13).
        try {
            val existed = saves.exists(name)
            saves.save(name, board.boardArray, board.SQUARE_SIDE)
            analytics.track("save_command_used", mapOf("result" to "success", "overwrote_existing" to existed))
            view.showMessage("Saved as '$name'.")
            return true
        } catch (e: Exception) {
            analytics.track("save_command_used", mapOf("result" to "error"))
            // e.message alone can be uninformative or outright misleading here - e.g.
            // AccessDeniedException's message is just the offending temp-file path, with no
            // hint that a save failed at all (audit round 3 on GH-12/PR #15, surfaced by the
            // exit-before-quitting flow where this is now the sole explanation for an aborted
            // quit, not just one line among several after an explicit `save`).
            view.showMessage("Could not save as '$name': ${e.message ?: e::class.simpleName}")
            return false
        }
    }

    override fun loadGame(name: String) {
        loadGame(name, trigger = "command")
    }

    /** [trigger] is `"command"` for the mid-game `load` console command, or `"startup_prompt"`
     *  when called from [restoreOnStartup] - see `load_command_used` in docs/ANALYTICS_EVENTS.md.
     *  Returns whether the board was actually restored, so [restoreOnStartup] can report an
     *  accurate `startup_restore_decision` instead of assuming success (audit on PR #14: a
     *  corrupted/mismatched save offered at startup was being recorded as "restored"). */
    private fun loadGame(name: String, trigger: String): Boolean {
        // The whole body is guarded, not just saves.load - availableSavesMessage() (itself
        // saves.listSaves()) and board.restoreState can also throw, and WU4's startup restore
        // flow calls this before play()'s try/catch exists, so loadGame must not throw
        // regardless of which step fails (audit on PR #13).
        try {
            val saved = saves.load(name)
            if (saved == null) {
                analytics.track("load_command_used", mapOf("trigger" to trigger, "result" to "not_found"))
                view.showMessage("No save named '$name'. ${availableSavesMessage()}")
                return false
            }
            if (saved.squareSide != board.SQUARE_SIDE) {
                analytics.track("load_command_used", mapOf("trigger" to trigger, "result" to "size_mismatch"))
                view.showMessage(
                    "Save '$name' is a ${saved.squareSide}x${saved.squareSide} board and can't be loaded onto " +
                        "this ${board.SQUARE_SIDE}x${board.SQUARE_SIDE} board."
                )
                return false
            }
            board.restoreState(saved.state)
            analytics.track("load_command_used", mapOf("trigger" to trigger, "result" to "success"))
            view.showMessage("Loaded '$name'.")
            return true
        } catch (e: Exception) {
            analytics.track("load_command_used", mapOf("trigger" to trigger, "result" to "error"))
            view.showMessage(e.message ?: "Could not load '$name'.")
            return false
        }
    }

    /**
     * Ends the current [play] session on demand (the `exit`/`quit` command), asking whether to
     * save first. Declining, or a blank/EOF name, ends the session unsaved. A save *attempt*
     * that fails does **not** end the session (see below) - [exitGame] can return normally
     * instead of always throwing [ExitRequestedException].
     */
    override fun exitGame() {
        if (!view.confirmSaveBeforeExit()) {
            analytics.track("exit_command_used", mapOf("save_choice" to "declined"))
            throw ExitRequestedException()
        }

        val name = promptForValidSaveName()
        if (name == null) {
            analytics.track("exit_command_used", mapOf("save_choice" to "declined"))
            throw ExitRequestedException()
        }

        if (!saveGame(name)) {
            // saveGame already showed the error via its own catch block, but that alone reads
            // like ordinary game output, not "your quit was cancelled" - say so explicitly
            // (audit round 3 on GH-12/PR #15). Quitting anyway would compound a failed save
            // with a lost session - instead, let the user try `exit` (or `save`) again once
            // the underlying issue (e.g. a read-only saves/ directory) is resolved. No
            // exit_command_used - the session didn't actually end, so "declined" would
            // misrepresent an explicit save request as a decision not to save (audit round 2).
            view.showMessage(
                "Not quitting - your game is still running. Fix the problem and try exit again, " +
                    "or answer n to quit without saving."
            )
            return
        }

        analytics.track("exit_command_used", mapOf("save_choice" to "saved"))
        throw ExitRequestedException()
    }

    /** Re-prompts until [View.promptSaveName] returns a name usable by the `save`/`load`
     *  commands, or `null` on a blank answer or EOF, meaning the user doesn't want to save.
     *  Re-prompting on *any* invalid name (rather than treating it as "don't save" or letting
     *  it fall through to [saveGame]'s failure path) avoids silently discarding an explicit
     *  save request over a naming mistake, and keeps `save_command_used {result: error}`
     *  meaning "the save attempt failed", not "the user mistyped" (audit rounds 2 and 3 on
     *  PR #15). Checks both:
     *  - no whitespace - `save`/`load`'s command-line parsing splits on it, see
     *    [view.ViewImpl.nameArg], so a name with spaces could never be `load`ed back;
     *  - [SaveRepository.isValidName] - blank, a path separator, or `..` would make
     *    [saveGame] throw, which [promptForValidSaveName]'s caller must not confuse with a
     *    genuine save failure (e.g. a read-only `saves/` directory).
     */
    private fun promptForValidSaveName(): String? {
        while (true) {
            val name = view.promptSaveName() ?: return null
            if (name.any { it.isWhitespace() } || !saves.isValidName(name)) {
                view.showMessage(
                    "'$name' isn't a usable save name (no spaces, path separators, or '..') - " +
                        "try again, or press Enter to skip saving."
                )
            } else {
                return name
            }
        }
    }

    /** Signals [play] to stop, the same way [EndOfInputException] does - thrown by [exitGame]
     *  after its save-before-quitting dialogue completes. */
    private class ExitRequestedException : Exception()

    private fun availableSavesMessage(): String {
        // Guarded on its own - a failure here (e.g. an unreadable saves/ directory) shouldn't
        // change the load's actual result (it was still "not found"), just degrade the message.
        val available = try {
            saves.listSaves()
        } catch (e: Exception) {
            return "Could not list available saves."
        }
        return if (available.isEmpty()) "No saves available." else "Available saves: ${available.joinToString(", ")}"
    }

    /**
     * Offers to restore a previous game at startup, before the play loop begins. No-op if
     * there are no saves. One save asks a yes/no question; two or more list names and let the
     * user type one (blank/EOF -> new game; an unknown name re-prompts rather than silently
     * falling back to a new game). Never throws - this runs before [play]'s own try/catch
     * exists, mirroring [loadGame]/[saveGame]'s non-throwing contract.
     */
    private fun restoreOnStartup() {
        try {
            val saveNames = saves.listSaves()
            if (saveNames.isEmpty()) return

            analytics.track("startup_restore_prompt_shown", mapOf("save_file_count" to saveNames.size))

            val nameToRestore = if (saveNames.size == 1) {
                saveNames[0].takeIf { view.confirmRestore(it) }
            } else {
                var chosen: String? = null
                while (chosen == null) {
                    val typed = view.chooseSaveToRestore(saveNames) ?: break
                    if (typed in saveNames) {
                        chosen = typed
                    } else {
                        view.showMessage("No save named '$typed'. Available saves: ${saveNames.joinToString(", ")}")
                    }
                }
                chosen
            }

            // loadGame can still fail here (a save deleted or corrupted since listSaves() ran
            // just above) - decision reflects what actually happened, not just what was picked.
            val restored = nameToRestore?.let { loadGame(it, trigger = "startup_prompt") } ?: false
            analytics.track(
                "startup_restore_decision",
                mapOf("decision" to (if (restored) "restored" else "new_game"), "save_file_count" to saveNames.size),
            )
        } catch (e: Exception) {
            // Any failure here (unreadable saves dir, view I/O error) just means the game
            // starts fresh instead of crashing at boot.
        }
    }

    override fun play() {
        restoreOnStartup()
        while (!board.isCorrect()) {
            try {
                view.displayBoard(board.boardArray, board.SQUARE_SIDE)
                view.processCommand()
            } catch (e: EndOfInputException) {
                return
            } catch (e: ExitRequestedException) {
                return
            } catch (e: Exception) {
                view.showMessage(e.message ?: "Error") // not System.err - stays in sync with the board output
            }
        }
    }
}
