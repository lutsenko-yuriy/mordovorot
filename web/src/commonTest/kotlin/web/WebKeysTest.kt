package web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import viewmodel.BoardKey
import viewmodel.Direction
import viewmodel.KeyboardLayout
import viewmodel.ShiftKey
import viewmodel.SolutionKey

/** GH-171: which digit presses which toolbar button, and the board's keys (ADR-0003). */
class WebKeysTest {

    private fun key(press: KeyPress, mode: PlayMode = PlayMode.KEYBOARD, controlsShown: Boolean = true) =
        WebKeys.pageKey(press, mode, controlsShown)

    @Test
    fun `keyboard mode - 1 to 6 press the toolbar`() {
        val digits = ToolbarButton.entries.map { key(KeyPress(it.digit.toString())) }
        assertEquals(ToolbarButton.entries.map { PageKey.Digit(it) }, digits)
    }

    @Test
    fun `mouse mode - only 6`() {
        for (button in ToolbarButton.entries - ToolbarButton.CONTROLS) assertNull(key(KeyPress(button.digit.toString()), PlayMode.MOUSE))
        assertEquals(PageKey.Digit(ToolbarButton.CONTROLS), key(KeyPress("6"), PlayMode.MOUSE))
    }

    @Test
    fun `with Controls hidden 6 is nothing`() {
        assertNull(key(KeyPress("6"), PlayMode.MOUSE, controlsShown = false))
    }

    @Test
    fun `modifier keys, IME composition and a held digit are nothing`() {
        assertNull(key(KeyPress("1", modified = true)))
        assertNull(key(KeyPress("1", repeat = true)))
    }

    @Test
    fun `other keys are nothing`() {
        for (other in listOf("7", "x", "Enter", "Escape", "F1", "!")) assertNull(key(KeyPress(other)))
    }

    @Test
    fun `a button shows its digit exactly when the digit works`() {
        fun shown(mode: PlayMode, controlsShown: Boolean) = ToolbarButton.entries.filter { WebKeys.live(it, mode, controlsShown) }
        assertEquals(ToolbarButton.entries, shown(PlayMode.KEYBOARD, true))
        assertEquals(listOf(ToolbarButton.CONTROLS), shown(PlayMode.MOUSE, true))
        assertEquals(emptyList(), shown(PlayMode.MOUSE, false))
    }

    private fun boardKey(key: String, layout: KeyboardLayout = KeyboardLayout.RIGHT_HANDED, repeat: Boolean = false, modified: Boolean = false) =
        (WebKeys.pageKey(KeyPress(key, modified, repeat), PlayMode.KEYBOARD, true, layout, onBoard = true) as? PageKey.Board)?.key

    @Test
    fun `on the board, a right-handed key selects or shifts`() {
        assertEquals(BoardKey.Select(Direction.UP), boardKey("w"))
        assertEquals(BoardKey.Select(Direction.RIGHT), boardKey("D"))
        assertEquals(BoardKey.Shift(Direction.LEFT, ShiftKey.HJKL), boardKey("h"))
        assertEquals(BoardKey.Shift(Direction.DOWN, ShiftKey.ARROW), boardKey("ArrowDown"))
    }

    @Test
    fun `on the board, a left-handed key selects or shifts, with no key to report`() {
        assertEquals(BoardKey.Select(Direction.LEFT), boardKey("ArrowLeft", KeyboardLayout.LEFT_HANDED))
        assertEquals(BoardKey.Select(Direction.UP), boardKey("K", KeyboardLayout.LEFT_HANDED))
        assertEquals(BoardKey.Shift(Direction.UP, null), boardKey("w", KeyboardLayout.LEFT_HANDED))
    }

    @Test
    fun `on the board, a held key repeats but a modified one is nothing`() {
        assertEquals(BoardKey.Select(Direction.UP), boardKey("w", repeat = true))
        assertNull(boardKey("w", modified = true))
    }

    @Test
    fun `on the board, digits still press the toolbar and other keys are nothing`() {
        assertEquals(PageKey.Digit(ToolbarButton.SAVE), WebKeys.pageKey(KeyPress("2"), PlayMode.KEYBOARD, true, onBoard = true))
        for (other in listOf("x", "Enter", " ", "Tab", "Escape", "F1")) assertNull(boardKey(other))
    }

    @Test
    fun `board keys need the board and keyboard mode`() {
        assertNull(WebKeys.pageKey(KeyPress("w"), PlayMode.KEYBOARD, true))
        assertNull(WebKeys.pageKey(KeyPress("w"), PlayMode.MOUSE, true, onBoard = true))
    }

    @Test
    fun `0 reads the board in keyboard mode, wherever focus is, once per press`() {
        assertEquals(PageKey.ReadBoard, key(KeyPress("0")))
        assertNull(key(KeyPress("0"), PlayMode.MOUSE))
        assertNull(key(KeyPress("0", repeat = true)))
        assertNull(key(KeyPress("0", modified = true)))
    }

    private fun solutionKey(key: String, layout: KeyboardLayout = KeyboardLayout.RIGHT_HANDED, repeat: Boolean = false) =
        (WebKeys.pageKey(KeyPress(key, repeat = repeat), PlayMode.KEYBOARD, true, layout, onSolution = true) as? PageKey.Solution)?.key

    @Test
    fun `on the solution's board, the keys are the same in both layouts`() {
        for (layout in KeyboardLayout.entries) {
            for (next in listOf("ArrowRight", "d", "L", " ", "Enter")) assertEquals(SolutionKey.NEXT, solutionKey(next, layout), next)
            for (prev in listOf("ArrowLeft", "A", "h")) assertEquals(SolutionKey.PREV, solutionKey(prev, layout), prev)
            for (up in listOf("ArrowUp", "w", "K")) assertEquals(SolutionKey.SCROLL_UP, solutionKey(up, layout), up)
            for (down in listOf("ArrowDown", "S", "j")) assertEquals(SolutionKey.SCROLL_DOWN, solutionKey(down, layout), down)
            assertEquals(SolutionKey.RESTART, solutionKey("r", layout))
        }
    }

    @Test
    fun `on the solution's board, a held Space or Enter is one press and a held direction repeats`() {
        assertNull(solutionKey(" ", repeat = true))
        assertNull(solutionKey("Enter", repeat = true))
        assertEquals(SolutionKey.NEXT, solutionKey("l", repeat = true))
    }

    @Test
    fun `solution keys need the board and keyboard mode`() {
        assertNull(WebKeys.pageKey(KeyPress("r"), PlayMode.KEYBOARD, true))
        assertNull(WebKeys.pageKey(KeyPress("r"), PlayMode.MOUSE, true, onSolution = true))
    }
}
