package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel

/** The Controls dialog (GH-170): its screen view. Switching mode is the platform's own business. */
internal class ControlsDialogUseCase(
    private val board: BoardModel,
    private val analytics: AnalyticsService,
    private val solution: SolutionUseCase,
) {

    /** [openedFrom] is `toolbar` or `shortcut`. */
    fun dialogOpened(openedFrom: String) =
        analytics.track("screen_controls_dialog", mapOf("opened_from" to openedFrom, "screen" to screenName(board, solution)))
}
