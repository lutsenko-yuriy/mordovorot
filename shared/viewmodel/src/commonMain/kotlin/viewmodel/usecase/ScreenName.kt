package viewmodel.usecase

import board_model.BoardModel

/** The `screen` property of the dialogs' analytics (GH-139, GH-170): `solution`, `solved` or `play`. */
internal fun screenName(board: BoardModel, solution: SolutionUseCase): String = when {
    solution.onScreen -> "solution"
    board.isCorrect() -> "solved"
    else -> "play"
}
