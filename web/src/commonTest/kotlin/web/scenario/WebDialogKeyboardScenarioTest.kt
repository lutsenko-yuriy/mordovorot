package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.SearchProgress
import mordie.SolutionMove
import viewmodel.TileLabels
import viewmodel.UnsolvableSaveChoice
import web.Arrow
import web.WebDialog
import web.a11y.Focus

/** GH-163 scenarios: the web's dialogs from the keyboard - focus on open, Escape, focus on close -
 *  through [WebHarness], reading the frame's `focus`. Same setup as [WebStartupScenarioTest]. */
class WebDialogKeyboardScenarioTest {

    /** 4x4 two moves from solved, as in [WebGiveUpScenarioTest]. */
    private val original = intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)
    private val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2))

    /** Mordie finds its solution, then five shifts of column 3 reveal it, so Give Up is live. */
    private fun WebHarness.reveal() {
        mordieSearches.latest.progress = SearchProgress(path.size, found = true, elapsedMs = 0, solution = path)
        repeat(5) { game.onArrow(Arrow.UP, 3) }
    }

    // Focus on open

    @Test
    fun `the size picker opens focused on the current board's size`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onNew()
        assertEquals(Focus.SizeChoice(4), page.state.focus)

        page.game.onSizeChosen(5)
        page.game.onNew()
        assertEquals(Focus.SizeChoice(5), page.state.focus)
    }

    @Test
    fun `Save opens focused on the name field`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onSave()

        assertEquals(Focus.SaveName, page.state.focus)
    }

    @Test
    fun `Load opens focused on the first save, or on Cancel when there are none`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onLoad()
        assertEquals(WebDialog.Load(restore = false, emptyList()), page.state.dialog)
        assertEquals(Focus.DialogCancel, page.state.focus)

        page.game.onLoadChosen(null)
        page.seed("bar", solved(4)).seed("foo", solved(3))
        page.game.onLoad()
        assertEquals(Focus.FirstSave, page.state.focus)
    }

    @Test
    fun `Can't be solved opens focused on Repair & load`() = webTest {
        val page = WebHarness(backgroundScope).seed("odd", UNSOLVABLE_3X3).startOn()

        page.game.onLoad()
        page.game.onLoadChosen("odd")

        assertTrue(page.state.dialog is WebDialog.Unsolvable, "${page.state.dialog}")
        assertEquals(Focus.Repair, page.state.focus)
    }

    @Test
    fun `Tile labels opens focused on the marked scheme and follows a pick`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onLabels()
        assertEquals(Focus.Label(TileLabels.NUMBERS), page.state.focus)

        page.game.onLabelsSelected(TileLabels.LETTERS)
        assertEquals(Focus.Label(TileLabels.LETTERS), page.state.focus)
    }

    @Test
    fun `Give Up opens focused on No`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original)
        page.reveal()

        page.game.solution.onGiveUp()

        assertEquals(Focus.No, page.state.focus)
    }

    // Escape

    @Test
    fun `Escape cancels each dialog like its Cancel button`() = webTest {
        val page = WebHarness(backgroundScope).seed("foo", solved(3)).startOn()
        val board = page.state.tiles

        page.game.onNew()
        page.game.onEscape()
        assertNull(page.state.dialog)
        assertEquals(board, page.state.tiles)

        page.game.onSave()
        page.game.onEscape()
        assertNull(page.state.dialog)
        assertEquals(listOf("foo"), page.saves.listSaves())

        page.game.onLoad()
        page.game.onEscape()
        assertNull(page.state.dialog)
        assertEquals(board, page.state.tiles)

        page.game.onLabels()
        page.game.onLabelsSelected(TileLabels.LETTERS)
        page.game.onEscape()
        assertNull(page.state.dialog)
        assertEquals(TileLabels.NUMBERS, page.state.labels)

        page.reveal()
        page.game.solution.onGiveUp()
        page.game.onEscape()
        assertNull(page.state.dialog)
        assertNull(page.state.solution)

        assertEquals(
            listOf("size", "save", "load", "labels", "give_up").map { mapOf("dialog" to it, "via" to "escape") },
            page.ownProps("dialog_cancelled"),
        )
    }

    @Test
    fun `Cancel through the button reports via button`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original)

        page.game.onNew()
        page.game.onSizeChosen(null)
        page.game.onLabels()
        page.game.onLabelsAnswered(ok = false)
        page.reveal()
        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(false)

        assertEquals(
            listOf("size", "labels", "give_up").map { mapOf("dialog" to it, "via" to "button") },
            page.ownProps("dialog_cancelled"),
        )
    }

    @Test
    fun `Escape on Restore opens the size picker and counts as new_game`() = webTest {
        val page = WebHarness(backgroundScope).seed("foo", solved(4)).start()
        assertEquals(true, (page.state.dialog as? WebDialog.Load)?.restore)

        page.game.onEscape()

        assertTrue(page.state.dialog is WebDialog.Size, "${page.state.dialog}")
        assertEquals(listOf("new_game"), page.ownProps("startup_restore_decision").map { it["decision"] })
    }

    @Test
    fun `Escape does nothing on Can't be solved or with no dialog open`() = webTest {
        val page = WebHarness(backgroundScope).seed("odd", UNSOLVABLE_3X3).startOn()
        val idle = page.state

        page.game.onEscape()
        assertEquals(idle, page.state)
        assertTrue(page.analytics.events.isEmpty(), "${page.analytics.events}")

        page.game.onLoad()
        page.game.onLoadChosen("odd")
        page.analytics.events.clear()
        page.game.onEscape()
        assertTrue(page.state.dialog is WebDialog.Unsolvable, "${page.state.dialog}")
        assertTrue(page.analytics.events.isEmpty(), "${page.analytics.events}")
    }

    // Focus on close

    @Test
    fun `closing a toolbar dialog gives focus back to its button`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onNew()
        page.game.onSizeChosen(4)
        assertEquals(Focus.NewButton, page.state.focus)

        page.game.onSave()
        page.game.onEscape()
        assertEquals(Focus.SaveButton, page.state.focus)

        page.game.onLoad()
        page.game.onLoadChosen(null)
        assertEquals(Focus.LoadButton, page.state.focus)

        page.game.onLabels()
        page.game.onLabelsAnswered(ok = true)
        assertEquals(Focus.LabelsButton, page.state.focus)
    }

    @Test
    fun `Give Up's No goes back to Give Up, Yes moves on to Next`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original)
        page.reveal()

        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(false)
        assertEquals(Focus.GiveUpRow, page.state.focus)

        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(true)
        assertNotNull(page.state.solution)
        assertEquals(Focus.ReplayNext, page.state.focus)
    }

    @Test
    fun `a dialog that leads to another hands focus to the next one`() = webTest {
        val page = WebHarness(backgroundScope).seed("foo", solved(4)).start()
        page.game.onLoadChosen(null)
        assertTrue(page.state.dialog is WebDialog.Size, "${page.state.dialog}")
        assertEquals(Focus.SizeChoice(4), page.state.focus)
        page.game.onSizeChosen(null)

        page.seed("odd", UNSOLVABLE_3X3)
        page.game.onLoad()
        page.game.onLoadChosen("odd")
        assertEquals(Focus.Repair, page.state.focus)
        val between = page.frames.dropLast(1).last()
        assertNull(between.dialog)
        assertNull(between.focus)

        page.game.onUnsolvableChoice(UnsolvableSaveChoice.REPAIR)
        assertNull(page.state.dialog)
        assertEquals(Focus.LoadButton, page.state.focus)
    }

    @Test
    fun `startup dialogs close to the start of the page`() = webTest {
        val restored = WebHarness(backgroundScope).seed("foo", solved(4)).start()
        restored.game.onLoadChosen("foo")
        assertNull(restored.state.dialog)
        assertEquals(Focus.PageStart, restored.state.focus)

        val fresh = WebHarness(backgroundScope).start()
        fresh.game.onSizeChosen(null)
        assertNull(fresh.state.dialog)
        assertEquals(Focus.PageStart, fresh.state.focus)
    }
}
