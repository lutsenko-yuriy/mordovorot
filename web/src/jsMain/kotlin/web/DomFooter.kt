package web

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.Node
import web.a11y.focusKey

/** The footer line (GH-111) as `<footer>`: its links open in a new tab, keeping the game in this one.
 *  Wide-only parts go in `.wide-only`, which narrow tall windows hide (GH-147). */
internal fun footerElement(parts: List<FooterPart>): Element = element("footer", "footer").apply {
    var links = 0
    for (part in parts) {
        val node: Node = when (part) {
            is FooterPart.Text -> document.createTextNode(part.text)
            is FooterPart.Link -> element("a").apply {
                textContent = part.text
                setAttribute("href", part.href)
                setAttribute("target", "_blank")
                setAttribute("rel", "noopener noreferrer")
                focusKey(Focus.FooterLink(links++))
            }
        }
        append(if (part.wideOnly) element("span", "wide-only").apply { append(node) } else node)
    }
}
