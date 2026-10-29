package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** GH-163: dialogs focus their default control by script, which would draw the focus ring for
 *  mouse users too, so the ring waits for the first key press (DomKeyboard's `keyboard` class). */
class KeyboardFocusStylesTest {
    private val styles = File("src/jsMain/resources/buttons.css").readText()

    private fun rule(selector: String) =
        Regex("""(?m)^${Regex.escape(selector)} \{([^}]*)}""").find(styles)?.groupValues?.get(1).orEmpty()

    @Test
    fun `no focus ring before a key is pressed`() {
        assertTrue("outline: none;" in rule("button:focus-visible"), styles)
    }

    @Test
    fun `the focus ring once a key is pressed`() {
        assertTrue("outline: 3px solid var(--warning);" in rule(".app.keyboard button:focus-visible"), styles)
    }
}
