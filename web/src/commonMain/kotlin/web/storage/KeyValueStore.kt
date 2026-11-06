package web.storage

/** String entries by key: the page's localStorage, a map in tests. */
interface KeyValueStore {
    fun keys(): List<String>
    fun get(key: String): String?
    fun set(key: String, value: String)

    /** Removes [key]; nothing happens when it isn't there (GH-251). */
    fun remove(key: String)
}
