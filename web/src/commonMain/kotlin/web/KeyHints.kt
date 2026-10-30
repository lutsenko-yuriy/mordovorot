package web

import viewmodel.KeyScheme
import viewmodel.KeyboardLayout

/** The web's texts naming the keys (GH-171). */
internal object KeyHints {

    /** The Controls dialog's key list. */
    fun keyList(layout: KeyboardLayout) = KeyScheme.boardKeyLines(layout) + TOOLBAR_KEYS + KeyScheme.SOLUTION_KEY_LINES

    private val TOOLBAR_KEYS = listOf("New 1 · Save 2 · Load 3 · Labels 4", "Give Up 5 · Controls 6", "0 Read board (screen reader)")
}
