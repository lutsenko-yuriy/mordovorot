package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import viewmodel.UnsolvableSaveChoice
import web.SaveEntry
import web.WebDialog

/** A solvable 5x5: solved, with row 0 shifted one tile right. */
private val FOO = intArrayOf(4, 0, 1, 2, 3) + IntArray(20) { it + 5 }

/** GH-110 scenarios: what the page opens with. Each scenario drives `WebGame` through a
 *  [WebHarness], which records every `WebState` frame. Each test names the WU that makes it green. */
class WebStartupScenarioTest {

    @Test
    fun `no saves - the page opens the size picker`() = webTest {
        val page = WebHarness(backgroundScope).start()
        assertEquals(WebDialog.Size(listOf(3, 4, 5)), page.state.dialog)

        page.game.onSizeChosen(4)

        assertEquals(4, page.state.side)
        assertEquals(16, page.state.tiles.size)
        assertEquals(0, page.state.moves)
        assertNull(page.state.dialog)
        assertEquals(listOf("startup"), page.props("screen_size_dialog").map { it["opened_from"] })
    }

    @Test
    fun `no saves - Cancel on the startup size picker keeps the current board`() = webTest {
        val page = WebHarness(backgroundScope)
        page.board.restoreState(ONE_SHIFT_FROM_SOLVED, 0)
        page.start()

        page.game.onSizeChosen(null)

        assertEquals(shown(ONE_SHIFT_FROM_SOLVED), page.state.tiles)
        assertEquals(0, page.state.moves)
        assertNull(page.state.dialog)
        assertEquals(listOf("size"), page.props("dialog_cancelled").map { it["dialog"] })
    }

    @Test
    fun `one save - yes to the restore offer loads it`() = webTest {
        val page = WebHarness(backgroundScope).seed("foo", FOO, moves = 3).start()
        assertEquals(WebDialog.Load(restore = true, listOf(SaveEntry("foo", "foo (5x5)"))), page.state.dialog)

        page.game.onLoadChosen("foo")

        assertEquals(shown(FOO), page.state.tiles)
        assertEquals(3, page.state.moves)
        assertNull(page.state.dialog)
        assertEquals("Loaded 'foo'.", page.state.status)
        assertEquals(listOf(mapOf<String, Any?>("opened_from" to "startup", "save_file_count" to 1)), page.ownProps("screen_load_dialog"))
    }

    @Test
    fun `one save - answering no opens the size picker`() = webTest {
        val page = WebHarness(backgroundScope).seed("foo", FOO, moves = 3).start()

        page.game.onLoadChosen(null)

        assertEquals(WebDialog.Size(listOf(3, 4, 5)), page.state.dialog)
        assertEquals(listOf("foo"), page.saves.listSaves())
        assertEquals(listOf("load"), page.props("dialog_cancelled").map { it["dialog"] })
    }

    @Test
    fun `two or more saves - a pick list, and the chosen one loads`() = webTest {
        val page = WebHarness(backgroundScope).seed("a", solved(3)).seed("b", ONE_SHIFT_FROM_SOLVED, moves = 2).start()
        assertEquals(
            WebDialog.Load(restore = true, listOf(SaveEntry("a", "a (3x3)"), SaveEntry("b", "b (4x4)"))),
            page.state.dialog,
        )

        page.game.onLoadChosen("b")

        assertEquals(shown(ONE_SHIFT_FROM_SOLVED), page.state.tiles)
        assertEquals(2, page.state.moves)
        assertNull(page.state.dialog)
    }

    @Test
    fun `unsolvable save at startup - Repair & load or New game, no Exit`() = webTest {
        val page = WebHarness(backgroundScope).seed("odd", UNSOLVABLE_3X3, moves = 5).start()
        page.game.onLoadChosen("odd")
        val unsolvable = WebDialog.Unsolvable("'odd (3x3)' can't be solved - no sequence of shifts reaches the solved board.")
        assertEquals(unsolvable, page.state.dialog)
        assertNull(page.state.status)

        page.game.onUnsolvableChoice(UnsolvableSaveChoice.EXIT)
        assertEquals(unsolvable, page.state.dialog)

        page.game.onUnsolvableChoice(UnsolvableSaveChoice.REPAIR)

        assertEquals(shown(intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8)), page.state.tiles)
        assertEquals(5, page.state.moves)
        assertNull(page.state.dialog)
        assertEquals("Repaired 'odd' (two tiles swapped) and loaded it.", page.state.status)
        assertEquals(listOf("startup"), page.props("screen_unsolvable_dialog").map { it["opened_from"] })
    }

}
