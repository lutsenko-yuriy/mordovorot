/**
 * Which UI is active - resolved once at startup ([resolve]), switchable mid-session from GH-30
 * on via `GameSession`/`ModeSwitcher`.
 */
enum class InputMode {
    MOUSE,
    KEYBOARD,
    CONSOLE;

    companion object {
        /** Shared default for both [resolve] and `ModeSwitcherImpl`. Caveat: breaks on JDK 22+
         *  - use `System.console()?.isTerminal() == true` there instead; fine while
         *  `jvmToolchain(17)` is pinned. */
        fun systemHasInteractiveTerminal(): Boolean = System.console() != null

        /**
         * `--console` always wins ([resolveInputMode] warns if `--keyboard` was also given).
         * Otherwise `--keyboard` selects [KEYBOARD] and everything else selects [MOUSE], unless
         * [hasInteractiveTerminal] says there's no real terminal - then [CONSOLE].
         */
        fun resolve(
            args: Array<String>,
            hasInteractiveTerminal: () -> Boolean = ::systemHasInteractiveTerminal,
        ): InputMode {
            if ("--console" in args) return CONSOLE
            if (!hasInteractiveTerminal()) return CONSOLE
            return if ("--keyboard" in args) KEYBOARD else MOUSE
        }
    }
}
