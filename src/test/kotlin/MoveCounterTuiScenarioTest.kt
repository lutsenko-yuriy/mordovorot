import kotlin.test.Test

/** GH-79: GH-62's console move-counter scenarios rewritten for the TUI - real BoardImpl +
 *  ViewModelImpl + FileSaveRepository (temp dir), driven through FakeTerminal, reading the
 *  `Moves:` row under the title. Renamed to `MoveCounterScenarioTest` once the console
 *  original is deleted. */
class MoveCounterTuiScenarioTest {

    @Test
    fun `TUI shows Moves 0 on a fresh game`() {
        // TODO: start a mouse session on a scrambled board
        // TODO: the first frame shows Moves: 0
    }

    @Test
    fun `repeated clicks on the same line count as one move, in either direction`() {
        // TODO: click the row 1 left arrow twice, then the row 1 right arrow
        // TODO: the last frame shows Moves: 1
    }

    @Test
    fun `switching lines starts a new move`() {
        // TODO: shift row 1, then column 1, then row 1 again
        // TODO: the last frame shows Moves: 3
    }

    @Test
    fun `New resets the count`() {
        // TODO: shift once; click [ New ] and pick size 4
        // TODO: the last frame shows Moves: 0
    }

    @Test
    fun `save then load restores the count, and the next shift starts a new move`() {
        // TODO: make 2 moves; Save as "m"
        // TODO: shift the same line once more
        // TODO: Load "m" - the frame shows Moves: 2
        // TODO: shift that same line - the frame shows Moves: 3
    }

    @Test
    fun `a save written before move counting loads with Moves 0`() {
        // TODO: seed a save with no move-count line
        // TODO: Load it - the frame shows Moves: 0
    }

    @Test
    fun `board_solved fires once in the TUI with the final count and size`() {
        // TODO: start one shift from solved; click the row 1 left arrow
        // TODO: exactly one board_solved with move_count=1, board_size=4, input_method=mouse
    }

    @Test
    fun `loading an already-solved save does not fire board_solved`() {
        // TODO: seed a solved save; Load it
        // TODO: no board_solved tracked
    }
}
