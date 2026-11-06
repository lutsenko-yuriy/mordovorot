package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import web.Arrow
import web.Focus
import web.SaveEntry
import web.WebDialog

/** GH-251 scenarios: deleting a save from the Load dialog and the startup restore dialog, in the
 *  browser, through [WebHarness]. Saves list sorted by name; a row is pressed by its index. */
class WebDeleteSaveScenarioTest {

    private fun entries(vararg names: String) = names.map { SaveEntry(it, "$it (4x4)") }

    private fun twoSaves(scope: kotlinx.coroutines.CoroutineScope) =
        WebHarness(scope).seed("foo", solved(4)).seed("bar", solved(4)).startOn()

    @Test
    fun `WU2 the trash icon opens a confirmation with focus on Cancel`() = webTest {
        val page = twoSaves(backgroundScope)

        page.game.onLoad()
        page.game.onDeletePressed(1)

        assertEquals(WebDialog.DeleteSave("foo"), page.state.dialog)
        assertEquals(Focus.DeleteCancel, page.state.focus)
        assertEquals(listOf("bar", "foo"), page.saves.listSaves())
    }

    @Test
    fun `WU2 Cancel and Escape in the confirmation keep the save and show the list again`() = webTest {
        val page = twoSaves(backgroundScope)
        val expected = WebDialog.Load(restore = false, entries("bar", "foo"), focus = Focus.DeleteSave(1))
        page.game.onLoad()

        page.game.onDeletePressed(1)
        page.game.onDeleteAnswered(false)
        assertEquals(expected, page.state.dialog)
        assertNull(page.state.status)
        assertEquals(Focus.DeleteSave(1), page.state.focus)

        page.game.onDeletePressed(1)
        page.game.onEscape()
        assertEquals(expected, page.state.dialog)
        assertNull(page.state.status)
        assertEquals(Focus.DeleteSave(1), page.state.focus)

        assertEquals(listOf("bar", "foo"), page.saves.listSaves())
        assertEquals(1, page.props("screen_load_dialog").size)
    }

    @Test
    fun `WU2 Delete removes the save, says so and shows the rest`() = webTest {
        val page = twoSaves(backgroundScope)
        val board = page.state.tiles
        page.game.onLoad()

        page.game.onDeletePressed(1)
        page.game.onDeleteAnswered(true)

        assertEquals(listOf("bar"), page.saves.listSaves())
        assertEquals(WebDialog.Load(restore = false, entries("bar"), Focus.SaveChoice(0), "Deleted 'foo'."), page.state.dialog)
        assertEquals("Deleted 'foo'.", page.state.status)
        assertEquals(board, page.state.tiles)
        assertEquals(0, page.state.moves)
    }

    @Test
    fun `WU2 after a Delete focus moves to the next save, or the previous one when the last went`() = webTest {
        val page = WebHarness(backgroundScope).seed("a", solved(4)).seed("b", solved(4)).seed("c", solved(4)).startOn()
        page.game.onLoad()

        page.game.onDeletePressed(1)
        page.game.onDeleteAnswered(true)
        assertEquals(Focus.SaveChoice(1), page.state.focus)
        assertEquals(listOf("a", "c"), (page.state.dialog as WebDialog.Load).saves.map { it.name })

        page.game.onDeletePressed(1)
        page.game.onDeleteAnswered(true)
        assertEquals(Focus.SaveChoice(0), page.state.focus)
        assertEquals(listOf("a"), (page.state.dialog as WebDialog.Load).saves.map { it.name })
    }

    @Test
    fun `WU2 the startup restore dialog deletes too, and another save still restores`() = webTest {
        val page = WebHarness(backgroundScope).seed("foo", solved(4)).seed("bar", solved(4)).start()
        assertEquals(true, (page.state.dialog as WebDialog.Load).restore)

        page.game.onDeletePressed(1)
        page.game.onDeleteAnswered(true)

        assertEquals(WebDialog.Load(restore = true, entries("bar"), Focus.SaveChoice(0), "Deleted 'foo'."), page.state.dialog)

        page.game.onLoadChosen("bar")
        assertNull(page.state.dialog)
        assertEquals(listOf("restored"), page.ownProps("startup_restore_decision").map { it["decision"] })
    }

