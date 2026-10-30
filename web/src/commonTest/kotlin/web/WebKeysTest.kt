package web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** GH-171: which digit presses which toolbar button (ADR-0003). */
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
        for (other in listOf("0", "7", "x", "Enter", "Escape", "F1", "!")) assertNull(key(KeyPress(other)))
    }

    @Test
    fun `a button shows its digit exactly when the digit works`() {
        fun shown(mode: PlayMode, controlsShown: Boolean) = ToolbarButton.entries.filter { WebKeys.live(it, mode, controlsShown) }
        assertEquals(ToolbarButton.entries, shown(PlayMode.KEYBOARD, true))
        assertEquals(listOf(ToolbarButton.CONTROLS), shown(PlayMode.MOUSE, true))
        assertEquals(emptyList(), shown(PlayMode.MOUSE, false))
    }
}
