package web.storage

/** A [KeyValueStore] in a map: localStorage for the tests. */
internal class InMemoryKeyValueStore(val entries: MutableMap<String, String> = mutableMapOf()) : KeyValueStore {
    override fun keys(): List<String> = entries.keys.toList()
    override fun get(key: String): String? = entries[key]
    override fun set(key: String, value: String) {
        entries[key] = value
    }
}
