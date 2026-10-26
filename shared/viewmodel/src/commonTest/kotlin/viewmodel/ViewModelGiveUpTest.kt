package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import mordie.Mordie
import mordie.SearchProgress
import mordie.SolutionMove
import storage.SavedBoard
import storage.SavedMordie
import storage.SavedMove
import testing.FakeMordieSearchFactory
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-126: the Give Up button, the solution screen and its replay, through the viewModel. */
class ViewModelGiveUpTest {

    /** A 3x3 two moves from solved: row 0 right once, then column 0 down once. */
    private val original = intArrayOf(1, 2, 3, 6, 4, 5, 0, 7, 8)
    private val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 0, amount = 1))
    private val afterFirstStep = intArrayOf(3, 1, 2, 6, 4, 5, 0, 7, 8)
    private val solved = IntArray(9) { it }
    private val steps = listOf("1. Row 1 → ×1", "2. Col 1 ↓ ×1")
    private val moves = listOf(SolutionStep(isRow = true, index = 0, forward = true, count = 1), SolutionStep(isRow = false, index = 0, forward = true, count = 1))

    private val factory = FakeMordieSearchFactory()
    private val analytics = RecordingAnalyticsService()
    private val board = BoardImpl(3).apply { restoreState(original) }
    private val mordie = Mordie(factory)
    private val unsavedProgress = UnsavedProgress()
    private val viewModel = testViewModel(board, analytics = analytics, mordie = mordie, unsavedProgress = unsavedProgress)

    private fun event(name: String) = analytics.events.single { it.name == name }.properties
    private fun count(name: String) = analytics.events.count { it.name == name }

    /** Mordie searching [original], its search having found [solution] (N = 2). */
    private fun searchFinds(solution: List<SolutionMove>? = path) {
        mordie.startFresh(original)
        factory.latest.progress = SearchProgress(2, found = true, elapsedMs = 0, solution = solution)
    }

    /** Five shifts of column 2 (one move, board unsolved), so Mordie's check reveals its find. */
    private fun reveal(solution: List<SolutionMove>? = path) {
        searchFinds(solution)
        repeat(5) { viewModel.shiftUp(2) }
    }

    /** Gives up after the reveal plus one more move (a shift). */
    private fun givenUp() {
        reveal()
        viewModel.shiftLeft(1)
        viewModel.giveUp()
    }

    /** The player solves [original] in Mordie's two moves, before any check. */
    private fun solveDirectly() {
        viewModel.shiftRight(0)
        viewModel.shiftDown(0)
    }

    // --- the button ---

    @Test
    fun `Give Up is dimmed before Found it, even with the solution already found`() {
        searchFinds()

        assertEquals(GiveUpButton.DIMMED, viewModel.giveUpButton())
    }

    @Test
    fun `Give Up goes live once Found it shows`() {
        reveal()

        assertEquals(GiveUpButton.GIVE_UP, viewModel.giveUpButton())
    }

    @Test
    fun `Give Up stays dimmed when the reveal had no path, or Mordie sits out`() {
        reveal(solution = null)
        assertEquals(GiveUpButton.DIMMED, viewModel.giveUpButton())

        mordie.clear()
        assertEquals(GiveUpButton.DIMMED, viewModel.giveUpButton())
    }

    @Test
    fun `after a win against a found solution, the button is Mordie's solution`() {
        searchFinds()
        solveDirectly()

        assertTrue(viewModel.isSolved())
        assertEquals(GiveUpButton.MORDIES_SOLUTION, viewModel.giveUpButton())
    }

    @Test
    fun `after a win the button is hidden on You beat me, a found N with no path, or Mordie sitting out`() {
        mordie.startFresh(original)
        solveDirectly()
        assertEquals(GiveUpButton.HIDDEN, viewModel.giveUpButton(), "You beat me")

        board.restoreState(original)
        searchFinds(solution = null)
        solveDirectly()
        assertEquals(GiveUpButton.HIDDEN, viewModel.giveUpButton(), "no path")

        board.restoreState(original)
        mordie.clear()
        solveDirectly()
        assertEquals(GiveUpButton.HIDDEN, viewModel.giveUpButton(), "sat out")
    }

    @Test
    fun `giveUp and showSolution do nothing when their button isn't shown`() {
        searchFinds()
        viewModel.giveUp()
        viewModel.showSolution()

        reveal()
        viewModel.showSolution()

        assertNull(viewModel.solution())
        assertTrue(analytics.events.none { it.name == "game_given_up" || it.name == "screen_solution" })
    }

    // --- entering the screen ---

    @Test
    fun `giving up shows the original arrangement with Mordie's steps, ready to play`() {
        givenUp()

        assertEquals(SolutionState(steps, current = null, atStart = true, atEnd = false, moves = moves), viewModel.solution())
        assertContentEquals(original, viewModel.boardState())
        assertContentEquals(original, board.boardArray)
        assertEquals(0, viewModel.moveCount())
        assertFalse(viewModel.isSolved())
        assertEquals(GiveUpButton.HIDDEN, viewModel.giveUpButton())
        assertEquals("Here's how I did it in 2 moves!", viewModel.mordieLine())
        assertNull(viewModel.mordieEnding())
    }

    @Test
    fun `giving up tracks game_given_up with the moves and shifts since Found it, then screen_solution`() {
        givenUp()

        assertEquals(
            mapOf(
                "board_size" to 3, "move_count" to 2, "solution_length" to 2,
                "moves_since_solution_found" to 1, "shifts_since_solution_found" to 1,
            ),
            event("game_given_up"),
        )
        assertEquals(mapOf("opened_from" to "give_up", "board_size" to 3, "solution_length" to 2), event("screen_solution"))
        assertEquals(listOf("game_given_up", "screen_solution"), analytics.events.map { it.name }.takeLast(2))
    }

    @Test
    fun `giving up on a save loaded already found leaves out the since-found counts`() = runTest {
        val saved = SavedMordie(original, 2, found = true, solution = path.map { SavedMove(it.isRow, it.index, it.amount) })
        val saves = FakeSaveRepository(mutableMapOf("m" to SavedBoard(3, original, 0, saved)))
        val loaded = testViewModel(board, saves, analytics, mordie = mordie)
        FakeViewModelUi().drive(loaded) { loaded.loadGame("m") }
        loaded.shiftLeft(1)

        loaded.giveUp()

        assertEquals(mapOf("board_size" to 3, "move_count" to 1, "solution_length" to 2), event("game_given_up"))
    }

    @Test
    fun `Mordie's solution after a win opens the screen with no game_given_up`() {
        searchFinds()
        solveDirectly()

        viewModel.showSolution()

        assertEquals(SolutionState(steps, current = null, atStart = true, atEnd = false, moves = moves), viewModel.solution())
        assertContentEquals(original, viewModel.boardState())
        assertEquals(mapOf("opened_from" to "after_win", "board_size" to 3, "solution_length" to 2), event("screen_solution"))
        assertEquals(0, count("game_given_up"))
    }

    @Test
    fun `entering the screen clears unsaved progress, and nothing on it sets it`() {
        givenUp()
        assertFalse(viewModel.hasUnsavedProgress())

        viewModel.shiftLeft(0)
        viewModel.replayNext()
        viewModel.replayNext()
        viewModel.replayPrevious()
        viewModel.replayRestart()

        assertFalse(unsavedProgress.unsaved)
    }

    @Test
    fun `shifts do nothing on the solution screen`() {
        givenUp()

        viewModel.shiftLeft(0)
        viewModel.shiftRight(1)
        viewModel.shiftUp(0)
        viewModel.shiftDown(1)
        viewModel.shiftRow(0, 2)
        viewModel.shiftColumn(0, -1)

        assertContentEquals(original, board.boardArray)
        assertEquals(0, viewModel.moveCount())
        assertTrue(viewModel.solution()!!.atStart)
    }

    // --- the replay ---

    @Test
    fun `Next steps Mordie's solution one shift at a time on the screen's board, never the game's`() {
        givenUp()

        assertEquals(SolutionStep(isRow = true, index = 0, forward = true, count = 1), viewModel.replayNext())
        assertEquals(SolutionState(steps, current = 0, atStart = false, atEnd = false, moves = moves), viewModel.solution())
        assertContentEquals(afterFirstStep, viewModel.boardState())
        viewModel.replayNext()

        assertContentEquals(solved, viewModel.boardState())
        assertEquals(SolutionState(steps, current = 1, atStart = false, atEnd = true, moves = moves), viewModel.solution())
        assertNull(viewModel.replayNext())
        assertContentEquals(original, board.boardArray)
        assertEquals(0, count("board_solved"))
    }

    @Test
    fun `Prev undoes a shift and Restart goes back to the original arrangement`() {
        givenUp()
        viewModel.replayNext()
        viewModel.replayNext()

        assertEquals(SolutionStep(isRow = false, index = 0, forward = false, count = 1), viewModel.replayPrevious())
        assertContentEquals(afterFirstStep, viewModel.boardState())
        viewModel.replayRestart()

        assertEquals(SolutionState(steps, current = null, atStart = true, atEnd = false, moves = moves), viewModel.solution())
        assertContentEquals(original, viewModel.boardState())
        assertNull(viewModel.replayPrevious())
    }

    @Test
    fun `solution_replay_started fires on each Next off the start, restart once a run has started on this visit`() {
        givenUp()

        viewModel.replayNext()
        viewModel.replayNext()
        assertEquals(mapOf("restart" to false, "board_size" to 3, "solution_length" to 2), event("solution_replay_started"))

        viewModel.replayRestart()
        viewModel.replayNext()
        viewModel.replayPrevious()
        viewModel.replayNext()

        val starts = analytics.events.filter { it.name == "solution_replay_started" }.map { it.properties["restart"] }
        assertEquals(listOf(false, true, true), starts)
    }

    @Test
    fun `solution_replay_finished fires once per run on reaching the end, with whether Prev was used`() {
        // Three shifts, so Prev can step back without returning to the start. Never checked against the board.
        reveal(solution = path + SolutionMove(isRow = true, index = 1, amount = 1))
        viewModel.giveUp()
        viewModel.replayNext()
        viewModel.replayNext()
        viewModel.replayPrevious()
        repeat(2) { viewModel.replayNext() }
        assertEquals(mapOf("board_size" to 3, "solution_length" to 3, "stepped_back" to true), event("solution_replay_finished"))

        viewModel.replayPrevious()
        viewModel.replayNext()
        viewModel.replayRestart()
        repeat(3) { viewModel.replayNext() }

        val finished = analytics.events.filter { it.name == "solution_replay_finished" }.map { it.properties["stepped_back"] }
        assertEquals(listOf(true, false), finished)
    }

    @Test
    fun `off the screen there is no solution and the replay does nothing`() {
        reveal()

        viewModel.replayRestart()

        assertNull(viewModel.solution())
        assertNull(viewModel.replayNext())
        assertNull(viewModel.replayPrevious())
        assertEquals(0, count("solution_replay_started"))
    }
}
