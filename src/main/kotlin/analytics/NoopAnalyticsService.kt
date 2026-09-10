package analytics

/**
 * No-op [AnalyticsService] — the default until a real analytics SDK is wired
 * up. Call sites can start tracking events now without waiting on that work.
 */
class NoopAnalyticsService : AnalyticsService {
    override fun track(event: String, properties: Map<String, Any?>) {
        // Intentionally does nothing.
    }
}
