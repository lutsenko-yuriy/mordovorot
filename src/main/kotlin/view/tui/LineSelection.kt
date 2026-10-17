package view.tui

/**
 * Keyboard mode's board selection (GH-101): a remembered [row] and [col] (0-based, the
 * [HitTarget] dialect), with [axis] saying which one is active. WASD go through [select],
 * HJKL/arrows through [shift]; a key on the inactive axis switches to its remembered line.
 */
data class LineSelection(val row: Int, val col: Int, val axis: Axis) {

    enum class Axis { ROW, COLUMN }

    sealed class ShiftOutcome {
        data class Shift(val target: HitTarget) : ShiftOutcome()
        data class Switched(val selection: LineSelection) : ShiftOutcome()
    }

    /** Moves the active line by one, wrapping on a [side]-sized board, or switches axis. */
    fun select(direction: Direction, side: Int): LineSelection = when (direction) {
        Direction.UP, Direction.DOWN ->
            if (axis == Axis.ROW) copy(row = (row + direction.step).mod(side)) else copy(axis = Axis.ROW)
        Direction.LEFT, Direction.RIGHT ->
            if (axis == Axis.COLUMN) copy(col = (col + direction.step).mod(side)) else copy(axis = Axis.COLUMN)
    }

    fun shift(direction: Direction): ShiftOutcome = when (direction) {
        Direction.LEFT -> if (axis == Axis.ROW) ShiftOutcome.Shift(HitTarget.ShiftLeft(row)) else switched()
        Direction.RIGHT -> if (axis == Axis.ROW) ShiftOutcome.Shift(HitTarget.ShiftRight(row)) else switched()
        Direction.UP -> if (axis == Axis.COLUMN) ShiftOutcome.Shift(HitTarget.ShiftUp(col)) else switched()
        Direction.DOWN -> if (axis == Axis.COLUMN) ShiftOutcome.Shift(HitTarget.ShiftDown(col)) else switched()
    }

    /** Whether the tile at [row], [col] belongs to the active line. */
    fun covers(row: Int, col: Int): Boolean = if (axis == Axis.ROW) row == this.row else col == this.col

    fun clampedTo(side: Int): LineSelection = copy(row = row.coerceAtMost(side - 1), col = col.coerceAtMost(side - 1))

    private fun switched() = ShiftOutcome.Switched(copy(axis = if (axis == Axis.ROW) Axis.COLUMN else Axis.ROW))

    companion object {
        val INITIAL = LineSelection(0, 0, Axis.ROW)
    }
}

private val Direction.step: Int
    get() = if (this == Direction.UP || this == Direction.LEFT) -1 else 1

/** The one [LineSelection] a session's keyboard modes share (GH-101), made by
 *  `buildGameSession` so a switch to mouse mode and back keeps it. */
class KeyboardSelection {
    var current: LineSelection = LineSelection.INITIAL
}
