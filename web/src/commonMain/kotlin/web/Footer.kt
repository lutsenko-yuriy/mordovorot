package web

import texts.texts

internal const val ORIGINAL_URL = "https://openprocessing.org/@carykh/580366"
internal const val LOOPOVER_URL = "https://loopover.xyz"

/** A piece of the footer: plain text, or a link that opens in a new tab. A [wideOnly] part is
 *  left out of the one-line footer on narrow tall windows (GH-147). The [credit] parts, in a footer with the install entry (GH-360),
 *  are the ones a short window cuts off with "…" so the entry stays whole. */
sealed interface FooterPart {
    val text: String
    val wideOnly: Boolean
    val credit: Boolean get() = false

    data class Text(override val text: String, override val wideOnly: Boolean = false, override val credit: Boolean = false) : FooterPart
    data class Link(override val text: String, val href: String, override val wideOnly: Boolean = false, override val credit: Boolean = false) : FooterPart
    /** The Update control of the New version available notice (GH-262): a button, not a link. */
    data class UpdateLink(override val text: String, override val wideOnly: Boolean = false) : FooterPart
    /** The Install entry (GH-360): a button, not a link. */
    data class InstallLink(override val text: String, override val wideOnly: Boolean = false) : FooterPart
}

/** The footer line (GH-111): the app's [version] and the Loopover credit, worded as the README's Credits.
 *  Narrow, it reads `v<version> · Based on Loopover by carykh`. With a new version [updateReady] (GH-262), the
 *  credit gives way to `v<version> · New version available. Update`, the same wide and narrow. With an [install] entry (GH-360)
 *  the line ends in `Install the game`, and narrow it replaces the whole credit: `v<version> · Install the game`. */
fun footer(version: String, updateReady: Boolean = false, install: InstallMethod? = null): List<FooterPart> = with(texts().page) {
    when {
        updateReady -> listOf(
            FooterPart.Text(footerUpdateNotice(version)),
            FooterPart.UpdateLink(footerUpdate),
        )
        install != null -> listOf(
            FooterPart.Text(footerVersion(version)),
            FooterPart.Text(footerCreditLead, wideOnly = true, credit = true),
            FooterPart.Text("Cary Huang (", wideOnly = true, credit = true),
            FooterPart.Text("carykh", wideOnly = true, credit = true),
            FooterPart.Text(") · ", wideOnly = true, credit = true),
            FooterPart.Link(footerOriginal, ORIGINAL_URL, wideOnly = true, credit = true),
            FooterPart.Text(" · ", wideOnly = true, credit = true),
            FooterPart.Link("loopover.xyz", LOOPOVER_URL, wideOnly = true, credit = true),
            FooterPart.Text(" · ", wideOnly = true),
            FooterPart.InstallLink(footerInstall),
        )
        else -> listOf(
            FooterPart.Text(footerBasedOn(version)),
            FooterPart.Text("Cary Huang (", wideOnly = true),
            FooterPart.Text("carykh"),
            FooterPart.Text(") · ", wideOnly = true),
            FooterPart.Link(footerOriginal, ORIGINAL_URL, wideOnly = true),
            FooterPart.Text(" · ", wideOnly = true),
            FooterPart.Link("loopover.xyz", LOOPOVER_URL, wideOnly = true),
        )
    }
}
