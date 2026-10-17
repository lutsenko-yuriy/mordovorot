package viewmodel

import analytics.AnalyticsService
import board_model.BoardModel
import kotlinx.coroutines.channels.ReceiveChannel
import viewmodel.usecase.ExitGameUseCase
import viewmodel.usecase.LoadGameUseCase
import viewmodel.usecase.NewGameUseCase
import viewmodel.usecase.SaveGameUseCase

/** The one [ViewModel] implementation (GH-42 WU3, collapsing GH-23's `BaseViewModel` +
 *  `ConsoleViewModelImpl` + `TuiViewModelImpl` split): shifts and the read-only query surface
 *  live here, and each other flow is a use case in `viewmodel.usecase` (GH-78). Holds no `View`
 *  reference; talks to whichever View is driving it through [uiRequests] instead of a
 *  constructor-injected dependency. */
class ViewModelImpl internal constructor(
    private val board: BoardModel,
    private val analytics: AnalyticsService,
    /** Seeded `true` by `GameSession` after a mode switch, so the prompt doesn't re-show. */
    startupRestoreDone: Boolean = false,
    /** `true` when `--size=N` was given at launch (GH-44) - the flag *is* the answer, so
     *  [restoreOnStartup] skips both the restore prompt and the size prompt entirely and starts
     *  fresh at the size the board was already constructed with, regardless of what saves
     *  exist. */
    private val sizeChosenAtLaunch: Boolean = false,
    private val mordieHooks: MordieHooks,
    private val ui: UiRequestChannel,
    private val catalog: SaveCatalog,
    private val newGameUseCase: NewGameUseCase,
    private val saveGameUseCase: SaveGameUseCase,
    private val loadGameUseCase: LoadGameUseCase,
    private val exitGameUseCase: ExitGameUseCase,
) : ViewModel {

    override val uiRequests: ReceiveChannel<UiRequest<*>> = ui.requests

    private suspend fun <R> ask(request: UiRequest<R>): R = ui.ask(request)

    override fun shiftLeft(row: Int) = shift { board.shiftLeft(row) }

    override fun shiftRight(row: Int) = shift { board.shiftRight(row) }

    override fun shiftUp(col: Int) = shift { board.shiftUp(col) }

    override fun shiftDown(col: Int) = shift { board.shiftDown(col) }

    /** Runs a shift and tracks `board_solved` when it turns an unsolved board into a solved one
     *  (GH-62). Loads never pass through here, so restoring a solved save doesn't fire it.
     *  Mordie's check runs first, so a solution it reveals on the solving shift counts. */
    private fun shift(action: () -> Unit) {
        val wasSolved = board.isCorrect()
        action()
        mordieHooks.onShift()
        if (!wasSolved && board.isCorrect()) {
            val mordieProps = mordieHooks.onSolved()
            analytics.track("board_solved", mapOf("move_count" to board.moveCount, "board_size" to board.squareSide) + mordieProps)
        }
    }

    override fun newGame(size: Int, trigger: String) = newGameUseCase.newGame(size, trigger)

    override suspend fun saveGame(name: String): Boolean = saveGameUseCase.saveGame(name)

    override suspend fun loadGame(name: String) {
        loadGameUseCase.loadGame(name)
    }

    override suspend fun exitGame() = exitGameUseCase.exitGame()

    /** Guards [restoreOnStartup] against running twice - including across a mode switch. */
    private var startupRestoreDone = startupRestoreDone

    /** The startup sequence (GH-44 grew this beyond just the restore prompt): offers to restore
     *  a previous save (0/1/2+ saves, unchanged from GH-6), then - only if nothing was actually
     *  restored - offers a size prompt for the fresh game about to start. Skips both steps
     *  entirely when [sizeChosenAtLaunch] is set: `--size=N` *is* the answer, so the board just
     *  keeps the size it was already constructed with, regardless of what saves exist. Mordie
     *  starts on whatever board play begins with, unless it is already bound (a mode switch). */
    override suspend fun restoreOnStartup() {
        restoreOrPickSize()
        mordieHooks.onStartup()
    }

    private suspend fun restoreOrPickSize() {
        if (startupRestoreDone) return
        startupRestoreDone = true
        if (sizeChosenAtLaunch) return
        // Any failure here just starts a fresh game instead of crashing at boot.
        catchingNonControl(onError = {}) {
            val saveInfos = catalog.infos()
            val restored = if (saveInfos.isEmpty()) {
                false
            } else {
                analytics.track("startup_restore_prompt_shown", mapOf("save_file_count" to saveInfos.size))

                val nameToRestore = if (saveInfos.size == 1) {
                    saveInfos[0].name.takeIf { ask(UiRequest.ConfirmRestore(saveInfos[0])) }
                } else {
                    // The TUI answers with a listed name; anything else counts as declined (GH-79).
                    val chosen = ask(UiRequest.ChooseSaveToRestore(saveInfos))
                    saveInfos.firstOrNull { it.name == chosen }?.name
                }

                // loadGame can still fail here (a save deleted or corrupted since listSaves()
                // ran just above) - decision reflects what actually happened, not just what was
                // picked.
                fun trackDecision(decision: String) =
                    analytics.track("startup_restore_decision", mapOf("decision" to decision, "save_file_count" to saveInfos.size))
                val didRestore = try {
                    nameToRestore?.let { loadGameUseCase.loadGame(it, trigger = "startup_prompt") } ?: false
                } catch (e: ExitRequestedException) {
                    // Exit from the unsolvable-save choice (GH-74).
                    trackDecision("quit")
                    throw e
                }
                trackDecision(if (didRestore) "restored" else "new_game")
                didRestore
            }

            if (!restored) {
                ask(UiRequest.ChooseBoardSize(board.squareSide))?.let { newGame(it, trigger = "startup_prompt") }
            }
        }
    }

    override fun listSaves(): List<SaveInfo> = catalog.listSaves()

    override fun saveExists(name: String): Boolean = catalog.saveExists(name)

    override fun isSolved(): Boolean = board.isCorrect()

    // A defensive copy - board.boardArray is the live, mutable backing array; handing it out
    // directly would let a caller (or a future one) mutate board state without going through
    // shiftLeft/Right/Up/Down (audit finding on PR #22).
    override fun boardState(): IntArray = board.boardArray.copyOf()

    override fun squareSide(): Int = board.squareSide

    override fun moveCount(): Int = board.moveCount

    override fun mordieLine(): String? = mordieHooks.line()

    override fun mordieEnding(): String? = mordieHooks.ending()
}
