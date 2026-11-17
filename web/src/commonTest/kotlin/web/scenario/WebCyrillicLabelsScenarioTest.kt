package web.scenario

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import texts.Language
import texts.texts
import texts.useLanguage
import viewmodel.TileLabels
import web.Focus
import web.LabelOption
import web.WebDialog
import web.title
import web.tutorial.TutorialText

/** GH-313: the two Cyrillic tile-label schemes. */
class WebCyrillicLabelsScenarioTest {
    private val pageTexts = texts().page
    private val statusTexts = texts().status

    @AfterTest
    fun backToEnglish() = useLanguage(Language.EN)

    private fun WebHarness.choose(labels: TileLabels) {
        game.labels.open()
        game.labels.onSelected(labels)
        game.labels.onAnswered(ok = true)
    }

    private fun WebHarness.newGame(side: Int) {
        game.onNew()
        game.onSizeChosen(side)
    }

    @Test
    fun `cyrillic letters relabel a 4x4 board`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.choose(TileLabels.CYRILLIC_LETTERS)

        // The harness board is the solved one with row 0 shifted a tile right.
        assertEquals(listOf("Г", "А", "Б", "В"), page.state.tileTexts.take(4))
        assertEquals(page.state.tiles.map { TileLabels.CYRILLIC_LETTERS.label(it - 1, 4) }, page.state.tileTexts)
    }

    @Test
    fun `the labels dialog lists eight rows, the Cyrillic ones last`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.labels.open()

        val dialog = page.state.dialog as WebDialog.Labels
        assertEquals(8, dialog.options.size)
        assertEquals(listOf(TileLabels.CYRILLIC_LETTERS, TileLabels.CYRILLIC_LETTER_PAIRS), dialog.options.takeLast(2).map { it.labels })
        assertEquals(TileLabels.LETTER_PAIRS, dialog.options[5].labels)
        assertEquals(LabelOption(TileLabels.CYRILLIC_LETTERS, "Cyrillic letters", listOf("А", "Б", "В", "Г")), dialog.options[6])
        assertEquals("label-cyrillic_letters", Focus.Label(TileLabels.CYRILLIC_LETTERS).key)
    }

    @Test
    fun `Cyrillic letters on 6x6 fall back to numbers`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.CYRILLIC_LETTERS)

        page.newGame(6)

        assertEquals(TileLabels.NUMBERS, page.state.labels)
        page.game.labels.open()
        val row = (page.state.dialog as WebDialog.Labels).options.single { it.labels == TileLabels.CYRILLIC_LETTERS }
        assertEquals(LabelOption(TileLabels.CYRILLIC_LETTERS, pageTexts.labelCyrillicLetters, listOf("up to 5x5"), available = false), row)
        assertTrue((page.state.dialog as WebDialog.Labels).options.filter { it.labels != TileLabels.CYRILLIC_LETTERS && it.labels != TileLabels.LETTERS }.all { it.available })
    }

    @Test
    fun `the Cyrillic preference survives a 6x6 game`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.CYRILLIC_LETTERS)

        page.newGame(6)
        page.newGame(5)

        assertEquals(TileLabels.CYRILLIC_LETTERS, page.state.labels)
        assertEquals(page.state.tiles.map { TileLabels.CYRILLIC_LETTERS.label(it - 1, 5) }, page.state.tileTexts)
    }

    @Test
    fun `Cyrillic letter pairs fit a 9x9 board`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.newGame(9)

        page.choose(TileLabels.CYRILLIC_LETTER_PAIRS)

        // A new game is scrambled: find the tiles by their solved place.
        fun label(tile: Int) = page.state.tileTexts[page.state.tiles.indexOf(tile)]
        assertEquals("АА", label(1))
        assertEquals("АИ", label(9))
        assertEquals("ИА", label(73))
    }

    @Test
    fun `a save keeps Cyrillic letter pairs`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.CYRILLIC_LETTER_PAIRS)
        page.game.onSave()
        page.game.onSaveAnswered("foo")
        page.choose(TileLabels.PAIRS)

        page.game.onLoad()
        page.game.onLoadChosen("foo")

        assertEquals(TileLabels.CYRILLIC_LETTER_PAIRS, page.state.labels)
        assertEquals("АА", page.state.tileTexts[page.state.tiles.indexOf(1)])
    }

    @Test
    fun `an old 6x6 save with Cyrillic letters opens the dialog with the Cyrillic message`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.saves.save("six", rotatedRow(6), 6, 0, labels = "cyrillic_letters")

        page.game.onLoad()
        page.game.onLoadChosen("six")

        val dialog = page.state.dialog as WebDialog.Labels
        assertEquals(statusTexts.cyrillicLettersDontFit(6), dialog.message)
        assertEquals("Cyrillic letters don't fit a 6x6 board (28 letters, 36 tiles). Please pick other labels.", dialog.message)
        assertEquals(
            listOf(mapOf("opened_from" to "letters_save", "screen" to "play", "current_labels" to "numbers")),
            page.ownProps("screen_labels_dialog"),
        )
    }

    @Test
    fun `a share link carries the Cyrillic labels`() = webTest {
        val sender = WebHarness(backgroundScope).startOn()
        sender.choose(TileLabels.CYRILLIC_LETTERS)
        sender.game.share.open()
        sender.game.share.onLabelsToggled()
        val link = (sender.state.dialog as WebDialog.Share).link
        assertTrue(link.endsWith("l=cyrillic_letters"), link)

        val receiver = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED, "cyrillic_letters")

        val prompt = assertIs<WebDialog.SharedLabels>(receiver.state.dialog)
        assertEquals(TileLabels.CYRILLIC_LETTERS, prompt.labels)
        assertEquals(texts().dialogs.sharedLabelsTitle("Cyrillic letters"), prompt.title)
    }

    @Test
    fun `Read board spells Cyrillic labels`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.CYRILLIC_LETTERS)

        page.game.readBoard()

        val said = page.said.last()
        assertTrue("Григорий, Анна, Борис, Василий" in said, said)
    }

    @Test
    fun `a German page names the Cyrillic rows in German`() = webTest {
        val page = WebHarness(backgroundScope, browserLanguages = listOf("de-DE")).startOn()

        page.game.labels.open()

        val options = (page.state.dialog as WebDialog.Labels).options
        assertEquals(listOf("Kyrillische Buchstaben", "Kyrillische Buchstabenpaare"), options.takeLast(2).map { it.title })
    }

    @Test
    fun `tutorial step 2 names its tile with the Cyrillic letter`() = webTest {
        val first = WebHarness(backgroundScope, firstLaunch = true)
        first.preferences.set("tile_labels", "cyrillic_letters")
        val page = first.start()
        page.stepByArrow(1)

        assertEquals(2, page.state.guidance?.step)
        assertEquals(TutorialText.step(2, computer = true, labels = TileLabels.CYRILLIC_LETTERS), page.state.guidance?.text)
        assertTrue("О" in page.state.guidance!!.text, page.state.guidance!!.text)
    }

    @Test
    fun `choosing Cyrillic labels sends tile_labels_changed`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.choose(TileLabels.CYRILLIC_LETTERS)

        assertEquals("cyrillic_letters", page.props("tile_labels_changed").single()["to"])
    }

    @Test
    fun `the 6x6 fallback is reported as letters_unavailable`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.CYRILLIC_LETTERS)
        page.newGame(6)

        page.game.labels.open()
        page.game.labels.onSelected(TileLabels.PAIRS)
        page.game.labels.onAnswered(ok = true)

        assertFalse(page.props("tile_labels_changed").isEmpty())
        assertEquals("letters_unavailable", page.props("tile_labels_changed").last()["from_source"])
    }
}
