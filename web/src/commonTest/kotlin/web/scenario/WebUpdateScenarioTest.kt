package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.SearchProgress
import mordie.SolutionMove
import web.Arrow
import web.CheckTrigger
import web.Focus
import web.FooterPart
import web.WebDialog

/** GH-262 scenarios: the New version available notice and the Update flow, through [WebHarness]. */
class WebUpdateScenarioTest {

    private val NOTICE = "v9.9.9 · New version available. Update"

    private fun WebHarness.footerText() = state.footer.joinToString("") { it.text }

    private fun WebHarness.hasUpdateLink() = state.footer.any { it is FooterPart.UpdateLink }

    /** Three shifts of three different rows that don't solve the board: unsaved progress, three moves. */
    private fun WebHarness.makeProgress() {
        for (row in 1..3) game.onArrow(Arrow.RIGHT, row)
    }

    /** Mordie has found its solution and five shifts of one column (one move) reveal it to the page, so Give Up is on offer (as in [WebGiveUpScenarioTest]). */
    private fun WebHarness.mordieFinds() {
        mordieSearches.latest.progress = SearchProgress(2, found = true, elapsedMs = 0, solution = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2)))
        repeat(5) { game.onArrow(Arrow.UP, 3) }
    }

    /** 4x4 two moves from solved. */
    private val twoMovesAway = intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)

    @Test
    fun `no notice until a new version is ready`() = webTest {
        val page = WebHarness(backgroundScope).start()

        assertFalse(page.hasUpdateLink())
        assertTrue(page.footerText().endsWith("Based on Loopover by Cary Huang (carykh) · original · loopover.xyz"), page.footerText())
        assertTrue(page.props("update_available").isEmpty())
    }

    @Test
    fun `a version found at load shows the notice`() = webTest {
        val page = WebHarness(backgroundScope)
        page.updates.readyAtStart = true
        page.start()

        assertEquals(NOTICE, page.footerText())
        assertEquals(listOf(mapOf("check_trigger" to "load", "current_version" to "9.9.9")), page.ownProps("update_available"))
    }

    @Test
    fun `returning to the tab finds a new version`() = webTest {
        val page = WebHarness(backgroundScope).start()

        page.game.updates.check(CheckTrigger.TAB_RETURN)
        page.updates.ready()

        assertEquals(NOTICE, page.footerText())
        assertEquals(listOf(mapOf("check_trigger" to "tab_return", "current_version" to "9.9.9")), page.ownProps("update_available"))
    }

    @Test
    fun `a version reported twice is announced once`() = webTest {
        val page = WebHarness(backgroundScope).start()

        page.updates.ready()
        page.updates.ready()

        assertEquals(1, page.props("update_available").size)
    }

    @Test
    fun `the end of a game finds a new version`() = webTest {
        val solved = WebHarness(backgroundScope).startOn()
        solved.game.onArrow(Arrow.LEFT, 0)
        solved.updates.ready()
        assertEquals(NOTICE, solved.footerText())
        assertEquals("game_end", solved.ownProps("update_available").single()["check_trigger"])

        val gaveUp = WebHarness(backgroundScope).startOn(twoMovesAway)
        gaveUp.mordieFinds()
        gaveUp.game.solution.onGiveUp()
        gaveUp.game.solution.onGiveUpAnswered(true)
        gaveUp.updates.ready()
        assertEquals(NOTICE, gaveUp.footerText())
        assertEquals("game_end", gaveUp.ownProps("update_available").single()["check_trigger"])
    }

    @Test
    fun `checks run at the right moments`() = webTest {
        val page = WebHarness(backgroundScope).startOn(twoMovesAway)
        assertEquals(1, page.updates.checks, "at start")

        page.game.updates.check(CheckTrigger.TAB_RETURN)
        assertEquals(2, page.updates.checks, "on a tab return")

        page.mordieFinds()
        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(false)
        assertEquals(2, page.updates.checks, "Give Up -> No")
        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(true)
        assertEquals(3, page.updates.checks, "Give Up -> Yes")

        val solving = WebHarness(backgroundScope).startOn()
        solving.game.onArrow(Arrow.LEFT, 0)
        assertEquals(2, solving.updates.checks, "the solved transition")
    }

    @Test
    fun `Update without unsaved progress reloads at once`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.updates.ready()

        page.game.updates.onUpdate()

        assertNull(page.state.dialog)
        assertEquals(1, page.updates.applied)
        assertEquals(listOf(mapOf("had_unsaved_progress" to false, "confirmed" to false)), page.ownProps("update_applied"))
    }

    @Test
    fun `Update with unsaved progress asks first`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.updates.ready()
        page.makeProgress()
        val board = page.state.tiles

        page.game.updates.onUpdate()

        assertEquals(WebDialog.Update, page.state.dialog)
        assertEquals(Focus.UpdateCancel, page.state.focus)
        assertEquals(listOf(mapOf("board_size" to 4, "move_count" to 3)), page.ownProps("screen_update_dialog"))
        assertEquals(0, page.updates.applied)
        assertEquals(board, page.state.tiles)
        assertEquals(3, page.state.moves)
    }

    @Test
    fun `confirming the dialog updates`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.updates.ready()
        page.makeProgress()
        page.game.updates.onUpdate()

        page.game.updates.onUpdateAnswered(true)

        assertEquals(1, page.updates.applied)
        assertEquals(listOf(mapOf("had_unsaved_progress" to true, "confirmed" to true)), page.ownProps("update_applied"))
        assertFalse(page.game.hasUnsavedProgress(), "the leave-site prompt must not ask a second time")
    }

    @Test
    fun `Cancel keeps the game and the notice`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.updates.ready()
        page.makeProgress()
        val board = page.state.tiles
        page.game.updates.onUpdate()

        page.game.updates.onUpdateAnswered(false)

        assertNull(page.state.dialog)
        assertEquals(Focus.UpdateLink, page.state.focus)
        assertEquals(board, page.state.tiles)
        assertEquals(3, page.state.moves)
        assertEquals(listOf(mapOf("dialog" to "update", "via" to "button")), page.ownProps("dialog_cancelled"))
        assertEquals(NOTICE, page.footerText())
        assertEquals(0, page.updates.applied)
        assertTrue(page.game.hasUnsavedProgress())
    }

    @Test
    fun `Escape in the dialog is Cancel`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.updates.ready()
        page.makeProgress()
        page.game.updates.onUpdate()

        page.game.onEscape()

        assertNull(page.state.dialog)
        assertEquals(Focus.UpdateLink, page.state.focus)
        assertEquals(listOf(mapOf("dialog" to "update", "via" to "escape")), page.ownProps("dialog_cancelled"))
        assertEquals(0, page.updates.applied)
    }

    @Test
    fun `updating leaves the saves alone`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.updates.ready()
        page.game.onArrow(Arrow.RIGHT, 1)
        page.game.onSave()
        page.game.onSaveAnswered("keep")
        page.game.onArrow(Arrow.RIGHT, 1)
        val before = page.store.entries.toMap()

        page.game.updates.onUpdate()
        page.game.updates.onUpdateAnswered(true)

        assertEquals(1, page.updates.applied)
        assertEquals(before, page.store.entries.toMap())
    }

    @Test
    fun `Update is ignored while another dialog is open`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.updates.ready()
        page.game.onSave()

        page.game.updates.onUpdate()

        assertTrue(page.state.dialog is WebDialog.Save)
        assertEquals(0, page.updates.applied)
    }

    @Test
    fun `Update does nothing before a version is ready`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.updates.onUpdate()

        assertEquals(0, page.updates.applied)
        assertTrue(page.props("update_applied").isEmpty())
    }
}
