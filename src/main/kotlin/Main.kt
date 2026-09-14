import analytics.AnalyticsService
import analytics.InputMethodAnalyticsService
import analytics.NoopAnalyticsService
import presenter.ConsolePresenterImpl
import presenter.TuiPresenterImpl
import view.ViewImpl
import view.tui.AnsiTerminal
import view.tui.KeyboardInput
import view.tui.TuiView

/**
 * Created by yurich on 02.12.16.
 */
fun main(args: Array<String>) {
    val analytics = NoopAnalyticsService()
    val mode = resolveLaunchMode(args, analytics)
    val decoratedAnalytics = InputMethodAnalyticsService(analytics, mode.name.lowercase())

    when (mode) {
        // Both TUI modes pass decoratedAnalytics - the view needs input_method too, not just the presenter.
        LaunchMode.MOUSE ->
            TuiView.create(AnsiTerminal(), analytics = decoratedAnalytics) { v -> TuiPresenterImpl(v, analytics = decoratedAnalytics) }.play()
        LaunchMode.KEYBOARD ->
            TuiView.create(AnsiTerminal(), analytics = decoratedAnalytics, input = KeyboardInput()) { v ->
                TuiPresenterImpl(v, analytics = decoratedAnalytics)
            }.play()
        LaunchMode.CONSOLE -> ViewImpl.create { v -> ConsolePresenterImpl(v, analytics = decoratedAnalytics) }.play()
    }
}

/** Resolves the launch mode, warns on any unrecognized argument, and tracks `app_launched` -
 *  split out from [main] so it's unit-testable without running the whole game loop. */
fun resolveLaunchMode(
    args: Array<String>,
    analytics: AnalyticsService,
    warnUnrecognizedArg: (String) -> Unit = { System.err.println("Unrecognized argument: '$it' - ignoring.") },
    warnConsoleKeyboardConflict: () -> Unit = { System.err.println("'--keyboard' ignored - '--console' takes precedence.") },
    warnNoInteractiveTerminal: (String) -> Unit = { flag ->
        System.err.println("'$flag' ignored - no interactive terminal detected, falling back to console mode.")
    },
    hasInteractiveTerminal: () -> Boolean = { System.console() != null },
): LaunchMode {
    val knownArgs = setOf("--console", "--keyboard", "--mouse")
    args.filter { it !in knownArgs }.forEach(warnUnrecognizedArg)
    if ("--console" in args && "--keyboard" in args) warnConsoleKeyboardConflict()
    val mode = LaunchMode.resolve(args, hasInteractiveTerminal)
    // An explicit --keyboard/--mouse silently downgrading to CONSOLE (no TTY) deserves the same
    // feedback the --console/--keyboard conflict above gets.
    if (mode == LaunchMode.CONSOLE && "--console" !in args) {
        val requested = listOfNotNull("--keyboard".takeIf { it in args }, "--mouse".takeIf { it in args })
        requested.forEach(warnNoInteractiveTerminal)
    }
    analytics.track("app_launched", mapOf("mode" to mode.name.lowercase()))
    return mode
}
