package viewmodel

import analytics.AnalyticsService
import board_model.BoardModel
import kotlinx.coroutines.channels.ReceiveChannel
import viewmodel.usecase.LoadGameUseCase
import viewmodel.usecase.LoadResult
import viewmodel.usecase.NewGameUseCase
import viewmodel.usecase.SaveGameUseCase
import viewmodel.usecase.StartupRestoreUseCase

/** The one [GameViewModel] implementation (GH-42 WU3, collapsing GH-23's `BaseViewModel` +
 *  `ConsoleViewModelImpl` + `TuiViewModelImpl` split; Exit split out for GH-41): shifts and the
 *  read-only query surface live here, and each other flow is a use case in `viewmodel.usecase`
 *  (GH-78). Holds no `View` reference; talks to whichever View is driving it through
 *  [uiRequests] instead of a constructor-injected dependency. Built by [buildGameViewModel]. */
class GameViewModelImpl internal constructor(
    private val board: BoardModel,
    private val analytics: AnalyticsService,
    private val mordieHooks: MordieHooks,
    private val ui: UiRequestChannel,
    private val catalog: SaveCatalog,
    private val newGameUseCase: NewGameUseCase,
    private val saveGameUseCase: SaveGameUseCase,
    private val loadGameUseCase: LoadGameUseCase,
    private val startupRestoreUseCase: StartupRestoreUseCase,
    private val currentSaveName: CurrentSaveName,
    private val gameShiftKeys: GameShiftKeys,
    private val unsolvableExit: UnsolvableSaveExit,
) : GameViewModel {

    override val uiRequests: ReceiveChannel<UiRequest<*>> = ui.requests

    override fun shiftLeft(row: Int, key: ShiftKey?) = shift(key) { board.shiftLeft(row) }

    override fun shiftRight(row: Int, key: ShiftKey?) = shift(key) { board.shiftRight(row) }

    override fun shiftUp(col: Int, key: ShiftKey?) = shift(key) { board.shiftUp(col) }

    override fun shiftDown(col: Int, key: ShiftKey?) = shift(key) { board.shiftDown(col) }

    /** Runs a shift and tracks `board_solved` when it turns an unsolved board into a solved one
     *  (GH-62). Loads never pass through here, so restoring a solved save doesn't fire it.
     *  Mordie's check runs first, so a solution it reveals on the solving shift counts.
     *  `shift_keys` (GH-101) only rides on a keyboard solve, so mouse mode never sends it. */
    private fun shift(key: ShiftKey?, action: () -> Unit) {
        val wasSolved = board.isCorrect()
        action()
        key?.let(gameShiftKeys::record)
        mordieHooks.onShift()
        if (!wasSolved && board.isCorrect()) {
            val mordieProps = mordieHooks.onSolved()
            val shiftKeyProps = gameShiftKeys.value?.takeIf { key != null }?.let { mapOf("shift_keys" to it) }.orEmpty()
            analytics.track(
                "board_solved",
                mapOf("move_count" to board.moveCount, "board_size" to board.squareSide) + mordieProps + shiftKeyProps,
            )
        }
    }

    override fun newGame(size: Int, trigger: String) = newGameUseCase.newGame(size, trigger)

    override suspend fun saveGame(name: String): Boolean = saveGameUseCase.saveGame(name)

    override suspend fun loadGame(name: String) {
        if (loadGameUseCase.loadGame(name) == LoadResult.EXIT_CHOSEN) unsolvableExit.onExit(this, atStartup = false)
    }

    override suspend fun restoreOnStartup() {
        val exitChosen = startupRestoreUseCase.restoreOnStartup()
        if (exitChosen) unsolvableExit.onExit(this, atStartup = true)
    }

    override fun listSaves(): List<SaveInfo> = catalog.listSaves()

    override fun saveExists(name: String): Boolean = catalog.saveExists(name)

    override fun currentSaveName(): String? = currentSaveName.name

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
