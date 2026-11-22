package web.scenario

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import texts.Language
import texts.texts
import texts.useLanguage
import viewmodel.TileLabelGroup
import viewmodel.TileLabels
import web.Focus
import web.WebDialog
import web.title
import web.tutorial.TutorialText

/** GH-322: the two Greek tile-label schemes. */
class WebGreekLabelsScenarioTest {
    private val statusTexts = texts().status

    @AfterTest
    fun backToEnglish() = useLanguage(Language.EN)

    private val WebHarness.labels get() = state.dialog as WebDialog.Labels

    private fun WebHarness.choose(labels: TileLabels) {
        game.labels.open()
        game.labels.onSelected(labels)
        game.labels.onAnswered(ok = true)
    }

    private fun WebHarness.newGame(side: Int) {
        game.onNew()
        game.onSizeChosen(side)
    }

    /** A new game is scrambled: find a tile's text by its solved place (1-based). */
    private fun WebHarness.textOf(tile: Int) = state.tileTexts[state.tiles.indexOf(tile)]

    // The board
    @Test
    fun `greek letters relabel a 4x4 board`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.choose(TileLabels.GREEK_LETTERS)

        // The harness board is the solved one with row 0 shifted a tile right.
        assertEquals(listOf("Δ", "Α", "Β", "Γ"), page.state.tileTexts.take(4))
        assertEquals(listOf(0x394, 0x391, 0x392, 0x393), page.state.tileTexts.take(4).map { it.single().code })
        assertEquals(page.state.tiles.map { TileLabels.GREEK_LETTERS.label(it - 1, 4) }, page.state.tileTexts)
    }

    @Test
    fun `the 25th greek label is the greek question mark`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.newGame(5)

        page.choose(TileLabels.GREEK_LETTERS)

        val label = page.textOf(25)
        assertEquals(0x37E, label.single().code)
        assertNotEquals(";", label)
    }

    @Test
    fun `greek letter pairs fit a 9x9 board`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.newGame(9)

        page.choose(TileLabels.GREEK_LETTER_PAIRS)

        assertEquals("ΑΑ", page.textOf(1))
        assertEquals("ΑΙ", page.textOf(9))
        assertEquals("ΙΑ", page.textOf(73))
        assertEquals("ΙΙ", page.textOf(81))
    }

    @Test
    fun `greek letters are dimmed on 6x6`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.GREEK_LETTERS)

        page.newGame(6)

        assertEquals(TileLabels.NUMBERS, page.state.labels)
        page.game.labels.open()
        val row = page.labels.options.single { it.labels == TileLabels.GREEK_LETTERS }
        assertEquals("up to 5x5", row.limit)
        assertEquals(false, row.available)
    }

    @Test
    fun `the greek preference survives a 6x6 game`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.GREEK_LETTERS)

        page.newGame(6)
        page.newGame(5)

        assertEquals(TileLabels.GREEK_LETTERS, page.state.labels)
        assertEquals(page.state.tiles.map { TileLabels.GREEK_LETTERS.label(it - 1, 5) }, page.state.tileTexts)
    }

    // The dialog
    @Test
    fun `the dialog lists Greek letters fifth`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.labels.open()

        assertEquals(
            listOf("Numbers", "Numbers from 0", "Latin letters", "Cyrillic letters", "Greek letters", "Pre-reform Russian"),
            TileLabelGroup.entries.map { it.title },
        )
        page.game.labels.onGroupOpened(TileLabelGroup.GREEK)
        assertEquals(listOf("Letters", "Pairs"), page.labels.openOptions.map { it.title })
        assertEquals("label-group-greek", Focus.LabelGroup(TileLabelGroup.GREEK).key)
    }

    @Test
    fun `a greek preference opens the greek group`() = webTest {
        val page = WebHarness(backgroundScope).apply { preferences.set("tile_labels", "greek_letters") }.startOn()

        page.game.labels.open()

        assertEquals(TileLabelGroup.GREEK, page.labels.open)
        assertEquals(Focus.Label(TileLabels.GREEK_LETTERS), page.state.focus)
        assertEquals("label-greek_letters", page.state.focus?.key)
    }

    @Test
    fun `the preview strip shows greek labels`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.labels.open()
        page.game.labels.onGroupOpened(TileLabelGroup.GREEK)

        page.game.labels.onSelected(TileLabels.GREEK_LETTERS)
        assertEquals(listOf("Α", "Β", "Γ"), page.labels.preview)
        page.game.labels.onSelected(TileLabels.GREEK_LETTER_PAIRS)

        assertEquals(listOf("ΑΑ", "ΑΒ", "ΑΓ"), page.labels.preview)
    }

    // Saves and links
    @Test
    fun `a save keeps greek letter pairs`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.GREEK_LETTER_PAIRS)
        page.game.onSave()
        page.game.onSaveAnswered("foo")
        page.choose(TileLabels.PAIRS)

        page.game.onLoad()
        page.game.onLoadChosen("foo")

        assertEquals(TileLabels.GREEK_LETTER_PAIRS, page.state.labels)
        assertEquals("ΑΑ", page.textOf(1))
    }

    @Test
    fun `a 5x5 save keeps the greek question mark`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.newGame(5)
        page.choose(TileLabels.GREEK_LETTERS)
        page.game.onSave()
        page.game.onSaveAnswered("five")
        page.choose(TileLabels.NUMBERS)

        page.game.onLoad()
        page.game.onLoadChosen("five")

        assertEquals(TileLabels.GREEK_LETTERS, page.state.labels)
        assertEquals(0x37E, page.textOf(25).single().code)
    }

    @Test
    fun `an old 6x6 save with greek letters opens the dialog with the greek message`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.saves.save("six", rotatedRow(6), 6, 0, labels = "greek_letters")

        page.game.onLoad()
        page.game.onLoadChosen("six")

        assertEquals(statusTexts.greekLettersDontFit(6), page.labels.message)
        assertEquals("Greek letters don't fit a 6x6 board (25 labels, 36 tiles). Please pick other labels.", page.labels.message)
        assertEquals(
            listOf(mapOf("opened_from" to "letters_save", "screen" to "play", "current_labels" to "numbers")),
            page.ownProps("screen_labels_dialog"),
        )
    }

    @Test
    fun `a share link carries the greek labels`() = webTest {
        val sender = WebHarness(backgroundScope).startOn()
        sender.choose(TileLabels.GREEK_LETTERS)
        sender.game.share.open()
        sender.game.share.onLabelsToggled()
        val link = (sender.state.dialog as WebDialog.Share).link
        assertTrue(link.endsWith("l=greek_letters"), link)

        val receiver = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED, "greek_letters")

        val prompt = assertIs<WebDialog.SharedLabels>(receiver.state.dialog)
        assertEquals(TileLabels.GREEK_LETTERS, prompt.labels)
        assertEquals(texts().dialogs.sharedLabelsTitle("Greek letters"), prompt.title)
    }

    // Reading aloud and texts
    @Test
    fun `read board spells greek labels`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.GREEK_LETTERS)

        page.game.readBoard()

        val said = page.said.last()
        assertTrue("delta, alpha, beta, gamma" in said, said)
    }

    @Test
    fun `read board says the greek question mark`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.newGame(5)
        page.choose(TileLabels.GREEK_LETTERS)

        page.game.readBoard()

        val said = page.said.last()
        assertTrue("Greek question mark" in said, said)
        assertEquals(1, "Greek question mark".toRegex().findAll(said).count())
    }

    @Test
    fun `a german page says the greek question mark in german`() = webTest {
        val german = WebHarness(backgroundScope, browserLanguages = listOf("de-DE")).startOn()
        german.newGame(5)
        german.choose(TileLabels.GREEK_LETTERS)
        german.game.readBoard()
        assertTrue("griechisches Fragezeichen" in german.said.last(), german.said.last())

        val russian = WebHarness(backgroundScope, browserLanguages = listOf("ru-RU")).startOn()
        russian.newGame(5)
        russian.choose(TileLabels.GREEK_LETTERS)
        russian.game.readBoard()
        assertTrue("греческий вопросительный знак" in russian.said.last(), russian.said.last())
    }

    @Test
    fun `a german page names the greek group and rows`() = webTest {
        val page = WebHarness(backgroundScope, browserLanguages = listOf("de-DE")).startOn()
        page.game.labels.open()

        page.game.labels.onGroupOpened(TileLabelGroup.GREEK)

        assertEquals("Griechische Buchstaben", TileLabelGroup.GREEK.title)
        assertEquals(listOf("Buchstaben", "Paare"), page.labels.openOptions.map { it.title })
    }

    @Test
    fun `a russian page names the greek group and rows`() = webTest {
        val page = WebHarness(backgroundScope, browserLanguages = listOf("ru-RU")).startOn()
        page.game.labels.open()

        page.game.labels.onGroupOpened(TileLabelGroup.GREEK)

        assertEquals("Греческие буквы", TileLabelGroup.GREEK.title)
        assertEquals(listOf("Буквы", "Пары"), page.labels.openOptions.map { it.title })
    }

    @Test
    fun `the share toggle names greek letter pairs in full`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.GREEK_LETTER_PAIRS)

        page.game.share.open()

        assertEquals("Include my tile labels (Greek letter pairs)", (page.state.dialog as WebDialog.Share).labelsToggle)
    }

    // The tutorial
    @Test
    fun `tutorial step 2 names its tile with the greek letter`() = webTest {
        val first = WebHarness(backgroundScope, firstLaunch = true)
        first.preferences.set("tile_labels", "greek_letters")
        val page = first.start()
        page.stepByArrow(1)

        assertEquals(2, page.state.guidance?.step)
        assertEquals(TutorialText.step(2, computer = true, labels = TileLabels.GREEK_LETTERS), page.state.guidance?.text)
        assertTrue("Ξ" in page.state.guidance!!.text, page.state.guidance!!.text)
    }

    @Test
    fun `the tutorial's labels step opens the greek group`() = webTest {
        val first = WebHarness(backgroundScope, firstLaunch = true)
        first.preferences.set("tile_labels", "greek_letters")
        val page = first.start()

        page.game.tutorial.onLabels()
        assertEquals(TileLabelGroup.GREEK, page.labels.open)
        page.game.labels.onAnswered(ok = false)

        assertEquals(Focus.TutorialLabels, page.state.focus)
        assertEquals("tutorial-labels", page.state.focus?.key)
    }

    // Events
    @Test
    fun `choosing greek labels sends tile_labels_changed`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.choose(TileLabels.GREEK_LETTERS)

        assertEquals("greek_letters", page.props("tile_labels_changed").single()["to"])
    }

    @Test
    fun `the 6x6 greek fallback is reported as letters_unavailable`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.saves.save("six", rotatedRow(6), 6, 0, labels = "greek_letters")
        page.game.onLoad()
        page.game.onLoadChosen("six")

        page.game.labels.onSelected(TileLabels.PAIRS)
        page.game.labels.onAnswered(ok = true)

        assertEquals("letters_unavailable", page.props("tile_labels_changed").last()["from_source"])
    }
}
