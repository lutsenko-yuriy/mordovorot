package web

import analytics.AnalyticsService
import board_model.BoardSize
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import viewmodel.GameViewModel
import viewmodel.PointerShift
import viewmodel.SaveInfo
import viewmodel.UnsolvableSaveChoice
import viewmodel.display
import viewmodel.displayWithSize

/** Which arrow was clicked: [LEFT]/[RIGHT] shift a row, [UP]/[DOWN] a column. */
enum class Arrow { LEFT, RIGHT, UP, DOWN }

/** The browser's View (GH-110): takes the page's intents, runs them on [viewModel] and renders a
 *  [WebState] after every change. A dialog is modal: while one is open, or a toolbar action or the
 *  startup restore is under way, only a dialog's answer counts. Tracks the view-layer analytics,
 *  like the terminal's dialog controllers. */
class WebGame internal constructor(
    private val viewModel: GameViewModel,
    private val analytics: AnalyticsService,
    private val renderer: WebRenderer,
    private val scope: CoroutineScope,
) {

    private class OpenDialog<T>(var shown: WebDialog) {
        val answer = CompletableDeferred<T>()
    }

    private val requests = WebRequestRouter(this)
    private var dialog: OpenDialog<*>? = null
    private var status: String? = null
    /** The toolbar action or startup restore under way, if any. */
    private var flow: Job? = null
    private val idle get() = dialog == null && flow?.isActive != true

    /** Answers the viewModel's requests, then runs the startup restore. */
    internal fun start() {
        render()
        scope.launch { for (request in viewModel.uiRequests) requests.handle(request) }
        flow = scope.launch {
            viewModel.restoreOnStartup()
            render()
        }
    }

    /** Backs the page's leave-site prompt. */
    fun hasUnsavedProgress(): Boolean = viewModel.hasUnsavedProgress()

    fun onArrow(arrow: Arrow, line: Int) {
        if (!idle || viewModel.isSolved()) return
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

    fun onNew() = runFlow {
        chooseSize("toolbar")?.let { viewModel.newGame(it, trigger = "toolbar") }
    }

    fun onSave() = runFlow {
        val name = askSaveName()
        when {
            name == null -> {}
            // The terminal's rule too: the name check alone allows spaces.
            name.any { it.isWhitespace() } -> status = "'$name' isn't a usable save name (no spaces) - not saved."
            else -> viewModel.saveGame(name)
        }
    }

    fun onSaveNameTyped(name: String) {
        val open = dialog?.takeIf { it.shown is WebDialog.Save } ?: return
        open.shown = saveDialog(name)
        render()
    }

    /** The Save dialog's answer; `null` is Cancel. */
    fun onSaveAnswered(name: String?) = answer<WebDialog.Save, String?>(name)

    fun onLoad() = runFlow {
        chooseSave(viewModel.listSaves(), restore = false)?.let { viewModel.loadGame(it) }
    }

    /** The Load dialog's answer, a listed save's name; `null` is Cancel. */
    fun onLoadChosen(name: String?) = answer<WebDialog.Load, String?>(name)

    /** The unsolvable-save dialog's answer. The page has no Exit, so [UnsolvableSaveChoice.EXIT] is ignored. */
    fun onUnsolvableChoice(choice: UnsolvableSaveChoice) {
        if (choice != UnsolvableSaveChoice.EXIT) answer<WebDialog.Unsolvable, UnsolvableSaveChoice>(choice)
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

    /** The startup restore offer, for one save or a pick list; the chosen save's name or `null`. */
    internal suspend fun chooseRestore(saves: List<SaveInfo>): String? = chooseSave(saves, restore = true)

    /** [openedFrom] is `startup` or `toolbar`, as `screen_unsolvable_dialog.opened_from`. */
    internal suspend fun resolveUnsolvable(save: SaveInfo, openedFrom: String): UnsolvableSaveChoice {
        analytics.track("screen_unsolvable_dialog", mapOf("opened_from" to openedFrom))
        // The viewModel's can't-be-solved message, just shown, goes in the dialog instead.
        val message = status ?: "'${save.displayWithSize()}' can't be solved by any sequence of shifts."
        status = null
        return ask(WebDialog.Unsolvable(message))
    }

    internal fun showStatus(text: String) {
        status = text
        render()
    }

    private suspend fun askSaveName(): String? {
        val prefill = viewModel.currentSaveName() ?: ""
        analytics.track("screen_save_dialog", mapOf("opened_from" to "toolbar", "prefilled" to prefill.isNotEmpty()))
        val name = ask<String?>(saveDialog(prefill))
        if (name == null) analytics.track("dialog_cancelled", mapOf("dialog" to "save"))
        // A blank name is Cancel, untracked like the terminal's empty Save.
        return name?.takeUnless { it.isBlank() }
    }

    private fun saveDialog(name: String) = WebDialog.Save(
        name,
        if (name.isNotEmpty() && viewModel.saveExists(name)) "'$name' already exists - it will be overwritten." else null,
    )

    private suspend fun chooseSave(saves: List<SaveInfo>, restore: Boolean): String? {
        val openedFrom = if (restore) "startup" else "toolbar"
        analytics.track("screen_load_dialog", mapOf("opened_from" to openedFrom, "save_file_count" to saves.size))
        val name = ask<String?>(WebDialog.Load(restore, saves.map { SaveEntry(it.name, it.display()) }))
        if (name == null) analytics.track("dialog_cancelled", mapOf("dialog" to "load"))
        return name
    }

    /** Runs a toolbar action, unless another action or a dialog is under way, then renders. */
    private fun runFlow(action: suspend () -> Unit) {
        if (!idle) return
        status = null
        // Undispatched, so the action is under way before this returns and a second click sees it.
        flow = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            action()
            render()
        }
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
