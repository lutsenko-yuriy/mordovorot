package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import board_model.Solvability
import viewmodel.CurrentSaveName
import viewmodel.GameShiftSources
import viewmodel.MordieHooks
import viewmodel.SessionTileLabels
import viewmodel.SharedProgress
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
        begin()
        analytics.track("new_game_size_selected", mapOf("size" to size, "trigger" to trigger))
    }

    /** A fresh game on [arrangement] (GH-243, a share link's): throws, changing nothing, unless it is a solvable board.
     *  [sender]'s progress, if the link carries it, opens Mordie's line (GH-272). [tutorial] marks the tutorial's game (GH-288). */
    fun newGameFrom(arrangement: IntArray, sender: SharedProgress?, tutorial: Boolean = false) {
        require(Solvability.isSolvable(arrangement)) { "No sequence of shifts solves this board" }
        board.restoreState(arrangement, 0)
        begin(sender, tutorial)
    }

    private fun begin(sender: SharedProgress? = null, tutorial: Boolean = false) {
        solution.leave()
        mordieHooks.onNewGame(sender, tutorial)
        currentSaveName.name = null
        tileLabels.resetToPreference(board.squareSide)
        gameShiftSources.reset()
        unsavedProgress.unsaved = false
    }
}
