package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import viewmodel.BoardKey
import viewmodel.KeyboardLayout
import viewmodel.KeyboardLayout.LEFT_HANDED
import viewmodel.KeyboardLayout.RIGHT_HANDED
import viewmodel.ShiftKey

/** GH-170's two key layouts (ADR-0003): what each board key does, and the texts naming them. */
class BoardKeysTest {

    private fun key(char: Char, layout: KeyboardLayout) = BoardKeys.boardKey(TerminalEvent.KeyPress(char), layout)
    private fun arrow(direction: Direction, layout: KeyboardLayout) = BoardKeys.boardKey(TerminalEvent.Arrow(direction), layout)

    @Test
    fun `right-handed - WASD select, HJKL and the arrows shift`() {
        assertEquals(BoardKey.Select(Direction.UP), key('w', RIGHT_HANDED))
        assertEquals(BoardKey.Select(Direction.LEFT), key('A', RIGHT_HANDED))
        assertEquals(BoardKey.Shift(Direction.DOWN, ShiftKey.HJKL), key('j', RIGHT_HANDED))
        assertEquals(BoardKey.Shift(Direction.RIGHT, ShiftKey.HJKL), key('L', RIGHT_HANDED))
        assertEquals(BoardKey.Shift(Direction.UP, ShiftKey.ARROW), arrow(Direction.UP, RIGHT_HANDED))
    }

    @Test
    fun `left-handed - HJKL and the arrows select, WASD shift with no shift key`() {
        assertEquals(BoardKey.Select(Direction.LEFT), key('h', LEFT_HANDED))
        assertEquals(BoardKey.Select(Direction.DOWN), key('J', LEFT_HANDED))
        assertEquals(BoardKey.Select(Direction.RIGHT), arrow(Direction.RIGHT, LEFT_HANDED))
        assertEquals(BoardKey.Shift(Direction.RIGHT, null), key('d', LEFT_HANDED))
        assertEquals(BoardKey.Shift(Direction.UP, null), key('W', LEFT_HANDED))
    }

    @Test
    fun `other keys do nothing in either layout`() {
        for (layout in KeyboardLayout.entries) {
            assertNull(key('x', layout))
            assertNull(BoardKeys.boardKey(TerminalEvent.Enter, layout))
        }
    }

    @Test
    fun `the side hints put each hand's keys on its side`() {
        assertEquals(listOf("W/A/S/D select"), BoardKeys.leftHint(RIGHT_HANDED))
        assertEquals(listOf("←/→ H/L row", "↑/↓ K/J column"), BoardKeys.rightHint(RIGHT_HANDED))
        assertEquals(listOf("A/D row", "W/S column"), BoardKeys.leftHint(LEFT_HANDED))
        assertEquals(listOf("←↑↓→ H/J/K/L select"), BoardKeys.rightHint(LEFT_HANDED))
    }

    @Test
    fun `the key list names the layout's keys and fits the Controls dialog`() {
        assertEquals(
            listOf("Select tile    W A S D", "Shift row      ← →  H L", "Shift column   ↑ ↓  K J"),
            BoardKeys.keyList(RIGHT_HANDED).take(3),
        )
        assertEquals(
            listOf("Select tile    ← ↑ ↓ →  H J K L", "Shift row      A D", "Shift column   W S"),
            BoardKeys.keyList(LEFT_HANDED).take(3),
        )
        assertEquals(BoardKeys.keyList(RIGHT_HANDED).drop(3), BoardKeys.keyList(LEFT_HANDED).drop(3))
        for (layout in KeyboardLayout.entries) assertTrue(BoardKeys.keyList(layout).all { it.length <= 44 })
    }

    /** Each key's Ukrainian and Russian letter: the two differ only at S. Named by code point, as
     *  Cyrillic а, о, р and с look like Latin letters. */
    private val cyrillic = mapOf(
        'w' to "ц", 'a' to "ф", 's' to "іы", 'd' to "в",
        'h' to "р", 'j' to "о", 'k' to "л", 'l' to "д",
    )

    @Test
    fun `a Ukrainian or Russian letter does what its Latin twin does, in either case and layout (GH-200)`() {
        for (layout in KeyboardLayout.entries) for ((latin, letters) in cyrillic) for (letter in letters) {
            assertEquals(key(latin, layout), key(letter, layout), "$letter")
            assertEquals(key(latin, layout), key(letter.uppercaseChar(), layout), "${letter.uppercaseChar()}")
        }
    }

    @Test
    fun `a Cyrillic letter steps the solution screen as its twin, and к restarts`() {
        for ((latin, letters) in cyrillic) for (letter in letters) {
            assertEquals(BoardKeys.solutionAction(TerminalEvent.KeyPress(latin)), BoardKeys.solutionAction(TerminalEvent.KeyPress(letter)), "$letter")
        }
        assertEquals(InputAction.Activate(HitTarget.ReplayRestart), BoardKeys.solutionAction(TerminalEvent.KeyPress('к')))
    }

    @Test
    fun `other letters do nothing`() {
        for (other in listOf('й', 'ω')) {
            for (layout in KeyboardLayout.entries) assertNull(key(other, layout))
            assertEquals(InputAction.None, BoardKeys.solutionAction(TerminalEvent.KeyPress(other)))
        }
    }

    @Test
    fun `the key list ends by naming the layouts it knows`() {
        for (layout in KeyboardLayout.entries) assertEquals("Letters: Latin, Ukrainian or Russian layout", BoardKeys.keyList(layout).last())
    }
}
