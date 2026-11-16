package web

import viewmodel.Direction
import viewmodel.KeyScheme
import viewmodel.KeyboardLayout

/** One column of the key hints: the [keys] above what they [does]. */
data class KeyHint(val keys: String, val does: String)

/** The web's texts naming the keys (GH-171). */
internal object KeyHints {

    /** The two lines under the board, ordered by hand; on a solved board too. */
    fun board(layout: KeyboardLayout): List<KeyHint> = when (layout) {
        KeyboardLayout.RIGHT_HANDED -> listOf(KeyHint("W A S D", "select"), KeyHint("← → H L", "row"), KeyHint("↑ ↓ K J", "column"))
        KeyboardLayout.LEFT_HANDED -> listOf(KeyHint("A D", "row"), KeyHint("W S", "column"), KeyHint("←↑↓→ H J K L", "select"))
    }

    /** The key that moves the selection [direction] (GH-290), as the tutorial names it: the letter, then the arrow where it works too. */
    fun selectKeys(direction: Direction, layout: KeyboardLayout) = when (layout) {
        KeyboardLayout.RIGHT_HANDED -> wasd(direction)
        KeyboardLayout.LEFT_HANDED -> hjkl(direction)
    }

    /** The key that shifts the selected row or column [direction] (GH-290), named as [selectKeys]. */
    fun shiftKeys(direction: Direction, layout: KeyboardLayout) = when (layout) {
        KeyboardLayout.RIGHT_HANDED -> hjkl(direction)
        KeyboardLayout.LEFT_HANDED -> wasd(direction)
    }

    private fun wasd(direction: Direction) = when (direction) {
        Direction.UP -> "W"
        Direction.LEFT -> "A"
        Direction.DOWN -> "S"
        Direction.RIGHT -> "D"
    }

    private fun hjkl(direction: Direction) = when (direction) {
        Direction.LEFT -> "H or ←"
        Direction.RIGHT -> "L or →"
        Direction.UP -> "K or ↑"
        Direction.DOWN -> "J or ↓"
    }

    /** The solution screen's, in the same form; the same in both layouts. */
    val SOLUTION = listOf(KeyHint("← → A D H L", "prev/next"), KeyHint("Space Enter", "next"), KeyHint("R", "restart"), KeyHint("↑ ↓ W S K J", "scroll"))

    /** The Controls dialog's key list. */
    fun keyList(layout: KeyboardLayout) = KeyScheme.boardKeyLines(layout) + TOOLBAR_KEYS + KeyScheme.SOLUTION_KEY_LINES

    private val TOOLBAR_KEYS = listOf("New 1 · Save 2 · Load 3 · Labels 4", "Give Up 5 · Controls 6 · Share 7 · Tutorial 8", "0 Read board (screen reader)")
}
