package view.tui

import analytics.AnalyticsService
import analytics.NoopAnalyticsService
import board_model.BoardSize
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import view.View
import viewmodel.ExitRequestedException
import viewmodel.ModeSwitcher
import viewmodel.NoopModeSwitcher
import viewmodel.UiRequest
import viewmodel.ViewModel

/**
 * The mouse-driven `view.View` implementation for GH-3: owns its own event loop instead of
 * going through [view.ViewImpl.play]'s console-only `while (!viewModel.isSolved())` loop
 * (see the ticket's solved-state note - this is what lets a Congratulations screen exist at
 * all). Every mouse gesture resolves to an existing [ViewModel] call. WU4 adds the Save/Load/
 * Exit dialogs and the startup restore prompt: [confirmSaveBeforeExit]/[promptSaveName]/
 * [confirmRestore]/[chooseSaveToRestore] each run their own blocking modal loop over
 * [terminal], the same way [view.ViewImpl]'s console prompts block on `readLine()`. Confirmed
 * product decision: once solved, the arrows go dead ([ScreenState.arrowsEnabled] false) - see
 * [ScreenState]'s KDoc. WU5 adds the Congratulations screen itself: since every repaint asks
 * [ViewModel.isSolved] fresh rather than tracking a phase flag, the toolbar stays fully live
 * and a load from the Congratulations screen that restores an unsolved board flips the title and
 * arrows straight back - see [wasSolved] for the one bit of state analytics needs that the
 * rendering doesn't.
 */
