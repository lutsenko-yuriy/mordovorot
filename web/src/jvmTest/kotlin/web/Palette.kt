package web

import java.io.File

/** GH-357: palette.css's colour tokens as the page shows them, for tests that pin a copy of one. */
object Palette {
    private val sheet = File("src/jsMain/resources/palette.css").readText()

    private fun tokens(block: String) =
        Regex("""--([\w-]+):\s*([^;]+);""").findAll(block).associate { it.groupValues[1] to it.groupValues[2].trim() }

    val light: Map<String, String> = tokens(sheet.substringBefore("@media"))

    /** The dark theme's tokens over the light ones, as the page resolves them. */
    val dark: Map<String, String> = light + tokens(sheet.substringAfter("@media (prefers-color-scheme: dark)"))
}
