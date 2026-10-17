package mordie

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

/** JVM-only: polls from the test thread, outside the search's Dispatchers.Default pool (GH-103). */
class BackgroundMordieSearchThreadTest {

    /** 3x3, needs 2 moves: row 0 rotated, then column 2 rotated. */
    private val twoMoves = intArrayOf(2, 0, 8, 3, 4, 1, 6, 7, 5)

    @Test
    fun `a 3x3 search is found and visible from the calling thread`() = runBlocking<Unit> {
        val search = BackgroundMordieSearch.Factory().start(twoMoves, ruledOut = -1)

        withTimeout(10_000) { while (!search.progress().found) delay(5) }

        assertEquals(2, search.progress().moves)
    }
}
