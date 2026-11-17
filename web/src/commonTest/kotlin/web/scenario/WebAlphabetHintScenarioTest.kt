package web.scenario

import kotlinx.coroutines.CoroutineScope
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.SearchProgress
import mordie.SolutionMove
import texts.Language
import texts.useLanguage
import viewmodel.TileLabels
import web.AlphabetHintButton
import web.Arrow
import web.Focus
import web.PickerControl
import web.ToolbarButton
import web.WebDialog

/** GH-332: the alphabet hint, `[ Hint 8 ]` between Give Up and Tutorial (Tutorial moves to 9). */
class WebAlphabetHintScenarioTest {

    @AfterTest
    fun backToEnglish() = useLanguage(Language.EN)

    private val WebHarness.hint get() = assertIs<WebDialog.AlphabetHint>(state.dialog)

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

    /** A page on a 4x4 board marked with [labels]. */
    private fun labelled(scope: CoroutineScope, labels: TileLabels, touch: Boolean = false) =
        WebHarness(scope, touch = touch).startOn().also { it.choose(labels) }

    // The button
    @Test
    fun hintButtonIsLiveForEachLetterScheme() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        for (labels in listOf(
            TileLabels.LETTERS, TileLabels.LETTER_PAIRS, TileLabels.CYRILLIC_LETTERS, TileLabels.CYRILLIC_LETTER_PAIRS,
            TileLabels.GREEK_LETTERS, TileLabels.GREEK_LETTER_PAIRS,
        )) {
            page.choose(labels)
            assertEquals(AlphabetHintButton.LIVE, page.state.alphabetHint, labels.name)
        }
    }

    @Test
    fun hintButtonIsDimmedForNumberSchemes() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        for (labels in listOf(TileLabels.NUMBERS, TileLabels.NUMBERS_FROM_0, TileLabels.PAIRS, TileLabels.PAIRS_FROM_0)) {
            page.choose(labels)
            assertEquals(AlphabetHintButton.DIMMED, page.state.alphabetHint, labels.name)
        }
    }

    @Test
    fun hintButtonIsDimmedWhenLettersStandInAsNumbers() = webTest {
        val page = labelled(backgroundScope, TileLabels.LETTERS)

        page.newGame(6)

        assertEquals(6, page.state.side)
        assertEquals(AlphabetHintButton.DIMMED, page.state.alphabetHint)
    }

    @Test
    fun hintButtonIsHiddenOnTheSolutionAndDuringTheTutorial() = webTest {
        val original = intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)
        val given = WebHarness(backgroundScope).startOn(original).also { it.choose(TileLabels.LETTERS) }
        given.mordieSearches.latest.progress = SearchProgress(
            2, found = true, elapsedMs = 0, solution = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2)),
        )
        repeat(5) { given.game.onArrow(Arrow.UP, 3) }
        assertEquals(AlphabetHintButton.LIVE, given.state.alphabetHint)
        given.game.solution.onGiveUp()
        given.game.solution.onGiveUpAnswered(true)
        assertNotNull(given.state.solution)
        assertEquals(AlphabetHintButton.HIDDEN, given.state.alphabetHint)

        val replay = labelled(backgroundScope, TileLabels.LETTERS)
        replay.game.tutorial.onReplay("button")
        assertNotNull(replay.state.guidance)
        assertEquals(AlphabetHintButton.HIDDEN, replay.state.alphabetHint)
    }

    @Test
    fun hintButtonSurvivesAWinAndTheTutorialSwitch() = webTest {
        val won = labelled(backgroundScope, TileLabels.LETTERS)
        won.game.onArrow(Arrow.LEFT, 0)
        assertTrue(won.state.solved)
        assertEquals(AlphabetHintButton.LIVE, won.state.alphabetHint)

        val noTutorial = WebHarness(backgroundScope, tutorialOn = false).startOn()
        noTutorial.choose(TileLabels.LETTERS)
        assertFalse(noTutorial.state.tutorialButton)
        assertEquals(AlphabetHintButton.LIVE, noTutorial.state.alphabetHint)
    }

    @Test
    fun hintHasDigit8OnAComputerAndNoneOnTouch() = webTest {
        val computer = WebHarness(backgroundScope).startOn()
        val touch = WebHarness(backgroundScope, touch = true).startOn()

        assertEquals(8, computer.state.digitOf(ToolbarButton.ALPHABET_HINT))
        assertNull(touch.state.digitOf(ToolbarButton.ALPHABET_HINT))
    }

    // The dialog
    @Test
    fun hintOpensWithTheLatinAlphabet() = webTest {
        val page = labelled(backgroundScope, TileLabels.LETTERS)

        page.game.alphabetHint.open()

        assertEquals("Latin alphabet", page.hint.alphabetTitle)
        assertEquals(('A'..'Z').map { it.toString() }, page.hint.hint.letters)
        assertEquals(16, page.inUse())
        assertEquals(10, page.hint.hint.letters.size - page.inUse())
        assertEquals(Focus.DialogOk, page.state.focus)
    }

    @Test
    fun hintOpensWithTheFullGreekAlphabetOn5x5() = webTest {
        val page = labelled(backgroundScope, TileLabels.GREEK_LETTERS)
        page.newGame(5)

        page.game.alphabetHint.open()

        assertEquals(25, page.hint.hint.letters.size)
        assertEquals(25, page.inUse())
        assertEquals(0x37E, page.hint.hint.letters.last().single().code)
    }

    @Test
    fun greekPairsKeepTheQuestionMarkGrey() = webTest {
        val page = labelled(backgroundScope, TileLabels.GREEK_LETTER_PAIRS)
        page.newGame(5)

        page.game.alphabetHint.open()

        assertEquals(5, page.inUse())
        assertEquals(0x37E, page.hint.hint.letters.last().single().code)
        assertFalse(page.hint.inUse(24))
    }

    @Test
    fun hintDialogIsTitledInTheGameLanguage() = webTest {
        val page = labelled(backgroundScope, TileLabels.CYRILLIC_LETTERS)

        page.game.language.onLanguage(Language.DE, PickerControl.TOGGLES)
        page.game.alphabetHint.open()
        assertEquals("Kyrillisches Alphabet", page.hint.alphabetTitle)
        page.game.alphabetHint.onClosed()

        page.choose(TileLabels.GREEK_LETTERS)
        page.game.language.onLanguage(Language.RU, PickerControl.TOGGLES)
        page.game.alphabetHint.open()
        assertEquals("Греческий алфавит", page.hint.alphabetTitle)
    }

    @Test
    fun okAndEscapeCloseTheHintWithoutChangingTheGame() = webTest {
        val page = labelled(backgroundScope, TileLabels.LETTERS)
        page.game.onArrow(Arrow.DOWN, 1)
        page.game.onArrow(Arrow.UP, 2)
        val before = page.state
        assertEquals(2, before.moves)

        page.game.alphabetHint.open()
        page.game.alphabetHint.onClosed()
        assertNull(page.state.dialog)
        assertEquals(before.unfocused(), page.state.unfocused())

        page.game.alphabetHint.open()
        page.game.onEscape()
        assertNull(page.state.dialog)
        assertEquals(before.unfocused(), page.state.unfocused())
    }

    @Test
    fun dimmedHintOpensNothing() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.NUMBERS)
        page.analytics.events.clear()

        page.game.alphabetHint.open()
        assertNull(page.state.dialog)
        page.press("8")

        assertNull(page.state.dialog)
        assertTrue(page.props("screen_alphabet_hint_dialog").isEmpty())
    }

    // Keys
    @Test
    fun key8OpensTheHintAndKey9ReplaysTheTutorial() = webTest {
        val page = labelled(backgroundScope, TileLabels.LETTERS)

        assertTrue(page.press("8"))
        assertIs<WebDialog.AlphabetHint>(page.state.dialog)
        page.game.onEscape()
        assertTrue(page.press("9"))

        assertNotNull(page.state.guidance)
    }

    @Test
    fun keys8And9DoNothingDuringTheTutorial() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps(1)

        page.press("8", from = Focus.Board)
        assertNull(page.state.dialog)
        assertEquals(2, page.state.guidance?.step)
        page.press("9", from = Focus.Board)

        assertNull(page.state.dialog)
        assertEquals(2, page.state.guidance?.step)
    }

    // Focus
    @Test
    fun focusReturnsToHintAfterAClick() = webTest {
        val page = labelled(backgroundScope, TileLabels.LETTERS)

        page.game.alphabetHint.open()
        page.game.alphabetHint.onClosed()
        assertEquals(Focus.AlphabetHintButton, page.state.focus)

        assertTrue(page.press("8", from = Focus.AlphabetHintButton))
        assertIs<WebDialog.AlphabetHint>(page.state.dialog)
        page.game.onEscape()
        assertEquals(Focus.AlphabetHintButton.key, page.state.focus?.key)
    }

    @Test
    fun focusReturnsToTheBoardAfterKey8() = webTest {
        val page = labelled(backgroundScope, TileLabels.LETTERS)

        page.press("8", from = Focus.Board)
        page.game.onEscape()
        assertEquals(Focus.Board, page.state.focus)

        assertTrue(page.press("h", from = Focus.Board))
        assertEquals(1, page.state.moves)
    }

    // Analytics
    @Test
    fun openingTheHintSendsOneScreenView() = webTest {
        val page = labelled(backgroundScope, TileLabels.LETTERS)
        page.analytics.events.clear()

        page.game.alphabetHint.open()
        assertEquals("button", page.ownProps("screen_alphabet_hint_dialog").single()["opened_from"])
        page.game.alphabetHint.onClosed()
        page.press("8")

        assertEquals(listOf("button", "shortcut"), page.ownProps("screen_alphabet_hint_dialog").map { it["opened_from"] })
    }

    @Test
    fun closingTheHintSendsDialogCancelled() = webTest {
        val page = labelled(backgroundScope, TileLabels.LETTERS)
        page.analytics.events.clear()

        page.game.alphabetHint.open()
        page.game.onEscape()
        assertEquals(mapOf("dialog" to "alphabet_hint", "via" to "escape"), page.ownProps("dialog_cancelled").single())

        page.analytics.events.clear()
        page.game.alphabetHint.open()
        page.game.alphabetHint.onClosed()
        assertEquals(mapOf("dialog" to "alphabet_hint", "via" to "button"), page.ownProps("dialog_cancelled").single())
    }
}
