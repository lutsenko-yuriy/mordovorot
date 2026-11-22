package web.scenario

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import texts.Language
import texts.texts
import texts.useLanguage
import viewmodel.TileLabelGroup
import viewmodel.TileLabels
import web.AlphabetHintButton
import web.Focus
import web.PickerControl
import web.WebDialog
import web.tutorial.TutorialText

/** GH-354: reading the pre-reform labels aloud, their alphabet hint, the tutorial and the events. */
class WebPrereformReadingScenarioTest {

    @AfterTest
    fun backToEnglish() = useLanguage(Language.EN)

    private val WebHarness.hint get() = assertIs<WebDialog.AlphabetHint>(state.dialog)
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

    private fun WebHarness.inUse() = (0 until hint.hint.letters.size).count { hint.inUse(it) }

    /** Row 3 of the harness board is solved, and holds the 9th to 12th labels. */
    private val rowThree = "Иван, Иван краткий, и десятеричное, Константин"

    // Reading aloud
    @Test
    fun `read board spells the new letters on 4x4`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.PREREFORM_LETTERS)

        page.game.readBoard()

        assertTrue(rowThree in page.said.last(), page.said.last())
    }

    @Test
    fun `read board spells Ѣ Ѳ Ѵ on 6x6`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.newGame(6)
        page.choose(TileLabels.PREREFORM_LETTERS)

        page.game.readBoard()

        val said = page.said.last()
        for (word in listOf("ять", "фита", "ижица")) assertTrue(word in said, said)
    }

    @Test
    fun `a german page says the new letters the same`() = webTest {
        val page = WebHarness(backgroundScope, browserLanguages = listOf("de-DE")).startOn()
        page.choose(TileLabels.PREREFORM_LETTERS)

        page.game.readBoard()

        assertTrue(rowThree in page.said.last(), page.said.last())
    }

    @Test
    fun `a russian page says the new letters the same`() = webTest {
        val page = WebHarness(backgroundScope, browserLanguages = listOf("ru-RU")).startOn()
        page.choose(TileLabels.PREREFORM_LETTERS)

        page.game.readBoard()

        assertTrue(rowThree in page.said.last(), page.said.last())
    }

    @Test
    fun `read board spells a pair`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.PREREFORM_LETTER_PAIRS)

        page.game.readBoard()

        assertTrue("Анна Борис" in page.said.last(), page.said.last())
    }

    // The hint
    @Test
    fun `the hint is titled for the prereform alphabet`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.PREREFORM_LETTERS)

        page.game.alphabetHint.open()

        assertEquals("Pre-reform Russian alphabet", page.hint.alphabetTitle)
    }

    @Test
    fun `the hint is titled in German and Russian`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.PREREFORM_LETTERS)

        page.game.language.onLanguage(Language.DE, PickerControl.TOGGLES)
        page.game.alphabetHint.open()
        assertEquals("Russisches Alphabet vor 1918", page.hint.alphabetTitle)
        page.game.alphabetHint.onClosed()

        page.game.language.onLanguage(Language.RU, PickerControl.TOGGLES)
        page.game.alphabetHint.open()
        assertEquals("Дореформенный алфавит", page.hint.alphabetTitle)
    }

    @Test
    fun `the hint shows 36 letters, 16 in use on 4x4`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.PREREFORM_LETTERS)

        page.game.alphabetHint.open()

        assertEquals(36, page.hint.hint.letters.size)
        assertEquals(16, page.inUse())
    }

    @Test
    fun `on 6x6 all 36 letters are in use`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.newGame(6)
        page.choose(TileLabels.PREREFORM_LETTERS)

        page.game.alphabetHint.open()

        assertEquals(36, page.inUse())
    }

    @Test
    fun `for pairs on 9x9 nine letters are in use`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.newGame(9)
        page.choose(TileLabels.PREREFORM_LETTER_PAIRS)

        page.game.alphabetHint.open()

        assertEquals(9, page.inUse())
    }

    @Test
    fun `the hint is dimmed on 7x7 with prereform letters`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.PREREFORM_LETTERS)

        page.newGame(7)

        assertEquals(AlphabetHintButton.DIMMED, page.state.alphabetHint)
    }

    @Test
    fun `a greyed Ѳ is said not on this board`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.PREREFORM_LETTERS)

        page.game.alphabetHint.open()

        val index = TileLabels.PREREFORM_ALPHABET.indexOf('Ѳ')
        assertEquals(false, page.hint.inUse(index))
        assertEquals(texts().reader.notOnBoard("фита"), page.hint.letterName(index))
    }

    // The tutorial
    @Test
    fun `tutorial step 2 names its tile М`() = webTest {
        val first = WebHarness(backgroundScope, firstLaunch = true)
        first.preferences.set("tile_labels", "prereform_letters")
        val page = first.start()
        page.stepByArrow(1)

        assertEquals(2, page.state.guidance?.step)
        assertEquals(TutorialText.step(2, computer = true, labels = TileLabels.PREREFORM_LETTERS), page.state.guidance?.text)
        assertTrue("М" in page.state.guidance!!.text, page.state.guidance!!.text)
    }

    @Test
    fun `the tutorial's labels step opens the prereform group`() = webTest {
        val first = WebHarness(backgroundScope, firstLaunch = true)
        first.preferences.set("tile_labels", "prereform_letters")
        val page = first.start()

        page.game.tutorial.onLabels()
        assertEquals(TileLabelGroup.PREREFORM, page.labels.open)
        page.game.labels.onAnswered(ok = false)

        assertEquals(Focus.TutorialLabels, page.state.focus)
    }

    // Events
    @Test
    fun `choosing prereform letters sends tile_labels_changed`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.choose(TileLabels.PREREFORM_LETTERS)

        assertEquals("prereform_letters", page.props("tile_labels_changed").single()["to"])
    }

    @Test
    fun `the 7x7 fallback is reported as letters_unavailable`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.saves.save("seven", rotatedRow(7), 7, 0, labels = "prereform_letters")
        page.game.onLoad()
        page.game.onLoadChosen("seven")

        page.game.labels.onSelected(TileLabels.PAIRS)
        page.game.labels.onAnswered(ok = true)

        assertEquals("letters_unavailable", page.props("tile_labels_changed").last()["from_source"])
    }
}
