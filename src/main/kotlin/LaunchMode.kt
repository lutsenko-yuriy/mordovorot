/**
 * Which UI the app runs at launch (GH-3, GH-18). Mutually exclusive per session - fixed once at
 * startup, no in-session switching.
 */
enum class LaunchMode {
    MOUSE,
    KEYBOARD,
    CONSOLE;

    companion object {
        /**
         * `--console` always wins and selects [CONSOLE], even alongside `--keyboard` (the caller
         * is expected to warn that `--keyboard` was ignored - see [resolveLaunchMode]). Otherwise
         * `--keyboard` selects [KEYBOARD] and everything else (no flag, or the explicit `--mouse`)
         * selects [MOUSE] - unless [hasInteractiveTerminal] says there's no real terminal (e.g. a
         * piped/scripted run), which falls both back to [CONSOLE] automatically.
         *
         * Caveat: the default `System.console() != null` check breaks on JDK 22+ (it stops
         * meaning "no TTY" - use `System.console()?.isTerminal() == true` there instead).
         * Fine while `jvmToolchain(17)` is pinned in build.gradle.kts; revisit on a bump.
         */
        fun resolve(
            args: Array<String>,
            hasInteractiveTerminal: () -> Boolean = { System.console() != null },
        ): LaunchMode {
            if ("--console" in args) return CONSOLE
            if (!hasInteractiveTerminal()) return CONSOLE
            return if ("--keyboard" in args) KEYBOARD else MOUSE
        }
    }
}
