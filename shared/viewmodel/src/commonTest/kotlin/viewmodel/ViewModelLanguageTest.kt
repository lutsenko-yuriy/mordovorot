package viewmodel

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import testing.FakePreferenceRepository
import testing.RecordingAnalyticsService
import testing.testViewModel
import texts.Language
import texts.activeLanguage
import texts.useLanguage

/** GH-275: `language_changed`, tracked only for a real switch, with where the language came from. */
class ViewModelLanguageTest {

    private val analytics = RecordingAnalyticsService()
    private val store = FakePreferenceRepository()
    private val session = SessionLanguage(LanguagePreference(store), listOf("de"))
    private val viewModel = testViewModel(analytics = analytics, language = session)

    private fun changed() = analytics.events.filter { it.name == "language_changed" }.map { it.properties }

    @AfterTest
    fun backToEnglish() = useLanguage(Language.EN)

    @Test
    fun `the viewModel says the session's language`() {
        assertEquals(Language.DE, viewModel.language())
    }

    @Test
    fun `a switch tracks where from and to, the source and the control`() {
        viewModel.changeLanguage(Language.RU, "toggles")

        assertEquals(
            listOf(mapOf("from" to "de", "to" to "ru", "from_source" to "browser", "screen" to "play", "control" to "toggles")),
            changed(),
        )
        assertEquals(Language.RU, viewModel.language())
        assertEquals(Language.RU, activeLanguage())
        assertEquals("ru", store.values["language"])
    }

    @Test
    fun `the next switch comes from a preference`() {
        viewModel.changeLanguage(Language.RU, "dropdown")
        viewModel.changeLanguage(Language.EN, "dropdown")

        assertEquals("preference", changed().last()["from_source"])
        assertEquals("ru", changed().last()["from"])
    }

    @Test
    fun `picking the current language tracks and writes nothing`() {
        viewModel.changeLanguage(Language.DE, "toggles")

        assertEquals(emptyList(), changed())
        assertEquals(emptyMap(), store.values)
    }
}
