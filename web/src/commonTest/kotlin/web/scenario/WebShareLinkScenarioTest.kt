package web.scenario

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import storage.BoardLinkFormat
import viewmodel.TileLabels
import web.Arrow
import web.Focus
import web.WebDialog

/** A solvable 5x5: solved, with row 0 shifted one tile right. */
private val SHARED_5X5 = intArrayOf(4, 0, 1, 2, 3) + IntArray(20) { it + 5 }

/** A solvable 4x4 other than the harness's: column 0 shifted one tile down. */
private val SHARED_4X4 = intArrayOf(12, 1, 2, 3, 0, 5, 6, 7, 4, 9, 10, 11, 8, 13, 14, 15)

private const val INVALID = "This link's board isn't a valid board."
private const val TOO_BIG = "This link's board is 12x12; this site plays 3x3 to 9x9."
private const val UNSOLVABLE = "No sequence of shifts can solve this link's board."

/** GH-243 scenarios: opening a share link at startup and on `hashchange`, through [WebHarness]. */
class WebShareLinkScenarioTest {

    @Test
    fun `a link at startup starts that board at once, skipping the restore prompt`() = webTest {
        val page = WebHarness(backgroundScope).seed("old", ONE_SHIFT_FROM_SOLVED)

        page.startWithLink(5, SHARED_5X5)

        assertEquals(5, page.state.side)
        assertEquals(shown(SHARED_5X5), page.state.tiles)
        assertEquals(0, page.state.moves)
        assertNull(page.state.dialog)
        assertContentEquals(SHARED_5X5, page.mordieSearches.latest.original)
        assertEquals("Started a shared 5x5 board.", page.state.status)
        assertEquals(listOf("app_launched", "shared_board_opened"), page.analytics.events.map { it.name })
        assertEquals(listOf(mapOf("result" to "started", "board_size" to 5)), page.ownProps("shared_board_opened"))
    }

    @Test
    fun `a started link is a fresh game, and the address bar is cleaned`() = webTest {
        val page = WebHarness(backgroundScope).remembering()
        page.preferences.set("tile_labels", "letters")

        page.startWithLink(5, SHARED_5X5)

        assertEquals(1, page.address.cleared)
        assertFalse(page.game.hasUnsavedProgress())
        assertEquals(TileLabels.LETTERS, page.state.labels)
        val reloaded = WebHarness(backgroundScope, store = page.store).start()
        assertEquals(WebDialog.Size::class, reloaded.state.dialog!!::class)
    }

    @Test
    fun `a bad link says why, then the usual startup`() = webTest {
        val cases = listOf(
            Triple("#b=!!!", INVALID, "invalid"),
            Triple("#b=" + BoardLinkFormat.encode(12, IntArray(144) { it }), TOO_BIG, "unsupported_size"),
            Triple("#b=" + BoardLinkFormat.encode(3, UNSOLVABLE_3X3), UNSOLVABLE, "unsolvable"),
        )
        for ((fragment, message, reason) in cases) {
            val page = WebHarness(backgroundScope).seed("old", ONE_SHIFT_FROM_SOLVED).startWithFragment(fragment)

            assertEquals(WebDialog.LinkRejected(message), page.state.dialog)
            assertEquals(1, page.address.cleared)
            page.game.share.onNoticeAnswered()

            assertEquals(WebDialog.Load::class, page.state.dialog!!::class)
            assertEquals(listOf(mapOf("result" to "rejected", "reason" to reason)), page.ownProps("shared_board_opened"))
        }
        val bare = WebHarness(backgroundScope).startWithFragment("#b=!!!")
        bare.game.share.onNoticeAnswered()
        assertEquals(WebDialog.Size::class, bare.state.dialog!!::class)
    }

    @Test
    fun `a fragment without a board changes nothing`() = webTest {
        for (fragment in listOf("#about", "#x=1")) {
            val page = WebHarness(backgroundScope).startWithFragment(fragment)

            assertEquals(WebDialog.Size::class, page.state.dialog!!::class)
            assertEquals(emptyList(), page.props("shared_board_opened"))
            assertEquals(0, page.address.cleared)
            assertEquals(fragment, page.address.fragment)
        }
    }

