package web.scenario

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

    private fun WebHarness.ways() = state.guidance!!.ways

    private fun WebHarness.keyLine() = ways().last()

    private fun WebHarness.key(name: String) = press(name, from = Focus.Board)

    private fun TestScope.leftHanded() = WebHarness(backgroundScope, firstLaunch = true).remembering(KeyboardLayout.LEFT_HANDED).start()

    @Test
    fun `step 1 lists drag, click and keys`() = webTest {
        val page = tutorialPage(backgroundScope)

        assertEquals(TutorialText.step(1, computer = true, labels = page.state.labels), page.state.guidance?.text)
        assertEquals(listOf("Drag it left", "Click its ◀", "Press H or ←"), page.ways())
    }

    @Test
    fun `step 2 starts the key line with a select key`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.stepByArrow(1)

        assertEquals(listOf("Drag it up", "Click its ▲", "Press D, then K or ↑"), page.ways())
    }

    @Test
    fun `the key line names what is left after a select key`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.stepByArrow(1)

        page.key("d")

        assertEquals("Press K or ↑", page.keyLine())
    }

    @Test
    fun `a tile press redraws the key line`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.stepByArrow(1)

        page.game.onTilePressed(2, 1)

        assertEquals("Press K or ↑", page.keyLine())
    }

    @Test
    fun `a column far from the selection asks for at most three select keys`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.playSteps(3)

        assertEquals("Press D three times, then J or ↓", page.keyLine())
    }

    @Test
    fun `a row asks only for up and down select keys`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.playSteps(2)

        assertEquals("Press S twice, then L or → twice", page.keyLine())
    }

    @Test
    fun `the left hand's lines`() = webTest {
        val page = leftHanded()

        assertEquals("Press A", page.keyLine())
        page.stepByArrow(1)
        assertEquals("Press L or →, then W", page.keyLine())
    }

    @Test
    fun `OK in Controls changes the key line`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.game.tutorial.onControls()

        page.game.controls.onLayout(KeyboardLayout.LEFT_HANDED)
        assertEquals("Press H or ←", page.keyLine())
        page.game.controls.onAnswered(ok = true)

        assertEquals("Press A", page.keyLine())
    }

    @Test
    fun `Cancel in Controls keeps the key line`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.game.tutorial.onControls()
        page.game.controls.onLayout(KeyboardLayout.LEFT_HANDED)

        page.game.controls.onAnswered(ok = false)

        assertEquals("Press H or ←", page.keyLine())
    }

    @Test
    fun `a replay's step 1 starts with select keys when the selection is elsewhere`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.key("s")
        page.playSteps()
        waitPause()

        page.game.tutorial.onReplay("button")
        page.game.tutorial.onReplayAnswered(ReplaySaveChoice.DONT_SAVE)

        assertEquals("Press W, then H or ←", page.keyLine())
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
        assertEquals(listOf("Drag it left", "Click its ◀", "Press S twice, then H or ←"), page.ways())
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
