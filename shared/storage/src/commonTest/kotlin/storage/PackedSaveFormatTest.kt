package storage

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** GH-237: the packed save format (`~` + base64url of a bit stream), ADR-0004. */
@OptIn(ExperimentalEncodingApi::class)
class PackedSaveFormatTest {

    private val names = listOf("numbers", "numbers_from_0", "pairs", "pairs_from_0", "letters", "letter_pairs")

    // --- goldens, from an independent encoder of the layout (the layout is frozen once saves exist) ---

    @Test
    fun `a 2x2 with nothing else is three zero bytes after the marker`() {
        assertEquals("~IAAA", PackedSaveFormat.encode(2, intArrayOf(0, 1, 2, 3), 0, null, null))
        assertEquals(SavedBoard(2, intArrayOf(0, 1, 2, 3), 0), PackedSaveFormat.decode("g", "~IAAA"))
    }

    @Test
    fun `golden 3x3 with moves and no Mordie`() {
        val state = IntArray(9) { it }

        assertEquals("~MAAAAAA", PackedSaveFormat.encode(3, state, 0, null, null))
    }

    @Test
    fun `golden 3x3 found unproven with a solution, the solution screen and pairs`() {
        val original = intArrayOf(2, 0, 1, 3, 4, 5, 6, 7, 8)
        val steps = listOf(SavedMove(true, 0, 1), SavedMove(true, 0, 1))
        val mordie = SavedMordie(original, 2, found = true, solution = steps, solutionScreen = true, provenShortest = false, ruledOut = 0)

        val text = PackedSaveFormat.encode(3, original, 5, mordie, "pairs")

        assertEquals("~N-CkAAAgAAAQAAA", text)
        assertEquals(SavedBoard(3, original, 5, mordie, "pairs"), PackedSaveFormat.decode("g", text))
    }

