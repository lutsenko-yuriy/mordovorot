package view.tui.dialog

import analytics.AnalyticsService
import view.tui.Dialog
import view.tui.DialogButtonSpec
import view.tui.HitTarget
import view.tui.InputAction
import view.tui.TuiScreen
import viewmodel.ViewModel

/** The give-up confirmation (GH-126): `[ Give Up ]` / 5. No or Escape keeps playing. */
internal class GiveUpDialogController(
    private val screen: TuiScreen,
    private val analytics: AnalyticsService,
    private val viewModel: ViewModel,
) {

    fun onToolbar() {
        if (confirm()) viewModel.giveUp()
    }

    private fun confirm(): Boolean {
        analytics.track("screen_give_up_dialog", mapOf("board_size" to viewModel.squareSide(), "move_count" to viewModel.moveCount()))
        val dialog = Dialog(
            kind = Dialog.Kind.GIVE_UP,
            title = "Give up and see Mordie's solution?",
            buttons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("cancel", "No")),
        )
        return screen.runModal("give_up", cancelled = false, dialog = { dialog }) { action ->
            if ((action as? InputAction.Activate)?.target == HitTarget.DialogButton("yes")) return true
        }
    }
}
