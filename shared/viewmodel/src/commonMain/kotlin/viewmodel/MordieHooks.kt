package viewmodel

import analytics.AnalyticsService
import board_model.BoardModel
import board_model.Solvability
import mordie.Mordie
import storage.SavedMordie

/** Mordie's part of [GameViewModelImpl] (GH-83): binds it to each game, tracks its analytics and
 *  words its line and ending. */
internal class MordieHooks(
    private val mordie: Mordie,
    private val board: BoardModel,
    private val analytics: AnalyticsService,
) {

    fun onNewGame() = mordie.startFresh(board.boardArray)

    /** Binds Mordie to the board just loaded from a save with [data]. A searching save with an
     *  unsolvable original can only be hand-edited, so it is treated like a save with no data. */
    fun onLoaded(data: SavedMordie?) {
        when {
            board.isCorrect() ->
                if (data == null) mordie.clear() else mordie.restoreSolved(data.original, data.moves, data.found)
            data != null && (data.found || Solvability.isSolvable(data.original)) ->
                mordie.resume(data.original, data.moves, data.found)
            else -> startOnLoadedBoard()
        }
    }

    fun onRepaired() = startOnLoadedBoard()

    /** Starts Mordie unless it is already bound, e.g. after a mode switch. */
    fun onStartup() = mordie.ensureStarted(board.boardArray)

    /** Call after every shift; tracks `mordie_solution_found` when this shift's check reveals it. */
    fun onShift() {
        val found = mordie.onShift() ?: return
        analytics.track(
            "mordie_solution_found",
            mapOf(
                "board_size" to board.squareSide,
                "solution_length" to found.moves,
                "player_shift_count" to mordie.shifts,
                "search_duration_ms" to found.elapsedMs,
                "resumed_from_save" to mordie.resumedFromSave,
            ),
        )
    }

    /** Freezes Mordie's result; returns its `board_solved` props, empty when it sat this game out. */
    fun onSolved(): Map<String, Any> {
        val outcome = mordie.onSolved() ?: return emptyMap()
        return mapOf("mordie_result" to outcome.result(board.moveCount).name.lowercase(), "mordie_moves" to outcome.moves)
    }

    fun snapshot(): SavedMordie? = mordie.snapshot()?.let { SavedMordie(it.original, it.moves, it.found) }

    fun line(): String? = mordie.line()?.let(MordieText::line)

    fun ending(): String? = mordie.outcome()?.let { MordieText.ending(it, board.moveCount) }

    /** Mordie's N would cover only the moves from here, so it sits out a board already played on. */
    private fun startOnLoadedBoard() {
        if (board.moveCount == 0) mordie.startFresh(board.boardArray) else mordie.clear()
    }
}
