package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import viewmodel.TileLabels
import web.Focus
import web.ShareLink
import web.WebDialog
import web.qr.QrCode

/** GH-267 scenarios: the Share dialog's `Include my tile labels` toggle, through [WebHarness]. */
class WebShareLabelsScenarioTest {

    private val WebHarness.share get() = assertIs<WebDialog.Share>(state.dialog)

    private fun WebHarness.playing(labels: String? = null): WebHarness {
        labels?.let { preferences.set("tile_labels", it) }
        return startOn()
    }

    private fun boardLink(page: WebHarness, labels: TileLabels? = null) =
        ShareLink.of(page.address.base(), 4, ONE_SHIFT_FROM_SOLVED, labels)

    @Test
    fun `the toggle starts off and the link is board-only`() = webTest {
        val page = WebHarness(backgroundScope).playing()

        page.game.share.open()

        assertEquals("Include my tile labels (Numbers)", page.share.labelsToggle)
        assertFalse(page.share.includeLabels)
        assertEquals(boardLink(page), page.share.link)
        assertEquals(listOf(mapOf("board_size" to 4, "start_known" to true, "include_labels" to false, "include_progress" to false)), page.ownProps("screen_share_dialog"))
    }

    @Test
    fun `turning it on adds the labels to the link and QR at once`() = webTest {
        val page = WebHarness(backgroundScope).playing("pairs")
        page.game.share.open()

        page.game.share.onLabelsToggled()

        assertTrue(page.share.includeLabels)
        assertEquals(boardLink(page, TileLabels.PAIRS), page.share.link)
        assertTrue(page.share.link.endsWith("&l=pairs"))
        val qr = assertNotNull(page.share.qr)
        val expected = QrCode.encode(page.share.link)
        assertEquals(expected.size, qr.size)
        for (y in 0 until qr.size) for (x in 0 until qr.size) assertEquals(expected.isDark(x, y), qr.isDark(x, y))
        assertEquals(Focus.ShareLabels, page.state.focus)
        assertEquals(listOf(mapOf("enabled" to true, "labels" to "pairs", "board_size" to 4)), page.ownProps("share_labels_toggled"))

        page.game.share.onLabelsToggled()

        assertFalse(page.share.includeLabels)
        assertEquals(boardLink(page), page.share.link)
        assertEquals(listOf(true, false), page.props("share_labels_toggled").map { it["enabled"] })
    }

    @Test
    fun `flipping it resets Copied`() = webTest {
        val page = WebHarness(backgroundScope).playing("pairs")
        page.game.share.open()
        page.game.share.onCopyLink(fromKeyboard = false)
        assertTrue(page.share.copied)

        page.game.share.onLabelsToggled()
        assertFalse(page.share.copied)

        page.game.share.onCopyLink(fromKeyboard = false)
        assertEquals(listOf(boardLink(page), boardLink(page, TileLabels.PAIRS)), page.clipboard.copied)
        assertEquals(listOf<Any?>(false, true), page.props("board_share_link_copied").map { it["includes_labels"] })
    }

    @Test
    fun `the choice is remembered`() = webTest {
        val page = WebHarness(backgroundScope).playing()
        page.game.share.open()
        page.game.share.onLabelsToggled()
        page.game.share.onClosed()

        page.game.share.open()
        assertTrue(page.share.includeLabels)
        assertEquals(true, page.ownProps("screen_share_dialog").last()["include_labels"])

        val reloaded = WebHarness(backgroundScope, store = page.store).playing()
        reloaded.game.share.open()
        assertTrue(reloaded.share.includeLabels)
        assertTrue(reloaded.share.link.endsWith("&l=numbers"))
    }

    @Test
    fun `the labels shown travel, not the preference`() = webTest {
        val page = WebHarness(backgroundScope).playing("letters")
        page.game.onNew()
        page.game.onSizeChosen(6)
        page.game.share.open()

        page.game.share.onLabelsToggled()

        assertEquals("Include my tile labels (Numbers)", page.share.labelsToggle)
        assertTrue(page.share.link.endsWith("&l=numbers"))
        assertEquals("numbers", page.ownProps("share_labels_toggled").single()["labels"])
    }

    @Test
    fun `a labelled link reaches the receiver end to end`() = webTest {
        val sender = WebHarness(backgroundScope).playing("pairs")
        sender.game.share.open()
        sender.game.share.onLabelsToggled()
        val fragment = "#" + sender.share.link.substringAfter('#')

        val receiver = WebHarness(backgroundScope).startWithFragment(fragment)

        assertEquals(WebDialog.SharedLabels(TileLabels.PAIRS), receiver.state.dialog)
        receiver.game.share.onSharedLabelsAnswered(true)
        assertEquals(shown(ONE_SHIFT_FROM_SOLVED), receiver.state.tiles)
        assertEquals(TileLabels.PAIRS, receiver.state.labels)
    }

    @Test
    fun `a failing preference write still toggles`() = webTest {
        val page = WebHarness(backgroundScope).playing()
        page.store.setException = IllegalStateException("storage is blocked")
        page.game.share.open()

        page.game.share.onLabelsToggled()

        assertTrue(page.share.includeLabels)
        assertTrue(page.share.link.endsWith("&l=numbers"))
        page.store.setException = null
        val reloaded = WebHarness(backgroundScope, store = page.store).playing()
        reloaded.game.share.open()
        assertFalse(reloaded.share.includeLabels)
        assertNull(reloaded.state.status)
    }
}
