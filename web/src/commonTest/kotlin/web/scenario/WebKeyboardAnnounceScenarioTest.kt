package web.scenario

import kotlin.test.Test

/** GH-171: what a screen reader hears in keyboard mode. The words may change after the VoiceOver test
 *  in WU3 (`role="grid"`, see `docs/knowledge/notes/GH-171.md`). Stubs until WU4 makes them green. */
class WebKeyboardAnnounceScenarioTest {

    @Test
    fun `moving the selection says the tile, where it is and in place when home`() {
        // TODO: move onto an out-of-place tile - `delta, row 2, column 4`
        // TODO: move onto a home tile - adds `, in place`
    }

    @Test
    fun `a shift in keyboard mode, by key or drag, adds the tile now under the selection`() {
        // TODO: press H - `Row 2 left, 5 moves. Now charlie`
        // TODO: drag a line - same form
    }

    @Test
    fun `the board taking focus says Board and where the selection is, or the current step`() {
        // TODO: in play - `Board, delta, row 2, column 4`
        // TODO: solution - `Board, step 3 of 12: Column 3 up 2`
        // TODO: at the start - `Board, start, step 0 of 12`
    }

    @Test
    fun `a frame's own words and the board's focus words are joined`() {
        // TODO: press 1 from the board, choose 4x4 - `New 4x4 game. Board, alpha, row 1, column 1`
    }

    @Test
    fun `0 reads the board in keyboard mode only, and not in a dialog`() {
        // TODO: press 0 - the read-out is said
        // TODO: in a dialog - nothing
        // TODO: in mouse mode - nothing
    }
}
