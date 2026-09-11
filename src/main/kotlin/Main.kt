import analytics.AnalyticsService
import analytics.InputMethodAnalyticsService
import analytics.NoopAnalyticsService
import presenter.PresenterImpl
import view.ViewImpl
import view.tui.AnsiTerminal
import view.tui.TuiView

/**
 * Created by yurich on 02.12.16.
 */
fun main(args: Array<String>) {
    val analytics = NoopAnalyticsService()
    val mode = resolveLaunchMode(args, analytics)
    val decoratedAnalytics = InputMethodAnalyticsService(analytics, mode.name.lowercase())

    when (mode) {
        LaunchMode.MOUSE ->
            TuiView.create(AnsiTerminal(), analytics = analytics) { v -> PresenterImpl(v, analytics = decoratedAnalytics) }.play()
        LaunchMode.CONSOLE -> ViewImpl.create { v -> PresenterImpl(v, analytics = decoratedAnalytics) }.play()
    }
}

/** Resolves the launch mode, warns on any unrecognized argument, and tracks `app_launched` -
 *  split out from [main] so it's unit-testable without running the whole game loop. */
fun resolveLaunchMode(
    args: Array<String>,
    analytics: AnalyticsService,
    warnUnrecognizedArg: (String) -> Unit = { System.err.println("Unrecognized argument: '$it' - ignoring.") },
): LaunchMode {
    args.filter { it != "--console" }.forEach(warnUnrecognizedArg)
    val mode = LaunchMode.resolve(args)
    analytics.track("app_launched", mapOf("mode" to mode.name.lowercase()))
    return mode
}
