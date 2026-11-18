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

    // GH-342: the label wave and the group slide rest in their end state, so the catch-all above makes them change at once.
    @Test
    fun `a label that waves rests in its end state`() {
        val css = sheets["board.css"].orEmpty()

        assertTrue(Regex("""\.label-out \{[^}]*opacity: 0;""").containsMatchIn(css), "the old label is clear at rest")
        assertTrue(Regex("""\.label-in\.wave \{\s*animation-name: label-in;""").containsMatchIn(css), "the animation comes only with .wave")
    }

    @Test
    fun `a group that slides shut rests collapsed and the one that slides open rests open`() {
        val css = sheets["labels.css"].orEmpty()

        assertTrue(Regex("""\.group-body\.leaving \{[^}]*grid-template-rows: 0fr;[^}]*visibility: hidden;""").containsMatchIn(css))
        assertTrue(Regex("""\.group-body \{[^}]*grid-template-rows: 1fr;""").containsMatchIn(css))
    }
}
