package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** GH-162: when the system asks for reduced motion, no stylesheet animates anything. */
class ReducedMotionStylesTest {
    private val sheets = File("src/jsMain/resources").listFiles { f -> f.extension == "css" }.orEmpty()
        .associate { it.name to it.readText() }
    private val reduced = Regex("""@media \(prefers-reduced-motion: reduce\) \{(.*?)\n}""", RegexOption.DOT_MATCHES_ALL)
        .find(sheets["styles.css"].orEmpty())?.groupValues?.get(1).orEmpty()

    @Test
    fun `reduced motion stops every animation and transition`() {
        val all = Regex("""\*, \*::before, \*::after \{([^}]*)}""").find(reduced)?.groupValues?.get(1).orEmpty()

        assertTrue("animation: none !important;" in all && "transition: none !important;" in all, reduced)
    }

    @Test
    fun `no animation outranks the reduced-motion rule`() {
        // An !important one elsewhere would win over the rule's catch-all selector.
        val important = Regex("""(animation|transition)[\w-]*:[^;]*!important""")
        sheets.forEach { (name, css) ->
            val outside = if (name == "styles.css") css.replace(reduced, "") else css
            assertTrue(!important.containsMatchIn(outside), "$name: ${important.find(outside)?.value}")
        }
    }
}
