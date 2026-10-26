package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** GH-166: the live region is hidden from sight only. `display: none` or `visibility: hidden` would
 *  hide it from screen readers too, and they'd say nothing. */
class AnnouncerStylesTest {
    private val rule = Regex("""(?m)^\.visually-hidden \{([^}]*)}""")
        .find(File("src/jsMain/resources/styles.css").readText())?.groupValues?.get(1).orEmpty()

    @Test
    fun `the live region is clipped out of sight`() {
        assertTrue("position: absolute;" in rule && "clip-path: inset(50%);" in rule, rule)
    }

    @Test
    fun `the live region is never hidden from screen readers`() {
        assertTrue(!Regex("""display:\s*none|visibility:\s*hidden""").containsMatchIn(rule), rule)
    }
}
