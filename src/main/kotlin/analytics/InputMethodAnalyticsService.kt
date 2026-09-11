package analytics

/**
 * Decorates another [AnalyticsService], adding an `input_method` property (`console` or
 * `mouse`) to every event it forwards - so the presenter-side events (`save_command_used`,
 * `load_command_used`, `startup_restore_prompt_shown`, `startup_restore_decision`,
 * `exit_command_used`) become distinguishable by launch mode (GH-3) without touching any of
 * their existing `track(...)` call sites. Wrap the [Presenter][presenter.Presenter]'s
 * analytics service with this rather than modifying [PresenterImpl][presenter.PresenterImpl]
 * itself.
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
