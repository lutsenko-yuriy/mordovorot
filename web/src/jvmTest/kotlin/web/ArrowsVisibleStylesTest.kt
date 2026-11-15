package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** GH-287: the arrows show on a computer next to the key hints; only the solution screen hides them. */
class ArrowsVisibleStylesTest {
    private val sheets = File("src/jsMain/resources").listFiles { f -> f.extension == "css" }.orEmpty().associate { it.name to it.readText() }

    @Test
    fun `no rule hides the arrows outside the solution screen`() {
        for ((name, css) in sheets) {
            for (rule in Regex("""(?m)^\s*([^{}\n]*\.arrows[^{}\n]*) \{([^}]*)}""").findAll(css)) {
                val (selector, body) = rule.destructured
                if ("visibility: hidden" in body) assertTrue(".solution" in selector, "$name: $selector hides the arrows")
            }
        }
    }

    @Test
    fun `the page has a computer class and no keyboard-mode one`() {
        assertTrue(sheets.values.any { ".app.computer" in it })
        assertFalse(sheets.values.any { "keyboard-mode" in it })
    }
}
