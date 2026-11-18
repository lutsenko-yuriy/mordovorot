package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import web.Arrow
import web.WebDialog

/** GH-338: Update, a share link and the tutorial replay try an autosave first and ask only if it fails. */
class WebAutosaveFlowsScenarioTest {

    /** A different board, for a share link. */
    private val linked = intArrayOf(1, 2, 3, 0) + IntArray(12) { it + 4 }

    private fun WebHarness.mid(): WebHarness {
        game.onArrow(Arrow.RIGHT, 1)
        return this
    }

    private fun WebHarness.fullStore(): WebHarness {
        store.setException = IllegalStateException("quota")
        return this
    }

    private fun WebHarness.written() = saves.exists("Autosave_numbers_4x4")

    @Test
    fun `Update with moves autosaves and reloads without asking`() = webTest {
        val page = WebHarness(backgroundScope).startOn().mid()
        page.updates.ready()

        page.game.updates.onUpdate()

        assertTrue(page.written())
        assertNull(page.state.dialog)
        assertEquals(1, page.updates.applied)
        assertEquals(listOf("update"), page.props("autosave_written").map { it["trigger"] })
        assertEquals(listOf(false), page.props("update_applied").map { it["had_unsaved_progress"] })
        assertTrue(page.props("screen_update_dialog").isEmpty())
    }

    @Test
    fun `Update asks first when the autosave fails`() = webTest {
        val page = WebHarness(backgroundScope).startOn().mid().fullStore()
        page.updates.ready()

        page.game.updates.onUpdate()

        assertIs<WebDialog.Update>(page.state.dialog)
    }

    @Test
    fun `a share link opened mid-game autosaves and starts at once`() = webTest {
        val page = WebHarness(backgroundScope).startOn().mid()

        page.changeLink(4, linked)

        assertTrue(page.written())
        assertNull(page.state.dialog)
        assertEquals(shown(linked), page.state.tiles)
        assertEquals(listOf("shared_link"), page.props("autosave_written").map { it["trigger"] })
    }

    @Test
    fun `a share link asks first when the autosave fails`() = webTest {
        val page = WebHarness(backgroundScope).startOn().mid().fullStore()

        page.changeLink(4, linked)

        assertIs<WebDialog.OpenShared>(page.state.dialog)
    }

    @Test
    fun `replaying the tutorial autosaves and starts at once`() = webTest {
        val page = WebHarness(backgroundScope).startOn().mid()

        page.game.tutorial.onReplay("button")

        assertTrue(page.written())
        assertNull(page.state.dialog)
        assertTrue(page.game.tutorial.running)
        assertEquals(listOf("tutorial_replay"), page.props("autosave_written").map { it["trigger"] })
    }

    @Test
    fun `replaying the tutorial asks first when the autosave fails`() = webTest {
        val page = WebHarness(backgroundScope).startOn().mid().fullStore()

        page.game.tutorial.onReplay("button")

        assertIs<WebDialog.ReplaySave>(page.state.dialog)
    }
}
