package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import viewmodel.TileLabels
import web.Arrow
import web.SaveEntry
import web.WebDialog

/** GH-338: leaving the page autosaves to `Autosave_<scheme>_<size>`. `onPageLeft` answers whether the browser's "Leave site?" prompt should show. */
class WebAutosaveScenarioTest {

    private fun WebHarness.shift() = game.onArrow(Arrow.RIGHT, 1)

    private fun WebHarness.slot(name: String) = saves.load(name)

    private fun WebHarness.autosaves() = saves.listSaves().filter { it.startsWith("Autosave_") }

    private fun WebHarness.choose(labels: TileLabels) {
        game.labels.open()
        game.labels.onSelected(labels)
        game.labels.onAnswered(ok = true)
    }

    private fun WebHarness.newGame(side: Int) {
        game.onNew()
        game.onSizeChosen(side)
    }

    @Test
    fun `leaving after a shift writes the autosave slot`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.shift()

        assertFalse(page.game.onPageLeft())

        assertEquals(listOf("Autosave_numbers_4x4"), page.autosaves())
        assertEquals(page.board.boardArray.toList(), page.slot("Autosave_numbers_4x4")?.state?.toList())
        assertFalse(page.game.hasUnsavedProgress())
    }

    @Test
    fun `each label scheme and size has its own slot`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.PAIRS)
        page.newGame(5)
        page.shift()
        page.game.onPageLeft()
        val first = page.slot("Autosave_pairs_5x5")
        assertNotNull(first)

        page.choose(TileLabels.CYRILLIC_LETTER_PAIRS)
        page.newGame(9)
        page.shift()
        page.game.onPageLeft()

        assertEquals(listOf("Autosave_cyrillic_letter_pairs_9x9", "Autosave_pairs_5x5"), page.autosaves())
        assertEquals(first.state.toList(), page.slot("Autosave_pairs_5x5")?.state?.toList())
    }

    @Test
    fun `an autosave replaces its slot silently`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.shift()
        page.game.onPageLeft()
        page.shift()
        page.said.clear()

        page.game.onPageLeft()

        assertEquals(page.board.boardArray.toList(), page.slot("Autosave_numbers_4x4")?.state?.toList())
        assertNull(page.state.dialog)
        assertTrue(page.said.isEmpty())
    }

    @Test
    fun `an autosave leaves the current save name alone`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onSave()
        page.game.onSaveAnswered("mine")
        page.shift()
        page.game.onPageLeft()

        page.game.onSave()

        assertEquals("mine", (page.state.dialog as WebDialog.Save).name)
    }

    @Test
    fun `leaving twice writes once`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.shift()

        page.game.onPageLeft()
        page.game.onPageLeft()

        assertEquals(1, page.props("autosave_written").size)
    }

    @Test
    fun `playing on after loading an autosave rewrites the same slot`() = webTest {
        val first = WebHarness(backgroundScope).startOn()
        first.shift()
        first.game.onPageLeft()
        val page = WebHarness(backgroundScope, store = first.store).start()
        page.game.onLoadChosen("Autosave_numbers_4x4")
        page.game.onArrow(Arrow.RIGHT, 2)

        page.game.onPageLeft()

        assertEquals(listOf("Autosave_numbers_4x4"), page.autosaves())
        assertEquals(page.board.boardArray.toList(), page.slot("Autosave_numbers_4x4")?.state?.toList())
    }

    @Test
    fun `leaving with no moves writes nothing`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        assertFalse(page.game.onPageLeft())

        assertTrue(page.autosaves().isEmpty())
    }

    @Test
    fun `leaving after Save, Load or New writes nothing`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.shift()
        page.game.onSave()
        page.game.onSaveAnswered("s")
        page.game.onPageLeft()
        assertTrue(page.autosaves().isEmpty())

        page.shift()
        page.game.onLoad()
        page.game.onLoadChosen("s")
        page.game.onPageLeft()
        assertTrue(page.autosaves().isEmpty())

        page.shift()
        page.newGame(3)
        page.game.onPageLeft()
        assertTrue(page.autosaves().isEmpty())
    }

    @Test
    fun `leaving the solution screen writes nothing`() = webTest {
        val page = WebHarness(backgroundScope).startOn(rotatedRow(4))
        page.mordieSearches.latest.progress = fastAnswer(1)
        repeat(5) { page.game.onArrow(Arrow.UP, 3) }
        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(true)

        page.game.onPageLeft()

        assertTrue(page.autosaves().isEmpty())
    }

    @Test
    fun `leaving a solved board writes nothing and still prompts`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.LEFT, 0)

        assertTrue(page.game.onPageLeft())

        assertTrue(page.autosaves().isEmpty())
    }

    @Test
    fun `leaving while the tutorial runs writes nothing`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.stepByArrow(1)

        assertFalse(page.game.onPageLeft())

        assertTrue(page.autosaves().isEmpty())
    }

    @Test
    fun `a full store keeps the prompt and the progress unsaved`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.store.setException = IllegalStateException("quota")
        page.shift()

        assertTrue(page.game.onPageLeft())

        assertTrue(page.game.hasUnsavedProgress())
        assertEquals(listOf("error"), page.props("autosave_written").map { it["result"] })
    }

    @Test
    fun `the next start offers the autosave in Restore`() = webTest {
        val first = WebHarness(backgroundScope).startOn()
        first.shift()
        first.game.onPageLeft()

        val page = WebHarness(backgroundScope, store = first.store).start()

        assertEquals(WebDialog.Load(restore = true, listOf(SaveEntry("Autosave_numbers_4x4", "Autosave_numbers_4x4 (4x4)"))), page.state.dialog)
    }

    @Test
    fun `Load lists the autosave`() = webTest {
        val first = WebHarness(backgroundScope).startOn()
        first.shift()
        first.game.onPageLeft()
        val page = WebHarness(backgroundScope, store = first.store).start()
        page.game.onLoadChosen(null)
        page.game.onSizeChosen(null)

        page.game.onLoad()

        assertEquals(listOf(SaveEntry("Autosave_numbers_4x4", "Autosave_numbers_4x4 (4x4)")), (page.state.dialog as WebDialog.Load).saves)
    }

    @Test
    fun `autosave_written carries its properties`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.shift()

        page.game.onPageLeft()

        assertEquals(
            listOf(mapOf("result" to "success", "board_size" to 4, "tile_labels" to "numbers", "moves" to 1, "trigger" to "page_left")),
            page.ownProps("autosave_written"),
        )
    }

    @Test
    fun `loading an autosave is marked in load_command_used`() = webTest {
        val page = WebHarness(backgroundScope).seed("Autosave_numbers_4x4", ONE_SHIFT_FROM_SOLVED).seed("mine", ONE_SHIFT_FROM_SOLVED).startOn()

        page.game.onLoad()
        page.game.onLoadChosen("Autosave_numbers_4x4")
        page.game.onLoad()
        page.game.onLoadChosen("mine")

        assertEquals(listOf<Any?>(true, false), page.props("load_command_used").map { it["autosave"] })
    }

    @Test
    fun `restoring an autosave is marked in startup_restore_decision`() = webTest {
        val auto = WebHarness(backgroundScope).seed("Autosave_numbers_4x4", ONE_SHIFT_FROM_SOLVED).start()
        auto.game.onLoadChosen("Autosave_numbers_4x4")
        val named = WebHarness(backgroundScope).seed("mine", ONE_SHIFT_FROM_SOLVED).start()
        named.game.onLoadChosen("mine")

        assertEquals(listOf<Any?>(true), auto.props("startup_restore_decision").map { it["restored_autosave"] })
        assertEquals(listOf<Any?>(false), named.props("startup_restore_decision").map { it["restored_autosave"] })
    }
}
