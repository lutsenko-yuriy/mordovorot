package view.tui

import analytics.AnalyticsService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import view.View
import viewmodel.ExitRequestedException
import viewmodel.ShiftKey
import viewmodel.ViewModel

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
    private val viewModel: ViewModel,
    private val screen: TuiScreen,
    private val requests: UiRequestRouter,
    private val toolbar: ToolbarRouter,
) : View {

    /** Drains [viewModel]'s [viewmodel.ViewModel.uiRequests] alongside the board's own event
     *  loop (GH-42 WU2) - what lets `saveGame`/`loadGame`/`exitGame`/`restoreOnStartup` suspend
     *  on [viewmodel.UiRequestChannel.ask] instead of calling back into this view directly. */
    override suspend fun play(notice: String?) = coroutineScope {
        notice?.let { screen.showMessage(it) }
        val ui = launch { for (request in viewModel.uiRequests) requests.handle(request) }
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
                            handleTarget(action.target, shortcut = action.shortcut)
                        } catch (e: ExitRequestedException) {
                            return@coroutineScope
                        }
                        screen.repaint()
                    }
                    is InputAction.Shift -> {
                        handleTarget(action.target, action.key)
                        screen.repaint()
                    }
                    InputAction.Redraw -> screen.repaint()
                    is InputAction.Scroll -> {
                        screen.scrollSteps(action.delta)
                        screen.repaint()
                    }
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

    /** [key] is the keyboard key behind a shift (GH-101), `null` for a click; [shortcut] says
     *  a toolbar target came from a key (GH-170). */
    private suspend fun handleTarget(target: HitTarget, key: ShiftKey? = null, shortcut: Boolean = false) {
        when (target) {
            is HitTarget.ShiftLeft -> shift { viewModel.shiftLeft(target.row, key) }
            is HitTarget.ShiftRight -> shift { viewModel.shiftRight(target.row, key) }
            is HitTarget.ShiftUp -> shift { viewModel.shiftUp(target.col, key) }
            is HitTarget.ShiftDown -> shift { viewModel.shiftDown(target.col, key) }
            else -> toolbar.handle(target, shortcut)
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
     *  happens when the layout it was built against had `arrowsEnabled = true`, or a keyboard
     *  shift key, which `KeyboardInput` makes inert once solved (GH-101); both gates track
     *  [TuiScreen.repaintWithDialog]'s own [TuiScreen.wasSolved] sync. The
     *  "fires exactly once" property currently rests on those gates, not on this
     *  check (round 2 audit finding on PR #25). The check stays anyway as the one line standing
     *  between a correct single fire and a silent double-count the day arrows stop going dead on
     *  solve (e.g. a future "keep playing" affordance) - deleting it would save nothing today and
     *  cost real correctness the day that assumption breaks. */
    private fun shift(action: () -> Unit) {
        action()
        if (viewModel.isSolved() && !screen.wasSolved) analytics.track("screen_congratulations")
    }
}
