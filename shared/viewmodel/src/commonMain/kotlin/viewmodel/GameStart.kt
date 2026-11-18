package viewmodel

import board_model.BoardModel

/** Where the current game began (GH-349), for Restart: the arrangement and the moves already on the
 *  counter, and how often it was restarted. A new game, share link and load record it; one per session. */
class GameStart {
    private var arrangement: IntArray? = null

    /** The moves the start already counts: 0, except a loaded save whose start is unknown (GH-349). */
    var moves = 0
        private set

    /** Restarts of this game, for `board_solved.restart_count`; a new game or a load starts again. */
    var restarts = 0
        internal set

    /** The start of the game now on [board]: [original] when Mordie knows it, else the board as it is. */
    internal fun record(board: BoardModel, original: IntArray?) {
        arrangement = (original ?: board.boardArray).copyOf()
        moves = if (original != null) 0 else board.moveCount
        restarts = 0
    }

    /** The first game of a launch, when nothing recorded one yet. */
    internal fun recordIfNone(board: BoardModel, original: IntArray?) {
        if (arrangement == null) record(board, original)
    }

    internal fun arrangement(): IntArray? = arrangement?.copyOf()
}
