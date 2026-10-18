package web.scenario

import kotlin.test.Test

/** GH-126 scenarios: giving up and Mordie's solution screen in the browser, through [WebHarness]
 *  stepping the replay by hand with Prev / Next / Restart. Same setup as
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
    fun `Next and Prev step the replay one shift at a time, with a slide, and Restart goes back to the start`() {
        // WU4
        // TODO: 1. At the start Prev and Restart are disabled; Next applies one shift (a one-tile slide) and highlights its step.
        // TODO: 2. Prev undoes the last shift (a slide back); stepping back to the start clears the highlight.
        // TODO: 3. At the solved board Next is disabled; Restart resets to the original arrangement.
        // TODO: 4. `hasUnsavedProgress()` stays false throughout; `solution_replay_started` {restart} and `solution_replay_finished` {stepped_back} fire.
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
