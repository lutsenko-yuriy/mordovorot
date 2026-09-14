/**
 * Which UI is active - resolved once at startup ([resolve]), and switchable mid-session from
 * GH-30 on. [GameSession] tracks the current one and rebuilds the `View`/presenter/analytics
 * stack around it every time [ModeSwitcher] throws [ModeSwitchRequestedException].
 */
enum class InputMode {
    MOUSE,
    KEYBOARD,
    CONSOLE;

    companion object {
        /**
         * `--console` always wins ([resolveInputMode] warns if `--keyboard` was also given).
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
        ): InputMode {
            if ("--console" in args) return CONSOLE
            if (!hasInteractiveTerminal()) return CONSOLE
            return if ("--keyboard" in args) KEYBOARD else MOUSE
        }
    }
}
