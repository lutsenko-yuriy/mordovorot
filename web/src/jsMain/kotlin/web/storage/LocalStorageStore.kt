package web.storage

import kotlinx.browser.window

/** A [KeyValueStore] over the page's `localStorage`, which keeps entries per origin. */
class LocalStorageStore : KeyValueStore {
    private val storage get() = window.localStorage

    override fun keys(): List<String> = (0 until storage.length).mapNotNull { storage.key(it) }
    override fun get(key: String): String? = storage.getItem(key)
    override fun set(key: String, value: String) = storage.setItem(key, value)
}
