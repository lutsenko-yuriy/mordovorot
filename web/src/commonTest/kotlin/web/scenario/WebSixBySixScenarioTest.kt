package web.scenario

import kotlin.test.Test
import kotlin.test.assertContentEquals
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

    /** Solved 6x6 with row 0 shifted one tile right: `shiftLeft(0)` solves it. */
    private val oneShiftFromSolved = intArrayOf(5, 0, 1, 2, 3, 4) + IntArray(30) { it + 6 }

    @Test
    fun `New offers 6x6 and starts a 6x6 board`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onNew()
        assertEquals(WebDialog.Size(listOf(3, 4, 5, 6), current = 4, veryLarge = listOf(7, 8, 9)), page.state.dialog)
        page.game.onSizeChosen(6)

        assertEquals(6, page.state.side)
        assertEquals((1..36).map { "$it" }, page.state.tileTexts.sortedBy { it.toInt() })
        assertEquals(listOf(mapOf("size" to 6, "trigger" to "toolbar")), page.ownProps("new_game_size_selected"))
    }

    @Test
    fun `Mordie plays 6x6, and a win is Mordie's ending`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onNew()
        page.game.onSizeChosen(6)

        assertEquals(36, page.mordieSearches.latest.original.size)
        assertEquals("Alright! Let's sort this one out!", page.state.mordie)
        assertEquals(GiveUpButton.DIMMED, page.state.giveUp)
        page.mordieSearches.latest.progress = fastAnswer(40)
        repeat(5) { page.game.onArrow(Arrow.UP, 0) }
        assertEquals("Found one in 40 moves, but maybe there's a shorter one! Can you beat it?", page.state.mordie)
        assertEquals(GiveUpButton.GIVE_UP, page.state.giveUp)

        val won = WebHarness(backgroundScope).startOn(oneShiftFromSolved)
        won.mordieSearches.latest.progress = fastAnswer(40)
        won.game.onArrow(Arrow.LEFT, 0)

        assertTrue(won.state.solved)
        assertEquals("You beat me! 40 moves vs your 1. Mine may not have been the shortest.", won.state.mordie)
        val solved = won.ownProps("board_solved").single()
        assertEquals(6, solved["board_size"])
        assertEquals(40, solved["mordie_moves"])
        assertEquals("beat_mordie", solved["mordie_result"])
        assertEquals(false, solved["mordie_proven_shortest"])
    }

    @Test
    fun `Give Up on 6x6 opens Mordie's solution`() = webTest {
        val page = WebHarness(backgroundScope).startOn(rotatedRow(6))
        page.mordieSearches.latest.progress = fastAnswer(37)
        repeat(5) { page.game.onArrow(Arrow.UP, 2) }

        page.game.solution.onGiveUp()
        assertEquals(WebDialog.GiveUp, page.state.dialog)
        page.game.solution.onGiveUpAnswered(true)

        assertEquals(37, page.state.solution!!.moves.size)
        assertEquals(6, page.props("game_given_up").single()["board_size"])
        assertEquals(6, page.props("screen_solution").single()["board_size"])
        repeat(3) { page.game.solution.onReplayNext() }
        assertEquals(2, page.state.solution?.current)
    }

    @Test
    fun `a 6x6 save with no Mordie data and no moves starts Mordie fresh`() = webTest {
        val page = WebHarness(backgroundScope).seed("foo", rotatedRow(6)).startOn(rotatedRow(6))
        val searches = page.mordieSearches.started.size

        page.game.onLoad()
        page.game.onLoadChosen("foo")

        assertEquals(searches + 1, page.mordieSearches.started.size)
        assertContentEquals(rotatedRow(6), page.mordieSearches.latest.original)
        page.mordieSearches.latest.progress = fastAnswer(37)
        repeat(5) { page.game.onArrow(Arrow.UP, 2) }
        assertEquals(GiveUpButton.GIVE_UP, page.state.giveUp)
    }

    @Test
    fun `a 6x6 save with moves made and no Mordie data sits Mordie out`() = webTest {
        val page = WebHarness(backgroundScope).seed("foo", rotatedRow(6), moves = 2).startOn(rotatedRow(6))
        val searches = page.mordieSearches.started.size

        page.game.onLoad()
        page.game.onLoadChosen("foo")

        assertEquals(searches, page.mordieSearches.started.size)
        assertNull(page.state.mordie)
        assertEquals(GiveUpButton.DIMMED, page.state.giveUp)
        page.game.onArrow(Arrow.RIGHT, 0)

        assertTrue(page.state.solved)
        assertEquals("Congratulations ✓", page.state.mordie)
        val solved = page.ownProps("board_solved").single()
        assertTrue("mordie_result" !in solved && "mordie_moves" !in solved, "$solved")
    }

    @Test
    fun `a 6x6 save with Mordie data resumes from its kept answer`() = webTest {
        val page = WebHarness(backgroundScope).startOn(rotatedRow(6))
        page.mordieSearches.latest.progress = fastAnswer(37)
        repeat(5) { page.game.onArrow(Arrow.UP, 2) }
        page.game.onSave()
        page.game.onSaveAnswered("foo")
        assertEquals(listOf("foo"), page.saves.listSaves())

        page.game.onLoad()
        page.game.onLoadChosen("foo")

        assertEquals("Found one in 37 moves, but maybe there's a shorter one! Can you beat it?", page.state.mordie)
        assertEquals(GiveUpButton.GIVE_UP, page.state.giveUp)
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
        val page = WebHarness(backgroundScope).startOn()
        page.saves.save("six", oneShiftFromSolved, 6, 0, labels = "letters")

        page.game.onLoad()
        page.game.onLoadChosen("six")

        val dialog = page.state.dialog as WebDialog.Labels
        assertEquals("Letters don't fit a 6x6 board (26 letters, 36 tiles) - pick other labels.", dialog.message)
        assertEquals(TileLabels.NUMBERS, dialog.selected)
        assertEquals(false, dialog.options.single { it.labels == TileLabels.LETTERS }.available)
        assertEquals(
            listOf(mapOf("opened_from" to "letters_save", "screen" to "play", "current_labels" to "numbers")),
            page.ownProps("screen_labels_dialog"),
        )
        page.game.onLabelsSelected(TileLabels.PAIRS)
        assertEquals(dialog.message, (page.state.dialog as WebDialog.Labels).message)
        page.game.onLabelsAnswered(ok = true)
        assertEquals(TileLabels.PAIRS, page.state.labels)
        assertEquals("Loaded 'six'.", page.state.status)
        assertEquals(listOf<Any?>("letters_unavailable"), page.props("tile_labels_changed").map { it["from_source"] })

        val reloaded = WebHarness(backgroundScope, store = page.store).startOn()
        reloaded.game.onLoad()
        reloaded.game.onLoadChosen("six")
        reloaded.game.onLabelsAnswered(ok = false)

        assertNull(reloaded.state.dialog)
        assertEquals(TileLabels.NUMBERS, reloaded.state.labels)
        assertEquals((1..6).map { "$it" }, reloaded.state.tileTexts.take(6).sortedBy { it.toInt() })
        assertEquals(listOf<Any?>("labels"), reloaded.props("dialog_cancelled").map { it["dialog"] })
    }
}
