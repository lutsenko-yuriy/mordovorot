package storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** GH-273: a solution-screen save keeps how the player's game ended, in the move-count slot. */
class PackedSavePlayerResultTest {

    private val original = intArrayOf(2, 0, 1, 3, 4, 5, 6, 7, 8)
    /** Made by the encoder before GH-273 (3x3, pairs, found unproven, a solution-screen save with move count 0). */
    private val oldSolutionScreenSave = "~N-AEAAAgAAAQAAA"
    private val steps = listOf(SavedMove(true, 0, 1), SavedMove(true, 0, 1))

    private fun screen(result: SavedPlayerResult?) =
        SavedMordie(original, 2, found = true, solution = steps, solutionScreen = true, playerResult = result)

    private fun roundTrip(mordie: SavedMordie, moveCount: Int = 0): SavedBoard =
        PackedSaveFormat.decode("t", PackedSaveFormat.encode(3, original, moveCount, mordie, null))

    @Test
    fun `a gave-up result round-trips`() {
        val loaded = roundTrip(screen(SavedPlayerResult(gaveUp = true, moves = 14)))

        assertEquals(SavedPlayerResult(gaveUp = true, moves = 14), loaded.mordie?.playerResult)
        assertEquals(0, loaded.moveCount)
    }

    @Test
    fun `a solved result round-trips`() {
        assertEquals(SavedPlayerResult(gaveUp = false, moves = 14), roundTrip(screen(SavedPlayerResult(false, 14))).mordie?.playerResult)
    }

    @Test
    fun `0 and 1 moves round-trip for both results`() {
        for (gaveUp in listOf(true, false)) for (moves in listOf(0, 1, 127, 128, 100_000)) {
            val result = SavedPlayerResult(gaveUp, moves)
            assertEquals(result, roundTrip(screen(result)).mordie?.playerResult, "$result")
        }
    }

    @Test
    fun `a solution-screen save with no result decodes with none`() {
        val loaded = roundTrip(screen(null))

        assertNull(loaded.mordie?.playerResult)
        assertEquals(0, loaded.moveCount)
    }

    @Test
    fun `a solution-screen save from before this ticket decodes with no result`() {
        val loaded = PackedSaveFormat.decode("old", oldSolutionScreenSave)

        assertNull(loaded.mordie?.playerResult)
        assertEquals(true, loaded.mordie?.solutionScreen)
    }

    @Test
    fun `a save off the solution screen keeps its move count and has no result`() {
        val mordie = SavedMordie(original, 2, found = true, solution = steps)
        val loaded = roundTrip(mordie, moveCount = 5)

        assertEquals(5, loaded.moveCount)
        assertNull(loaded.mordie?.playerResult)
    }
}
