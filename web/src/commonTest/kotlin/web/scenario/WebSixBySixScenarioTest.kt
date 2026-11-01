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
        // TODO: 1. Start a 6x6 game from New
        // TODO: 2. A search starts on the 36-tile original and the opening line shows - no "too much for me" line
        // TODO: 3. Give Up is dimmed
        // TODO: 4. After 5 shifts Give Up is lit and the `Found one in N moves, but maybe there's a shorter one!` line shows
        // TODO: 5. Start a game one shift from solved (oneShiftFromSolved) and make that shift
        // TODO: 6. The title is Mordie's ending, not `Congratulations ✓`
        // TODO: 7. board_solved carries board_size 6 and mordie_result, mordie_moves and mordie_proven_shortest
    }

    @Test
    fun `Give Up on 6x6 opens Mordie's solution`() = webTest {
        // TODO: 1. Start a 6x6 game and shift 5 times
        // TODO: 2. Press Give Up - the confirm dialog opens
        // TODO: 3. Confirm - the solution screen opens on Mordie's solution
        // TODO: 4. game_given_up and screen_solution fire with board_size 6
        // TODO: 5. Press Next three times - the step counter advances
    }

    @Test
    fun `a 6x6 save with no Mordie data and no moves starts Mordie fresh`() = webTest {
        // TODO: 1. Seed a 6x6 save with 0 moves and no Mordie data
        // TODO: 2. Load it
        // TODO: 3. A search starts from the loaded board
        // TODO: 4. After 5 shifts Give Up is lit
    }

    @Test
    fun `a 6x6 save with moves made and no Mordie data sits Mordie out`() = webTest {
        // TODO: 1. Seed a 6x6 save with moves made and no Mordie data
        // TODO: 2. Load it
        // TODO: 3. No search starts
        // TODO: 4. There is no Mordie line and Give Up is dimmed
        // TODO: 5. Win it - the title is `Congratulations ✓` and board_solved has no mordie_* props
    }

    @Test
    fun `a 6x6 save with Mordie data resumes from its kept answer`() = webTest {
        // TODO: 1. Play a 6x6 game until Mordie's answer is found
        // TODO: 2. Save, then load
        // TODO: 3. The `Found one in N moves…` line shows at once, with the same N
        // TODO: 4. Give Up is lit
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
