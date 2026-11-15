package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import viewmodel.GiveUpButton
import web.WebDialog
import web.tutorial.TutorialScript
import web.tutorial.TutorialText

/** GH-288: when the tutorial shows, how it starts and how it ends (Skip, the share link, the leave-site prompt). */
class WebTutorialStartScenarioTest {

    private fun step1(page: WebHarness) = TutorialText.step(1, computer = true, labels = page.state.labels)

    @Test
    fun `the tutorial shows on a first launch`() = webTest {
        val page = tutorialPage(backgroundScope)

        assertEquals(shown(TutorialScript.board), page.state.tiles)
        assertEquals(step1(page), page.state.guidance?.text)
        assertNull(page.state.dialog)
    }

    @Test
    fun `the toolbar and the Give Up row are replaced by the panel`() = webTest {
        val page = tutorialPage(backgroundScope)

        assertTrue(page.state.guidance?.controls == true)
        assertEquals(GiveUpButton.HIDDEN, page.state.giveUp)
    }

    @Test
    fun `a player with a save does not see the tutorial`() = webTest {
        val page = WebHarness(backgroundScope, firstLaunch = true).seed("foo", solved(4)).start()

        assertTrue((page.state.dialog as WebDialog.Load).restore)
        assertNull(page.state.guidance)
    }

    @Test
    fun `a corrupted save also counts as a save`() = webTest {
        val page = WebHarness(backgroundScope, firstLaunch = true).seedCorrupted("bad").start()

        assertNull(page.state.guidance)
    }

    @Test
    fun `a seen tutorial does not show again`() = webTest {
        val page = WebHarness(backgroundScope, firstLaunch = true)
        page.preferences.set("tutorial", "seen")
        page.start()

        assertIs<WebDialog.Size>(page.state.dialog)
        assertNull(page.state.guidance)
    }

    @Test
    fun `a reload halfway restarts from step 1`() = webTest {
        val first = tutorialPage(backgroundScope)
        first.stepByArrow(1)

        val page = tutorialPage(backgroundScope, first.store)

        assertEquals(shown(TutorialScript.board), page.state.tiles)
        assertEquals(1, page.state.guidance?.step)
    }

    @Test
    fun `Skip with no saves opens the size picker`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.game.tutorial.onSkip()

        assertNull(page.state.guidance)
        assertIs<WebDialog.Size>(page.state.dialog)
    }

    @Test
    fun `Skip with a save asks to restore`() = webTest {
        val page = tutorialPage(backgroundScope)
        // Saved in another tab since the page opened.
        page.seed("foo", solved(4))

        page.game.tutorial.onSkip()

        assertTrue((page.state.dialog as WebDialog.Load).restore)
    }

    @Test
    fun `Cancel in the size picker after Skip keeps the tutorial board`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.game.tutorial.onSkip()

        page.game.onSizeChosen(null)

        assertEquals(shown(TutorialScript.board), page.state.tiles)
        assertNull(page.state.dialog)
        assertNull(page.state.guidance)
    }

    @Test
    fun `Skip marks the tutorial seen`() = webTest {
        val first = tutorialPage(backgroundScope)
        first.game.tutorial.onSkip()

        val page = tutorialPage(backgroundScope, first.store)

        assertNull(page.state.guidance)
    }

    @Test
    fun `finishing the tutorial marks it seen`() = webTest {
        val first = tutorialPage(backgroundScope)
        first.playSteps()
        waitPause()

        val page = tutorialPage(backgroundScope, first.store)

        assertNull(page.state.guidance)
    }

    @Test
    fun `a share link at launch waits for the tutorial`() = webTest {
        val page = WebHarness(backgroundScope, firstLaunch = true).startWithLink(4, ONE_SHIFT_FROM_SOLVED)

        assertNotNull(page.state.guidance)
        assertTrue(page.address.fragment.isNotEmpty())
        assertEquals(shown(TutorialScript.board), page.state.tiles)
    }

    @Test
    fun `after the last step the waiting link asks first`() = webTest {
        val page = WebHarness(backgroundScope, firstLaunch = true).startWithLink(4, ONE_SHIFT_FROM_SOLVED)
        page.playSteps()
        assertNull(page.state.dialog)

        waitPause()

        assertIs<WebDialog.OpenShared>(page.state.dialog)
    }

    @Test
    fun `Yes opens the shared board in place of the tutorial board`() = webTest {
        val page = WebHarness(backgroundScope, firstLaunch = true).startWithLink(4, ONE_SHIFT_FROM_SOLVED)
        page.playSteps()
        waitPause()

        page.game.share.onOpenAnswered(true)

        assertEquals(shown(ONE_SHIFT_FROM_SOLVED), page.state.tiles)
        assertEquals("", page.address.fragment)
    }

    @Test
    fun `Cancel on the waiting link keeps the tutorial board and opens no picker`() = webTest {
        val page = WebHarness(backgroundScope, firstLaunch = true).startWithLink(4, ONE_SHIFT_FROM_SOLVED)
        page.playSteps()
        waitPause()

        page.game.share.onOpenAnswered(false)

        assertEquals(TUTORIAL_SOLVED, page.state.tiles)
        assertNull(page.state.dialog)
    }

    @Test
    fun `Skip at step 1 opens the waiting link straight away`() = webTest {
        val page = WebHarness(backgroundScope, firstLaunch = true).startWithLink(4, ONE_SHIFT_FROM_SOLVED)

        page.game.tutorial.onSkip()

        assertEquals(shown(ONE_SHIFT_FROM_SOLVED), page.state.tiles)
        assertNull(page.state.dialog)
    }

    @Test
    fun `a link arriving mid-tutorial waits`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.stepByArrow(1)
        val before = page.state

        page.changeLink(4, ONE_SHIFT_FROM_SOLVED)

        assertEquals(before, page.state)
        (2..4).forEach(page::stepByArrow)
        waitPause()
        assertIs<WebDialog.OpenShared>(page.state.dialog)
    }

    @Test
    fun `no leave-site prompt during steps 1 to 5`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.stepByArrow(1)

        assertFalse(page.game.hasUnsavedProgress())
    }

    @Test
    fun `the leave-site prompt returns after step 6`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps()
        waitPause()

        assertTrue(page.game.hasUnsavedProgress())
    }
}
