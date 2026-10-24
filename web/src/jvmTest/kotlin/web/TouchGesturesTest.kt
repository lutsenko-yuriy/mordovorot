package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** GH-145: on touch screens a double tap doesn't zoom and a long press doesn't select text, but pinch-zoom stays. */
class TouchGesturesTest {
    private val styles = File("src/jsMain/resources/styles.css").readText()
    private val page = File("src/jsMain/resources/index.html").readText()

    /** Every declaration block for exactly [selector], joined. */
    private fun rule(selector: String): String =
        Regex("""(?m)^${Regex.escape(selector)} \{([^}]*)}""").findAll(styles).joinToString("") { it.groupValues[1] }

    @Test
    fun `a double tap doesn't zoom the page`() {
        assertTrue("touch-action: manipulation;" in rule("html"), rule("html"))
    }

    @Test
    fun `pinch-zoom stays allowed`() {
        val viewport = Regex("""<meta name="viewport" content="([^"]*)">""").find(page)?.groupValues?.get(1).orEmpty()

        assertTrue("user-scalable" !in viewport && "maximum-scale" !in viewport, viewport)
    }

    @Test
    fun `the game's text can't be selected or long-pressed for copying`() {
        val app = rule(".app")

        assertTrue("user-select: none;" in app && "-webkit-user-select: none;" in app, app)
        assertTrue("-webkit-touch-callout: none;" in app, app)
    }

    @Test
    fun `the save dialog's input still takes text`() {
        val input = rule(".dialog input")

        assertTrue("user-select: text;" in input && "-webkit-user-select: text;" in input, input)
    }

    @Test
    fun `taps don't flash a highlight`() {
        assertTrue("-webkit-tap-highlight-color: transparent;" in rule("html"), rule("html"))
    }
}
