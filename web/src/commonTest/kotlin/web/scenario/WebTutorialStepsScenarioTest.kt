package web.scenario

import kotlin.test.Test

/** GH-288 (WU0) scenario stubs: each step advances on its shift. The board is `8 1 14 3 / 5 2 7 12 / 11 6 9 10 / 13 16 15 4`; the shifts are top row left 1, column 2 up 1, row 3 right 2, column 4 down 1. The WU that wires the tutorial fills them in. */
class WebTutorialStepsScenarioTest {

    @Test
    fun `step 1 asks for the top row, one tile left`() = webTest {
        // TODO: 1. First launch.
        // TODO: 2. The panel says `Hi there! …` and names the top row.
        // TODO: 3. The top row is the asked line.
    }

    @Test
    fun `step 1 advances on a drag`() = webTest {
        // TODO: 1. Drag the top row one tile left.
        // TODO: 2. The board shows the shift.
        // TODO: 3. The panel moves to step 2.
    }

    @Test
    fun `step 1 advances on an arrow click`() = webTest {
        // TODO: 1. Click `◀` on row 1.
        // TODO: 2. The panel moves to step 2.
    }

    @Test
    fun `step 1 advances on a shift key`() = webTest {
        // TODO: 1. Select the top row (the selection starts there).
        // TODO: 2. Press `ArrowLeft`.
        // TODO: 3. The panel moves to step 2.
    }

    @Test
    fun `step 2 teaches the wrap`() = webTest {
        // TODO: 1. Do step 1.
        // TODO: 2. The panel names tile 14 at the top of column 2.
        // TODO: 3. Click `▲` on column 2.
        // TODO: 4. Tile 14 is at the bottom.
        // TODO: 5. The panel moves to step 3.
    }

    @Test
    fun `step 3 advances on one drag of two tiles`() = webTest {
        // TODO: 1. Do steps 1 and 2.
        // TODO: 2. Drag row 3 two tiles right.
        // TODO: 3. The panel moves to step 4.
    }

    @Test
    fun `step 3 advances on two ▶ clicks`() = webTest {
        // TODO: 1. Do steps 1 and 2.
        // TODO: 2. Click `▶` on row 3.
        // TODO: 3. The panel is still on step 3.
        // TODO: 4. Click `▶` again.
        // TODO: 5. The panel moves to step 4.
    }

    @Test
    fun `step 3 advances on two ◀ clicks too`() = webTest {
        // TODO: 1. Do steps 1 and 2.
        // TODO: 2. Click `◀` on row 3.
        // TODO: 3. The panel is still on step 3.
        // TODO: 4. Click `◀` again (a half turn gives the same row).
        // TODO: 5. The panel moves to step 4.
    }

    @Test
    fun `step 3 says once more after the first tile`() = webTest {
        // TODO: 1. Do steps 1 and 2.
        // TODO: 2. Click `▶` on row 3.
        // TODO: 3. The panel says `Once more! It's the same row, so Moves stays at 3.`
    }

    @Test
    fun `Moves stays 3 through both tiles of step 3`() = webTest {
        // TODO: 1. Do steps 1 and 2.
        // TODO: 2. Click `▶` on row 3 twice.
        // TODO: 3. Moves reads 3.
    }

    @Test
    fun `Moves reads 1, 2, 3, 4 through the four steps`() = webTest {
        // TODO: 1. Do step 1; Moves reads 1.
        // TODO: 2. Do step 2; Moves reads 2.
        // TODO: 3. Do step 3; Moves reads 3.
        // TODO: 4. Do step 4; Moves reads 4. (One scenario: splitting it repeats the same steps.)
    }

    @Test
    fun `step 4 asks for column 4, one tile down, and solves the board`() = webTest {
        // TODO: 1. Do steps 1 to 3.
        // TODO: 2. The panel says `Last one: …`.
        // TODO: 3. Click `▼` on column 4.
        // TODO: 4. The board is solved.
    }

    @Test
    fun `the same four steps work by dragging, by arrows and by keys`() = webTest {
        // TODO: 1. For each of the three inputs, play the four steps.
        // TODO: 2. The board ends solved.
        // TODO: 3. The panel ends on step 5. (A loop over inputs: the other scenarios cover each input one step at a time.)
    }

    @Test
    fun `Mordie searches the tutorial board like any game`() = webTest {
        // TODO: 1. First launch.
        // TODO: 2. Mordie's search is running.
        // TODO: 3. Mordie's line shows under Moves.
    }
}
