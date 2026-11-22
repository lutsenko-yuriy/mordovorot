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
import web.Arrow
import web.Focus
import web.WebDialog
import web.title

/** GH-354: the pre-reform Russian tile labels on the board, in the dialog, in saves and in links. */
class WebPrereformLabelsScenarioTest {
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
    fun `prereform letters relabel a 4x4 board`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.choose(TileLabels.PREREFORM_LETTERS)

        // The harness board is the solved one with row 0 shifted a tile right.
        assertEquals(listOf("Г", "А", "Б", "В"), page.state.tileTexts.take(4))
        assertEquals(page.state.tiles.map { TileLabels.PREREFORM_LETTERS.label(it - 1, 4) }, page.state.tileTexts)
    }

    @Test
    fun `the 10th and 11th labels are Й and the Cyrillic І`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.choose(TileLabels.PREREFORM_LETTERS)

        assertEquals("Й", page.textOf(10))
        assertEquals(0x0406, page.textOf(11).single().code)
        assertNotEquals("I", page.textOf(11))
    }

    @Test
    fun `a 6x6 board shows the three old letters`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.newGame(6)

        page.choose(TileLabels.PREREFORM_LETTERS)

        assertEquals("Ѣ", page.textOf(31))
        assertEquals("Ѳ", page.textOf(35))
        assertEquals("Ѵ", page.textOf(36))
    }

    @Test
    fun `prereform letter pairs fit a 9x9 board`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.newGame(9)

        page.choose(TileLabels.PREREFORM_LETTER_PAIRS)

        assertEquals("АА", page.textOf(1))
        assertEquals("ИИ", page.textOf(81))
    }

    @Test
    fun `prereform letters are dimmed on 7x7`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.PREREFORM_LETTERS)

        page.newGame(7)

        assertEquals(TileLabels.NUMBERS, page.state.labels)
        page.game.labels.open()
        val row = page.labels.options.single { it.labels == TileLabels.PREREFORM_LETTERS }
        assertEquals("up to 6x6", row.limit)
        assertEquals(false, row.available)
        page.game.labels.onSelected(TileLabels.PREREFORM_LETTERS)
        assertEquals(TileLabels.PREREFORM_LETTERS, page.labels.tip)
    }

    @Test
    fun `the prereform preference survives a 7x7 game`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.PREREFORM_LETTERS)

        page.newGame(7)
        assertEquals(TileLabels.NUMBERS, page.state.labels)
        page.newGame(4)

        assertEquals(TileLabels.PREREFORM_LETTERS, page.state.labels)
        assertEquals(page.state.tiles.map { TileLabels.PREREFORM_LETTERS.label(it - 1, 4) }, page.state.tileTexts)
    }

    // The dialog
    @Test
    fun `the dialog lists the prereform group sixth`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.labels.open()

        assertEquals(6, TileLabelGroup.entries.size)
        assertEquals("Pre-reform Russian", TileLabelGroup.entries.last().title)
        page.game.labels.onGroupOpened(TileLabelGroup.PREREFORM)
        assertEquals(listOf("Letters", "Pairs"), page.labels.openOptions.map { it.title })
        assertEquals("label-group-prereform", Focus.LabelGroup(TileLabelGroup.PREREFORM).key)
    }

    @Test
    fun `a prereform preference opens the prereform group`() = webTest {
        val page = WebHarness(backgroundScope).apply { preferences.set("tile_labels", "prereform_letters") }.startOn()

        page.game.labels.open()

        assertEquals(TileLabelGroup.PREREFORM, page.labels.open)
        assertEquals(Focus.Label(TileLabels.PREREFORM_LETTERS), page.state.focus)
        assertEquals("label-prereform_letters", page.state.focus?.key)
    }

    @Test
    fun `the preview strip shows prereform labels`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.labels.open()
        page.game.labels.onGroupOpened(TileLabelGroup.PREREFORM)

        page.game.labels.onSelected(TileLabels.PREREFORM_LETTERS)
        assertEquals(listOf("А", "Б", "В"), page.labels.preview)
        page.game.labels.onSelected(TileLabels.PREREFORM_LETTER_PAIRS)

        assertEquals(listOf("АА", "АБ", "АВ"), page.labels.preview)
    }

    // Saves and links
    @Test
    fun `a save keeps prereform letter pairs`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.newGame(5)
        page.choose(TileLabels.PREREFORM_LETTER_PAIRS)
        page.game.onSave()
        page.game.onSaveAnswered("foo")
        page.choose(TileLabels.PAIRS)

        page.game.onLoad()
        page.game.onLoadChosen("foo")

        assertEquals(TileLabels.PREREFORM_LETTER_PAIRS, page.state.labels)
        assertEquals("АА", page.textOf(1))
    }

    @Test
    fun `a 6x6 save keeps Ѳ and Ѵ`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.newGame(6)
        page.choose(TileLabels.PREREFORM_LETTERS)
        page.game.onSave()
        page.game.onSaveAnswered("six")
        page.choose(TileLabels.NUMBERS)

        page.game.onLoad()
        page.game.onLoadChosen("six")

        assertEquals(TileLabels.PREREFORM_LETTERS, page.state.labels)
        assertEquals("Ѳ", page.textOf(35))
        assertEquals("Ѵ", page.textOf(36))
    }

    @Test
    fun `a 7x7 save with prereform letters opens the dialog with its own message`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.saves.save("seven", rotatedRow(7), 7, 0, labels = "prereform_letters")

        page.game.onLoad()
        page.game.onLoadChosen("seven")

        assertEquals(statusTexts.prereformLettersDontFit(7), page.labels.message)
        assertEquals("Pre-reform Russian letters don't fit a 7x7 board (36 labels, 49 tiles). Please pick other labels.", page.labels.message)
        assertEquals(TileLabels.NUMBERS, page.labels.selected)
        assertEquals(
            listOf(mapOf("opened_from" to "letters_save", "screen" to "play", "current_labels" to "numbers")),
            page.ownProps("screen_labels_dialog"),
        )
    }

    @Test
    fun `a share link carries the prereform labels`() = webTest {
        val sender = WebHarness(backgroundScope).startOn()
        sender.choose(TileLabels.PREREFORM_LETTERS)
        sender.game.share.open()
        sender.game.share.onLabelsToggled()
        val link = (sender.state.dialog as WebDialog.Share).link
        assertTrue(link.endsWith("l=prereform_letters"), link)

        val receiver = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED, "prereform_letters")

        val prompt = assertIs<WebDialog.SharedLabels>(receiver.state.dialog)
        assertEquals(TileLabels.PREREFORM_LETTERS, prompt.labels)
        assertEquals(texts().dialogs.sharedLabelsTitle("Pre-reform Russian letters"), prompt.title)
    }

    @Test
    fun `a 7x7 link with prereform letters opens as a plain board`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(7, rotatedRow(7), "prereform_letters")

        assertEquals(7, page.state.side)
        assertEquals(TileLabels.NUMBERS, page.state.labels)
        assertTrue(page.state.dialog !is WebDialog.SharedLabels, "${page.state.dialog}")
    }

    @Test
    fun `the autosave slot is named for the scheme`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.newGame(6)
        page.choose(TileLabels.PREREFORM_LETTERS)
        page.game.onArrow(Arrow.RIGHT, 1)

        page.game.onPageLeft()

        assertTrue("Autosave_prereform_letters_6x6" in page.saves.listSaves(), "${page.saves.listSaves()}")
    }

    // Texts
    @Test
    fun `a german page names the prereform group and rows`() = webTest {
        val page = WebHarness(backgroundScope, browserLanguages = listOf("de-DE")).startOn()
        page.game.labels.open()

        page.game.labels.onGroupOpened(TileLabelGroup.PREREFORM)

        assertEquals("Russisch vor 1918", TileLabelGroup.PREREFORM.title)
        assertEquals(listOf("Buchstaben", "Paare"), page.labels.openOptions.map { it.title })
    }

    @Test
    fun `a russian page names the prereform group and rows`() = webTest {
        val page = WebHarness(backgroundScope, browserLanguages = listOf("ru-RU")).startOn()
        page.game.labels.open()

        page.game.labels.onGroupOpened(TileLabelGroup.PREREFORM)

        assertEquals("Дореформенный алфавит", TileLabelGroup.PREREFORM.title)
        assertEquals(listOf("Буквы", "Пары"), page.labels.openOptions.map { it.title })
    }

    @Test
    fun `the share toggle names prereform letter pairs in full`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.PREREFORM_LETTER_PAIRS)

        page.game.share.open()

        assertEquals("Include my tile labels (Pre-reform Russian letter pairs)", (page.state.dialog as WebDialog.Share).labelsToggle)
    }
}
