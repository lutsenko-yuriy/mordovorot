import analytics.AnalyticsService
import analytics.InputMethodAnalyticsService
import analytics.NoopAnalyticsService
import presenter.PresenterImpl
import view.ViewImpl

/**
 * Created by yurich on 02.12.16.
 */
fun main(args: Array<String>) {
    val analytics = NoopAnalyticsService()
    val mode = resolveLaunchMode(args, analytics)

    // TODO(GH-3 WU3): once view.tui.TuiView exists, LaunchMode.MOUSE builds and plays a
    // mouse-driven TuiView instead of the console ViewImpl below. This WU only lands the
    // mode-resolution/analytics plumbing, so both modes run the console UI for now.
    val view = ViewImpl.create { v -> PresenterImpl(v, analytics = InputMethodAnalyticsService(analytics, mode.name.lowercase())) }
    view.play()
}

/** Resolves the launch mode and tracks `app_launched` - split out from [main] so it's
 *  unit-testable without running the whole game loop. Reports any argument other than
 *  `--console` via [warnUnrecognizedArg] instead of silently falling through to the default
 *  mode (audit on PR #20 - a typo like `-console` previously left the user guessing why
 *  `--console` "didn't work"). */
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
