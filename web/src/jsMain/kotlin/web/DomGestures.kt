package web

import org.w3c.dom.Document
import org.w3c.dom.Navigator

/** WebKit's pinch events, which iOS Safari and Firefox fire whatever `touch-action` and the viewport say. */
private val pinchEvents = listOf("gesturestart", "gesturechange", "gestureend")

/** GH-149: on touch screens a pinch doesn't zoom the page. Desktop Safari's trackpad pinch still does. */
internal fun stopPinchZoom(document: Document, navigator: Navigator) {
    if ((navigator.asDynamic().maxTouchPoints as? Int ?: 0) == 0) return
    // Not passive, or preventDefault is ignored.
    val options: dynamic = js("({ passive: false })")
    for (type in pinchEvents) document.addEventListener(type, { it.preventDefault() }, options)
}
