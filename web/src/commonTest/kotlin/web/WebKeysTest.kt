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

    private fun key(press: KeyPress, computer: Boolean = true) = WebKeys.pageKey(press, computer)

    @Test
    fun `on a computer 1 to 9 press the toolbar`() {
        val digits = ToolbarButton.entries.map { key(KeyPress(it.digit.toString())) }
        assertEquals(ToolbarButton.entries.map { PageKey.Digit(it) }, digits)
    }

    @Test
    fun `on touch no digit works`() {
        for (button in ToolbarButton.entries) assertNull(key(KeyPress(button.digit.toString()), computer = false), button.name)
    }

    @Test
    fun `modifier keys, IME composition and a held digit are nothing`() {
        assertNull(key(KeyPress("1", modified = true)))
        assertNull(key(KeyPress("1", repeat = true)))
    }

    @Test
    fun `other keys are nothing`() {
        for (other in listOf("x", "Enter", "Escape", "F1", "!")) assertNull(key(KeyPress(other)))
    }

    @Test
    fun `a button shows its digit exactly when the digit works`() {
        fun shown(computer: Boolean) = ToolbarButton.entries.filter { WebKeys.live(it, computer) }
        assertEquals(ToolbarButton.entries, shown(true))
        assertEquals(emptyList(), shown(false))
    }

    private fun boardKey(key: String, layout: KeyboardLayout = KeyboardLayout.RIGHT_HANDED, repeat: Boolean = false, modified: Boolean = false) =
        (WebKeys.pageKey(KeyPress(key, modified, repeat), true, layout, onBoard = true) as? PageKey.Board)?.key

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
        assertEquals(PageKey.Digit(ToolbarButton.SAVE), WebKeys.pageKey(KeyPress("2"), true, onBoard = true))
        for (other in listOf("x", "Enter", " ", "Tab", "Escape", "F1")) assertNull(boardKey(other))
    }

    @Test
    fun `board keys need the board and a computer`() {
        assertNull(WebKeys.pageKey(KeyPress("w"), true))
        assertNull(WebKeys.pageKey(KeyPress("w"), false, onBoard = true))
    }

    @Test
    fun `0 reads the board on a computer, wherever focus is, once per press`() {
        assertEquals(PageKey.ReadBoard, key(KeyPress("0")))
        assertNull(key(KeyPress("0"), computer = false))
        assertNull(key(KeyPress("0", repeat = true)))
        assertNull(key(KeyPress("0", modified = true)))
    }

    private fun solutionKey(key: String, layout: KeyboardLayout = KeyboardLayout.RIGHT_HANDED, repeat: Boolean = false) =
        (WebKeys.pageKey(KeyPress(key, repeat = repeat), true, layout, onSolution = true) as? PageKey.Solution)?.key

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
    fun `solution keys need the board and a computer`() {
        assertNull(WebKeys.pageKey(KeyPress("r"), true))
        assertNull(WebKeys.pageKey(KeyPress("r"), false, onSolution = true))
    }

    private fun boardKey(key: String, code: String, layout: KeyboardLayout = KeyboardLayout.RIGHT_HANDED, modified: Boolean = false) =
        (WebKeys.pageKey(KeyPress(key, modified, code = code), true, layout, onBoard = true) as? PageKey.Board)?.key

    @Test
    fun `a non-Latin key acts by its position, in either case and layout (GH-200)`() {
        assertEquals(BoardKey.Select(Direction.UP), boardKey("ц", "KeyW"))
        assertEquals(BoardKey.Select(Direction.UP), boardKey("Ц", "KeyW"))
        assertEquals(BoardKey.Shift(Direction.UP, null), boardKey("ц", "KeyW", KeyboardLayout.LEFT_HANDED))
        assertEquals(BoardKey.Select(Direction.UP), boardKey("ω", "KeyW"))
        assertEquals(SolutionKey.RESTART, (WebKeys.pageKey(KeyPress("к", code = "KeyR"), true, onSolution = true) as? PageKey.Solution)?.key)
    }

    @Test
    fun `a combining mark counts as non-Latin, as Thai's H key types one`() {
        assertEquals(BoardKey.Shift(Direction.LEFT, ShiftKey.HJKL), boardKey("้", "KeyH"))
    }

    @Test
    fun `Latin layouts still act by the letter they type`() {
        // AZERTY.
        assertEquals(BoardKey.Select(Direction.LEFT), boardKey("a", "KeyQ"))
        assertNull(boardKey("z", "KeyW"))
        // Dvorak.
        assertNull(boardKey(",", "KeyW"))
        assertEquals(BoardKey.Shift(Direction.LEFT, ShiftKey.HJKL), boardKey("h", "KeyJ"))
        // Latin letters with marks: Turkish F, Bépo, Vietnamese.
        for ((key, code) in listOf("ğ" to "KeyE", "é" to "KeyW", "ạ" to "KeyW")) assertNull(boardKey(key, code), key)
    }

    @Test
    fun `a non-Latin key needs a letter position and no modifier`() {
        assertNull(boardKey("ц", "KeyW", modified = true))
        assertNull(boardKey("ц", ""))
        assertNull(boardKey("ё", "Backquote"))
        assertNull(boardKey("ц", "Key"))
        assertEquals(PageKey.Digit(ToolbarButton.SAVE), WebKeys.pageKey(KeyPress("2", code = "Digit2"), true, onBoard = true))
    }
}
