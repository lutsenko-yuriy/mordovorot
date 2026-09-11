package analytics

/**
 * Decorates another [AnalyticsService], adding an `input_method` (`console`/`mouse`)
 * property to every forwarded event - lets [presenter.BasePresenter]'s existing events be
 * split by launch mode without touching its `track(...)` call sites.
 */
class InputMethodAnalyticsService(
    private val delegate: AnalyticsService,
    private val inputMethod: String,
) : AnalyticsService {

    override fun track(event: String, properties: Map<String, Any?>) {
        val decorated = if ("input_method" in properties) properties else properties + ("input_method" to inputMethod)
        delegate.track(event, decorated)
    }
}
