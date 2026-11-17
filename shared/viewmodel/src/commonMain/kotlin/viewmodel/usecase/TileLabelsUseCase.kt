package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import viewmodel.SessionTileLabels
import viewmodel.TileLabels

/** The Tile labels dialog (GH-139): its screen view, and OK with a new scheme. */
internal class TileLabelsUseCase(
    private val board: BoardModel,
    private val labels: SessionTileLabels,
    private val analytics: AnalyticsService,
    private val solution: SolutionUseCase,
) {

    /** [openedFrom] is `toolbar`, `shortcut` or `letters_save` (GH-160). */
    fun dialogOpened(openedFrom: String) = analytics.track(
        "screen_labels_dialog",
        mapOf("opened_from" to openedFrom, "screen" to screenName(board, solution), "current_labels" to labels.current.wireName),
    )

    /** The alphabet hint's screen view (GH-332); [openedFrom] is `button` or `shortcut`. `labels` is the scheme shown, so Numbers on a
     *  board too big for Letters. */
    fun alphabetHintOpened(openedFrom: String) = analytics.track(
        "screen_alphabet_hint_dialog",
        mapOf(
            "opened_from" to openedFrom, "screen" to screenName(board, solution),
            "labels" to labels.current.wireName, "board_size" to board.squareSide,
        ),
    )

    /** Tracks `tile_labels_changed` only when [to] isn't already shown. */
    fun change(to: TileLabels) {
        val from = labels.current
        val fromSource = labels.source
        if (!labels.choose(to)) return
        analytics.track(
            "tile_labels_changed",
            mapOf(
                "from" to from.wireName, "to" to to.wireName, "from_source" to fromSource.name.lowercase(),
                "screen" to screenName(board, solution), "board_size" to board.squareSide,
            ),
        )
    }
}
