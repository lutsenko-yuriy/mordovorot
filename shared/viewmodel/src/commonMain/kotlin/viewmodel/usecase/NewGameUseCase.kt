package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import viewmodel.CurrentSaveName
import viewmodel.GameShiftSources
import viewmodel.MordieHooks
import viewmodel.SessionTileLabels
import viewmodel.UnsavedProgress

/** Starts a fresh game at a chosen size (GH-44), with Mordie racing it (GH-83). */
internal class NewGameUseCase(
    private val board: BoardModel,
    private val mordieHooks: MordieHooks,
    private val analytics: AnalyticsService,
    private val currentSaveName: CurrentSaveName,
    private val gameShiftSources: GameShiftSources,
    private val unsavedProgress: UnsavedProgress,
    private val solution: SolutionUseCase,
    private val tileLabels: SessionTileLabels,
) {

    fun newGame(size: Int, trigger: String) {
        board.newGame(size)
        solution.leave()
        mordieHooks.onNewGame()
        currentSaveName.name = null
        tileLabels.resetToPreference(board.squareSide)
        gameShiftSources.reset()
        unsavedProgress.unsaved = false
        analytics.track("new_game_size_selected", mapOf("size" to size, "trigger" to trigger))
    }
}
