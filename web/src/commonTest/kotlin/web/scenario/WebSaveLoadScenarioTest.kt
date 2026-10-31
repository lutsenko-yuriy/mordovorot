package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.SearchProgress
import viewmodel.UnsolvableSaveChoice
import web.Arrow
import web.SaveEntry
import web.WebDialog

/** GH-110 scenarios: Save and Load in the browser, kept in the in-memory store. Same setup as
 *  [WebStartupScenarioTest]; a "reload" is a new `WebGame` over the same store. */
class WebSaveLoadScenarioTest {

    @Test
    fun `Save prefills the current save name and warns before overwriting`() = webTest {
        val page = WebHarness(backgroundScope).seed("foo", ONE_SHIFT_FROM_SOLVED).start()
        page.game.onLoadChosen("foo")
        page.analytics.events.clear()

        page.game.onSave()
        assertEquals(WebDialog.Save("foo", "'foo' already exists - it will be overwritten."), page.state.dialog)
        page.game.onSaveNameTyped("bar")
        assertEquals(WebDialog.Save("bar", null), page.state.dialog)
        page.game.onSaveNameTyped("foo")
        page.game.onSaveAnswered("foo")

        assertNull(page.state.dialog)
        assertEquals("Saved as 'foo'.", page.state.status)
        assertEquals(listOf(mapOf<String, Any?>("opened_from" to "toolbar", "prefilled" to true)), page.ownProps("screen_save_dialog"))
    }

    @Test
    fun `Save rejects whitespace, slash and dot-dot names, and a blank name is Cancel`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        fun saveAs(name: String) {
            page.game.onSave()
            assertEquals(WebDialog.Save("", null), page.state.dialog)
            page.game.onSaveAnswered(name)
        }

        saveAs("a b")
        assertEquals("'a b' isn't a usable save name (no spaces) - not saved.", page.state.status)
        for (name in listOf("a/b", "..")) {
            saveAs(name)
            assertEquals(
                "Could not save as '$name': Save name must not be blank, contain a path separator, or contain '..'",
                page.state.status,
            )
        }
        saveAs("   ")
        assertNull(page.state.status)
        assertNull(page.state.dialog)

        assertTrue(page.store.entries.isEmpty())
    }

    @Test
    fun `Save Cancel stores nothing`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onSave()
        page.game.onSaveAnswered(null)

        assertNull(page.state.dialog)
        assertTrue(page.store.entries.isEmpty())
        assertEquals(listOf("save"), page.props("dialog_cancelled").map { it["dialog"] })
    }

    @Test
    fun `Load lists every save with its size and marks unsolvable ones`() = webTest {
        val page = WebHarness(backgroundScope).seed("even", solved(4)).seed("odd", UNSOLVABLE_3X3).startOn()

        page.game.onLoad()

        assertEquals(
            WebDialog.Load(restore = false, listOf(SaveEntry("even", "even (4x4)"), SaveEntry("odd", "odd (3x3) unsolvable"))),
            page.state.dialog,
        )
        assertEquals(listOf(mapOf<String, Any?>("opened_from" to "toolbar", "save_file_count" to 2)), page.ownProps("screen_load_dialog"))
    }

    @Test
    fun `Load with no saves says so`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onLoad()

        assertEquals(WebDialog.Load(restore = false, emptyList()), page.state.dialog)
    }

    @Test
    fun `loading an unsolvable save - New game opens the size picker`() = webTest {
        val page = WebHarness(backgroundScope).seed("odd", UNSOLVABLE_3X3).startOn()
        page.game.onLoad()
        page.game.onLoadChosen("odd")

        page.game.onUnsolvableChoice(UnsolvableSaveChoice.NEW_GAME)
        assertEquals(WebDialog.Size(listOf(3, 4, 5, 6), current = 4, veryLarge = listOf(7, 8, 9)), page.state.dialog)
        page.game.onSizeChosen(5)

        assertEquals(5, page.state.side)
        assertNull(page.state.dialog)
        assertEquals(listOf("toolbar"), page.props("screen_unsolvable_dialog").map { it["opened_from"] })
        assertEquals(listOf("unsolvable_save"), page.props("screen_size_dialog").map { it["opened_from"] })
    }

    @Test
    fun `Mordie's data round-trips through a browser save`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.mordieSearches.latest.progress = SearchProgress(moves = 11, found = true, elapsedMs = 0)
        // Row 1 back and forth: row 0 stays off, so the board never solves.
        repeat(5) { page.game.onArrow(if (it % 2 == 0) Arrow.RIGHT else Arrow.LEFT, 1) }
        val found = "Found it! I had to make 11 moves to find the solution! Can you beat it?"
        assertEquals(found, page.state.mordie)
        page.game.onSave()
        page.game.onSaveAnswered("m")
        page.game.onNew()
        page.game.onSizeChosen(3)
        val searches = page.mordieSearches.started.size

        page.game.onLoad()
        page.game.onLoadChosen("m")

        assertEquals(found, page.state.mordie)
        assertEquals(searches, page.mordieSearches.started.size)
    }

    @Test
    fun `saves survive a reload`() = webTest {
        val first = WebHarness(backgroundScope).startOn()
        first.game.onArrow(Arrow.RIGHT, 1)
        first.game.onSave()
        first.game.onSaveAnswered("foo")
        val saved = first.state

        val reloaded = WebHarness(backgroundScope, store = first.store).start()
        assertEquals(WebDialog.Load(restore = true, listOf(SaveEntry("foo", "foo (4x4)"))), reloaded.state.dialog)
        reloaded.game.onLoadChosen("foo")

        assertEquals(saved.tiles, reloaded.state.tiles)
        assertEquals(saved.moves, reloaded.state.moves)
    }
}
