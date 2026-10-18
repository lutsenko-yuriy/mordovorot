package web.drag

import kotlin.math.pow
import kotlin.math.roundToInt
import web.Arrow

/** The slides that ease a line into its new place after a shift (GH-110). Each starts at the
 *  offset where the line still shows its old place and eases to none, drawn by [WrapPositions]. */
object Slides {

    fun ofArrow(arrow: Arrow, line: Int): LineOffset = when (arrow) {
        Arrow.LEFT -> LineOffset(Axis.ROW, line, 1.0)
        Arrow.RIGHT -> LineOffset(Axis.ROW, line, -1.0)
        Arrow.UP -> LineOffset(Axis.COLUMN, line, 1.0)
        Arrow.DOWN -> LineOffset(Axis.COLUMN, line, -1.0)
    }

    /** After [drag] on a board of [side] ends: the rest of the way to the nearest tile when the line
     *  [moved], else back to where it started (a full loop counting as none). `null` when already there. */
    fun afterDrag(drag: LineOffset, moved: Boolean, side: Int): LineOffset? {
        val left = if (moved) drag.tiles - drag.tiles.roundToInt() else drag.tiles
        val tiles = left - side * (left / side).roundToInt()
        return if (tiles == 0.0) null else drag.copy(tiles = tiles)
    }

    /** [start] eased out at [progress] from 0 to 1; `null` once it is over. */
    fun at(start: LineOffset, progress: Double): LineOffset? =
        if (progress >= 1) null else start.copy(tiles = start.tiles * (1 - progress).pow(3))
}
