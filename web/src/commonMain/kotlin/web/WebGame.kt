package web

import analytics.AnalyticsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import viewmodel.GameViewModel
import viewmodel.GiveUpButton
import viewmodel.PointerShift
import viewmodel.SessionKeyboardLayout
import viewmodel.ShiftSource
import viewmodel.UnsolvableSaveChoice
import web.a11y.Announcements
import web.drag.Axis
import web.drag.LineOffset

/** The browser's View (GH-110): takes the page's intents, runs them on [viewModel] and renders a
 *  [WebState] after every change. A dialog is modal: while one is open, or a toolbar action or the
 *  startup restore is under way, only a dialog's answer counts; while a drag is, only the drag.
 *  Tracks the view-layer analytics. */
class WebGame internal constructor(
    private val viewModel: GameViewModel,
    private val analytics: AnalyticsService,
    private val renderer: WebRenderer,
    private val scope: CoroutineScope,
    pressedWith: PressedWith,
    keyboardLayout: SessionKeyboardLayout,
    private val computer: Boolean,
    address: PageAddress,
    clipboard: Clipboard,
    updates: AppUpdates,
    appVersion: String,
    shareLabels: ShareTogglePreference,
    shareProgress: ShareTogglePreference,
    tutorialSeen: TutorialPreference,
    tutorialOn: Boolean,
) {

    private val modal = WebModal { render(focus = it) }
    internal val labels = WebLabelsDialog(this, viewModel, modal)
    internal val dialogs = WebDialogFlows(viewModel, analytics, modal, ::cancelled, ::noteStatus, ::takeStatus) { saying = it }
    internal val solution = WebSolutionFlows(this, viewModel, analytics, modal)
    internal val controls = WebControlsDialog(this, viewModel, modal, keyboardLayout)
    internal val share = WebShareFlows(this, viewModel, analytics, modal, address, clipboard, shareLabels, shareProgress)
    internal val updates = WebUpdateFlows(this, viewModel, analytics, modal, updates, appVersion)
    internal val tutorial = WebTutorial(this, viewModel, analytics, tutorialSeen, scope, computer, tutorialOn)
    private val keys = WebKeyPlay(this, viewModel, pressedWith, computer)
    private val requests = WebRequestRouter(this)
    private val dragging = WebDragFlows(this, viewModel)
    internal var status: String? = null
    /** The toolbar action or startup restore under way, if any. */
    private var flow: Job? = null
    /** Where focus goes once [flow] ends: back to the button that started it, unless it moves on. */
    internal var closing: Focus = Focus.PageStart
    /** What the next frame says (GH-166); [render] hands it on once. */
    internal var saying: String? = null
    internal val idle get() = modal.shown == null && flow?.isActive != true && !dragging.active
    /** Mordie's solution screen (GH-126): the board is the replay's, not the player's. */
    internal val playable get() = idle && !viewModel.isSolved() && viewModel.solution() == null

    /** Answers the viewModel's requests, then starts the tutorial on a first launch, else runs the startup restore. */
    internal fun start() {
        updates.start()
        render()
        scope.launch { for (request in viewModel.uiRequests) requests.handle(request) }
        flow = scope.launch {
            if (tutorial.shouldStart()) tutorial.begin() else startup()
            share.openPending()
            dialogs.sayAskedNewGame()
            render(focus = onBoard ?: Focus.PageStart)
        }
    }

    /** A share link's board replaces the restore offer. After the tutorial's Skip, a link that isn't opened (Cancel) keeps the tutorial's board too. */
    internal suspend fun startup(afterSkip: Boolean = false) {
        val linked = afterSkip && share.hasPlayableStartupLink()
        if (!share.openAtStartup() && !linked) viewModel.restoreOnStartup()
    }

    /** Where a computer's board has focus, the keys working there (GH-171); none on touch. */
    internal val onBoard get() = Focus.Board.takeIf { computer }

    /** Backs the page's leave-site prompt. */
    fun hasUnsavedProgress(): Boolean = !updates.applying && !tutorial.running && viewModel.hasUnsavedProgress()

    /** A click on an arrow; on a computer a mouse's puts focus on the board, so its keys work next (GH-287). */
    fun onArrow(arrow: Arrow, line: Int, byMouse: Boolean = false) {
        if (!playable) return
        keys.onArrowPressed()
        shiftLine(arrow, line, PointerShift.ARROW, focus = onBoard.takeIf { byMouse })
    }

    /** One step of the line [arrow] and [line] name, by [source], sliding it into place. */
    internal fun shiftLine(arrow: Arrow, line: Int, source: ShiftSource, focus: Focus? = null) {
        val row = arrow == Arrow.LEFT || arrow == Arrow.RIGHT
        val forward = arrow == Arrow.RIGHT || arrow == Arrow.DOWN
        if (!tutorial.take(if (row) Axis.ROW else Axis.COLUMN, line, if (forward) 1 else -1)) return
        shift({ Announcements.shift(arrow.direction, line, it) }, Slides.ofArrow(arrow, line), focus) {
            when (arrow) {
                Arrow.LEFT -> viewModel.shiftLeft(line, source)
                Arrow.RIGHT -> viewModel.shiftRight(line, source)
                Arrow.UP -> viewModel.shiftUp(line, source)
                Arrow.DOWN -> viewModel.shiftDown(line, source)
            }
        }
        tutorial.afterShift()
    }

    fun onDragStart(row: Int, col: Int, x: Double, y: Double, pitch: Double) = dragging.onStart(row, col, x, y, pitch)

    fun onDragMove(x: Double, y: Double) = dragging.onMove(x, y)

    fun onDragEnd() = dragging.onEnd()

    fun onDragCancel() = dragging.onCancel()

    /** A key outside a dialog, with focus on the control keyed [from] (GH-171); whether the game
     *  took it. */
    fun onKey(press: KeyPress, from: String? = null): Boolean = keys.onKey(press, from)

    /** A press on a tile (GH-171): on a computer it selects the tile and gives the board focus. */
    fun onTilePressed(row: Int, col: Int) = keys.onTilePressed(row, col)

    fun onNew(opener: Focus = Focus.NewButton) = runFlow(opener) {
        dialogs.chooseSize("toolbar")?.let {
            viewModel.newGame(it, trigger = "toolbar")
            saying = Announcements.newGame(it)
            closing = onBoard ?: closing
        }
    }

    fun onSave(opener: Focus = Focus.SaveButton) = runFlow(opener) {
        val name = dialogs.askSaveName()
        when {
            name == null -> {}
            // The name check alone allows spaces.
            name.any { it.isWhitespace() } -> showStatus("'$name' isn't a usable save name (no spaces) - not saved.")
            else -> if (viewModel.saveGame(name)) closing = onBoard ?: closing
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
        dialogs.chooseSave(viewModel.listSaves(), restore = false)?.let {
            // A repaired or loaded save, or Can't be solved's new game, is a different game: the keys work on the board (GH-277).
            if (viewModel.loadGame(it) || dialogs.newGameAsked) closing = onBoard ?: closing
        }
    }

    /** The Load dialog's answer, a listed save's name; `null` is Cancel. */
    fun onLoadChosen(name: String?) = modal.answer<WebDialog.Load, LoadPick>(name?.let { LoadPick.Chosen(it) } ?: LoadPick.Cancel)

    /** The trash button of the Load dialog's row at [index] (GH-251). */
    fun onDeletePressed(index: Int) = modal.answer<WebDialog.Load, LoadPick>(LoadPick.Delete(index))

    /** The Delete confirmation's answer: `true` is Delete (GH-251). */
    fun onDeleteAnswered(confirm: Boolean) = modal.answer<WebDialog.DeleteSave, Boolean>(confirm)

    /** The unsolvable-save dialog's answer. */
    fun onUnsolvableChoice(choice: UnsolvableSaveChoice) = modal.answer<WebDialog.Unsolvable, UnsolvableSaveChoice>(choice)

    /** The size picker's answer; `null` is Cancel. */
    fun onSizeChosen(size: Int?) = modal.answer<WebDialog.Size, Int?>(size)

    /** Escape (GH-163): the open dialog's Cancel, or No. */
    fun onEscape() = modal.escape()

    /** The status message just shown, taken: a dialog shows it instead. */
    private fun takeStatus(): String? = status.also { status = null }

    /** A status message, said too; a load's names [loadedSave] and says its size and moves instead. */
    internal fun showStatus(text: String, loadedSave: String? = null) {
        noteStatus(text, loadedSave)
        render()
    }

    /** [showStatus] without drawing: the next frame shows it (GH-251, so a dialog that follows doesn't blink). */
    internal fun noteStatus(text: String, loadedSave: String? = null) {
        status = text
        saying = loadedSave?.let {
            Announcements.loaded(it, viewModel.squareSide(), viewModel.moveCount(), viewModel.isSolved(), viewModel.solution()?.moves?.size)
        } ?: text
    }

    /** `dialog_cancelled` for the dialog just closed, with how it was cancelled. */
    internal fun cancelled(dialog: String) =
        analytics.track("dialog_cancelled", mapOf("dialog" to dialog, "via" to modal.closedVia.wire))

    /** Runs a shift on the unsolved board and renders, sliding [slide] and saying it in [words] (given
     *  the move count when the shift started a move), or the win. */
    internal fun shift(words: (newMove: Int?) -> String, slide: LineOffset? = null, focus: Focus? = null, action: () -> Unit) {
        status = null
        val before = viewModel.moveCount()
        action()
        val moves = viewModel.moveCount()
        // Unsolved before this shift (the callers check), so this is the transition.
        saying = if (viewModel.isSolved()) {
            analytics.track("screen_congratulations")
            updates.check(CheckTrigger.GAME_END)
            Announcements.solved(moves)
        } else keys.withTileNow(words(moves.takeIf { it != before }))
        render(slide, focus.takeUnless { viewModel.isSolved() })
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
            share.openPending()
            dialogs.sayAskedNewGame()
            render(focus = closing)
        }
    }

    /** 0 (GH-171): as the Read board button. */
    internal fun readBoard() {
        saying = frame(null, null).boardWords
        render()
    }

    internal fun scrollSteps(lines: Int) = renderer.scrollSteps(lines)

    internal fun render(slide: LineOffset? = null, focus: Focus? = null) {
        renderer.render(frame(slide, focus))
        saying = null
    }

    private fun frame(slide: LineOffset?, focus: Focus?): WebState {
        val solved = viewModel.isSolved()
        val guidance = tutorial.guidance()
        return WebState(
            side = viewModel.squareSide(),
            tiles = viewModel.boardState().map { it + 1 },
            moves = viewModel.moveCount(),
            mordie = when {
                tutorial.holdsEnding -> null
                solved -> viewModel.mordieEnding() ?: SOLVED_TITLE
                else -> viewModel.mordieLine()
            },
            solved = solved,
            status = status,
            dialog = modal.shown,
            drag = dragging.offset,
            giveUp = if (guidance == null) viewModel.giveUpButton() else GiveUpButton.HIDDEN,
            solution = viewModel.solution(),
            slide = slide,
            // The Labels dialog's marked scheme, or a share link's labels, previewed until OK or Yes keeps them or Cancel or No drops them.
            labels = (modal.shown as? WebDialog.Labels)?.selected ?: (modal.shown as? WebDialog.SharedLabels)?.labels ?: viewModel.tileLabels(),
            focus = focus,
            announcement = saying,
            computer = computer,
            selection = keys.shownSelection(),
            selectionShown = keys.outlined(),
            hints = when {
                !computer -> null
                viewModel.solution() != null -> KeyHints.SOLUTION
                else -> KeyHints.board(controls.layout)
            },
            footer = updates.footer,
            guidance = guidance,
        )
    }

    private companion object {
        const val SOLVED_TITLE = "Congratulations ✓"
    }
}
