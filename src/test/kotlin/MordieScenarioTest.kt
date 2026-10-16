import kotlin.test.Test

/** GH-83 scenarios: Mordie, the computer opponent racing the player to the shortest solution.
 *  Real BoardImpl + ViewModelImpl + FileSaveRepository (temp dir), driven through a mouse-mode
 *  GameSession (sizeChosenAtLaunch) over an 80x40 FakeTerminal, like [MoveCounterScenarioTest],
 *  with `Mordie(FakeMordieSearchFactory())` scripting the search's progress - no real search runs.
 *  Helpers read the `Mordie:` row(s) under `Moves:` and the title row, ANSI escapes stripped.
 *  Each test names the WU that makes it green. */
class MordieScenarioTest {

    @Test
    fun `new game greets for shifts 0-4, then the line empties`() {
        // WU4
        // TODO: 1. Start a scrambled 4x4; fake progress N=3, not found.
        // TODO: 2. Frame 0 shows "Mordie: Alright! Let's sort this one out!" under "Moves: 0".
        // TODO: 3. After shifts 1-4 the greeting is still shown.
        // TODO: 4. After shift 5 the Mordie row is blank.
    }

    @Test
    fun `progress shows at shifts 20-24, empties at 25, and gets the hard prefix from 60`() {
        // WU4
        // TODO: 1. Fake progress N=6 up to shift 19, N=7 right after the shift-20 check.
        // TODO: 2. Shift 19: the Mordie row is blank.
        // TODO: 3. Shift 20: "Mordie: I have already made 6 moves and have not solved it yet!" (the value at the check).
        // TODO: 4. Shift 24: same text.
        // TODO: 5. Shift 25: blank.
        // TODO: 6. Set N=9 before shift 60; shift 60: "Mordie: This one is hard! I have already made 9 moves and have not solved it yet!"
    }

    @Test
    fun `found is revealed at the next 5-shift check and stays until solved`() {
        // WU3 (analytics) + WU4 (line)
        // TODO: 1. Make 7 shifts; then set the fake to found, N=11.
        // TODO: 2. Shifts 8 and 9: the Mordie row is still blank.
        // TODO: 3. Shift 10: "Mordie: Found it! I had to make 11 moves to find the solution! Can you beat it?"
        // TODO: 4. Shifts 11-30: the same line on every frame.
        // TODO: 5. mordie_solution_found fired once: {board_size: 4, solution_length: 11, player_shift_count: 10,
        //          resumed_from_save: false, input_method: mouse}.
    }

    @Test
    fun `solving before Mordie finds it - You beat me`() {
        // WU3 (analytics) + WU4 (title)
        // TODO: 1. Start one shift from solved; fake progress N=0, not found.
        // TODO: 2. Make the solving shift.
        // TODO: 3. Title is "You beat me! I was still at 0 moves."; the Mordie row is blank.
        // TODO: 4. board_solved has mordie_result: player_won, mordie_moves: 0.
    }

    @Test
    fun `matching Mordie - Cool, you matched me`() {
        // WU3 (analytics) + WU4 (title)
        // TODO: 1. Start one shift from solved; fake found, N=1.
        // TODO: 2. Solve in 1 move.
        // TODO: 3. Title is "Cool, you matched me! 1 move each!" (singular).
        // TODO: 4. board_solved has mordie_result: matched, mordie_moves: 1.
    }

    @Test
    fun `more moves than Mordie - Sorry, but I win`() {
        // WU3 (analytics) + WU4 (title)
        // TODO: 1. Start one shift from solved; fake found, N=1.
        // TODO: 2. Reach solved in 3 moves (e.g. a column shift and back, then the solving row shift).
        // TODO: 3. Title is "Sorry, but I win: 1 move vs your 3."
        // TODO: 4. board_solved has mordie_result: mordie_won, mordie_moves: 1.
    }

    @Test
    fun `fewer moves than Mordie's N - Are you a machine`() {
        // WU3 (analytics) + WU4 (title)
        // TODO: 1. Hand-write a save one shift from solved: move count 0, line 4 = that arrangement, line 5 = "5 found".
        // TODO: 2. Load it; solve in 1 move.
        // TODO: 3. Title is "I made 5 moves and you made 1 move! Are you a machine?"
        // TODO: 4. board_solved has mordie_result: machine, mordie_moves: 5.
    }

    @Test
    fun `save and load while searching resumes at the saved N`() {
        // WU3 (save/resume) + WU4 (line)
        // TODO: 1. Make 7 shifts; fake progress N=8, not found.
        // TODO: 2. Save as "m"; m.save has line 4 = the original arrangement, line 5 = "8 searching".
        // TODO: 3. Make one more shift, then load "m".
        // TODO: 4. The factory was started again with the original arrangement and ruledOut = 8.
        // TODO: 5. Line is "Mordie: Okay, so we stopped when I was at 8 moves! What was I doing?.."
        // TODO: 6. The shift count restarted: after 5 more shifts the Mordie row is blank.
    }

    @Test
    fun `loading a found save shows Found it immediately and starts no search`() {
        // WU3 + WU4
        // TODO: 1. Hand-write an unsolved save with line 4 = its original, line 5 = "11 found".
        // TODO: 2. Load it; the first frame after the load shows "Mordie: Found it! I had to make 11 moves to find the solution! Can you beat it?"
        // TODO: 3. The factory started no search for it.
        // TODO: 4. After 5 shifts, no mordie_solution_found has fired.
    }

    @Test
    fun `a pre-Mordie save starts Mordie fresh from the loaded board and keeps the move count`() {
        // WU3 + WU4
        // TODO: 1. Hand-write a 3-line save (side, tiles, move count 4).
        // TODO: 2. Load it; the frame shows "Moves: 4" and "Mordie: Alright! Let's sort this one out!"
        // TODO: 3. The factory was started with the loaded arrangement and ruledOut = 0.
    }

    @Test
    fun `a repaired unsolvable save starts Mordie fresh from the repaired board`() {
        // WU3
        // TODO: 1. Hand-write an unsolvable 3x3 save (with Mordie lines 4-5) and a nonzero move count.
        // TODO: 2. Load it; choose Repair in the "Can't be solved" dialog.
        // TODO: 3. The factory was started with the repaired arrangement and ruledOut = 0.
        // TODO: 4. The greeting is the fresh one; the move count is kept.
    }

    @Test
    fun `a solved save with Mordie data shows its ending, without data Congratulations`() {
        // WU3 + WU4
        // TODO: 1. Load a solved save with move count 3, line 4 = its original, line 5 = "3 found";
        //          title is "Cool, you matched me! 3 moves each!"
        // TODO: 2. Load a solved save with no lines 4-5; title is "Congratulations ✓".
        // TODO: 3. Neither load fires board_solved.
    }

    @Test
    fun `switching input mode keeps the shift count and line text`() {
        // WU4
        // TODO: 1. Make 3 shifts, then click [ Keyboard ].
        // TODO: 2. The first keyboard-mode frame shows the same greeting.
        // TODO: 3. After 2 keyboard shifts (shift 5 overall) the Mordie row is blank.
        // TODO: 4. The factory was started only once.
    }

    @Test
    fun `New cancels the running search and Exit stops it`() {
        // WU3
        // TODO: 1. Click [ New ], pick 4x4: the first fake search was cancelled and a second started with the new arrangement.
        // TODO: 2. Exit, answering No to saving: the second search was cancelled.
    }
}
