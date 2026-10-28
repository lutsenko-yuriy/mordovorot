package web

import org.w3c.dom.Document
import org.w3c.dom.HTMLElement
import org.w3c.dom.Navigator
import org.w3c.dom.Window

/** GH-168: the page's text follows the system text size, read once at load: the root's font size, before
 *  any is set, is the browser's default (Chrome on Android's text scaling too, with the `text-scale`
 *  meta), and on iOS a probe reads Dynamic Type, which the default never shows. Sets the root's font
 *  size, which every rem follows, and `--text-scale`, which tile labels follow. */
internal fun applySystemTextSize(document: Document, window: Window) {
    val root = document.documentElement as? HTMLElement ?: return
    val scale = textScaleOf(pxOf(window.getComputedStyle(root).fontSize), appleBodyPx(document, window))
    root.style.setProperty("--text-scale", scale.toString())
    root.style.fontSize = "${16 * scale}px"
}

/** Dynamic Type's body size, iOS and iPadOS only: macOS Safari knows the same font, at 13px. */
private fun appleBodyPx(document: Document, window: Window): Double? {
    if (!isAppleTouch(window.navigator)) return null
    val probe = document.createElement("span") as HTMLElement
    probe.style.cssText = "position: absolute; visibility: hidden; font: -apple-system-body"
    probe.textContent = "A"
    document.body?.append(probe) ?: return null
    return pxOf(window.getComputedStyle(probe).fontSize).also { probe.remove() }
}

private fun isAppleTouch(navigator: Navigator): Boolean =
    (navigator.asDynamic().maxTouchPoints as? Int ?: 0) > 0 && js("CSS.supports('-webkit-touch-callout', 'none')") == true

private fun pxOf(size: String): Double? = size.removeSuffix("px").toDoubleOrNull()