class TuiView internal constructor(
    private val terminal: Terminal,
    private val analytics: AnalyticsService = NoopAnalyticsService(),
    private val input: TuiInput = MouseInput(),
) : View {

    /** Must be assigned before [play] is called - use [create]. */
    lateinit var viewModel: ViewModel
        internal set

    /** Wired atomically alongside [viewModel] in [create] - defaults to a no-op so every
     *  existing construction site keeps compiling unchanged (GH-30). */
    var modeSwitcher: ModeSwitcher = NoopModeSwitcher()
        internal set

    private val renderer = ScreenRenderer()

    /** The layout the most recent repaint was drawn against - hit-tested on the next click, so
     *  what's clickable always matches what's on screen. Null only before the first repaint. */
    private var layout: BoardLayout? = null

    /** The dialog geometry the most recent dialog repaint was drawn against - hit-tested on the
     *  next click inside a dialog loop, same reasoning as [layout]. A fresh `terminal.size()`
     *  read at click time could disagree with the size the frame was actually drawn at (audit
     *  finding on PR #24). Null whenever no dialog is open. */
    private var dialogLayout: DialogLayout? = null

    /** Set by a toolbar Exit click just before calling [viewModel.exitGame], consumed once by
     *  [confirmSaveBeforeExit] - the dialog already collected the answer, so exitGame's
     *  callback doesn't prompt a second time. */
    private var pendingExitAnswer: Boolean? = null

    /** Set by [showMessage] so the next dialog (the exit flow's invalid-name re-prompt) can
     *  surface it, since this view has no separate message line of its own. */
    private var pendingMessage: String? = null

    /** Whether the previous repaint saw the board solved - synced on every repaint regardless
     *  of cause (shift, Load, startup restore). [shift] reads this just before its own action to
     *  decide whether `screen_congratulations` (WU5) fires - see its KDoc for why the firing
     *  decision itself lives there instead of here. */
    private var wasSolved = false

    companion object {
        /** The only public way to obtain a [TuiView] - wires [viewModel] atomically, same
         *  pattern as [view.ViewImpl.create]. */
        fun create(
            terminal: Terminal,
            analytics: AnalyticsService = NoopAnalyticsService(),
            input: TuiInput = MouseInput(),
            modeSwitcherFactory: (View) -> ModeSwitcher = { NoopModeSwitcher() },
            viewModel: ViewModel,
        ): TuiView {
            val view = TuiView(terminal, analytics, input)
            view.modeSwitcher = modeSwitcherFactory(view)
            view.viewModel = viewModel
            return view
        }
    }

    /** Drains [viewModel]'s [viewmodel.ViewModel.uiRequests] alongside the board's own event
     *  loop (GH-42 WU2) - what lets `saveGame`/`loadGame`/`exitGame`/`restoreOnStartup` suspend
     *  on [viewmodel.ViewModelImpl.ask] instead of calling back into this view directly. */
    override suspend fun play() = coroutineScope {
        val ui = launch { for (request in viewModel.uiRequests) handle(request) }
        try {
            terminal.enterRawMode()
            // GH-18's input-strategy seam (WU3): mouse reporting is MouseInput's own business
            // now, never turned on by a keyboard-only mode.
            input.prepare(terminal)
            viewModel.restoreOnStartup()
            repaint()
            while (true) {
                // layout is always non-null here - the repaint() call just above (or the one at
                // the end of every loop iteration below) always runs before the next readEvent.
                when (val action = input.onBoardEvent(terminal.readEvent(), checkNotNull(layout))) {
                    is InputAction.Activate -> {
                        try {
                            handleTarget(action.target)
                        } catch (e: ExitRequestedException) {
                            return@coroutineScope
                        }
                        repaint()
                    }
                    InputAction.Redraw -> repaint()
                    InputAction.Quit -> return@coroutineScope
                    else -> {}
                }
            }
        } finally {
            // Belt-and-braces alongside AnsiTerminal's own shutdown hook - see WU3's note here.
            terminal.restore()
            ui.cancel()
        }
    }

    private suspend fun handle(request: UiRequest<*>) {
        when (request) {
            is UiRequest.ShowMessage -> {
                showMessage(request.text)
                request.respond(Unit)
            }
            is UiRequest.ConfirmRestore -> request.respond(confirmRestore(request.saveName))
            is UiRequest.ChooseSaveToRestore -> request.respond(chooseSaveToRestore(request.saveNames))
            is UiRequest.ConfirmSaveBeforeExit -> request.respond(confirmSaveBeforeExit())
            is UiRequest.PromptSaveName -> request.respond(promptSaveName())
            is UiRequest.ChooseBoardSize -> request.respond(runSizeDialog("startup"))
        }
    }

    private suspend fun handleTarget(target: HitTarget) {
        when (target) {
            is HitTarget.ShiftLeft -> shift { viewModel.shiftLeft(target.row) }
            is HitTarget.ShiftRight -> shift { viewModel.shiftRight(target.row) }
            is HitTarget.ShiftUp -> shift { viewModel.shiftUp(target.col) }
            is HitTarget.ShiftDown -> shift { viewModel.shiftDown(target.col) }
            HitTarget.ToolbarSave -> handleToolbarSave()
            HitTarget.ToolbarLoad -> handleToolbarLoad()
            HitTarget.ToolbarExit -> handleToolbarExit()
            HitTarget.ToolbarNew -> handleToolbarNew()
            is HitTarget.ToolbarMode -> modeSwitcher.switchTo(target.mode, trigger = input.switchTrigger)
            else -> {}
        }
    }

    /** Runs a shift and, only here, checks for the transition into solved -
     *  `screen_congratulations` tracks a board solved *by playing*, not one that arrives
     *  already solved via a toolbar/startup Load (audit finding on PR #25: gating on
     *  [viewmodel.ViewModel.isSolved] at repaint time alone fired the event on every restore of
     *  a pre-solved save).
     *
     *  Today, [wasSolved] is guaranteed `false` on every call here - [shift] is only reachable
     *  through a click [BoardLayout.hitTest] resolves to a `HitTarget.Shift*`, which only
     *  happens when the layout it was built against had `arrowsEnabled = true`, which
     *  [repaintWithDialog] only sets when the same [wasSolved] sync came out `false`. The
     *  "fires exactly once" property currently rests on that arrows-disabled gate, not on this
     *  check (round 2 audit finding on PR #25). The check stays anyway as the one line standing
     *  between a correct single fire and a silent double-count the day arrows stop going dead on
     *  solve (e.g. a future "keep playing" affordance) - deleting it would save nothing today and
     *  cost real correctness the day that assumption breaks. */
    private fun shift(action: () -> Unit) {
        action()
        if (viewModel.isSolved() && !wasSolved) analytics.track("screen_congratulations")
    }

    private suspend fun handleToolbarSave() {
        when (val outcome = runSaveDialog("toolbar")) {
            is SaveOutcome.Confirm -> {
                // The exit flow's promptForValidSaveName rejects a whitespace name because
                // console mode's save/load parsing splits on it (view.ViewImpl.nameArg) - a
                // name saved with a space could never be `load`ed back from the console.
                // Toolbar Save bypassed that check entirely, since it calls saveGame directly
                // rather than going through ViewModelImpl's exit-flow validation (audit round 5
                // on PR #24).
                if (outcome.name.any { it.isWhitespace() }) {
                    showMessage("'${outcome.name}' isn't a usable save name (no spaces) - not saved.")
                } else {
                    viewModel.saveGame(outcome.name)
                }
            }
            SaveOutcome.Cancel -> {}
        }
    }

    private suspend fun handleToolbarLoad() {
        when (val outcome = runLoadDialog("Load game", "toolbar")) {
            is LoadOutcome.Confirm -> viewModel.loadGame(outcome.name)
            LoadOutcome.Cancel -> {}
        }
    }

    /** GH-44 WU3: the `[ New ]` toolbar button - opens the same size picker
     *  [UiRequest.ChooseBoardSize]'s startup handler does, but calls [ViewModel.newGame]
     *  directly instead of returning through `ask` (this isn't answering a raised request). */
    private fun handleToolbarNew() {
        runSizeDialog("toolbar")?.let { viewModel.newGame(it, trigger = "toolbar") }
    }

    private suspend fun handleToolbarExit() {
        analytics.track("screen_exit_dialog")
        input.onDialogOpened()
        val dialog = Dialog(
            kind = Dialog.Kind.EXIT,
            title = "Save before quitting?",
            buttons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("no", "No"), DialogButtonSpec("cancel", "Cancel")),
        )
        while (true) {
            repaintWithDialog(dialog)
            when (val action = input.onDialogEvent(terminal.readEvent(), dialog, checkNotNull(dialogLayout))) {
                InputAction.Cancel -> return trackDialogCancelled("exit")
                is InputAction.Activate -> when (action.target) {
                    HitTarget.DialogButton("yes") -> { pendingExitAnswer = true; viewModel.exitGame(); return }
                    HitTarget.DialogButton("no") -> { pendingExitAnswer = false; viewModel.exitGame(); return }
                    HitTarget.DialogButton("cancel") -> return trackDialogCancelled("exit")
                    else -> {}
                }
                InputAction.Quit -> return
                else -> {}
            }
        }
    }

    private sealed class SaveOutcome {
        data class Confirm(val name: String) : SaveOutcome()
        object Cancel : SaveOutcome()
    }

    /** Runs the Save dialog's own blocking loop: typed name via key events, Save/Cancel via
     *  click. Shared by the toolbar's direct save and [promptSaveName] (the exit flow). */
    private fun runSaveDialog(openedFrom: String): SaveOutcome {
        analytics.track("screen_save_dialog", mapOf("opened_from" to openedFrom))
        input.onDialogOpened()
        // Consumed once, then kept sticky for the whole dialog session (overridden by a live
        // overwrite warning when one applies) - clearing it every iteration made the exit
        // flow's invalid-name explanation vanish after the user's very first keystroke, before
        // they'd typed a full corrected name (audit round 2 on PR #24).
        val initialMessage = pendingMessage
        pendingMessage = null
        var typed = ""
        while (true) {
            val dialog = Dialog(
                kind = Dialog.Kind.SAVE,
                title = "Save game",
                message = overwriteWarning(typed) ?: initialMessage,
                textFieldValue = typed,
                buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")),
            )
            repaintWithDialog(dialog)
            when (val action = input.onDialogEvent(terminal.readEvent(), dialog, checkNotNull(dialogLayout))) {
                is InputAction.TextChar -> typed += action.char
                InputAction.EraseChar -> typed = typed.dropLast(1)
                // An empty name is "skip saving" (matches the invalid-name re-prompt's own
                // "press Enter to skip saving" instruction) rather than a Confirm(""), which
                // would re-prompt forever - ViewModelImpl.promptForValidSaveName only stops on
                // null. Applies to both Submit and the Save button - round 1 only fixed Enter
                // (audit round 2 on PR #24).
                InputAction.Submit -> return if (typed.isEmpty()) SaveOutcome.Cancel else SaveOutcome.Confirm(typed)
                InputAction.Cancel -> { trackDialogCancelled("save"); return SaveOutcome.Cancel }
                is InputAction.Activate -> when (action.target) {
                    HitTarget.DialogButton("save") -> return if (typed.isEmpty()) SaveOutcome.Cancel else SaveOutcome.Confirm(typed)
                    HitTarget.DialogButton("cancel") -> { trackDialogCancelled("save"); return SaveOutcome.Cancel }
                    else -> {}
                }
                InputAction.Quit -> return SaveOutcome.Cancel
                else -> {}
            }
        }
    }

    private fun overwriteWarning(name: String): String? =
        if (name.isNotEmpty() && viewModel.saveExists(name)) "'$name' already exists - it will be overwritten." else null

    private sealed class LoadOutcome {
        data class Confirm(val name: String) : LoadOutcome()
        object Cancel : LoadOutcome()
    }

    /** Runs a Load-shaped dialog's own blocking loop: click a list row to select, Load/Cancel
     *  via click. Shared by the toolbar's Load and the startup restore prompt (same shape for
     *  any save count, per the plan). [preloadedSaves], when given, is shown as-is instead of
     *  a fresh [ViewModel.listSaves] call - [confirmRestore]/[chooseSaveToRestore] already
     *  receive the save list [viewmodel.ViewModelImpl.restoreOnStartup] queried, and re-querying
     *  instead risked disagreeing with it (audit finding on PR #24). */
    private fun runLoadDialog(title: String, openedFrom: String, preloadedSaves: List<String>? = null): LoadOutcome {
        val saves = preloadedSaves ?: viewModel.listSaves()
        analytics.track("screen_load_dialog", mapOf("opened_from" to openedFrom, "save_file_count" to saves.size))
        input.onDialogOpened()
        var selected = if (saves.isNotEmpty()) 0 else -1
        while (true) {
            val dialog = Dialog(
                kind = Dialog.Kind.LOAD,
                title = title,
                message = if (saves.isEmpty()) "No saves found." else null,
                listItems = saves,
                selectedIndex = selected,
                buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")),
            )
            repaintWithDialog(dialog)
            when (val action = input.onDialogEvent(terminal.readEvent(), dialog, checkNotNull(dialogLayout))) {
                InputAction.Cancel -> { trackDialogCancelled("load"); return LoadOutcome.Cancel }
                is InputAction.Activate -> when (val target = action.target) {
                    is HitTarget.DialogListRow -> selected = target.index
                    HitTarget.DialogButton("load") -> if (selected in saves.indices) return LoadOutcome.Confirm(saves[selected])
                    HitTarget.DialogButton("cancel") -> { trackDialogCancelled("load"); return LoadOutcome.Cancel }
                    else -> {}
                }
                InputAction.Quit -> return LoadOutcome.Cancel
                else -> {}
            }
        }
    }

    /** Runs the size-picker dialog's own blocking loop: a button per valid side, plus Cancel -
     *  buttons-only, like [handleToolbarExit]'s Exit dialog, so no text field or list to manage.
     *  Shared by the startup [UiRequest.ChooseBoardSize] handler and (GH-44 WU3) the toolbar
     *  `[ New ]` button/F9 - [openedFrom] is `"startup"` or `"toolbar"`. Returns the chosen side,
     *  or `null` on Cancel/Escape/EOF - deliberately returns a value instead of calling
     *  `viewModel.newGame` itself, so the startup call site stays outside the deadlock-prone
     *  "View calls a request-raising viewModel method from its own request handler" shape (see
     *  [viewmodel.ViewModelImpl.ask]'s KDoc). */
    private fun runSizeDialog(openedFrom: String): Int? {
        analytics.track("screen_size_dialog", mapOf("opened_from" to openedFrom))
        input.onDialogOpened()
        val dialog = Dialog(
            kind = Dialog.Kind.SIZE,
            title = "New game size",
            buttons = (BoardSize.MIN..BoardSize.MAX).map { DialogButtonSpec(it.toString(), "${it}x$it") } +
                DialogButtonSpec("cancel", "Cancel"),
        )
        while (true) {
            repaintWithDialog(dialog)
            when (val action = input.onDialogEvent(terminal.readEvent(), dialog, checkNotNull(dialogLayout))) {
                InputAction.Cancel -> { trackDialogCancelled("size"); return null }
                is InputAction.Activate -> when (val target = action.target) {
                    is HitTarget.DialogButton -> when (target.id) {
                        "cancel" -> { trackDialogCancelled("size"); return null }
                        else -> {
                            val chosen = target.id.toIntOrNull()
                            if (chosen != null) return chosen
                        }
                    }
                    else -> {}
                }
                InputAction.Quit -> return null
                else -> {}
            }
        }
    }

    private fun trackDialogCancelled(dialog: String) {
        analytics.track("dialog_cancelled", mapOf("dialog" to dialog))
    }

    private fun repaint() = repaintWithDialog(null)

    private fun repaintWithDialog(dialog: Dialog?) {
        val terminalSize = terminal.size()
        // The board-level status line is only shown once no dialog is up (dialogs show their
        // own Dialog.message instead) - consumed here so a stale message can't leak into a
        // dialog opened by the very next click (audit finding on PR #24).
        val message = if (dialog == null) pendingMessage.also { pendingMessage = null } else null
        // Only syncs wasSolved here - the actual screen_congratulations firing decision lives
        // in shift() so a Load doesn't count as the transition (see its KDoc).
        val solved = viewModel.isSolved()
        wasSolved = solved
        // Both the state and dialog run through the active input's decoration (cursor, focus
        // highlight, toolbar shortcut labels) before layout is built from them - draw and
        // hit-test must always agree on the same geometry, same invariant every layout class in
        // this package already keeps. MouseInput's decoration is the identity, so this is a
        // no-op for mouse mode (WU3's "no behaviour change" requirement).
        val decoratedDialog = dialog?.let { input.decorateDialog(it) }
        val state = input.decorateBoard(
            ScreenState
                .forBoard(viewModel.boardState().toList(), viewModel.squareSide(), solved)
                .copy(dialog = decoratedDialog, message = message),
        )
        layout = BoardLayout(terminalSize, state.squareSide, state.arrowsEnabled, state.toolbarShortcuts, state.modeButtons)
        // Built from state.dialog (post-decorateBoard), not decoratedDialog directly - the two
        // are the same object for MouseInput today, but a future decorateBoard that touches the
        // dialog (e.g. a keyboard mode collapsing decoration into one pass) must not be able to
        // desync what's hit-tested from what's actually drawn (audit finding on GH-18 WU3 PR #32).
        dialogLayout = state.dialog?.let { DialogLayout(it, terminalSize) }
        terminal.write(renderer.render(state, terminalSize))
    }

    // The board's own displayBoard/processCommand belong to ViewImpl.play()'s console-only
    // loop, which TuiView never calls (see class KDoc).
    override fun displayBoard(boardState: IntArray, squareSide: Int) {}

    override suspend fun processCommand() {}

    override suspend fun showMessage(message: String) {
        pendingMessage = message
    }

    /** The startup restore prompt for exactly one save - the same Load-shaped modal as any
     *  other save count (per the plan, this is where the console's 1-save yes/no split
     *  disappears in the TUI). */
    internal suspend fun confirmRestore(saveName: String): Boolean =
        runLoadDialog("Restore a saved game?", "startup", preloadedSaves = listOf(saveName)) is LoadOutcome.Confirm

    internal suspend fun chooseSaveToRestore(saveNames: List<String>): String? =
        (runLoadDialog("Restore a saved game?", "startup", preloadedSaves = saveNames) as? LoadOutcome.Confirm)?.name

    /** Consumes the answer the Exit dialog's Yes/No click already collected - see
     *  [pendingExitAnswer]. */
    internal suspend fun confirmSaveBeforeExit(): Boolean {
        val answer = pendingExitAnswer ?: false
        pendingExitAnswer = null
        return answer
    }

    internal suspend fun promptSaveName(): String? =
        when (val outcome = runSaveDialog("exit_flow")) {
            is SaveOutcome.Confirm -> outcome.name
            SaveOutcome.Cancel -> null
        }
}
