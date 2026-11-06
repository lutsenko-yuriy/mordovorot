package web.storage

/** A [KeyValueStore] in a map: localStorage for the tests. [removeException] makes [remove]
 *  fail, as a browser that blocks site data does (GH-251). */
internal class InMemoryKeyValueStore(val entries: MutableMap<String, String> = mutableMapOf()) : KeyValueStore {
    var removeException: Throwable? = null

    override fun keys(): List<String> = entries.keys.toList()
    override fun get(key: String): String? = entries[key]
    override fun set(key: String, value: String) {
        entries[key] = value
    }
    override fun remove(key: String) {
        removeException?.let { throw it }
        entries.remove(key)
    }
}
