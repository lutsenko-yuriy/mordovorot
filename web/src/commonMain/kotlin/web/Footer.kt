package web

internal const val ORIGINAL_URL = "https://openprocessing.org/@carykh/580366"
internal const val LOOPOVER_URL = "https://loopover.xyz"

/** A piece of the footer: plain text, or a link that opens in a new tab. A [wideOnly] part is
 *  left out of the one-line footer on narrow tall windows (GH-147). */
sealed interface FooterPart {
    val text: String
    val wideOnly: Boolean

    data class Text(override val text: String, override val wideOnly: Boolean = false) : FooterPart
    data class Link(override val text: String, val href: String, override val wideOnly: Boolean = false) : FooterPart
}

/** The footer line (GH-111): the app's [version] and the Loopover credit, worded as the README's Credits.
 *  Narrow, it reads `v<version> · Based on Loopover by carykh`. */
fun footer(version: String): List<FooterPart> = listOf(
    FooterPart.Text("v$version · Based on Loopover by "),
    FooterPart.Text("Cary Huang (", wideOnly = true),
    FooterPart.Text("carykh"),
    FooterPart.Text(") · ", wideOnly = true),
    FooterPart.Link("original", ORIGINAL_URL, wideOnly = true),
    FooterPart.Text(" · ", wideOnly = true),
    FooterPart.Link("loopover.xyz", LOOPOVER_URL, wideOnly = true),
)
