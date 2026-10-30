package web

import viewmodel.KeyScheme
import viewmodel.KeyboardLayout

/** One column of the key hints: the [keys] above what they [does]. */
data class KeyHint(val keys: String, val does: String)

/** The web's texts naming the keys (GH-171). */
internal object KeyHints {

    /** The two lines under the board, ordered by hand. */
    fun board(layout: KeyboardLayout): List<KeyHint> = when (layout) {
        KeyboardLayout.RIGHT_HANDED -> listOf(KeyHint("W A S D", "select"), KeyHint("← → H L", "row"), KeyHint("↑ ↓ K J", "column"))
        KeyboardLayout.LEFT_HANDED -> listOf(KeyHint("A D", "row"), KeyHint("W S", "column"), KeyHint("←↑↓→ H J K L", "select"))
    }

    /** The Controls dialog's key list. */
    fun keyList(layout: KeyboardLayout) = KeyScheme.boardKeyLines(layout) + TOOLBAR_KEYS + KeyScheme.SOLUTION_KEY_LINES

    private val TOOLBAR_KEYS = listOf("New 1 · Save 2 · Load 3 · Labels 4", "Give Up 5 · Controls 6", "0 Read board (screen reader)")
}
