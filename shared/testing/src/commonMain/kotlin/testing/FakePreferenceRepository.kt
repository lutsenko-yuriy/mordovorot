package testing

import storage.PreferenceRepository

/** A [PreferenceRepository] test double backed by an in-memory map. */
class FakePreferenceRepository(
    val values: MutableMap<String, String> = mutableMapOf(),
) : PreferenceRepository {

    /** When set, [get] throws this instead of returning - simulates an unreadable store. */
    var getException: Throwable? = null

    /** When set, [set] throws this instead of storing - simulates a read-only store. */
    var setException: Throwable? = null

    override fun get(key: String): String? {
        getException?.let { throw it }
        return values[key]
    }

    override fun set(key: String, value: String) {
        setException?.let { throw it }
        values[key] = value
    }
}
