import kotlin.test.Test
import kotlin.test.assertEquals

/** Covers [resolveBoardSize] - the `--size=N` launch flag (GH-44). Only the `--size=N` form
 *  (no space) is recognized; a non-numeric or out-of-range value warns and falls back to
 *  [board_model.BoardSize.DEFAULT] rather than crashing at boot. */
class BoardSizeArgTest {

    @Test
    fun `no --size given resolves to the default`() {
        assertEquals(4, resolveBoardSize(emptyArray()))
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
    fun `--size=2 warns and falls back to the default`() {
        val warnings = mutableListOf<String>()

        val size = resolveBoardSize(arrayOf("--size=2"), warnInvalidSize = { warnings.add(it) })

        assertEquals(4, size)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `--size=9 warns and falls back to the default`() {
        val warnings = mutableListOf<String>()

        val size = resolveBoardSize(arrayOf("--size=9"), warnInvalidSize = { warnings.add(it) })

        assertEquals(4, size)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `--size=abc warns and falls back to the default`() {
        val warnings = mutableListOf<String>()

        val size = resolveBoardSize(arrayOf("--size=abc"), warnInvalidSize = { warnings.add(it) })

        assertEquals(4, size)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `--size= (blank value) warns and falls back to the default`() {
        val warnings = mutableListOf<String>()

        val size = resolveBoardSize(arrayOf("--size="), warnInvalidSize = { warnings.add(it) })

        assertEquals(4, size)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `--size 4 (space, not equals) is not recognized - resolves to the default with no warning from resolveBoardSize itself`() {
        val warnings = mutableListOf<String>()

        val size = resolveBoardSize(arrayOf("--size", "4"), warnInvalidSize = { warnings.add(it) })

        assertEquals(4, size)
        assertEquals(emptyList(), warnings)
    }
}
