package web

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.asList
import org.w3c.dom.events.KeyboardEvent

// GH-163: the dialogs from the keyboard. WebGame decides where focus goes; this applies it.
// GH-164: across every other redraw, the focused control keeps focus or passes it on.

/** Escape cancels the open dialog, Tab and Shift+Tab cycle inside it, and a held Enter presses
 *  once. The first key shows focus rings from then on (styles.css). [game] is read only on a key,
 *  as in [replayButtons]. */
internal fun listenForKeys(root: Element, game: () -> WebGame) {
    document.addEventListener("keydown", { event ->
        val key = event as KeyboardEvent
        root.classList.add("keyboard")
        // A held Enter presses the focused button once: it keeps focus now, so each repeat would
        // press it again (GH-164).
        if (key.key == "Enter" && key.repeat) {
            key.preventDefault()
            return@addEventListener
        }
        val dialog = root.querySelector(".dialog") ?: return@addEventListener
        when {
            // An Escape that drops an IME composition isn't the player's, and a held one would
            // cancel the next dialog too (Restore -> the size picker).
            key.key == "Escape" && !key.composing && !key.repeat -> {
                key.preventDefault()
                game().onEscape()
            }
            key.key == "Tab" -> wrapTab(dialog, key)
            else -> moveMark(key)
        }
    })
}

/** Tab moves through the dialog's controls, wrapping at its ends, Shift+Tab the other way; from
 *  outside it, in. Always by hand: Safari's own Tab skips buttons unless the system's Keyboard
 *  navigation is on, and would leave the dialog for the address bar (GH-171). */
private fun wrapTab(dialog: Element, key: KeyboardEvent) {
    val controls = dialog.querySelectorAll("button:not([disabled]):not([tabindex='-1']), input").asList().filterIsInstance<HTMLElement>()
    if (controls.isEmpty()) return
    val from = controls.indexOf(document.activeElement)
    val target = when {
        from < 0 -> if (key.shiftKey) controls.last() else controls.first()
        key.shiftKey -> controls[(from - 1 + controls.size) % controls.size]
        else -> controls[(from + 1) % controls.size]
    }
    key.preventDefault()
    target.focus()
}

/** Inside a group of radios the arrows press the next or previous one, wrapping (GH-171). */
private fun moveMark(key: KeyboardEvent) {
    val step = when (key.key) {
        "ArrowDown", "ArrowRight" -> 1
        "ArrowUp", "ArrowLeft" -> -1
        else -> return
    }
    val radio = document.activeElement as? HTMLElement ?: return
    val options = radio.closest(".radios")?.querySelectorAll("[role=radio]")?.asList()?.filterIsInstance<HTMLElement>() ?: return
    key.preventDefault()
    options[(options.indexOf(radio) + step + options.size) % options.size].click()
}

/** The focused control's key, read before [root] is rebuilt. */
internal fun focusedKey(root: Element): String? =
    document.activeElement?.takeIf { root.contains(it) }?.getAttribute("data-focus")

/** Focuses [state]'s focus in the rebuilt [root]; with none, or [Focus.Restore], puts back the
 *  control that had focus, [before] or the restored one, or the one [fallbackFocus] names when
 *  it's gone or disabled (GH-164). A control that isn't on the page leaves focus at the page's start. */
internal fun applyFocus(root: Element, state: WebState, before: String?) {
    val focus = state.focus
    if (focus != null && focus !is Focus.Restore) {
        val control = root.querySelector("[data-focus=\"${focus.key}\"]") as? HTMLElement
        if (control != null) control.focus() else blurFocused()
        return
    }
    val key = focus?.key ?: before ?: return
    // Without preventScroll a redraw could scroll the page to the restored control.
    val control = focusable(root, key) ?: focusable(root, fallbackFocus(key, state).key)
    if (control != null) control.asDynamic().focus(js("({ preventScroll: true })")) else blurFocused()
}

private fun focusable(root: Element, key: String): HTMLElement? =
    (root.querySelector("[data-focus=\"$key\"]") as? HTMLElement)?.takeUnless { (it as? HTMLButtonElement)?.disabled == true }

private fun blurFocused() = (document.activeElement as? HTMLElement)?.blur()

/** While a dialog is open, the page behind it, the footer included, is out of reach. */
internal fun setBehindDialog(root: Element, dialogOpen: Boolean) {
    val behind = root.children.asList().filter { !it.classList.contains("overlay") } + listOfNotNull(document.querySelector("footer"))
    behind.forEach { if (dialogOpen) it.setAttribute("inert", "") else it.removeAttribute("inert") }
}

/** Part of an IME composition: Safari ends it before the key's keydown, which then has only keyCode 229. */
internal val KeyboardEvent.composing: Boolean get() = isComposing || keyCode == 229

internal fun <E : Element> E.focusKey(focus: Focus): E = apply { setAttribute("data-focus", focus.key) }
