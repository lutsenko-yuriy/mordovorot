package web.scenario

import kotlin.test.Test

/** GH-110 scenarios: shifting with the arrows, New, Moves, Mordie and the solved state. Same setup
 *  as [WebStartupScenarioTest]. */
class WebPlayScenarioTest {

    @Test
    fun `an arrow click shifts the line by one, wrapping around`() {
        // WU4
        // TODO: 1. Start a known 4x4.
        // TODO: 2. Click ▶ on row 0: the row wraps one tile right (tiles shown 1-based).
        // TODO: 3. Click ▲ on column 2: the column shifts up, its top tile moves to the bottom.
    }

    @Test
    fun `Moves follows the move rule`() {
        // WU4
        // TODO: 1. Shift row 0 right twice: "Moves: 1".
        // TODO: 2. Shift row 0 left: still "Moves: 1".
        // TODO: 3. Shift row 1: "Moves: 2".
        // TODO: 4. Shift column 0: "Moves: 3".
    }

    @Test
    fun `New opens the size picker, and Cancel leaves the game untouched`() {
        // WU4
        // TODO: 1. Make 2 shifts; note the board.
        // TODO: 2. Press New: the size picker, with Cancel.
        // TODO: 3. Cancel: the same board and Moves.
        // TODO: 4. dialog_cancelled {dialog: size} fired.
    }

    @Test
    fun `New with a size starts a fresh solvable game`() {
        // WU4
        // TODO: 1. Press New, pick 3.
        // TODO: 2. A 3x3 board, "Moves: 0", and the board is solvable.
    }

    @Test
    fun `solving fades the arrows and stops shifts`() {
        // WU4
        // TODO: 1. Start one shift from solved; make the solving shift.
        // TODO: 2. arrowsEnabled is false; Mordie's spot is "Congratulations ✓" (Mordie sat out).
        // TODO: 3. Click an arrow: the board and Moves are unchanged.
        // TODO: 4. screen_congratulations fired once.
    }

    @Test
    fun `Mordie's greeting, progress and Found it! appear in Mordie's spot`() {
        // WU4
        // TODO: 1. Fake progress N=3, not found; shift: the greeting is shown.
        // TODO: 2. At the shift-20 check: "I have already made 3 moves and have not solved it yet!"
        // TODO: 3. Set the fake to found, N=11; at the next 5-shift check:
        //          "Found it! I had to make 11 moves to find the solution! Can you beat it?"
    }

    @Test
    fun `solving before Mordie finds it - Mordie's ending replaces its text`() {
        // WU4
        // TODO: 1. Fake progress N=0, not found; make the solving shift.
        // TODO: 2. Mordie's spot is "You beat me! I was still at 0 moves.", not "Congratulations ✓".
    }
}
