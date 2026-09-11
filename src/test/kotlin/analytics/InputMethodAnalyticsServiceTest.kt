package analytics

import kotlin.test.Test

/**
 * Scenario stubs for GH-3's `input_method` decorator: wrapping an `AnalyticsService` so
 * every forwarded event gains an `input_method` property, without touching the five
 * existing presenter-side `track(...)` call sites. Filled in by `implement` during WU1,
 * once `InputMethodAnalyticsService` exists.
 */
class InputMethodAnalyticsServiceTest {

    @Test
    fun `adds input_method to a forwarded event's properties`() {
        // TODO: Wrap a RecordingAnalyticsService with InputMethodAnalyticsService(mode = "mouse")
        // TODO: Call track("save_command_used", mapOf("result" to "success"))
        // TODO: Verify the recorded event's properties include input_method = "mouse"
    }

    @Test
    fun `preserves all other properties already present`() {
        // TODO: Call track with multiple properties
        // TODO: Verify all original properties are still present alongside input_method
    }

    @Test
    fun `does not overwrite an input_method the caller already supplied explicitly`() {
        // TODO: Call track with an explicit input_method already in the map
        // TODO: Verify the decorator leaves that value untouched
    }
}
