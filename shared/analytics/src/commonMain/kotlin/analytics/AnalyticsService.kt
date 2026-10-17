package analytics

/**
 * Thin abstraction over whatever analytics SDK eventually gets wired in.
 * Callers depend on this interface, not a concrete implementation, so the
 * backing service can be swapped without touching call sites.
 *
 * See docs/ANALYTICS_EVENTS.md for the catalogue of event names and properties.
 */
interface AnalyticsService {
    fun track(event: String, properties: Map<String, Any?> = emptyMap())
}
