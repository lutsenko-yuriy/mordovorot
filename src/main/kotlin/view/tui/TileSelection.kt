package view.tui

/**
 * Keyboard mode's selected tile (GH-170, ADR-0003): [row] and [col], 0-based (the [HitTarget]
 * dialect). The select keys [move] it, wrapping; the shift keys shift its row or column
 * ([shiftTarget]) and leave it where it is.
 */
data class TileSelection(val row: Int, val col: Int) {

    fun move(direction: Direction, side: Int): TileSelection = when (direction) {
        Direction.UP -> copy(row = (row - 1).mod(side))
        Direction.DOWN -> copy(row = (row + 1).mod(side))
        Direction.LEFT -> copy(col = (col - 1).mod(side))
        Direction.RIGHT -> copy(col = (col + 1).mod(side))
    }

    /** Left/right shift the selected row, up/down its column. */
    fun shiftTarget(direction: Direction): HitTarget = when (direction) {
        Direction.LEFT -> HitTarget.ShiftLeft(row)
        Direction.RIGHT -> HitTarget.ShiftRight(row)
        Direction.UP -> HitTarget.ShiftUp(col)
        Direction.DOWN -> HitTarget.ShiftDown(col)
    }

    fun clampedTo(side: Int): TileSelection = copy(row = row.coerceAtMost(side - 1), col = col.coerceAtMost(side - 1))

    companion object {
        val INITIAL = TileSelection(0, 0)
    }
}

/** The one [TileSelection] a session's keyboard modes share, made by `buildGameSession` so a
 *  switch to mouse mode and back keeps it. */
class KeyboardSelection {
    var current: TileSelection = TileSelection.INITIAL
}
