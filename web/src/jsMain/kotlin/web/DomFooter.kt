package web

import kotlinx.browser.document
import org.w3c.dom.Element

/** The footer line (GH-111) as `<footer>`: its links open in a new tab, keeping the game in this one. */
internal fun footerElement(parts: List<FooterPart>): Element = element("footer", "footer").apply {
    for (part in parts) {
        when (part) {
            is FooterPart.Text -> append(document.createTextNode(part.text))
            is FooterPart.Link -> append(
                element("a").apply {
                    textContent = part.text
                    setAttribute("href", part.href)
                    setAttribute("target", "_blank")
                    setAttribute("rel", "noopener noreferrer")
                },
            )
        }
    }
}
