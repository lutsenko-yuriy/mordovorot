package web.scenario

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import texts.Language
import texts.texts
import texts.useLanguage
import viewmodel.TileLabelGroup
import viewmodel.TileLabels
import web.Focus
import web.WebDialog

/** GH-323: the Tile labels dialog's groups and preview strip. */
class WebLabelGroupsScenarioTest {
    @AfterTest
    fun backToEnglish() = useLanguage(Language.EN)

    private val WebHarness.labels get() = state.dialog as WebDialog.Labels

    private fun WebHarness.openDialog() = game.labels.open()

    // Opening
    @Test
    fun `the dialog opens with the group in use open`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.openDialog()

        assertEquals(listOf("Numbers", "Numbers from 0", "Latin letters", "Cyrillic letters", "Greek letters"), TileLabelGroup.entries.map { it.title })
        assertEquals(TileLabelGroup.NUMBERS, page.labels.open)
        assertEquals(Focus.Label(TileLabels.NUMBERS), page.state.focus)
        assertEquals("label-numbers", page.state.focus?.key)
    }

    @Test
    fun `the preview strip shows the marked scheme's first three labels`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.openDialog()

        assertEquals(listOf("1", "2", "3"), page.labels.preview)
    }

    @Test
    fun `opening sends one screen view and no group event`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.openDialog()
        assertEquals(1, page.props("screen_labels_dialog").size)
        val before = page.analytics.events.size
        page.game.labels.onGroupOpened(TileLabelGroup.LATIN)

        assertEquals(before, page.analytics.events.size)
    }

    @Test
    fun `a Cyrillic letter pairs preference opens the Cyrillic group`() = webTest {
        val page = WebHarness(backgroundScope).apply { preferences.set("tile_labels", "cyrillic_letter_pairs") }.startOn()

        page.openDialog()

        assertEquals(TileLabelGroup.CYRILLIC, page.labels.open)
        assertEquals(Focus.Label(TileLabels.CYRILLIC_LETTER_PAIRS), page.state.focus)
        assertEquals(listOf("АА", "АБ", "АВ"), page.labels.preview)
    }

    // Opening a group
    @Test
    fun `opening a group focuses its first option when nothing there is marked`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.openDialog()

        page.game.labels.onGroupOpened(TileLabelGroup.LATIN)

        assertEquals(TileLabelGroup.LATIN, page.labels.open)
        assertEquals(Focus.Label(TileLabels.LETTERS), page.state.focus)
        assertEquals(TileLabels.NUMBERS, page.labels.selected)
    }

    @Test
    fun `opening the open group again does nothing`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.openDialog()
        val before = page.state

        page.game.labels.onGroupOpened(TileLabelGroup.NUMBERS)

        assertEquals(before, page.state)
    }

    @Test
    fun `on 6x6 Letters is dimmed and focus lands on Letter pairs`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onNew()
        page.game.onSizeChosen(6)
        page.openDialog()

        page.game.labels.onGroupOpened(TileLabelGroup.LATIN)

        assertEquals(Focus.Label(TileLabels.LETTER_PAIRS), page.state.focus)
        val letters = page.labels.options.single { it.labels == TileLabels.LETTERS }
        assertEquals("up to 5x5", letters.limit)
        assertEquals(false, letters.available)
    }

    // The mark and the strip
    @Test
    fun `a mark in a closed group survives OK`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.openDialog()
        page.game.labels.onGroupOpened(TileLabelGroup.LATIN)
        page.game.labels.onSelected(TileLabels.LETTER_PAIRS)

        page.game.labels.onGroupOpened(TileLabelGroup.NUMBERS)
        assertEquals(listOf("AA", "AB", "AC"), page.labels.preview)
        page.game.labels.onAnswered(ok = true)

        assertEquals(TileLabels.LETTER_PAIRS, page.state.labels)
        assertEquals(page.state.tiles.map { TileLabels.LETTER_PAIRS.label(it - 1, 4) }, page.state.tileTexts)
    }

    @Test
    fun `Cancel drops a mark in another group`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.openDialog()
        page.game.labels.onGroupOpened(TileLabelGroup.LATIN)
        page.game.labels.onSelected(TileLabels.LETTER_PAIRS)

        page.game.labels.onAnswered(ok = false)

        assertNull(page.state.dialog)
        assertEquals(TileLabels.NUMBERS, page.state.labels)
        assertEquals(listOf<Any?>("labels"), page.props("dialog_cancelled").map { it["dialog"] })
    }

    @Test
    fun `the strip follows the mark`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.openDialog()

        page.game.labels.onGroupOpened(TileLabelGroup.CYRILLIC)
        page.game.labels.onSelected(TileLabels.CYRILLIC_LETTERS)
        assertEquals(listOf("А", "Б", "В"), page.labels.preview)
        page.game.labels.onGroupOpened(TileLabelGroup.NUMBERS_FROM_0)
        page.game.labels.onSelected(TileLabels.PAIRS_FROM_0)

        assertEquals(listOf("00", "01", "02"), page.labels.preview)
    }

    @Test
    fun `the strip is the same on every board size`() = webTest {
        for (side in listOf(3, 4, 9)) {
            val page = WebHarness(backgroundScope).startOn()
            page.game.onNew()
            page.game.onSizeChosen(side)
            page.openDialog()

            page.game.labels.onSelected(TileLabels.PAIRS)

            assertEquals(listOf("11", "12", "13"), page.labels.preview, "$side")
        }
    }

    @Test
    fun `options show names only`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.openDialog()

        assertTrue(page.labels.options.all { it.limit == null && it.title.isNotBlank() })
    }

    // Layout and names
    @Test
    fun `every group names its two schemes Numbers or Letters, and Pairs`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.openDialog()

        val titles = TileLabelGroup.entries.map { group ->
            page.game.labels.onGroupOpened(group)
            page.labels.openOptions.map { it.title }
        }

        assertEquals(
            listOf(
                listOf("Numbers", "Pairs"),
                listOf("Numbers", "Pairs"),
                listOf("Letters", "Pairs"),
                listOf("Letters", "Pairs"),
                listOf("Letters", "Pairs"),
            ),
            titles,
        )
    }

    @Test
    fun `German and Russian pages name the groups`() = webTest {
        val german = WebHarness(backgroundScope, browserLanguages = listOf("de-DE")).startOn()
        german.openDialog()
        assertEquals(listOf("Zahlen", "Zahlen ab 0", "Lateinische Buchstaben", "Kyrillische Buchstaben", "Griechische Buchstaben"), TileLabelGroup.entries.map { it.title })
        assertEquals("Zahlen", german.labels.openOptions.first().title)

        val russian = WebHarness(backgroundScope, browserLanguages = listOf("ru-RU")).startOn()
        russian.openDialog()
        assertEquals(listOf("Числа", "Числа с 0", "Латинские буквы", "Кириллические буквы", "Греческие буквы"), TileLabelGroup.entries.map { it.title })
        assertEquals(texts().page.labelNumbers, russian.labels.openOptions.first().title)
    }

    @Test
    fun `a closed group that holds the mark says so in its header name`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.openDialog()
        page.game.labels.onGroupOpened(TileLabelGroup.LATIN)
        page.game.labels.onSelected(TileLabels.LETTER_PAIRS)

        page.game.labels.onGroupOpened(TileLabelGroup.NUMBERS)

        assertEquals("Latin letters, marked: Pairs", page.labels.headerName(TileLabelGroup.LATIN))
        assertEquals("Numbers", page.labels.headerName(TileLabelGroup.NUMBERS))
        assertEquals("Cyrillic letters", page.labels.headerName(TileLabelGroup.CYRILLIC))
    }

    // Other ways in
    @Test
    fun `a load that asks for other labels opens Numbers`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.saves.save("six", rotatedRow(6), 6, 0, labels = "letters")

        page.game.onLoad()
        page.game.onLoadChosen("six")

        assertTrue(page.labels.message != null)
        assertEquals(TileLabelGroup.NUMBERS, page.labels.open)
        assertEquals(Focus.Label(TileLabels.NUMBERS), page.state.focus)
    }

    @Test
    fun `the tutorial's Labels opens the group in use and Cancel goes back`() = webTest {
        val first = WebHarness(backgroundScope, firstLaunch = true)
        first.preferences.set("tile_labels", "cyrillic_letters")
        val page = first.start()

        page.game.tutorial.onLabels()
        assertEquals(TileLabelGroup.CYRILLIC, page.labels.open)
        page.game.labels.onAnswered(ok = false)

        assertNull(page.state.dialog)
        assertEquals(Focus.TutorialLabels, page.state.focus)
    }
}
