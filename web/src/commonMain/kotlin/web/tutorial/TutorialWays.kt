package web.tutorial

import kotlin.math.abs
import viewmodel.Direction
import viewmodel.KeyboardLayout
import viewmodel.TileSelection
import web.KeyHints

/** The three lines under a guided step's text on a computer (GH-290): drag, click and keys, for the shift still asked. */
internal object TutorialWays {

    /** The lines for [tiles] more tiles of [line] towards [asked], in the order drag, click, keys. The keys start with the select keys
     *  that take [selection] (if shown) straight to the line, a row needing only up and down, a column only left and right. */
    fun lines(asked: Direction, line: Int, tiles: Int, selection: TileSelection?, layout: KeyboardLayout): List<String> {
        val word = word(asked)
        val arrow = ARROWS.getValue(asked)
        return listOf(
            "Drag it " + (if (tiles == 1) word else "${NUMBERS.getValue(tiles)} tiles $word"),
            "Click its $arrow" + times(tiles),
            keys(asked, line, tiles, selection, layout),
        )
    }

    private fun keys(asked: Direction, line: Int, tiles: Int, selection: TileSelection?, layout: KeyboardLayout): String {
        val row = asked == Direction.LEFT || asked == Direction.RIGHT
        val shift = KeyHints.shiftKeys(asked, layout) + times(tiles)
        if (selection == null) return "Press $shift"
        val delta = line - if (row) selection.row else selection.col
        if (delta == 0) return "Press $shift"
        val toward = if (row) (if (delta < 0) Direction.UP else Direction.DOWN) else (if (delta < 0) Direction.LEFT else Direction.RIGHT)
        return "Press ${KeyHints.selectKeys(toward, layout)}${times(abs(delta))}, then $shift"
    }

    private fun times(count: Int) = when (count) {
        1 -> ""
        2 -> " twice"
        else -> " ${NUMBERS.getValue(count)} times"
    }

    private fun word(direction: Direction) = direction.name.lowercase()

    private val ARROWS = mapOf(Direction.LEFT to "◀", Direction.RIGHT to "▶", Direction.UP to "▲", Direction.DOWN to "▼")
    private val NUMBERS = mapOf(1 to "one", 2 to "two", 3 to "three")
}
