package view.tui

/** Which of the board's four arrow rows/columns [ArrowCursor.index] is counted along - `LEFT`/
 *  `RIGHT` index by row, `TOP`/`BOTTOM` by column, all 0-based, same dialect as [HitTarget]. */
enum class Edge { LEFT, RIGHT, TOP, BOTTOM }

/** The keyboard-mode board cursor (GH-18): one highlighted shift arrow, identified by which
 *  [edge] it's on and its 0-based [index] along that edge - `LEFT[r]`/`RIGHT[r]` are row `r`'s
 *  `◀`/`▶`, `TOP[c]`/`BOTTOM[c]` are column `c`'s `▲`/`▼`. */
data class ArrowCursor(val edge: Edge, val index: Int)

/**
 * Pure geometry for GH-18's keyboard board cursor: the 4×[squareSide] shift arrows as one
 * perimeter ring, with [move] stepping an [ArrowCursor] in an arrow-key direction (corners wrap
 * rather than dead-ending) and [toHitTarget] mapping a cursor to the [HitTarget.ShiftLeft]/
 * [HitTarget.ShiftRight]/[HitTarget.ShiftUp]/[HitTarget.ShiftDown] a click on that same arrow
 * would produce. No terminal or viewModel involved - see the plan comment on GH-18 for the full
 * movement table this implements.
 */
class ArrowRing(private val squareSide: Int) {

    fun move(cursor: ArrowCursor, direction: Direction): ArrowCursor {
        val last = squareSide - 1
        val (edge, index) = cursor
        return when (edge) {
            Edge.LEFT -> when (direction) {
                Direction.UP -> if (index > 0) ArrowCursor(Edge.LEFT, index - 1) else ArrowCursor(Edge.TOP, 0)
                Direction.DOWN -> if (index < last) ArrowCursor(Edge.LEFT, index + 1) else ArrowCursor(Edge.BOTTOM, 0)
                Direction.LEFT, Direction.RIGHT -> ArrowCursor(Edge.RIGHT, index)
            }
            Edge.RIGHT -> when (direction) {
                Direction.UP -> if (index > 0) ArrowCursor(Edge.RIGHT, index - 1) else ArrowCursor(Edge.TOP, last)
                Direction.DOWN -> if (index < last) ArrowCursor(Edge.RIGHT, index + 1) else ArrowCursor(Edge.BOTTOM, last)
                Direction.LEFT, Direction.RIGHT -> ArrowCursor(Edge.LEFT, index)
            }
            Edge.TOP -> when (direction) {
                Direction.UP, Direction.DOWN -> ArrowCursor(Edge.BOTTOM, index)
                Direction.LEFT -> if (index > 0) ArrowCursor(Edge.TOP, index - 1) else ArrowCursor(Edge.LEFT, 0)
                Direction.RIGHT -> if (index < last) ArrowCursor(Edge.TOP, index + 1) else ArrowCursor(Edge.RIGHT, 0)
            }
            Edge.BOTTOM -> when (direction) {
                Direction.UP, Direction.DOWN -> ArrowCursor(Edge.TOP, index)
                Direction.LEFT -> if (index > 0) ArrowCursor(Edge.BOTTOM, index - 1) else ArrowCursor(Edge.LEFT, last)
                Direction.RIGHT -> if (index < last) ArrowCursor(Edge.BOTTOM, index + 1) else ArrowCursor(Edge.RIGHT, last)
            }
        }
    }

    fun toHitTarget(cursor: ArrowCursor): HitTarget = when (cursor.edge) {
        Edge.LEFT -> HitTarget.ShiftLeft(cursor.index)
        Edge.RIGHT -> HitTarget.ShiftRight(cursor.index)
        Edge.TOP -> HitTarget.ShiftUp(cursor.index)
        Edge.BOTTOM -> HitTarget.ShiftDown(cursor.index)
    }
}
