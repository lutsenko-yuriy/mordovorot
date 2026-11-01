package web

import mordie.SearchProgress
import mordie.SearchProtocol
import mordie.SolutionMove
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkerMordieSearchTest {

    private class FakePort : SearchPort {
        val sent = mutableListOf<String>()
        var terminated = false
        override var onMessage: (String) -> Unit = {}
        override fun send(message: String) { sent += message }
        override fun terminate() { terminated = true }
        fun receive(progress: SearchProgress) = onMessage(SearchProtocol.encodeProgress(progress))
    }

    private val original = intArrayOf(2, 0, 8, 3, 4, 1, 6, 7, 5)

    private fun start(ruledOut: Int = -1): Pair<FakePort, WorkerMordieSearch> {
        val port = FakePort()
        return port to WorkerMordieSearch.Factory { port }.start(original, ruledOut) as WorkerMordieSearch
    }

    @Test
    fun `start sends the arrangement and the saved N`() {
        val (port, _) = start(ruledOut = 5)

        val sent = SearchProtocol.decodeStart(port.sent.single())
        assertContentEquals(original, sent.original)
        assertEquals(5, sent.ruledOut)
    }

    @Test
    fun `progress is the saved N until the worker reports`() {
        val (_, search) = start(ruledOut = 5)

        assertEquals(SearchProgress(moves = 5, found = false, elapsedMs = 0), search.progress())
    }

    @Test
    fun `progress follows the worker's messages`() {
        val (port, search) = start()

        port.receive(SearchProgress(moves = 3, found = false, elapsedMs = 40))

        assertEquals(SearchProgress(moves = 3, found = false, elapsedMs = 40), search.progress())
    }

    @Test
    fun `a found progress carries the solution`() {
        val (port, search) = start()
        val solution = listOf(SolutionMove(isRow = true, index = 0, amount = 1))

        port.receive(SearchProgress(moves = 1, found = true, elapsedMs = 9, solution = solution))

        assertTrue(search.progress().found)
        assertEquals(solution, search.progress().solution)
    }

    @Test
    fun `cancel terminates the worker and freezes progress`() {
        val (port, search) = start()
        port.receive(SearchProgress(moves = 4, found = false, elapsedMs = 20))

        search.cancel()
        port.receive(SearchProgress(moves = 6, found = false, elapsedMs = 90))

        assertTrue(port.terminated)
        assertEquals(4, search.progress().moves)
    }

    @Test
    fun `a message after the solution is found is ignored`() {
        val (port, search) = start()
        port.receive(SearchProgress(moves = 2, found = true, elapsedMs = 5, solution = emptyList()))

        port.receive(SearchProgress(moves = 3, found = false, elapsedMs = 50))

        assertTrue(search.progress().found)
        assertEquals(2, search.progress().moves)
        assertFalse(port.terminated)
    }
}
