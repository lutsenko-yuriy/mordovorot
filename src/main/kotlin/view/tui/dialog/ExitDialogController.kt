package view.tui.dialog

import analytics.AnalyticsService
import view.tui.Dialog
import view.tui.DialogButtonSpec
import view.tui.HitTarget
import view.tui.InputAction
import view.tui.TuiScreen
import viewmodel.ViewModel

/** The Save-before-quitting dialog (GH-78): the toolbar Exit, and the viewModel's
 *  `ConfirmSaveBeforeExit` request it already answered. */
internal class ExitDialogController(
    private val screen: TuiScreen,
    private val analytics: AnalyticsService,
    private val viewModel: ViewModel,
) {

    /** Set by a toolbar Exit click just before calling [viewModel.exitGame], consumed once by
     *  [confirmSaveBeforeExit] - the dialog already collected the answer, so exitGame's
     *  callback doesn't prompt a second time. */
    private var pendingExitAnswer: Boolean? = null

    suspend fun onToolbar() {
        pendingExitAnswer = runExitDialog(cancellable = true) ?: return
        viewModel.exitGame()
    }

    /** The Save-before-quitting dialog: `true`/`false` for Yes/No, `null` on Cancel/Escape or end
     *  of input. Without Cancel ([cancellable] `false`, GH-74's mid-game Exit from an unsolvable
     *  save) Escape is ignored. */
    private fun runExitDialog(cancellable: Boolean): Boolean? {
        analytics.track("screen_exit_dialog")
        val buttons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("no", "No"))
        val dialog = Dialog(
            kind = Dialog.Kind.EXIT,
            title = "Save before quitting?",
            buttons = if (cancellable) buttons + DialogButtonSpec("cancel", "Cancel") else buttons,
        )
        return screen.runModal<Boolean?>("exit", cancelled = null, cancellable = cancellable, dialog = { dialog }) { action ->
            when ((action as? InputAction.Activate)?.target) {
                HitTarget.DialogButton("yes") -> return true
                HitTarget.DialogButton("no") -> return false
                else -> {}
            }
        }
    }

    /** Consumes the answer the toolbar Exit dialog already collected - see [pendingExitAnswer].
     *  With none (an Exit from GH-74's unsolvable-save dialog), asks here instead; end of input
     *  declines. */
    suspend fun confirmSaveBeforeExit(): Boolean {
        val answer = pendingExitAnswer ?: runExitDialog(cancellable = false) ?: false
        pendingExitAnswer = null
        return answer
    }
}
