package web.storage

/** String entries by key: the page's localStorage, a map in tests. */
interface KeyValueStore {
    fun keys(): List<String>
    fun get(key: String): String?
    fun set(key: String, value: String)
}
