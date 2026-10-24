package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** GH-147, GH-148: tall or wide, the whole game fits the window and nothing scrolls; the tiles shrink instead. */
class PhoneFitStylesTest {
    private val styles = File("src/jsMain/resources/styles.css").readText()
    private val board = File("src/jsMain/resources/board.css").readText()
    private val solution = File("src/jsMain/resources/solution.css").readText()

    /** Every top-level block in [css] that opens with [opening], up to its closing brace, joined. */
    private fun block(opening: String, css: String = styles): String =
        Regex("""(?m)^${Regex.escape(opening)} \{(.*?)\n}""", RegexOption.DOT_MATCHES_ALL)
            .findAll(css).joinToString("") { it.groupValues[1] }

    /** Every declaration block for exactly [selector] inside [css], joined. */
    private fun rule(css: String, selector: String): String =
        Regex("""(?m)^\s*${Regex.escape(selector)} \{([^}]*)}""").findAll(css).joinToString("") { it.groupValues[1] }

    /** The tall layout's fit, for browsers with size container queries; others keep the estimate. */
    private val fit = block("@supports (container-type: size)")
    private val solutionFit = block("@supports (container-type: size)", solution)
    private val narrow = block("@media (orientation: portrait) and (max-width: 480px)")

    /** The wide layout's rules (GH-148). */
    private val wide = block("@media (orientation: landscape)")
    private val solutionWide = block("@media (orientation: landscape)", solution)

    @Test
    fun `the size-query fit is for the tall layout`() {
        assertTrue("@media (orientation: portrait) {" in fit, fit)
    }

    @Test
    fun `the page is exactly the window's height`() {
        val body = rule(fit, "body")

        assertTrue("height: var(--vh);" in body && "minmax(0, 1fr)" in body, body)
    }

    @Test
    fun `the board's row takes the height the rest leaves`() {
        val app = rule(fit, ".app")

        assertTrue("minmax(0, 1fr)" in app && "min-height: 0;" in app, app)
        assertTrue("minmax(0, 1fr)" in rule(solutionFit, ".app.solution"), solutionFit)
    }

    /** GH-153: it overrides the base .steps at the same specificity, so it must come after it, in solution.css. */
    @Test
    fun `fewer steps show on short phones`() {
        val steps = solution.substringBefore("@supports")

        assertTrue("max-height: calc(6 * 1.9em);" in rule(steps, ".steps"), steps)
        assertTrue("max-height: clamp(" in rule(solutionFit, ".steps"), solutionFit)
    }

    @Test
    fun `the tiles are sized from the stage they sit in`() {
        val stage = rule(fit, ".stage")

        assertTrue("container-type: size;" in stage, stage)
        assertTrue(Regex("""--tile: clamp\(16px, min\(100cqw, 100cqh\)""").containsMatchIn(stage), stage)
        // Each refers to --tile, so it must be declared again where --tile changes.
        for (name in listOf("--gap", "--pitch", "--arrow")) assertTrue("$name:" in stage, "$name in $stage")
    }

    @Test
    fun `the board sits in the stage everywhere`() {
        assertTrue("grid-area: board;" in rule(styles, ".stage"), rule(styles, ".stage"))
        assertTrue("grid-area" !in rule(board, ".board"), rule(board, ".board"))
    }

    @Test
    fun `the page doesn't bounce or pull to refresh`() {
        assertTrue("overscroll-behavior: none;" in rule(fit, "html"), rule(fit, "html"))
    }

    @Test
    fun `the toolbar stays on one row`() {
        assertTrue("flex-wrap: nowrap;" in rule(styles, ".toolbar"), rule(styles, ".toolbar"))
    }

    @Test
    fun `under 300px the toolbar wraps rather than run past the window's edge`() {
        val tiny = block("@media (max-width: 300px)")

        assertTrue("flex-wrap: wrap;" in rule(tiny, ".toolbar"), tiny)
    }

    @Test
    fun `narrow tall windows show the footer's short line`() {
        assertTrue("display: none;" in rule(narrow, ".footer .wide-only"), narrow)
        assertTrue("white-space: nowrap;" in rule(narrow, ".footer"), narrow)
    }

    @Test
    fun `wide, the page is exactly the window's height`() {
        val body = rule(wide, "body")

        assertTrue("height: var(--vh);" in body && "minmax(0, 1fr)" in body, body)
        assertTrue("min-height: 0;" in rule(wide, ".app"), rule(wide, ".app"))
        assertTrue("overscroll-behavior: none;" in rule(wide, "html"), rule(wide, "html"))
    }

    @Test
    fun `wide, the tiles take the room beside the panel and above the footer`() {
        val app = rule(wide, ".app")

        assertTrue("--tile: clamp(16px, min(100vw - var(--beside), var(--vh) - var(--footer) - 2 * var(--pad-y))" in app, app)
    }

    @Test
    fun `wide, the room beside the board is the sum of named sizes`() {
        assertTrue(
            "--beside: calc(var(--panel) + var(--column-gap) + 2 * var(--pad-x));" in rule(wide, ".app"),
            rule(wide, ".app"),
        )
        assertTrue("grid-template-columns: auto var(--steps) var(--panel);" in rule(solutionWide, ".app.solution"), solutionWide)
        for (css in listOf(styles, solution)) {
            assertTrue(!Regex("""--beside: \d+px""").containsMatchIn(css), "no bare px --beside")
        }
    }

    @Test
    fun `wide, the footer is one line of fixed height`() {
        val footer = rule(wide, ".footer")

        assertTrue("height: var(--footer);" in footer && "white-space: nowrap;" in footer, footer)
    }
}
