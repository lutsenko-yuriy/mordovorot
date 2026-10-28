package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** GH-168: with larger text, rows of buttons wrap rather than run past the window's side. */
class TextSizeStylesTest {
    private val styles = File("src/jsMain/resources/styles.css").readText()
    private val solution = File("src/jsMain/resources/solution.css").readText()
    private val dialogs = File("src/jsMain/resources/dialogs.css").readText()
    private val fit = File("src/jsMain/resources/fit.css").readText()

    /** Every top-level block in [css] that opens with [opening], up to its closing brace, joined. */
    private fun block(opening: String, css: String = styles): String =
        Regex("""(?m)^${Regex.escape(opening)} \{(.*?)\n}""", RegexOption.DOT_MATCHES_ALL)
            .findAll(css).joinToString("") { it.groupValues[1] }

    /** Every declaration block for exactly [selector] inside [css], joined. */
    private fun rule(css: String, selector: String): String =
        Regex("""(?m)^\s*${Regex.escape(selector)} \{([^}]*)}""").findAll(css).joinToString("") { it.groupValues[1] }

    @Test
    fun `button rows wrap`() {
        for ((css, selector) in listOf(styles to ".toolbar", solution to ".replay", dialogs to ".dialog .actions")) {
            assertTrue("flex-wrap: wrap;" in rule(css, selector), "$selector: ${rule(css, selector)}")
        }
        assertTrue("nowrap" !in rule(styles, ".toolbar"), rule(styles, ".toolbar"))
    }

    @Test
    fun `tall, the toolbar's rows are centred`() {
        val tall = block("@media (orientation: portrait)")

        assertTrue("justify-content: center;" in rule(tall, ".toolbar"), tall)
    }

    @Test
    fun `narrow tall windows let the footer's short line wrap`() {
        val narrow = rule(block("@media (orientation: portrait) and (max-width: 480px)"), ".footer")

        assertTrue("nowrap" !in narrow, narrow)
    }

    /** Grown with the text, the columns beside the board would leave it less than 16px tiles. */
    @Test
    fun `wide, the columns beside the board give way to its 16px tiles`() {
        val wide = rule(block("@media (orientation: landscape)"), ".app")
        val narrowWide = rule(block("@media (orientation: landscape) and (max-width: 760px)", fit), ".app")
        val steps = rule(block("@media (orientation: landscape)", solution), ".app.solution")
        val narrowSteps = rule(block("@media (orientation: landscape) and (max-width: 760px)", solution), ".app.solution")

        assertTrue("--board-min: calc(16px * (var(--side) * 1.1 + var(--band)));" in rule(styles, ".app"), rule(styles, ".app"))
        assertTrue("--room: calc(100vw - var(--board-min) - 2 * var(--pad-x));" in wide, wide)
        assertTrue("--panel: min(21rem, var(--room) - var(--column-gap));" in wide, wide)
        assertTrue("--panel: min(18rem, var(--room) - var(--column-gap));" in narrowWide, narrowWide)
        assertTrue("--panel: min(21rem, (var(--room) - 2 * var(--column-gap)) * 0.65);" in steps, steps)
        assertTrue("--steps: min(11rem, (var(--room) - 2 * var(--column-gap)) * 0.35);" in steps, steps)
        assertTrue("--panel: min(18rem, (var(--room) - 2 * var(--column-gap)) * 0.65);" in narrowSteps, narrowSteps)
        assertTrue("--steps: min(9.5rem, (var(--room) - 2 * var(--column-gap)) * 0.35);" in narrowSteps, narrowSteps)
    }

    /** A fixed width would set the page's narrowest, however small the window (max-width doesn't). */
    @Test
    fun `tall, the steps are never wider than the window`() {
        val steps = rule(solution, ".steps")

        assertTrue("width: min(14rem, 100%);" in steps && "max-width" !in steps, steps)
    }

    /** One line of its 0.8rem text: 26px at the standard size, as before. */
    @Test
    fun `wide, the footer's line grows with its text`() {
        val wide = rule(block("@media (orientation: landscape)"), ":root")

        assertTrue("--footer: max(26px, 1.625rem);" in wide, wide)
    }
}
