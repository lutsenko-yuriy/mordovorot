package view.tui

import analytics.AnalyticsService
import analytics.NoopAnalyticsService
import presenter.ExitRequestedException
import presenter.Presenter
import view.View

/**
 * The mouse-driven `view.View` implementation for GH-3: owns its own event loop instead of
 * going through [presenter.PresenterImpl.play]'s console-only `while (!board.isCorrect())` loop
 * (see the ticket's solved-state note - this is what lets a Congratulations screen exist at
 * all). Every mouse gesture resolves to an existing [Presenter] call. WU4 adds the Save/Load/
 * Exit dialogs and the startup restore prompt: [confirmSaveBeforeExit]/[promptSaveName]/
 * [confirmRestore]/[chooseSaveToRestore] each run their own blocking modal loop over
 * [terminal], the same way [view.ViewImpl]'s console prompts block on `readLine()`. Confirmed
 * product decision: once solved, the arrows go dead ([ScreenState.arrowsEnabled] false) - see
 * [ScreenState]'s KDoc.
 */
class TuiView internal constructor(
    private val terminal: Terminal,
    private val analytics: AnalyticsService = NoopAnalyticsService(),
) : View {

    /** Must be assigned before [play] is called - use [create]. */
    lateinit var presenter: Presenter
        internal set

    private val renderer = ScreenRenderer()

    /** The layout the most recent repaint was drawn against - hit-tested on the next click, so
     *  what's clickable always matches what's on screen. Null only before the first repaint. */
    private var layout: BoardLayout? = null

    /** Set by a toolbar Exit click just before calling [presenter.exitGame], consumed once by
     *  [confirmSaveBeforeExit] - the dialog already collected the answer, so exitGame's
     *  callback doesn't prompt a second time. */
    private var pendingExitAnswer: Boolean? = null

    /** Set by [showMessage] so the next dialog (the exit flow's invalid-name re-prompt) can
     *  surface it, since this view has no separate message line of its own. */
    private var pendingMessage: String? = null

    companion object {
        /** The only public way to obtain a [TuiView] - wires [presenter] atomically, same
         *  pattern as [view.ViewImpl.create]. */
        fun create(terminal: Terminal, analytics: AnalyticsService = NoopAnalyticsService(), presenterFactory: (View) -> Presenter): TuiView {
            val view = TuiView(terminal, analytics)
            view.presenter = presenterFactory(view)
            return view
        }
    }

    override fun play() {
        terminal.enterRawMode()
        terminal.enableMouseReporting()
        try {
            presenter.restoreOnStartup()
            repaint()
            while (true) {
                when (val event = terminal.readEvent()) {
                    is TerminalEvent.MouseClick -> {
                        try {
                            handleClick(event.x, event.y)
                        } catch (e: ExitRequestedException) {
                            return
                        }
                        repaint()
                    }
                    TerminalEvent.EndOfInput -> return
                    // Keys/Backspace/Enter/Escape/Resize only matter while a dialog's modal
                    // loop is reading events directly - the board screen itself is mouse-only.
                    else -> {}
                }
            }
        } finally {
            // Belt-and-braces alongside AnsiTerminal's own shutdown hook - see WU3's note here.
            terminal.restore()
        }
    }

    private fun handleClick(x: Int, y: Int) {
        when (val target = layout?.hitTest(x, y) ?: HitTarget.Nothing) {
            is HitTarget.ShiftLeft -> presenter.shiftLeft(target.row)
            is HitTarget.ShiftRight -> presenter.shiftRight(target.row)
            is HitTarget.ShiftUp -> presenter.shiftUp(target.col)
            is HitTarget.ShiftDown -> presenter.shiftDown(target.col)
            HitTarget.ToolbarSave -> handleToolbarSave()
            HitTarget.ToolbarLoad -> handleToolbarLoad()
            HitTarget.ToolbarExit -> handleToolbarExit()
            else -> {}
        }
    }

    private fun handleToolbarSave() {
        when (val outcome = runSaveDialog("toolbar")) {
            is SaveOutcome.Confirm -> presenter.saveGame(outcome.name)
            SaveOutcome.Cancel -> {}
        }
    }

    private fun handleToolbarLoad() {
        when (val outcome = runLoadDialog("Load game", "toolbar")) {
            is LoadOutcome.Confirm -> presenter.loadGame(outcome.name)
            LoadOutcome.Cancel -> {}
        }
    }

    private fun handleToolbarExit() {
        analytics.track("screen_exit_dialog")
        val dialog = Dialog(
            kind = Dialog.Kind.EXIT,
            title = "Save before quitting?",
            buttons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("no", "No"), DialogButtonSpec("cancel", "Cancel")),
        )
        while (true) {
            repaintWithDialog(dialog)
            when (val event = terminal.readEvent()) {
                TerminalEvent.Escape -> return trackDialogCancelled("exit")
                is TerminalEvent.MouseClick -> when (DialogLayout(dialog, terminal.size()).hitTest(event.x, event.y)) {
                    HitTarget.DialogButton("yes") -> { pendingExitAnswer = true; presenter.exitGame(); return }
                    HitTarget.DialogButton("no") -> { pendingExitAnswer = false; presenter.exitGame(); return }
                    HitTarget.DialogButton("cancel") -> return trackDialogCancelled("exit")
                    else -> {}
                }
                TerminalEvent.EndOfInput -> return
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
        var typed = ""
        while (true) {
            val message = pendingMessage ?: overwriteWarning(typed)
            pendingMessage = null
            val dialog = Dialog(
                kind = Dialog.Kind.SAVE,
                title = "Save game",
                message = message,
                textFieldValue = typed,
                buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")),
            )
            repaintWithDialog(dialog)
            when (val event = terminal.readEvent()) {
                is TerminalEvent.KeyPress -> typed += event.char
                TerminalEvent.Backspace -> typed = typed.dropLast(1)
                TerminalEvent.Enter -> return SaveOutcome.Confirm(typed)
                TerminalEvent.Escape -> { trackDialogCancelled("save"); return SaveOutcome.Cancel }
                is TerminalEvent.MouseClick -> when (DialogLayout(dialog, terminal.size()).hitTest(event.x, event.y)) {
                    HitTarget.DialogButton("save") -> return SaveOutcome.Confirm(typed)
                    HitTarget.DialogButton("cancel") -> { trackDialogCancelled("save"); return SaveOutcome.Cancel }
                    else -> {}
                }
                TerminalEvent.EndOfInput -> return SaveOutcome.Cancel
                else -> {}
            }
        }
    }

    private fun overwriteWarning(name: String): String? =
        if (name.isNotEmpty() && presenter.saveExists(name)) "'$name' already exists - it will be overwritten." else null

    private sealed class LoadOutcome {
        data class Confirm(val name: String) : LoadOutcome()
        object Cancel : LoadOutcome()
    }

    /** Runs a Load-shaped dialog's own blocking loop: click a list row to select, Load/Cancel
     *  via click. Shared by the toolbar's Load and the startup restore prompt (same shape for
     *  any save count, per the plan). */
    private fun runLoadDialog(title: String, openedFrom: String): LoadOutcome {
        val saves = presenter.listSaves()
        analytics.track("screen_load_dialog", mapOf("opened_from" to openedFrom, "save_file_count" to saves.size))
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
            when (val event = terminal.readEvent()) {
                TerminalEvent.Escape -> { trackDialogCancelled("load"); return LoadOutcome.Cancel }
                is TerminalEvent.MouseClick -> when (val target = DialogLayout(dialog, terminal.size()).hitTest(event.x, event.y)) {
                    is HitTarget.DialogListRow -> selected = target.index
                    HitTarget.DialogButton("load") -> if (selected in saves.indices) return LoadOutcome.Confirm(saves[selected])
                    HitTarget.DialogButton("cancel") -> { trackDialogCancelled("load"); return LoadOutcome.Cancel }
                    else -> {}
                }
                TerminalEvent.EndOfInput -> return LoadOutcome.Cancel
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
        val state = ScreenState
            .forBoard(presenter.boardState().toList(), presenter.squareSide(), presenter.isSolved())
            .copy(dialog = dialog)
        layout = BoardLayout(terminalSize, state.squareSide, state.arrowsEnabled)
        terminal.write(renderer.render(state, terminalSize))
    }

    // The board's own displayBoard/processCommand belong to PresenterImpl.play()'s
    // console-only loop, which TuiView never calls (see class KDoc).
    override fun displayBoard(boardState: IntArray, squareSide: Int) {}

    override fun processCommand() {}

    override fun showMessage(message: String) {
        pendingMessage = message
    }

    /** The startup restore prompt for exactly one save - the same Load-shaped modal as any
     *  other save count (per the plan, this is where the console's 1-save yes/no split
     *  disappears in the TUI). */
    override fun confirmRestore(saveName: String): Boolean =
        runLoadDialog("Restore a saved game?", "startup") is LoadOutcome.Confirm

    override fun chooseSaveToRestore(saveNames: List<String>): String? =
        (runLoadDialog("Restore a saved game?", "startup") as? LoadOutcome.Confirm)?.name

    /** Consumes the answer the Exit dialog's Yes/No click already collected - see
     *  [pendingExitAnswer]. */
    override fun confirmSaveBeforeExit(): Boolean {
        val answer = pendingExitAnswer ?: false
        pendingExitAnswer = null
        return answer
    }

    override fun promptSaveName(): String? =
        when (val outcome = runSaveDialog("exit_flow")) {
            is SaveOutcome.Confirm -> outcome.name
            SaveOutcome.Cancel -> null
        }
}
