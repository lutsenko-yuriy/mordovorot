package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * GH-145: on touch screens a double tap doesn't zoom and a long press doesn't select text.
 * GH-149: nor does a pinch, and the Save field doesn't zoom in when it gets focus.
 */
class TouchGesturesTest {
    private val resources = File("src/jsMain/resources")
    private val styles = File(resources, "styles.css").readText()
    private val dialogs = File(resources, "dialogs.css").readText()
    private val page = File(resources, "index.html").readText()

    /** Every declaration block for exactly [selector] in [css], joined. */
    private fun rule(selector: String, css: String = styles): String =
        Regex("""(?m)^${Regex.escape(selector)} \{([^}]*)}""").findAll(css).joinToString("") { it.groupValues[1] }

    @Test
    fun `neither a pinch nor a double tap zooms the page`() {
        val html = rule("html")

        assertTrue("touch-action: pan-x pan-y;" in html, html)
        assertTrue("manipulation" !in html, html)
    }

    @Test
    fun `the viewport can't be scaled`() {
        val viewport = Regex("""<meta name="viewport" content="([^"]*)">""").find(page)?.groupValues?.get(1).orEmpty()

        assertTrue("width=device-width, initial-scale=1" in viewport, viewport)
        assertTrue("maximum-scale=1" in viewport && "user-scalable=no" in viewport, viewport)
    }

    @Test
    fun `the save field is never under 16px, so iOS doesn't zoom into it`() {
        assertTrue("font: 16px/" in rule("body"), rule("body"))
        assertTrue("font-size: max(16px, 1em);" in rule(".dialog input", dialogs), rule(".dialog input", dialogs))
    }

    @Test
    fun `only tiles take every touch, so dialogs and the step list still scroll`() {
        val sheets = resources.listFiles { f -> f.extension == "css" }.orEmpty()
        val none = Regex("""(?m)^([^{}\n]+) \{[^}]*touch-action: none;""")

        for (sheet in sheets) {
            for (match in none.findAll(sheet.readText())) {
                assertTrue(match.groupValues[1].trim() == ".tile", "${sheet.name}: ${match.groupValues[1]}")
            }
        }
    }

    @Test
    fun `the game's text can't be selected or long-pressed for copying`() {
        val app = rule(".app")

        assertTrue("user-select: none;" in app && "-webkit-user-select: none;" in app, app)
        assertTrue("-webkit-touch-callout: none;" in app, app)
    }

    @Test
    fun `the save dialog's input still takes text`() {
        val input = rule(".dialog input", dialogs)

        assertTrue("user-select: text;" in input && "-webkit-user-select: text;" in input, input)
    }

    @Test
    fun `taps don't flash a highlight`() {
        assertTrue("-webkit-tap-highlight-color: transparent;" in rule("html"), rule("html"))
    }
}
