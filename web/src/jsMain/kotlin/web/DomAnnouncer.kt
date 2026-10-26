package web

import kotlinx.browser.window
import org.w3c.dom.Element

/** Speaks through the page's polite live region, `#announcer` (GH-166). It empties the region and
 *  sets the text a moment later, so a reader says the same text again; a newer text replaces one
 *  still waiting. */
class DomAnnouncer(private val region: Element) {
    private var pending: Int? = null

    fun say(text: String) {
        pending?.let(window::clearTimeout)
        region.textContent = ""
        pending = window.setTimeout({
            pending = null
            region.textContent = text
        }, DELAY_MS)
    }

    private companion object {
        const val DELAY_MS = 100
    }
}
