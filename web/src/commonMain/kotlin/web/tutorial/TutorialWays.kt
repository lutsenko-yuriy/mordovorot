package web.tutorial

import kotlin.math.abs
import texts.texts
import viewmodel.Direction
import viewmodel.KeyboardLayout
import viewmodel.TileSelection
import web.KeyHints

/** The three lines under a guided step's text on a computer (GH-290): drag, click and keys, for the shift still asked. */
internal object TutorialWays {

    /** The lines for [tiles] more tiles of [line] towards [asked], in the order drag, click, keys. The keys start with the select keys
     *  that take [selection] (if shown) straight to the line, a row needing only up and down, a column only left and right. */
    fun lines(asked: Direction, line: Int, tiles: Int, selection: TileSelection?, layout: KeyboardLayout): List<String> = with(texts().tutorial) {
        listOf(
            when (asked) {
                Direction.LEFT -> dragLeft(tiles)
                Direction.RIGHT -> dragRight(tiles)
                Direction.UP -> dragUp(tiles)
                Direction.DOWN -> dragDown(tiles)
            },
            click(ARROWS.getValue(asked), tiles),
            keys(asked, line, tiles, selection, layout),
        )
    }

    private fun keys(asked: Direction, line: Int, tiles: Int, selection: TileSelection?, layout: KeyboardLayout): String = with(texts().tutorial) {
        val row = asked == Direction.LEFT || asked == Direction.RIGHT
        val shift = KeyHints.shiftKeys(asked, layout)
        if (selection == null) return press(shift, tiles)
        val delta = line - if (row) selection.row else selection.col
        if (delta == 0) return press(shift, tiles)
        val toward = if (row) (if (delta < 0) Direction.UP else Direction.DOWN) else (if (delta < 0) Direction.LEFT else Direction.RIGHT)
        return pressThen(KeyHints.selectKeys(toward, layout), abs(delta), shift, tiles)
    }

    private val ARROWS = mapOf(Direction.LEFT to "◀", Direction.RIGHT to "▶", Direction.UP to "▲", Direction.DOWN to "▼")
}
