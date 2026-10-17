package testing

import analytics.AnalyticsService

/**
 * An [AnalyticsService] test double that records every `track` call instead of sending it
 * anywhere, so tests can assert on the exact event name/property sequence emitted.
 */
class RecordingAnalyticsService : AnalyticsService {

    data class Event(val name: String, val properties: Map<String, Any?>)

    val events = mutableListOf<Event>()

    override fun track(event: String, properties: Map<String, Any?>) {
        events.add(Event(event, properties))
    }
}
