package web.scenario

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import viewmodel.UnsolvableSaveChoice
import web.Arrow

/** GH-110 scenarios: the unsaved-progress flag behind the leave-site prompt (the prompt itself is a
 *  DOM `beforeunload` check in WU7's checklist). Same setup as [WebStartupScenarioTest]. */
class WebUnsavedProgressScenarioTest {

    @Test
    fun `unsaved progress is set by a shift and cleared by save, load and New game`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        assertFalse(page.game.hasUnsavedProgress())

        page.game.onArrow(Arrow.RIGHT, 1)
        assertTrue(page.game.hasUnsavedProgress())
        page.game.onSave()
        page.game.onSaveAnswered("s")
        assertFalse(page.game.hasUnsavedProgress())

        page.game.onArrow(Arrow.RIGHT, 1)
        assertTrue(page.game.hasUnsavedProgress())
        page.game.onLoad()
        page.game.onLoadChosen("s")
        assertFalse(page.game.hasUnsavedProgress())

        page.game.onArrow(Arrow.RIGHT, 1)
        page.game.onNew()
        page.game.onSizeChosen(3)
        assertFalse(page.game.hasUnsavedProgress())
    }

    @Test
    fun `a failed or cancelled save or load keeps the progress unsaved`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.RIGHT, 1)

        page.game.onSave()
        page.game.onSaveAnswered("a b")
        assertTrue(page.game.hasUnsavedProgress())
        page.game.onSave()
        page.game.onSaveAnswered(null)
        assertTrue(page.game.hasUnsavedProgress())
        page.game.onLoad()
        page.game.onLoadChosen(null)
        assertTrue(page.game.hasUnsavedProgress())
    }

    @Test
    fun `Repair & load clears unsaved progress`() = webTest {
        val page = WebHarness(backgroundScope).seed("odd", UNSOLVABLE_3X3).startOn()
        page.game.onArrow(Arrow.RIGHT, 1)

        page.game.onLoad()
        page.game.onLoadChosen("odd")
        page.game.onUnsolvableChoice(UnsolvableSaveChoice.REPAIR)

        assertFalse(page.game.hasUnsavedProgress())
    }
}
