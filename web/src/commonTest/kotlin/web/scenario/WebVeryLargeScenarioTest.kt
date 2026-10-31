package web.scenario

import kotlin.test.Test

/** GH-207 scenarios: very large boards (7x7 to 9x9) in the web client, through [WebHarness]. */
class WebVeryLargeScenarioTest {

    @Test
    fun `New offers a Very large section and starts 7x7, 8x8 and 9x9 boards`() = webTest {
        // TODO: 1. Start the page on 4x4, then press New.
        // TODO: 2. The dialog is WebDialog.Size with 3-6, veryLarge 7-9, current 4, the title "Very large"
        //          and the line "Harder to solve, and the tiles may be too small to use comfortably."
        // TODO: 3. Choose 7, then 8, then 9, each time through New.
        // TODO: 4. The side and the tile count are right (49, 64 and 81 tiles, labelled 1..N²).
        // TODO: 5. new_game_size_selected carries size 7, 8 and 9 with trigger toolbar.
    }

    @Test
    fun `the startup picker shows the Very large section too`() = webTest {
        // TODO: 1. Start the page with no saves.
        // TODO: 2. The size picker is open with the Very large section.
        // TODO: 3. Cancel keeps the board already shown, and dialog_cancelled fires for size.
    }

    @Test
    fun `the Very large section follows the unsolvable-save New game too`() = webTest {
        // TODO: 1. Seed an unsolvable save.
        // TODO: 2. Load it, then choose New game.
        // TODO: 3. The size picker has the Very large section, with opened_from unsolvable_save.
    }

    @Test
    fun `Mordie sits 9x9 out, and a win is Congratulations`() = webTest {
        // TODO: 1. Start a 9x9 game.
        // TODO: 2. Mordie's line is "9x9 is too much for me, you're on your own!"
        // TODO: 3. No Mordie search starts for it, and Give Up is hidden.
        // TODO: 4. After 5 shifts the line is empty.
        // TODO: 5. A 9x9 board one shift from solved is won with one shift.
        // TODO: 6. The title is "Congratulations ✓", there is no [ Mordie's solution ], and board_solved has
        //          board_size 9 with no mordie_result or mordie_moves.
    }

    @Test
    fun `Letters shows Numbers on 7x7, and is unavailable in the Tile labels dialog`() = webTest {
        // TODO: 1. Set a Letters preference on a 4x4 board.
        // TODO: 2. Start a 7x7 game, which shows Numbers.
        // TODO: 3. In the Tile labels dialog the Letters row is unavailable with "up to 5x5",
        //          and Pairs and Letter pairs are available.
        // TODO: 4. The preference is still Letters after OK.
        // TODO: 5. A reloaded page on a smaller board shows Letters again.
    }

    @Test
    fun `7x7 and 9x9 new games can always be solved, 8x8 never needs a fix`() = webTest {
        // TODO: 1. Start many 7x7, 8x8 and 9x9 games.
        // TODO: 2. Each shuffled arrangement can be solved (checked with Solvability).
    }

    @Test
    fun `an 8x8 game saves, loads and is listed with its size`() = webTest {
        // TODO: 1. Start 8x8 and make a shift.
        // TODO: 2. Save as "big".
        // TODO: 3. Load from a 4x4 board.
        // TODO: 4. The list shows "big (8x8)", and loading resizes the board to 8x8 with its tiles and move count.
    }

    @Test
    fun `an unsolvable 7x7 save is marked and can be repaired`() = webTest {
        // TODO: 1. Seed an unsolvable 7x7 save.
        // TODO: 2. The Load list shows "foo (7x7) unsolvable".
        // TODO: 3. Loading it opens "Can't be solved".
        // TODO: 4. Repair & load swaps two tiles, the board loads solvable, and unsolvable_save_resolved
        //          carries board_size 7.
    }

    @Test
    fun `arrows, drag and keyboard shifts work on a 9x9 board`() = webTest {
        // TODO: 1. Start 9x9.
        // TODO: 2. An arrow shifts a row once.
        // TODO: 3. A drag of 3 tiles is 3 shifts and one move.
        // TODO: 4. In keyboard mode, a selection moves and wraps at the 9th row and column,
        //          then a row shift works.
    }

    @Test
    fun `the size picker opens focused on the current big size`() = webTest {
        // TODO: 1. Start a 9x9 game and press New.
        // TODO: 2. Focus is on the 9x9 button (Focus.SizeChoice(9)).
        // TODO: 3. Tab and Shift+Tab cycle through all the size buttons and Cancel only.
        // TODO: 4. Escape cancels, as Cancel does.
    }
}
