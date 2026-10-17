package storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SaveFileCodecTest {

    private val state = intArrayOf(3, 0, 2, 1)

    @Test
    fun `encode writes square side, values and move count, one per line`() {
        assertEquals("2\n3 0 2 1\n7\n", SaveFileCodec.encode(2, state, 7, null))
    }

    @Test
    fun `encode puts Mordie data on lines 4-5, GH-83`() {
        assertEquals(
            "2\n3 0 2 1\n7\n1 0 3 2\n8 searching\n",
            SaveFileCodec.encode(2, state, 7, SavedMordie(intArrayOf(1, 0, 3, 2), 8, found = false)),
        )
        assertEquals(
            "2\n3 0 2 1\n7\n1 0 3 2\n1 found\n",
            SaveFileCodec.encode(2, state, 7, SavedMordie(intArrayOf(1, 0, 3, 2), 1, found = true)),
        )
    }

    @Test
    fun `encode rejects an invalid board, move count or Mordie data`() {
        assertFailsWith<IllegalArgumentException> { SaveFileCodec.encode(0, intArrayOf(), 0, null) }
        assertFailsWith<IllegalArgumentException> { SaveFileCodec.encode(2, intArrayOf(0, 1, 2), 0, null) }
        assertFailsWith<IllegalArgumentException> { SaveFileCodec.encode(2, intArrayOf(0, 1, 1, 2), 0, null) }
        assertFailsWith<IllegalArgumentException> { SaveFileCodec.encode(2, state, -1, null) }
        assertFailsWith<IllegalArgumentException> { SaveFileCodec.encode(2, state, 0, SavedMordie(intArrayOf(0, 1, 2), 1, found = true)) }
        assertFailsWith<IllegalArgumentException> { SaveFileCodec.encode(2, state, 0, SavedMordie(intArrayOf(1, 0, 3, 2), -1, found = true)) }
    }

    @Test
    fun `decode round-trips encode, with and without Mordie data`() {
        val mordie = SavedMordie(intArrayOf(2, 3, 0, 1), moves = 11, found = true)

        assertEquals(SavedBoard(2, state, 5), SaveFileCodec.decode("foo", SaveFileCodec.encode(2, state, 5, null)))
        assertEquals(SavedBoard(2, state, 5, mordie), SaveFileCodec.decode("foo", SaveFileCodec.encode(2, state, 5, mordie)))
    }

    @Test
    fun `decode treats a missing or blank move count line as 0, GH-62`() {
        for (text in listOf("2\n3 0 2 1", "2\n3 0 2 1\n", "2\n3 0 2 1\n  \n")) {
            assertEquals(SavedBoard(2, state, 0), SaveFileCodec.decode("legacy", text), text)
        }
    }

    @Test
    fun `decode reads a missing or blank line 4 as no Mordie data`() {
        for (text in listOf("2\n3 0 2 1\n7", "2\n3 0 2 1\n7\n", "2\n3 0 2 1\n7\n  \n")) {
            assertNull(SaveFileCodec.decode("legacy", text).mordie, text)
        }
    }

    @Test
    fun `decode accepts Windows line endings`() {
        assertEquals(SavedBoard(2, state, 7), SaveFileCodec.decode("foo", "2\r\n3 0 2 1\r\n7\r\n"))
    }

    @Test
    fun `decode names the save and the reason in its message`() {
        val e = assertFailsWith<SaveFileFormatException> { SaveFileCodec.decode("foo", "") }

        assertEquals("Save 'foo' is corrupted: missing square side", e.message)
    }

    @Test
    fun `decode rejects a corrupted board or move count`() {
        val bad = mapOf(
            "empty" to "",
            "blank first line" to "\n3 0 2 1\n",
            "no values line" to "2\n",
            "non-integer side" to "two\n3 0 2 1\n",
            "non-positive side" to "0\n\n",
            "non-integer tile" to "2\n3 x 2 1\n",
            "wrong value count" to "2\n3 0 2\n",
            "not a permutation" to "2\n3 0 3 1\n",
            "non-integer moves" to "2\n3 0 2 1\nmany\n",
            "negative moves" to "2\n3 0 2 1\n-1\n",
        )
        for ((case, text) in bad) assertFailsWith<SaveFileFormatException>(case) { SaveFileCodec.decode("foo", text) }
    }

    @Test
    fun `decode rejects bad Mordie data`() {
        val bad = listOf(
            "1 0 3 2\n",
            "1 0 3 2\n \n",
            "1 0 3 2\n8 solved\n",
            "1 0 3 2\n-1 searching\n",
            "1 0 3 2\neight found\n",
            "1 0 3 2\n8 found now\n",
            "1 0 2\n8 found\n",
            "1 1 3 2\n8 found\n",
            "1 x 3 2\n8 found\n",
        )
        for (lines in bad) assertFailsWith<SaveFileFormatException>(lines) { SaveFileCodec.decode("foo", "2\n3 0 2 1\n7\n$lines") }
    }
}
