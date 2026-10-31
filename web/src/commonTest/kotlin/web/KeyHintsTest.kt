package web

import kotlin.test.Test
import kotlin.test.assertEquals
import viewmodel.KeyScheme
import viewmodel.KeyboardLayout

/** GH-171: the web's Controls key list. No Exit here, and 0 reads the board. */
class KeyHintsTest {

    @Test
    fun `the board hints are ordered by hand per layout`() {
        assertEquals(
            listOf(KeyHint("W A S D", "select"), KeyHint("← → H L", "row"), KeyHint("↑ ↓ K J", "column")),
            KeyHints.board(KeyboardLayout.RIGHT_HANDED),
        )
        assertEquals(
            listOf(KeyHint("A D", "row"), KeyHint("W S", "column"), KeyHint("←↑↓→ H J K L", "select")),
            KeyHints.board(KeyboardLayout.LEFT_HANDED),
        )
    }

    @Test
    fun `the solution's hints are its keys, in the same form`() {
        assertEquals(
            listOf(KeyHint("← → A D H L", "prev/next"), KeyHint("Space Enter", "next"), KeyHint("R", "restart"), KeyHint("↑ ↓ W S K J", "scroll")),
            KeyHints.SOLUTION,
        )
    }

    @Test
    fun `the key list follows the layout, then names the toolbar, Read board and the solution keys`() {
        for (layout in KeyboardLayout.entries) {
            assertEquals(
                KeyScheme.boardKeyLines(layout) + listOf("New 1 · Save 2 · Load 3 · Labels 4", "Give Up 5 · Controls 6", "0 Read board (screen reader)") + KeyScheme.SOLUTION_KEY_LINES,
                KeyHints.keyList(layout),
            )
        }
    }
}
