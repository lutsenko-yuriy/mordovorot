import kotlin.test.Test

/** GH-74 scenarios: unsolvable saves on odd board sizes - the `unsolvable` mark in listings and
 *  the repair / new game / exit choice on load. Real BoardImpl + ViewModelImpl +
 *  FileSaveRepository (temp dir), driven through the console view like [SolvedConsoleScenarioTest].
 *
 *  Seeds:
 *  - `odd.save`: 3x3, state `2 1 0 3 4 5 6 7 8` (tiles 0/2 swapped - odd, unsolvable), 5 moves.
 *    Repair swaps positions 0/1, giving `1 2 0 3 4 5 6 7 8` (solvable, not solved).
 *  - `even.save`: 4x4, state `1 0 2 3 ... 15` (every 4x4 board is solvable). */
class UnsolvableSaveScenarioTest {

    @Test
    fun `an unsolvable save is marked in the startup restore prompt`() {
        // TODO: 1. Seed odd.save alone; start with no --size (sizeChosenAtLaunch = false).
        // TODO: 2. Output contains "Restore save 'odd (3x3) unsolvable'? [y/N] ".
    }

    @Test
    fun `a 4x4 save is never marked unsolvable`() {
        // TODO: 1. Seed even.save alone; start with no --size.
        // TODO: 2. Output contains "Restore save 'even (4x4)'? [y/N] " and no "unsolvable" anywhere.
    }

    @Test
    fun `restoring an unsolvable save at startup offers repair, new game or exit`() {
        // TODO: 1. Seed odd.save; input "y".
        // TODO: 2. Output contains "'odd (3x3)' can't be solved - no sequence of shifts reaches the solved board."
        //          followed by "Repair and load (r), start a new game (n), or exit (e)? ".
    }

    @Test
    fun `repair at startup loads the fixed board and rewrites the save`() {
        // TODO: 1. Seed odd.save; input "y", "r".
        // TODO: 2. Output contains "Repaired 'odd' (two tiles swapped) and loaded it."
        // TODO: 3. Board is 3x3, solvable, and the output shows "Moves: 5".
        // TODO: 4. odd.save on disk now holds the repaired state (1 2 0 3 4 5 6 7 8) with 5 moves.
        // TODO: 5. Events: load_command_used {result: unsolvable, trigger: startup_prompt},
        //          unsolvable_save_resolved {choice: repair, trigger: startup_prompt, board_size: 3, repair_saved: true},
        //          startup_restore_decision {decision: restored}.
    }

    @Test
    fun `new game at startup shows the board-size prompt`() {
        // TODO: 1. Seed odd.save; input "y", "n", "5".
        // TODO: 2. After the r/n/e prompt, output shows "Board size? [3-5, Enter for 4]: ".
        // TODO: 3. Board is 5x5 and solvable.
        // TODO: 4. odd.save on disk is unchanged.
        // TODO: 5. Events: unsolvable_save_resolved {choice: new_game}, startup_restore_decision {decision: new_game}.
    }

    @Test
    fun `exit at startup quits without asking to save`() {
        // TODO: 1. Seed odd.save; input "y", "e".
        // TODO: 2. Output ends right after the r/n/e prompt - no "Save before quitting?".
        // TODO: 3. Events: unsolvable_save_resolved {choice: exit}, startup_restore_decision {decision: quit}.
    }

    @Test
    fun `loading an unsolvable save mid-game and choosing new game asks for the size`() {
        // TODO: 1. Seed odd.save; start with the size chosen at launch (4x4 game).
        // TODO: 2. Input "load odd", "n", "3".
        // TODO: 3. Output shows the can't-be-solved message, the r/n/e prompt, then "Board size? [3-5, Enter for 4]: ".
        // TODO: 4. Board is 3x3 and solvable.
        // TODO: 5. Events: new_game_size_selected {trigger: unsolvable_save}.
    }

    @Test
    fun `exiting from a mid-game unsolvable load runs the normal exit flow`() {
        // TODO: 1. Seed odd.save; start with the size chosen at launch; input "load odd", "e", "n".
        // TODO: 2. Output shows "Save before quitting? [y/N] " and the session ends.
        // TODO: 3. Events: exit_command_used {save_choice: declined},
        //          unsolvable_save_resolved {choice: exit, trigger: command}.
    }

    @Test
    fun `an invalid answer re-prompts for repair, new game or exit`() {
        // TODO: 1. Seed odd.save; start with the size chosen at launch; input "load odd", "x", "", "R".
        // TODO: 2. The r/n/e prompt appears 3 times.
        // TODO: 3. The repaired board is loaded (capital R counts as repair).
    }

    @Test
    fun `load accepts the listed form with the unsolvable mark`() {
        // TODO: 1. Seed odd.save; start with the size chosen at launch; input "load odd (3x3) unsolvable", "r".
        // TODO: 2. The r/n/e prompt appears and the repair loads odd - no "No save named" message.
    }
}