    @Test
    fun `a link opened in a playing tab with no unsaved progress starts at once`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.changeLink(4, SHARED_4X4)

        assertNull(page.state.dialog)
        assertEquals(shown(SHARED_4X4), page.state.tiles)
        assertEquals("Started a shared 4x4 board.", page.state.status)
        assertEquals(listOf(mapOf("result" to "started", "board_size" to 4)), page.ownProps("shared_board_opened"))
    }

    @Test
    fun `a link opened over unsaved progress asks first, and Cancel keeps the game`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.LEFT, 0)
        val played = page.state.tiles

        page.changeLink(4, SHARED_4X4)

        assertEquals(WebDialog.OpenShared(4), page.state.dialog)
        assertEquals(Focus.OpenSharedCancel, page.state.focus)
        page.game.share.onOpenAnswered(false)
        assertNull(page.state.dialog)
        assertEquals(played, page.state.tiles)
        assertEquals(1, page.state.moves)
        assertEquals(1, page.address.cleared)
        assertEquals(emptyList(), page.props("shared_board_opened"))

        page.changeLink(4, SHARED_4X4)
        page.game.share.onOpenAnswered(true)
        assertEquals(shown(SHARED_4X4), page.state.tiles)
        assertEquals(0, page.state.moves)
        assertEquals(1, page.props("shared_board_opened").size)
    }

    @Test
    fun `a link that arrives during an open dialog waits until it closes, and the latest wins`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onSave()
        val dialog = page.state.dialog

        page.changeLink(5, SHARED_5X5)
        page.changeLink(4, SHARED_4X4)

        assertEquals(dialog, page.state.dialog)
        assertEquals(emptyList(), page.props("shared_board_opened"))
        page.game.onSaveAnswered(null)
        assertEquals(shown(SHARED_4X4), page.state.tiles)
        assertEquals(1, page.props("shared_board_opened").size)
    }

    @Test
    fun `a bad link in a playing tab shows the notice and keeps the game`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.LEFT, 0)
        val played = page.state.tiles

        page.changeFragment("#b=" + BoardLinkFormat.encode(3, UNSOLVABLE_3X3))

        assertEquals(WebDialog.LinkRejected(UNSOLVABLE), page.state.dialog)
        page.game.share.onNoticeAnswered()
        assertNull(page.state.dialog)
        assertEquals(played, page.state.tiles)
        assertEquals(1, page.state.moves)
        assertEquals(listOf(mapOf("result" to "rejected", "reason" to "unsolvable")), page.ownProps("shared_board_opened"))
    }

    @Test
    fun `Escape closes the notice like OK, and the confirmation like Cancel`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.LEFT, 0)

        page.changeFragment("#b=!!!")
        page.game.onEscape()
        assertNull(page.state.dialog)

        page.changeLink(4, SHARED_4X4)
        page.game.onEscape()
        assertNull(page.state.dialog)
        assertEquals(1, page.state.moves)
    }

    @Test
    fun `a second link that arrives while the first is being asked about is asked about next`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.LEFT, 0)
        page.game.onSave()
        page.changeLink(5, SHARED_5X5)
        page.game.onSaveAnswered(null)
        assertEquals(WebDialog.OpenShared(5), page.state.dialog)

        page.changeLink(4, SHARED_4X4)
        page.game.share.onOpenAnswered(false)

        assertEquals(WebDialog.OpenShared(4), page.state.dialog)
        page.game.share.onOpenAnswered(true)
        assertEquals(shown(SHARED_4X4), page.state.tiles)
        assertEquals(1, page.props("shared_board_opened").size)
    }

    @Test
    fun `a link that arrives during a drag opens when the drag ends`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onDragStart(0, 0, 10.0, 10.0, 50.0)

        page.changeLink(4, SHARED_4X4)
        assertEquals(shown(ONE_SHIFT_FROM_SOLVED), page.state.tiles)
        page.game.onDragCancel()

        assertEquals(shown(SHARED_4X4), page.state.tiles)
        assertEquals(1, page.props("shared_board_opened").size)
    }
}
