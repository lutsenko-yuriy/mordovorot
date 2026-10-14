import kotlin.test.Test

/** GH-66 scenarios: console solved state - final board, Congratulations message, and the
 *  `Start a new game?` prompt. Real BoardImpl + ViewModelImpl + FileSaveRepository (temp dir),
 *  driven through the console view like [MoveCounterScenarioTest]. */
class SolvedConsoleScenarioTest {

    @Test
    fun `solving shows the final board, its move count and the congratulations message`() {
        // TODO: 4x4 one `left 1` from solved, --size semantics; input `up 2`, `down 2`, `left 1`, EOF
        // TODO: output has the solved board (1 2 3 4 ... 16) followed by `Moves: 2`
        // TODO: then `Congratulations, you solved this puzzle in 2 moves!`
        // TODO: then `Start a new game? [y/N] `
        // TODO: session ends
    }

    @Test
    fun `a one-move solve uses the singular`() {
        // TODO: input `left 1`, EOF
        // TODO: output contains `in 1 move!` and not `1 moves`
    }

    @Test
    fun `accepting a new game asks for the board size and starts a fresh game at that size`() {
        // TODO: input `left 1`, `y`, `3`, EOF
        // TODO: after the prompt, output contains `Board size? [3-5, Enter for 4]: `
        // TODO: a 3x3 board with `Moves: 0` is shown afterwards
        // TODO: analytics has play_again_decision {decision: new_game, input_method: console}
        // TODO: analytics has new_game_size_selected {size: 3, trigger: play_again, input_method: console}
    }

    @Test
    fun `declining a new game ends the session`() {
        // TODO: three runs - `left 1` + `n` + `left 1`; `left 1` + empty line + `left 1`; `left 1` + EOF
        // TODO: each: nothing printed after `Start a new game? [y/N] ` (trailing `left 1` never processed)
        // TODO: each: play_again_decision {decision: quit}
    }

    @Test
    fun `loading an already-solved save shows congratulations with 0 moves and the prompt`() {
        // TODO: seed done.save with a solved 4x4 board; start scrambled; input `load done`, EOF
        // TODO: output has `in 0 moves!` then `Start a new game? [y/N] `
        // TODO: board_solved not fired
        // TODO: play_again_decision {decision: quit} fired
    }

    @Test
    fun `restoring an already-solved save at startup goes straight to congratulations`() {
        // TODO: seed exactly one save done.save (solved); launch without --size
        // TODO: input `y` (restore), EOF
        // TODO: output has the solved board, `Moves: 0`, `in 0 moves!`, and the prompt
    }
}
