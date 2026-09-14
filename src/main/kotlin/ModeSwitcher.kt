import analytics.AnalyticsService
import analytics.NoopAnalyticsService
import view.View

/**
 * Requests a switch to a different [InputMode] mid-session - the single place that checks for
 * an interactive terminal, tracks `input_mode_switched` (docs/ANALYTICS_EVENTS.md), and either
 * throws [ModeSwitchRequestedException] (success) or shows a rejection message and returns
 * normally (no interactive terminal for a mouse/keyboard target), mirroring
 * [presenter.BasePresenter.exitGame]'s "usually throws, returns normally on failure" contract.
 * Reached from each `View`'s own mode-switch affordance: `view.ViewImpl`'s `mouse`/`keyboard`
 * commands, `view.tui.TuiView`'s toolbar buttons and F7/F8 shortcuts (GH-30).
 */
interface ModeSwitcher {
    /** [trigger] is `"toolbar"`, `"shortcut"`, or `"command"` - carried straight through into
     *  `input_mode_switched`'s `trigger` property, unvalidated (the caller owns which value is
     *  correct for its own affordance). */
    fun switchTo(target: InputMode, trigger: String)
}

/**
 * The real [ModeSwitcher]. [currentMode] is fixed for the lifetime of one instance - [GameSession]
 * builds a fresh one (alongside the View/presenter it wires it into) every time it rebuilds for a
 * new mode, so this never needs to track a mode change itself.
 */
class ModeSwitcherImpl(
    private val view: View,
    private val currentMode: InputMode,
    private val analytics: AnalyticsService = NoopAnalyticsService(),
    private val hasInteractiveTerminal: () -> Boolean = { System.console() != null },
) : ModeSwitcher {

    override fun switchTo(target: InputMode, trigger: String) {
        if (target == currentMode) return

        // Console never needs a TTY - only a mouse/keyboard target does (same constraint
        // InputMode.resolve already enforces at launch).
        if (target != InputMode.CONSOLE && !hasInteractiveTerminal()) {
            track(target, trigger, "rejected_no_tty")
            view.showMessage(
                "Can't switch to ${target.name.lowercase()} mode - no interactive terminal available."
            )
            return
        }

        track(target, trigger, "success")
        throw ModeSwitchRequestedException(target)
    }

    private fun track(target: InputMode, trigger: String, result: String) {
        analytics.track(
            "input_mode_switched",
            mapOf(
                "from_mode" to currentMode.name.lowercase(),
                "to_mode" to target.name.lowercase(),
                "trigger" to trigger,
                "result" to result,
            ),
        )
    }
}

/** No-op [ModeSwitcher] - the default for a `View` built with no mode-switch affordance wired
 *  in yet, so adding the [ModeSwitcher] seam to `ViewImpl`/`TuiView` (GH-30 WU2/WU3) doesn't
 *  force every existing call site (and test) to supply one. */
class NoopModeSwitcher : ModeSwitcher {
    override fun switchTo(target: InputMode, trigger: String) {}
}
