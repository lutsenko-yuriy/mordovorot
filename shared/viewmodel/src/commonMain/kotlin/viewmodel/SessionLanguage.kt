package viewmodel

import texts.Language
import texts.useLanguage

/** Where the session's language came from (GH-275); lowercased, `language_changed.from_source`. */
enum class LanguageSource { DEFAULT, BROWSER, PREFERENCE }

/** The session's language (GH-275): a stored pick, else the browser's first language ([browserTags]), else English.
 *  Applies it to `texts()` as it is built and when [choose]n. One per session, like [SessionKeyboardLayout].
 *  Only a pick is stored, so a player who never picks follows the browser on every visit.
 *  [enabled] off keeps the session English, ignoring both (the `languages` switch, until the picker is on). */
class SessionLanguage(private val preference: LanguagePreference, browserTags: List<String>, enabled: Boolean = true) {

    var current: Language = Language.EN
        private set

    var source: LanguageSource = LanguageSource.DEFAULT
        private set

    init {
        if (enabled) {
            val stored = preference.load()
            val browser = Language.fromBrowser(browserTags)
            when {
                stored != null -> show(stored, LanguageSource.PREFERENCE)
                browser != null -> show(browser, LanguageSource.BROWSER)
            }
        }
        useLanguage(current)
    }

    /** The picker's pick. A failed preference write still switches the session; it just isn't remembered. */
    fun choose(to: Language) {
        current = to
        source = LanguageSource.PREFERENCE
        useLanguage(to)
        preference.save(to)
    }

    private fun show(language: Language, from: LanguageSource) {
        current = language
        source = from
    }
}
