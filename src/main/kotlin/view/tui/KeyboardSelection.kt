package view.tui

/** The shared [viewmodel.TileSelection], under the name the terminal's call sites use (GH-171). */
typealias TileSelection = viewmodel.TileSelection

/** Left/right shift the selected row, up/down its column. */
fun TileSelection.shiftTarget(direction: Direction): HitTarget = when (direction) {
    Direction.LEFT -> HitTarget.ShiftLeft(row)
    Direction.RIGHT -> HitTarget.ShiftRight(row)
    Direction.UP -> HitTarget.ShiftUp(col)
    Direction.DOWN -> HitTarget.ShiftDown(col)
}

/** The one [TileSelection] a session's keyboard modes share, made by `buildGameSession` so a
 *  switch to mouse mode and back keeps it. */
class KeyboardSelection {
    var current: TileSelection = TileSelection.INITIAL
}
