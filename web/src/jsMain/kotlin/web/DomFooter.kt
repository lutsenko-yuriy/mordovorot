package web

import kotlinx.browser.document
import kotlinx.dom.clear
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import web.a11y.focusKey

/** Fills the page's `<footer>` (GH-111) with [parts]: its links open in a new tab, keeping the game in this one, the
 *  Update button (GH-262) calls [onUpdate] and the Install entry (GH-360) [onInstall], told whether it was clicked or pressed with a key.
 *  Wide-only parts go in `.wide-only`, which narrow tall windows hide (GH-147); a run of credit parts goes in one `.credit`, so a short window
 *  can cut it off with "…" and keep the entry whole. */
internal fun fillFooter(footer: Element, parts: List<FooterPart>, onUpdate: () -> Unit, onInstall: (byPointer: Boolean) -> Unit) {
    footer.clear()
    footer.classList.toggle("install", parts.any { it is FooterPart.InstallLink })
    var links = 0
    var credit: Element? = null
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
            // A click's detail is not 0; Enter or Space on the button fires one with 0.
            is FooterPart.InstallLink -> clickButton(part.text, "install") { onInstall(it.detail != 0) }.focusKey(Focus.InstallLink)
        }
        val placed = if (part.wideOnly) element("span", "wide-only").apply { append(node) } else node
        if (!part.credit) credit = null
        else if (credit == null) credit = element("span", "credit").also { footer.append(it) }
        (credit ?: footer).append(placed)
    }
}

/** Redraws the page's `<footer>` outside [root], which [DomRenderer] keeps rather than rebuilds: `inert` stays on the same node.
 *  A keyboard user on a control the new line no longer has lands on Update, or where [fallbackFocus] says for [state], not back at the page's start. */
internal fun drawFooter(parts: List<FooterPart>, state: WebState, game: WebGame) {
    val footer = document.querySelector("footer") ?: return
    val focused = (document.activeElement as? HTMLElement)?.takeIf { footer.contains(it) }?.getAttribute("data-focus")
    fillFooter(footer, parts, { game.updates.onUpdate() }) { byPointer -> game.install.onInstall(byPointer) }
    if (focused == null) return
    val target = listOf(focused, Focus.UpdateLink.key).firstOrNull { footer.querySelector("[data-focus=\"$it\"]") != null }
        ?: fallbackFocus(Focus.InstallLink.key, state).key
    (document.querySelector("[data-focus=\"$target\"]") as? HTMLElement)?.focus()
}
