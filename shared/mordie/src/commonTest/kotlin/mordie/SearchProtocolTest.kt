package mordie

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SearchProtocolTest {

    @Test
    fun `a fresh start round-trips`() {
        val original = intArrayOf(2, 0, 8, 3, 4, 1, 6, 7, 5)

        val start = SearchProtocol.decodeStart(SearchProtocol.encodeStart(original, ruledOut = -1))

        assertContentEquals(original, start.original)
        assertEquals(-1, start.ruledOut)
    }

    @Test
    fun `a resumed start keeps the saved N`() {
        val start = SearchProtocol.decodeStart(SearchProtocol.encodeStart(IntArray(25) { 24 - it }, ruledOut = 9))

        assertEquals(9, start.ruledOut)
        assertEquals(25, start.original.size)
    }

    @Test
    fun `progress while searching round-trips`() {
        val progress = SearchProgress(moves = 7, found = false, elapsedMs = 1234)

        assertEquals(progress, SearchProtocol.decodeProgress(SearchProtocol.encodeProgress(progress)))
    }

    @Test
    fun `a found progress keeps its solution`() {
        val progress = SearchProgress(
            moves = 2,
            found = true,
            elapsedMs = 15,
            solution = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 2, amount = 2)),
        )

        assertEquals(progress, SearchProtocol.decodeProgress(SearchProtocol.encodeProgress(progress)))
    }

    @Test
    fun `an unproven fast solution round-trips with its source and ruled out length`() {
        val progress = SearchProgress(
            moves = 40,
            found = true,
            elapsedMs = 3,
            solution = listOf(SolutionMove(isRow = true, index = 1, amount = 2)),
            ruledOut = 12,
            provenShortest = false,
            source = SolutionSource.FAST_SOLVER,
        )

        assertEquals(progress, SearchProtocol.decodeProgress(SearchProtocol.encodeProgress(progress)))
    }

    @Test
    fun `an unknown source letter fails loudly`() {
        assertFailsWith<IllegalArgumentException> { SearchProtocol.decodeProgress("P|1|1|0|1|x|0|~") }
    }

    @Test
    fun `an empty solution is not a missing one`() {
        val progress = SearchProgress(moves = 0, found = true, elapsedMs = 0, solution = emptyList())

        assertEquals(progress, SearchProtocol.decodeProgress(SearchProtocol.encodeProgress(progress)))
    }

    @Test
    fun `the message kind is told apart`() {
        assertEquals(SearchProtocol.Kind.START, SearchProtocol.kindOf(SearchProtocol.encodeStart(intArrayOf(0), -1)))
        assertEquals(
            SearchProtocol.Kind.PROGRESS,
            SearchProtocol.kindOf(SearchProtocol.encodeProgress(SearchProgress(1, false, 0))),
        )
    }

    @Test
    fun `a malformed message fails loudly`() {
        assertFailsWith<IllegalArgumentException> { SearchProtocol.decodeStart("P|1|0|0|~") }
        assertFailsWith<IllegalArgumentException> { SearchProtocol.decodeProgress("S|-1|0,1") }
        assertFailsWith<IllegalArgumentException> { SearchProtocol.decodeProgress("P|x|0|0|0|~|0|~") }
        assertFailsWith<IllegalArgumentException> { SearchProtocol.kindOf("nonsense") }
    }

    @Test
    fun `a start without a kept solution decodes with none`() {
        val start = SearchProtocol.decodeStart(SearchProtocol.encodeStart(intArrayOf(1, 0), ruledOut = -1))

        assertNull(start.best)
    }

    @Test
    fun `a three-field start from an older page decodes with no kept solution`() {
        val start = SearchProtocol.decodeStart("S|9|1,0,2")

        assertEquals(9, start.ruledOut)
        assertNull(start.best)
    }

    @Test
    fun `a start carries the kept solution`() {
        val best = listOf(SolutionMove(isRow = true, index = 2, amount = -1), SolutionMove(isRow = false, index = 0, amount = 3))

        val start = SearchProtocol.decodeStart(SearchProtocol.encodeStart(IntArray(25) { 24 - it }, ruledOut = 9, best = best))

        assertEquals(best, start.best)
        assertEquals(9, start.ruledOut)
    }

    @Test
    fun `a start carries an empty kept solution`() {
        val start = SearchProtocol.decodeStart(SearchProtocol.encodeStart(intArrayOf(0, 1), ruledOut = -1, best = emptyList()))

        assertEquals(emptyList(), start.best)
    }
}
