package web

import analytics.AnalyticsService
import board_model.BoardSize
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import viewmodel.GameViewModel
import viewmodel.PointerShift

/** Which arrow was clicked: [LEFT]/[RIGHT] shift a row, [UP]/[DOWN] a column. */
enum class Arrow { LEFT, RIGHT, UP, DOWN }

/** The browser's View (GH-110): takes the page's intents, runs them on [viewModel] and renders a
 *  [WebState] after every change. A dialog is modal: while one is open, only its answer counts.
 *  Tracks the view-layer analytics, like the terminal's dialog controllers. */
class WebGame internal constructor(
    private val viewModel: GameViewModel,
    private val analytics: AnalyticsService,
    private val renderer: WebRenderer,
    private val scope: CoroutineScope,
) {

    private class OpenDialog<T>(val shown: WebDialog) {
        val answer = CompletableDeferred<T>()
    }

    private val requests = WebRequestRouter(this)
    private var dialog: OpenDialog<*>? = null
    private var status: String? = null

    /** Answers the viewModel's requests, then runs the startup restore. */
    internal fun start() {
        render()
        scope.launch { for (request in viewModel.uiRequests) requests.handle(request) }
        scope.launch {
            viewModel.restoreOnStartup()
            render()
        }
    }

    fun onArrow(arrow: Arrow, line: Int) {
        if (dialog != null || viewModel.isSolved()) return
        status = null
        when (arrow) {
            Arrow.LEFT -> viewModel.shiftLeft(line, PointerShift.ARROW)
            Arrow.RIGHT -> viewModel.shiftRight(line, PointerShift.ARROW)
            Arrow.UP -> viewModel.shiftUp(line, PointerShift.ARROW)
            Arrow.DOWN -> viewModel.shiftDown(line, PointerShift.ARROW)
        }
        // Unsolved before this shift (see the guard above), so this is the transition.
        if (viewModel.isSolved()) analytics.track("screen_congratulations")
        render()
    }

    fun onNew() {
        if (dialog != null) return
        status = null
        // Undispatched, so the dialog is open before this returns and a second click sees it.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            chooseSize("toolbar")?.let {
                viewModel.newGame(it, trigger = "toolbar")
                render()
            }
        }
    }

    /** The size picker's answer; `null` is Cancel. */
    fun onSizeChosen(size: Int?) = answer<WebDialog.Size, Int?>(size)

    /** [openedFrom] is `toolbar` or a `ChooseBoardSize.origin`, as `screen_size_dialog.opened_from`. */
    internal suspend fun chooseSize(openedFrom: String): Int? {
        analytics.track("screen_size_dialog", mapOf("opened_from" to openedFrom))
        val size = ask<Int?>(WebDialog.Size((BoardSize.MIN..BoardSize.MAX).toList()))
        if (size == null) analytics.track("dialog_cancelled", mapOf("dialog" to "size"))
        return size
    }

    internal fun showStatus(text: String) {
        status = text
        render()
    }

    private suspend fun <T> ask(shown: WebDialog): T {
        val open = OpenDialog<T>(shown)
        dialog = open
        render()
        try {
            return open.answer.await()
        } finally {
            dialog = null
            render()
        }
    }

    /** Completes the open dialog when it is a [D]; any other answer is stale and ignored. */
    private inline fun <reified D : WebDialog, T> answer(value: T) {
        val open = dialog?.takeIf { it.shown is D } ?: return
        @Suppress("UNCHECKED_CAST")
        (open as OpenDialog<T>).answer.complete(value)
    }

    private fun render() {
        val solved = viewModel.isSolved()
        renderer.render(
            WebState(
                side = viewModel.squareSide(),
                tiles = viewModel.boardState().map { it + 1 },
                moves = viewModel.moveCount(),
                mordie = if (solved) viewModel.mordieEnding() ?: SOLVED_TITLE else viewModel.mordieLine(),
                solved = solved,
                status = status,
                dialog = dialog?.shown,
            ),
        )
    }

    private companion object {
        const val SOLVED_TITLE = "Congratulations ✓"
    }
}
