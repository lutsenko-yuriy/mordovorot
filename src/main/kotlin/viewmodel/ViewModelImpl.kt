package viewmodel

import analytics.AnalyticsService
import analytics.NoopAnalyticsService
import board_model.BoardImpl
import board_model.BoardModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import storage.FileSaveRepository
import storage.SaveRepository

/** The one [ViewModel] implementation (GH-42 WU3, collapsing GH-23's `BaseViewModel` +
 *  `ConsoleViewModelImpl` + `TuiViewModelImpl` split): owns board/saves/analytics wiring and
 *  every domain-mutation flow - shift/reset/save/load/exit/restoreOnStartup plus the read-only
 *  query surface. Holds no `View` reference; talks to whichever View is driving it through
 *  [uiRequests] instead of a constructor-injected dependency. */
class ViewModelImpl(
    private val board: BoardModel = BoardImpl(),
    private val saves: SaveRepository = FileSaveRepository(),
    private val analytics: AnalyticsService = NoopAnalyticsService(),
    /** Seeded `true` by `GameSession` after a mode switch, so the prompt doesn't re-show. */
    startupRestoreDone: Boolean = false,
    /** `true` when `--size=N` was given at launch (GH-44) - the flag *is* the answer, so
     *  [restoreOnStartup] skips both the restore prompt and the size prompt entirely and starts
     *  fresh at the size the board was already constructed with, regardless of what saves
     *  exist. */
    private val sizeChosenAtLaunch: Boolean = false,
) : ViewModel {

    /** Rendezvous - `ask` doesn't return until the View has actually finished handling the
     *  request, which is what keeps message/prompt ordering identical to the old direct blocking
     *  calls into `View` (GH-42 WU2, see docs/ARCHITECTURE.md's viewModel section). */
    private val requests = Channel<UiRequest<*>>(Channel.RENDEZVOUS)

    override val uiRequests: ReceiveChannel<UiRequest<*>> = requests

    /** Raises [request] on [uiRequests] and suspends until the View responds. The View must
     *  never call back into a request-raising viewModel method (`saveGame`/`loadGame`/`exitGame`/
     *  `restoreOnStartup`) from inside its own request handler - doing so deadlocks, since the
     *  handler is `ask`'s only consumer and would be busy with the request that triggered the
     *  callback. */
    private suspend fun <R> ask(request: UiRequest<R>): R {
        requests.send(request)
        return request.awaitResponse()
    }

    /** The one place [UiRequest.ShowMessage] is raised - every other message in this class goes
     *  through this, not a bare `ask` call, so a future new message site can't forget it. */
    private suspend fun showMessage(text: String) {
        ask(UiRequest.ShowMessage(text))
    }

    override fun shiftLeft(row: Int) = board.shiftLeft(row)

    override fun shiftRight(row: Int) = board.shiftRight(row)

    override fun shiftUp(col: Int) = board.shiftUp(col)

    override fun shiftDown(col: Int) = board.shiftDown(col)

    override fun resetGame() = board.resetGame()

    override fun newGame(size: Int, trigger: String) {
        board.newGame(size)
        analytics.track("new_game_size_selected", mapOf("size" to size, "trigger" to trigger))
    }

    override suspend fun saveGame(name: String): Boolean {
        // saves.exists/save can throw (bad name, IOException) - caught here rather than left to
        // play()'s generic handler, so a failed save is still tracked and gets its own message
        // instead of silently missing from save_command_used (audit finding on PR #13).
        try {
            val existed = saves.exists(name)
            saves.save(name, board.boardArray, board.squareSide)
            analytics.track("save_command_used", mapOf("result" to "success", "overwrote_existing" to existed))
            showMessage("Saved as '$name'.")
            return true
        } catch (e: SessionControlException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            analytics.track("save_command_used", mapOf("result" to "error"))
            // e.message alone can be uninformative or outright misleading here - e.g.
            // AccessDeniedException's message is just the offending temp-file path, with no
            // hint that a save failed at all (audit round 3 on GH-12/PR #15, surfaced by the
            // exit-before-quitting flow where this is now the sole explanation for an aborted
            // quit, not just one line among several after an explicit `save`).
            showMessage("Could not save as '$name': ${e.message ?: e::class.simpleName}")
            return false
        }
    }

    override suspend fun loadGame(name: String) {
        loadGame(name, trigger = "command")
    }

    /** [trigger] is `"command"` for the mid-game `load` console command, or `"startup_prompt"`
     *  when called from [restoreOnStartup] - see `load_command_used` in docs/ANALYTICS_EVENTS.md.
     *  Returns whether the board was actually restored, so [restoreOnStartup] can report an
     *  accurate `startup_restore_decision` instead of assuming success (audit on PR #14: a
     *  corrupted/mismatched save offered at startup was being recorded as "restored"). */
    private suspend fun loadGame(name: String, trigger: String): Boolean {
        // The whole body is guarded, not just saves.load - availableSavesMessage() (itself
        // saves.listSaves()) and board.restoreState can also throw, and the startup restore
        // flow calls this before any View's own try/catch exists, so loadGame must not throw
        // regardless of which step fails (audit on PR #13).
        try {
            val saved = saves.load(name)
            if (saved == null) {
                analytics.track("load_command_used", mapOf("trigger" to trigger, "result" to "not_found"))
                showMessage("No save named '$name'. ${availableSavesMessage()}")
                return false
            }
            if (saved.squareSide != board.squareSide) {
                analytics.track("load_command_used", mapOf("trigger" to trigger, "result" to "size_mismatch"))
                showMessage(
                    "Save '$name' is a ${saved.squareSide}x${saved.squareSide} board and can't be loaded onto " +
                        "this ${board.squareSide}x${board.squareSide} board."
                )
                return false
            }
            board.restoreState(saved.state)
            analytics.track("load_command_used", mapOf("trigger" to trigger, "result" to "success"))
            showMessage("Loaded '$name'.")
            return true
        } catch (e: SessionControlException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            analytics.track("load_command_used", mapOf("trigger" to trigger, "result" to "error"))
            showMessage(e.message ?: "Could not load '$name'.")
            return false
        }
    }

    /**
     * Ends the current session on demand (the `exit`/`quit` command), asking whether to
     * save first. Declining, or a blank/EOF name, ends the session unsaved. A save *attempt*
     * that fails does **not** end the session (see below) - [exitGame] can return normally
     * instead of always throwing [ExitRequestedException].
     */
    override suspend fun exitGame() {
        if (!ask(UiRequest.ConfirmSaveBeforeExit())) {
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
            showMessage(
                "Not quitting - your game is still running. Fix the problem and try exit again, " +
                    "or answer n to quit without saving."
            )
            return
        }

        analytics.track("exit_command_used", mapOf("save_choice" to "saved"))
        throw ExitRequestedException()
    }

    /** Re-prompts until [UiRequest.PromptSaveName] returns a name usable by the `save`/`load`
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
    private suspend fun promptForValidSaveName(): String? {
        while (true) {
            val name = ask(UiRequest.PromptSaveName()) ?: return null
            if (name.any { it.isWhitespace() } || !saves.isValidName(name)) {
                showMessage(
                    "'$name' isn't a usable save name (no spaces, path separators, or '..') - " +
                        "try again, or press Enter to skip saving."
                )
            } else {
                return name
            }
        }
    }

    private fun availableSavesMessage(): String {
        // Guarded on its own - a failure here (e.g. an unreadable saves/ directory) shouldn't
        // change the load's actual result (it was still "not found"), just degrade the message.
        // No CancellationException guard needed here (unlike the suspend methods above) -
        // saves.listSaves() isn't suspend, so this catch-all can never observe one; revisit if
        // that ever changes (audit finding on PR #45).
        val names = try {
            saves.listSaves()
        } catch (e: Exception) {
            return "Could not list available saves."
        }
        return if (names.isEmpty()) {
            "No saves available."
        } else {
            "Available saves: ${names.joinToString(", ") { SaveInfo(it, sizeOf(it)).display() }}"
        }
    }

    /** [SaveInfo.squareSide] for [name], `null` when the save can't be read (corrupted or an
     *  IO error) - a listing degrades to showing the name alone rather than dropping the save
     *  or failing the whole listing (GH-44 WU4). No CancellationException guard needed here -
     *  same reasoning as [availableSavesMessage]'s: [SaveRepository.load] isn't suspend, so this
     *  catch-all can never observe one, even though this is reachable from suspend callers
     *  (audit finding on PR #54). */
    private fun sizeOf(name: String): Int? =
        try {
            saves.load(name)?.squareSide
        } catch (e: Exception) {
            null
        }

    /** Guards [restoreOnStartup] against running twice - including across a mode switch. */
    private var startupRestoreDone = startupRestoreDone

    /** The startup sequence (GH-44 grew this beyond just the restore prompt): offers to restore
     *  a previous save (0/1/2+ saves, unchanged from GH-6), then - only if nothing was actually
     *  restored - offers a size prompt for the fresh game about to start. Skips both steps
     *  entirely when [sizeChosenAtLaunch] is set: `--size=N` *is* the answer, so the board just
     *  keeps the size it was already constructed with, regardless of what saves exist. */
    override suspend fun restoreOnStartup() {
        if (startupRestoreDone) return
        startupRestoreDone = true
        if (sizeChosenAtLaunch) return
        try {
            val saveInfos = saves.listSaves().map { SaveInfo(it, sizeOf(it)) }
            val restored = if (saveInfos.isEmpty()) {
                false
            } else {
                analytics.track("startup_restore_prompt_shown", mapOf("save_file_count" to saveInfos.size))

                val nameToRestore = if (saveInfos.size == 1) {
                    saveInfos[0].name.takeIf { ask(UiRequest.ConfirmRestore(saveInfos[0])) }
                } else {
                    var chosen: String? = null
                    while (chosen == null) {
                        val typed = ask(UiRequest.ChooseSaveToRestore(saveInfos)) ?: break
                        // Matches either the bare name or the "name (NxN)" the console prompt
                        // now prints (GH-44 WU4, audit finding on PR #54) - a user typing back
                        // exactly what they see must not get stuck re-prompted forever. Exact
                        // name match tried first - only a hand-placed save file with a space in
                        // its name (no in-app save path allows one) could make a display-form
                        // match ambiguous, and even then this order picks the save whose actual
                        // name was typed (round 2 audit finding on PR #54).
                        val match = saveInfos.firstOrNull { it.name == typed } ?: saveInfos.firstOrNull { it.display() == typed }
                        if (match != null) {
                            chosen = match.name
                        } else {
                            showMessage("No save named '$typed'. Available saves: ${saveInfos.joinToString(", ") { it.display() }}")
                        }
                    }
                    chosen
                }

                // loadGame can still fail here (a save deleted or corrupted since listSaves()
                // ran just above) - decision reflects what actually happened, not just what was
                // picked.
                val didRestore = nameToRestore?.let { loadGame(it, trigger = "startup_prompt") } ?: false
                analytics.track(
                    "startup_restore_decision",
                    mapOf("decision" to (if (didRestore) "restored" else "new_game"), "save_file_count" to saveInfos.size),
                )
                didRestore
            }

            if (!restored) {
                ask(UiRequest.ChooseBoardSize(board.squareSide))?.let { newGame(it, trigger = "startup_prompt") }
            }
        } catch (e: SessionControlException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Any failure here (unreadable saves dir, view I/O error) just means the game
            // starts fresh instead of crashing at boot.
        }
    }

    override fun listSaves(): List<SaveInfo> =
        try {
            saves.listSaves().map { SaveInfo(it, sizeOf(it)) }
        } catch (e: Exception) {
            emptyList()
        }

    override fun saveExists(name: String): Boolean =
        try {
            saves.exists(name)
        } catch (e: Exception) {
            false
        }

    override fun isSolved(): Boolean = board.isCorrect()

    // A defensive copy - board.boardArray is the live, mutable backing array; handing it out
    // directly would let a caller (or a future one) mutate board state without going through
    // shiftLeft/Right/Up/Down (audit finding on PR #22).
    override fun boardState(): IntArray = board.boardArray.copyOf()

    override fun squareSide(): Int = board.squareSide
}
