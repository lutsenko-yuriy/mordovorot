package web

import texts.texts
import viewmodel.Direction
import viewmodel.KeyScheme
import viewmodel.KeyboardLayout

/** One column of the key hints: the [keys] above what they [does]. */
data class KeyHint(val keys: String, val does: String)

/** The web's texts naming the keys (GH-171). */
internal object KeyHints {

    /** The two lines under the board, ordered by hand; on a solved board too. */
    fun board(layout: KeyboardLayout): List<KeyHint> = with(texts().keys) {
        when (layout) {
            KeyboardLayout.RIGHT_HANDED -> listOf(KeyHint("W A S D", hintSelect), KeyHint("← → H L", hintRow), KeyHint("↑ ↓ K J", hintColumn))
            KeyboardLayout.LEFT_HANDED -> listOf(KeyHint("A D", hintRow), KeyHint("W S", hintColumn), KeyHint("←↑↓→ H J K L", hintSelect))
        }
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

    private fun hjkl(direction: Direction) = with(texts().keys) {
        when (direction) {
            Direction.LEFT -> letterOrArrow("H", "←")
            Direction.RIGHT -> letterOrArrow("L", "→")
            Direction.UP -> letterOrArrow("K", "↑")
            Direction.DOWN -> letterOrArrow("J", "↓")
        }
    }

    /** The solution screen's, in the same form; the same in both layouts. */
    val SOLUTION: List<KeyHint>
        get() = with(texts().keys) {
            listOf(KeyHint("← → A D H L", hintPrevNext), KeyHint("Space Enter", hintNext), KeyHint("R", hintRestart), KeyHint("↑ ↓ W S K J", hintScroll))
        }

    /** The Controls dialog's key list. */
    fun keyList(layout: KeyboardLayout) = KeyScheme.boardKeyLines(layout) + TOOLBAR_KEYS + KeyScheme.SOLUTION_KEY_LINES

    private val TOOLBAR_KEYS get() = with(texts().keys) { listOf(toolbarLine1, toolbarLine2, toolbarLine3, toolbarLine4) }
}
