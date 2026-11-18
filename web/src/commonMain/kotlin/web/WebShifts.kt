package web

import analytics.AnalyticsService
import viewmodel.GameViewModel
import viewmodel.ShiftSource
import web.a11y.Announcements
import web.drag.Axis
import web.drag.LineOffset

/** [WebGame]'s shifts on the unsolved board (GH-110): one step of a line by arrow or key, and any shift the page renders and says. */
internal class WebShifts(private val game: WebGame, private val viewModel: GameViewModel, private val analytics: AnalyticsService) {

    /** One step of the line [arrow] and [line] name, by [source], sliding it into place. */
    fun line(arrow: Arrow, line: Int, source: ShiftSource, focus: Focus? = null) {
        val row = arrow == Arrow.LEFT || arrow == Arrow.RIGHT
        val forward = arrow == Arrow.RIGHT || arrow == Arrow.DOWN
        if (!game.tutorial.take(if (row) Axis.ROW else Axis.COLUMN, line, if (forward) 1 else -1, source)) return
        shift({ Announcements.shift(arrow.direction, line, it) }, Slides.ofArrow(arrow, line), focus) {
            when (arrow) {
                Arrow.LEFT -> viewModel.shiftLeft(line, source)
                Arrow.RIGHT -> viewModel.shiftRight(line, source)
                Arrow.UP -> viewModel.shiftUp(line, source)
                Arrow.DOWN -> viewModel.shiftDown(line, source)
            }
        }
        game.tutorial.afterShift()
    }

    /** Runs a shift on the unsolved board and renders, sliding [slide] and saying it in [words] (given
     *  the move count when the shift started a move), or the win. */
    fun shift(words: (newMove: Int?) -> String, slide: LineOffset? = null, focus: Focus? = null, action: () -> Unit) {
        game.status = null
        val before = viewModel.moveCount()
        action()
        val moves = viewModel.moveCount()
        // Unsolved before this shift (the callers check), so this is the transition.
        game.saying = if (viewModel.isSolved()) {
            analytics.track("screen_congratulations")
            game.updates.check(CheckTrigger.GAME_END)
            Announcements.solved(moves)
        } else game.keys.withTileNow(words(moves.takeIf { it != before }))
        game.render(slide, focus.takeUnless { viewModel.isSolved() })
    }
}
