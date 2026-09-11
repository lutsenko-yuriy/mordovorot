/**
 * Which UI the app runs at launch (GH-3). Mutually exclusive per session - fixed once at
 * startup, no in-session switching.
 */
enum class LaunchMode {
    MOUSE,
    CONSOLE;

    companion object {
        /**
         * `--console` always selects [CONSOLE]. Otherwise [MOUSE] is the default, unless
         * [hasInteractiveTerminal] says there's no real terminal (e.g. a piped/scripted run)
         * - then it falls back to [CONSOLE] automatically.
         *
         * Caveat: the default `System.console() != null` check breaks on JDK 22+ (it stops
         * meaning "no TTY" - use `System.console()?.isTerminal() == true` there instead).
         * Fine while `jvmToolchain(17)` is pinned in build.gradle.kts; revisit on a bump.
         */
        fun resolve(
            args: Array<String>,
            hasInteractiveTerminal: () -> Boolean = { System.console() != null },
        ): LaunchMode {
            return if (hasInteractiveTerminal() && "--console" !in args) MOUSE else CONSOLE
        }
    }
}
