package web

import kotlinx.browser.document
import kotlinx.dom.clear
import org.w3c.dom.Element
import org.w3c.dom.Node
import web.a11y.focusKey

/** Fills the page's `<footer>` (GH-111) with [parts]: its links open in a new tab, keeping the game in this one, and the
 *  Update button (GH-262) calls [onUpdate]. Wide-only parts go in `.wide-only`, which narrow tall windows hide (GH-147). */
internal fun fillFooter(footer: Element, parts: List<FooterPart>, onUpdate: () -> Unit) {
    footer.clear()
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
            is FooterPart.UpdateLink -> button(part.text, "update") { onUpdate() }.focusKey(Focus.UpdateLink)
        }
        footer.append(if (part.wideOnly) element("span", "wide-only").apply { append(node) } else node)
    }
}
