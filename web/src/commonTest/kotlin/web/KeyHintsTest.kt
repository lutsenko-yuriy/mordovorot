package web

import texts.texts
import kotlin.test.Test
import kotlin.test.assertEquals
import viewmodel.BoardKey
import viewmodel.Direction
import viewmodel.KeyScheme
import viewmodel.KeyboardLayout

/** GH-171: the web's Controls key list. No Exit here, and 0 reads the board. */
class KeyHintsTest {
    private val keys = texts().keys


    @Test
    fun `the board hints are ordered by hand per layout`() {
        assertEquals(
            listOf(KeyHint("W A S D", keys.hintSelect), KeyHint("← → H L", keys.hintRow), KeyHint("↑ ↓ K J", keys.hintColumn)),
            KeyHints.board(KeyboardLayout.RIGHT_HANDED),
        )
        assertEquals(
            listOf(KeyHint("A D", keys.hintRow), KeyHint("W S", keys.hintColumn), KeyHint("←↑↓→ H J K L", keys.hintSelect)),
            KeyHints.board(KeyboardLayout.LEFT_HANDED),
        )
    }

    @Test
    fun `the solution's hints are its keys, in the same form`() {
        assertEquals(
            listOf(KeyHint("← → A D H L", keys.hintPrevNext), KeyHint("Space Enter", keys.hintNext), KeyHint("R", keys.hintRestart), KeyHint("↑ ↓ W S K J", keys.hintScroll)),
            KeyHints.SOLUTION,
        )
    }

    @Test
    fun `the key list follows the layout, then names the toolbar, Read board and the solution keys`() {
        for (layout in KeyboardLayout.entries) {
            assertEquals(
                KeyScheme.boardKeyLines(layout) + listOf(keys.toolbarLine1, keys.toolbarLine2, keys.toolbarLine3) + KeyScheme.SOLUTION_KEY_LINES,
                KeyHints.keyList(layout),
            )
        }
    }

    private val arrows = mapOf('←' to Direction.LEFT, '→' to Direction.RIGHT, '↑' to Direction.UP, '↓' to Direction.DOWN)

    /** What the named key does: the first of its ` or ` names, by letter or by arrow. */
    private fun press(name: String, layout: KeyboardLayout): List<BoardKey?> = name.split(" or ").map {
        val c = it.single()
        if (c in arrows) KeyScheme.boardKey(null, arrows.getValue(c), layout) else KeyScheme.boardKey(c, null, layout)
    }

    @Test
    fun `the select and shift names are letter then arrow, per hand (GH-290)`() {
        assertEquals("W", KeyHints.selectKeys(Direction.UP, KeyboardLayout.RIGHT_HANDED))
        assertEquals("D", KeyHints.selectKeys(Direction.RIGHT, KeyboardLayout.RIGHT_HANDED))
        assertEquals("H or ←", KeyHints.shiftKeys(Direction.LEFT, KeyboardLayout.RIGHT_HANDED))
        assertEquals("J or ↓", KeyHints.shiftKeys(Direction.DOWN, KeyboardLayout.RIGHT_HANDED))
        assertEquals("L or →", KeyHints.selectKeys(Direction.RIGHT, KeyboardLayout.LEFT_HANDED))
        assertEquals("K or ↑", KeyHints.selectKeys(Direction.UP, KeyboardLayout.LEFT_HANDED))
        assertEquals("A", KeyHints.shiftKeys(Direction.LEFT, KeyboardLayout.LEFT_HANDED))
        assertEquals("S", KeyHints.shiftKeys(Direction.DOWN, KeyboardLayout.LEFT_HANDED))
    }

    @Test
    fun `every named key does what it is named for`() {
        for (layout in KeyboardLayout.entries) for (direction in Direction.entries) {
            press(KeyHints.selectKeys(direction, layout), layout).forEach {
                assertEquals(BoardKey.Select(direction), it)
            }
            press(KeyHints.shiftKeys(direction, layout), layout).forEach {
                assertEquals(direction, (it as BoardKey.Shift).direction)
            }
        }
    }
}
