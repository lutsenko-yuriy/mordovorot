package web

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLInputElement

/** The browser's clipboard: the async API where the page may use it (https, localhost), else the old copy
 *  command on the Share dialog's selected link field. */
internal object DomClipboard : Clipboard {
    override fun copy(text: String, done: (Boolean) -> Unit) {
        val api = window.navigator.asDynamic().clipboard
        if (api == undefined) return done(copySelected())
        try {
            (api.writeText(text) as kotlin.js.Promise<Unit>).then({ done(true) }, { done(copySelected()) })
        } catch (e: Throwable) {
            done(copySelected())
        }
    }

    private fun copySelected(): Boolean {
        val field = document.querySelector("[data-focus=${Focus.ShareLinkField.key}]") as? HTMLInputElement ?: return false
        field.focus()
        field.select()
        return try { document.execCommand("copy") } catch (e: Throwable) { false }
    }
}
