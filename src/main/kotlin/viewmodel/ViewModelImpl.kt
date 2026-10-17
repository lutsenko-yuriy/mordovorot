package viewmodel

import viewmodel.usecase.ExitGameUseCase

/** The terminal [ViewModel] (GH-41): the shared [GameViewModelImpl] plus the terminal-only Exit
 *  flow, whose save goes through [game]. */
class ViewModelImpl internal constructor(
    private val game: GameViewModelImpl,
    private val exitGameUseCase: ExitGameUseCase,
) : ViewModel, GameViewModel by game {

    override suspend fun exitGame() = exitGameUseCase.exitGame(game::saveGame)
}
