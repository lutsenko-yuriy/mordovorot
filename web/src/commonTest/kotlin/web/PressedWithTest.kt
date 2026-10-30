package web

import kotlin.test.Test
import kotlin.test.assertEquals

/** GH-171 (D6): the session's last input, as `input_method`. */
class PressedWithTest {

    @Test
    fun `starts as a pointer and follows the last input`() {
        val pressed = PressedWith()
        assertEquals("rich_ui", pressed.inputMethod)
        pressed.key()
        assertEquals("keyboard", pressed.inputMethod)
        pressed.pointer()
        assertEquals("rich_ui", pressed.inputMethod)
    }
}
