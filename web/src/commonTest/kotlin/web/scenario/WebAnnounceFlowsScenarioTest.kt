package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import mordie.SearchProgress
import mordie.SolutionMove
import storage.SavedMordie
import storage.SavedMove
import viewmodel.UnsolvableSaveChoice
import web.Arrow

/** A solvable 5x5: solved, with row 0 shifted one tile right. */
private val FIVE = intArrayOf(4, 0, 1, 2, 3) + IntArray(20) { it + 5 }

/** 4x4 two moves from solved: row 0 right once, then column 1 down twice (Mordie's solution). */
private val ORIGINAL = intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)
private val PATH = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2))

private fun WebHarness.searchFinds() {
    mordieSearches.latest.progress = SearchProgress(PATH.size, found = true, elapsedMs = 0, solution = PATH)
}

/** GH-166 scenarios: what a screen reader hears from the status line, New, Load, startup and the
 *  solution screen. */
class WebAnnounceFlowsScenarioTest {

    @Test
    fun `the same status message twice is announced twice`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        repeat(2) {
            page.game.onSave()
            page.game.onSaveAnswered("a")
        }

        assertEquals(listOf("Saved as 'a'.", "Saved as 'a'."), page.said)
    }

    @Test
    fun `a rejected save name is announced`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onSave()
        page.game.onSaveAnswered("a b")

        assertEquals(listOf("'a b' isn't a usable save name (no spaces) - not saved."), page.said)
    }

    @Test
    fun `toolbar New announces the new game, and Cancel announces nothing`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onNew()
        page.game.onSizeChosen(5)
        page.game.onNew()
        page.game.onSizeChosen(null)

        assertEquals(listOf("New 5x5 game"), page.said)
    }

    @Test
    fun `startup with no saves announces the new game`() = webTest {
        val picked = WebHarness(backgroundScope).start()
        picked.game.onSizeChosen(4)
        assertEquals(listOf("New 4x4 game"), picked.said)

        val cancelled = WebHarness(backgroundScope).start()
        cancelled.game.onSizeChosen(null)
        assertEquals(listOf("New 4x4 game"), cancelled.said)
    }

    @Test
    fun `a load announces the save's name, size and moves`() = webTest {
        val page = WebHarness(backgroundScope).seed("foo", FIVE, moves = 12).startOn()

        page.game.onLoad()
        page.game.onLoadChosen("foo")

        assertEquals("Loaded 'foo'.", page.state.status)
        assertEquals(listOf("Loaded 'foo', 5x5, 12 moves"), page.said)
    }

    @Test
    fun `loading a solved save announces it solved`() = webTest {
        val page = WebHarness(backgroundScope).seed("done", solved(4), moves = 14).startOn()

        page.game.onLoad()
        page.game.onLoadChosen("done")

        assertEquals(listOf("Loaded 'done', 4x4, solved in 14 moves"), page.said)
    }

    @Test
    fun `loading a save on the solution screen announces the solution`() = webTest {
        val mordie = SavedMordie(ORIGINAL, 2, found = true, PATH.map { SavedMove(it.isRow, it.index, it.amount) }, solutionScreen = true)
        val page = WebHarness(backgroundScope).seed("sol", ORIGINAL, mordie = mordie).startOn()

        page.game.onLoad()
        page.game.onLoadChosen("sol")

        assertEquals(listOf("Loaded 'sol', 4x4, solution, 2 moves"), page.said)
    }

    @Test
    fun `the startup restore announces the load`() = webTest {
        val page = WebHarness(backgroundScope).seed("foo", ONE_SHIFT_FROM_SOLVED).start()

        page.game.onLoadChosen("foo")

        assertEquals(listOf("Loaded 'foo', 4x4, 0 moves"), page.said)
    }

    @Test
    fun `a load error is announced`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.store.entries["mordovorot.save.bad"] = "x\n0 1 2 3\n"

        page.game.onLoad()
        page.game.onLoadChosen("bad")

        assertEquals(listOf("Save 'bad' is corrupted: square side is not an integer"), page.said)
    }

    @Test
    fun `Can't be solved - New game announces the new game, Repair announces its message`() = webTest {
        val page = WebHarness(backgroundScope).seed("odd", UNSOLVABLE_3X3).startOn()
        val unsolvable = "'odd (3x3)' can't be solved - no sequence of shifts reaches the solved board."

        page.game.onLoad()
        page.game.onLoadChosen("odd")
        page.game.onUnsolvableChoice(UnsolvableSaveChoice.NEW_GAME)
        page.game.onSizeChosen(3)
        // D12: the message is read as a status, then again as the dialog's.
        assertEquals(listOf(unsolvable, "New 3x3 game"), page.said)
        page.said.clear()

        page.game.onLoad()
        page.game.onLoadChosen("odd")
        page.game.onUnsolvableChoice(UnsolvableSaveChoice.REPAIR)

        assertEquals(listOf(unsolvable, "Repaired 'odd' (two tiles swapped) and loaded it."), page.said)
    }

    @Test
    fun `Give Up's solution screen announces itself and each step`() = webTest {
        val page = WebHarness(backgroundScope).startOn(ORIGINAL)
        page.searchFinds()
        // Five shifts of column 4 reveal Give Up.
        repeat(5) { page.game.onArrow(Arrow.UP, 3) }
        page.said.clear()

        page.game.onGiveUp()
        page.game.onGiveUpAnswered(true)
        page.game.onReplayNext()
        // Column 2 down 2 is two shifts: each Next re-reads it.
        page.game.onReplayNext()
        page.game.onReplayNext()
        assertEquals(
            listOf("Solution, 2 moves", "Step 1 of 2: Row 1 right 1", "Step 2 of 2: Column 2 down 2", "Step 2 of 2: Column 2 down 2"),
            page.said,
        )
        page.said.clear()

        repeat(3) { page.game.onReplayPrevious() }
        assertEquals("Start, step 0 of 2", page.said.last())
        page.game.onReplayNext()
        page.said.clear()
        page.game.onReplayRestart()

        assertEquals(listOf("Start, step 0 of 2"), page.said)
    }

    @Test
    fun `Mordie's solution after a win announces the solution screen`() = webTest {
        val page = WebHarness(backgroundScope).startOn(ORIGINAL)
        page.searchFinds()
        page.game.onArrow(Arrow.RIGHT, 0)
        repeat(2) { page.game.onArrow(Arrow.DOWN, 1) }
        page.said.clear()

        page.game.onMordiesSolution()

        assertEquals(listOf("Solution, 2 moves"), page.said)
    }
}
