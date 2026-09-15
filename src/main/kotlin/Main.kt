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
    val parsedSize = resolveBoardSize(args)
    val boardSize = parsedSize ?: BoardSize.DEFAULT
    val mode = resolveInputMode(args, analytics, boardSize = boardSize)
    // An invalid --size=N (out of range, non-numeric, blank) degrades to "no flag given" here
    // too - sizeChosenAtLaunch must track whether the flag was actually usable, not just
    // present, or a typo'd flag would silently suppress the restore/size prompts on top of
    // falling back to the default size (audit finding on PR #52).
    val sizeChosenAtLaunch = parsedSize != null
    GameSession(
        initialMode = mode,
        board = BoardImpl(boardSize),
        sizeChosenAtLaunch = sizeChosenAtLaunch,
        analytics = analytics,
    ).run()
}

/** Parses the `--size=N` launch flag (GH-44) - only this `=`-joined form is recognized (a
 *  space-separated `--size 4` falls through as two unrecognized arguments to
 *  [resolveInputMode] instead). Returns `null` - not [BoardSize.DEFAULT] - for a missing flag,
 *  a non-numeric value, or one outside [BoardSize.isValid]: a caller needs to tell "the flag
 *  wasn't usable" apart from "the flag asked for exactly the default", since
 *  [main]'s `sizeChosenAtLaunch` (whether the startup restore/size prompts should run at all)
 *  depends on that distinction, not just on whether `--size=` was present (audit finding on
 *  PR #52 - a typo'd flag was silently suppressing both prompts while falling back to the
 *  default size). Split out from [main] for the same testability reason as [resolveInputMode]. */
fun resolveBoardSize(
    args: Array<String>,
    warnInvalidSize: (String) -> Unit = { System.err.println(it) },
): Int? {
    val flag = args.firstOrNull { it.startsWith("--size=") } ?: return null
    val value = flag.removePrefix("--size=")
    val size = value.toIntOrNull()
    if (size == null || !BoardSize.isValid(size)) {
        warnInvalidSize(
            "'--size=$value' is not a valid board size (must be ${BoardSize.MIN}-${BoardSize.MAX}) - " +
                "falling back to ${BoardSize.DEFAULT}."
        )
        return null
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
