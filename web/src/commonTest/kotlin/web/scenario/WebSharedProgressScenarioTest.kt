package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import viewmodel.TileLabels
import web.Arrow
import web.WebDialog

/** GH-272 scenarios: opening a share link that carries the sender's progress, through [WebHarness]. */
class WebSharedProgressScenarioTest {

    private val usual = "Alright! Let's sort this one out!"
    private val solving12 = "Okay, so your friend made 12 moves and did not solve the puzzle yet. Maybe we could help?"

    /** [count] shifts of column 3, which never solve this board. */
    private fun WebHarness.shifts(count: Int) = repeat(count) { game.onArrow(Arrow.UP, 3) }

    @Test
    fun `a link that says the sender is still solving puts that status in Mordie's opening`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED, progress = "n12")

        assertEquals(0, page.state.moves)
        assertEquals(shown(ONE_SHIFT_FROM_SOLVED), page.state.tiles)
        assertEquals(solving12, page.state.mordie)
    }

    @Test
    fun `a link that says the sender solved it puts that status in Mordie's opening`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED, progress = "s12")

        assertEquals("Okay, so your friend solved this one in 12 moves. Wanna beat this?", page.state.mordie)
    }

    @Test
    fun `a link that says the sender gave up puts that status in Mordie's opening`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED, progress = "g12")

        assertEquals("So after making 12 moves your friend gave up before finding the solution. Can we find one?", page.state.mordie)
    }

    @Test
    fun `a gave-up link with no count drops the count from Mordie's line`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED, progress = "g")

        assertEquals("So your friend gave up before finding the solution. Can we find one?", page.state.mordie)
    }

    @Test
    fun `a count of 1 reads 1 move`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED, progress = "n1")

        assertEquals("Okay, so your friend made 1 move and did not solve the puzzle yet. Maybe we could help?", page.state.mordie)
    }

    @Test
    fun `the sender's status stays through shift 4`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED, progress = "n12")

        repeat(4) {
            page.shifts(1)
            assertEquals(solving12, page.state.mordie)
        }
    }

    @Test
    fun `from shift 5 the normal Mordie rules apply`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED, progress = "n12")

        page.shifts(5)

        assertNotEquals(solving12, page.state.mordie)
    }

    @Test
    fun `a link without progress opens with Mordie's usual line`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED)

        assertEquals(usual, page.state.mordie)
    }

    @Test
    fun `progress the page can't read is ignored without a word`() = webTest {
        for (progress in listOf("x", "n0", "n012", "n10000000", "s")) {
            val page = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED, progress = progress)

            assertNull(page.state.dialog, progress)
            assertEquals("Started a shared 4x4 board.", page.state.status, progress)
            assertEquals(usual, page.state.mordie, progress)
        }
    }

    @Test
    fun `a new game after a progress link opens with Mordie's usual line`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED, progress = "n12")

        page.game.onNew()
        page.game.onSizeChosen(4)

        assertEquals(usual, page.state.mordie)
    }

    @Test
    fun `a load at 0 moves after a progress link opens with Mordie's usual line`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED, progress = "n12")
        page.seed("zero", ONE_SHIFT_FROM_SOLVED)

        page.game.onLoad()
        page.game.onLoadChosen("zero")

        assertEquals(usual, page.state.mordie)
    }

    @Test
    fun `a link with labels and progress shows both`() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED, labels = "pairs", progress = "g3")

        assertEquals(WebDialog.SharedLabels(TileLabels.PAIRS), page.state.dialog)
        page.game.share.onSharedLabelsAnswered(true)

        assertNull(page.state.dialog)
        assertEquals(TileLabels.PAIRS, page.state.labels)
        assertEquals("So after making 3 moves your friend gave up before finding the solution. Can we find one?", page.state.mordie)
    }

    @Test
    fun `a progress link met in an open tab works like one at load`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.LEFT, 0)

        page.changeLink(4, ONE_SHIFT_FROM_SOLVED, progress = "s12")
        assertEquals(WebDialog.OpenShared(4), page.state.dialog)
        page.game.share.onOpenAnswered(true)

        assertNull(page.state.dialog)
        assertEquals("Okay, so your friend solved this one in 12 moves. Wanna beat this?", page.state.mordie)
    }

    @Test
    fun `shared_board_opened tells what the link carried`() = webTest {
        val cases = listOf("n3" to "solving", "s3" to "solved", "g3" to "gave_up", "g" to "gave_up", null to "none", "x" to "unreadable")
        for ((sent, told) in cases) {
            val page = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED, progress = sent)

            assertEquals(told, page.ownProps("shared_board_opened").single()["progress"], "p=$sent")
        }
    }
}
