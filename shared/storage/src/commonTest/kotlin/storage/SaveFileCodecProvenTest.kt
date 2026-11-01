package storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** GH-224: line 9 of a save, `unproven <R>`, for a found answer not proven shortest. */
class SaveFileCodecProvenTest {

    /** 3x3 with rows 0 and 1 each shifted right once; rotating each right twice solves it. */
    private val original = intArrayOf(2, 0, 1, 5, 3, 4, 6, 7, 8)
    private val path = listOf(SavedMove(isRow = true, index = 0, amount = 2), SavedMove(isRow = true, index = 1, amount = 2))
    private val board = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
    private val unproven = SavedMordie(original, 2, found = true, solution = path, provenShortest = false, ruledOut = 0)

    private fun encode(mordie: SavedMordie?, labels: String? = null) = SaveFileCodec.encode(3, board, 4, mordie, labels)

    private fun decode(text: String) = SaveFileCodec.decode("s", text)

    private val head = "3\n0 1 2 3 4 5 6 7 8\n4\n2 0 1 5 3 4 6 7 8\n2 found\nr0:2 r1:2\n"

    @Test
    fun `an unproven answer writes line 9 after blank lines 7 and 8`() {
        assertEquals(head + "\n\nunproven 0\n", encode(unproven))
    }

    @Test
    fun `line 9 follows the solution-screen flag and the labels`() {
        assertEquals(head + "solution\nnumbers\nunproven 0\n", encode(unproven.withScreen(), labels = "numbers"))
    }

    @Test
    fun `an unproven answer round-trips`() {
        assertEquals(unproven, decode(encode(unproven)).mordie)
        assertEquals(unproven.withScreen(), decode(encode(unproven.withScreen(), labels = "numbers")).mordie)
    }

    @Test
    fun `a proven answer writes no line 9 and reads as proven`() {
        val proven = SavedMordie(original, 2, found = true, solution = path)

        assertEquals(head, encode(proven))
        assertEquals(true, decode(head).mordie!!.provenShortest)
    }

    @Test
    fun `an unproven answer with no solution round-trips`() {
        val noPath = SavedMordie(original, 5, found = true, provenShortest = false, ruledOut = 3)

        assertEquals(noPath, decode(encode(noPath)).mordie)
    }

    @Test
    fun `a first word that is not unproven on line 9 is ignored`() {
        val mordie = decode(head + "\n\nfuture 7\n").mordie!!

        assertEquals(true, mordie.provenShortest)
    }

    @Test
    fun `a malformed unproven line is a broken save`() {
        listOf("unproven", "unproven x", "unproven -1", "unproven 1", "unproven 0 0").forEach { line ->
            assertFailsWith<SaveFileFormatException>(line) { decode(head + "\n\n$line\n") }
        }
    }

    @Test
    fun `an unproven line on a searching save is a broken save`() {
        val searching = "3\n0 1 2 3 4 5 6 7 8\n4\n2 0 1 5 3 4 6 7 8\n3 searching\n\n\n\nunproven 1\n"

        assertFailsWith<SaveFileFormatException> { decode(searching) }
    }

    @Test
    fun `encode rejects an unproven flag it cannot write`() {
        assertFailsWith<IllegalArgumentException> { encode(SavedMordie(original, 3, found = false, provenShortest = false, ruledOut = 1)) }
        assertFailsWith<IllegalArgumentException> { encode(unproven.copyRuledOut(1)) }
        assertFailsWith<IllegalArgumentException> { encode(unproven.copyRuledOut(-1)) }
    }

    @Test
    fun `a save written without line 9 reads as proven`() {
        val old = encode(SavedMordie(original, 2, found = true, solution = path))

        assertEquals(true, decode(old).mordie!!.provenShortest)
    }

    @Test
    fun `a 9x9 save with a long solution stays under 8 KB`() {
        val side = 9
        val solved = IntArray(side * side) { it }
        // Rotating row 0 right once, 1,450 times, nets one cell right (1,450 mod 9 = 1): the original is row 0 one cell left.
        val start = IntArray(side * side) { if (it < side) (it + 1) % side else it }
        val long = SavedMordie(start, 1450, found = true, solution = List(1450) { SavedMove(true, 0, 1) }, provenShortest = false, ruledOut = 10)

        val text = SaveFileCodec.encode(side, solved, 0, long)

        assertTrue(text.length < 8 * 1024, "${text.length} chars")
        assertEquals(long, SaveFileCodec.decode("s", text).mordie)
    }

    private fun SavedMordie.withScreen() = SavedMordie(original, moves, found, solution, solutionScreen = true, provenShortest = provenShortest, ruledOut = ruledOut)

    private fun SavedMordie.copyRuledOut(ruledOut: Int) = SavedMordie(original, moves, found, solution, provenShortest = false, ruledOut = ruledOut)
}
