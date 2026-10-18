package web.storage

import kotlinx.browser.window

/** A [KeyValueStore] over the page's `localStorage`, which keeps entries per origin. */
class LocalStorageStore : KeyValueStore {

    override fun keys(): List<String> = guarded { (0 until storage.length).mapNotNull { storage.key(it) } }
    override fun get(key: String): String? = guarded { storage.getItem(key) }
    override fun set(key: String, value: String) = guarded { storage.setItem(key, value) }

    private val storage get() = window.localStorage

    /** The browser throws a native error (full quota, blocked site data), which the viewModel's
     *  `catch (e: Exception)` doesn't see, so it is rethrown as an [IllegalStateException]. */
    private inline fun <T> guarded(block: () -> T): T =
        try {
            block()
        } catch (e: Exception) {
            throw e
        } catch (e: Throwable) {
            throw IllegalStateException(e.message ?: "Browser storage is unavailable", e)
        }
}