    @Test
    fun `golden 4x4 searching with a label this build does not know`() {
        val original = intArrayOf(1, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
        val mordie = SavedMordie(original, 6, found = false)

        val text = PackedSaveFormat.encode(4, original, 3, mordie, "hexes")

        assertEquals("~ToCtDK8MrmBiAAAAAAABAAAAAAAAMA", text)
        assertEquals(SavedBoard(4, original, 3, mordie, "hexes"), PackedSaveFormat.decode("g", text))
    }

    // --- every size and every shape round-trips ---

    private fun movesFromSolved(side: Int, random: Random): Pair<IntArray, List<SavedMove>> {
        val tiles = IntArray(side * side) { it }
        val line = IntArray(side)
        val done = List(1 + random.nextInt(40)) { SavedMove(random.nextBoolean(), random.nextInt(side), 1 + random.nextInt(side - 1)) }
        for (move in done) {
            val at = { j: Int -> if (move.isRow) move.index * side + j else j * side + move.index }
            for (j in 0 until side) line[(j + move.amount) % side] = tiles[at(j)]
            for (j in 0 until side) tiles[at(j)] = line[j]
        }
        return tiles to done.reversed().map { SavedMove(it.isRow, it.index, side - it.amount) }
    }

    private fun shapes(side: Int, random: Random): List<Pair<String, SavedMordie?>> {
        val (original, solution) = movesFromSolved(side, random)
        val n = solution.size
        val other = IntArray(side * side) { it }.also { it.shuffle(random) }
        return listOf(
            "no Mordie" to null,
            "searching" to SavedMordie(other, 7, found = false),
            "found proven, no solution" to SavedMordie(other, 12, found = true),
            "found proven, solution" to SavedMordie(original, n, found = true, solution = solution),
            "found unproven, no solution" to SavedMordie(other, 12, found = true, provenShortest = false, ruledOut = 10),
            "found unproven, solution" to SavedMordie(original, n.coerceAtLeast(2), found = true, solution = solution.takeIf { n >= 2 }, provenShortest = false, ruledOut = (n - 2).coerceAtLeast(0)),
            "screen, proven" to SavedMordie(original, n, found = true, solution = solution, solutionScreen = true),
            "screen, unproven" to SavedMordie(original, n.coerceAtLeast(2), found = true, solution = solution.takeIf { n >= 2 }, solutionScreen = n >= 2, provenShortest = false, ruledOut = 0),
        )
    }

    @Test
    fun `every size and shape round-trips`() {
        val random = Random(237)
        for (side in 2..9) {
            val current = IntArray(side * side) { it }.also { it.shuffle(random) }
            for ((label, mordie) in shapes(side, random)) {
                for (labels in listOf(null) + names + "hexes") {
                    val moves = random.nextInt(100_000)
                    val text = PackedSaveFormat.encode(side, current, moves, mordie, labels)
                    assertEquals(SavedBoard(side, current, moves, mordie, labels), PackedSaveFormat.decode("s", text), "side $side, $label, $labels")
                }
            }
        }
    }

    @Test
    fun `an empty solution is written as none and reads back as none`() {
        val board = intArrayOf(0, 1, 2, 3)
        val mordie = SavedMordie(board, 0, found = true, solution = emptyList())

        val saved = PackedSaveFormat.decode("e", PackedSaveFormat.encode(2, board, 0, mordie, null))

        assertNull(saved.mordie!!.solution)
        assertEquals(SavedMordie(board, 0, found = true), saved.mordie)
    }

    // --- decode rejects what is not a valid packed save ---

    private fun packed(build: BitWriter.() -> Unit) = "~" + Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(BitWriter().apply(build).toByteArray())

    private fun BitWriter.header(side: Int = 2, labels: Int = 0, status: Int = 0, screen: Int = 0, solution: Int = 0) {
        put(side, 4); put(labels, 3); put(status, 2); put(screen, 1); put(solution, 1)
    }

    private fun assertCorrupted(reason: String, text: String) {
        val e = assertFailsWith<SaveFileFormatException> { PackedSaveFormat.decode("s", text) }
        assertEquals("Save 's' is corrupted: $reason", e.message)
    }

    private val board2 = intArrayOf(0, 1, 2, 3)

    @Test
    fun `characters outside base64url, or padding, are rejected`() {
        assertCorrupted("the packed data is not valid base64url", "~IA+A")
        assertCorrupted("the packed data is not valid base64url", "~IAAA=")
        assertCorrupted("the packed data is not valid base64url", "~I")
    }

    @Test
    fun `data that ends early is rejected`() {
        assertCorrupted("the packed data ends early", "~IA")
        assertCorrupted("the packed data ends early", packed { header(side = 3); putVarint(0) })
    }

    @Test
    fun `extra data and non-zero padding are rejected`() {
        assertCorrupted("the packed data has extra bytes at the end", packed { header(); putVarint(0); putPermutation(board2); put(0, 8) })
        // A 4x4 board ends mid-byte (68 bits), so a set bit after it is in the padding, not in an extra byte.
        assertCorrupted("the packed data's padding bits are not zero", packed { header(side = 4); putVarint(0); putPermutation(IntArray(16) { it }); put(1, 2) })
    }

    @Test
    fun `a side outside 2 to 15 is rejected`() {
        assertCorrupted("square side must be 2-15, not 1", packed { header(side = 1) })
        assertCorrupted("square side must be 2-15, not 0", packed { header(side = 0) })
    }

    @Test
    fun `a Lehmer digit out of range is rejected for the board and for Mordie's original`() {
        // A 3x3's first digit has 4 bits and range 0-8, so 9 is out of range; a 2x2's second has 2 bits and range 0-2, so 3 is.
        assertCorrupted("the board is not a valid arrangement", packed { header(side = 3); putVarint(0); put(9, 4) })
        assertCorrupted(
            "Mordie's original is not a valid arrangement",
            packed { header(status = 1); putVarint(0); putPermutation(board2); put(0, 2); put(3, 2) },
        )
    }

    @Test
    fun `a number that is too big is rejected`() {
        assertCorrupted("a number in the packed data is too big", packed { header(); put(0xFF, 8); put(0xFF, 8); put(0xFF, 8); put(0xFF, 8); put(0x0F, 8) })
    }

    @Test
    fun `flags that do not fit the status are rejected`() {
        assertCorrupted("Mordie's flags without Mordie data", packed { header(screen = 1) })
        assertCorrupted("Mordie's flags without Mordie data", packed { header(solution = 1) })
        assertCorrupted("the solution-screen flag needs Mordie's solution", packed { header(status = 2, screen = 1) })
        assertCorrupted("Mordie's solution on a save where it is still searching", packed { header(status = 1, solution = 1); putVarint(0); putPermutation(board2); putPermutation(board2); putVarint(1); put(0, 2) })
    }

    @Test
    fun `an unproven R out of range is rejected`() {
        assertCorrupted(
            "Mordie's ruled out length must be 0 to 4",
            packed { header(status = 3); putVarint(0); putPermutation(board2); putPermutation(board2); putVarint(6); putVarint(5) },
        )
    }

    @Test
    fun `a solution step out of range is rejected`() {
        // A 3x3 has 2*3*2 = 12 joint codes in 4 bits, so 15 is out of range.
        val three = IntArray(9) { it }
        assertCorrupted(
            "a solution step is out of range",
            packed { header(side = 3, status = 2, solution = 1); putVarint(0); putPermutation(three); putPermutation(three); putVarint(1); put(15, 4) },
        )
    }

    @Test
    fun `a solution that does not solve the original is rejected`() {
        assertCorrupted(
            "Mordie's solution doesn't solve the original arrangement",
            packed { header(status = 2, solution = 1); putVarint(0); putPermutation(board2); putPermutation(board2); putVarint(1); put(0, 2) },
        )
    }

    @Test
    fun `an unknown label that is not valid UTF-8 or not one word is rejected`() {
        assertCorrupted("the label is not valid UTF-8", packed { header(labels = 7); putVarint(1); put(0xFF, 8); putVarint(0); putPermutation(board2) })
        assertCorrupted("the label must be one word", packed { header(labels = 7); putVarint(3); "a b".encodeToByteArray().forEach { put(it.toInt() and 0xFF, 8) }; putVarint(0); putPermutation(board2) })
        assertCorrupted("the label must be one word", packed { header(labels = 7); putVarint(0); putVarint(0); putPermutation(board2) })
    }
}
