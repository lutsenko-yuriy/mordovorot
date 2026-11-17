package web.scenario

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.SearchProgress
import mordie.SolutionMove
import storage.SavedMordie
import storage.SavedMove
import texts.Language
import texts.activeLanguage
import texts.useLanguage
import viewmodel.TileLabels
import web.Arrow
import web.PickerControl
import web.WebDialog

/** GH-275: the game follows the browser's language, a picker switches it, and a switch keeps the game.
 *  The picker's own markup (and the toolbar's buttons, which `DomRenderer` words) is checked in the smoke test. */
class WebLanguageScenarioTest {
    private val en = Language.EN.texts
    private val de = Language.DE.texts
    private val ru = Language.RU.texts

    private val original = intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)
    private val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2))
    private val savedPath = path.map { SavedMove(it.isRow, it.index, it.amount) }

    @AfterTest
    fun backToEnglish() = useLanguage(Language.EN)

    private fun WebHarness.pick(language: Language, control: PickerControl = PickerControl.TOGGLES) = game.language.onLanguage(language, control)

    @Test
    fun `a German browser starts the game in German`() = webTest {
        val page = WebHarness(backgroundScope, browserLanguages = listOf("de-DE")).startOn()

        assertEquals(Language.DE, page.state.language)
        assertEquals(de.page.footerBasedOn("9.9.9"), page.state.footer.first().text)
        assertEquals(de.mordie.opening, page.state.mordie)
        assertEquals(Language.DE, activeLanguage())
        assertNull(page.store.entries["mordovorot.pref.language"])
    }

    @Test
    fun `an unsupported browser language starts in English`() = webTest {
        val page = WebHarness(backgroundScope, browserLanguages = listOf("fr")).startOn()

        assertEquals(Language.EN, page.state.language)
        assertEquals(en.mordie.opening, page.state.mordie)
    }

    @Test
    fun `only the browser's first language counts`() = webTest {
        val page = WebHarness(backgroundScope, browserLanguages = listOf("uk", "ru")).startOn()

        assertEquals(Language.EN, page.state.language)
        assertEquals(en.mordie.opening, page.state.mordie)
    }

    @Test
    fun `a stored language beats the browser`() = webTest {
        val page = WebHarness(backgroundScope, browserLanguages = listOf("de")).remembering(language = "ru").startOn()

        assertEquals(Language.RU, page.state.language)
        assertEquals(ru.mordie.opening, page.state.mordie)
    }

    @Test
    fun `an unknown stored language falls back to detection`() = webTest {
        val page = WebHarness(backgroundScope, browserLanguages = listOf("de")).remembering(language = "xx").startOn()

        assertEquals(Language.DE, page.state.language)
    }

    @Test
    fun `with the switch off the page stays English`() = webTest {
        val page = WebHarness(backgroundScope, browserLanguages = listOf("de"), languages = false).remembering(language = "ru").startOn()

        assertEquals(Language.EN, page.state.language)
        assertEquals(en.mordie.opening, page.state.mordie)
    }

    @Test
    fun `switching mid-game keeps the game`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.RIGHT, 0)
        page.game.onArrow(Arrow.DOWN, 1)
        page.game.onTilePressed(2, 3)
        page.game.onSave()
        page.game.onSaveAnswered("a b")
        val search = page.mordieSearches.latest
        val before = page.state
        assertEquals(2, before.moves)
        assertEquals(en.status.saveNameHasSpaces("a b"), before.status)

        page.pick(Language.DE)

        val after = page.state
        assertEquals(Language.DE, after.language)
        assertEquals(before.tiles, after.tiles)
        assertEquals(before.moves, after.moves)
        assertEquals(before.selection, after.selection)
        assertTrue(page.mordieSearches.latest === search)
        assertEquals(de.page.footerBasedOn("9.9.9"), after.footer.first().text)
        assertEquals(de.keys.hintSelect, after.hints!!.first().does)
        assertEquals(de.mordie.opening, after.mordie)
        assertNull(after.status)
    }

    @Test
    fun `a pick is remembered`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.pick(Language.DE)

        assertEquals("de", page.store.entries["mordovorot.pref.language"])
        val reloaded = WebHarness(backgroundScope, store = page.store).startOn()
        assertEquals(Language.DE, reloaded.state.language)
        assertEquals(de.mordie.opening, reloaded.state.mordie)
    }

    @Test
    fun `picking the current language does nothing`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        val frames = page.frames.size

        page.pick(Language.EN)

        assertEquals(frames, page.frames.size)
        assertEquals(emptyList(), page.props("language_changed"))
        assertNull(page.store.entries["mordovorot.pref.language"])
    }

    @Test
    fun `switching on the solution screen re-words it`() = webTest {
        val page = WebHarness(backgroundScope).seed("m", original, mordie = SavedMordie(original, 2, found = true, solution = savedPath)).start()
        page.game.onLoadChosen("m")
        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(true)
        page.game.solution.onReplayNext()
        val before = page.state.solution!!
        assertEquals("${en.mordie.solutionNotStarted} ${en.mordie.solutionHere(2)}", page.state.mordie)

        page.pick(Language.DE)

        assertEquals("${de.mordie.solutionNotStarted} ${de.mordie.solutionHere(2)}", page.state.mordie)
        val after = page.state.solution!!
        assertEquals(before.current, after.current)
        assertEquals(before.moves, after.moves)
        assertTrue(after.steps.all { it.contains(de.page.stepRow) || it.contains(de.page.stepColumn) }, "${after.steps}")
        assertEquals(page.props("screen_solution").size, 1)
        assertEquals("solution", page.props("language_changed").single()["screen"])
    }

    @Test
    fun `switching on a solved board re-words the ending`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.LEFT, 0)
        assertTrue(page.state.solved)
        assertEquals(en.mordie.endingPlayerWon(0), page.state.mordie)

        page.pick(Language.DE)

        assertTrue(page.state.solved)
        assertEquals(de.mordie.endingPlayerWon(0), page.state.mordie)
        assertEquals("solved", page.props("language_changed").single()["screen"])
    }

    @Test
    fun `switching during the tutorial keeps the step`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps(1)
        assertEquals(2, page.state.guidance?.step)

        page.pick(Language.DE)

        assertEquals(2, page.state.guidance?.step)
        assertEquals(de.tutorial.step2("14", computer = true), page.state.guidance?.text)
    }

    @Test
    fun `a pick while a dialog is open is ignored`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onSave()
        val dialog = page.state.dialog
        assertTrue(dialog is WebDialog.Save)
        val frames = page.frames.size

        page.pick(Language.DE)

        assertEquals(Language.EN, page.state.language)
        assertEquals(dialog, page.state.dialog)
        assertEquals(frames, page.frames.size)
        assertEquals(emptyList(), page.props("language_changed"))
    }

    @Test
    fun `a pick during a drag is ignored`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onDragStart(1, 0, 100.0, 100.0, 40.0)

        page.pick(Language.DE)

        assertEquals(Language.EN, page.state.language)
    }

    @Test
    fun `a save and a share link carry no language`() = webTest {
        val page = WebHarness(backgroundScope, browserLanguages = listOf("de")).startOn()
        page.game.onArrow(Arrow.RIGHT, 0)
        page.game.onSave()
        page.game.onSaveAnswered("mein-Spiel")
        page.game.share.open()
        val link = (page.state.dialog as WebDialog.Share).link
        assertFalse("lang" in link.lowercase() || "language" in link.lowercase(), link)
        page.game.share.onClosed()

        page.pick(Language.EN)
        page.game.onLoad()
        page.game.onLoadChosen("mein-Spiel")

        assertEquals(Language.EN, page.state.language)
        assertEquals(en.status.loaded("mein-Spiel"), page.state.status)
        assertEquals(listOf("mein-Spiel"), page.saves.listSaves())
    }

    @Test
    fun `Russian keeps Latin letters and NATO words`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.labels.open()
        page.game.labels.onSelected(TileLabels.LETTERS)
        page.game.labels.onAnswered(ok = true)

        page.pick(Language.RU)

        assertEquals(Language.RU, page.state.language)
        assertEquals(listOf("D", "A", "B", "C"), page.state.tileTexts.take(4))
        assertEquals(ru.reader.phonetic, en.reader.phonetic)
        assertTrue(page.state.boardWords!!.startsWith(ru.reader.boardRow(1, "delta, alpha, bravo, charlie")), page.state.boardWords)
    }

    @Test
    fun `a failed preference write still switches the session`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.store.setException = RuntimeException("read-only")

        page.pick(Language.DE)

        assertEquals(Language.DE, page.state.language)
        assertEquals(de.page.footerBasedOn("9.9.9"), page.state.footer.first().text)
    }
}
