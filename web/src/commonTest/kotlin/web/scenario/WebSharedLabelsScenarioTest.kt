package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import viewmodel.TileLabels
import web.Arrow
import web.Focus
import web.WebDialog
import web.title

/** A solvable 4x4 other than the harness's: column 0 shifted one tile down. */
private val LINKED_4X4 = intArrayOf(12, 1, 2, 3, 0, 5, 6, 7, 4, 9, 10, 11, 8, 13, 14, 15)

/** GH-267 scenarios: opening a share link that carries tile labels, through [WebHarness]. */
class WebSharedLabelsScenarioTest {

    private val PromptForPairs = WebDialog.SharedLabels(TileLabels.PAIRS)

    @Test
    fun `a link with different labels asks before showing them`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, LINKED_4X4, "pairs")

        assertEquals(shown(LINKED_4X4), page.state.tiles)
        assertEquals(0, page.state.moves)
        assertEquals("Started a shared 4x4 board.", page.state.status)
        assertEquals(PromptForPairs, page.state.dialog)
        assertEquals("Use the sender's labels (Pairs)?", PromptForPairs.title)
        assertEquals("For this game only: your next new game uses your own labels.", PromptForPairs.message)
        assertEquals(Focus.SharedLabelsYes, page.state.focus)
        assertEquals(TileLabels.PAIRS, page.state.labels)
        assertEquals(
            listOf(mapOf("board_size" to 4, "sender_labels" to "pairs", "receiver_labels" to "numbers")),
            page.ownProps("screen_shared_labels_dialog"),
        )
        assertEquals(
            listOf(mapOf("result" to "started", "board_size" to 4, "labels" to "differs")),
            page.ownProps("shared_board_opened"),
        )
    }

    @Test
    fun `Yes shows the sender's labels for this game only`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, LINKED_4X4, "pairs")

        page.game.share.onSharedLabelsAnswered(true)

        assertNull(page.state.dialog)
        assertEquals(TileLabels.PAIRS, page.state.labels)
        assertEquals(
            listOf(mapOf("sender_labels" to "pairs", "receiver_labels" to "numbers", "board_size" to 4)),
            page.ownProps("shared_labels_accepted"),
        )
        assertEquals(emptyList(), page.props("dialog_cancelled"))
        assertNull(page.preferences.get("tile_labels"))
        page.game.onNew()
        page.game.onSizeChosen(4)
        assertEquals(TileLabels.NUMBERS, page.state.labels)
        assertEquals(TileLabels.NUMBERS, WebHarness(backgroundScope, store = page.store).start().state.labels)
    }

    @Test
    fun `No keeps the receiver's own labels`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, LINKED_4X4, "pairs")

        page.game.share.onSharedLabelsAnswered(false)

        assertNull(page.state.dialog)
        assertEquals(TileLabels.NUMBERS, page.state.labels)
        assertEquals(listOf(mapOf("dialog" to "shared_labels", "via" to "button")), page.ownProps("dialog_cancelled"))
        assertEquals(emptyList(), page.props("shared_labels_accepted"))
    }

    @Test
    fun `Escape is No`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, LINKED_4X4, "pairs")

        page.game.onEscape()

        assertNull(page.state.dialog)
        assertEquals(TileLabels.NUMBERS, page.state.labels)
        assertEquals(listOf(mapOf("dialog" to "shared_labels", "via" to "escape")), page.ownProps("dialog_cancelled"))
        assertEquals(emptyList(), page.props("shared_labels_accepted"))
    }

    @Test
    fun `the same labels raise no prompt`() = webTest {
        val page = WebHarness(backgroundScope)
        page.preferences.set("tile_labels", "pairs")
        page.startWithLink(4, LINKED_4X4, "pairs")

        assertNull(page.state.dialog)
        assertEquals(TileLabels.PAIRS, page.state.labels)
        assertEquals(emptyList(), page.props("screen_shared_labels_dialog"))
        assertEquals("same", page.ownProps("shared_board_opened").single()["labels"])
    }

    @Test
    fun `a link without labels is as before`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, LINKED_4X4)

        assertNull(page.state.dialog)
        assertEquals(TileLabels.NUMBERS, page.state.labels)
        assertEquals("none", page.ownProps("shared_board_opened").single()["labels"])
    }

    @Test
    fun `unreadable labels are ignored silently`() = webTest {
        val six = rotatedRow(6)
        val cases = listOf(
            Triple(4, LINKED_4X4, "colours"),
            Triple(4, LINKED_4X4, ""),
            Triple(4, LINKED_4X4, "PAIRS"),
            Triple(6, six, "letters"),
        )
        for ((side, tiles, labels) in cases) {
            val page = WebHarness(backgroundScope).startWithLink(side, tiles, labels)

            assertNull(page.state.dialog, labels)
            assertEquals(TileLabels.NUMBERS, page.state.labels, labels)
            assertEquals("Started a shared ${side}x$side board.", page.state.status, labels)
            assertEquals("unreadable", page.ownProps("shared_board_opened").single()["labels"], labels)
            assertEquals(emptyList(), page.props("screen_shared_labels_dialog"), labels)
        }
    }

    @Test
    fun `with unsaved moves, Open the shared board comes first`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.LEFT, 0)

        page.changeLink(4, LINKED_4X4, "pairs")

        assertEquals(WebDialog.OpenShared(4), page.state.dialog)
        page.game.share.onOpenAnswered(true)
        assertEquals(PromptForPairs, page.state.dialog)
        assertEquals(shown(LINKED_4X4), page.state.tiles)
        assertEquals(Focus.SharedLabelsYes, page.state.focus)
        assertFalse(page.frames.any { it.dialog == null && it.tiles == shown(LINKED_4X4) && it.labels == TileLabels.NUMBERS })
    }

    @Test
    fun `Cancel on the first prompt skips both`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.LEFT, 0)
        val played = page.state.tiles

        page.changeLink(4, LINKED_4X4, "pairs")
        page.game.share.onOpenAnswered(false)

        assertNull(page.state.dialog)
        assertEquals(played, page.state.tiles)
        assertEquals(TileLabels.NUMBERS, page.state.labels)
        assertEquals(emptyList(), page.props("screen_shared_labels_dialog"))
        assertEquals(emptyList(), page.props("shared_board_opened"))
    }

    @Test
    fun `a labelled link in an open tab asks the same way`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.changeLink(4, LINKED_4X4, "pairs")

        assertEquals(shown(LINKED_4X4), page.state.tiles)
        assertEquals(PromptForPairs, page.state.dialog)
        assertTrue(page.state.labels == TileLabels.PAIRS)
    }

    @Test
    fun `a later Labels OK reports the link as the source`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, LINKED_4X4, "pairs")
        page.game.share.onSharedLabelsAnswered(true)

        page.game.labels.open()
        page.game.labels.onSelected(TileLabels.NUMBERS)
        page.game.labels.onAnswered(ok = true)

        val changed = page.ownProps("tile_labels_changed").single()
        assertEquals("shared_link", changed["from_source"])
        assertEquals("pairs", changed["from"])
        assertEquals("numbers", changed["to"])
    }
}