    @Test
    fun `WU2 deleting the last save at startup opens the size picker`() = webTest {
        val one = WebHarness(backgroundScope).seed("foo", solved(4)).start()

        one.game.onDeletePressed(0)
        one.game.onDeleteAnswered(true)

        assertTrue(one.state.dialog is WebDialog.Size, "${one.state.dialog}")
        assertEquals("Deleted 'foo'.", one.state.status)
        assertEquals(listOf("all_deleted"), one.ownProps("startup_restore_decision").map { it["decision"] })
        assertEquals(listOf("startup"), one.ownProps("screen_size_dialog").map { it["opened_from"] })

        val two = WebHarness(backgroundScope).seed("foo", solved(4)).seed("bar", solved(4)).start()
        two.game.onDeletePressed(0)
        two.game.onDeleteAnswered(true)
        assertTrue(two.state.dialog is WebDialog.Load)
        assertTrue(two.props("startup_restore_decision").isEmpty())

        two.game.onDeletePressed(0)
        two.game.onDeleteAnswered(true)
        assertTrue(two.state.dialog is WebDialog.Size, "${two.state.dialog}")
        assertEquals(listOf("all_deleted"), two.ownProps("startup_restore_decision").map { it["decision"] })
    }

    @Test
    fun `WU2 deleting the last save mid-game returns to the game`() = webTest {
        val page = WebHarness(backgroundScope).seed("foo", solved(4)).startOn()
        page.game.onArrow(Arrow.RIGHT, 1)
        val tiles = page.state.tiles
        val moves = page.state.moves
        page.game.onLoad()

        page.game.onDeletePressed(0)
        page.game.onDeleteAnswered(true)

        assertNull(page.state.dialog)
        assertEquals(tiles, page.state.tiles)
        assertEquals(moves, page.state.moves)
        assertEquals("Deleted 'foo'. No saves left.", page.state.status)
        assertTrue(page.props("dialog_cancelled").isEmpty())
        assertEquals(Focus.LoadButton, page.state.focus)
    }

    @Test
    fun `WU2 deleting the current save clears the Save dialog's name, and another save does not`() = webTest {
        val page = twoSaves(backgroundScope)
        page.game.onLoad()
        page.game.onLoadChosen("foo")

        page.game.onLoad()
        page.game.onDeletePressed(0)
        page.game.onDeleteAnswered(true)
        page.game.onLoadChosen(null)
        page.game.onSave()
        assertEquals("foo", (page.state.dialog as WebDialog.Save).name)
        page.game.onSaveAnswered(null)

        page.game.onLoad()
        page.game.onDeletePressed(0)
        page.game.onDeleteAnswered(true)
        page.game.onSave()
        assertEquals("", (page.state.dialog as WebDialog.Save).name)
    }

    @Test
    fun `WU2 corrupted and unsolvable saves can be deleted`() = webTest {
        val page = WebHarness(backgroundScope).startOn().seed("odd", UNSOLVABLE_3X3).seedCorrupted("bad")
        page.game.onLoad()
        assertEquals(listOf(SaveEntry("bad", "bad"), SaveEntry("odd", "odd (3x3) unsolvable")), (page.state.dialog as WebDialog.Load).saves)

        page.game.onDeletePressed(1)
        page.game.onDeleteAnswered(true)
        page.game.onDeletePressed(0)
        page.game.onDeleteAnswered(true)

        assertTrue(page.store.entries.isEmpty())
        assertEquals(listOf("unsolvable", "corrupted"), page.ownProps("screen_delete_save_dialog").map { it["save_state"] })
        assertEquals(listOf("unsolvable", "corrupted"), page.ownProps("save_deleted").map { it["save_state"] })
    }

