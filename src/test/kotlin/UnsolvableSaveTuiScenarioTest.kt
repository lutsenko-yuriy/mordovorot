import kotlin.test.Test

/** GH-79: GH-74's console unsolvable-save scenarios rewritten for the TUI - real BoardImpl +
 *  ViewModelImpl + FileSaveRepository (temp dir), driven through FakeTerminal. Same seeds:
 *  `odd.save` 3x3 `2 1 0 3 4 5 6 7 8` (unsolvable, 5 moves), `even.save` 4x4. Renamed to
 *  `UnsolvableSaveScenarioTest` once the console original is deleted. */
class UnsolvableSaveTuiScenarioTest {

    @Test
    fun `an unsolvable save is marked in the startup restore dialog`() {
        // TODO: seed odd.save; start a session without --size
        // TODO: the restore dialog lists "odd (3x3) unsolvable"
    }

    @Test
    fun `a 4x4 save is never marked unsolvable`() {
        // TODO: seed even.save; start a session without --size
        // TODO: the restore dialog lists "even (4x4)" with no "unsolvable"
    }

    @Test
    fun `restoring an unsolvable save at startup opens the Can't-be-solved dialog`() {
        // TODO: seed odd.save; confirm it in the restore dialog
        // TODO: the Can't be solved dialog shows [ Repair & load ] [ New game ] [ Exit ]
    }

    @Test
    fun `Repair & load at startup loads the fixed board and rewrites the save`() {
        // TODO: seed odd.save; confirm restore; choose Repair & load
        // TODO: the board is 1 2 0 3 4 5 6 7 8 and the frame shows Moves: 5
        // TODO: odd.save now holds the repaired state with 5 moves
    }

    @Test
    fun `New game at startup opens the size picker`() {
        // TODO: seed odd.save; confirm restore; choose New game
        // TODO: the size-picker dialog is shown
    }

    @Test
    fun `Exit at startup quits without asking to save`() {
        // TODO: seed odd.save; confirm restore; choose Exit
        // TODO: the session ends and no Save before quitting dialog was shown
    }

    @Test
    fun `loading an unsolvable save mid-game and choosing New game opens the size picker`() {
        // TODO: start with --size semantics; seed odd.save; Load it from the toolbar
        // TODO: choose New game - the size-picker dialog is shown
    }

    @Test
    fun `exiting from a mid-game unsolvable load runs the Save-before-quitting dialog`() {
        // TODO: start with --size semantics; seed odd.save; Load it from the toolbar
        // TODO: choose Exit - the Save before quitting dialog (Yes/No) is shown
    }
}
