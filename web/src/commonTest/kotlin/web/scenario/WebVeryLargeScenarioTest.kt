package web.scenario

import board_model.Solvability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import viewmodel.GiveUpButton
import viewmodel.TileLabels
import viewmodel.TileSelection
import viewmodel.UnsolvableSaveChoice
import web.Arrow
import web.Focus
import web.LabelOption
import web.SaveEntry
import web.VERY_LARGE_TITLE
import web.VERY_LARGE_WARNING
import web.WebDialog

/** GH-207 scenarios: very large boards (7x7 to 9x9) in the web client, through [WebHarness]. */
class WebVeryLargeScenarioTest {

    private val sizePicker = WebDialog.Size(listOf(3, 4, 5, 6), current = 4, veryLarge = listOf(7, 8, 9))

    private fun oneShiftFromSolved(side: Int) = intArrayOf(side - 1) + IntArray(side - 1) { it } + IntArray(side * side - side) { it + side }

    /** Solved, with the first two tiles swapped: [side] odd makes it unsolvable. */
    private fun swapped(side: Int) = solved(side).also { it[0] = 1; it[1] = 0 }

    @Test
    fun `New offers a Very large section and starts 7x7, 8x8 and 9x9 boards`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onNew()
        assertEquals(sizePicker, page.state.dialog)
        assertEquals("Very large", VERY_LARGE_TITLE)
        assertEquals("Harder to solve, and the tiles may be too small to use comfortably.", VERY_LARGE_WARNING)
        page.game.onSizeChosen(7)
        page.game.onNew()
        page.game.onSizeChosen(8)
        page.game.onNew()
        page.game.onSizeChosen(9)