    @Test
    fun `WU2 a save that is already gone, or that storage refuses to delete, leaves the list and the board untouched`() = webTest {
        val page = twoSaves(backgroundScope)
        val board = page.state.tiles
        val list = entries("bar", "foo")
        page.game.onLoad()

        page.game.onDeletePressed(1)
        page.saves.delete("foo")
        page.game.onDeleteAnswered(true)
        assertEquals("No save named 'foo' - it was already deleted.", page.state.status)
        assertEquals(WebDialog.Load(restore = false, list, Focus.DeleteSave(1), "No save named 'foo' - it was already deleted."), page.state.dialog)
        assertEquals(Focus.DeleteSave(1), page.state.focus)

        page.store.removeException = IllegalStateException("storage is blocked")
        page.game.onDeletePressed(0)
        page.game.onDeleteAnswered(true)
        assertEquals("Could not delete 'bar'.", page.state.status)
        assertEquals(WebDialog.Load(restore = false, list, Focus.DeleteSave(0), "Could not delete 'bar'."), page.state.dialog)
        assertEquals(Focus.DeleteSave(0), page.state.focus)

        assertEquals(board, page.state.tiles)
        assertEquals(listOf("not_found", "error"), page.ownProps("save_deleted").map { it["result"] })
    }

    @Test
    fun `WU2 deleting a save does not make the game unsaved progress`() = webTest {
        val page = WebHarness(backgroundScope).seed("foo", solved(4)).startOn()
        page.game.onLoad()
        page.game.onLoadChosen("foo")
        assertFalse(page.game.hasUnsavedProgress())

        page.game.onLoad()
        page.game.onDeletePressed(0)
        page.game.onDeleteAnswered(true)

        assertFalse(page.game.hasUnsavedProgress())
    }

    @Test
    fun `WU2 deleting reports its screen view and event without the save's name, and by key in keyboard mode`() = webTest {
        val page = WebHarness(backgroundScope).seed("secretname", solved(4)).seed("other", solved(4)).startOn()
        page.game.onArrow(Arrow.RIGHT, 1)
        page.analytics.events.clear()
        page.pressedWith.pointer()
        page.game.onLoad()

        page.game.onDeletePressed(0)
        assertEquals(
            listOf(mapOf<String, Any?>("opened_from" to "toolbar", "save_state" to "ok", "save_file_count" to 2)),
            page.ownProps("screen_delete_save_dialog"),
        )
        page.game.onDeleteAnswered(false)
        page.game.onDeletePressed(0)
        page.game.onEscape()
        assertEquals(
            listOf(mapOf("dialog" to "delete_save", "via" to "button"), mapOf("dialog" to "delete_save", "via" to "escape")),
            page.ownProps("dialog_cancelled"),
        )

        page.game.onDeletePressed(0)
        page.game.onDeleteAnswered(true)
        assertEquals(
            listOf(
                mapOf<String, Any?>(
                    "result" to "success", "trigger" to "command", "save_state" to "ok",
                    "was_current_save" to false, "remaining_save_count" to 1,
                ),
            ),
            page.ownProps("save_deleted"),
        )
        assertEquals(listOf("rich_ui"), page.props("save_deleted").map { it["input_method"] })
        assertTrue(page.analytics.events.none { event -> event.properties.values.any { it.toString().contains("secret") || it.toString().contains("other") } })

        page.pressedWith.key()
        page.game.onDeletePressed(0)
        page.game.onDeleteAnswered(true)
        assertEquals(listOf("rich_ui", "keyboard"), page.props("save_deleted").map { it["input_method"] })

        val startup = WebHarness(backgroundScope).seed("foo", solved(4)).seed("bar", solved(4)).start()
        startup.game.onDeletePressed(0)
        startup.game.onDeleteAnswered(true)
        assertEquals("startup", startup.ownProps("screen_delete_save_dialog").single()["opened_from"])
        assertEquals("startup_prompt", startup.ownProps("save_deleted").single()["trigger"])
    }

    @Test
    fun `WU2 the restore dialog offers only the saves it opened with, even if another tab saved one meanwhile`() = webTest {
        val page = WebHarness(backgroundScope).seed("a", solved(4)).seed("b", solved(4)).start()
        page.seed("c", solved(4))

        page.game.onDeletePressed(0)
        page.game.onDeleteAnswered(true)

        assertEquals(listOf("b"), (page.state.dialog as WebDialog.Load).saves.map { it.name })
    }
}
