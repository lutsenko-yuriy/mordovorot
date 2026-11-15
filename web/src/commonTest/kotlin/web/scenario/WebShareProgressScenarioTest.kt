package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.SearchProgress
import mordie.SolutionMove
import storage.SavedMordie
import storage.SavedMove
import viewmodel.SharedProgress
import web.Arrow
import web.Focus
import web.ShareLink
import web.WebDialog
import web.qr.QrCode

/** GH-272 scenarios: the Share dialog's `Include my progress` toggle, through [WebHarness]. */
class WebShareProgressScenarioTest {

    private val WebHarness.share get() = assertIs<WebDialog.Share>(state.dialog)

    /** A 4x4 board two moves from solved, as in [WebPlayerResultScenarioTest]. */
    private val original = intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)
    private val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2))
    private val savedPath = path.map { SavedMove(it.isRow, it.index, it.amount) }

    /** [ONE_SHIFT_FROM_SOLVED] with [count] moves made on other rows, so it stays unsolved. */
    private fun WebHarness.playing(count: Int = 0): WebHarness {
        startOn()
        repeat(count) { game.onArrow(Arrow.LEFT, it + 1) }
        return this
    }

    private fun link(page: WebHarness, progress: SharedProgress? = null, start: IntArray = ONE_SHIFT_FROM_SOLVED) =
        ShareLink.of(page.address.base(), 4, start, progress = progress)

    private fun WebHarness.searchFinds() {
        mordieSearches.latest.progress = SearchProgress(path.size, found = true, elapsedMs = 0, solution = path)
    }

    /** Mordie finds its solution, then five shifts of column 3 (one move) reveal it, then [more] moves on other lines. */
    private fun WebHarness.movesMade(more: Int) {
        searchFinds()
        repeat(5) { game.onArrow(Arrow.UP, 3) }
        repeat(more) { game.onArrow(Arrow.LEFT, it + 1) }
    }

    private fun WebHarness.giveUp() {
        game.solution.onGiveUp()
        game.solution.onGiveUpAnswered(true)
    }

    /** Right on row 0, then down twice on column 1: Mordie's own solution, in 2 moves. */
    private fun WebHarness.solve() {
        game.onArrow(Arrow.RIGHT, 0)
        repeat(2) { game.onArrow(Arrow.DOWN, 1) }
    }

    @Test
    fun `the toggle starts off and names the sender's progress`() = webTest {
        val page = WebHarness(backgroundScope).playing(count = 3)

        page.game.share.open()

        assertEquals("Include my progress (3 moves, not solved)", page.share.progressToggle)
        assertFalse(page.share.includeProgress)
        assertTrue(page.share.progressAvailable)
        assertEquals(link(page), page.share.link)
        assertEquals(
            listOf(mapOf("board_size" to 4, "start_known" to true, "include_labels" to false, "include_progress" to false)),
            page.ownProps("screen_share_dialog"),
        )
    }

    @Test
    fun `turning it on adds the progress to the link and QR at once`() = webTest {
        val page = WebHarness(backgroundScope).playing(count = 3)
        page.game.share.open()

        page.game.share.onProgressToggled()

        assertTrue(page.share.includeProgress)
        assertEquals(link(page, SharedProgress.Solving(3)), page.share.link)
        assertTrue(page.share.link.endsWith("&p=n3"))
        val qr = assertNotNull(page.share.qr)
        val expected = QrCode.encode(page.share.link)
        assertEquals(expected.size, qr.size)
        for (y in 0 until qr.size) for (x in 0 until qr.size) assertEquals(expected.isDark(x, y), qr.isDark(x, y))
    }

    @Test
    fun `flipping it puts Copy link back and keeps focus on the toggle`() = webTest {
        val page = WebHarness(backgroundScope).playing(count = 3)
        page.game.share.open()
        page.game.share.onCopyLink(fromKeyboard = false)
        assertTrue(page.share.copied)

        page.game.share.onProgressToggled()

        assertFalse(page.share.copied)
        assertEquals(Focus.ShareProgress, page.state.focus)
    }

    @Test
    fun `flipping it sends share_progress_toggled`() = webTest {
        val page = WebHarness(backgroundScope).playing(count = 3)
        page.game.share.open()

        page.game.share.onProgressToggled()
        page.game.share.onProgressToggled()

        assertEquals(
            listOf(
                mapOf("enabled" to true, "progress" to "solving", "board_size" to 4),
                mapOf("enabled" to false, "progress" to "solving", "board_size" to 4),
            ),
            page.ownProps("share_progress_toggled"),
        )
    }

    @Test
    fun `copying the link sends includes_progress`() = webTest {
        val page = WebHarness(backgroundScope).playing(count = 3)
        page.game.share.open()
        page.game.share.onProgressToggled()
        page.game.share.onCopyLink(fromKeyboard = false)

        page.game.share.onProgressToggled()
        page.game.share.onCopyLink(fromKeyboard = false)

        assertEquals(listOf<Any?>(true, false), page.props("board_share_link_copied").map { it["includes_progress"] })
        assertEquals(listOf(link(page, SharedProgress.Solving(3)), link(page)), page.clipboard.copied)
    }

    @Test
    fun `a solved board offers solved in N moves`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original)
        page.searchFinds()
        page.solve()

        page.game.share.open()
        page.game.share.onProgressToggled()

        assertEquals("Include my progress (solved in 2 moves)", page.share.progressToggle)
        assertEquals(link(page, SharedProgress.Solved(2), original), page.share.link)
        assertTrue(page.share.link.endsWith("&p=s2"))
    }

    @Test
    fun `after Give Up the toggle offers gave up after N moves`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original)
        page.movesMade(more = 2)
        page.giveUp()

        page.game.share.open()
        page.game.share.onProgressToggled()

        assertEquals("Include my progress (gave up after 3 moves)", page.share.progressToggle)
        assertTrue(page.share.link.endsWith("&p=g3"))
        assertEquals("gave_up", page.ownProps("share_progress_toggled").single()["progress"])
    }

    @Test
    fun `solving, then opening Mordie's solution counts as solved`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original)
        page.searchFinds()
        page.solve()
        page.game.solution.onMordiesSolution()

        page.game.share.open()

        assertEquals("Include my progress (solved in 2 moves)", page.share.progressToggle)
    }

    @Test
    fun `a loaded save offers the same progress as the live game`() = webTest {
        val page = WebHarness(backgroundScope).playing(count = 3)
        page.game.onSave()
        page.game.onSaveAnswered("s")
        page.game.onNew()
        page.game.onSizeChosen(4)

        page.game.onLoad()
        page.game.onLoadChosen("s")
        page.game.share.open()

        assertEquals("Include my progress (3 moves, not solved)", page.share.progressToggle)
    }

    @Test
    fun `a gave-up save from before GH-273 sends gave up with no count`() = webTest {
        val page = WebHarness(backgroundScope)
            .seed("old", original, mordie = SavedMordie(original, 2, found = true, solution = savedPath, solutionScreen = true)).start()
        page.game.onLoadChosen("old")

        page.game.share.open()
        page.game.share.onProgressToggled()

        assertEquals("Include my progress (gave up)", page.share.progressToggle)
        assertTrue(page.share.link.endsWith("&p=g"))
    }

    @Test
    fun `at 0 moves the toggle is dimmed and can't be switched on`() = webTest {
        val page = WebHarness(backgroundScope).playing()

        page.game.share.open()
        page.game.share.onProgressToggled()

        assertEquals("Include my progress", page.share.progressToggle)
        assertFalse(page.share.progressAvailable)
        assertFalse(page.share.includeProgress)
        assertEquals(link(page), page.share.link)
        assertTrue(page.props("share_progress_toggled").isEmpty())
    }

    @Test
    fun `giving up at 0 moves keeps the toggle dimmed`() = webTest {
        val page = WebHarness(backgroundScope).seed("m", original, mordie = SavedMordie(original, 2, found = true, solution = savedPath)).start()
        page.game.onLoadChosen("m")
        page.giveUp()

        page.game.share.open()

        assertFalse(page.share.progressAvailable)
        assertEquals("Include my progress", page.share.progressToggle)
        assertEquals(link(page, start = original), page.share.link)
    }

    @Test
    fun `a count of 1 reads 1 move in the toggle`() = webTest {
        val page = WebHarness(backgroundScope).playing(count = 1)

        page.game.share.open()

        assertEquals("Include my progress (1 move, not solved)", page.share.progressToggle)
    }

    @Test
    fun `an unknown start still sends the real count`() = webTest {
        val page = WebHarness(backgroundScope).seed("s", ONE_SHIFT_FROM_SOLVED, moves = 3).start()
        page.game.onLoadChosen("s")

        page.game.share.open()
        assertFalse(page.share.startKnown)
        assertTrue(page.share.progressAvailable)
        page.game.share.onProgressToggled()

        assertTrue(page.share.link.endsWith("&p=n3"))
    }

    @Test
    fun `a sender who opened a shared link sends their own progress`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED, progress = "s12")
        page.game.onArrow(Arrow.LEFT, 1)
        page.game.onArrow(Arrow.LEFT, 2)

        page.game.share.open()
        page.game.share.onProgressToggled()

        assertEquals("Include my progress (2 moves, not solved)", page.share.progressToggle)
        assertTrue(page.share.link.endsWith("&p=n2"))
    }

    @Test
    fun `the choice is remembered, starting off the first time`() = webTest {
        val page = WebHarness(backgroundScope).playing(count = 3)
        page.game.share.open()
        assertFalse(page.share.includeProgress)
        page.game.share.onProgressToggled()
        page.game.share.onClosed()

        page.game.share.open()
        assertTrue(page.share.includeProgress)
        assertEquals(true, page.ownProps("screen_share_dialog").last()["include_progress"])
        assertEquals("on", page.preferences.get("share_progress"))

        val reloaded = WebHarness(backgroundScope, store = page.store).startOn()
        reloaded.game.onArrow(Arrow.LEFT, 1)
        reloaded.game.share.open()
        assertTrue(reloaded.share.includeProgress)
        assertTrue(reloaded.share.link.endsWith("&p=n1"))

        val fresh = WebHarness(backgroundScope).playing(count = 1)
        fresh.game.share.open()
        assertFalse(fresh.share.includeProgress)
    }

    @Test
    fun `a remembered on at 0 moves shows off and is kept`() = webTest {
        val page = WebHarness(backgroundScope)
        page.preferences.set("share_progress", "on")
        page.playing()

        page.game.share.open()
        assertFalse(page.share.includeProgress)
        assertEquals(link(page), page.share.link)
        assertEquals(false, page.ownProps("screen_share_dialog").single()["include_progress"])
        page.game.share.onClosed()
        assertEquals("on", page.preferences.get("share_progress"))

        page.game.onArrow(Arrow.LEFT, 1)
        page.game.share.open()

        assertTrue(page.share.includeProgress)
        assertTrue(page.share.link.endsWith("&p=n1"))
    }

    @Test
    fun `both toggles can be on in one link`() = webTest {
        val page = WebHarness(backgroundScope).playing(count = 3)
        page.game.share.open()

        page.game.share.onLabelsToggled()
        page.game.share.onProgressToggled()

        assertTrue(page.share.link.endsWith("&l=numbers&p=n3"), page.share.link)
        assertNull(page.state.status)
    }
}
