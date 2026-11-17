package web.scenario

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import texts.Language
import texts.useLanguage
import viewmodel.TileLabels
import web.Arrow
import web.PickerControl

/** GH-275: the language picker's events; every other event is the same in every language. */
class WebLanguageAnalyticsScenarioTest {

    @AfterTest
    fun backToEnglish() = useLanguage(Language.EN)

    @Test
    fun `switching sends language_changed`() = webTest {
        val page = WebHarness(backgroundScope, browserLanguages = listOf("de")).startOn()
        page.game.onArrow(Arrow.RIGHT, 0)

        page.game.language.onLanguage(Language.RU, PickerControl.DROPDOWN)

        assertEquals(
            listOf(mapOf("from" to "de", "to" to "ru", "from_source" to "browser", "screen" to "play", "control" to "dropdown")),
            page.ownProps("language_changed"),
        )
    }

    @Test
    fun `launch reports the language`() = webTest {
        val page = WebHarness(backgroundScope, browserLanguages = listOf("ru")).start()

        assertEquals("ru", page.ownProps("app_launched").single()["language"])
        assertEquals("en", WebHarness(backgroundScope, browserLanguages = listOf("fr")).start().ownProps("app_launched").single()["language"])
    }

    /** New game, shifts, save, load, labels, give up, replay and share, as the events a page records. */
    private suspend fun kotlinx.coroutines.test.TestScope.scripted(language: String): List<Pair<String, Map<String, Any?>>> {
        val page = WebHarness(backgroundScope, browserLanguages = listOf(language)).startOn()
        page.game.onNew()
        page.game.onSizeChosen(4)
        page.game.onArrow(Arrow.RIGHT, 0)
        page.game.onArrow(Arrow.DOWN, 1)
        page.game.onSave()
        page.game.onSaveAnswered("s")
        page.game.onLoad()
        page.game.onLoadChosen("s")
        page.game.labels.open()
        page.game.labels.onSelected(TileLabels.PAIRS)
        page.game.labels.onAnswered(ok = true)
        page.game.share.open()
        page.game.share.onClosed()
        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(true)
        page.game.solution.onReplayNext()
        page.game.solution.onReplayRestart()
        return page.analytics.events.map { it.name to it.properties }
    }

    @Test
    fun `analytics don't depend on the language`() = webTest {
        val english = scripted("en")
        val german = scripted("de")
        val russian = scripted("ru")

        assertEquals(english, german)
        assertEquals(english, russian)
    }
}
