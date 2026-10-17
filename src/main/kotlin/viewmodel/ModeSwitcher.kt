package viewmodel

import InputMode
import analytics.AnalyticsService

/**
 * Requests a switch to a different [InputMode] mid-session: tracks `input_mode_switched` and
 * throws [ModeSwitchRequestedException] - mirrors [viewmodel.usecase.ExitGameUseCase.exitGame]'s contract. Reached
 * from `TuiView`'s toolbar button and F7 (GH-30). No terminal check: a running TUI always has
 * one (GH-79 removed the console mode that could ask without one).
 *
 * WU2-4 wiring note: pass the *undecorated* [AnalyticsService] - `input_mode_switched` isn't
 * documented with an `input_method` property (same reasoning as `app_launched`).
 */
interface ModeSwitcher {
    /** [trigger]: `"toolbar"` or `"shortcut"`. */
    suspend fun switchTo(target: InputMode, trigger: String)
}

/** [currentMode] is fixed per instance - `defaultView` builds a fresh one on every rebuild. */
class ModeSwitcherImpl(
    private val currentMode: InputMode,
    private val analytics: AnalyticsService,
) : ModeSwitcher {

    override suspend fun switchTo(target: InputMode, trigger: String) {
        if (target == currentMode) return // already active - no event

        analytics.track(
            "input_mode_switched",
            mapOf(
                "from_mode" to currentMode.name.lowercase(),
                "to_mode" to target.name.lowercase(),
                "trigger" to trigger,
                "result" to "success",
            ),
        )
        throw ModeSwitchRequestedException(target)
    }
}
