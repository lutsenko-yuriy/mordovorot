package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import viewmodel.GameShiftSources
import viewmodel.GameStart
import viewmodel.MordieHooks
import viewmodel.RestartButton
import viewmodel.UnsavedProgress

/** Restart (GH-349): puts the same board back to its starting arrangement, with its size, labels and
 *  save name; Mordie keeps his search. Asking about the save first is the view's. */
internal class RestartGameUseCase(
    private val board: BoardModel,
    private val gameStart: GameStart,
    private val mordieHooks: MordieHooks,
    private val analytics: AnalyticsService,
    private val solution: SolutionUseCase,
    private val gameShiftSources: GameShiftSources,
    private val unsavedProgress: UnsavedProgress,
) {

    fun button(): RestartButton {
        val start = gameStart.arrangement()
        return when {
            solution.onScreen -> RestartButton.HIDDEN
            start == null || board.moveCount == gameStart.moves && board.boardArray.contentEquals(start) -> RestartButton.DIMMED
            else -> RestartButton.LIVE
        }
    }

    /** [saveChoice] is `not_asked`, `dont_save` or `saved`; does nothing unless the button is live. */
    fun restart(openedFrom: String, saveChoice: String) {
        if (button() != RestartButton.LIVE) return
        val wasSolved = board.isCorrect()
        val discarded = board.moveCount - gameStart.moves
        board.restoreState(gameStart.arrangement()!!, gameStart.moves)
        mordieHooks.onRestart()
        gameShiftSources.reset()
        unsavedProgress.unsaved = false
        gameStart.restarts++
        analytics.track(
            "game_restarted",
            mapOf(
                "board_size" to board.squareSide,
                "move_count" to discarded,
                "was_solved" to wasSolved,
                "save_choice" to saveChoice,
                "opened_from" to openedFrom,
            ),
        )
    }
}
