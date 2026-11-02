package web

import kotlinx.browser.window

/** The browser's address bar. */
internal object DomPageAddress : PageAddress {
    override fun base() = window.location.origin + window.location.pathname

    override fun fragment() = window.location.hash

    /** `replaceState` neither reloads nor adds a history entry, and fires no `hashchange`. */
    override fun clearFragment() {
        window.history.replaceState(null, "", window.location.pathname + window.location.search)
    }
}
