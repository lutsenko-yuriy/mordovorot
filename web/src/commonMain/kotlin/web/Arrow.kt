package web

import viewmodel.Direction

/** Which arrow was clicked: [LEFT]/[RIGHT] shift a row, [UP]/[DOWN] a column. */
enum class Arrow(val direction: Direction) {
    LEFT(Direction.LEFT), RIGHT(Direction.RIGHT), UP(Direction.UP), DOWN(Direction.DOWN)
}
