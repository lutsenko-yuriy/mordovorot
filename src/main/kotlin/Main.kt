import analytics.AnalyticsService
import analytics.NoopAnalyticsService
import board_model.BoardImpl
import board_model.BoardSize
import kotlinx.coroutines.runBlocking

/**
 * Created by yurich on 02.12.16.
 */
fun main(args: Array<String>): Unit = runBlocking {
    val analytics = NoopAnalyticsService()
    val boardSize = resolveBoardSize(args)
    val mode = resolveInputMode(args, analytics, boardSize = boardSize)
    // sizeChosenAtLaunch threading into ViewModelImpl's startup size prompt lands in WU2, once
    // that prompt exists (GH-44).
    GameSession(initialMode = mode, board = BoardImpl(boardSize), analytics = analytics).run()
}

/** Parses the `--size=N` launch flag (GH-44) - only this `=`-joined form is recognized (a
 *  space-separated `--size 4` falls through as two unrecognized arguments to
 *  [resolveInputMode] instead). A missing flag, a non-numeric value, or one outside
 *  [BoardSize.isValid] all fall back to [BoardSize.DEFAULT] with a warning (except a missing
 *  flag, which is simply the default and warns nothing). Split out from [main] for the same
 *  testability reason as [resolveInputMode]. */
fun resolveBoardSize(
    args: Array<String>,
    warnInvalidSize: (String) -> Unit = { System.err.println(it) },
): Int {
    val flag = args.firstOrNull { it.startsWith("--size=") } ?: return BoardSize.DEFAULT
    val value = flag.removePrefix("--size=")
    val size = value.toIntOrNull()
    if (size == null || !BoardSize.isValid(size)) {
        warnInvalidSize(
            "'--size=$value' is not a valid board size (must be ${BoardSize.MIN}-${BoardSize.MAX}) - " +
                "falling back to ${BoardSize.DEFAULT}."
        )
        return BoardSize.DEFAULT
    }
    return size
}

/** Resolves the starting input mode, warns on any unrecognized argument, and tracks
 *  `app_launched` - split out from [main] so it's unit-testable without running the whole game
 *  loop. */
fun resolveInputMode(
    args: Array<String>,
    analytics: AnalyticsService,
    boardSize: Int = BoardSize.DEFAULT,
    warnUnrecognizedArg: (String) -> Unit = { System.err.println("Unrecognized argument: '$it' - ignoring.") },
    warnConsoleKeyboardConflict: () -> Unit = { System.err.println("'--keyboard' ignored - '--console' takes precedence.") },
    warnNoInteractiveTerminal: (String) -> Unit = { flag ->
        System.err.println("'$flag' ignored - no interactive terminal detected, falling back to console mode.")
    },
    hasInteractiveTerminal: () -> Boolean = { System.console() != null },
): InputMode {
    val knownArgs = setOf("--console", "--keyboard", "--mouse")
    args.filter { it !in knownArgs && !it.startsWith("--size=") }.forEach(warnUnrecognizedArg)
    if ("--console" in args && "--keyboard" in args) warnConsoleKeyboardConflict()
    val mode = InputMode.resolve(args, hasInteractiveTerminal)
    // An explicit --keyboard/--mouse silently downgrading to CONSOLE (no TTY) deserves the same
    // feedback the --console/--keyboard conflict above gets.
    if (mode == InputMode.CONSOLE && "--console" !in args) {
        val requested = listOfNotNull("--keyboard".takeIf { it in args }, "--mouse".takeIf { it in args })
        requested.forEach(warnNoInteractiveTerminal)
    }
    analytics.track("app_launched", mapOf("mode" to mode.name.lowercase(), "board_size" to boardSize))
    return mode
}
