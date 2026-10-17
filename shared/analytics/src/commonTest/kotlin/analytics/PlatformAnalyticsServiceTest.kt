package analytics

import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import kotlin.test.Test
import kotlin.test.assertEquals

/** Covers [PlatformAnalyticsService] (GH-110): every forwarded event gains `platform`. */
class PlatformAnalyticsServiceTest {

    @Test
    fun `adds platform to a forwarded event, keeping its other properties`() {
        val recording = RecordingAnalyticsService()
        val analytics = PlatformAnalyticsService(recording, platform = "web")

        analytics.track("save_command_used", mapOf("result" to "success"))

        assertEquals(
            listOf(Event("save_command_used", mapOf("result" to "success", "platform" to "web"))),
            recording.events,
        )
    }

    @Test
    fun `adds platform to an event with no properties`() {
        val recording = RecordingAnalyticsService()
        val analytics = PlatformAnalyticsService(recording, platform = "terminal")

        analytics.track("screen_congratulations")

        assertEquals(listOf(Event("screen_congratulations", mapOf("platform" to "terminal"))), recording.events)
    }

    @Test
    fun `does not overwrite a platform the caller already supplied explicitly`() {
        val recording = RecordingAnalyticsService()
        val analytics = PlatformAnalyticsService(recording, platform = "terminal")

        analytics.track("some_event", mapOf("platform" to "web"))

        assertEquals(listOf(Event("some_event", mapOf("platform" to "web"))), recording.events)
    }
}
