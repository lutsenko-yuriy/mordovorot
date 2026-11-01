package mordie

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchProgressTest {

    @Test
    fun `a searching progress defaults to ruled out equal to N, unproven, no source`() {
        val progress = SearchProgress(moves = 7, found = false, elapsedMs = 0)

        assertEquals(7, progress.ruledOut)
        assertFalse(progress.provenShortest)
        assertNull(progress.source)
    }

    @Test
    fun `a found progress defaults to the pre-GH-222 meaning, the exact minimum`() {
        val progress = SearchProgress(moves = 5, found = true, elapsedMs = 0, solution = emptyList())

        assertEquals(4, progress.ruledOut)
        assertTrue(progress.provenShortest)
        assertEquals(SolutionSource.EXACT_SEARCH, progress.source)
    }
}
