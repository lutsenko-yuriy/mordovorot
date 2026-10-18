package web

internal const val ORIGINAL_URL = "https://openprocessing.org/@carykh/580366"
internal const val LOOPOVER_URL = "https://loopover.xyz"

/** A piece of the footer: plain text, or a link that opens in a new tab. */
sealed interface FooterPart {
    val text: String

    data class Text(override val text: String) : FooterPart
    data class Link(override val text: String, val href: String) : FooterPart
}

/** The footer line (GH-111): the app's [version] and the Loopover credit, worded as the README's Credits. */
fun footer(version: String): List<FooterPart> = listOf(
    FooterPart.Text("v$version · Based on Loopover by Cary Huang (carykh) · "),
    FooterPart.Link("original", ORIGINAL_URL),
    FooterPart.Text(" · "),
    FooterPart.Link("loopover.xyz", LOOPOVER_URL),
)
