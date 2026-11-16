package web.scenario

import kotlin.test.Test

/** GH-289 (WU0) scenario stubs: the replay's analytics, as catalogued in `docs/ANALYTICS_EVENTS.md`. */
class WebTutorialReplayAnalyticsScenarioTest {
    @Test
    fun `the save question sends screen_replay_save_dialog from a click`() = webTest {
        // TODO: 1. Click Tutorial with unsaved progress.
        // TODO: 2. The `screen_replay_save_dialog` event is sent with `opened_from: button`.
    }

    @Test
    fun `the save question sends screen_replay_save_dialog from key 8`() = webTest {
        // TODO: 1. Press 8 with unsaved progress.
        // TODO: 2. The `screen_replay_save_dialog` event is sent with `opened_from: shortcut`.
    }

    @Test
    fun `the question's event carries the move count`() = webTest {
        // TODO: 1. Make two shifts.
        // TODO: 2. Open the save question.
        // TODO: 3. The event's `move_count` is 2.
    }

    @Test
    fun `Save sends tutorial_replay_decision`() = webTest {
        // TODO: 1. Choose Save.
        // TODO: 2. The `tutorial_replay_decision` event is sent with `decision: save`.
    }

    @Test
    fun `Don't save sends tutorial_replay_decision`() = webTest {
        // TODO: 1. Choose Don't save.
        // TODO: 2. The `tutorial_replay_decision` event is sent with `decision: dont_save`.
    }

    @Test
    fun `Cancel sends dialog_cancelled, not a decision`() = webTest {
        // TODO: 1. Choose Cancel.
        // TODO: 2. The `dialog_cancelled` event is sent with `dialog: replay_save`.
        // TODO: 3. `tutorial_replay_decision` is not sent.
    }

    @Test
    fun `the Save dialog from the question has its own source`() = webTest {
        // TODO: 1. Choose Save.
        // TODO: 2. The `screen_save_dialog` event is sent with `opened_from: tutorial_replay`.
    }

    @Test
    fun `a replay with nothing to save sends tutorial_started with source replay`() = webTest {
        // TODO: 1. Click Tutorial with no unsaved progress.
        // TODO: 2. The `tutorial_started` event is sent with `source: replay`.
    }

    @Test
    fun `a replay after Don't save sends tutorial_started with source replay`() = webTest {
        // TODO: 1. Choose Don't save.
        // TODO: 2. The `tutorial_started` event is sent with `source: replay`.
    }

    @Test
    fun `a replay's end sends tutorial_ended with source replay`() = webTest {
        // TODO: 1. Play a replay to the end.
        // TODO: 2. The `tutorial_ended` event is sent with `source: replay`.
    }

    @Test
    fun `a first launch's end sends tutorial_ended with source first_launch`() = webTest {
        // TODO: 1. Play the first-launch tutorial to the end.
        // TODO: 2. The `tutorial_ended` event is sent with `source: first_launch`.
    }
}
