package web.scenario

import texts.texts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.SearchProgress
import mordie.SolutionMove
import viewmodel.GiveUpButton
import viewmodel.TileLabels
import web.Focus
import web.fallbackFocus
import web.WebDialog
import web.tutorial.TutorialScript
import web.tutorial.TutorialText

/** GH-288: steps 5 and 6, the game after the tutorial, the phone variant and the labels. */
class WebTutorialEndingScenarioTest {
    private val mordieTexts = texts().mordie
    private val tutorialTexts = texts().tutorial


    /** Mordie solves the board in four moves too. */
    private fun WebHarness.mordieFoundFour() {
        mordieSearches.latest.progress = SearchProgress(4, found = true, elapsedMs = 0, solution = List(4) { SolutionMove(isRow = true, index = 0, amount = 1) })
    }

    @Test
    fun `step 5 says Can you beat my results over the solved board`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.playSteps()

        assertTrue(page.state.solved)
        assertEquals(tutorialTexts.step5, page.state.guidance?.text)
    }

    @Test
    fun `step 5 holds Mordie's ending back`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.mordieFoundFour()

        page.playSteps()

        assertNull(page.state.mordie)
    }

    @Test
    fun `step 6 comes after 3 seconds`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.mordieFoundFour()
        page.playSteps()

        waitPause()

        assertNull(page.state.guidance)
        assertEquals(GiveUpButton.MORDIES_SOLUTION, page.state.giveUp)
    }

    @Test
    fun `Mordie's ending shows at step 6`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.mordieFoundFour()
        page.playSteps()

        waitPause()

        assertEquals(mordieTexts.endingMatched(4), page.state.mordie)
    }

    @Test
    fun `Save works after step 6`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps()
        waitPause()

        page.game.onSave()
        page.game.onSaveAnswered("first")

        assertEquals(listOf("first"), page.saves.listSaves())
    }

    @Test
    fun `Load works after step 6`() = webTest {
        val page = tutorialPage(backgroundScope).seed("foo", IntArray(9) { 8 - it })
        page.playSteps()
        waitPause()

        page.game.onLoad()
        page.game.onLoadChosen("foo")

        assertEquals(9, page.state.tiles.size)
    }

    @Test
    fun `New works after step 6`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps()
        waitPause()

        page.game.onNew()
        page.game.onSizeChosen(5)

        assertEquals(25, page.state.tiles.size)
    }

    @Test
    fun `on a phone, the texts name dragging only`() = webTest {
        val page = tutorialPage(backgroundScope, touch = true)
        val first = page.state.guidance!!
        assertEquals(tutorialTexts.step1(computer = false), first.text)
        assertTrue(!first.text.contains("◀"))

        page.stepByDrag(1)

        assertEquals(TutorialText.step(2, computer = false, labels = page.state.labels), page.state.guidance?.text)
            }

    @Test
    fun `on a phone, the panel has no Controls button`() = webTest {
        val page = tutorialPage(backgroundScope, touch = true)

        assertEquals(false, page.state.guidance?.controls)
        assertNull(page.state.guidance?.highlight?.arrow)
    }

    @Test
    fun `a Labels change renames the tile in step 2's text`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.game.tutorial.onLabels()
        assertIs<WebDialog.Labels>(page.state.dialog)
        page.game.labels.onSelected(TileLabels.LETTERS)
        page.game.labels.onAnswered(true)

        page.stepByArrow(1)

        val tile = TileLabels.LETTERS.label(TutorialScript.WRAPPING_TILE, TutorialScript.SIDE)
        assertTrue(page.state.guidance!!.text.contains("see tile $tile at the top"))
    }

    @Test
    fun `a dialog from the panel still open when step 6 comes hands its focus to what a won board offers`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps()
        page.game.tutorial.onLabels()

        waitPause()
        page.game.labels.onAnswered(false)

        val focus = checkNotNull(page.state.focus)
        assertTrue(focus.fallsBack, "the page looks for the gone panel button, then falls back")
        assertEquals(Focus.NewButton, fallbackFocus(focus.key, page.state))
    }
}
