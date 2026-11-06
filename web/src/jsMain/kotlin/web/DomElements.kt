package web

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.events.KeyboardEvent
import web.a11y.hiddenFromReaders
import web.a11y.keyShortcut
import web.a11y.named

// The page's building blocks (GH-110), shared by DomRenderer and the dialogs.

internal fun element(tag: String, cssClass: String? = null): Element =
    document.createElement(tag).apply { cssClass?.let { className = it } }

internal fun text(cssClass: String, content: String) = element("div", cssClass).apply { textContent = content }

internal fun button(label: String, cssClass: String? = null, onClick: () -> Unit) =
    (element("button", cssClass) as HTMLButtonElement).apply {
        textContent = label
        addEventListener("click", { onClick() })
    }

/** A square button showing the inline [svg] icon, named [label] for readers and as a tooltip (GH-251). */
internal fun iconButton(label: String, svg: String, onClick: () -> Unit) =
    (element("button", "icon-button") as HTMLButtonElement).apply {
        title = label
        named(this, label)
        innerHTML = svg
        firstElementChild?.let { hiddenFromReaders(it) }
        addEventListener("click", { onClick() })
    }

/** Shows [digit], the key that presses this button: heard as the shortcut, not in its name (GH-171). */
internal fun <E : Element> E.withDigit(digit: Int?): E = apply {
    if (digit == null) return@apply
    keyShortcut(this, digit.toString())
    append(hiddenFromReaders(element("span").apply { textContent = " $digit" }))
}

/** Part of an IME composition: Safari ends it before the key's keydown, which then has only keyCode 229. */
internal val KeyboardEvent.composing: Boolean get() = isComposing || keyCode == 229
