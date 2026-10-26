package web

import analytics.AnalyticsService
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
    internal val dialogs = WebDialogFlows(viewModel, analytics, modal, ::cancelled)
    private val requests = WebRequestRouter(this)
    private var status: String? = null
    /** The toolbar action or startup restore under way, if any. */
    private var flow: Job? = null
    /** Where focus goes once [flow] ends: back to the button that started it, unless it moves on. */
    private var closing: Focus = Focus.PageStart
    private var drag: LineDrag? = null
    /** What the next frame says (GH-166); [render] hands it on once. */
    private var saying: String? = null
    /** The viewModel asked for a size during [flow], which so ends in a new game. */
    private var sizeAsked = false
    private val idle get() = modal.shown == null && flow?.isActive != true && drag == null
    /** Mordie's solution screen (GH-126): the board is the replay's, not the player's. */
    private val playable get() = idle && !viewModel.isSolved() && viewModel.solution() == null

    /** Answers the viewModel's requests, then runs the startup restore. */
    internal fun start() {
        render()
        scope.launch { for (request in viewModel.uiRequests) requests.handle(request) }
        flow = scope.launch {
            viewModel.restoreOnStartup()
            sayAskedNewGame()
            render(focus = Focus.PageStart)
        }
    }

    /** Backs the page's leave-site prompt. */
    fun hasUnsavedProgress(): Boolean = viewModel.hasUnsavedProgress()

    fun onArrow(arrow: Arrow, line: Int) {
        if (!playable) return
        shift({ Announcements.shift(arrow, line, it) }) {
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
        shift({ Announcements.drag(released, it) }) {
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
                saying = solutionScreen()
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
        saying = solutionScreen()
        render()
    }

    fun onReplayNext() = replay { viewModel.replayNext() }

    fun onReplayPrevious() = replay { viewModel.replayPrevious() }

    fun onReplayRestart() = replay(restart = true) {
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
        dialogs.chooseSize("toolbar")?.let {
            viewModel.newGame(it, trigger = "toolbar")
            saying = Announcements.newGame(it)
        }
    }

    fun onSave() = runFlow(Focus.SaveButton) {
        val name = dialogs.askSaveName()
        when {
            name == null -> {}
            // The terminal's rule too: the name check alone allows spaces.
            name.any { it.isWhitespace() } -> showStatus("'$name' isn't a usable save name (no spaces) - not saved.")
            else -> viewModel.saveGame(name)
        }
    }

    fun onSaveNameTyped(name: String) {
        if (modal.shown !is WebDialog.Save) return
        modal.update(dialogs.saveDialog(name))
        render()
    }

    /** The Save dialog's answer; `null` is Cancel. */
    fun onSaveAnswered(name: String?) = modal.answer<WebDialog.Save, String?>(name)

    fun onLoad() = runFlow(Focus.LoadButton) {
        dialogs.chooseSave(viewModel.listSaves(), restore = false)?.let { viewModel.loadGame(it) }
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

    /** [openedFrom] is `startup` or `toolbar`, as `screen_unsolvable_dialog.opened_from`. */
    internal suspend fun resolveUnsolvable(save: SaveInfo, openedFrom: String): UnsolvableSaveChoice {
        // The viewModel's can't-be-solved message, just shown, goes in the dialog instead.
        val message = status ?: "'${save.displayWithSize()}' can't be solved by any sequence of shifts."
        status = null
        return dialogs.resolveUnsolvable(message, openedFrom)
    }

    /** The size picker the viewModel asks for, at startup or after Can't be solved. */
    internal suspend fun chooseNewGameSize(origin: String): Int? {
        sizeAsked = true
        return dialogs.chooseSize(origin)
    }

    /** A status message, said too; a load's names [loadedSave] and says its size and moves instead. */
    internal fun showStatus(text: String, loadedSave: String? = null) {
        status = text
        saying = loadedSave?.let {
            Announcements.loaded(it, viewModel.squareSide(), viewModel.moveCount(), viewModel.isSolved(), viewModel.solution()?.moves?.size)
        } ?: text
        render()
    }

    /** `dialog_cancelled` for the dialog just closed, with how it was cancelled. */
    private fun cancelled(dialog: String) =
        analytics.track("dialog_cancelled", mapOf("dialog" to dialog, "via" to modal.closedVia.wire))

    /** Runs a shift on the unsolved board and renders, saying it in [words] (given the move count
     *  when the shift started a move), or the win. */
    private fun shift(words: (newMove: Int?) -> String, action: () -> Unit) {
        status = null
        val before = viewModel.moveCount()
        action()
        val moves = viewModel.moveCount()
        // Unsolved before this shift (the callers check), so this is the transition.
        saying = if (viewModel.isSolved()) {
            analytics.track("screen_congratulations")
            Announcements.solved(moves)
        } else words(moves.takeIf { it != before })
        render()
    }

    /** Steps the replay and renders, sliding the shift [action] made and saying the step it's on,
     *  as a [restart] always does. */
    private fun replay(restart: Boolean = false, action: () -> SolutionStep?) {
        if (!idle || viewModel.solution() == null) return
        status = null
        val shifted = action()
        if (shifted != null || restart) saying = viewModel.solution()?.let { Announcements.step(it.moves, it.current) }
        render(shifted?.let(Slides::ofStep))
    }

    private fun solutionScreen() = viewModel.solution()?.let { Announcements.solution(it.moves.size) }

    /** A new game once a flow in which the viewModel asked for its size ends. */
    private fun sayAskedNewGame() {
        if (!sizeAsked) return
        sizeAsked = false
        saying = Announcements.newGame(viewModel.squareSide())
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
            sayAskedNewGame()
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
                announcement = saying,
            ),
        )
        saying = null
    }

    private companion object {
        const val SOLVED_TITLE = "Congratulations ✓"
    }
}
