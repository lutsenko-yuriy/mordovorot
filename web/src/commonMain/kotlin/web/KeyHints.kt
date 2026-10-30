package web

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

    /** The solution screen's, in the same form; the same in both layouts. */
    val SOLUTION = listOf(KeyHint("← → A D H L", "prev/next"), KeyHint("Space Enter", "next"), KeyHint("R", "restart"), KeyHint("↑ ↓ W S K J", "scroll"))

    /** The Controls dialog's key list. */
    fun keyList(layout: KeyboardLayout) = KeyScheme.boardKeyLines(layout) + TOOLBAR_KEYS + KeyScheme.SOLUTION_KEY_LINES + LATIN_ONLY

    /** Keys are read as the characters they type (D12), so `ц` isn't W. */
    private const val LATIN_ONLY = "Letter keys need a Latin keyboard layout"

    private val TOOLBAR_KEYS = listOf("New 1 · Save 2 · Load 3 · Labels 4", "Give Up 5 · Controls 6", "0 Read board (screen reader)")
}
