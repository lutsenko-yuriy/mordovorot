package web.storage

import storage.PreferenceRepository

/** The page's preferences (GH-139): one [store] entry per key under `mordovorot.pref.<key>`,
 *  apart from [BrowserSaveRepository]'s `mordovorot.save.*`. */
class BrowserPreferenceRepository(private val store: KeyValueStore) : PreferenceRepository {

    override fun get(key: String): String? = store.get(PREFIX + key)

    override fun set(key: String, value: String) = store.set(PREFIX + key, value)

    private companion object {
        const val PREFIX = "mordovorot.pref."
    }
}
