package web

import analytics.AnalyticsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import viewmodel.GameViewModel
import viewmodel.PointerShift
import viewmodel.SaveInfo
import viewmodel.SessionKeyboardLayout
import viewmodel.ShiftSource
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
    pressedWith: PressedWith,
    keyboardLayout: SessionKeyboardLayout,
    modePreference: PlayModePreference,
    startMode: PlayMode,
    private val controlsShown: Boolean,
) {

    private val modal = WebModal { render(focus = it) }
    internal val labelsDialog = WebLabelsDialog(viewModel, ::cancelled) { modal.ask(it) }
    internal val dialogs = WebDialogFlows(viewModel, analytics, modal, ::cancelled)
    internal val solution = WebSolutionFlows(this, viewModel, analytics, modal)
    internal val controls = WebControlsDialog(this, viewModel, analytics, modal, keyboardLayout, modePreference, startMode)
    private val keys = WebKeyPlay(this, viewModel, pressedWith, controlsShown)
    private val requests = WebRequestRouter(this)
    internal var status: String? = null
    /** The toolbar action or startup restore under way, if any. */
    private var flow: Job? = null
    /** Where focus goes once [flow] ends: back to the button that started it, unless it moves on. */
    internal var closing: Focus = Focus.PageStart
    private var drag: LineDrag? = null
    /** What the next frame says (GH-166); [render] hands it on once. */
    internal var saying: String? = null
    /** The viewModel asked for a size during [flow], which so ends in a new game. */
    private var sizeAsked = false
    internal val idle get() = modal.shown == null && flow?.isActive != true && drag == null
    /** Mordie's solution screen (GH-126): the board is the replay's, not the player's. */
    internal val playable get() = idle && !viewModel.isSolved() && viewModel.solution() == null

    /** Answers the viewModel's requests, then runs the startup restore. */
    internal fun start() {
        render()
        scope.launch { for (request in viewModel.uiRequests) requests.handle(request) }
        flow = scope.launch {
            viewModel.restoreOnStartup()
            sayAskedNewGame()
            render(focus = if (controls.mode == PlayMode.KEYBOARD) Focus.Board else Focus.PageStart)
        }
    }

    /** Backs the page's leave-site prompt. */
    fun hasUnsavedProgress(): Boolean = viewModel.hasUnsavedProgress()

    fun onArrow(arrow: Arrow, line: Int) {
        if (playable) shiftLine(arrow, line, PointerShift.ARROW)
    }

    /** One step of the line [arrow] and [line] name, by [source], sliding it into place. */
    internal fun shiftLine(arrow: Arrow, line: Int, source: ShiftSource) =
        shift({ Announcements.shift(arrow, line, it) }, Slides.ofArrow(arrow, line)) {
            when (arrow) {
                Arrow.LEFT -> viewModel.shiftLeft(line, source)
                Arrow.RIGHT -> viewModel.shiftRight(line, source)
                Arrow.UP -> viewModel.shiftUp(line, source)
                Arrow.DOWN -> viewModel.shiftDown(line, source)
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

    /** A key outside a dialog, with focus on the control keyed [from] (GH-171); whether the game
     *  took it. */
    fun onKey(press: KeyPress, from: String? = null): Boolean = keys.onKey(press, from)

    /** A press on a tile (GH-171): in keyboard mode it selects the tile and gives the board focus. */
    fun onTilePressed(row: Int, col: Int) = keys.onTilePressed(row, col)

    /** `[ Labels ]` (GH-139): the picker, live on play, after a win and on the solution screen. */
    fun onLabels(openedFrom: String = "toolbar", opener: Focus = Focus.LabelsButton) = runFlow(opener) { labelsDialog.fromToolbar(openedFrom) }

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

    fun onNew(opener: Focus = Focus.NewButton) = runFlow(opener) {
        dialogs.chooseSize("toolbar")?.let {
            viewModel.newGame(it, trigger = "toolbar")
            saying = Announcements.newGame(it)
        }
    }

    fun onSave(opener: Focus = Focus.SaveButton) = runFlow(opener) {
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

    fun onLoad(opener: Focus = Focus.LoadButton) = runFlow(opener) {
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
    internal fun cancelled(dialog: String) =
        analytics.track("dialog_cancelled", mapOf("dialog" to dialog, "via" to modal.closedVia.wire))

    /** Runs a shift on the unsolved board and renders, sliding [slide] and saying it in [words] (given
     *  the move count when the shift started a move), or the win. */
    private fun shift(words: (newMove: Int?) -> String, slide: LineOffset? = null, action: () -> Unit) {
        status = null
        val before = viewModel.moveCount()
        action()
        val moves = viewModel.moveCount()
        // Unsolved before this shift (the callers check), so this is the transition.
        saying = if (viewModel.isSolved()) {
            analytics.track("screen_congratulations")
            Announcements.solved(moves)
        } else words(moves.takeIf { it != before })
        render(slide)
    }

    /** A new game once a flow in which the viewModel asked for its size ends. */
    private fun sayAskedNewGame() {
        if (!sizeAsked) return
        sizeAsked = false
        saying = Announcements.newGame(viewModel.squareSide())
    }

    /** Runs a toolbar action, unless another action or a dialog is under way, then renders with
     *  focus back on its [opener]. */
    internal fun runFlow(opener: Focus, action: suspend () -> Unit) {
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

    internal fun render(slide: LineOffset? = null, focus: Focus? = null) {
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
                mode = controls.mode,
                controlsShown = controlsShown,
                selection = keys.shownSelection(),
                hints = KeyHints.board(controls.layout).takeIf { controls.mode == PlayMode.KEYBOARD && viewModel.solution() == null },
            ),
        )
        saying = null
    }

    private companion object {
        const val SOLVED_TITLE = "Congratulations ✓"
    }
}
