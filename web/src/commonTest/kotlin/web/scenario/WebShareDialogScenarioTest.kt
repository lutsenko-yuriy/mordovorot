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
import viewmodel.TileLabels
import web.Arrow
import web.Focus
import web.WebDialog
import web.qr.QrCode

/** GH-243 scenarios: the Share dialog and Copy link, through [WebHarness]. */
class WebShareDialogScenarioTest {

    private val WebHarness.share get() = assertIs<WebDialog.Share>(state.dialog)

    /** The link as a page opened from it sees it. */
    private fun fragmentOf(link: String) = "#" + link.substringAfter('#')

    private fun WebHarness.shifted(): WebHarness {
        for (line in 1..3) game.onArrow(Arrow.UP, line)
        return this
    }

    @Test
    fun `Share opens the dialog with the starting board's link`() = webTest {
        val page = WebHarness(backgroundScope).startOn().shifted()
        assertEquals(3, page.state.moves)

        page.game.share.open()

        assertEquals(ShareLinkOf(page, ONE_SHIFT_FROM_SOLVED), page.share.link)
        assertTrue(page.share.note.endsWith("starts from the same 4x4 board."))
        assertEquals(Focus.CopyLink, page.state.focus)
        assertEquals(listOf(mapOf("board_size" to 4, "start_known" to true)), page.ownProps("screen_share_dialog"))
    }

    @Test
    fun `the link opens the same board, with no moves and no labels`() = webTest {
        val page = WebHarness(backgroundScope).startOn().shifted()
        page.game.labels.open()
        page.game.labels.onSelected(TileLabels.PAIRS)
        page.game.labels.onAnswered(ok = true)
        page.game.share.open()

        val friend = WebHarness(backgroundScope).startWithFragment(fragmentOf(page.share.link))

        assertNull(friend.state.dialog)
        assertEquals(shown(ONE_SHIFT_FROM_SOLVED), friend.state.tiles)
        assertEquals(0, friend.state.moves)
        assertEquals(TileLabels.DEFAULT, friend.state.labels)
    }

    @Test
    fun `an unknown start shares the current board`() = webTest {
        val page = WebHarness(backgroundScope).seed("old", ONE_SHIFT_FROM_SOLVED, moves = 3).startOn()
        page.game.onLoad()
        page.game.onLoadChosen("old")
        val current = page.state.tiles.map { it - 1 }.toIntArray()

        page.game.share.open()

        assertEquals(ShareLinkOf(page, current), page.share.link)
        assertTrue(page.share.note.endsWith("starts from this 4x4 board as it is now."))
        assertEquals(listOf(mapOf("board_size" to 4, "start_known" to false)), page.ownProps("screen_share_dialog"))
    }

    @Test
    fun `the QR code is the link`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.share.open()

        val qr = assertNotNull(page.share.qr)
        val expected = QrCode.encode(page.share.link)

        assertEquals(expected.size, qr.size)
        for (y in 0 until qr.size) for (x in 0 until qr.size) assertEquals(expected.isDark(x, y), qr.isDark(x, y))
    }

    @Test
    fun `a link too long for a QR code is shown without one`() = webTest {
        val page = WebHarness(backgroundScope, address = FakePageAddress(base = "https://mordovorot.test/" + "x".repeat(200))).startOn()

        page.game.share.open()

        assertNull(page.share.qr)
        assertEquals(ShareLinkOf(page, ONE_SHIFT_FROM_SOLVED), page.share.link)
    }

    @Test
    fun `Copy link copies and says so`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.share.open()
        val link = page.share.link
        assertFalse(page.share.copied)

        page.game.share.onCopyLink(fromKeyboard = false)

        assertEquals(listOf(link), page.clipboard.copied)
        assertTrue(page.share.copied)
        assertEquals("Link copied.", page.state.status)
        assertEquals(listOf(mapOf("board_size" to 4, "start_known" to true, "via" to "button")), page.ownProps("board_share_link_copied"))

        page.game.share.onCopyLink(fromKeyboard = true)
        assertEquals("enter_key", page.props("board_share_link_copied").last()["via"])
    }

    @Test
    fun `a blocked clipboard selects the link and tells the player`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.clipboard.allowed = false
        page.game.share.open()

        page.game.share.onCopyLink(fromKeyboard = false)

        assertEquals(1, page.clipboard.refused)
        assertFalse(page.share.copied)
        assertEquals(Focus.ShareLinkField, page.state.focus)
        assertEquals("Couldn't copy the link - it's selected, so copy it yourself.", page.state.status)
        assertTrue(page.props("board_share_link_copied").isEmpty())
    }

    @Test
    fun `Close and Escape cancel the dialog and focus returns to Share`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        val tiles = page.state.tiles

        page.game.share.open()
        page.game.share.onClosed()
        assertNull(page.state.dialog)
        assertEquals(Focus.ShareButton, page.state.focus)

        page.game.share.open()
        page.game.onEscape()
        assertNull(page.state.dialog)
        assertEquals(Focus.ShareButton, page.state.focus)

        assertEquals(
            listOf(mapOf("dialog" to "share", "via" to "button"), mapOf("dialog" to "share", "via" to "escape")),
            page.ownProps("dialog_cancelled"),
        )
        assertEquals(tiles, page.state.tiles)
    }

    @Test
    fun `key 7 opens Share in keyboard mode only, including after a win and on the solution screen`() = webTest {
        val mouse = WebHarness(backgroundScope).startOn()
        assertFalse(mouse.press("7"))
        assertNull(mouse.state.dialog)

        val page = WebHarness(backgroundScope).remembering().startOn()
        assertEquals(7, page.state.digitOf(web.ToolbarButton.SHARE))
        assertTrue(page.press("7", from = Focus.ShareButton))
        assertIs<WebDialog.Share>(page.state.dialog)
        page.game.onEscape()
        assertEquals(Focus.Restore(Focus.ShareButton.key), page.state.focus)

        page.mordieSearches.latest.progress = SearchProgress(1, found = true, elapsedMs = 0, solution = listOf(SolutionMove(isRow = true, index = 0, amount = 1)))
        repeat(5) { page.game.onArrow(Arrow.UP, 3) }
        repeat(5) { page.game.onArrow(Arrow.DOWN, 3) }
        page.game.onArrow(Arrow.LEFT, 0)
        assertTrue(page.state.solved)
        assertTrue(page.press("7"))
        assertIs<WebDialog.Share>(page.state.dialog)
        page.game.onEscape()

        page.press("5")
        assertNotNull(page.state.solution)
        assertTrue(page.press("7"))
        assertIs<WebDialog.Share>(page.state.dialog)
    }

    private fun ShareLinkOf(page: WebHarness, tiles: IntArray) = web.ShareLink.of(page.address.base(), kotlin.math.sqrt(tiles.size.toDouble()).toInt(), tiles)
}
