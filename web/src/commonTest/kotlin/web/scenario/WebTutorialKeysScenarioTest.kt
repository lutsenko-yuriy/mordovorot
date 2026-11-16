package web.scenario

import texts.texts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import viewmodel.KeyboardLayout
import web.Arrow
import web.Focus
import web.KeyHints
import web.ReplaySaveChoice
import web.tutorial.TutorialText

/** GH-290: on a computer, the tutorial's steps list the ways to do the asked shift: drag, click, keys. */
class WebTutorialKeysScenarioTest {
    private val tutorialTexts = texts().tutorial

    private fun or(letter: String, arrow: String) = texts().keys.letterOrArrow(letter, arrow)

    private fun WebHarness.ways() = state.guidance!!.ways

    private fun WebHarness.keyLine() = ways().last()

    private fun WebHarness.key(name: String) = press(name, from = Focus.Board)

    private fun TestScope.leftHanded() = WebHarness(backgroundScope, firstLaunch = true).remembering(KeyboardLayout.LEFT_HANDED).start()

    @Test
    fun `step 1 lists drag, click and keys`() = webTest {
        val page = tutorialPage(backgroundScope)

        assertEquals(TutorialText.step(1, computer = true, labels = page.state.labels), page.state.guidance?.text)
        assertEquals(listOf(tutorialTexts.dragLeft(1), tutorialTexts.click("◀", 1), tutorialTexts.press(or("H", "←"), 1)), page.ways())
    }

    @Test
    fun `step 2 starts the key line with a select key`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.stepByArrow(1)

        assertEquals(listOf(tutorialTexts.dragUp(1), tutorialTexts.click("▲", 1), tutorialTexts.pressThen("D", 1, or("K", "↑"), 1)), page.ways())
    }

    @Test
    fun `the key line names what is left after a select key`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.stepByArrow(1)

        page.key("d")

        assertEquals(tutorialTexts.press(or("K", "↑"), 1), page.keyLine())
    }

    @Test
    fun `a tile press redraws the key line`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.stepByArrow(1)

        page.game.onTilePressed(2, 1)

        assertEquals(tutorialTexts.press(or("K", "↑"), 1), page.keyLine())
    }

    @Test
    fun `a column far from the selection asks for at most three select keys`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.playSteps(3)

        assertEquals(tutorialTexts.pressThen("D", 3, or("J", "↓"), 1), page.keyLine())
    }

    @Test
    fun `a row asks only for up and down select keys`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.playSteps(2)

        assertEquals(tutorialTexts.pressThen("S", 2, or("L", "→"), 2), page.keyLine())
    }

    @Test
    fun `the left hand's lines`() = webTest {
        val page = leftHanded()

        assertEquals(tutorialTexts.press("A", 1), page.keyLine())
        page.stepByArrow(1)
        assertEquals(tutorialTexts.pressThen(or("L", "→"), 1, "W", 1), page.keyLine())
    }

    @Test
    fun `OK in Controls changes the key line`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.game.tutorial.onControls()

        page.game.controls.onLayout(KeyboardLayout.LEFT_HANDED)
        assertEquals(tutorialTexts.press(or("H", "←"), 1), page.keyLine())
        page.game.controls.onAnswered(ok = true)

        assertEquals(tutorialTexts.press("A", 1), page.keyLine())
    }

    @Test
    fun `Cancel in Controls keeps the key line`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.game.tutorial.onControls()
        page.game.controls.onLayout(KeyboardLayout.LEFT_HANDED)

        page.game.controls.onAnswered(ok = false)

        assertEquals(tutorialTexts.press(or("H", "←"), 1), page.keyLine())
    }

    @Test
    fun `a replay's step 1 starts with select keys when the selection is elsewhere`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.key("s")
        page.playSteps()
        waitPause()

        page.game.tutorial.onReplay("button")
        page.game.tutorial.onReplayAnswered(ReplaySaveChoice.DONT_SAVE)

        assertEquals(tutorialTexts.pressThen("W", 1, or("H", "←"), 1), page.keyLine())
    }

    @Test
    fun `the lines stay under a blocked message`() = webTest {
        val page = tutorialPage(backgroundScope)
        val before = page.ways()

        page.game.onArrow(Arrow.LEFT, 1)

        assertEquals(TutorialText.blocked(web.drag.Axis.ROW), page.state.guidance?.text)
        assertEquals(before, page.ways())
    }

    @Test
    fun `the lines stay under Once more, naming the way begun`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps(2)

        page.game.onArrow(Arrow.LEFT, 2)

        assertEquals(TutorialText.again(3), page.state.guidance?.text)
        assertEquals(listOf(tutorialTexts.dragLeft(1), tutorialTexts.click("◀", 1), tutorialTexts.pressThen("S", 2, or("H", "←"), 1)), page.ways())
    }

    @Test
    fun `a phone shows no lines`() = webTest {
        val page = tutorialPage(backgroundScope, touch = true)

        assertEquals(TutorialText.step(1, computer = false, labels = page.state.labels), page.state.guidance?.text)
        assertTrue(page.state.guidance!!.ways.isEmpty())
    }

    @Test
    fun `the Key hints are unchanged through the tutorial`() = webTest {
        val page = tutorialPage(backgroundScope)

        (1..4).forEach {
            assertEquals(KeyHints.board(KeyboardLayout.RIGHT_HANDED), page.state.hints)
            page.stepByArrow(it)
        }
        assertEquals(KeyHints.board(KeyboardLayout.RIGHT_HANDED), page.state.hints)
    }
}
