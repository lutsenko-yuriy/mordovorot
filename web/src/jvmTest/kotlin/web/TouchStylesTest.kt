package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** GH-111: where the main input is touch, the arrows go and the board grows into their room. */
class TouchStylesTest {
    private val styles = File("src/jsMain/resources/styles.css").readText()
    private val touch = Regex("""@media \(pointer: coarse\) \{(.*?)\n}""", RegexOption.DOT_MATCHES_ALL)
        .find(styles)?.groupValues?.get(1).orEmpty()

    @Test
    fun `touch screens hide the board's empty corners`() {
        assertTrue(Regex("""\.board > span \{\s*display: none;""").containsMatchIn(touch), touch)
    }

    @Test
    fun `touch screens hide the arrows from sight but not from screen readers`() {
        val arrows = Regex("""\.board > \.arrows \{([^}]*)}""").find(touch)?.groupValues?.get(1).orEmpty()

        assertTrue("position: absolute;" in arrows && "clip-path: inset(50%);" in arrows, arrows)
        assertTrue("display: none" !in arrows, arrows)
    }

    @Test
    fun `touch screens give the arrows' room to the tiles`() {
        assertTrue(Regex("""--band: [0-9.]+;""").containsMatchIn(touch), touch)
        assertTrue(Regex("""--lead: 0px;""").containsMatchIn(touch), touch)
        assertTrue(styles.split("var(--side) * 1.1 + var(--band)").size == 5, "both --tile budgets use --band")
    }
}
