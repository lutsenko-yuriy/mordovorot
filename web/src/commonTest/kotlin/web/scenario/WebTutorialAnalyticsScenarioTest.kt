package web.scenario

import kotlin.test.Test

/** GH-288 (WU0) scenario stubs: the tutorial's analytics events (docs/ANALYTICS_EVENTS.md, GH-288 section). The WU that wires the tutorial fills them in. */
class WebTutorialAnalyticsScenarioTest {

    @Test
    fun `tutorial_started is sent on a first launch`() = webTest {
        // TODO: 1. First launch.
        // TODO: 2. The `tutorial_started` event is sent.
    }

    @Test
    fun `tutorial_started is sent again after a reload halfway`() = webTest {
        // TODO: 1. Make a shift.
        // TODO: 2. Relaunch.
        // TODO: 3. `tutorial_started` is sent a second time.
    }

    @Test
    fun `no tutorial events for a player with saves`() = webTest {
        // TODO: 1. Seed a save.
        // TODO: 2. Launch.
        // TODO: 3. No `tutorial_*` event is sent.
    }

    @Test
    fun `tutorial_step_completed is sent for each of steps 1 to 4`() = webTest {
        // TODO: 1. Play the four steps.
        // TODO: 2. `tutorial_step_completed` is sent four times, with `step` 1 to 4.
    }

    @Test
    fun `tutorial_step_completed carries the step's blocked shifts`() = webTest {
        // TODO: 1. Make two blocked shifts on step 1.
        // TODO: 2. Do step 1.
        // TODO: 3. The event for step 1 carries `blocked_shifts` 2.
    }

    @Test
    fun `tutorial_ended is sent as completed at step 6`() = webTest {
        // TODO: 1. Play the four steps and wait 3 s.
        // TODO: 2. `tutorial_ended` is sent with `result` completed and `last_step` 6.
    }

    @Test
    fun `tutorial_ended is sent as skipped, with the step`() = webTest {
        // TODO: 1. Do step 1.
        // TODO: 2. Press Skip.
        // TODO: 3. `tutorial_ended` is sent with `result` skipped and `last_step` 2.
    }

    @Test
    fun `Controls opened from the panel says so`() = webTest {
        // TODO: 1. Press the panel's Controls button.
        // TODO: 2. `screen_controls_dialog` is sent with `opened_from` `tutorial`.
    }

    @Test
    fun `Labels opened from the panel says so`() = webTest {
        // TODO: 1. Press the panel's Labels button.
        // TODO: 2. `screen_labels_dialog` is sent with `opened_from` `tutorial`.
    }

    @Test
    fun `board_solved is flagged as the tutorial's`() = webTest {
        // TODO: 1. Play the four steps.
        // TODO: 2. `board_solved` is sent with `tutorial` true.
    }

    @Test
    fun `mordie_solution_found is flagged as the tutorial's`() = webTest {
        // TODO: 1. First launch.
        // TODO: 2. Mordie finds its solution.
        // TODO: 3. `mordie_solution_found` is sent with `tutorial` true.
    }

    @Test
    fun `a new game after the tutorial is not flagged`() = webTest {
        // TODO: 1. Play to step 6.
        // TODO: 2. Choose New, then 4x4.
        // TODO: 3. Solve it.
        // TODO: 4. `board_solved` is sent with `tutorial` false.
    }
}
