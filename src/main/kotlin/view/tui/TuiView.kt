package view.tui

import presenter.Presenter
import view.View

/**
 * The mouse-driven `view.View` implementation for GH-3: owns its own event loop instead of
 * going through [presenter.PresenterImpl.play]'s console-only `while (!board.isCorrect())` loop
 * (see the ticket's solved-state note - this is what lets a Congratulations screen exist at
 * all). Every mouse gesture resolves to an existing [Presenter] call; this WU wires the board's
 * 16 shift arrows only - the toolbar (Save/Load/Exit) and dialogs are WU4.
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
        presenter.restoreOnStartup()
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
