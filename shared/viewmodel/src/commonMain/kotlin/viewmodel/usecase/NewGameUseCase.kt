package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import viewmodel.CurrentSaveName
import viewmodel.GameShiftKeys
import viewmodel.MordieHooks

/** Starts a fresh game at a chosen size (GH-44), with Mordie racing it (GH-83). */
internal class NewGameUseCase(
    private val board: BoardModel,
    private val mordieHooks: MordieHooks,
    private val analytics: AnalyticsService,
    private val currentSaveName: CurrentSaveName,
    private val gameShiftKeys: GameShiftKeys,
) {

    fun newGame(size: Int, trigger: String) {
        board.newGame(size)
        mordieHooks.onNewGame()
        currentSaveName.name = null
        gameShiftKeys.reset()
        analytics.track("new_game_size_selected", mapOf("size" to size, "trigger" to trigger))
    }
}