        assertEquals(9, page.state.side)
        assertEquals((1..81).map { "$it" }, page.state.tileTexts.sortedBy { it.toInt() })
        assertEquals(
            listOf(7, 8, 9).map { mapOf("size" to it, "trigger" to "toolbar") },
            page.ownProps("new_game_size_selected"),
        )
    }

    @Test
    fun `the startup picker shows the Very large section too`() = webTest {
        val page = WebHarness(backgroundScope).start()
        assertEquals(sizePicker, page.state.dialog)

        page.game.onSizeChosen(null)

        assertNull(page.state.dialog)
        assertEquals(listOf("size"), page.props("dialog_cancelled").map { it["dialog"] })
    }

    @Test
    fun `the Very large section follows the unsolvable-save New game too`() = webTest {
        val page = WebHarness(backgroundScope).seed("odd", UNSOLVABLE_3X3).startOn()

        page.game.onLoad()
        page.game.onLoadChosen("odd")
        page.game.onUnsolvableChoice(UnsolvableSaveChoice.NEW_GAME)

        assertEquals(sizePicker, page.state.dialog)
        assertEquals(listOf("unsolvable_save"), page.props("screen_size_dialog").map { it["opened_from"] })
    }

    @Test
    fun `Mordie plays 9x9, and a win is Mordie's ending`() = webTest {
        // TODO: 1. Start a 9x9 game from New
        // TODO: 2. A search starts on the 81-tile original and the opening line shows - no "too much for me" line
        // TODO: 3. Give Up is dimmed
        // TODO: 4. After 5 shifts Give Up is lit and the `Found one in N moves, but maybe there's a shorter one!` line shows
        // TODO: 5. Start a game one shift from solved (oneShiftFromSolved(9)) and make that shift
        // TODO: 6. The title is Mordie's ending, not `Congratulations ✓`
        // TODO: 7. board_solved carries board_size 9 and mordie_result, mordie_moves and mordie_proven_shortest
    }

    @Test
    fun `a win on 9x9 offers Mordie's solution`() = webTest {
        // TODO: 1. Start a game one shift from solved on 9x9 and make that shift
        // TODO: 2. The `[ Mordie's solution ]` button shows
        // TODO: 3. Press it - the solution screen opens with opened_from=after_win
    }

    @Test
    fun `a 9x9 save keeps Mordie's long answer`() = webTest {
        // TODO: 1. Play a 9x9 game until Mordie's answer (a few hundred moves) is found
        // TODO: 2. Save, then load
        // TODO: 3. The line shows at once, with the same N
        // TODO: 4. Give Up opens the solution screen on the full path
    }

    @Test
    fun `Letters shows Numbers on 7x7, and is unavailable in the Tile labels dialog`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onLabels()
        page.game.onLabelsSelected(TileLabels.LETTERS)
        page.game.onLabelsAnswered(ok = true)

        page.game.onNew()
        page.game.onSizeChosen(7)
        assertEquals(TileLabels.NUMBERS, page.state.labels)

        page.game.onLabels()
        val dialog = page.state.dialog as WebDialog.Labels
        assertEquals(LabelOption(TileLabels.LETTERS, "Letters", listOf("up to 5x5"), available = false), dialog.options[4])
        assertTrue(dialog.options.filter { it.labels != TileLabels.LETTERS }.all { it.available })
        page.game.onLabelsAnswered(ok = true)
        assertEquals("letters", page.preferences.get("tile_labels"))

        val reloaded = WebHarness(backgroundScope, store = page.store).startOn()
        assertEquals(TileLabels.LETTERS, reloaded.state.labels)
    }

    @Test
    fun `7x7 and 9x9 new games can always be solved, 8x8 never needs a fix`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        for (side in 7..9) repeat(100) {
            page.game.onNew()
            page.game.onSizeChosen(side)
            assertTrue(Solvability.isSolvable(page.board.boardArray), "side $side: ${page.board.boardArray.toList()}")
        }
    }

    @Test
    fun `an 8x8 game saves, loads and is listed with its size`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onNew()
        page.game.onSizeChosen(8)
        page.game.onArrow(Arrow.LEFT, 0)
        val tiles = page.state.tiles
        page.game.onSave()
        page.game.onSaveAnswered("big")

        val other = WebHarness(backgroundScope, store = page.store).startOn()
        other.game.onLoad()
        assertEquals(WebDialog.Load(restore = false, listOf(SaveEntry("big", "big (8x8)"))), other.state.dialog)
        other.game.onLoadChosen("big")

        assertEquals(8, other.state.side)
        assertEquals(tiles, other.state.tiles)
    }

    @Test
    fun `an unsolvable 7x7 save is marked and can be repaired`() = webTest {
        val page = WebHarness(backgroundScope).seed("foo", swapped(7)).startOn()

        page.game.onLoad()
        assertEquals(WebDialog.Load(restore = false, listOf(SaveEntry("foo", "foo (7x7) unsolvable"))), page.state.dialog)
        page.game.onLoadChosen("foo")
        assertTrue(page.state.dialog is WebDialog.Unsolvable, "${page.state.dialog}")
        page.game.onUnsolvableChoice(UnsolvableSaveChoice.REPAIR)

        assertEquals(7, page.state.side)
        assertTrue(Solvability.isSolvable(page.board.boardArray))
        assertEquals(listOf(7), page.props("unsolvable_save_resolved").map { it["board_size"] })
    }

    private fun WebHarness.startNine(): WebHarness = apply {
        game.onNew()
        game.onSizeChosen(9)
    }

    @Test
    fun `an arrow shifts a row on a 9x9 board`() = webTest {
        val page = WebHarness(backgroundScope).startOn().startNine()
        val before = page.state.tiles.subList(0, 9)

        page.game.onArrow(Arrow.LEFT, 0)

        assertEquals(before.drop(1) + before.first(), page.state.tiles.subList(0, 9))
        assertEquals(1, page.state.moves)
    }

    @Test
    fun `a drag of 3 tiles on a 9x9 board is 3 shifts and one move`() = webTest {
        val page = WebHarness(backgroundScope).startOn().startNine()

        page.game.onDragStart(3, 0, 100.0, 100.0, 40.0)
        page.game.onDragMove(220.0, 100.0)
        page.game.onDragEnd()

        assertEquals(1, page.state.moves)
        assertEquals(3, page.mordie.shifts)
    }

    @Test
    fun `keyboard selection wraps at the 9th row and column, and a row shifts`() = webTest {
        val keys = WebHarness(backgroundScope).remembering().startOn(swapped(9))

        repeat(8) { keys.press("s", from = Focus.Board) }
        assertEquals(TileSelection(8, 0), keys.state.selection)
        keys.press("s", from = Focus.Board)
        assertEquals(TileSelection(0, 0), keys.state.selection)
        repeat(9) { keys.press("d", from = Focus.Board) }
        assertEquals(TileSelection(0, 0), keys.state.selection)
        val row = keys.state.tiles.subList(0, 9)
        keys.press("ArrowLeft", from = Focus.Board)
        assertEquals(row.drop(1) + row.first(), keys.state.tiles.subList(0, 9))
    }

    @Test
    fun `the size picker opens focused on the current big size`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onNew()
        page.game.onSizeChosen(9)

        page.game.onNew()
        assertEquals(Focus.SizeChoice(9), page.state.focus)
        assertEquals(sizePicker.copy(current = 9), page.state.dialog)

        page.game.onEscape()
        assertNull(page.state.dialog)
        assertEquals(9, page.state.side)
    }
}
