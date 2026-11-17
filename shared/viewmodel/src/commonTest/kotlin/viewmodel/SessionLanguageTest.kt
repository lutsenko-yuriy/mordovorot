package viewmodel

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import testing.FakePreferenceRepository
import texts.Language
import texts.activeLanguage
import texts.useLanguage

class SessionLanguageTest {

    private val store = FakePreferenceRepository()

    private fun session(vararg browser: String, enabled: Boolean = true) =
        SessionLanguage(LanguagePreference(store), browser.toList(), enabled)

    @AfterTest
    fun backToEnglish() = useLanguage(Language.EN)

    @Test
    fun `nothing known starts in English from the default`() {
        val session = session()
        assertEquals(Language.EN, session.current)
        assertEquals(LanguageSource.DEFAULT, session.source)
    }

    @Test
    fun `the browser's language is used and said to be from the browser`() {
        val session = session("de-DE", "en")
        assertEquals(Language.DE, session.current)
        assertEquals(LanguageSource.BROWSER, session.source)
        assertEquals(Language.DE, activeLanguage())
        assertEquals(emptyMap(), store.values)
    }

    @Test
    fun `an unsupported browser language is the default`() {
        val session = session("fr")
        assertEquals(Language.EN, session.current)
        assertEquals(LanguageSource.DEFAULT, session.source)
    }

    @Test
    fun `a stored language beats the browser`() {
        store.values["language"] = "ru"
        val session = session("de")
        assertEquals(Language.RU, session.current)
        assertEquals(LanguageSource.PREFERENCE, session.source)
    }

    @Test
    fun `an unknown stored language falls back to the browser`() {
        store.values["language"] = "xx"
        assertEquals(Language.DE, session("de").current)
    }

    @Test
    fun `choosing applies, remembers and says it is a preference`() {
        val session = session("de")
        session.choose(Language.RU)
        assertEquals(Language.RU, session.current)
        assertEquals(LanguageSource.PREFERENCE, session.source)
        assertEquals(Language.RU, activeLanguage())
        assertEquals("ru", store.values["language"])
    }

    @Test
    fun `a failed write still switches the session`() {
        store.setException = RuntimeException("read-only")
        val session = session()
        session.choose(Language.DE)
        assertEquals(Language.DE, session.current)
        assertEquals(Language.DE, activeLanguage())
    }

    @Test
    fun `turned off, the session is English whatever is stored or asked for`() {
        store.values["language"] = "ru"
        val session = session("de", enabled = false)
        assertEquals(Language.EN, session.current)
        assertEquals(Language.EN, activeLanguage())
        assertEquals(LanguageSource.DEFAULT, session.source)
    }
}
