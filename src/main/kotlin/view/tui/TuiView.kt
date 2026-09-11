package view.tui

import presenter.Presenter
import view.View

/**
 * The mouse-driven `view.View` implementation for GH-3: owns its own event loop instead of
 * going through [presenter.PresenterImpl.play]'s console-only `while (!board.isCorrect())` loop
 * (see the ticket's solved-state note - this is what lets a Congratulations screen exist at
 * all). Every mouse gesture resolves to an existing [Presenter] call; this WU wires the board's
 * 16 shift arrows only - the toolbar (Save/Load/Exit) and dialogs are WU4. Confirmed product
 * decision: once solved, the arrows go dead ([ScreenState.arrowsEnabled] false) and Ctrl+C is
 * the only way to end the session until WU4/5 add a live toolbar/Congratulations screen -
 * accepted, not a dead-end bug (see [ScreenState]'s KDoc).
 */
class TuiView internal constructor(
    private val terminal: Terminal,
) : View {

    /** Must be assigned before [play] is called - use [create]. */
    lateinit var presenter: Presenter
        internal set

    private val renderer = ScreenRenderer()

    /** The layout the most recent repaint was drawn against - hit-tested on the next click, so
     *  what's clickable always matches what's on screen. Null only before the first repaint. */
    private var layout: BoardLayout? = null

    companion object {
        /** The only public way to obtain a [TuiView] - wires [presenter] atomically, same
         *  pattern as [view.ViewImpl.create]. */
        fun create(terminal: Terminal, presenterFactory: (View) -> Presenter): TuiView {
            val view = TuiView(terminal)
            view.presenter = presenterFactory(view)
            return view
        }
    }

    override fun play() {
        terminal.enterRawMode()
        terminal.enableMouseReporting()
        try {
            // presenter.restoreOnStartup() is deliberately not called yet: it would fire
            // startup_restore_prompt_shown/startup_restore_decision analytics for a prompt this
            // WU can't actually show (confirmRestore/chooseSaveToRestore are still no-op stubs,
            // see below) - misrepresenting every save-carrying launch as a declined restore, and
            // latching startupRestoreDone so WU4 couldn't retry it once the real dialog lands.
            // WU4 wires this in alongside the Load-dialog-shaped startup prompt (audit finding
            // on PR #22).
            repaint()
            while (true) {
                when (val event = terminal.readEvent()) {
                    is TerminalEvent.MouseClick -> {
                        handleClick(event.x, event.y)
                        repaint()
                    }
                    TerminalEvent.EndOfInput -> return
                    // Keys/Backspace/Enter/Escape/Resize are WU4 (dialog text field) territory -
                    // the board screen itself is mouse-only.
                    else -> {}
                }
            }
        } finally {
            // Belt-and-braces alongside AnsiTerminal's own shutdown hook. On a real TTY,
            // EndOfInput in practice never fires today (cbreak's -icanon disables VEOF, so
            // Ctrl+D arrives as a byte, not a stream close, and there's no dialog-driven exit
            // yet - both land in WU4) - restoration currently rests on the shutdown hook. This
            // branch exists for piped/scripted input (real EOF) and for WU4's exit dialog, which
            // will make EndOfInput/return reachable on a live terminal too.
            terminal.restore()
        }
    }

    private fun handleClick(x: Int, y: Int) {
        when (val target = layout?.hitTest(x, y) ?: HitTarget.Nothing) {
            is HitTarget.ShiftLeft -> presenter.shiftLeft(target.row)
            is HitTarget.ShiftRight -> presenter.shiftRight(target.row)
            is HitTarget.ShiftUp -> presenter.shiftUp(target.col)
            is HitTarget.ShiftDown -> presenter.shiftDown(target.col)
            // Toolbar/dialog targets are WU4; a click there is a no-op until then.
            else -> {}
        }
    }

    private fun repaint() {
        val terminalSize = terminal.size()
        val state = ScreenState.forBoard(presenter.boardState().toList(), presenter.squareSide(), presenter.isSolved())
        layout = BoardLayout(terminalSize, state.squareSide, state.arrowsEnabled)
        terminal.write(renderer.render(state, terminalSize))
    }

    // The remaining view.View members are WU4's dialog surface - stubbed out here so TuiView
    // compiles as a full View before dialogs exist. None of them are reachable in WU3: the
    // toolbar is a no-op (see handleClick) and displayBoard/processCommand belong to
    // PresenterImpl.play()'s console-only loop, which TuiView never calls (see class KDoc).

    override fun displayBoard(boardState: IntArray, squareSide: Int) {}

    override fun showMessage(message: String) {}

    override fun processCommand() {}

    override fun confirmRestore(saveName: String): Boolean = false

    override fun chooseSaveToRestore(saveNames: List<String>): String? = null

    override fun confirmSaveBeforeExit(): Boolean = false

    override fun promptSaveName(): String? = null
}
