package storage

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** GH-243: the board in a share link, ADR-0005. */
class BoardLinkFormatTest {

    // Goldens: the layout is frozen once links are shared.
    @Test
    fun `a solved 2x2 is 0010 and zero digits`() {
        assertEquals("IAA", BoardLinkFormat.encode(2, intArrayOf(0, 1, 2, 3)))
        assertContentEquals(intArrayOf(0, 1, 2, 3), BoardLinkFormat.decode("IAA"))
    }

    @Test
    fun `golden solved 3x3 and 4x4`() {
        assertEquals("MAAAAA", BoardLinkFormat.encode(3, IntArray(9) { it }))
        assertEquals("QAAAAAAAAA", BoardLinkFormat.encode(4, IntArray(16) { it }))
    }

    @Test
    fun `every side from 2 to 15 round trips random arrangements`() {
        val random = Random(243)
        for (side in 2..15) repeat(20) {
            val arrangement = IntArray(side * side) { it }.also { it.shuffle(random) }
            assertContentEquals(arrangement, BoardLinkFormat.decode(BoardLinkFormat.encode(side, arrangement)), "side $side")
        }
    }

    @Test
    fun `a 9x9 link is 75 characters`() {
        assertEquals(75, BoardLinkFormat.encode(9, IntArray(81) { 80 - it }).length)
    }

    @Test
    fun `the text is url safe`() {
        val text = BoardLinkFormat.encode(9, IntArray(81) { 80 - it })
        assertEquals(text.filter { it.isLetterOrDigit() || it == '-' || it == '_' }, text)
    }

    @Test
    fun `text that is not base64url is rejected`() {
        assertFailsWith<BoardLinkFormatException> { BoardLinkFormat.decode("not base64!") }
        assertFailsWith<BoardLinkFormatException> { BoardLinkFormat.decode("MAAA=AAA") }
    }

    @Test
    fun `a side below 2 is rejected`() {
        assertFailsWith<BoardLinkFormatException> { BoardLinkFormat.decode("AAAA") }
        assertFailsWith<BoardLinkFormatException> { BoardLinkFormat.decode("EAAA") }
    }

    @Test
    fun `a digit above its range is rejected`() {
        // Side 2: after 0010 the second digit has 3 values left and takes 2 bits, so 11 is one too many.
        assertFailsWith<BoardLinkFormatException> { BoardLinkFormat.decode("IwA") }
    }

    @Test
    fun `an empty link is rejected`() {
        assertFailsWith<BoardLinkFormatException> { BoardLinkFormat.decode("") }
    }

    @Test
    fun `data after the board is rejected`() {
        val text = BoardLinkFormat.encode(3, IntArray(9) { it })
        assertFailsWith<BoardLinkFormatException> { BoardLinkFormat.decode(text + "AA") }
    }

    @Test
    fun `non zero padding bits are rejected`() {
        val text = BoardLinkFormat.encode(3, IntArray(9) { it })
        val last = text.last()
        val altered = text.dropLast(1) + (if (last == 'A') 'B' else 'A')
        assertFailsWith<BoardLinkFormatException> { BoardLinkFormat.decode(altered) }
    }

    @Test
    fun `encoding refuses a side a link cannot carry or the wrong tile count`() {
        assertFailsWith<IllegalArgumentException> { BoardLinkFormat.encode(1, intArrayOf(0)) }
        assertFailsWith<IllegalArgumentException> { BoardLinkFormat.encode(16, IntArray(256) { it }) }
        assertFailsWith<IllegalArgumentException> { BoardLinkFormat.encode(3, IntArray(8) { it }) }
    }
}
