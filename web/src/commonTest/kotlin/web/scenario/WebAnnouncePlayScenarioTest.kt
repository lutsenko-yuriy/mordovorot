package web.scenario

import kotlin.test.Test

/** GH-166 scenarios: what a screen reader hears while playing - shifts, drags and the win. */
class WebAnnouncePlayScenarioTest {

    @Test
    fun `an arrow announces its line and direction, and the move count when a move starts`() = webTest {
        // TODO: start on a board several shifts from solved
        // TODO: shift row 2 right - says `Row 2 right, 1 move`
        // TODO: shift row 2 right again - says `Row 2 right`, no count (same move)
        // TODO: shift column 4 up - says `Column 4 up, 2 moves`
    }

    @Test
    fun `a drag announces once, on release`() = webTest {
        // TODO: press a tile in row 1 and drag two tiles left - nothing said mid-drag
        // TODO: release - says `Row 1 left 2, 1 move`, once
    }

    @Test
    fun `a drag released where it started announces nothing`() = webTest {
        // TODO: drag a tile one tile over and back
        // TODO: release - nothing said
    }

    @Test
    fun `the winning shift announces only Solved in M moves`() = webTest {
        // TODO: start one shift from solved
        // TODO: shift row 1 left - the board is solved
        // TODO: the only new announcement is `Solved in 1 move` (no `Row 1 left`, no Mordie ending)
    }

    @Test
    fun `a solved board announces nothing more`() = webTest {
        // TODO: solve the board
        // TODO: click an arrow and drag a tile - nothing new said
    }

    @Test
    fun `Mordie's lines are never announced`() = webTest {
        // TODO: start a game - Mordie's opening line shows, not said
        // TODO: shift 20+ times while the fake search reports progress, then a found solution - his progress line and `Found it!` show
        // TODO: nothing said equals or contains a Mordie line; every announcement is a shift wording
    }
}
