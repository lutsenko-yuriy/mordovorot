package analytics

/** Decorates another [AnalyticsService], adding `platform` (`terminal`/`web`, GH-110) to every
 *  forwarded event, the way [InputMethodAnalyticsService] adds `input_method`. */
class PlatformAnalyticsService(
    private val delegate: AnalyticsService,
    private val platform: String,
) : AnalyticsService {

    override fun track(event: String, properties: Map<String, Any?>) {
        val decorated = if ("platform" in properties) properties else properties + ("platform" to platform)
        delegate.track(event, decorated)
    }
}
