package web

import kotlin.test.Test
import kotlin.test.assertEquals
import viewmodel.GiveUpButton
import viewmodel.SolutionState

/** GH-164: where focus goes when the control that had it is gone or disabled after a redraw. */
class PageFocusTest {
    private val playing = WebState(side = 3, tiles = (1..9).toList(), moves = 4, mordie = null, solved = false, status = null, dialog = null)
    private val won = playing.copy(solved = true)
    private fun replay(atStart: Boolean, atEnd: Boolean) =
        playing.copy(solution = SolutionState(listOf("01. Row 1 ← ×1", "02. Col 2 ↑ ×1"), current = null, atStart = atStart, atEnd = atEnd))

    @Test
    fun `a winning arrow passes focus to Mordie's solution when it is shown`() {
        val key = Focus.ArrowButton(Arrow.UP, 0).key
        assertEquals(Focus.GiveUpRow, fallbackFocus(key, won.copy(giveUp = GiveUpButton.MORDIES_SOLUTION)))
    }

    @Test
    fun `a winning arrow passes focus to New when Mordie's solution isn't shown`() {
        val key = Focus.ArrowButton(Arrow.LEFT, 2).key
        assertEquals(Focus.NewButton, fallbackFocus(key, won.copy(giveUp = GiveUpButton.HIDDEN)))
    }

    @Test
    fun `Next at the last step passes focus to Prev`() {
        assertEquals(Focus.ReplayPrev, fallbackFocus(Focus.ReplayNext.key, replay(atStart = false, atEnd = true)))
    }

    @Test
    fun `Prev at the first step and Restart pass focus to Next`() {
        val atStart = replay(atStart = true, atEnd = false)
        assertEquals(Focus.ReplayNext, fallbackFocus(Focus.ReplayPrev.key, atStart))
        assertEquals(Focus.ReplayNext, fallbackFocus(Focus.ReplayRestart.key, atStart))
    }

    @Test
    fun `the Give Up row passes focus to Next once the solution screen opens`() {
        assertEquals(Focus.ReplayNext, fallbackFocus(Focus.GiveUpRow.key, replay(atStart = true, atEnd = false)))
    }

    @Test
    fun `anything else goes to the start of the page`() {
        assertEquals(Focus.PageStart, fallbackFocus("nothing-like-this", playing))
        assertEquals(Focus.PageStart, fallbackFocus(Focus.GiveUpRow.key, playing))
        assertEquals(Focus.PageStart, fallbackFocus(Focus.SaveButton.key, playing))
    }
}
