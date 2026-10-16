package mordie

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackgroundMordieSearchTest {

    /** 3x3, needs 2 moves: row 0 rotated, then column 2 rotated. */
    private val twoMoves = intArrayOf(2, 0, 8, 3, 4, 1, 6, 7, 5)

    /** 5x5 reversed (an even permutation, so solvable) - its search never finishes within a test. */
    private val hard = IntArray(25) { 24 - it }

    @Test
    fun `a 3x3 search is found and visible from the calling thread`() = runBlocking<Unit> {
        val search = BackgroundMordieSearch.Factory().start(twoMoves, ruledOut = -1)

        withTimeout(10_000) { while (!search.progress().found) delay(5) }

        assertEquals(2, search.progress().moves)
    }

    @Test
    fun `elapsed time freezes once found`() = runBlocking<Unit> {
        val search = BackgroundMordieSearch.Factory().start(twoMoves, ruledOut = -1)
        withTimeout(10_000) { while (!search.progress().found) delay(5) }
        val elapsed = search.progress().elapsedMs

        delay(50)

        assertEquals(elapsed, search.progress().elapsedMs)
    }

    @Test
    fun `a resumed search starts at the saved N`() {
        val search = BackgroundMordieSearch.Factory().start(hard, ruledOut = 9)

        assertTrue(search.progress().moves >= 9)
        search.cancel()
    }

    @Test
    fun `cancel stops the search`() = runBlocking<Unit> {
        val search = BackgroundMordieSearch(hard, ruledOut = -1)
        delay(50)

        search.cancel()
        withTimeout(5_000) { search.join() }
        val stopped = search.progress()
        delay(50)

        assertEquals(stopped, search.progress())
        assertFalse(stopped.found)
    }

    @Test
    fun `the NONE factory never searches`() {
        val search = MordieSearchFactory.NONE.start(twoMoves, ruledOut = 4)

        assertEquals(SearchProgress(moves = 4, found = false, elapsedMs = 0), search.progress())
        search.cancel()
    }
}
