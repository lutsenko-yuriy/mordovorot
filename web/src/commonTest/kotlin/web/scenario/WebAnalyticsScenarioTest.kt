package web.scenario

import kotlin.test.Test

/** GH-110 scenarios: the web's analytics, per `docs/ANALYTICS_EVENTS.md`. Same setup as
 *  [WebStartupScenarioTest]. */
class WebAnalyticsScenarioTest {

    @Test
    fun `every event carries platform web and input_method rich_ui`() {
        // WU4
        // TODO: 1. Play a session: launch, New, a shift, a solve.
        // TODO: 2. Every event and screen view has platform: web.
        // TODO: 3. Every one except app_launched has input_method: rich_ui.
    }

    @Test
    fun `app_launched has mode rich_ui and the layout, and no terminal-only properties`() {
        // WU4
        // TODO: 1. Launch at 1200x800: {mode: rich_ui, layout: wide, platform: web}.
        // TODO: 2. Launch at 800x800: layout: tall.
        // TODO: 3. No board_size and no console_flag_ignored.
    }

    @Test
    fun `no Exit events ever fire on web`() {
        // WU5
        // TODO: 1. Go through every dialog path.
        // TODO: 2. None of exit_command_used, input_mode_switched, app_launch_aborted, screen_exit_dialog,
        //          decision: quit, choice: exit, dialog: exit fired.
    }
}
