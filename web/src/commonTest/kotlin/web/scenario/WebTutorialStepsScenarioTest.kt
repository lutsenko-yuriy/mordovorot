package web.scenario

import texts.texts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import web.Arrow
import web.drag.Axis
import web.tutorial.TutorialScript
import web.tutorial.TutorialText

/** GH-288: each step advances on its shift. The board is `8 1 14 3 / 5 2 7 12 / 11 6 9 10 / 13 16 15 4`; the shifts are top row left 1, column 2 up 1, row 3 right 2, column 4 down 1. */
class WebTutorialStepsScenarioTest {
    private val tutorialTexts = texts().tutorial


    private fun text(page: WebHarness, step: Int) = TutorialText.step(step, computer = true, labels = page.state.labels)

    @Test
    fun `step 1 asks for the top row, one tile left`() = webTest {
        val page = tutorialPage(backgroundScope)

        assertEquals(text(page, 1), page.state.guidance?.text)
        assertTrue(page.state.guidance!!.text.startsWith("Hi there!"))
        assertEquals(Axis.ROW, page.state.guidance?.highlight?.axis)
        assertEquals(0, page.state.guidance?.highlight?.line)
        assertEquals(Arrow.LEFT, page.state.guidance?.highlight?.arrow)
    }

    @Test
    fun `step 1 advances on a drag`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.stepByDrag(1)

        assertEquals(listOf(1, 14, 3, 8), page.state.tiles.take(4))
        assertEquals(2, page.state.guidance?.step)
    }

    @Test
    fun `step 1 advances on an arrow click`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.stepByArrow(1)

        assertEquals(2, page.state.guidance?.step)
    }

    @Test
    fun `step 1 advances on a shift key`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.stepByKey(1)

        assertEquals(2, page.state.guidance?.step)
    }

    @Test
    fun `step 2 teaches the wrap`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.stepByArrow(1)
        assertEquals(14, page.state.tiles[1])
        assertEquals(tutorialTexts.step2("14", computer = true), page.state.guidance?.text)

        page.stepByArrow(2)

        assertEquals(14, page.state.tiles[3 * 4 + 1])
        assertEquals(3, page.state.guidance?.step)
    }

    @Test
    fun `step 3 advances on one drag of two tiles`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps(2)

        page.stepByDrag(3)

        assertEquals(4, page.state.guidance?.step)
    }

    @Test
    fun `step 3 advances on two ▶ clicks`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps(2)

        page.game.onArrow(Arrow.RIGHT, 2)
        assertEquals(3, page.state.guidance?.step)
        page.game.onArrow(Arrow.RIGHT, 2)

        assertEquals(4, page.state.guidance?.step)
    }

    @Test
    fun `step 3 advances on two ◀ clicks too`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps(2)

        page.game.onArrow(Arrow.LEFT, 2)
        assertEquals(3, page.state.guidance?.step)
        page.game.onArrow(Arrow.LEFT, 2)

        assertEquals(4, page.state.guidance?.step)
    }

    @Test
    fun `step 3 says once more after the first tile`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps(2)

        page.game.onArrow(Arrow.RIGHT, 2)

        assertEquals(tutorialTexts.again(3), page.state.guidance?.text)
    }

    @Test
    fun `Moves stays 3 through both tiles of step 3`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps(2)

        page.stepByArrow(3)

        assertEquals(3, page.state.moves)
    }

    // One scenario: splitting it would repeat the same steps.
    @Test
    fun `Moves reads 1, 2, 3, 4 through the four steps`() = webTest {
        val page = tutorialPage(backgroundScope)

        val moves = (1..4).map { page.stepByArrow(it); page.state.moves }

        assertEquals(listOf(1, 2, 3, 4), moves)
    }

    @Test
    fun `step 4 asks for column 4, one tile down, and solves the board`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps(3)
        assertTrue(page.state.guidance!!.text.startsWith("Last one:"))

        page.game.onArrow(Arrow.DOWN, 3)

        assertEquals(TUTORIAL_SOLVED, page.state.tiles)
        assertTrue(page.state.solved)
    }

    // A loop over inputs: the other scenarios cover each input one step at a time.
    @Test
    fun `the same four steps work by dragging, by arrows and by keys`() = webTest {
        val inputs = listOf<(WebHarness, Int) -> Unit>(
            { page, n -> page.stepByDrag(n) },
            { page, n -> page.stepByArrow(n) },
            { page, n -> page.stepByKey(n) },
        )
        for (input in inputs) {
            val page = tutorialPage(backgroundScope)

            (1..4).forEach { input(page, it) }

            assertEquals(TUTORIAL_SOLVED, page.state.tiles)
            assertEquals(TutorialScript.SOLVED_STEP, page.state.guidance?.step)
        }
    }

    @Test
    fun `Mordie searches the tutorial board like any game`() = webTest {
        val page = tutorialPage(backgroundScope)

        assertEquals(1, page.mordieSearches.started.size)
        assertNotNull(page.state.mordie)
    }
}
