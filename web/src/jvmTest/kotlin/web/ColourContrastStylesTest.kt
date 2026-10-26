package web

import java.io.File
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** GH-165: every text and tile colour pair meets WCAG AA in light and dark: 4.5:1 for text (tile
 *  labels included, as they're under the large-text size on phones and 6x6), 3:1 for non-text. */
class ColourContrastStylesTest {
    private fun sheet(name: String) = File("src/jsMain/resources/$name").readText()

    private val palette = sheet("palette.css")
    private val dialogs = sheet("dialogs.css")

    private fun tokens(block: String) =
        Regex("""--([\w-]+):\s*([^;]+);""").findAll(block).associate { it.groupValues[1] to it.groupValues[2].trim() }

    private val light = tokens(palette.substringBefore("@media"))
    private val dark = light + tokens(palette.substringAfter("@media (prefers-color-scheme: dark)"))
    private val themes = mapOf("light" to light, "dark" to dark)

    /** The colour token a rule's [property] takes, e.g. `muted` for `color: var(--muted);`. */
    private fun tokenIn(css: String, selector: String, property: String) =
        Regex("""(?m)^${Regex.escape(selector)} \{([^}]*)}""").find(css)?.groupValues?.get(1)
            ?.let { Regex("""(?m)^\s*$property:[^;]*var\(--([\w-]+)\)""").find(it)?.groupValues?.get(1) }

    private fun percent(theme: Map<String, String>, token: String) = theme.getValue(token).removeSuffix("%").toDouble()

    private fun check(fg: String, bg: String, minimum: Double, what: String): List<String> =
        themes.mapNotNull { (name, theme) ->
            val ratio = Wcag.contrast(Wcag.hex(theme.getValue(fg)), Wcag.hex(theme.getValue(bg)))
            if (ratio < minimum) "$name: $what (--$fg on --$bg) is %.2f, under %.1f".format(Locale.ROOT, ratio, minimum) else null
        }

    @Test
    fun `text meets 4_5 to 1`() {
        val hoverSample = tokenIn(dialogs, ".dialog .choices.labels button:hover .sample", "color")
            ?: tokenIn(dialogs, ".dialog .choices.labels .sample", "color")!!
        val failures = listOf(
            check("ink", "page", 4.5, "moves and text"),
            check("ink", "frame", 4.5, "solution steps"),
            check("muted", "page", 4.5, "Mordie's line, status, footer"),
            check("muted", "card", 4.5, "dialog message and label samples"),
            check(hoverSample, "frame", 4.5, "a hovered label row's sample"),
            check("button-ink", "button", 4.5, "buttons and the current solution step"),
            check("button-ink", "button-hover", 4.5, "a hovered button"),
            check("mordie", "page", 4.5, "Mordie's name"),
            check("warning", "card", 4.5, "dialog warnings"),
        ).flatten()

        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `non-text meets 3 to 1`() {
        val inputBorder = tokenIn(dialogs, ".dialog input", "border")!!
        val failures = listOf(
            check("warning", "page", 3.0, "focus ring"),
            check("warning", "frame", 3.0, "focus ring on the board's frame"),
            check("button", "card", 3.0, "the checked label row's ring"),
            check(inputBorder, "card", 3.0, "the Save name field's edge"),
        ).flatten()

        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `board's tile colour is the formula this test checks`() {
        val tile = Regex("""(?m)^\.tile \{([^}]*)}""").find(sheet("board.css"))?.groupValues?.get(1).orEmpty()

        assertTrue(
            "background: hsl(calc(var(--row) * 360deg / var(--side) + 8deg) 68% " +
                "calc(var(--tile-light) - var(--col) * var(--tile-ramp) / (var(--side) - 1)));" in tile,
            tile,
        )
        assertEquals("var(--tile-ink)", Regex("""color:\s*([^;]+);""").find(tile)?.groupValues?.get(1))
    }

    @Test
    fun `every tile's label meets 4_5 to 1 on every board`() {
        val failures = themes.flatMap { (name, theme) ->
            val ink = Wcag.hex(theme.getValue("tile-ink"))
            (3..6).flatMap { side ->
                (0 until side).flatMap { row ->
                    (0 until side).mapNotNull { col ->
                        val lightness = percent(theme, "tile-light") - col * percent(theme, "tile-ramp") / (side - 1)
                        val ratio = Wcag.contrast(ink, Wcag.hsl(row * 360.0 / side + 8, 68.0, lightness))
                        if (ratio < 4.5) "$name: ${side}x$side row $row col $col is %.2f".format(Locale.ROOT, ratio) else null
                    }
                }
            }
        }

        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}
