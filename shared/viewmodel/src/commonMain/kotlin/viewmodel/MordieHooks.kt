package viewmodel

import analytics.AnalyticsService
import board_model.BoardModel
import board_model.Solvability
import mordie.Mordie
import mordie.MordieCheck
import mordie.SearchProgress
import mordie.SolutionMove
import mordie.SolutionSource
import storage.SavedMordie
import storage.SavedMove

/** Mordie's part of [GameViewModelImpl] (GH-83): binds it to each game, tracks its analytics and
 *  words its line and ending. */
internal class MordieHooks(
    private val mordie: Mordie,
    private val board: BoardModel,
    private val analytics: AnalyticsService,
    private val solutionScreen: SolutionScreen,
) {

    fun onNewGame() = if (tooBig()) mordie.sitOut(board.squareSide) else mordie.startFresh(board.boardArray)

    /** Binds Mordie to the board just loaded from a save with [data]. A solution-screen save
     *  (GH-126) resumes found, whatever the board. A searching save with an unsolvable original
     *  can only be hand-edited, so it is treated like a save with no data. */
    fun onLoaded(data: SavedMordie?) {
        when {
            tooBig() -> if (board.isCorrect()) mordie.clear() else mordie.sitOut(board.squareSide)
            data != null && data.solutionScreen -> mordie.resume(data.original, data.moves, found = true, data.solutionMoves(), data.provenShortest, data.ruledOut)
            board.isCorrect() ->
                if (data == null) mordie.clear() else mordie.restoreSolved(data.original, data.moves, data.found, data.solutionMoves(), data.provenShortest, data.ruledOut)
            data != null && (data.found || Solvability.isSolvable(data.original)) ->
                mordie.resume(data.original, data.moves, data.found, data.solutionMoves(), data.provenShortest, data.ruledOut)
            else -> startOnLoadedBoard()
        }
    }

    fun onRepaired() = startOnLoadedBoard()

    /** Starts Mordie unless it is already bound. */
    fun onStartup() = if (tooBig()) mordie.ensureSatOut(board.squareSide) else mordie.ensureStarted(board.boardArray)

    /** Call after every shift; tracks `mordie_solution_found` when this shift's check reveals the
     *  solution, and `mordie_shorter_found` when it announces a shorter one (GH-223). */
    fun onShift() {
        when (val check = mordie.onShift(board.moveCount)) {
            null -> {}
            is MordieCheck.Revealed -> trackRevealed(check.progress)
            is MordieCheck.Shortened -> analytics.track(
                "mordie_shorter_found",
                mapOf(
                    "board_size" to board.squareSide,
                    "solution_length" to check.moves,
                    "previous_length" to check.previous,
                    "proven_shortest" to check.provenShortest,
                ),
            )
        }
    }

    private fun trackRevealed(found: SearchProgress) = analytics.track(
        "mordie_solution_found",
        mapOf(
            "board_size" to board.squareSide,
            "solution_length" to found.moves,
            "player_shift_count" to mordie.shifts,
            "search_duration_ms" to found.elapsedMs,
            "resumed_from_save" to mordie.resumedFromSave,
            "source" to (found.source ?: SolutionSource.EXACT_SEARCH).analyticsName,
            "proven_shortest" to found.provenShortest,
        ),
    )

    /** Freezes Mordie's result; returns its `board_solved` props, empty when it sat this game out. */
    fun onSolved(): Map<String, Any> {
        val outcome = mordie.onSolved() ?: return emptyMap()
        return mapOf(
            "mordie_result" to outcome.result(board.moveCount).name.lowercase(),
            "mordie_moves" to outcome.moves,
            "mordie_proven_shortest" to outcome.provenShortest,
        )
    }

    /** The player gives up: Mordie takes its live best solution and stops (GH-222). */
    fun onGiveUp() = mordie.giveUp()

    /** Whether the solution Mordie shows is proven shortest. */
    fun provenShortest(): Boolean = mordie.provenShortest

    fun snapshot(): SavedMordie? = mordie.snapshot()?.let { snapshot ->
        val solution = snapshot.solution?.map { SavedMove(it.isRow, it.index, it.amount) }
        SavedMordie(
            snapshot.original, snapshot.moves, snapshot.found, solution, solutionScreen = solutionScreen.replay != null,
            provenShortest = snapshot.provenShortest, ruledOut = snapshot.ruledOut,
        )
    }

    fun line(): String? = mordie.line()?.let(MordieText::line)

    /** Mordie's solution for the solution screen (GH-126); `null` when unknown or empty. */
    fun solution(): List<SolutionMove>? = mordie.solution?.takeIf { it.isNotEmpty() }

    fun original(): IntArray? = mordie.original()

    /** Whether Mordie sits games of this board's size out (GH-160), solved or not. */
    fun sitsOut(): Boolean = tooBig()

    /** Whether Mordie's solution has been revealed, whichever line announces it. */
    fun revealed(): Boolean = mordie.revealed

    /** Whether Mordie's frozen result, after a win, is a found solution. */
    fun foundBeforeWin(): Boolean = mordie.outcome()?.found == true

    /** `game_given_up`'s moves and shifts since `Found it!`; empty after loading a save that was
     *  already found. */
    fun sinceRevealProps(): Map<String, Int> = listOfNotNull(
        mordie.playerMovesAtReveal?.let { "moves_since_solution_found" to board.moveCount - it },
        mordie.shiftsAtReveal?.let { "shifts_since_solution_found" to mordie.shifts - it },
    ).toMap()

    fun ending(): String? = mordie.outcome()?.let { MordieText.ending(it, board.moveCount) }

    /** Boards Mordie can't search yet sit out, whatever a save says (GH-160). */
    private fun tooBig() = board.squareSide > Mordie.MAX_SIDE

    private fun SavedMordie.solutionMoves() = solution?.map { SolutionMove(it.isRow, it.index, it.amount) }

    /** Mordie's N would cover only the moves from here, so it sits out a board already played on. */
    private fun startOnLoadedBoard() {
        if (tooBig()) mordie.sitOut(board.squareSide) else if (board.moveCount == 0) mordie.startFresh(board.boardArray) else mordie.clear()
    }
}
