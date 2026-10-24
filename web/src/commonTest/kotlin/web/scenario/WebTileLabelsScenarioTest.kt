package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.SearchProgress
import mordie.SolutionMove
import viewmodel.TileLabels
import web.Arrow
import web.LabelOption
import web.WebDialog

/** GH-139 scenarios: tile labels in the web client. A relaunch is a new [WebHarness] over the same
 *  store. The toolbar's `[ Labels ]` button itself is `DomRenderer`'s, checked in the smoke test. */
class WebTileLabelsScenarioTest {

    private fun WebHarness.choose(labels: TileLabels) {
        game.onLabels()
        game.onLabelsSelected(labels)
        game.onLabelsAnswered(ok = true)
    }

    @Test
    fun `labels button after load opens the dialog`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onLabels()

        val dialog = page.state.dialog
        assertTrue(dialog is WebDialog.Labels, "$dialog")
        assertEquals(TileLabels.entries, dialog.options.map { it.labels })
        assertEquals(LabelOption(TileLabels.PAIRS, "Pairs", listOf("11", "12", "13", "14")), dialog.options[2])
        assertEquals(TileLabels.NUMBERS, dialog.selected)
        assertEquals(
            listOf(mapOf("opened_from" to "toolbar", "screen" to "play", "current_labels" to "numbers")),
            page.ownProps("screen_labels_dialog"),
        )
    }

    @Test
    fun `choosing letter pairs relabels the tiles`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        assertEquals("7", page.state.tileTexts[6])

        page.choose(TileLabels.LETTER_PAIRS)

        assertNull(page.state.dialog)
        assertEquals(TileLabels.LETTER_PAIRS, page.state.labels)
        assertEquals("BC", page.state.tileTexts[6])
        assertEquals(
            listOf(mapOf("from" to "numbers", "to" to "letter_pairs", "from_source" to "default", "screen" to "play", "board_size" to 4)),
            page.ownProps("tile_labels_changed"),
        )
    }

    @Test
    fun `cancel changes nothing`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onLabels()
        page.game.onLabelsSelected(TileLabels.LETTERS)
        assertEquals(TileLabels.LETTERS, (page.state.dialog as WebDialog.Labels).selected)
        page.game.onLabelsAnswered(ok = false)

        assertNull(page.state.dialog)
        assertEquals("7", page.state.tileTexts[6])
        assertEquals(listOf<Any?>("labels"), page.props("dialog_cancelled").map { it["dialog"] })

        page.choose(TileLabels.NUMBERS)
        assertTrue(page.props("tile_labels_changed").isEmpty())
    }

    @Test
    fun `preference survives a reload`() = webTest {
        val first = WebHarness(backgroundScope).startOn()
        first.choose(TileLabels.LETTERS)

        val reloaded = WebHarness(backgroundScope, store = first.store).start()

        assertEquals(TileLabels.LETTERS, reloaded.state.labels)
        assertEquals("letters", reloaded.props("app_launched").single()["tile_labels"])
    }

    @Test
    fun `a save keeps its labels for that game only`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.LETTERS)
        page.game.onSave()
        page.game.onSaveAnswered("foo")
        page.choose(TileLabels.PAIRS)

        page.game.onLoad()
        page.game.onLoadChosen("foo")
        assertEquals(TileLabels.LETTERS, page.state.labels)
        assertEquals(listOf<Any?>(true), page.props("load_command_used").map { it["labels_differ_from_preference"] })

        page.game.onNew()
        page.game.onSizeChosen(4)
        assertEquals(TileLabels.PAIRS, page.state.labels)

        page.game.onLoad()
        page.game.onLoadChosen("foo")
        page.choose(TileLabels.NUMBERS)
        assertEquals(listOf<Any?>("default", "preference", "save"), page.props("tile_labels_changed").map { it["from_source"] })
    }

    @Test
    fun `labels on the solution screen`() = webTest {
        // 4x4 two moves from solved; Mordie finds them, then five shifts of column 3 reveal Give Up.
        val page = WebHarness(backgroundScope).startOn(intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15))
        page.mordieSearches.latest.progress = SearchProgress(
            2, found = true, elapsedMs = 0,
            solution = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2)),
        )
        repeat(5) { page.game.onArrow(Arrow.UP, 3) }
        page.game.onGiveUp()
        page.game.onGiveUpAnswered(true)
        val steps = assertNotNull(page.state.solution).steps

        page.choose(TileLabels.LETTERS)

        assertEquals(page.state.tiles.map { "${'A' + it - 1}" }, page.state.tileTexts)
        assertEquals(steps, assertNotNull(page.state.solution).steps)
        assertEquals(listOf<Any?>("solution"), page.props("tile_labels_changed").map { it["screen"] })
    }
}
