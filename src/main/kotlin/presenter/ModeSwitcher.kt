package presenter

import InputMode
import analytics.AnalyticsService
import analytics.NoopAnalyticsService
import view.View

/**
 * Requests a switch to a different [InputMode] mid-session: checks for an interactive terminal,
 * tracks `input_mode_switched`, and either throws [ModeSwitchRequestedException] or shows a
 * rejection message and returns - mirrors [BasePresenter.exitGame]'s contract. Reached from each
 * `View`'s own affordance: `ViewImpl`'s `mouse`/`keyboard` commands, `TuiView`'s toolbar
 * buttons and F7/F8 (GH-30).
 *
 * WU2-4 wiring note: pass the *undecorated* [AnalyticsService] - `input_mode_switched` isn't
 * documented with an `input_method` property (same reasoning as `app_launched`).
 */
interface ModeSwitcher {
    /** [trigger]: `"toolbar"`, `"shortcut"`, or `"command"`. */
    fun switchTo(target: InputMode, trigger: String)
}

/** [currentMode] is fixed per instance - `GameSession` builds a fresh one on every rebuild. */
class ModeSwitcherImpl(
    private val view: View,
    private val currentMode: InputMode,
    private val analytics: AnalyticsService = NoopAnalyticsService(),
    private val hasInteractiveTerminal: () -> Boolean = InputMode.Companion::systemHasInteractiveTerminal,
) : ModeSwitcher {

    override fun switchTo(target: InputMode, trigger: String) {
        if (target == currentMode) return // already active - no event, no message

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

/** No-op [ModeSwitcher] - default for a `View` with no mode-switch affordance wired in yet. */
class NoopModeSwitcher : ModeSwitcher {
    override fun switchTo(target: InputMode, trigger: String) {}
}
