package view.tui

import analytics.AnalyticsService
import viewmodel.ViewModel

/** What the TUI draws and the dialog loop over it (GH-78): the last-drawn geometry, the parked
 *  status message and the solved flag, shared by `TuiView`'s board loop and every dialog.
 *  [terminal], [input] and [analytics] are public only because the inline [runModal] reads
 *  them - dialogs go through [runModal] and [repaint], never read events themselves. */
internal class TuiScreen(
    val terminal: Terminal,
    val input: TuiInput,
    private val renderer: ScreenRenderer,
    private val viewModel: ViewModel,
    val analytics: AnalyticsService,
) {

    /** The layout the most recent repaint was drawn against - hit-tested on the next click, so
     *  what's clickable always matches what's on screen. Null only before the first repaint. */
    var layout: BoardLayout? = null
        private set

    /** The dialog geometry the most recent dialog repaint was drawn against - hit-tested on the
     *  next click inside a dialog loop, same reasoning as [layout]. A fresh `terminal.size()`
     *  read at click time could disagree with the size the frame was actually drawn at (audit
     *  finding on PR #24). Null whenever no dialog is open. */
    var dialogLayout: DialogLayout? = null
        private set

    /** Set by [showMessage] so the next dialog (the exit flow's invalid-name re-prompt) can
     *  surface it, since this view has no separate message line of its own. */
    private var pendingMessage: String? = null

    /** Whether the previous repaint saw the board solved - synced on every repaint regardless
     *  of cause (shift, Load, startup restore). `TuiView.shift` reads this just before its own
     *  action to decide whether `screen_congratulations` (WU5) fires - see its KDoc for why the
     *  firing decision itself lives there instead of here. */
    var wasSolved = false
        private set

    /** The solution screen's step list scroll (GH-126), reset whenever the steps change -
     *  leaving the screen, or loading another solution straight from this one. */
    private val stepWindow = StepListWindow()
    private var windowSteps: List<String>? = null

    fun scrollSteps(delta: Int) = stepWindow.scroll(delta)

    /** Shared dialog loop: tells [input] a dialog opened, then repaints [dialog] and passes each
     *  event to [onAction], which returns from the caller when done. Escape or the "cancel"
     *  button tracks `dialog_cancelled` and returns [cancelled] - unless not [cancellable], when
     *  Escape is ignored. End of input returns [cancelled] untracked either way. Inline so
     *  [onAction] can return from the caller. */
    inline fun <T> runModal(
        name: String,
        cancelled: T,
        cancellable: Boolean = true,
        dialog: () -> Dialog,
        onAction: (InputAction) -> Unit,
    ): T {
        input.onDialogOpened()
        while (true) {
            val current = dialog()
            repaintWithDialog(current)
            val action = input.onDialogEvent(terminal.readEvent(), current, checkNotNull(dialogLayout))
            when {
                action == InputAction.Quit -> return cancelled
                action == InputAction.Cancel && !cancellable -> {}
                action == InputAction.Cancel || (action as? InputAction.Activate)?.target == HitTarget.DialogButton("cancel") -> {
                    analytics.track("dialog_cancelled", mapOf("dialog" to name))
                    return cancelled
                }
                else -> onAction(action)
            }
        }
    }

    fun repaint() = repaintWithDialog(null)

    fun repaintWithDialog(dialog: Dialog?) {
        val terminalSize = terminal.size()
        // The board-level status line is only shown once no dialog is up (dialogs show their
        // own Dialog.message instead) - consumed here so a stale message can't leak into a
        // dialog opened by the very next click (audit finding on PR #24).
        val message = if (dialog == null) pendingMessage.also { pendingMessage = null } else null
        // Only syncs wasSolved here - the actual screen_congratulations firing decision lives
        // in `TuiView.shift` so a Load doesn't count as the transition (see its KDoc).
        val solved = viewModel.isSolved()
        wasSolved = solved
        // Both the state and dialog run through the active input's decoration (selection, focus
        // highlight, toolbar shortcut labels) before layout is built from them - draw and
        // hit-test must always agree on the same geometry, same invariant every layout class in
        // this package already keeps. MouseInput's decoration is the identity outside the
        // Controls dialog (GH-170).
        val decoratedDialog = dialog?.let { input.decorateDialog(it) }
        val solution = viewModel.solution()
        val base = input.decorateBoard(
            ScreenState
                .forBoard(
                    viewModel.boardState().toList(),
                    viewModel.squareSide(),
                    solved,
                    viewModel.moveCount(),
                    solvedTitle = viewModel.mordieEnding() ?: TITLE_SOLVED,
                    mordieLine = viewModel.mordieLine(),
                    giveUpButton = viewModel.giveUpButton(),
                    solution = solution,
                    tileLabels = viewModel.tileLabels(),
                )
                .copy(dialog = decoratedDialog, message = message),
        )
        // The step list's scroll goes into the state, not the geometry, so the layout comes first.
        val baseLayout = base.layout(terminalSize)
        layout = baseLayout
        if (solution?.steps != windowSteps) stepWindow.reset()
        windowSteps = solution?.steps
        solution?.let { stepWindow.update(it.steps.size, baseLayout.stepRows, it.current) }
        val state = base.copy(stepOffset = stepWindow.offset)
        // Built from state.dialog (post-decorateBoard), not decoratedDialog directly - the two
        // are the same object for MouseInput today, but a future decorateBoard that touches the
        // dialog (e.g. a keyboard mode collapsing decoration into one pass) must not be able to
        // desync what's hit-tested from what's actually drawn (audit finding on GH-18 WU3 PR #32).
        dialogLayout = state.dialog?.let { DialogLayout(it, terminalSize) }
        terminal.write(renderer.render(state, terminalSize))
    }

    fun showMessage(message: String) {
        pendingMessage = message
    }

    /** The parked message, cleared - for a dialog that shows it itself. */
    fun takeMessage(): String? = pendingMessage.also { pendingMessage = null }
}
