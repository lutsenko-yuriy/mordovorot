import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Covers [resolveBoardSize] - the `--size=N` launch flag (GH-44). Only the `--size=N` form
 *  (no space) is recognized; a missing, non-numeric, or out-of-range value all resolve to
 *  `null` - not [board_model.BoardSize.DEFAULT] - so a caller can tell "the flag wasn't usable"
 *  apart from "the flag asked for exactly the default" (audit finding on PR #52: that
 *  distinction is what `sizeChosenAtLaunch` needs to avoid silently suppressing the startup
 *  prompts on an invalid flag). */
class BoardSizeArgTest {

    @Test
    fun `no --size given resolves to null`() {
        assertNull(resolveBoardSize(emptyArray()))
    }

    @Test
    fun `--size=3 resolves to 3`() {
        assertEquals(3, resolveBoardSize(arrayOf("--size=3")))
    }

    @Test
    fun `--size=5 resolves to 5`() {
        assertEquals(5, resolveBoardSize(arrayOf("--size=5")))
    }

    @Test
    fun `--size=2 warns and resolves to null`() {
        val warnings = mutableListOf<String>()

        val size = resolveBoardSize(arrayOf("--size=2"), warnInvalidSize = { warnings.add(it) })

        assertNull(size)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `--size=9 warns and resolves to null`() {
        val warnings = mutableListOf<String>()

        val size = resolveBoardSize(arrayOf("--size=9"), warnInvalidSize = { warnings.add(it) })

        assertNull(size)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `--size=abc warns and resolves to null`() {
        val warnings = mutableListOf<String>()

        val size = resolveBoardSize(arrayOf("--size=abc"), warnInvalidSize = { warnings.add(it) })

        assertNull(size)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `--size= (blank value) warns and resolves to null`() {
        val warnings = mutableListOf<String>()

        val size = resolveBoardSize(arrayOf("--size="), warnInvalidSize = { warnings.add(it) })

        assertNull(size)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `--size 4 (space, not equals) is not recognized - resolves to null with no warning from resolveBoardSize itself`() {
        val warnings = mutableListOf<String>()

        val size = resolveBoardSize(arrayOf("--size", "4"), warnInvalidSize = { warnings.add(it) })

        assertNull(size)
        assertEquals(emptyList(), warnings)
    }
}
