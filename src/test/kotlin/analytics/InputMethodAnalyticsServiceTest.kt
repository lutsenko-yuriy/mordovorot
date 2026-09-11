package analytics

import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers [InputMethodAnalyticsService], the decorator that adds `input_method` to every
 * event it forwards (GH-3) - so `save_command_used`, `load_command_used`,
 * `startup_restore_prompt_shown`, `startup_restore_decision`, and `exit_command_used` all
 * gain the property with zero changes to `BasePresenter`'s own `track(...)` call sites.
 */
class InputMethodAnalyticsServiceTest {

    @Test
    fun `adds input_method to a forwarded event's properties`() {
        val recording = RecordingAnalyticsService()
        val analytics = InputMethodAnalyticsService(recording, inputMethod = "mouse")

        analytics.track("save_command_used", mapOf("result" to "success"))

        assertEquals(
            listOf(Event("save_command_used", mapOf("result" to "success", "input_method" to "mouse"))),
            recording.events,
        )
    }

    @Test
    fun `preserves all other properties already present`() {
        val recording = RecordingAnalyticsService()
        val analytics = InputMethodAnalyticsService(recording, inputMethod = "console")

        analytics.track("load_command_used", mapOf("trigger" to "command", "result" to "success"))

        assertEquals(
            listOf(
                Event(
                    "load_command_used",
                    mapOf("trigger" to "command", "result" to "success", "input_method" to "console"),
                ),
            ),
            recording.events,
        )
    }

    @Test
    fun `does not overwrite an input_method the caller already supplied explicitly`() {
        val recording = RecordingAnalyticsService()
        val analytics = InputMethodAnalyticsService(recording, inputMethod = "mouse")

        analytics.track("some_event", mapOf("input_method" to "console"))

        assertEquals(
            listOf(Event("some_event", mapOf("input_method" to "console"))),
            recording.events,
        )
    }
}
