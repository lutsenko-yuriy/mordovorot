import kotlin.test.Test

/** GH-62 scenarios: real BoardImpl + ViewModelImpl + FileSaveRepository (temp dir), driven
 *  through the console view or the TUI via FakeTerminal. */
class MoveCounterScenarioTest {

    @Test
    fun `console shows Moves 0 on a fresh game`() {
        // TODO: 1. Start a console game on a known board with --size=4 (no startup prompts)
        // TODO: 2. Verify the first board printout is followed by `Moves: 0`
    }

    @Test
    fun `repeated shifts on the same line count as one move, in either direction`() {
        // TODO: 1. Console: left 1, left 1, left 1, right 1, right 1
        // TODO: 2. Verify the last board printout is followed by `Moves: 1`
    }

    @Test
    fun `switching lines starts a new move, including returning to an earlier line and row vs column`() {
        // TODO: 1. Console: left 1, right 2, right 1 - verify `Moves: 3`
        // TODO: 2. up 1 (column 1, not row 1) - verify `Moves: 4`
    }

    @Test
    fun `a rejected shift does not change the count`() {
        // TODO: 1. Console: left 1, then left 9 (out of range)
        // TODO: 2. Verify the rejection message is shown and the count stays `Moves: 1`
    }

    @Test
    fun `starting a new game resets the count`() {
        // TODO: 1. Console: left 1, up 2 - verify `Moves: 2`
        // TODO: 2. size 3 - verify `Moves: 0`
    }

    @Test
    fun `save then load restores the count, and the next shift starts a new move`() {
        // TODO: 1. Console: left 1, left 1, up 2 - verify `Moves: 2`
        // TODO: 2. save s1
        // TODO: 3. up 2 (continues the current move, count stays 2)
        // TODO: 4. load s1 - verify `Moves: 2`
        // TODO: 5. up 2 (same column as the last pre-save shift) - verify `Moves: 3`
    }

    @Test
    fun `a save written before move counting loads with Moves 0`() {
        // TODO: 1. Write a legacy two-line save file `old` into the temp saves dir
        // TODO: 2. Console: load old - verify `Moves: 0`
    }

    @Test
    fun `TUI shows Moves under the title and updates on clicks, staying under Congratulations once solved`() {
        // TODO: 1. Start the mouse TUI (FakeTerminal) on a board one shift from solved
        // TODO: 2. Verify the row under `Mordovorot` reads `Moves: 0`
        // TODO: 3. Click a non-solving arrow - verify `Moves: 1`
        // TODO: 4. Click its reverse (same row, undoes it) - verify still `Moves: 1`
        // TODO: 5. Click the solving arrow on a different line - verify `Moves: 2` under `Congratulations ✓`
    }

    @Test
    fun `switching input mode keeps the count`() {
        // TODO: 1. Console: left 1, up 1 - `Moves: 2`
        // TODO: 2. `mouse` command switches to the TUI
        // TODO: 3. Verify the TUI frame shows `Moves: 2`
    }

    @Test
    fun `board_solved fires once in console mode with the final count and size`() {
        // TODO: 1. Console game on a board one shift from solved, with RecordingAnalyticsService
        // TODO: 2. One shift on another line, then the solving shift
        // TODO: 3. Verify board_solved recorded exactly once: move_count=2, board_size=4, input_method=console
    }

    @Test
    fun `loading an already-solved save does not fire board_solved`() {
        // TODO: 1. Load a save of a solved board
        // TODO: 2. Verify no board_solved event was recorded
    }
}
