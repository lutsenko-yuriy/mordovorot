package web

import analytics.AnalyticsService
import board_model.BoardSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import viewmodel.GameViewModel
import viewmodel.GiveUpButton
import viewmodel.PointerShift
import viewmodel.SaveInfo
import viewmodel.SolutionStep
import viewmodel.TileLabels
import viewmodel.UnsolvableSaveChoice
import viewmodel.display
import viewmodel.displayWithSize
import web.drag.Axis
import web.drag.LineDrag
import web.drag.LineOffset

/** Which arrow was clicked: [LEFT]/[RIGHT] shift a row, [UP]/[DOWN] a column. */
enum class Arrow { LEFT, RIGHT, UP, DOWN }

/** The browser's View (GH-110): takes the page's intents, runs them on [viewModel] and renders a
 *  [WebState] after every change. A dialog is modal: while one is open, or a toolbar action or the
 *  startup restore is under way, only a dialog's answer counts; while a drag is, only the drag.
 *  Tracks the view-layer analytics, like the terminal's dialog controllers. */
class WebGame internal constructor(
    private val viewModel: GameViewModel,
    private val analytics: AnalyticsService,
    private val renderer: WebRenderer,
    private val scope: CoroutineScope,
) {

    private val modal = WebModal { render(focus = it) }
    internal val labelsDialog = WebLabelsDialog(viewModel, ::cancelled) { modal.ask(it) }
    private val requests = WebRequestRouter(this)
    private var status: String? = null
    /** The toolbar action or startup restore under way, if any. */
    private var flow: Job? = null
    /** Where focus goes once [flow] ends: back to the button that started it, unless it moves on. */
    private var closing: Focus = Focus.PageStart
    private var drag: LineDrag? = null
    private val idle get() = modal.shown == null && flow?.isActive != true && drag == null
    /** Mordie's solution screen (GH-126): the board is the replay's, not the player's. */
    private val playable get() = idle && !viewModel.isSolved() && viewModel.solution() == null

    /** Answers the viewModel's requests, then runs the startup restore. */
    internal fun start() {
        render()
        scope.launch { for (request in viewModel.uiRequests) requests.handle(request) }
        flow = scope.launch {
            viewModel.restoreOnStartup()
            render(focus = Focus.PageStart)
        }
    }

    /** Backs the page's leave-site prompt. */
    fun hasUnsavedProgress(): Boolean = viewModel.hasUnsavedProgress()

    fun onArrow(arrow: Arrow, line: Int) {
        if (!playable) return
        shift {
            when (arrow) {
                Arrow.LEFT -> viewModel.shiftLeft(line, PointerShift.ARROW)
                Arrow.RIGHT -> viewModel.shiftRight(line, PointerShift.ARROW)
                Arrow.UP -> viewModel.shiftUp(line, PointerShift.ARROW)
                Arrow.DOWN -> viewModel.shiftDown(line, PointerShift.ARROW)
            }
        }
    }

    /** A press on the tile at [row], [col], at ([x], [y]) on a board with tiles [pitch] apart, in pixels. */
    fun onDragStart(row: Int, col: Int, x: Double, y: Double, pitch: Double) {
        if (!playable) return
        drag = LineDrag(row, col, x, y, pitch, viewModel.squareSide())
    }

    fun onDragMove(x: Double, y: Double) {
        val drag = drag ?: return
        val before = drag.offset
        drag.move(x, y)
        if (drag.offset != before) render()
    }

    /** The release: the line snaps to the nearest tile, and the whole drag is one shift. */
    fun onDragEnd() {
        val released = drag?.release() ?: return onDragCancel()
        drag = null
        shift {
            when (released.axis) {
                Axis.ROW -> viewModel.shiftRow(released.line, released.steps, PointerShift.DRAG)
                Axis.COLUMN -> viewModel.shiftColumn(released.line, released.steps, PointerShift.DRAG)
            }
        }
    }

    /** Drops the drag, leaving the board as it was. */
    fun onDragCancel() {
        val moved = drag?.offset != null
        drag = null
        if (moved) render()
    }

    /** `[ Give Up ]`: the confirmation, then Mordie's solution screen on Yes. */
    fun onGiveUp() {
        if (viewModel.giveUpButton() != GiveUpButton.GIVE_UP) return
        runFlow(Focus.GiveUpRow) {
            analytics.track("screen_give_up_dialog", mapOf("board_size" to viewModel.squareSide(), "move_count" to viewModel.moveCount()))
            if (modal.ask(WebDialog.GiveUp)) {
                viewModel.giveUp()
                // The solution screen has no Give Up.
                closing = Focus.ReplayNext
            } else cancelled("give_up")
        }
    }

    /** The give-up confirmation's answer: Yes or No. */
    fun onGiveUpAnswered(yes: Boolean) = modal.answer<WebDialog.GiveUp, Boolean>(yes)

    /** `[ Mordie's solution ]` after a win: the screen, with no confirmation. */
    fun onMordiesSolution() {
        if (!idle) return
        status = null
        viewModel.showSolution()
        render()
    }

    fun onReplayNext() = replay { viewModel.replayNext() }

    fun onReplayPrevious() = replay { viewModel.replayPrevious() }

    fun onReplayRestart() = replay {
        viewModel.replayRestart()
        null
    }

    /** `[ Labels ]` (GH-139): the picker, live on play, after a win and on the solution screen. */
    fun onLabels() = runFlow(Focus.LabelsButton) { labelsDialog.fromToolbar() }

    /** A Labels row pressed: it becomes the marked one, and the board previews it. */
    fun onLabelsSelected(labels: TileLabels) {
        val shown = modal.shown as? WebDialog.Labels ?: return
        modal.update(labelsDialog.select(shown, labels) ?: return)
        render(focus = Focus.Label(labels))
    }

    /** The Labels dialog's answer: OK with the marked scheme, or Cancel. */
    fun onLabelsAnswered(ok: Boolean) {
        val shown = modal.shown as? WebDialog.Labels ?: return
        modal.answer<WebDialog.Labels, TileLabels?>(if (ok) shown.selected else null)
    }

    fun onNew() = runFlow(Focus.NewButton) {
        chooseSize("toolbar")?.let { viewModel.newGame(it, trigger = "toolbar") }
    }

    fun onSave() = runFlow(Focus.SaveButton) {
        val name = askSaveName()
        when {
            name == null -> {}
            // The terminal's rule too: the name check alone allows spaces.
            name.any { it.isWhitespace() } -> status = "'$name' isn't a usable save name (no spaces) - not saved."
            else -> viewModel.saveGame(name)
        }
    }

    fun onSaveNameTyped(name: String) {
        if (modal.shown !is WebDialog.Save) return
        modal.update(saveDialog(name))
        render()
    }

    /** The Save dialog's answer; `null` is Cancel. */
    fun onSaveAnswered(name: String?) = modal.answer<WebDialog.Save, String?>(name)

    fun onLoad() = runFlow(Focus.LoadButton) {
        chooseSave(viewModel.listSaves(), restore = false)?.let { viewModel.loadGame(it) }
    }

    /** The Load dialog's answer, a listed save's name; `null` is Cancel. */
    fun onLoadChosen(name: String?) = modal.answer<WebDialog.Load, String?>(name)

    /** The unsolvable-save dialog's answer. The page has no Exit, so [UnsolvableSaveChoice.EXIT] is ignored. */
    fun onUnsolvableChoice(choice: UnsolvableSaveChoice) {
        if (choice != UnsolvableSaveChoice.EXIT) modal.answer<WebDialog.Unsolvable, UnsolvableSaveChoice>(choice)
    }

    /** The size picker's answer; `null` is Cancel. */
    fun onSizeChosen(size: Int?) = modal.answer<WebDialog.Size, Int?>(size)

    /** Escape (GH-163): the open dialog's Cancel, or No. */
    fun onEscape() = modal.escape()

    /** [openedFrom] is `toolbar` or a `ChooseBoardSize.origin`, as `screen_size_dialog.opened_from`. */
    internal suspend fun chooseSize(openedFrom: String): Int? {
        analytics.track("screen_size_dialog", mapOf("opened_from" to openedFrom))
        val size = modal.ask<Int?>(WebDialog.Size((BoardSize.MIN..BoardSize.MAX).toList(), viewModel.squareSide()))
        if (size == null) cancelled("size")
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
        return modal.ask(WebDialog.Unsolvable(message))
    }

    internal fun showStatus(text: String) {
        status = text
        render()
    }

    private suspend fun askSaveName(): String? {
        val prefill = viewModel.currentSaveName() ?: ""
        analytics.track("screen_save_dialog", mapOf("opened_from" to "toolbar", "prefilled" to prefill.isNotEmpty()))
        val name = modal.ask<String?>(saveDialog(prefill))
        if (name == null) cancelled("save")
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
        val name = modal.ask<String?>(WebDialog.Load(restore, saves.map { SaveEntry(it.name, it.display()) }))
        if (name == null) cancelled("load")
        return name
    }

    /** `dialog_cancelled` for the dialog just closed, with how it was cancelled. */
    private fun cancelled(dialog: String) =
        analytics.track("dialog_cancelled", mapOf("dialog" to dialog, "via" to modal.closedVia.wire))

    /** Runs a shift on the unsolved board and renders. */
    private fun shift(action: () -> Unit) {
        status = null
        action()
        // Unsolved before this shift (the callers check), so this is the transition.
        if (viewModel.isSolved()) analytics.track("screen_congratulations")
        render()
    }

    /** Steps the replay and renders, sliding the shift [action] made. */
    private fun replay(action: () -> SolutionStep?) {
        if (!idle || viewModel.solution() == null) return
        status = null
        render(action()?.let(Slides::ofStep))
    }

    /** Runs a toolbar action, unless another action or a dialog is under way, then renders with
     *  focus back on its [opener]. */
    private fun runFlow(opener: Focus, action: suspend () -> Unit) {
        if (!idle) return
        status = null
        closing = opener
        // Undispatched, so the action is under way before this returns and a second click sees it.
        flow = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            action()
            render(focus = closing)
        }
    }

    private fun render(slide: LineOffset? = null, focus: Focus? = null) {
        val solved = viewModel.isSolved()
        renderer.render(
            WebState(
                side = viewModel.squareSide(),
                tiles = viewModel.boardState().map { it + 1 },
                moves = viewModel.moveCount(),
                mordie = if (solved) viewModel.mordieEnding() ?: SOLVED_TITLE else viewModel.mordieLine(),
                solved = solved,
                status = status,
                dialog = modal.shown,
                drag = drag?.offset,
                giveUp = viewModel.giveUpButton(),
                solution = viewModel.solution(),
                slide = slide,
                // The Labels dialog's marked scheme, previewed until OK keeps it or Cancel drops it.
                labels = (modal.shown as? WebDialog.Labels)?.selected ?: viewModel.tileLabels(),
                focus = focus,
            ),
        )
    }

    private companion object {
        const val SOLVED_TITLE = "Congratulations ✓"
    }
}
