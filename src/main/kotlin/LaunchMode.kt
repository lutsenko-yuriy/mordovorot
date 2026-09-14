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
         * `--console` always wins ([resolveLaunchMode] warns if `--keyboard` was also given).
         * Otherwise `--keyboard` selects [KEYBOARD] and everything else selects [MOUSE], unless
         * [hasInteractiveTerminal] says there's no real terminal - then [CONSOLE].
         *
         * Caveat: the default `System.console() != null` check breaks on JDK 22+ - use
         * `System.console()?.isTerminal() == true` there instead. Fine while `jvmToolchain(17)`
         * is pinned; revisit on a bump.
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
