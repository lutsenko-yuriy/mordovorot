package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import texts.Language
import viewmodel.SessionLanguage

/** The language picker (GH-275): a pick of another language. */
internal class LanguageUseCase(
    private val board: BoardModel,
    private val language: SessionLanguage,
    private val analytics: AnalyticsService,
    private val solution: SolutionUseCase,
) {

    /** Tracks `language_changed` only when [to] isn't already the language; [control] is `dropdown` or `toggles`. */
    fun change(to: Language, control: String) {
        val from = language.current
        if (to == from) return
        val fromSource = language.source
        language.choose(to)
        analytics.track(
            "language_changed",
            mapOf(
                "from" to from.code, "to" to to.code, "from_source" to fromSource.name.lowercase(),
                "screen" to screenName(board, solution), "control" to control,
            ),
        )
    }
}
