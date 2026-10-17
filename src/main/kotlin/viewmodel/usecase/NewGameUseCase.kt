package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import viewmodel.MordieHooks

/** Starts a fresh game at a chosen size (GH-44), with Mordie racing it (GH-83). */
internal class NewGameUseCase(
    private val board: BoardModel,
    private val mordieHooks: MordieHooks,
    private val analytics: AnalyticsService,
) {

    fun newGame(size: Int, trigger: String) {
        board.newGame(size)
        mordieHooks.onNewGame()
        analytics.track("new_game_size_selected", mapOf("size" to size, "trigger" to trigger))
    }
}
