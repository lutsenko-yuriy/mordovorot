package viewmodel

/**
 * Keyboard mode's selected tile (GH-170, ADR-0003): [row] and [col], 0-based. The select keys
 * [move] it, wrapping; the shift keys shift its row or column and leave it where it is.
 */
data class TileSelection(val row: Int, val col: Int) {

    fun move(direction: Direction, side: Int): TileSelection = when (direction) {
        Direction.UP -> copy(row = (row - 1).mod(side))
        Direction.DOWN -> copy(row = (row + 1).mod(side))
        Direction.LEFT -> copy(col = (col - 1).mod(side))
        Direction.RIGHT -> copy(col = (col + 1).mod(side))
    }

    fun clampedTo(side: Int): TileSelection = copy(row = row.coerceAtMost(side - 1), col = col.coerceAtMost(side - 1))

    companion object {
        val INITIAL = TileSelection(0, 0)
    }
}
