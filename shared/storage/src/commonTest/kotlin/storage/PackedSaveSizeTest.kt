package storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** GH-237: how big a packed save is. The 1 KB is the ticket's Should, measured in GH-220. */
class PackedSaveSizeTest {

    private val side = 9
    private val solved = IntArray(side * side) { it }

    /** Row 0 one cell left of solved: [steps] steps of "row 0 right once" solve it when steps mod 9 = 1. */
    private fun save(steps: Int): SavedMordie {
        val start = IntArray(side * side) { if (it < side) (it + 1) % side else it }
        return SavedMordie(start, steps, found = true, solution = List(steps) { SavedMove(true, 0, 1) }, solutionScreen = true, provenShortest = false, ruledOut = 10)
    }

    @Test
    fun `a 9x9 save with a 604-move solution, the labels and the screen flag is under 1 KB`() {
        val text = PackedSaveFormat.encode(side, solved, 500, save(604), "letter_pairs")

        assertTrue(text.length < 1024, "${text.length} chars")
        assertEquals(SavedBoard(side, solved, 500, save(604), "letter_pairs"), PackedSaveFormat.decode("s", text))
    }

    @Test
    fun `a 1450-move solution stays under 8 KB and round-trips`() {
        val text = PackedSaveFormat.encode(side, solved, 0, save(1450), null)

        assertTrue(text.length < 8 * 1024, "${text.length} chars")
        assertEquals(save(1450), PackedSaveFormat.decode("s", text).mordie)
    }
}
