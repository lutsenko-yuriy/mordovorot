package web.scenario

import kotlinx.coroutines.test.TestScope
import mordie.SearchProgress
import mordie.SolutionMove
import web.Arrow
import web.ReplaySaveChoice

/** GH-349: what the Restart scenarios share. */

/** 4x4 two moves from solved: row 0 right once, then column 1 down twice. */
internal val RESTART_TWO_MOVES = intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)

/** [RESTART_TWO_MOVES]' solution. */
internal val RESTART_PATH = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2))

/** A 4x4 other than the harness's, for a share link: column 0 shifted one tile down. */
internal val RESTART_LINKED = intArrayOf(12, 1, 2, 3, 0, 5, 6, 7, 4, 9, 10, 11, 8, 13, 14, 15)

/** [ONE_SHIFT_FROM_SOLVED] with row 1 shifted one tile right: where a saved game of it stands after a move. */
internal val RESTART_MOVED = intArrayOf(3, 0, 1, 2, 7, 4, 5, 6) + IntArray(8) { it + 8 }

/** Three moves of different lines, which never solve [ONE_SHIFT_FROM_SOLVED]: each counts as a move of its own. */
internal fun WebHarness.threeMoves() {
    game.onArrow(Arrow.RIGHT, 1)
    game.onArrow(Arrow.DOWN, 2)
    game.onArrow(Arrow.RIGHT, 3)
}

/** The board at [ONE_SHIFT_FROM_SOLVED], 1-based, as a restart puts it back. */
internal val RESTART_START = shown(ONE_SHIFT_FROM_SOLVED)

/** `[ Restart ]` pressed with the question answered [choice]. */
internal fun WebHarness.restartWith(choice: ReplaySaveChoice) {
    game.restart.onRestart("button")
    game.restart.onAnswered(choice)
}

/** `[ Restart ]` then Don't save. */
internal fun WebHarness.restartDiscarding() = restartWith(ReplaySaveChoice.DONT_SAVE)

/** Mordie finds [RESTART_PATH] on [RESTART_TWO_MOVES]. */
internal fun WebHarness.searchFinds() {
    mordieSearches.latest.progress = SearchProgress(RESTART_PATH.size, found = true, elapsedMs = 0, solution = RESTART_PATH)
}

/** Five shifts of column 3 (one move): enough for him to check. */
internal fun WebHarness.fiveShifts() = repeat(5) { game.onArrow(Arrow.UP, 3) }

/** Right on row 0, then down twice on column 1: [RESTART_PATH]. */
internal fun WebHarness.solveTwoMoves() {
    game.onArrow(Arrow.RIGHT, 0)
    repeat(2) { game.onArrow(Arrow.DOWN, 1) }
}

internal fun TestScope.onTwoMoves() = WebHarness(backgroundScope).startOn(RESTART_TWO_MOVES)
