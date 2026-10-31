package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.SearchProgress
import mordie.SolutionMove
import viewmodel.GiveUpButton
import viewmodel.KeyboardLayout
import web.Arrow
import web.KeyHints
import web.a11y.Focus
import web.a11y.fallbackFocus

/** GH-171: the solution screen from the keyboard, and where focus goes on the way there. */
class WebKeyboardSolutionScenarioTest {

    /** 4x4 two moves from solved: row 0 right once, then column 1 down twice. */
    private val original = intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)
    private val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2))

    private fun WebHarness.reveal() {
        mordieSearches.latest.progress = SearchProgress(path.size, found = true, elapsedMs = 0, solution = path)
        repeat(5) { game.onArrow(Arrow.UP, 3) }
    }

    private fun WebHarness.givenUp(): WebHarness {
        reveal()
        game.solution.onGiveUp()
        game.solution.onGiveUpAnswered(true)
        return this
    }

    private fun WebHarness.key(key: String, repeat: Boolean = false) = press(key, from = Focus.Board, repeat = repeat)

    private val WebHarness.step get() = state.solution?.current

    @Test
    fun `the solution keys step, restart and scroll in both layouts`() = webTest {
        for (layout in KeyboardLayout.entries) {
            val page = WebHarness(backgroundScope).remembering(keys = layout).startOn(original).givenUp()

            for (next in listOf("ArrowRight", "d", "L", " ", "Enter")) {
                assertTrue(page.key(next), next)
                assertEquals(0, page.step, next)
                assertTrue(page.key("r"))
                assertNull(page.step)
            }
            for (prev in listOf("ArrowLeft", "A", "h")) {
                page.key("l")
                assertTrue(page.key(prev), prev)
                assertNull(page.step, prev)
            }
            for (up in listOf("ArrowUp", "w", "K")) assertTrue(page.key(up), up)
            for (down in listOf("ArrowDown", "S", "j")) assertTrue(page.key(down), down)
            assertEquals(listOf(-1, -1, -1, 1, 1, 1), page.scrolled)
        }
    }

    @Test
    fun `the solution keys need focus on the board, as the board keys do`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(original).givenUp()

        assertFalse(page.press("l", from = Focus.ReplayNext))
        assertFalse(page.press("l"))
        assertNull(page.step)
    }

    @Test
    fun `mouse mode has no solution keys`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original).givenUp()

        assertFalse(page.key("l"))
        assertFalse(page.key("r"))
        assertNull(page.step)
    }

    @Test
    fun `Give Up's Yes and Mordie's solution put focus on the board in keyboard mode`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(original).givenUp()
        assertEquals(Focus.Board, page.state.focus)
        assertTrue(page.state.boardFocusable)

        val won = WebHarness(backgroundScope).remembering().startOn(original)
        won.reveal()
        repeat(5) { won.game.onArrow(Arrow.DOWN, 3) }
        won.key("l")
        won.game.onTilePressed(0, 1)
        won.key("j")
        won.key("j")
        assertEquals(GiveUpButton.MORDIES_SOLUTION, won.state.giveUp)
        won.game.solution.onMordiesSolution()
        assertEquals(Focus.Board, won.state.focus)
    }

    @Test
    fun `in mouse mode they keep focus off the board`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original).givenUp()

        assertEquals(Focus.ReplayNext, page.state.focus)
        assertFalse(page.state.boardFocusable)
    }

    @Test
    fun `a press on a tile gives the solution's board focus, without selecting`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(original).givenUp()

        page.game.onTilePressed(1, 1)
        assertEquals(Focus.Board, page.state.focus)
        assertNull(page.state.selection)
    }

    @Test
    fun `the solution shows its own hints, and a step keeps the board`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(original).givenUp()
        assertEquals(KeyHints.SOLUTION, page.state.hints)

        val before = page.state
        page.key("l")
        assertTrue(page.state.keepsBoardFrom(before))
    }

    @Test
    fun `a solved board keeps the board's hints and takes no focus`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn()
        val hints = page.state.hints
        page.press("h", from = Focus.Board)

        assertEquals(hints, page.state.hints)
        assertFalse(page.state.boardFocusable)
    }

    @Test
    fun `a solving key shift passes focus to Mordie's solution, else New`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn()
        page.press("h", from = Focus.Board)
        assertEquals(GiveUpButton.HIDDEN, page.state.giveUp)
        assertEquals(Focus.NewButton, fallbackFocus(Focus.Board.key, page.state))

        val revealed = WebHarness(backgroundScope).remembering().startOn()
        revealed.mordieSearches.latest.progress = SearchProgress(1, found = true, elapsedMs = 0, solution = listOf(SolutionMove(isRow = true, index = 0, amount = 1)))
        repeat(5) { revealed.game.onArrow(Arrow.UP, 3) }
        repeat(5) { revealed.game.onArrow(Arrow.DOWN, 3) }
        revealed.press("h", from = Focus.Board)
        assertEquals(GiveUpButton.MORDIES_SOLUTION, revealed.state.giveUp)
        assertEquals(Focus.GiveUpRow, fallbackFocus(Focus.Board.key, revealed.state))
    }

    @Test
    fun `non-Latin keys step through the solution screen`() = webTest {
        // TODO: 1. Open the solution screen with its board focused.
        // TODO: 2. Press в at KeyD: Next.
        // TODO: 3. Press ф at KeyA: Prev.
        // TODO: 4. Press к at KeyR: Restart.
    }
}
