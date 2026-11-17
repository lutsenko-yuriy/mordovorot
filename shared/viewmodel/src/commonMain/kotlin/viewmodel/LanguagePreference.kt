package viewmodel

import storage.PreferenceRepository
import texts.Language

/** The player's chosen [Language] (GH-275), kept like [KeyboardLayoutPreference]: a failed read
 *  means no preference, a failed write isn't remembered. */
class LanguagePreference(private val store: PreferenceRepository) {

    /** `null` when nothing is stored, the code is unknown, or the read fails. */
    fun load(): Language? = try {
        Language.fromCode(store.get(KEY))
    } catch (e: Exception) {
        null
    }

    /** Whether the write succeeded. */
    fun save(language: Language): Boolean = try {
        store.set(KEY, language.code)
        true
    } catch (e: Exception) {
        false
    }

    private companion object {
        const val KEY = "language"
    }
}
