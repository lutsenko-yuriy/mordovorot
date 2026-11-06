package analytics

/**
 * Decorates another [AnalyticsService], adding an `input_method` property to every forwarded
 * event (on `web`: `keyboard` or `rich_ui`, the press that fired it, GH-171) - lets the viewModel's
 * existing events be split by input without touching its `track(...)` call sites.
 */
class InputMethodAnalyticsService(
    private val delegate: AnalyticsService,
    private val inputMethod: () -> String,
) : AnalyticsService {

    /** For a client whose input method is fixed for the session. */
    constructor(delegate: AnalyticsService, inputMethod: String) : this(delegate, { inputMethod })

    override fun track(event: String, properties: Map<String, Any?>) {
        val decorated = if ("input_method" in properties) properties else properties + ("input_method" to inputMethod())
        delegate.track(event, decorated)
    }
}
