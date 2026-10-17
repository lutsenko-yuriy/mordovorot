package view.tui

import analytics.AnalyticsService
import board_model.BoardSize
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import view.View
import view.tui.dialog.ExitDialogController
import view.tui.dialog.LoadDialogController
import view.tui.dialog.RestoreDialogController
import view.tui.dialog.SaveDialogController
import viewmodel.ExitRequestedException
import viewmodel.ModeSwitcher
import viewmodel.SaveInfo
import viewmodel.UiRequest
import viewmodel.UnsolvableSaveChoice
import viewmodel.ViewModel
import viewmodel.displayWithSize

/**
 * The terminal-UI `view.View` implementation for GH-3 (mouse or keyboard, per [input]): owns its
 * own event loop, which is what lets a Congratulations screen exist at all. Every gesture
 * resolves to an existing [ViewModel] call. The Save/Load/Exit dialogs and the startup restore
 * prompt are dialog controllers in `view.tui.dialog` (GH-78), each blocking in the shared
 * [TuiScreen.runModal] loop over [terminal] (GH-64). Confirmed product decision: once solved, the arrows go dead ([ScreenState.arrowsEnabled] false) - see
 * [ScreenState]'s KDoc. WU5 adds the Congratulations screen itself: since every repaint asks
 * [ViewModel.isSolved] fresh rather than tracking a phase flag, the toolbar stays fully live
 * and a load from the Congratulations screen that restores an unsolved board flips the title and
 * arrows straight back - see [TuiScreen.wasSolved] for the one bit of state analytics needs
 * that the rendering doesn't.
 */
