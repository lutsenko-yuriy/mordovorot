package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import mordie.SearchProgress
import mordie.SolutionMove
import viewmodel.GiveUpButton
import web.Arrow
import web.Focus
import web.ReplaySaveChoice
import web.ToolbarButton
import web.WebDialog
import web.tutorial.TutorialScript

/** GH-289: the Tutorial button, key 8, the save question, and Skip and the end of a replay. */
class WebTutorialReplayScenarioTest {

    /** 4x4 two moves from solved: row 0 right once, then column 1 down twice. */
    private val original = intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)
    private val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2))

    /** A different board, for a share link. */
    private val linked = intArrayOf(1, 2, 3, 0) + IntArray(12) { it + 4 }

    private fun TestScope.fresh() = WebHarness(backgroundScope).startOn()

    /** A game with one move made: unsaved progress. */
    private fun TestScope.mid() = fresh().also { it.game.onArrow(Arrow.RIGHT, 1) }

    /** [mid], with the save question open. */
    private fun TestScope.asked() = mid().also { it.game.tutorial.onReplay("button") }

    private fun TestScope.won(mordieFound: Boolean): WebHarness {
        val page = WebHarness(backgroundScope).startOn(original)
        if (mordieFound) page.mordieSearches.latest.progress = SearchProgress(path.size, found = true, elapsedMs = 0, solution = path)
        page.game.onArrow(Arrow.RIGHT, 0)
        repeat(2) { page.game.onArrow(Arrow.DOWN, 1) }
        return page
    }

    private fun TestScope.givenUp(): WebHarness {
        val page = WebHarness(backgroundScope).startOn(original)
        page.mordieSearches.latest.progress = SearchProgress(path.size, found = true, elapsedMs = 0, solution = path)
        repeat(5) { page.game.onArrow(Arrow.UP, 3) }
        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(true)
        return page
    }

    private fun WebHarness.assertTutorialStep1() {
        assertEquals(shown(TutorialScript.board), state.tiles)
        assertEquals(1, state.guidance?.step)
    }

    // The button
    @Test
    fun `Tutorial shows beside Give Up mid-game`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original)
        page.game.onArrow(Arrow.RIGHT, 0)

        assertTrue(page.state.tutorialButton)
        assertEquals(GiveUpButton.DIMMED, page.state.giveUp)
    }

    @Test
    fun `Tutorial shows beside Mordie's solution on a solved board`() = webTest {
        val page = won(mordieFound = true)

        assertTrue(page.state.tutorialButton)
        assertEquals(GiveUpButton.MORDIES_SOLUTION, page.state.giveUp)
    }

    @Test
    fun `Tutorial stands alone on a solved board with no solution to show`() = webTest {
        val page = won(mordieFound = false)

        assertEquals(GiveUpButton.HIDDEN, page.state.giveUp)
        assertTrue(page.state.tutorialButton)
    }

    @Test
    fun `Tutorial is hidden on the solution screen`() = webTest {
        val page = givenUp()

        assertNotNull(page.state.solution)
        assertFalse(page.state.tutorialButton)
    }

    @Test
    fun `Tutorial is hidden during steps 1 to 4`() = webTest {
        val page = tutorialPage(backgroundScope)
        assertFalse(page.state.tutorialButton)

        page.playSteps(3)

        assertFalse(page.state.tutorialButton)
    }

    @Test
    fun `Tutorial is hidden at step 5`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.playSteps()

        assertEquals(5, page.state.guidance?.step)
        assertFalse(page.state.tutorialButton)
    }

    @Test
    fun `Tutorial is back at step 6`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps()

        waitPause()

        assertNull(page.state.guidance)
        assertTrue(page.state.tutorialButton)
    }

    @Test
    fun `Tutorial shows no digit on touch`() = webTest {
        val touch = WebHarness(backgroundScope, touch = true).startOn()
        val computer = fresh()

        assertTrue(touch.state.tutorialButton)
        assertNull(touch.state.digitOf(ToolbarButton.TUTORIAL))
        assertEquals(8, computer.state.digitOf(ToolbarButton.TUTORIAL))
    }

    @Test
    fun `Tutorial is hidden when the tutorial switch is off`() = webTest {
        val page = WebHarness(backgroundScope, tutorialOn = false).startOn()

        assertFalse(page.state.tutorialButton)
        page.press("8")
        assertNull(page.state.guidance)
    }

    // Key 8
    @Test
    fun `8 starts the replay when there is no unsaved progress`() = webTest {
        val page = fresh()

        assertTrue(page.press("8"))

        page.assertTutorialStep1()
    }

    @Test
    fun `8 does nothing on touch`() = webTest {
        val page = WebHarness(backgroundScope, touch = true).startOn()
        val before = page.state

        assertFalse(page.press("8"))

        assertEquals(before, page.state)
    }

    @Test
    fun `8 does nothing during the tutorial`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps(1)

        page.press("8", from = Focus.Board)

        assertEquals(2, page.state.guidance?.step)
        assertNull(page.state.dialog)
    }

    @Test
    fun `8 does nothing on the solution screen`() = webTest {
        val page = givenUp()
        val before = page.state

        page.press("8", from = Focus.Board)

        assertEquals(before, page.state)
    }

    // A replay with no unsaved progress
    @Test
    fun `the replay starts at once`() = webTest {
        val page = fresh()

        page.game.tutorial.onReplay("button")

        assertNull(page.state.dialog)
        page.assertTutorialStep1()
    }

    @Test
    fun `the replay leaves saves untouched`() = webTest {
        val page = fresh().seed("foo", solved(4))
        val before = page.store.entries.toMap()

        page.game.tutorial.onReplay("button")

        assertEquals(before, page.store.entries.toMap())
    }

    @Test
    fun `the replay clears the current save name`() = webTest {
        val page = fresh().seed("foo", solved(4))
        page.game.onLoad()
        page.game.onLoadChosen("foo")

        page.game.tutorial.onReplay("button")
        page.game.onSave()

        assertEquals("", (page.state.dialog as WebDialog.Save).name)
    }

    @Test
    fun `focus goes to the board on a computer`() = webTest {
        val page = fresh()

        page.game.tutorial.onReplay("button")

        assertEquals(Focus.Board, page.state.focus)
    }

    @Test
    fun `focus goes to the page start on touch`() = webTest {
        val page = WebHarness(backgroundScope, touch = true).startOn()

        page.game.tutorial.onReplay("button")

        assertEquals(Focus.PageStart, page.state.focus)
    }

    // The save question
    @Test
    fun `with unsaved progress, Tutorial opens the save question`() = webTest {
        val page = mid()

        page.game.tutorial.onReplay("button")

        assertIs<WebDialog.ReplaySave>(page.state.dialog)
        assertNull(page.state.guidance)
        assertEquals(1, page.state.moves)
    }

    @Test
    fun `the question opens on Cancel`() = webTest {
        val page = asked()

        assertEquals(Focus.DialogCancel, page.state.focus)
    }

    @Test
    fun `Save opens the Save dialog`() = webTest {
        val page = fresh().seed("foo", ONE_SHIFT_FROM_SOLVED)
        page.game.onLoad()
        page.game.onLoadChosen("foo")
        page.game.onArrow(Arrow.RIGHT, 1)
        page.game.tutorial.onReplay("button")

        page.game.tutorial.onReplayAnswered(ReplaySaveChoice.SAVE)

        assertEquals("foo", (page.state.dialog as WebDialog.Save).name)
    }

    @Test
    fun `a successful save starts the replay`() = webTest {
        val page = asked()
        page.game.tutorial.onReplayAnswered(ReplaySaveChoice.SAVE)

        page.game.onSaveAnswered("mine")

        assertEquals(listOf("mine"), page.saves.listSaves())
        assertNull(page.state.dialog)
        page.assertTutorialStep1()
    }

    @Test
    fun `a cancelled save starts nothing`() = webTest {
        val page = mid()
        val before = page.state.unfocused()
        page.game.tutorial.onReplay("button")
        page.game.tutorial.onReplayAnswered(ReplaySaveChoice.SAVE)

        page.game.onSaveAnswered(null)

        assertEquals(before, page.state.unfocused())
        assertEquals(Focus.TutorialReplay, page.state.focus)
    }

    @Test
    fun `a name the save rejects starts nothing`() = webTest {
        val page = asked()
        page.game.tutorial.onReplayAnswered(ReplaySaveChoice.SAVE)

        page.game.onSaveAnswered("two words")

        assertNull(page.state.guidance)
        assertNull(page.state.dialog)
        assertEquals(1, page.state.moves)
        assertTrue(page.saves.listSaves().isEmpty())
    }

    @Test
    fun `Don't save starts the replay`() = webTest {
        val page = asked()

        page.game.tutorial.onReplayAnswered(ReplaySaveChoice.DONT_SAVE)

        page.assertTutorialStep1()
        assertTrue(page.saves.listSaves().isEmpty())
    }

    @Test
    fun `Cancel starts nothing`() = webTest {
        val page = mid()
        val before = page.state.unfocused()
        page.game.tutorial.onReplay("button")

        page.game.tutorial.onReplayAnswered(ReplaySaveChoice.CANCEL)

        assertEquals(before, page.state.unfocused())
        assertEquals(Focus.TutorialReplay, page.state.focus)
    }

    @Test
    fun `Escape does what Cancel does`() = webTest {
        val page = mid()
        val before = page.state.unfocused()
        page.game.tutorial.onReplay("button")

        page.game.onEscape()

        assertEquals(before, page.state.unfocused())
        assertEquals(Focus.TutorialReplay, page.state.focus)
    }

    // Skip and the end of a replay
    @Test
    fun `Skip in a replay opens the restore prompt when there are saves`() = webTest {
        val page = fresh().seed("foo", solved(4))
        page.game.tutorial.onReplay("button")

        page.game.tutorial.onSkip()

        assertTrue((page.state.dialog as WebDialog.Load).restore)
    }

    @Test
    fun `Skip in a replay opens the size picker when there are no saves`() = webTest {
        val page = fresh()
        page.game.tutorial.onReplay("button")

        page.game.tutorial.onSkip()

        assertIs<WebDialog.Size>(page.state.dialog)
    }

    @Test
    fun `cancelling that picker keeps the tutorial board`() = webTest {
        val page = fresh()
        page.game.tutorial.onReplay("button")
        page.game.tutorial.onSkip()

        page.game.onSizeChosen(null)

        assertEquals(shown(TutorialScript.board), page.state.tiles)
        assertNull(page.state.dialog)
        assertNull(page.state.guidance)
    }

    @Test
    fun `Skip in a replay opens a share link waiting in the address`() = webTest {
        val page = fresh()
        page.game.tutorial.onReplay("button")
        page.changeLink(4, linked)

        page.game.tutorial.onSkip()

        assertEquals(shown(linked), page.state.tiles)
        assertNull(page.state.dialog)
    }

    @Test
    fun `finishing a replay opens no restore and no picker`() = webTest {
        val page = fresh().seed("foo", solved(4))
        page.game.tutorial.onReplay("button")
        page.playSteps()

        waitPause()

        assertNull(page.state.dialog)
        assertNull(page.state.guidance)
        assertEquals(TUTORIAL_SOLVED, page.state.tiles)
    }

    @Test
    fun `the leave-site prompt is off during the replay`() = webTest {
        val page = fresh()
        page.game.tutorial.onReplay("button")

        page.stepByArrow(1)

        assertFalse(page.game.hasUnsavedProgress())
    }
}
