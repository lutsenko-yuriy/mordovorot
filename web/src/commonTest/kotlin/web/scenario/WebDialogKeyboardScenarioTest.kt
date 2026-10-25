package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.SearchProgress
import mordie.SolutionMove
import viewmodel.TileLabels
import web.Arrow
import web.WebDialog

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
        // TODO: 1. Start on a 4x4 board; New - focus is on the 4x4 choice.
        // TODO: 2. Pick 5x5, New again - focus is on 5x5.
    }

    @Test
    fun `Save opens focused on the name field`() = webTest {
        // TODO: 1. Save - focus is on the name field.
    }

    @Test
    fun `Load opens focused on the first save, or on Cancel when there are none`() = webTest {
        // TODO: 1. No saves: Load - "No saves found.", focus is on Cancel.
        // TODO: 2. Cancel, seed two saves, Load - focus is on the first save in the list.
    }

    @Test
    fun `Can't be solved opens focused on Repair & load`() = webTest {
        // TODO: 1. Seed an unsolvable 3x3 save; toolbar Load, pick it - Can't be solved opens, focus is on Repair & load.
    }

    @Test
    fun `Tile labels opens focused on the marked scheme and follows a pick`() = webTest {
        // TODO: 1. Labels - focus is on Numbers (marked).
        // TODO: 2. Pick Letters - focus is on Letters.
    }

    @Test
    fun `Give Up opens focused on No`() = webTest {
        // TODO: 1. Mordie reveals his solution; Give Up - focus is on No.
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
        page.game.onGiveUp()
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
        page.game.onGiveUp()
        page.game.onGiveUpAnswered(false)

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
        // TODO: 1. New -> pick 4x4 - focus is on New.
        // TODO: 2. Save -> Escape - focus is on Save.
        // TODO: 3. Load -> Cancel - focus is on Load.
        // TODO: 4. Labels -> OK - focus is on Labels.
    }

    @Test
    fun `Give Up's No goes back to Give Up, Yes moves on to Next`() = webTest {
        // TODO: 1. Mordie reveals; Give Up -> No - focus is on Give Up.
        // TODO: 2. Give Up -> Yes - the solution screen, focus is on Next.
    }

    @Test
    fun `a dialog that leads to another hands focus to the next one`() = webTest {
        // TODO: 1. Seed one save; start, Cancel Restore - the size picker opens, focus is on 4x4.
        // TODO: 2. Seed an unsolvable save; toolbar Load, pick it - Can't be solved, focus is on Repair & load.
        // TODO: 3. Repair & load - focus is back on Load.
        // TODO: 4. The frame drawn between the two dialogs asks for no focus.
    }

    @Test
    fun `startup dialogs close to the start of the page`() = webTest {
        // TODO: 1. Seed one save; start, Restore -> pick it - focus goes to the start of the page.
        // TODO: 2. Reload with no saves; the size picker -> Cancel - focus goes to the start of the page.
    }
}
