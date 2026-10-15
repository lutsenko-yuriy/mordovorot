/**
 * Which UI is active - resolved once at startup ([resolve]), switchable mid-session from GH-30
 * on via `GameSession`/`ModeSwitcher`.
 */
enum class InputMode {
    MOUSE,
    KEYBOARD;

    companion object {
        /** Default terminal check for [resolveInputMode]. Caveat: breaks on JDK 22+
         *  - use `System.console()?.isTerminal() == true` there instead; fine while
         *  `jvmToolchain(17)` is pinned. */
        fun systemHasInteractiveTerminal(): Boolean = System.console() != null

        /** Pure flag parsing: `--keyboard` selects [KEYBOARD], everything else [MOUSE].
         *  `--console` selects nothing since GH-79; the terminal check lives in
         *  [resolveInputMode]. */
        fun resolve(args: Array<String>): InputMode = if ("--keyboard" in args) KEYBOARD else MOUSE
    }
}
