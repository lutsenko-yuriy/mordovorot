import kotlin.test.Test

/** GH-126 scenarios: giving up and Mordie's solution screen in the terminal. Real BoardImpl +
 *  FileSaveRepository (temp dir) through a GameSession over a FakeTerminal, with
 *  `Mordie(FakeMordieSearchFactory())` scripting the search and its solution, like
 *  [MordieScenarioTest]. The replay advances on `TerminalEvent.Tick`s. Each test names the WU
 *  that makes it green. */
class GiveUpScenarioTest {

    @Test
    fun `Give Up is dimmed and inert until Mordie finds its solution`() {
        // WU3
        // TODO: 1. Mouse mode, Mordie still searching: `[ Give Up ]` is dimmed, on its own row above the toolbar.
        // TODO: 2. Click it: no dialog, no `screen_give_up_dialog`.
        // TODO: 3. Script the search to find its solution, then shift: `Found it!` shows and `[ Give Up ]` is no longer dimmed.
    }

    @Test
    fun `No keeps the game and Yes opens the solution screen`() {
        // WU3
        // TODO: 1. After the reveal, make 2 moves, click `[ Give Up ]`: the `Give up and see Mordie's solution?` dialog opens; `screen_give_up_dialog` {board_size 4, move_count 2}.
        // TODO: 2. Click No: board and `Moves: 2` unchanged; `dialog_cancelled` {dialog: give_up}.
        // TODO: 3. Click `[ Give Up ]`, then Yes: the board shows the original arrangement and `Moves:` is hidden.
        // TODO: 4. Mordie's line reads `Here's how I did it in N moves!`.
        // TODO: 5. The step list is numbered, zero-padded to the total's width, every line the same width.
        // TODO: 6. `[ ▶ Play ]` shows, `[ Give Up ]` is dimmed, and clicking an arrow or tile shifts nothing.
        // TODO: 7. `game_given_up` {move_count 2, solution_length N, moves_since_solution_found, shifts_since_solution_found}; `screen_solution` {opened_from: give_up}.
    }

    @Test
    fun `the replay plays, pauses, resumes and plays again`() {
        // WU3
        // TODO: 1. On the solution screen, click Play: the button reads `[ ❚❚ Pause ]`, step 1 is in reverse video; `solution_replay_started` {restart: false}.
        // TODO: 2. Ticks advance the board one shift per 500 ms timeout, with a 500 ms pause between moves.
        // TODO: 3. Click Pause mid-move: further Ticks change nothing.
        // TODO: 4. Open the Save dialog and cancel it: the replay stays paused (D7).
        // TODO: 5. Click Play: it resumes from the same shift.
        // TODO: 6. On the solved board: the button offers Play again, the last step stays highlighted; `solution_replay_finished` {paused: true}.
        // TODO: 7. Click Play again: the board resets to the original arrangement; `solution_replay_started` {restart: true}.
        // TODO: 8. With a long solution, the list auto-scrolls to keep the current step in view, and the mouse wheel scrolls it.
    }

    @Test
    fun `keyboard mode - F8 gives up, Space and Enter play and pause, W and S scroll the steps`() {
        // WU3
        // TODO: 1. Keyboard mode before the reveal: F8 does nothing; the controls hint lists F8.
        // TODO: 2. After the reveal, F8 then Escape: the game continues; `dialog_cancelled` {dialog: give_up}.
        // TODO: 3. F8, confirm Yes: the solution screen opens.
        // TODO: 4. Enter plays, Space pauses, Enter resumes.
        // TODO: 5. With a long solution, S and ↓ scroll the list down, W and ↑ scroll it back up.
    }

    @Test
    fun `after a win, Mordie's solution opens the screen without a confirmation`() {
        // WU3
        // TODO: 1. Mordie has found its solution, then the player solves the board: the row shows `[ Mordie's solution ]`.
        // TODO: 2. Click it: no dialog; the solution screen opens with `Here's how I did it in N moves!`.
        // TODO: 3. `screen_solution` {opened_from: after_win}; no `game_given_up`.
    }

    @Test
    fun `after You beat me, the Give Up row is empty`() {
        // WU3
        // TODO: 1. The player solves the board before Mordie finds its solution: the `You beat me!` ending, the row is blank.
        // TODO: 2. Clicking where the row was does nothing.
    }

    @Test
    fun `a solution-screen save reopens the screen via Load and startup restore`() {
        // WU3
        // TODO: 1. Give up, play part of the replay, save as `s`.
        // TODO: 2. New: the screen is left - `Moves:` shows and the board is playable.
        // TODO: 3. Load `s`: the solution screen at the original arrangement, `[ ▶ Play ]`, no highlight; `screen_solution` {opened_from: load}.
        // TODO: 4. A second session restores `s` at startup: the solution screen; `screen_solution` {opened_from: startup_prompt}.
    }

    @Test
    fun `Load of a regular save leaves the solution screen`() {
        // WU3
        // TODO: 1. On the solution screen, load a regular game save: that game is playable, with its `Moves:` count.
    }

    @Test
    fun `switching input mode keeps the solution screen and the replay`() {
        // WU3
        // TODO: 1. Start the replay, switch mouse to keyboard: the screen, the current step and the playing state are kept.
        // TODO: 2. Further Ticks keep advancing the replay (D6).
    }

    @Test
    fun `a save from before GH-126 with Mordie's solution found keeps Give Up dimmed`() {
        // WU3
        // TODO: 1. Load a `found` save with no solution line: `Found it!` shows, `[ Give Up ]` stays dimmed (D2).
        // TODO: 2. Solve the board: the row stays empty.
    }
}
