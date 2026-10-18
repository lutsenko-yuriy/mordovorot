package web.scenario

import kotlin.test.Test

/** GH-126 scenarios: giving up and Mordie's solution screen in the browser, through [WebHarness]
 *  with virtual time for the replay's 1 s moves and 0.5 s pauses. Same setup as
 *  [WebStartupScenarioTest]. Each test names the WU that makes it green. */
class WebGiveUpScenarioTest {

    @Test
    fun `Give Up is disabled until Mordie finds its solution`() {
        // WU4
        // TODO: 1. Before the reveal: the state shows Give Up disabled, and `onGiveUp()` opens no dialog.
        // TODO: 2. After the reveal: Give Up is enabled.
    }

    @Test
    fun `cancel keeps the game and confirm opens the solution screen`() {
        // WU4
        // TODO: 1. Shift, Give Up, cancel: the game is unchanged; `dialog_cancelled` {dialog: give_up}.
        // TODO: 2. Give Up, confirm: the original arrangement, moves hidden, padded steps, Mordie's `Here's how I did it in N moves!`.
        // TODO: 3. Arrows and drags shift nothing.
        // TODO: 4. `hasUnsavedProgress()` is false (D1).
        // TODO: 5. `screen_give_up_dialog`, `game_given_up` and `screen_solution` {opened_from: give_up} as in the terminal.
    }

    @Test
    fun `the replay animates each move over 1 s with 0_5 s between moves, and Pause finishes the current move`() {
        // WU4
        // TODO: 1. Play: after 1 s the first move is done; after another 0.5 s the second move starts.
        // TODO: 2. Pause during the second move: it finishes, then nothing changes (D5).
        // TODO: 3. Play, open a dialog: the replay holds, and continues once the dialog closes (D7).
        // TODO: 4. At the end, the button offers Play again, which resets the board to the original arrangement.
        // TODO: 5. `hasUnsavedProgress()` stays false throughout; `solution_replay_started` and `solution_replay_finished` fire.
    }

    @Test
    fun `after a win, Mordie's solution opens the screen, and after You beat me there is none`() {
        // WU4
        // TODO: 1. Win with Mordie's solution found: Mordie's solution opens the screen with no confirmation; `screen_solution` {opened_from: after_win}.
        // TODO: 2. Win on the `You beat me!` path: the action is absent.
    }

    @Test
    fun `a solution-screen save reopens the screen, and New and Load leave it`() {
        // WU4
        // TODO: 1. Save on the screen; load it: the screen reopens reset; `screen_solution` {opened_from: load}.
        // TODO: 2. Restore it at startup: the screen reopens reset; `screen_solution` {opened_from: startup_prompt}.
        // TODO: 3. New leaves the screen; so does loading a regular save.
    }
}
