package view.tui

import view.tui.dialog.ExitDialogController
import view.tui.dialog.GiveUpDialogController
import view.tui.dialog.LoadDialogController
import view.tui.dialog.SaveDialogController
import view.tui.dialog.SizeDialogController
import viewmodel.ModeSwitcher
import viewmodel.ViewModel

/** Runs the toolbar button (or its keyboard shortcut) the player activated (GH-78), the Give Up
 *  row's and the solution screen's Play included (GH-126). */
internal class ToolbarRouter(
    private val saveDialog: SaveDialogController,
    private val loadDialog: LoadDialogController,
    private val exitDialog: ExitDialogController,
    private val sizeDialog: SizeDialogController,
    private val giveUpDialog: GiveUpDialogController,
    private val viewModel: ViewModel,
    private val modeSwitcher: ModeSwitcher,
    private val input: TuiInput,
) {

    /** Any other target is ignored. */
    suspend fun handle(target: HitTarget) {
        when (target) {
            HitTarget.ToolbarSave -> saveDialog.onToolbar()
            HitTarget.ToolbarLoad -> loadDialog.onToolbar()
            HitTarget.ToolbarExit -> exitDialog.onToolbar()
            HitTarget.ToolbarNew -> sizeDialog.onToolbar()
            HitTarget.GiveUp -> giveUpDialog.onToolbar()
            HitTarget.MordiesSolution -> viewModel.showSolution()
            HitTarget.Play -> viewModel.playPause()
            is HitTarget.ToolbarMode -> modeSwitcher.switchTo(target.mode, trigger = input.switchTrigger)
            else -> {}
        }
    }
}
