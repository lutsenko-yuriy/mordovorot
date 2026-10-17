package viewmodel

import analytics.AnalyticsService
import board_model.BoardModel
import kotlin.math.abs
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
    private val gameShiftSources: GameShiftSources,
    private val unsavedProgress: UnsavedProgress,
    private val unsolvableExit: UnsolvableSaveExit,
) : GameViewModel {

    override val uiRequests: ReceiveChannel<UiRequest<*>> = ui.requests

    override fun shiftLeft(row: Int, source: ShiftSource?) = shift(source) { board.shiftLeft(row) }

    override fun shiftRight(row: Int, source: ShiftSource?) = shift(source) { board.shiftRight(row) }

    override fun shiftUp(col: Int, source: ShiftSource?) = shift(source) { board.shiftUp(col) }

    override fun shiftDown(col: Int, source: ShiftSource?) = shift(source) { board.shiftDown(col) }

    override fun shiftRow(row: Int, steps: Int, source: ShiftSource?) =
        shift(source, steps) { if (steps > 0) board.shiftRight(row) else board.shiftLeft(row) }

    override fun shiftColumn(col: Int, steps: Int, source: ShiftSource?) =
        shift(source, steps) { if (steps > 0) board.shiftDown(col) else board.shiftUp(col) }

    /** Runs a shift and tracks `board_solved` when it turns an unsolved board into a solved one
     *  (GH-62). Loads never pass through here, so restoring a solved save doesn't fire it.
     *  Mordie's check runs first, so a solution it reveals on the solving shift counts.
     *  `shift_keys` (GH-101) and `shift_input` (GH-110) follow the solving shift's [source].
     *  [action] runs `|steps|` times (GH-110), each one counted by Mordie, and solved is checked
     *  once at the end, so a drag passing through the solved board doesn't count. */
    private fun shift(source: ShiftSource?, steps: Int = 1, action: () -> Unit) {
        if (steps == 0) return
        val wasSolved = board.isCorrect()
        repeat(abs(steps)) {
            action()
            mordieHooks.onShift()
        }
        source?.let(gameShiftSources::record)
        unsavedProgress.unsaved = true
        if (!wasSolved && board.isCorrect()) {
            val mordieProps = mordieHooks.onSolved()
            analytics.track(
                "board_solved",
                mapOf("move_count" to board.moveCount, "board_size" to board.squareSide) + mordieProps +
                    gameShiftSources.propertiesFor(source),
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

    override fun hasUnsavedProgress(): Boolean = unsavedProgress.unsaved

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
