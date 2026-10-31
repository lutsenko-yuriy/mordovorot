package web.drag

import kotlin.math.abs
import kotlin.math.roundToInt
import viewmodel.Direction

enum class Axis { ROW, COLUMN }

/** A drag under way: [line] on [axis], moved by [tiles] (positive is right or down). */
data class LineOffset(val axis: Axis, val line: Int, val tiles: Double)

/** What a released drag does: [steps] shifts of [line] on [axis] (positive is right or down). */
data class DragShift(val axis: Axis, val line: Int, val steps: Int)

/** The way the line moves: a row right or left, a column down or up, by the sign of [DragShift.steps]. */
val DragShift.direction: Direction get() = when (axis) {
    Axis.ROW -> if (steps > 0) Direction.RIGHT else Direction.LEFT
    Axis.COLUMN -> if (steps > 0) Direction.DOWN else Direction.UP
}

/** One drag from a press on the tile at [row], [col] at ([x], [y]), on a board of [side] with tiles
 *  [pitch] apart, all in pixels (GH-110). The first move past [THRESHOLD] locks the axis. */
class LineDrag(
    private val row: Int,
    private val col: Int,
    private val x: Double,
    private val y: Double,
    private val pitch: Double,
    private val side: Int,
) {

    /** `null` until the axis locks. */
    var offset: LineOffset? = null
        private set

    fun move(x: Double, y: Double) {
        val dx = x - this.x
        val dy = y - this.y
        val axis = offset?.axis ?: when {
            maxOf(abs(dx), abs(dy)) < THRESHOLD -> return
            abs(dx) >= abs(dy) -> Axis.ROW
            else -> Axis.COLUMN
        }
        offset = when (axis) {
            Axis.ROW -> LineOffset(axis, row, dx / pitch)
            Axis.COLUMN -> LineOffset(axis, col, dy / pitch)
        }
    }

    /** The shift the drag snaps to: the nearest whole tile, a full loop counting as none (D5);
     *  `null` when that changes nothing. */
    fun release(): DragShift? {
        val offset = offset ?: return null
        val steps = offset.tiles.roundToInt() % side
        return if (steps == 0) null else DragShift(offset.axis, offset.line, steps)
    }

    companion object {
        const val THRESHOLD = 6.0
    }
}