class TuiView internal constructor(
    private val terminal: Terminal,
    private val analytics: AnalyticsService,
    private val input: TuiInput,
    /** Internal so a test can check the production wiring isn't a no-op (GH-30). */
    internal val modeSwitcher: ModeSwitcher,
    private val viewModel: ViewModel,
    private val screen: TuiScreen,
    private val saveDialog: SaveDialogController,
    private val exitDialog: ExitDialogController,
    private val loadDialog: LoadDialogController,
    private val restoreDialog: RestoreDialogController,
) : View {

    /** Drains [viewModel]'s [viewmodel.ViewModel.uiRequests] alongside the board's own event
     *  loop (GH-42 WU2) - what lets `saveGame`/`loadGame`/`exitGame`/`restoreOnStartup` suspend
     *  on [viewmodel.UiRequestChannel.ask] instead of calling back into this view directly. */
    override suspend fun play(notice: String?) = coroutineScope {
        notice?.let { screen.showMessage(it) }
        val ui = launch { for (request in viewModel.uiRequests) handle(request) }
        try {
            terminal.enterRawMode()
            // GH-18's input-strategy seam (WU3): mouse reporting is MouseInput's own business
            // now, never turned on by a keyboard-only mode.
            input.prepare(terminal)
            try {
                viewModel.restoreOnStartup()
            } catch (e: ExitRequestedException) {
                return@coroutineScope // Exit from an unsolvable save at startup (GH-74).
            }
            screen.repaint()
            while (true) {
                // layout is always non-null here - the screen.repaint() call just above (or the one at
                // the end of every loop iteration below) always runs before the next readEvent.
                when (val action = input.onBoardEvent(terminal.readEvent(), checkNotNull(screen.layout))) {
                    is InputAction.Activate -> {
                        try {
                            handleTarget(action.target)
                        } catch (e: ExitRequestedException) {
                            return@coroutineScope
                        }
                        screen.repaint()
                    }
                    InputAction.Redraw -> screen.repaint()
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
                screen.showMessage(request.text)
                request.respond(Unit)
            }
            is UiRequest.ConfirmRestore -> request.respond(restoreDialog.confirmRestore(request.save))
            is UiRequest.ChooseSaveToRestore -> request.respond(restoreDialog.chooseSaveToRestore(request.saves))
            is UiRequest.ConfirmSaveBeforeExit -> request.respond(exitDialog.confirmSaveBeforeExit())
            is UiRequest.PromptSaveName -> request.respond(saveDialog.promptSaveName())
            is UiRequest.ChooseBoardSize -> request.respond(runSizeDialog(request.origin))
            is UiRequest.ResolveUnsolvableSave ->
                request.respond(runUnsolvableDialog(request.save, if (request.atStartup) "startup" else "toolbar"))
        }
    }

    private suspend fun handleTarget(target: HitTarget) {
        when (target) {
            is HitTarget.ShiftLeft -> shift { viewModel.shiftLeft(target.row) }
            is HitTarget.ShiftRight -> shift { viewModel.shiftRight(target.row) }
            is HitTarget.ShiftUp -> shift { viewModel.shiftUp(target.col) }
            is HitTarget.ShiftDown -> shift { viewModel.shiftDown(target.col) }
            HitTarget.ToolbarSave -> saveDialog.onToolbar()
            HitTarget.ToolbarLoad -> loadDialog.onToolbar()
            HitTarget.ToolbarExit -> exitDialog.onToolbar()
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
     *  Today, [TuiScreen.wasSolved] is guaranteed `false` on every call here - [shift] is only reachable
     *  through a click [BoardLayout.hitTest] resolves to a `HitTarget.Shift*`, which only
     *  happens when the layout it was built against had `arrowsEnabled = true`, which
     *  [TuiScreen.repaintWithDialog] only sets when the same [TuiScreen.wasSolved] sync came out `false`. The
     *  "fires exactly once" property currently rests on that arrows-disabled gate, not on this
     *  check (round 2 audit finding on PR #25). The check stays anyway as the one line standing
     *  between a correct single fire and a silent double-count the day arrows stop going dead on
     *  solve (e.g. a future "keep playing" affordance) - deleting it would save nothing today and
     *  cost real correctness the day that assumption breaks. */
    private fun shift(action: () -> Unit) {
        action()
        if (viewModel.isSolved() && !screen.wasSolved) analytics.track("screen_congratulations")
    }

    /** GH-44 WU3: the `[ New ]` toolbar button - opens the same size picker
     *  [UiRequest.ChooseBoardSize]'s startup handler does, but calls [ViewModel.newGame]
     *  directly instead of returning through `ask` (this isn't answering a raised request). */
    private fun handleToolbarNew() {
        runSizeDialog("toolbar")?.let { viewModel.newGame(it, trigger = "toolbar") }
    }

    /** GH-74's choice for an unsolvable save. Non-cancellable - the ticket offers exactly these
     *  three outcomes; end of input means Exit. Shows the viewModel's can't-be-solved message,
     *  which [TuiScreen.showMessage] just parked. */
    private fun runUnsolvableDialog(save: SaveInfo, openedFrom: String): UnsolvableSaveChoice {
        analytics.track("screen_unsolvable_dialog", mapOf("opened_from" to openedFrom))
        val message = screen.takeMessage() ?: "'${save.displayWithSize()}' can't be solved by any sequence of shifts."
        val dialog = Dialog(
            kind = Dialog.Kind.UNSOLVABLE,
            title = "Can't be solved",
            message = message,
            buttons = listOf(
                DialogButtonSpec("repair", "Repair & load"),
                DialogButtonSpec("new", "New game"),
                DialogButtonSpec("exit", "Exit"),
            ),
        )
        return screen.runModal("unsolvable", cancelled = UnsolvableSaveChoice.EXIT, cancellable = false, dialog = { dialog }) { action ->
            when ((action as? InputAction.Activate)?.target) {
                HitTarget.DialogButton("repair") -> return UnsolvableSaveChoice.REPAIR
                HitTarget.DialogButton("new") -> return UnsolvableSaveChoice.NEW_GAME
                HitTarget.DialogButton("exit") -> return UnsolvableSaveChoice.EXIT
                else -> {}
            }
        }
    }

    /** Runs the size-picker dialog's own blocking loop: a button per valid side, plus Cancel -
     *  buttons-only, like [handleToolbarExit]'s Exit dialog, so no text field or list to manage.
     *  Shared by the startup [UiRequest.ChooseBoardSize] handler and (GH-44 WU3) the toolbar
     *  `[ New ]` button/F9 - [openedFrom] is `"toolbar"` or a [UiRequest.ChooseBoardSize.origin]. Returns the chosen side,
     *  or `null` on Cancel/Escape/EOF - deliberately returns a value instead of calling
     *  `viewModel.newGame` itself, so the startup call site stays outside the deadlock-prone
     *  "View calls a request-raising viewModel method from its own request handler" shape (see
     *  [viewmodel.UiRequestChannel.ask]'s KDoc). */
    private fun runSizeDialog(openedFrom: String): Int? {
        analytics.track("screen_size_dialog", mapOf("opened_from" to openedFrom))
        val dialog = Dialog(
            kind = Dialog.Kind.SIZE,
            title = "New game size",
            buttons = (BoardSize.MIN..BoardSize.MAX).map { DialogButtonSpec(it.toString(), "${it}x$it") } +
                DialogButtonSpec("cancel", "Cancel"),
        )
        return screen.runModal<Int?>("size", cancelled = null, dialog = { dialog }) { action ->
            val target = (action as? InputAction.Activate)?.target
            (target as? HitTarget.DialogButton)?.id?.toIntOrNull()?.let { return it }
        }
    }

}
