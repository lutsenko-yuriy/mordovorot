import analytics.AnalyticsService
import analytics.NoopAnalyticsService
import kotlinx.coroutines.runBlocking

/**
 * Created by yurich on 02.12.16.
 */
fun main(args: Array<String>): Unit = runBlocking {
    val analytics = NoopAnalyticsService()
    val mode = resolveInputMode(args, analytics)
    GameSession(initialMode = mode, analytics = analytics).run()
}

/** Resolves the starting input mode, warns on any unrecognized argument, and tracks
 *  `app_launched` - split out from [main] so it's unit-testable without running the whole game
 *  loop. */
fun resolveInputMode(
    args: Array<String>,
    analytics: AnalyticsService,
    warnUnrecognizedArg: (String) -> Unit = { System.err.println("Unrecognized argument: '$it' - ignoring.") },
    warnConsoleKeyboardConflict: () -> Unit = { System.err.println("'--keyboard' ignored - '--console' takes precedence.") },
    warnNoInteractiveTerminal: (String) -> Unit = { flag ->
        System.err.println("'$flag' ignored - no interactive terminal detected, falling back to console mode.")
    },
    hasInteractiveTerminal: () -> Boolean = { System.console() != null },
): InputMode {
    val knownArgs = setOf("--console", "--keyboard", "--mouse")
    args.filter { it !in knownArgs }.forEach(warnUnrecognizedArg)
    if ("--console" in args && "--keyboard" in args) warnConsoleKeyboardConflict()
    val mode = InputMode.resolve(args, hasInteractiveTerminal)
    // An explicit --keyboard/--mouse silently downgrading to CONSOLE (no TTY) deserves the same
    // feedback the --console/--keyboard conflict above gets.
    if (mode == InputMode.CONSOLE && "--console" !in args) {
        val requested = listOfNotNull("--keyboard".takeIf { it in args }, "--mouse".takeIf { it in args })
        requested.forEach(warnNoInteractiveTerminal)
    }
    analytics.track("app_launched", mapOf("mode" to mode.name.lowercase()))
    return mode
}
