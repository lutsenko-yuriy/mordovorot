package web.tutorial

import web.Arrow
import web.drag.Axis

/** The shift a tutorial step asks for (GH-288): [tiles] tiles of [line] the way [arrow] points. */
data class AskedShift(val arrow: Arrow, val line: Int, val tiles: Int) {

    val axis: Axis get() = when (arrow) {
        Arrow.LEFT, Arrow.RIGHT -> Axis.ROW
        Arrow.UP, Arrow.DOWN -> Axis.COLUMN
    }

    /** Half a turn round the board, which either way gives the same line. */
    val halfTurn: Boolean get() = tiles * 2 == TutorialScript.SIDE
}

/** The tutorial's game (GH-288): a 4x4 board and the four shifts that solve it, the only four-move solution. */
object TutorialScript {

    const val SIDE = 4

    /** 0-based, row by row; 1-based it reads `8 1 14 3 / 5 2 7 12 / 11 6 9 10 / 13 16 15 4`. */
    val board = intArrayOf(7, 0, 13, 2, 4, 1, 6, 11, 10, 5, 8, 9, 12, 15, 14, 3)

    /** Top row one tile left; second column one up (tile 14 comes round); third row two right; fourth column one down. */
    val shifts = listOf(
        AskedShift(Arrow.LEFT, line = 0, tiles = 1),
        AskedShift(Arrow.UP, line = 1, tiles = 1),
        AskedShift(Arrow.RIGHT, line = 2, tiles = 2),
        AskedShift(Arrow.DOWN, line = 3, tiles = 1),
    )

    /** The 0-based tile step 2 points at, as it leaves the top of its column and comes back at the bottom. */
    const val WRAPPING_TILE = 13

    /** The step after the last shift: the solved board and Mordie's challenge. */
    const val SOLVED_STEP = 5
}
