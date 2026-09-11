/**
 * Which UI the app runs at launch (GH-3). Mutually exclusive per session - fixed once at
 * startup, no in-session switching.
 */
enum class LaunchMode {
    MOUSE,
    CONSOLE;

    companion object {
        /**
         * Resolves the launch mode from CLI arguments. `--console` always selects [CONSOLE].
         * Otherwise [MOUSE] is the default, unless [hasInteractiveTerminal] reports there is
         * no real terminal to draw the mouse UI on (e.g. stdin is a pipe or redirected file,
         * such as a scripted/piped run) - in that case it falls back to [CONSOLE]
         * automatically rather than failing.
         *
         * The default [hasInteractiveTerminal] (`System.console() != null`) is only a valid
         * "no TTY" test through JDK 21 - build.gradle.kts pins `jvmToolchain(17)`, so it holds
         * today. Since JDK 22 (JDK-8295803), `System.console()` returns a non-null `Console`
         * even when stdin/stdout are redirected; the replacement check there is
         * `System.console()?.isTerminal() == true`. Revisit this default before bumping the
         * toolchain past 21, or every piped/CI run would silently resolve to MOUSE (audit on
         * PR #20).
         */
        fun resolve(
            args: Array<String>,
            hasInteractiveTerminal: () -> Boolean = { System.console() != null },
        ): LaunchMode {
            if ("--console" in args) return CONSOLE
            return if (hasInteractiveTerminal()) MOUSE else CONSOLE
        }
    }
}
