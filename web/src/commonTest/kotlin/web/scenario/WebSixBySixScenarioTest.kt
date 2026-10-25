package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import viewmodel.GiveUpButton
import viewmodel.TileLabels
import web.Arrow
import web.LabelOption
import web.WebDialog

/** GH-160 scenarios: 6x6 boards in the web client, through [WebHarness]. */
class WebSixBySixScenarioTest {

    private val sitsOut = "6x6 is too much for me, you're on your own!"

    /** Solved 6x6 with row 0 shifted one tile right: `shiftLeft(0)` solves it. */
    private val oneShiftFromSolved = intArrayOf(5, 0, 1, 2, 3, 4) + IntArray(30) { it + 6 }

    @Test
    fun `New offers 6x6 and starts a 6x6 board`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onNew()
        assertEquals(WebDialog.Size(listOf(3, 4, 5, 6)), page.state.dialog)
        page.game.onSizeChosen(6)

        assertEquals(6, page.state.side)
        assertEquals((1..36).map { "$it" }, page.state.tileTexts.sortedBy { it.toInt() })
        assertEquals(listOf(mapOf("size" to 6, "trigger" to "toolbar")), page.ownProps("new_game_size_selected"))
    }

    @Test
    fun `Mordie sits 6x6 out, and a win is Congratulations`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onNew()
        page.game.onSizeChosen(6)

        assertEquals(sitsOut, page.state.mordie)
        assertEquals(listOf(16), page.mordieSearches.started.map { it.original.size }, "only the 4x4 game's search")
        assertEquals(GiveUpButton.DIMMED, page.state.giveUp)
        repeat(5) { page.game.onArrow(Arrow.UP, 0) }
        assertNull(page.state.mordie)

        val won = WebHarness(backgroundScope).startOn(oneShiftFromSolved)
        assertEquals(sitsOut, won.state.mordie)
        won.game.onArrow(Arrow.LEFT, 0)

        assertTrue(won.state.solved)
        assertEquals("Congratulations ✓", won.state.mordie)
        assertEquals(GiveUpButton.HIDDEN, won.state.giveUp)
        val solved = won.ownProps("board_solved").single()
        assertEquals(6, solved["board_size"])
        assertTrue("mordie_result" !in solved && "mordie_moves" !in solved, "$solved")
    }

    @Test
    fun `a Letters preference shows Numbers on 6x6, with Letters unavailable in the dialog`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onLabels()
        page.game.onLabelsSelected(TileLabels.LETTERS)
        page.game.onLabelsAnswered(ok = true)

        page.game.onNew()
        page.game.onSizeChosen(6)
        assertEquals(TileLabels.NUMBERS, page.state.labels)

        page.game.onLabels()
        val dialog = page.state.dialog as WebDialog.Labels
        assertEquals(LabelOption(TileLabels.LETTERS, "Letters", listOf("up to 5x5"), available = false), dialog.options[4])
        assertEquals(listOf("AA", "AB", "AC", "AD", "AE", "AF"), dialog.options[5].sample)
        assertTrue(dialog.options.filter { it.labels != TileLabels.LETTERS }.all { it.available })
        page.game.onLabelsSelected(TileLabels.LETTERS)
        assertEquals(TileLabels.NUMBERS, (page.state.dialog as WebDialog.Labels).selected)
        page.game.onLabelsAnswered(ok = true)
        assertEquals("letters", page.preferences.get("tile_labels"))
        assertEquals(1, page.props("tile_labels_changed").size, "only the first change")

        val reloaded = WebHarness(backgroundScope, store = page.store).startOn()
        assertEquals(TileLabels.LETTERS, reloaded.state.labels)
    }

    @Test
    fun `a 6x6 save with Letters asks for labels - OK and Cancel`() = webTest {
        // TODO: store a 6x6 save with Letters labels and load it
        // TODO: assert the Labels dialog opens with the warning message, Letters unavailable, screen_labels_dialog {opened_from: letters_save}
        // TODO: pick Pairs and OK - assert the tiles show pairs
        // TODO: reload, load the save again and Cancel - assert the tiles show numbers and dialog_cancelled {dialog: labels}
    }
}
