package web

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement

// The page's building blocks (GH-110), shared by DomRenderer and the dialogs.

internal fun element(tag: String, cssClass: String? = null): Element =
    document.createElement(tag).apply { cssClass?.let { className = it } }

internal fun text(cssClass: String, content: String) = element("div", cssClass).apply { textContent = content }

internal fun button(label: String, cssClass: String? = null, onClick: () -> Unit) =
    (element("button", cssClass) as HTMLButtonElement).apply {
        textContent = label
        addEventListener("click", { onClick() })
    }

/** Shows [digit], the key that presses this button: heard as the shortcut, not in its name (GH-171). */
internal fun <E : Element> E.withDigit(digit: Int?): E = apply {
    if (digit == null) return@apply
    setAttribute("aria-keyshortcuts", digit.toString())
    append(element("span").apply {
        setAttribute("aria-hidden", "true")
        textContent = " $digit"
    })
}
