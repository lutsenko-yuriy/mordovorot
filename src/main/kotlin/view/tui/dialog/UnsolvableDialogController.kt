package view.tui.dialog

import analytics.AnalyticsService
import view.tui.Dialog
import view.tui.DialogButtonSpec
import view.tui.HitTarget
import view.tui.InputAction
import view.tui.TuiScreen
import viewmodel.SaveInfo
import viewmodel.UnsolvableSaveChoice
import viewmodel.displayWithSize

/** The dialog for a save no sequence of shifts can solve (GH-74, GH-78). */
internal class UnsolvableDialogController(
    private val screen: TuiScreen,
    private val analytics: AnalyticsService,
) {

    /** GH-74's choice for an unsolvable save. Non-cancellable - the ticket offers exactly these
     *  three outcomes; end of input means Exit. Shows the viewModel's can't-be-solved message,
     *  which [TuiScreen.showMessage] just parked. */
    fun runUnsolvableDialog(save: SaveInfo, openedFrom: String): UnsolvableSaveChoice {
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
}
