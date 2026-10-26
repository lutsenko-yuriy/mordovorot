package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.SearchProgress
import mordie.SolutionMove
import viewmodel.GiveUpButton
import viewmodel.SolutionState
import viewmodel.SolutionStep
import web.Arrow
import web.WebDialog
import web.drag.Axis
import web.drag.LineOffset

/** GH-126 scenarios: giving up and Mordie's solution screen in the browser, through [WebHarness]
 *  stepping the replay by hand with Prev / Next / Restart. Same setup as
 *  [WebStartupScenarioTest]. */
class WebGiveUpScenarioTest {

    /** 4x4 two moves from solved: row 0 right once, then column 1 down twice. */
    private val original = intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)
    private val afterFirstStep = intArrayOf(0, 9, 2, 3, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)
    private val afterOneColumnShift = intArrayOf(0, 5, 2, 3, 4, 9, 6, 7, 8, 13, 10, 11, 12, 1, 14, 15)
    private val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2))
    private val steps = listOf("1. Row 1 → ×1", "2. Col 2 ↓ ×2")
    private val moves = listOf(SolutionStep(isRow = true, index = 0, forward = true, count = 1), SolutionStep(isRow = false, index = 1, forward = true, count = 2))
    private val solutionLine = "Here's how I did it in 2 moves!"

    private fun WebHarness.searchFinds() {
        mordieSearches.latest.progress = SearchProgress(path.size, found = true, elapsedMs = 0, solution = path)
    }

    /** Mordie finds its solution, then five shifts of column 3 (one move) reveal it. */
    private fun WebHarness.reveal() {
        searchFinds()
        repeat(5) { game.onArrow(Arrow.UP, 3) }
    }

    private fun WebHarness.givenUp(): WebHarness {
        reveal()
        game.onGiveUp()
        game.onGiveUpAnswered(true)
        return this
    }

    /** Right on row 0, then down twice on column 1: Mordie's own solution. */
    private fun WebHarness.solve() {
        game.onArrow(Arrow.RIGHT, 0)
        repeat(2) { game.onArrow(Arrow.DOWN, 1) }
    }

    @Test
    fun `Give Up is disabled until Mordie finds its solution`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original)
        assertEquals(GiveUpButton.DIMMED, page.state.giveUp)

        page.game.onGiveUp()
        assertNull(page.state.dialog)
        assertTrue(page.props("screen_give_up_dialog").isEmpty())

        page.reveal()
        assertEquals(GiveUpButton.GIVE_UP, page.state.giveUp)
    }

    @Test
    fun `cancel keeps the game and confirm opens the solution screen`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original)
        page.reveal()
        page.game.onArrow(Arrow.LEFT, 2)
        val before = page.state

        page.game.onGiveUp()
        assertEquals(WebDialog.GiveUp, page.state.dialog)
        page.game.onGiveUpAnswered(false)
        assertEquals(before.unfocused(), page.state.unfocused())
        assertEquals(listOf("give_up"), page.props("dialog_cancelled").map { it["dialog"] })

        page.game.onGiveUp()
        page.game.onGiveUpAnswered(true)
        assertNull(page.state.dialog)
        assertEquals(shown(original), page.state.tiles)
        assertFalse(page.state.movesShown)
        assertEquals(solutionLine, page.state.mordie)
        assertEquals(SolutionState(steps, current = null, atStart = true, atEnd = false, moves = moves), page.state.solution)
        assertEquals(GiveUpButton.HIDDEN, page.state.giveUp)
        assertFalse(page.state.arrowsEnabled)

        page.game.onArrow(Arrow.LEFT, 0)
        page.game.onDragStart(0, 0, x = 0.0, y = 0.0, pitch = 10.0)
        page.game.onDragMove(25.0, 0.0)
        page.game.onDragEnd()
        assertEquals(shown(original), page.state.tiles)
        assertNull(page.state.drag)
        assertFalse(page.game.hasUnsavedProgress())

        val dialogs = page.ownProps("screen_give_up_dialog")
        assertEquals(listOf(mapOf<String, Any?>("board_size" to 4, "move_count" to 2)), dialogs.take(1))
        assertEquals(2, page.props("game_given_up").single()["move_count"])
        assertEquals("give_up", page.props("screen_solution").single()["opened_from"])
    }

    @Test
    fun `Next and Prev step the replay one shift at a time, with a slide, and Restart goes back to the start`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original).givenUp()

        page.game.onReplayPrevious()
        page.game.onReplayRestart()
        assertEquals(shown(original), page.state.tiles)

        page.game.onReplayNext()
        assertEquals(shown(afterFirstStep), page.state.tiles)
        assertEquals(LineOffset(Axis.ROW, line = 0, tiles = -1.0), page.state.slide)
        assertEquals(SolutionState(steps, current = 0, atStart = false, atEnd = false, moves = moves), page.state.solution)
        assertEquals(false, page.props("solution_replay_started").single()["restart"])

        page.game.onReplayNext()
        assertEquals(shown(afterOneColumnShift), page.state.tiles)
        assertEquals(LineOffset(Axis.COLUMN, line = 1, tiles = -1.0), page.state.slide)
        assertEquals(1, page.state.solution?.current)

        page.game.onReplayPrevious()
        assertEquals(shown(afterFirstStep), page.state.tiles)
        assertEquals(LineOffset(Axis.COLUMN, line = 1, tiles = 1.0), page.state.slide)
        repeat(2) { page.game.onReplayNext() }
        assertEquals(shown(solved(4)), page.state.tiles)
        assertEquals(true, page.state.solution?.atEnd)
        val atEnd = page.state
        page.game.onReplayNext()
        assertEquals(atEnd.tiles, page.state.tiles)
        assertEquals(true, page.props("solution_replay_finished").single()["stepped_back"])

        page.game.onReplayRestart()
        assertEquals(shown(original), page.state.tiles)
        assertNull(page.state.slide)
        assertEquals(SolutionState(steps, current = null, atStart = true, atEnd = false, moves = moves), page.state.solution)

        page.game.onReplayNext()
        page.game.onReplayPrevious()
        assertEquals(shown(original), page.state.tiles)
        assertEquals(SolutionState(steps, current = null, atStart = true, atEnd = false, moves = moves), page.state.solution)
        assertFalse(page.game.hasUnsavedProgress())
        assertTrue(page.props("board_solved").isEmpty())
    }

    @Test
    fun `after a win, Mordie's solution opens the screen, and after You beat me there is none`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original)
        page.searchFinds()
        page.solve()
        assertEquals(GiveUpButton.MORDIES_SOLUTION, page.state.giveUp)

        page.game.onMordiesSolution()
        assertNull(page.state.dialog)
        assertEquals(shown(original), page.state.tiles)
        assertEquals(solutionLine, page.state.mordie)
        assertEquals("after_win", page.props("screen_solution").single()["opened_from"])
        assertTrue(page.props("game_given_up").isEmpty())

        val beaten = WebHarness(backgroundScope).startOn(original)
        beaten.solve()
        assertEquals(GiveUpButton.HIDDEN, beaten.state.giveUp)
        beaten.game.onMordiesSolution()
        assertNull(beaten.state.solution)
        assertTrue(beaten.props("screen_solution").isEmpty())
    }

    @Test
    fun `a solution-screen save reopens the screen, and New and Load leave it`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original).givenUp()
        repeat(2) { page.game.onReplayNext() }
        page.game.onSave()
        page.game.onSaveAnswered("s")

        page.game.onNew()
        page.game.onSizeChosen(4)
        assertNull(page.state.solution)
        assertTrue(page.state.movesShown)
        assertTrue(page.state.arrowsEnabled)

        page.game.onLoad()
        page.game.onLoadChosen("s")
        assertEquals(shown(original), page.state.tiles)
        assertEquals(SolutionState(steps, current = null, atStart = true, atEnd = false, moves = moves), page.state.solution)
        assertEquals(listOf("give_up", "load"), page.props("screen_solution").map { it["opened_from"] })

        val reloaded = WebHarness(backgroundScope, store = page.store).start()
        reloaded.game.onLoadChosen("s")
        assertEquals(shown(original), reloaded.state.tiles)
        assertEquals(steps, reloaded.state.solution?.steps)
        assertEquals("startup_prompt", reloaded.props("screen_solution").single()["opened_from"])

        page.seed("plain", original, moves = 3)
        page.game.onLoad()
        page.game.onLoadChosen("plain")
        assertNull(page.state.solution)
        assertEquals(3, page.state.moves)
        page.game.onArrow(Arrow.LEFT, 2)
        assertEquals(4, page.state.moves)
    }
}
