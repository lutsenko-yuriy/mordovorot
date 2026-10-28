package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** GH-168: the page's text follows the system text size, and with larger text, rows of buttons wrap
 *  rather than run past the window's side. */
class TextSizeStylesTest {
    private val styles = File("src/jsMain/resources/styles.css").readText()
    private val solution = File("src/jsMain/resources/solution.css").readText()
    private val dialogs = File("src/jsMain/resources/dialogs.css").readText()
    private val fit = File("src/jsMain/resources/fit.css").readText()
    private val board = File("src/jsMain/resources/board.css").readText()

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

    /** Grown with the text, the columns beside the board would leave it less than 16px tiles. A classic
     *  scrollbar, once the page scrolls, comes out of 100vw too. The steps, whose lines never wrap, keep
     *  their width up to half the room; the panel, whose toolbar wraps, gives way first. */
    @Test
    fun `wide, the columns beside the board give way to its 16px tiles`() {
        val wide = rule(block("@media (orientation: landscape)"), ".app")
        val narrowWide = rule(block("@media (orientation: landscape) and (max-width: 760px)", fit), ".app")
        val steps = rule(block("@media (orientation: landscape)", solution), ".app.solution")
        val narrowSteps = rule(block("@media (orientation: landscape) and (max-width: 760px)", solution), ".app.solution")

        assertTrue("--board-min: calc(16px * (var(--side) * 1.1 + var(--band)));" in rule(styles, ".app"), rule(styles, ".app"))
        assertTrue("--room: calc(100vw - 20px - var(--board-min) - 2 * var(--pad-x));" in wide, wide)
        assertTrue("--panel-max: 21rem;" in wide && "--panel-max: 18rem;" in narrowWide, wide + narrowWide)
        assertTrue("--panel: min(var(--panel-max), var(--room) - var(--column-gap));" in wide, wide)
        assertTrue("--panel: min(var(--panel-max), var(--room) - 2 * var(--column-gap) - var(--steps));" in steps, steps)
        assertTrue("--steps: min(11rem, (var(--room) - 2 * var(--column-gap)) / 2);" in steps, steps)
        assertTrue("--steps: min(9.5rem, (var(--room) - 2 * var(--column-gap)) / 2);" in narrowSteps, narrowSteps)
        // The widest panel is named once per width, so the solution screen's narrow block only resizes the steps.
        assertTrue("--panel" !in narrowSteps && "--panel:" !in narrowWide, narrowSteps + narrowWide)
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

    /** In rem, so it follows the root size DomTextSize sets; 16px at the standard size, as before. */
    @Test
    fun `the page's text is 1rem`() {
        assertTrue("font: 1rem/1.4 system-ui, -apple-system, \"Segoe UI\", Roboto, sans-serif;" in rule(styles, "body"), rule(styles, "body"))
    }

    /** A px size ignores the system text size. Tile labels and arrows follow the tile instead, and
     *  the save name field's 16px floor keeps iOS from zooming into it (GH-149). */
    @Test
    fun `no text is sized in bare px`() {
        val sheets = File("src/jsMain/resources").listFiles { f -> f.extension == "css" }.orEmpty()
        val px = sheets.flatMap { sheet ->
            Regex("""font(?:-size)?:[^;]*\d+px[^;]*;""").findAll(sheet.readText()).map { "${sheet.name}: ${it.value}" }
        }

        assertTrue(px == listOf("dialogs.css: font-size: max(16px, 1em);"), px.toString())
    }

    /** Grown with the text, up to the size at which a two-character label still fits its tile. At
     *  the standard size the first term wins, as before. */
    @Test
    fun `tile labels grow with the text, up to their tile`() {
        val tile = rule(board, ".tile")

        assertTrue("font-size: calc(var(--tile) * min(0.4 * var(--text-scale, 1), 0.6));" in tile, tile)
    }
}
