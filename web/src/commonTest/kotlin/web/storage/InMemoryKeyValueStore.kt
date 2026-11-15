package web.storage

/** A [KeyValueStore] in a map: localStorage for the tests. [removeException] makes [remove]
 *  fail, as a browser that blocks site data does (GH-251), and [setException] does the same for [set] (GH-267). */
internal class InMemoryKeyValueStore(val entries: MutableMap<String, String> = mutableMapOf()) : KeyValueStore {
    var removeException: Throwable? = null
    var setException: Throwable? = null

    override fun keys(): List<String> = entries.keys.toList()
    override fun get(key: String): String? = entries[key]
    override fun set(key: String, value: String) {
        setException?.let { throw it }
        entries[key] = value
    }
    override fun remove(key: String) {
        removeException?.let { throw it }
        entries.remove(key)
    }
}
