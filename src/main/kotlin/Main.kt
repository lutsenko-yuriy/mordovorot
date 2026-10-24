import analytics.AnalyticsService
import analytics.NoopAnalyticsService
import board_model.BoardImpl
import board_model.BoardSize
import kotlinx.coroutines.runBlocking
import mordie.BackgroundMordieSearch
import mordie.Mordie
import storage.FilePreferenceRepository
import storage.FileSaveRepository
import view.tui.AnsiTerminal
import viewmodel.SessionTileLabels
import viewmodel.TileLabels
import viewmodel.TileLabelsPreference

/**
 * Created by yurich on 02.12.16.
 */
fun main(args: Array<String>): Unit = runBlocking {
    val analytics = terminalAnalytics(NoopAnalyticsService())
    val parsedSize = resolveBoardSize(args)
    val boardSize = parsedSize ?: BoardSize.DEFAULT
    // No terminal: the abort message is already printed - return normally, so the exit is 0.
    val tileLabels = SessionTileLabels(TileLabelsPreference(FilePreferenceRepository()))
    val mode = resolveInputMode(args, analytics, boardSize = boardSize, tileLabels = tileLabels.effectivePreference.wireName)
        ?: return@runBlocking
    // An invalid --size=N (out of range, non-numeric, blank) degrades to "no flag given" here
    // too - sizeChosenAtLaunch must track whether the flag was actually usable, not just
    // present, or a typo'd flag would silently suppress the restore/size prompts on top of
    // falling back to the default size (audit finding on PR #52).
    val sizeChosenAtLaunch = parsedSize != null
    buildGameSession(
        initialMode = mode,
        board = BoardImpl(boardSize),
        saves = FileSaveRepository(),
        analytics = analytics,
        sizeChosenAtLaunch = sizeChosenAtLaunch,
        mordie = Mordie(BackgroundMordieSearch.Factory()),
        tileLabels = tileLabels,
        terminalFactory = { AnsiTerminal() },
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

/** Resolves the starting input mode, warns on any unrecognized argument, and tracks the launch -
 *  split out from [main] so it's unit-testable without running the whole game loop. Returns
 *  `null` (and tracks `app_launch_aborted` instead of `app_launched`) when there is no
 *  interactive terminal: the game only runs as a TUI since GH-79. [tileLabels] is the remembered
 *  preference at launch (GH-139). */
fun resolveInputMode(
    args: Array<String>,
    analytics: AnalyticsService,
    boardSize: Int = BoardSize.DEFAULT,
    tileLabels: String = TileLabels.DEFAULT.wireName,
    warnUnrecognizedArg: (String) -> Unit = { System.err.println("Unrecognized argument: '$it' - ignoring.") },
    reportNoInteractiveTerminal: (String) -> Unit = { System.err.println(it) },
    noticeConsoleRemoved: (String) -> Unit = { System.err.println(it) },
    hasInteractiveTerminal: () -> Boolean = InputMode.Companion::systemHasInteractiveTerminal,
): InputMode? {
    val knownArgs = setOf("--console", "--keyboard", "--mouse")
    args.filter { it !in knownArgs && !it.startsWith("--size=") }.forEach(warnUnrecognizedArg)
    val consoleFlag = "--console" in args
    if (!hasInteractiveTerminal()) {
        reportNoInteractiveTerminal("No interactive terminal found - Mordovorot only runs as a terminal UI.")
        val requested = when {
            "--keyboard" in args -> "keyboard"
            "--mouse" in args -> "mouse"
            consoleFlag -> "console"
            else -> "none"
        }
        analytics.track("app_launch_aborted", mapOf("reason" to "no_interactive_terminal", "requested_mode" to requested))
        return null
    }
    if (consoleFlag) noticeConsoleRemoved("Console mode was removed - starting the terminal UI.")
    val mode = InputMode.resolve(args)
    analytics.track(
        "app_launched",
        mapOf(
            "mode" to mode.name.lowercase(), "board_size" to boardSize, "console_flag_ignored" to consoleFlag,
            "tile_labels" to tileLabels,
        ),
    )
    return mode
}
