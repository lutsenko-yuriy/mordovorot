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
        val page = WebHarness(backgroundScope).startOn()
        page.game.onNew()
        page.game.onSizeChosen(9)

        assertEquals(81, page.mordieSearches.latest.original.size)
        assertEquals("Alright! Let's sort this one out!", page.state.mordie)
        assertEquals(GiveUpButton.DIMMED, page.state.giveUp)
        page.mordieSearches.latest.progress = fastAnswer(334)
        repeat(5) { page.game.onArrow(Arrow.UP, 0) }
        assertEquals("Found one in 334 moves, but maybe there's a shorter one! Can you beat it?", page.state.mordie)
        assertEquals(GiveUpButton.GIVE_UP, page.state.giveUp)

        val won = WebHarness(backgroundScope).startOn(oneShiftFromSolved(9))
        won.mordieSearches.latest.progress = fastAnswer(334)
        won.game.onArrow(Arrow.LEFT, 0)

        assertTrue(won.state.solved)
        assertEquals("You beat me! 334 moves vs your 1. Mine may not have been the shortest.", won.state.mordie)
        val solved = won.ownProps("board_solved").single()
        assertEquals(9, solved["board_size"])
        assertEquals(334, solved["mordie_moves"])
        assertEquals("beat_mordie", solved["mordie_result"])
        assertEquals(false, solved["mordie_proven_shortest"])
    }

    @Test
    fun `a win on 9x9 offers Mordie's solution`() = webTest {
        val page = WebHarness(backgroundScope).startOn(oneShiftFromSolved(9))
        page.mordieSearches.latest.progress = fastAnswer(334)
        page.game.onArrow(Arrow.LEFT, 0)

        assertEquals(GiveUpButton.MORDIES_SOLUTION, page.state.giveUp)
        page.game.solution.onMordiesSolution()

        assertEquals(334, page.state.solution!!.moves.size)
        assertEquals("after_win", page.props("screen_solution").single()["opened_from"])
    }

    @Test
    fun `a 9x9 save keeps Mordie's long answer`() = webTest {
        val page = WebHarness(backgroundScope).startOn(rotatedRow(9))
        page.mordieSearches.latest.progress = fastAnswer(334)
        repeat(5) { page.game.onArrow(Arrow.UP, 2) }
        page.game.onSave()
        page.game.onSaveAnswered("foo")
        assertEquals(334, page.saves.load("foo")!!.mordie!!.solution!!.size)

        page.game.onLoad()
        page.game.onLoadChosen("foo")

        assertEquals("Found one in 334 moves, but maybe there's a shorter one! Can you beat it?", page.state.mordie)
        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(true)
        assertEquals(334, page.state.solution!!.moves.size)
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
