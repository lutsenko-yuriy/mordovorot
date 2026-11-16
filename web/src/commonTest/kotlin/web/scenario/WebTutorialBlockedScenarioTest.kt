package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import web.Arrow
import web.Focus
import web.WebDialog
import web.drag.Axis
import web.tutorial.TutorialScript
import web.tutorial.TutorialText

/** GH-288: shifts that aren't asked for, and the keys and buttons during steps 1 to 5. */
class WebTutorialBlockedScenarioTest {

    private val rowLine = "You can do that later. For now shift that row please!"
    private val columnLine = "You can do that later. For now shift that column please!"

    @Test
    fun `another row is blocked`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.game.onArrow(Arrow.LEFT, 1)

        assertEquals(shown(TutorialScript.board), page.state.tiles)
        assertEquals(0, page.state.moves)
        assertEquals(rowLine, page.state.guidance?.text)
    }

    @Test
    fun `a column on step 1 is blocked`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.game.onArrow(Arrow.UP, 1)

        assertEquals(shown(TutorialScript.board), page.state.tiles)
        assertEquals(rowLine, page.state.guidance?.text)
    }

    @Test
    fun `the wrong direction is blocked`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.game.onArrow(Arrow.RIGHT, 0)

        assertEquals(shown(TutorialScript.board), page.state.tiles)
        assertEquals(rowLine, page.state.guidance?.text)
    }

    @Test
    fun `a drag of two tiles on step 1 is blocked`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.dragLine(Axis.ROW, 0, -2)

        assertEquals(shown(TutorialScript.board), page.state.tiles)
        assertEquals(0, page.state.moves)
        assertEquals(rowLine, page.state.guidance?.text)
        assertNull(page.state.drag)
    }

    @Test
    fun `a blocked shift on a column step says that column`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.stepByArrow(1)

        page.game.onArrow(Arrow.LEFT, 1)

        assertEquals(columnLine, page.state.guidance?.text)
    }

    @Test
    fun `a blocked shift doesn't reach Mordie`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.game.onArrow(Arrow.LEFT, 1)

        assertEquals(0, page.mordie.shifts)
    }

    @Test
    fun `the next shift that counts replaces the blocked line`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.game.onArrow(Arrow.LEFT, 1)

        page.stepByArrow(1)

        assertEquals(TutorialText.step(2, computer = true, labels = page.state.labels), page.state.guidance?.text)
    }

    @Test
    fun `a drag released at 0 tiles does nothing`() = webTest {
        val page = tutorialPage(backgroundScope)
        val before = page.state

        page.dragLine(Axis.ROW, 0, 0)

        assertEquals(before.guidance, page.state.guidance)
        assertEquals(TutorialText.step(1, computer = true, labels = page.state.labels), page.state.guidance?.text)
    }

    @Test
    fun `a shift key that isn't asked for is blocked`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.press("s", from = Focus.Board)

        page.press("ArrowLeft", from = Focus.Board)

        assertEquals(shown(TutorialScript.board), page.state.tiles)
        assertEquals(rowLine, page.state.guidance?.text)
    }

    @Test
    fun `select keys work during the tutorial`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.press("d", from = Focus.Board)

        assertEquals(1, page.state.selection?.col)
        assertTrue(page.state.selectionShown)
    }

    // One loop over the digits, as in GH-287.
    @Test
    fun `digits 1, 2, 3, 5 and 7 do nothing`() = webTest {
        val page = tutorialPage(backgroundScope)

        for (digit in listOf("1", "2", "3", "5", "7")) {
            page.press(digit, from = Focus.Board)
            assertNull(page.state.dialog, digit)
        }
    }

    @Test
    fun `4 opens Labels and 6 opens Controls`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.press("4", from = Focus.Board)
        assertIs<WebDialog.Labels>(page.state.dialog)
        page.game.onEscape()
        page.press("6", from = Focus.Board)

        assertIs<WebDialog.Controls>(page.state.dialog)
    }

    @Test
    fun `the panel's Controls and Labels buttons open their dialogs`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.game.tutorial.onControls()
        assertIs<WebDialog.Controls>(page.state.dialog)
        page.game.onEscape()
        page.game.tutorial.onLabels()

        assertIs<WebDialog.Labels>(page.state.dialog)
    }

    @Test
    fun `closing a dialog keeps the step`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.stepByArrow(1)

        page.game.tutorial.onLabels()
        page.game.onEscape()

        assertFalse(page.state.dialog != null)
        assertEquals(2, page.state.guidance?.step)
    }
}
