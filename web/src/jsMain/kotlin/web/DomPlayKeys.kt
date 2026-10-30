package web

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.events.KeyboardEvent

/** The keys outside dialogs (GH-171): [game] says whether it took each, and a key it took does
 *  nothing else, so the 2 that opens Save isn't typed into its name field. [pressedWith] hears
 *  every press first, in the capture phase, whichever control it lands on. */
internal fun listenForPlayKeys(root: Element, pressedWith: PressedWith, game: () -> WebGame) {
    document.addEventListener("keydown", { pressedWith.key() }, true)
    document.addEventListener("pointerdown", { pressedWith.pointer() }, true)
    document.addEventListener("keydown", { event ->
        if (root.querySelector(".dialog") != null) return@addEventListener
        val key = event as KeyboardEvent
        val press = KeyPress(key.key, key.ctrlKey || key.altKey || key.metaKey || key.composing, key.repeat)
        if (game().onKey(press, focusedKey(root))) key.preventDefault()
    })
}
