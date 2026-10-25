package web

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.asList
import org.w3c.dom.events.KeyboardEvent

// GH-163: the dialogs from the keyboard. WebGame decides where focus goes; this applies it.

/** Escape cancels the open dialog, and Tab and Shift+Tab cycle inside it. The first key shows
 *  focus rings from then on (styles.css). [game] is read only on a key, as in [replayButtons]. */
internal fun listenForDialogKeys(root: Element, game: () -> WebGame) {
    document.addEventListener("keydown", { event ->
        val key = event as KeyboardEvent
        root.classList.add("keyboard")
        val dialog = root.querySelector(".dialog") ?: return@addEventListener
        when {
            // An Escape that cancels an IME composition isn't the player's.
            key.key == "Escape" && !key.isComposing -> {
                key.preventDefault()
                game().onEscape()
            }
            key.key == "Tab" -> wrapTab(dialog, key)
        }
    })
}

/** From the dialog's last control Tab goes to its first, Shift+Tab the other way; from outside it, in. */
private fun wrapTab(dialog: Element, key: KeyboardEvent) {
    val controls = dialog.querySelectorAll("button:not([disabled]), input").asList().filterIsInstance<HTMLElement>()
    if (controls.isEmpty()) return
    val active = document.activeElement
    val inside = active != null && dialog.contains(active)
    val target = when {
        key.shiftKey && (!inside || active == controls.first()) -> controls.last()
        !key.shiftKey && (!inside || active == controls.last()) -> controls.first()
        else -> return
    }
    key.preventDefault()
    target.focus()
}

/** The focused control's key, read before [root] is rebuilt. */
internal fun focusedKey(root: Element): String? =
    document.activeElement?.takeIf { root.contains(it) }?.getAttribute("data-focus")

/** Focuses [focus] in the rebuilt [root]; with none, puts back the control that had focus while a
 *  dialog stays open, [before]. A control that isn't on the page leaves focus at the page's start. */
internal fun applyFocus(root: Element, focus: Focus?, before: String?, dialogOpen: Boolean) {
    val key = focus?.key ?: before?.takeIf { dialogOpen } ?: return
    val control = root.querySelector("[data-focus=\"$key\"]") as? HTMLElement
    if (control != null) control.focus() else (document.activeElement as? HTMLElement)?.blur()
}

/** While a dialog is open, the page behind it, the footer included, is out of reach. */
internal fun setBehindDialog(root: Element, dialogOpen: Boolean) {
    val behind = root.children.asList().filter { !it.classList.contains("overlay") } + listOfNotNull(document.querySelector("footer"))
    behind.forEach { if (dialogOpen) it.setAttribute("inert", "") else it.removeAttribute("inert") }
}

internal fun <E : Element> E.focusKey(focus: Focus): E = apply { setAttribute("data-focus", focus.key) }
