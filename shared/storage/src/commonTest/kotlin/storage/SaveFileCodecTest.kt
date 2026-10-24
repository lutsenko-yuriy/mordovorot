package storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SaveFileCodecTest {

    private val state = intArrayOf(3, 0, 2, 1)

    /** Rows 0 and 1 each swapped: one right rotation of each solves it. */
    private val swapped = intArrayOf(1, 0, 3, 2)
    private val path = listOf(SavedMove(isRow = true, index = 0, amount = 1), SavedMove(isRow = true, index = 1, amount = 1))
    private val found = SavedMordie(swapped, 2, found = true, solution = path)

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

    // --- Mordie's solution, lines 6-7 (GH-126) ---

    @Test
    fun `encode puts Mordie's solution on line 6 of a found save`() {
        assertEquals("2\n3 0 2 1\n7\n1 0 3 2\n2 found\nr0:1 r1:1\n", SaveFileCodec.encode(2, state, 7, found))
    }

    @Test
    fun `encode marks a solution-screen save on line 7`() {
        assertEquals(
            "2\n1 0 3 2\n0\n1 0 3 2\n2 found\nr0:1 r1:1\nsolution\n",
            SaveFileCodec.encode(2, swapped, 0, SavedMordie(swapped, 2, found = true, solution = path, solutionScreen = true)),
        )
    }

    @Test
    fun `encode writes columns as c and an empty path as no line 6`() {
        val columns = listOf(SavedMove(isRow = false, index = 0, amount = 1), SavedMove(isRow = false, index = 1, amount = 1))
        assertEquals(
            "2\n3 0 2 1\n7\n2 3 0 1\n2 found\nc0:1 c1:1\n",
            SaveFileCodec.encode(2, state, 7, SavedMordie(intArrayOf(2, 3, 0, 1), 2, found = true, solution = columns)),
        )
        assertEquals(
            "2\n0 1 2 3\n0\n0 1 2 3\n0 found\n",
            SaveFileCodec.encode(2, intArrayOf(0, 1, 2, 3), 0, SavedMordie(intArrayOf(0, 1, 2, 3), 0, found = true, solution = emptyList())),
        )
    }

    @Test
    fun `encode rejects a solution that doesn't fit the save`() {
        val bad = listOf(
            SavedMordie(swapped, 2, found = false, solution = path),
            SavedMordie(swapped, 3, found = true, solution = path),
            SavedMordie(swapped, 2, found = true, solution = listOf(path[0], path[0])),
            SavedMordie(swapped, 2, found = true, solution = listOf(path[0], SavedMove(isRow = true, index = 2, amount = 1))),
            SavedMordie(swapped, 2, found = true, solution = listOf(path[0], SavedMove(isRow = true, index = 1, amount = 2))),
            SavedMordie(swapped, 2, found = true, solutionScreen = true),
        )
        for (mordie in bad) assertFailsWith<IllegalArgumentException>(mordie.toString()) { SaveFileCodec.encode(2, state, 7, mordie) }
    }

    @Test
    fun `decode round-trips the solution and the solution-screen flag`() {
        val screen = SavedMordie(swapped, 2, found = true, solution = path, solutionScreen = true)

        assertEquals(SavedBoard(2, state, 7, found), SaveFileCodec.decode("foo", SaveFileCodec.encode(2, state, 7, found)))
        assertEquals(SavedBoard(2, swapped, 0, screen), SaveFileCodec.decode("foo", SaveFileCodec.encode(2, swapped, 0, screen)))
    }

    @Test
    fun `decode reads a found save from before GH-126 as having no solution`() {
        for (text in listOf("2\n3 0 2 1\n7\n1 0 3 2\n2 found", "2\n3 0 2 1\n7\n1 0 3 2\n2 found\n \n")) {
            assertEquals(SavedMordie(swapped, 2, found = true), SaveFileCodec.decode("legacy", text).mordie, text)
        }
    }

    @Test
    fun `decode rejects a bad solution or solution-screen line`() {
        val bad = mapOf(
            "on a searching save" to "1 0 3 2\n2 searching\nr0:1 r1:1\n",
            "unknown axis" to "1 0 3 2\n2 found\nx0:1 r1:1\n",
            "line out of range" to "1 0 3 2\n2 found\nr0:1 r2:1\n",
            "zero rotation" to "1 0 3 2\n2 found\nr0:1 r1:0\n",
            "full rotation" to "1 0 3 2\n2 found\nr0:1 r1:2\n",
            "no amount" to "1 0 3 2\n2 found\nr0 r1:1\n",
            "extra part" to "1 0 3 2\n2 found\nr0:1 r1:1:1\n",
            "too few steps" to "1 0 3 2\n2 found\nr0:1\n",
            "doesn't solve" to "1 0 3 2\n2 found\nr0:1 c0:1\n",
            "line 7 without line 6" to "1 0 3 2\n2 found\n\nsolution\n",
            "line 7 not 'solution'" to "1 0 3 2\n2 found\nr0:1 r1:1\nsolved\n",
            "line 6 without Mordie data" to "\n\nr0:1 r1:1\n",
        )
        for ((case, lines) in bad) assertFailsWith<SaveFileFormatException>(case) { SaveFileCodec.decode("foo", "2\n3 0 2 1\n7\n$lines") }
    }

    @Test
    fun `encode puts the labels on line 8, padding lines 4-7 with blanks, GH-139`() {
        val screen = SavedMordie(swapped, 2, found = true, solution = path, solutionScreen = true)
        assertEquals("2\n3 0 2 1\n7\n\n\n\n\npairs\n", SaveFileCodec.encode(2, state, 7, null, "pairs"))
        assertEquals(
            "2\n3 0 2 1\n7\n1 0 3 2\n8 searching\n\n\nletters\n",
            SaveFileCodec.encode(2, state, 7, SavedMordie(swapped, 8, found = false), "letters"),
        )
        assertEquals("2\n3 0 2 1\n7\n1 0 3 2\n2 found\nr0:1 r1:1\n\nnumbers\n", SaveFileCodec.encode(2, state, 7, found, "numbers"))
        assertEquals(
            "2\n1 0 3 2\n0\n1 0 3 2\n2 found\nr0:1 r1:1\nsolution\nletter_pairs\n",
            SaveFileCodec.encode(2, swapped, 0, screen, "letter_pairs"),
        )
    }

    @Test
    fun `encode rejects blank or spaced labels`() {
        for (labels in listOf("", " ", "two words", "pairs\n")) {
            assertFailsWith<IllegalArgumentException>(labels) { SaveFileCodec.encode(2, state, 7, null, labels) }
        }
    }

    @Test
    fun `decode round-trips the labels, with and without Mordie data`() {
        val screen = SavedMordie(swapped, 2, found = true, solution = path, solutionScreen = true)
        for (mordie in listOf(null, SavedMordie(swapped, 8, found = false), found)) {
            assertEquals(SavedBoard(2, state, 7, mordie, "pairs"), SaveFileCodec.decode("foo", SaveFileCodec.encode(2, state, 7, mordie, "pairs")))
        }
        assertEquals(SavedBoard(2, swapped, 0, screen, "pairs"), SaveFileCodec.decode("foo", SaveFileCodec.encode(2, swapped, 0, screen, "pairs")))
    }

    @Test
    fun `decode reads a missing or blank line 8 as no labels, and keeps an unknown name`() {
        for (text in listOf("2\n3 0 2 1\n7\n", "2\n3 0 2 1\n7\n\n\n\n\n", "2\n3 0 2 1\n7\n\n\n\n\n  \n")) {
            assertNull(SaveFileCodec.decode("foo", text).labels, text)
        }
        assertEquals("emoji", SaveFileCodec.decode("foo", "2\n3 0 2 1\n7\n\n\n\n\n emoji \n").labels)
    }

    @Test
    fun `decode keeps only line 8's first word, so the labels always encode again`() {
        val labels = SaveFileCodec.decode("foo", "2\n3 0 2 1\n7\n\n\n\n\nletter pairs\n").labels
        assertEquals("letter", labels)
        SaveFileCodec.encode(2, state, 7, null, labels)
    }
}
